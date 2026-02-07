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

"""
Sdv Sample Diagnostics Communication Test

Tests communication between Two SDV VMs using IPv4
"""
import logging
import time
import os
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.update.update_manager_base_class import UpdateManagerBaseClass
from sdv_test_fw.verification import polling


class SdvSampleDiagUpdateTest(sdv_base_test.SdvBaseTestClass, UpdateManagerBaseClass):
    APEX_NAME = "com.sdv.google.sample.diagnostics_update"
    BUNDLE_NAME = "UpdatableDiagnosticsBundle"
    BUNDLE_FQIN = f"instance1:{APEX_NAME}.{BUNDLE_NAME}/myinstance"

    START_BUNDLE_COMMAND = f"sdv_service_bundle start {BUNDLE_FQIN}"
    DESTROY_BUNDLE_COMMAND = f"sdv_service_bundle destroy {BUNDLE_FQIN}"

    DIAG_AGENT_TAG = "sdv_diagnostics_agent"
    RE_WHEN_BUNDLE_V1 = r'\(TEST RELEVANT LOG.*tire_pressurization.*elem: F32, value: Some\(F32\(99.99\)\)'
    RE_WHEN_BUNDLE_V2_1 = r'\(TEST RELEVANT LOG.*tire_pressurization.*elem: F32, value: Some\(F32\(88.88\)\).*V2 of TirePressure message'
    RE_WHEN_BUNDLE_V2_2 = r'\(TEST RELEVANT LOG.*seat_temperature_check_routine.*elem: F32, value: Some\(F32\(20.0\)\).*elem: F32, value: Some\(F32\(68.0\)\)'

    def setup_class(self):
        super().setup_class()
        self.init_update_manager_base_class(
            self.APEX_NAME, local_artifact_dir="out/host/**")

    def teardown_class(self):
        self.adb_shell(self.DESTROY_BUNDLE_COMMAND)
        try:
            self.client.rollback()
            # reboot needed after rollback, for initial state of v1 installed:
            self.sdv_device.adb().reboot_device()
        except Exception as e:
            logging.info(
                f"Exception in teardown/rollback update. Test failed earlier. Exception: {e}")
        super().teardown_class()

    def test_diagnostics_behaviour_across_apex_update(self):
        logging.info("Start diag update test")
        logging.info(f"Running test from this dir: {os.getcwd()}")

        # start v1 of bundle, preinstalled on image:
        self.adb_shell(self.START_BUNDLE_COMMAND)

        # wait for diagnostic agent reporting correct routine response from bundle v1:
        polling.wait_and_verify_expected_logs(self.sdv_device.adb(),
                                                     grep_text=self.DIAG_AGENT_TAG,
                                                     expected_result=self.RE_WHEN_BUNDLE_V1)

        self.update_diagnostics_bundle()

        # start bundle, this time v2 will be started:
        self.adb_shell(self.START_BUNDLE_COMMAND)

        # wait for diag agent logs reporting new rotuine and modified routine:
        polling.wait_and_verify_expected_logs(
            self.sdv_device.adb(),
            grep_text=self.DIAG_AGENT_TAG,
            expected_result=self.RE_WHEN_BUNDLE_V2_1)
        polling.wait_and_verify_expected_logs(
            self.sdv_device.adb(),
            grep_text=self.DIAG_AGENT_TAG,
            expected_result=self.RE_WHEN_BUNDLE_V2_1)

    def update_diagnostics_bundle(self):
        self.assert_apex_version_number(1)
        self.upload_service_bundle_update_payload()
        self.prepare_service_bundle_update()
        self.client.activate()

        # following fun also roots adb and waits for logcat:
        self.sdv_device.adb().reboot_device_and_verify_logcat()

        self.assert_apex_version_number(2)


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
