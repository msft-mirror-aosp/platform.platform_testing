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

"""SDV VPM SUSPEND/RESUME TEST."""

import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods
from vpm.sdv_vpm import SdvVpm

class SdvVpmSuspendTest(sdv_base_test.SdvBaseTestClass):
    """Tests CUJ Core 24 (Suspend/Resume)."""
    VPM_POWER_STATE_CLIENT_POWER_ON = "vepsm power-state power-on"
    VPM_POWER_STATE_CLIENT_PREPARE_RAM = "vepsm power-state prepare ram"
    VPM_POWER_STATE_CLIENT_FINISH_RAM = "vepsm power-state finish ram"
    WAKE_VM_UP_AFTER_5_SECONDS="rtcwake -m no -s 5"

    def setup_class(self):
        """Sets up the test class"""
        super().setup_class()
        self.adb_device = self.get_device("device1").adb()
        self.adb_device.wait_for_device_online()
        self.device_vpm = SdvVpm(self.adb_device)

    def setup_test(self):
        # Avoid parent `clear_all_devices` to store the start up logs from VMs.
        logging.info('VPM suspend e2e')


    def suspend_to_ram_and_wake_up(self):
        self.device_vpm.suspend_and_resume_vm_from_ram()


    def assert_vm_resumes(self):
        """asserts vm has been resumed"""
        service_tag = f"sdv_vpm_agent"
        expected_logs = f"sdv_vpm_agent has resumed successfully"

        WaitingMethods.wait_and_verify_expected_logs(
            self.adb_device,
            grep_text=service_tag,
            expected_result=expected_logs,
            logcat_args='*:F sdv_vpm_agent:*',
            assert_msg=f"VPM not resumed successfully."
        )


    def test_suspend_to_ram(self):
        """Tests suspend and wakeup"""

        # Suspend the first VM
        self.suspend_to_ram_and_wake_up()
        self.assert_vm_resumes()


if __name__ == "__main__":
    sdv_test_runner.run()
