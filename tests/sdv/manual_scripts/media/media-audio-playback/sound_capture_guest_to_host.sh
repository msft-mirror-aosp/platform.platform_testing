#!/usr/bin/env bash
# Copyright (C) 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


if [[ "${BASH_SOURCE[0]}" == */* ]]; then
	SCRIPT_DIR="${BASH_SOURCE[0]%/*}"
else
	SCRIPT_DIR="${PWD}"
fi

usage() {
	printf 'Usage: %q <ssh_target> <adb_device>\n' "${BASH_SOURCE[0]}"
	printf 'Script to test sound capture on host from guest playback.\n'
	printf 'Result files will automatically be download in the current '\
'working directory.\n'
	printf 'Provide the ssh target for the QNX hypervisor, '\
'and the adb device for the actual SDV guest to test\n'
}

if (( $# != 2 )); then
	usage >&2
	printf 'Please provide the ssh target and/or the adb device\n' >&2
	exit 1
fi

# Guest
ADB_TARGET="${2}"

# Host
SSH_HOST="${1}"
SSH_PARAMS=(-F "${HOME}/.ssh/hardware.config")

# Sound parameters (also compare with QVM configuration for the guest)
# Source: ag/36237010
HOST_SND_CARD=2
HOST_SND_DEV=0
GUEST_SND_CARD=0
GUEST_SND_DEV=2
mixer_guest="mixerC${HOST_SND_CARD}D${HOST_SND_DEV}"

# Very hardware dependent AND file dependent too!
CHANNELS=2
RATE=48000
SAMPLE_BITS=16
PERIOD_SIZE=768
PERIOD_NB=3
DURATION_REC_S=10
DURATION_SLEEP_EARLY_REC_S=1

# For QNX’s simulator, the source sample file is required to be 16-bits!
LOCAL_FILE="${SCRIPT_DIR}/victory.wav"
SOURCE_FILE="/data/local/tmp/${LOCAL_FILE##*/}"

# Using /guests filesystem as it is expected to have place.
# The ".%s" at the end is a placeholder for campaigns.
REC_FILE="/guests/host_record.C${HOST_SND_CARD}D${HOST_SND_DEV}.stereo.${RATE}Hz.${SAMPLE_BITS}b.${DURATION_REC_S}s.%s.wav"


# Playing on device pcmCXDY with X and Y
# respectively GUEST_SND_CARD and GUEST_SND_DEV
guest_play_sound() {
	printf '[*] Playing sound on ADB device ...\n'
	local GUEST_PLAY_SOUND=(
		tinyplay2
		-D "${GUEST_SND_CARD}"
		-d "${GUEST_SND_DEV}"
		-p "${PERIOD_SIZE}"
		-n "${PERIOD_NB}"
		-t "$((PERIOD_SIZE * PERIOD_NB))"
		"${SOURCE_FILE}"
	)
	adb -s "${ADB_TARGET}" shell "${GUEST_PLAY_SOUND[@]}"
}

adb_connect_and_root() {
	printf '[*] Connecting to guest through ADB ...\n'
	adb connect "${ADB_TARGET}" || {
		printf 'adb: Unable to connect to target %s.\n' "${ADB_TARGET}" >&2
		return 1
	}
	adb -s "${ADB_TARGET}" root || {
		printf 'adb: Unable to become root on target %s.\n' "${ADB_TARGET}" >&2
		#adb disconnect "${ADB_TARGET}"
		return 1
	}
}

adb_push_asset() {
	local asset="${1:-"${LOCAL_FILE}"}"

	printf '[*] Pushing source file in adb ...\n'
	adb -s "${ADB_TARGET}" push "${asset}" "${SOURCE_FILE}" || {
		printf 'adb: unable to push source file "%s" on remote target "%s".\n' "${asset}" "${ADB_TARGET}" >&2
		return 1
	}
}

guest_current_sound_controls() {
	printf '[*] Display current audio controls in guest ...\n'
	adb -s "${ADB_TARGET}" shell tinymix2 contents || {
		printf 'adb: unable to query audio controls with `tinymix2` on remote target "%s".\n' "${ADB_TARGET}" >&2
		return 1
	}
}

# Capturing on device pcmCXDY with X and Y
# respectively HOST_SND_CARD and HOST_SND_DEV
host_record() {
	local record="${1:-"${REC_FILE}"}"

	printf '[*] Recording sound on host in background (%d seconds) ...\n' "${DURATION_REC_S}"
	local HOST_CAPTURE_SOUND=(
		waverec
		-v
		-a "${HOST_SND_CARD}:${HOST_SND_DEV}"
		-b "${SAMPLE_BITS}"
		-n "${CHANNELS}"
		-r "${RATE}"
		-t "${DURATION_REC_S}"
		"${record}"
	)
	ssh "${SSH_PARAMS[@]}" "${SSH_HOST}" "${HOST_CAPTURE_SOUND[@]}" &

	# Not a local variable, but still, use `declare background_pid` before
	# calling this function.
	background_pid=$!

	printf '    INFO: Sleep for %d second (the recording is a bit slow to start) ...\n' "${DURATION_SLEEP_EARLY_REC_S}"
	sleep "${DURATION_SLEEP_EARLY_REC_S}"s
}

fetch_from_host() {
	local record="${1:-"${REC_FILE}"}"

	printf '[*] Fetching recorded file from host ...\n'
	scp "${SSH_PARAMS[@]}" "${SSH_HOST}:${record}" . || {
		printf 'scp: unable to fetch file "%s" on remote SSH "%s".\n' "${record}" "${SSH_HOST}" >&2
		return 1
	}
}


# Set default values.
# BEWARE! Do NOT set 100%, this is very LOUD and can be harmful.
# Important: the group name has to keep its double quotes, so keep between
# single ones.
campaign1() {
	printf '[*] Set volume of 70%% with balance center ...\n'
	adb -s "${ADB_TARGET}" shell \
		tinymix2 set "'${mixer_guest} Volume'" 70% 70%
	adb -s "${ADB_TARGET}" shell \
		tinymix2 set "'${mixer_guest} Balance'" 100
	adb -s "${ADB_TARGET}" shell \
		tinymix2 set "'${mixer_guest} Mute'" 0
}

# Set volume at 40% left and 70% right
# Important: the group name has to keep its double quotes, so keep between
# single ones.
campaign2() {
	printf '[*] Set volume of 40%% left, 70%% right ...\n'
	adb -s "${ADB_TARGET}" shell \
		tinymix2 set "'${mixer_guest} Volume'" 40% 70%
	adb -s "${ADB_TARGET}" shell \
		tinymix2 set "'${mixer_guest} Balance'" 100
	adb -s "${ADB_TARGET}" shell \
		tinymix2 set "'${mixer_guest} Mute'" 0
}

# Set balance at 10, so 95% to the left
# Important: the group name has to keep its double quotes, so keep between
# single ones.
campaign3() {
	printf '[*] Set volume of 70%%, balance at 95%% to the left (raw value 10) ...\n'
	adb -s "${ADB_TARGET}" shell \
		tinymix2 set "'${mixer_guest} Volume'" 70% 70%
	adb -s "${ADB_TARGET}" shell \
		tinymix2 set "'${mixer_guest} Balance'" 10
	adb -s "${ADB_TARGET}" shell \
		tinymix2 set "'${mixer_guest} Mute'" 0
}

# Mute
# Important: the group name has to keep its double quotes, so keep between
# single ones.
campaign4() {
	printf '[*] Set volume of 70%%, balance center, and mute ...\n'
	adb -s "${ADB_TARGET}" shell \
		tinymix2 set "'${mixer_guest} Volume'" 70% 70%
	adb -s "${ADB_TARGET}" shell \
		tinymix2 set "'${mixer_guest} Balance'" 100
	adb -s "${ADB_TARGET}" shell \
		tinymix2 set "'${mixer_guest} Mute'" 1
}


execute_campaign() {
	if (( $# < 1 )); then
		printf 'Missing parameter for campaign to execute\n' >&2
		return 1
	fi

	local campaign=$1

	# Execute campaign here.
	printf '[*] Executing campaign "%s"\n' "${campaign}"
	"${campaign}" || return $?

	# Get modifications
	guest_current_sound_controls

	local rec_file="$(printf "${REC_FILE}" "${campaign}")"
	declare background_pid
	host_record "${rec_file}"

	# PLAY SOUND HERE, AND ONLY THIS
	guest_play_sound

	printf '[*] Waiting for record on host to finish ...\n'
	wait "${background_pid}"
	background_rc=$?
	printf ' done, with return code: %s\n' "${background_rc}"

	fetch_from_host "${rec_file}" || return $?

	printf 'Success. File at path: %s\n' "${PWD}/${rec_file##*/}"
	return 0
}


## Setup environment
adb_connect_and_root || exit $?
adb_push_asset || exit $?

for camp in "campaign1" "campaign2" "campaign3" "campaign4"; do
	execute_campaign "${camp}"
done
