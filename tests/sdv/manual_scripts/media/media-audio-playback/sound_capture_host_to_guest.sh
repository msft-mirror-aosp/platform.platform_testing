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
	printf 'Script to test sound capture on guest from host playback.\n'
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
PERIOD_SIZE=3840
PERIOD_NB=2
DURATION_REC_S=10
DURATION_SLEEP_EARLY_REC_S=1

LOCAL_FILE="${SCRIPT_DIR}/Road_Trip.48000.wav"
# Using /guests filesystem as it is expected to have place.
SOURCE_FILE="/guests/${LOCAL_FILE##*/}"

# The ".%s" at the end is a placeholder for campaigns.
REC_FILE="/data/local/tmp/guest_record.C${GUEST_SND_CARD}D${GUEST_SND_DEV}.stereo.${RATE}Hz.${SAMPLE_BITS}b.${DURATION_REC_S}s.%s.wav"


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

# Capturing on device pcmCXDY with X and Y
# respectively GUEST_SND_CARD and GUEST_SND_DEV
guest_record() {
	local record="${1:-"${REC_FILE}"}"

	printf '[*] Recording sound on guest in background (%d seconds) ...\n' "${DURATION_REC_S}"
	local GUEST_CAPTURE_SOUND=(
		tinycap2
		"${record}"
		-D "${GUEST_SND_CARD}"
		-d "${GUEST_SND_DEV}"
		-c "${CHANNELS}"
		-p "${PERIOD_SIZE}"
		-n "${PERIOD_NB}"
		-t "${DURATION_REC_S}"
	)
	adb -s "${ADB_TARGET}" shell "${GUEST_CAPTURE_SOUND[@]}" &

	# Not a local variable, but still, use `declare background_pid` before
	# calling this function.
	background_pid=$!

	printf '    INFO: Sleep for %d second (the recording is a bit slow to start) ...\n' "${DURATION_SLEEP_EARLY_REC_S}"
	sleep "${DURATION_SLEEP_EARLY_REC_S}"s
}

fetch_from_guest() {
	local record="${1:-"${REC_FILE}"}"

	printf '[*] Fetching recorded file from guest ...\n'
	adb -s "${ADB_TARGET}" pull "${record}" . || {
		printf 'adb: unable to pull file "%s" from guest target %s.\n' "${record}" "${ADB_TARGET}" >&2
		return 1
	}
}

scp_push_asset() {
	local asset="${1:-"${LOCAL_FILE}"}"

	printf '[*] Pushing source file in host ...\n'
	scp "${SSH_PARAMS[@]}" "${asset}" "${SSH_HOST}:${SOURCE_FILE}" || {
		printf 'scp: unable to push source file "%s" on remote SSH "%s".\n' "${asset}" "${SSH_HOST}" >&2
		return 1
	}
}

host_current_sound_controls() {
	printf '[*] Display current audio controls in host ...\n'
	local MIX_CTL_CMD=(
		mix_ctl
		-a "${HOST_SND_CARD}:${HOST_SND_DEV}"
		group '"PCM Mixer"'
	)

	ssh "${SSH_PARAMS[@]}" "${SSH_HOST}" "${MIX_CTL_CMD[@]}" || {
		printf 'ssh: unable to query audio controls with `mix_ctl` on remote SSH "%s".\n' "${SSH_HOST}" >&2
		return 1
	}
}

# Playing on device pcmCXDY with X and Y
# respectively HOST_SND_CARD and HOST_SND_DEV
host_play_sound() {
	printf '[*] Playing sound on host ...\n'
	local HOST_PLAY_SOUND=(
		wave
		-v
		-a "${HOST_SND_CARD}:${HOST_SND_DEV}"
		-m '"PCM Mixer"'
		"${SOURCE_FILE}"
	)
	ssh "${SSH_PARAMS[@]}" "${SSH_HOST}" "${HOST_PLAY_SOUND[@]}"
}


# Set default values.
# BEWARE! Do NOT set 100%, this is very LOUD and can be harmful.
# Important: the group name has to keep its double quotes, so keep between
# single ones.
campaign1() {
	printf '[*] Set volume of 70%% with balance center ...\n'
	local MIX_CTL_CMD=(
		mix_ctl
		-a "${HOST_SND_CARD}:${HOST_SND_DEV}"
		group '"PCM Mixer"'
		volume=70%
		balance=100
		mute=off
	)
	ssh "${SSH_PARAMS[@]}" "${SSH_HOST}" "${MIX_CTL_CMD[@]}"
}

# Set volume at 40% left and 70% right
# Important: the group name has to keep its double quotes, so keep between
# single ones.
campaign2() {
	printf '[*] Set volume of 40%% left, 70%% right ...\n'
	local MIX_CTL_CMD=(
		mix_ctl
		-a "${HOST_SND_CARD}:${HOST_SND_DEV}"
		group '"PCM Mixer"'
		volume0=40%
		volume1=70%
		balance=100
		mute=off
	)
	ssh "${SSH_PARAMS[@]}" "${SSH_HOST}" "${MIX_CTL_CMD[@]}"
}

# Set balance at 10, so 95% to the left
# Important: the group name has to keep its double quotes, so keep between
# single ones.
campaign3() {
	printf '[*] Set volume of 70%%, balance at 95%% to the left (raw value 10) ...\n'
	local MIX_CTL_CMD=(
		mix_ctl
		-a "${HOST_SND_CARD}:${HOST_SND_DEV}"
		group '"PCM Mixer"'
		volume=70%
		balance=10
		mute=off
	)
	ssh "${SSH_PARAMS[@]}" "${SSH_HOST}" "${MIX_CTL_CMD[@]}"
}

# Mute
# Important: the group name has to keep its double quotes, so keep between
# single ones.
campaign4() {
	printf '[*] Set volume of 70%%, balance center, and mute ...\n'
	local MIX_CTL_CMD=(
		mix_ctl
		-a "${HOST_SND_CARD}:${HOST_SND_DEV}"
		group '"PCM Mixer"'
		volume=70%
		balance=100
		mute=on
	)
	ssh "${SSH_PARAMS[@]}" "${SSH_HOST}" "${MIX_CTL_CMD[@]}"
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
	host_current_sound_controls

	local rec_file="$(printf "${REC_FILE}" "${campaign}")"
	declare background_pid
	guest_record "${rec_file}"

	# PLAY SOUND HERE, AND ONLY THIS
	host_play_sound

	printf '[*] Waiting for record on guest to finish ...\n'
	wait "${background_pid}"
	background_rc=$?
	printf ' done, with return code: %s\n' "${background_rc}"

	fetch_from_guest "${rec_file}" || return $?

	printf 'Success. File at path: %s\n' "${PWD}/${rec_file##*/}"
	return 0
}


## Setup environment
adb_connect_and_root || exit $?
scp_push_asset || exit $?

for camp in "campaign1" "campaign2" "campaign3" "campaign4"; do
	execute_campaign "${camp}"
done
