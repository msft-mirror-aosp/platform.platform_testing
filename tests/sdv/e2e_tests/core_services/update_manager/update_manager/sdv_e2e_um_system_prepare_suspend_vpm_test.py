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
SDV Update Manager Agent System Prepare Suspend Vpm Test

Check that VPM indicating the VM is suspending to RAM suspends a system update in the PREPARE state.
"""

from mobly import asserts
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_vepsm_session import SdvVepsmSession
from sdv_test_fw.update.update_manager_base_class import UpdateManagerBaseClass
from time import sleep


class SdvE2EUMSystemPrepareSuspendVpmTest(
    sdv_base_test.SdvBaseTestClass, UpdateManagerBaseClass
):
    APEX_NAME = "com.sdv.google.sample.apex.provider"

    # This is fairly arbitrary and it can be adjusted as needed
    PREPARE_SUSPEND_COMPLETE_STATUS_CHECK_ATTEMPTS = 10

    def setup_class(self):
        super().setup_class()
        self.init_update_manager_base_class(self.APEX_NAME)
        self.sdv_device.adb().root_device()
        self.upload_system_update_payload()

    def test_system_prepare_suspend_vpm(self):
        initial_current_slot, initial_active_boot_slot = self.get_boot_slot_info()

        vepsm_session = SdvVepsmSession(self.sdv_device.adb())

        vepsm_session.command_and_wait_for_output(
            "power-state power-on",
            "Received power-state-report from VPM ON , reason HOST_REQUESTED",
        )

        vepsm_session.command_and_wait_for_output(
            "power-state prepare ram",
            "Received power-state-report from VPM WAIT_FOR_FINISH , reason HOST_REQUESTED",
        )

        self.prepare_system_update(interrupt_threshold=self.TEST_INTERRUPT_THRESHOLD)

        # Automatically wake up the VM in 5 seconds from now.
        #
        # TODO: b/393555389 - This could potentially be rewritten to use the
        # equivalent to `cvd powerbtn` instead.
        self.adb_shell("rtcwake -m no -s 5")

        # No response expected
        vepsm_session.command("power-state finish ram")

        # The above command returns before the Update Manager receives the message from VPM.
        # This means the Update Manager may not complete the transition in time before we query the status.
        # Since we don't know when the Update Manager will complete the transition, we need to query the
        # status in a loop for as long as we expect the transition to occur.
        prepare_suspend_complete = False
        for _ in range(self.PREPARE_SUSPEND_COMPLETE_STATUS_CHECK_ATTEMPTS):
            # Add time between each loop to allow the state machine to progress
            sleep(1)

            status = self.client.status()
            if "PREPARE_SUSPEND_COMPLETE" in status:
                prepare_suspend_complete = True
                break

        asserts.assert_true(
            prepare_suspend_complete, "PREPARE_SUSPEND_COMPLETE state not reached"
        )

        resume_output = self.client.resume()
        self.assert_in("Update resumed", resume_output)
        self.assert_in_status("PREPARE_COMPLETE")

        self.client.rollback()
        self.assert_in_status("READY")
        self.assert_equal(self.get_active_boot_slot(), initial_active_boot_slot)


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
