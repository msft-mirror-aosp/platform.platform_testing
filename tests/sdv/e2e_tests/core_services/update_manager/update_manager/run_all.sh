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

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
RUNNER="${1:-atest}"

if [[ "${RUNNER}" == "atest" ]]; then
	TESTS=$(grep -hoP 'SdvE2E[^"]+' ${SCRIPT_DIR}/Android.bp)
	TEST_PREFIX="atest"
	TEST_SUFFIX="-- --test-arg com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-config-file-name:sdv_one_device_local_only_config.yaml"
elif [[ "${RUNNER}" == "catbox" ]]; then
	# Use catbox
	TESTS=$(find ${SCRIPT_DIR}/../../../tools/sdv_catbox/res/config -name 'sdv-e2e-um*' -printf '%f\n' | xargs -I{} -n 1 basename {} .xml)
	# Required for catbox to run correctly
	NOTIFY_AS_NATIVE="0.0.0.0:6520"
	TEST_PREFIX="./out/host/linux-x86/catbox/android-catbox/tools/catbox-tradefed run commandAndExit"
	TEST_SUFFIX="--{device1}serial 0.0.0.0:6520 --mobly-config-file-name sdv_one_device_local_only_config.yaml"
else
	echo "Unrecognized runner \"${RUNNER}\". Only \"atest\" or \"catbox\" are supported."
	exit 1
fi

TESTS_COUNT=$(echo ${TESTS} | wc -w)
COUNT=0

for TEST in $TESTS; do
	COUNT=$((COUNT+1))
	echo
	echo
	echo "--- [${COUNT}/${TESTS_COUNT}] RUNNING ${TEST} ---"
	echo
	echo

	yes | cvd create
	adb wait-for-device
	${TEST_PREFIX} ${TEST} ${TEST_SUFFIX}
	cvd rm
done
