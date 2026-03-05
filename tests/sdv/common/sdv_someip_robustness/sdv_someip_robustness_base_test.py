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
The base class defined here is specific to SOME/IP Robustness CUJs 30 and 31.

It doesn't need LM commands to restart the robustness service bundle, only to start it once and stop it st the end.
The toggling aspect is achieved by the vsomeip test binary by restarting the service offer - without needing to restart the binary.
"""

from mobly import asserts
import logging
import time
from dataclasses import dataclass
from enum import Enum

from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods

class SdvSomeIpRobBaseTestClass(sdv_base_test.SdvBaseTestClass):
    def setup_class(self):
        super().setup_class()
        self.sdv_device1 = self.get_device("device1").adb()
        self.sdv_device2 = self.get_device("device2").adb()

    def verify_bundle_lifecycle(self, name, action):
        EXPECTED_LOG = f"Service bundle.*{name}.*succeed"

        """Verifies that a service bundle has reached a specific lifecycle state."""
        WaitingMethods.wait_and_verify_expected_logs(
            self.sdv_device2,
            logcat_args="*:F sdv_service_bundle:*",
            grep_text= EXPECTED_LOG,
            assert_msg=f"Service bundle {name} didn't {action}",
        )

    def assert_bundle_successful_execution(self, action, service, bundle_instance):
        SB_PACKAGE = "com.sdv.google.sample.someip"
        SB_COMMAND = f"sdv_service_bundle {action} local-vm:{SB_PACKAGE}.{service}/{bundle_instance}"
        self.sdv_device2.execute_shell_command(
            shell_command=SB_COMMAND
        )
        self.verify_bundle_lifecycle(
            name=service,
            action=action,
        )

    def verify_robustness_bundle_output(self, cuj_num, run_vsomeip_counterpart = True):
        VALID_CUJ_NUMS = [30, 31, 33]
        if cuj_num not in VALID_CUJ_NUMS:
            asserts.fail(
                f"Unknown SOME/IP CUJ number: {cuj_num}"
            )

        self.execute_and_verify_service_bundle_action(action="start")

        if run_vsomeip_counterpart:
            self.run_vsomeip_counterpart(cuj_num)

        LOGCAT_ARGS = "*:F com_sdv_google_sample_someip_RobustnessChecker_instance:*"
        TIMEOUT_IN_SECS = 1800
        POLL_INTERVAL = 1

        WaitingMethods.wait_and_verify_expected_logs(
            self.sdv_device2,
            logcat_args=LOGCAT_ARGS,
            grep_text="Robustness checker test passed",
            assert_msg="Service bundle did not pass robustness test",
            poll_interval=POLL_INTERVAL,
            timeout=TIMEOUT_IN_SECS,
        )
        self.sdv_device2.clear_logcat()

        self.execute_and_verify_service_bundle_action(action="stop")

    def execute_and_verify_service_bundle_action(self, action):
        ROBUSTNESS_CHECKER = "RobustnessChecker"
        BUNDLE_INSTANCE = "instance"

        self.assert_bundle_successful_execution(
            action=action, service=ROBUSTNESS_CHECKER, bundle_instance=BUNDLE_INSTANCE
        )

    def run_vsomeip_counterpart(self, cuj_num):
        """Restart the device to bring sdv_vsomeip_robustness_tester into a consistent state."""
        self.sdv_device1.reboot_device()
        self.sdv_device1.wait_for_device_online()

        CUJ_NUM_PROP = "ROB_CUJ_NUM=" + str(cuj_num)
        SOMEIP_CONFIG_FILE = (
            "VSOMEIP_CONFIGURATION=/vendor/etc/vsomeip/robustness_tester.json"
        )
        SOMEIP_BASE_PATH = "VSOMEIP_BASE_PATH=/data/vendor/vsomeip/"
        SOMEIP_TESTER_COMMAND = "vendor/bin/sdv_vsomeip_robustness_tester"

        self.sdv_device1.execute_shell_command_in_subprocess_log(
            CUJ_NUM_PROP + " " + SOMEIP_CONFIG_FILE + " " + SOMEIP_BASE_PATH + " " + SOMEIP_TESTER_COMMAND
        )

    def set_sys_property(self, sys_property, value):
        """Set the system property."""
        self.sdv_device2.execute_shell_command(f"setprop {sys_property} {value}")
