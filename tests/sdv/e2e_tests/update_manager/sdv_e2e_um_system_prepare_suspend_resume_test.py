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
SDV Update Manager Agent System Prepare Suspend Resume Test

Check that if a Suspend() request is sent during the PREPARE state of a system update that the update suspends correctly.
Also test to ensure this update can be resumed successfully.
"""
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.update.update_manager_base_class import UpdateManagerBaseClass, InterruptAction


class SdvE2EUMSystemPrepareSuspendResumeTest(sdv_base_test.SdvBaseTestClass, UpdateManagerBaseClass):
    APEX_NAME = "com.sdv.google.sample.apex.provider"

    def setup_class(self):
        super().setup_class()
        self.init_update_manager_base_class(self.APEX_NAME)
        self.sdv_device.adb().root_device()
        self.upload_system_update_payload()

    def test_system_prepare_suspend_resume(self):
        initial_current_slot, initial_active_boot_slot = self.get_boot_slot_info()

        self.prepare_system_update(
            interrupt_threshold=self.TEST_INTERRUPT_THRESHOLD, interrupt_action=InterruptAction.Suspend)
        self.assert_in_status("PREPARE_SUSPEND_COMPLETE")

        resume_output = self.client.resume()
        self.assert_in("Update resumed", resume_output)
        self.assert_in_status("PREPARE_COMPLETE")

        self.client.rollback()
        self.assert_in_status("READY")
        self.assert_equal(self.get_active_boot_slot(),
                          initial_active_boot_slot)


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
