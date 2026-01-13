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
SDV sample 'CUJ-Rob-033' test

Verifies:
 * Service bundle can be started/stopped via LCM commands.
 * Cross-vm RPC stops when the service bundle is stopped.
 * Cross-vm RPC resumes when the service bundle is restarted.
 * SOME/IP counterpart can detect stop/start of SDV service bundle.
"""

import logging

from sdv_test_fw.test_execution import sdv_test_runner

from sdv_someip_robustness import sdv_someip_robustness_base_test

from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods

CHECKER_MODE_PROP = "persist.com.sdv.google.sample.checker_mode"
CHECKER_CUJ_PROP = "persist.com.sdv.google.sample.checker_cuj"
ITERATIONS = 100

class SdvSampleCujRob033Test(sdv_someip_robustness_base_test.SdvSomeIpRobBaseTestClass):
    def setup_class(self):
        super().setup_class()
        self.set_sys_property(CHECKER_MODE_PROP, 2)
        self.set_sys_property(CHECKER_CUJ_PROP, 33)

        self.sdv_device2.reboot_device_and_verify_logcat()

    def test_cuj_rob_033(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )
        self.run_vsomeip_counterpart(33)
        for i in range(ITERATIONS):
            logging.info(f"Iteration {i}")
            self.verify_robustness_bundle_output(cuj_num=33, run_vsomeip_counterpart=False)

        ROBUSTNESS_CHECKER_LOGCAT_TAG = "*:F sdv_vsomeip_robustness_tester:*"

        WaitingMethods.wait_and_verify_expected_logs(
            self.sdv_device1,
            logcat_args=ROBUSTNESS_CHECKER_LOGCAT_TAG,
            grep_text="RPC robustness test ended, passed = 1",
            assert_msg="SOME/IP counterpart did not pass robustness test",
        )

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed"
        )

if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
