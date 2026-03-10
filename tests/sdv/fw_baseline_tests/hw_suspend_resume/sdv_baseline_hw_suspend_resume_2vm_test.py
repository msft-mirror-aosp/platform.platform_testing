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

"""Two VM Baseline test for verifying the hardware suspend/resume testing flow.

This test ensures the reliability and stability of the suspend/resume testing
infrastructure on hardware for 2 VM.

The test verifies:
  1. Connection to the hypervisor.
  2. Responsiveness of the test environment.
  3. Correct hypervisor setup for suspend/resume operations.
  4. Basic suspend and resume functionality.
  5. Suspend and resume with a short idle period to ensure CI/CD support of
     complex scenarios.
"""

import logging

from absl.testing import parameterized
import sdv_baseline_hw_suspend_resume_mixin as hw_suspend_resume
import sdv_baseline_hw_suspend_resume_test_verification as test_verification
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner


class SdvBaselineHwSuspendResumeTwoVMTest(
    sdv_base_test.SdvBaseTestClass,
    hw_suspend_resume.SdvBaselineHwSuspendResumeMixin,
    test_verification.SdvBaselineHwSuspendResumeTestVerification,
    parameterized.TestCase,
):

    def setup_class(self):
        super().setup_class()
        self.sdv_device1 = self.get_device("device1")
        self.sdv_device2 = self.get_device("device2")

        # Hypervisor QNX is common for all VMs, so it is only necessary
        # to connect once. We use device1 serial as it always exists
        # independently of he number of VMs the test requires.
        self.connect_to_hypervisor_qnx()
        self.enable_fake_powerbtn(self.sdv_device1, self.DEVICE1_VM_CONFIG)
        self.enable_fake_powerbtn(self.sdv_device2, self.DEVICE2_VM_CONFIG)

    def setup_test(self):
        super().setup_test()

        # Useful for debugging specific errors in the test.
        self.log_vm_status(self.DEVICE1_VM_CONFIG)
        self.log_vm_status(self.DEVICE2_VM_CONFIG)

        # Open sessions for Power Management
        self.sdv_device1_pwm_session = (
            self.sdv_device1.adb().interactive_session(label="PWM")
        )
        self.sdv_device2_pwm_session = (
            self.sdv_device2.adb().interactive_session(label="PWM")
        )

    def teardown_test(self):
        logging.info("Cleaning up after test case.")

        # Useful for debugging specific errors in the test.
        self.log_vm_status(self.DEVICE1_VM_CONFIG)
        self.log_vm_status(self.DEVICE2_VM_CONFIG)

        # end Power Management session
        self.sdv_device1_pwm_session.close()
        self.sdv_device2_pwm_session.close()
        super().teardown_test()

    def teardown_class(self):
        logging.info("Cleaning up after test.")
        # Concluding the sleep process makes adb connection to get lost
        # because the device hangs. We cannot clean up the spawned processes
        # in QNX. This is a known limitation of the current approach.
        # See b/487626556 for details.
        logging.debug(
            "Not possible to fully clean spawned daemon for powerbtn in QNX."
            " This is a known limitation"
        )
        # End connection to QNX.
        self.host_session.logout()
        super().teardown_class()

    def test_verify_host_connection(self):
        self.verify_host_connection()

    def test_powerbtn_daemon_is_running_in_host(self):
        self.verify_powerbtn_daemon_is_running_in_host(self.DEVICE1_VM_CONFIG)
        self.verify_powerbtn_daemon_is_running_in_host(self.DEVICE2_VM_CONFIG)

    def test_device_is_responsive(self):
        self.verify_device_is_responsive(
            self.DEVICE1_VM_CONFIG, self.sdv_device1
        )
        self.verify_device_is_responsive(
            self.DEVICE2_VM_CONFIG, self.sdv_device2
        )

    @parameterized.named_parameters(
        {
            "testcase_name": "",
            "idle_seconds": 0,
        },
        {
            "testcase_name": "idle_15_secs",
            "idle_seconds": 15,
        },
    )
    def test_suspend_resume_hw(self, idle_seconds):
        self.verify_device_suspend_resume(
            self.DEVICE1_VM_CONFIG, self.sdv_device1_pwm_session, idle_seconds
        )
        self.verify_device_suspend_resume(
            self.DEVICE2_VM_CONFIG, self.sdv_device2_pwm_session, idle_seconds
        )


if __name__ == "__main__":
    sdv_test_runner.run()
