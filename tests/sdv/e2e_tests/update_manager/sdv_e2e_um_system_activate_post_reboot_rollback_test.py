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
SDV Update Manager Agent System Activate Post Reboot Rollback Test

Check that a rollback from the ACTIVATE_POST_REBOOT_COMPLETE state for a system update succeeds.
"""
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.update.update_manager_base_class import get_opposite_slot, UpdateManagerBaseClass


class SdvE2EUMSystemActivatePostRebootRollbackTest(sdv_base_test.SdvBaseTestClass, UpdateManagerBaseClass):
    APEX_NAME = "com.android.sdv.sample.apex.provider"

    def setup_class(self):
        super().setup_class()
        self.init_update_manager_base_class(self.APEX_NAME)
        self.sdv_device.adb().root_device()
        self.upload_system_update_payload()

    def test_system_activate_post_reboot_rollback(self):
        initial_current_slot, initial_active_boot_slot = self.get_boot_slot_info()

        self.prepare_system_update()
        self.assert_in_status("PREPARE_COMPLETE")

        self.client.activate()
        self.assert_in_status("ACTIVATE_PRE_REBOOT_COMPLETE")
        self.assert_equal(self.get_active_boot_slot(),
                          get_opposite_slot(initial_active_boot_slot))

        self.sdv_device.adb().reboot_device()

        self.write_user_data_file()
        self.assert_equal(self.get_active_boot_slot(),
                          get_opposite_slot(initial_active_boot_slot))
        self.assert_equal(self.get_current_slot(),
                          get_opposite_slot(initial_current_slot))
        self.assert_in_status("ACTIVATE_POST_REBOOT_COMPLETE")

        self.client.rollback()
        self.assert_in_status("ACTIVATE_POST_REBOOT_ROLLBACK_COMPLETE")

        self.sdv_device.adb().reboot_device()

        self.assert_in_status("READY")
        self.assert_equal(self.get_current_slot(), initial_current_slot)
        self.assert_equal(self.get_active_boot_slot(),
                          initial_active_boot_slot)
        self.assert_user_data_removed()


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
