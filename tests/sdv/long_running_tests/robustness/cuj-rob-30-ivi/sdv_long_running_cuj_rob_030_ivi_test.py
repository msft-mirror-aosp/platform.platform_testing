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
SDV sample 'CUJ-Rob-030-IVI' test.
"""

import logging

from sdv_test_fw.test_execution import sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods
from robustness import sdv_ivi_rob_base_test

CHECKER_MODE_PROP = "persist.com.sdv.google.sample.checker_mode"
CHECKER_CUJ_PROP = "persist.com.sdv.google.sample.checker_cuj"

class SdvLongRunningCujRob030IviTest(
    sdv_ivi_rob_base_test.SdvIviRobBaseTest,
):
    def setup_class(self):
        super().setup_class(ivi_device_name="device2", core_device_name="device1")

    def test_cuj_rob_030_ivi(self):
        # GIVEN SdvCarMonitorTestApp is running.
        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)
        self.wait_for_ivi_app_ready()

        # AND GIVEN the SdvCarMonitorTestApp is listening for the SOME/IP variant of the Foo message
        self.ivi_vm_device.execute_shell_command(self.ENABLE_SOMEIP_SUBSCRIBER_CMD)
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result="Starting foo message polling thread",
        )

        # WHEN the SOME/IP service publisher is run.
        self.run_vsomeip_counterpart()

        # THEN the SdvCarMonitorTestApp receives the correct sequence of messages
        VALUES = 1000
        INCREMENT = 10
        for i in range(5, VALUES, INCREMENT):
            self.verify_new_message_received_on_ivi_with_value(value=i)

    def run_vsomeip_counterpart(self):
        CUJ_NUM_PROP = "ROB_CUJ_NUM=30"
        SOMEIP_CONFIG_FILE = (
            "VSOMEIP_CONFIGURATION=/vendor/etc/vsomeip/robustness_tester.json"
        )
        SOMEIP_BASE_PATH = "VSOMEIP_BASE_PATH=/data/vendor/vsomeip/"
        SOMEIP_TESTER_COMMAND = "vendor/bin/sdv_vsomeip_robustness_tester"

        self.core_vm_device.execute_shell_command_in_subprocess_log(
            f"{CUJ_NUM_PROP} {SOMEIP_CONFIG_FILE} {SOMEIP_BASE_PATH} {SOMEIP_TESTER_COMMAND}"
        )

if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
