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

"""Verification logic for hardware suspend/resume baseline tests.

This class encapsulates the verification steps used across both single and
multi-VM test scenarios. It is intended to be used exclusively by the
`sdv_baseline_hw_suspend_resume_*vm_test.py` test suites to ensure consistent
validation behavior.
"""

import logging
import time
from mobly import asserts
import sdv_baseline_hw_suspend_resume_mixin as hw_suspend_resume


class SdvBaselineHwSuspendResumeTestVerification:
    VERIFY_CONNECTION_TEXT = "Connection works"

    def verify_host_connection(self):
        """Verifies the connection to the QNX hypervisor.

        Since the QNX connection is shared across all VMs, this verification
        is independent of specific device configurations.
        """
        self.host_command(f"echo {self.VERIFY_CONNECTION_TEXT}")

        logging.info(
            f"Verification echo output: {self.host_output_last_line()}"
        )
        asserts.assert_equal(
            self.host_output_last_line(), self.VERIFY_CONNECTION_TEXT
        )

    def verify_powerbtn_daemon_is_running_in_host(self, vm_config):
        """Verifies that the daemon that allows to fake powerbtn is running in

        the QNX hypervisor.

        Args:
            vm_config: The VM config for the device being tested.
        """
        asserts.assert_true(
            self._processes_are_running(vm_config.daemon_label),
            f"Daemon to wake up {vm_config.sdv_guest_name} is not running",
        )

    def verify_device_is_responsive(self, vm_config, sdv_device):
        """Verifies that the device is responsive.

        Checks if the device is reported as running by the hypervisor and
        verifies ADB responsiveness by executing a simple echo command.

        Args:
            vm_config: The VM config for the device being tested.
            sdv_device: The SDV device.
        """
        asserts.assert_true(
            self._device_is_running(vm_config),
            f"Device {vm_config.sdv_guest_name} is not running",
        )

        command = f'echo "{self.VERIFY_CONNECTION_TEXT}"'
        output = sdv_device.adb().execute_shell_command(command)
        asserts.assert_equal(output, self.VERIFY_CONNECTION_TEXT)

    def verify_device_suspend_resume(
        self, vm_config, pwm_session, idle_seconds
    ):
        """Verifies the suspend and resume flow for a specific VM.

        Executes the suspend-to-RAM sequence, waits for the specified idle time,
        and then triggers the resume sequence.

        Args:
            vm_config: The VM config for the device being tested.
            pwm_session: The open session for power management.
            idle_seconds: The duration in seconds to wait while the device is
              suspended.
        """
        self.pwm_suspend_to_ram(pwm_session, vm_config)

        logging.info(f"Do nothing for {idle_seconds} seconds.")
        time.sleep(idle_seconds)
        logging.info(f"{idle_seconds} seconds passed.")

        self.pwm_resume(pwm_session, vm_config)
