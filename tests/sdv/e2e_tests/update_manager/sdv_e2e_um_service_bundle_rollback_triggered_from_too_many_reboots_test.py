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
SDV Update Manager Agent Service Bundle Rollback Triggered From Too Many Reboots Test

Check that a service bundle update that is rebooted too many times during the activation stage without sending a Commit() request is rolled back.
"""
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.update.update_manager_base_class import UpdateManagerBaseClass


class SdvE2EUMServiceBundleRollbackTriggeredFromTooManyRebootsTest(sdv_base_test.SdvBaseTestClass, UpdateManagerBaseClass):
    APEX_NAME = "com.android.sdv.sample.apex.provider"

    def setup_class(self):
        super().setup_class()
        self.init_update_manager_base_class(self.APEX_NAME)
        self.sdv_device.adb().root_device()
        self.upload_service_bundle_update_payload()

    def test_service_bundle_rollback_triggered_from_too_many_reboots(self):
        def assert_update_activated():
            self.assert_apex_version_number(2)
            self.assert_in_status("ACTIVATE_POST_REBOOT_COMPLETE")

        self.prepare_service_bundle_update()
        self.assert_in_status("PREPARE_COMPLETE")

        self.client.activate()
        self.assert_in_status("ACTIVATE_PRE_REBOOT_COMPLETE")
        self.assert_apex_version_number(1)

        self.sdv_device.adb().reboot_device()

        # Activate Boot 1
        assert_update_activated()
        self.sdv_device.adb().reboot_device()

        # Activate Boot 2
        assert_update_activated()
        self.sdv_device.adb().reboot_device()

        # Activate Boot 3
        self.assert_apex_version_number(1)
        status = self.client.status()
        self.assert_in_status(
            "ACTIVATE_PRE_REBOOT_FAILURE", status_text=status)
        self.assert_in_status("ApexServiceError", status_text=status)

        self.client.rollback()
        self.assert_in_status("READY")

        self.sdv_device.adb().reboot_device()

        self.assert_apex_version_number(1)
        self.assert_in_status("READY")
        self.assert_no_staged_apex_directories()


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
