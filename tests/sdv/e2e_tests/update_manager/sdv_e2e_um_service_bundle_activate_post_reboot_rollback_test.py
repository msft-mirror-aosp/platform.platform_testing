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
SDV Update Manager Agent Service Bundle Activate Post Reboot Rollback Test

Check that a rollback during the ACTIVATE_POST_REBOOT_COMPLETE state for a service bundle update succeeds
"""
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.update.update_manager_base_class import UpdateManagerBaseClass


class SdvE2EUMServiceBundleActivatePostRebootRollbackTest(sdv_base_test.SdvBaseTestClass, UpdateManagerBaseClass):
    APEX_NAME = "com.android.sdv.sample.apex.provider"

    def setup_class(self):
        super().setup_class()
        self.init_update_manager_base_class(self.APEX_NAME)
        self.sdv_device.adb().root_device()
        self.upload_service_bundle_update_payload()

    def test_service_bundle_activate_post_reboot_rollback(self):
        self.prepare_service_bundle_update()
        self.assert_in_status("PREPARE_COMPLETE")

        self.client.activate()
        self.assert_in_status("ACTIVATE_PRE_REBOOT_COMPLETE")
        self.assert_apex_version_number(1)

        self.sdv_device.adb().reboot_device()

        self.write_user_data_file()
        self.assert_apex_version_number(2)
        self.assert_in_status("ACTIVATE_POST_REBOOT_COMPLETE")

        self.client.rollback()
        self.assert_in_status("ACTIVATE_POST_REBOOT_ROLLBACK_COMPLETE")

        self.sdv_device.adb().reboot_device()

        self.assert_apex_version_number(1)
        self.assert_in_status("READY")
        self.assert_no_staged_apex_directories()
        self.assert_user_data_removed()


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
