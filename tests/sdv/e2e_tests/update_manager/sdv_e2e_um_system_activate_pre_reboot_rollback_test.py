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
SDV Update Manager Agent System Activate Pre Reboot Rollback Test

Check that a rollback from the ACTIVATE_PRE_REBOOT_COMPLETE for a system update succeeds.
"""
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.update.update_manager_base_class import get_opposite_slot, UpdateManagerBaseClass


class SdvE2EUMSystemActivatePreRebootRollbackTest(sdv_base_test.SdvBaseTestClass, UpdateManagerBaseClass):
    APEX_NAME = "com.sdv.google.sample.apex.provider"

    def setup_class(self):
        super().setup_class()
        self.init_update_manager_base_class(self.APEX_NAME)
        self.sdv_device.adb().root_device()
        self.upload_system_update_payload()

    def test_system_activate_pre_reboot_rollback(self):
        initial_current_slot, initial_active_boot_slot = self.get_boot_slot_info()

        self.prepare_system_update()
        self.assert_in_status("PREPARE_COMPLETE")

        self.client.activate()
        self.assert_in_status("ACTIVATE_PRE_REBOOT_COMPLETE")
        self.assert_equal(self.get_active_boot_slot(),
                          get_opposite_slot(initial_active_boot_slot))

        self.client.rollback()
        self.assert_in_status("READY")
        self.assert_equal(self.get_active_boot_slot(),
                          initial_active_boot_slot)


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
