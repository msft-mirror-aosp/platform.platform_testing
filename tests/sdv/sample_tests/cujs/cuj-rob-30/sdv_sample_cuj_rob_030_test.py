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
SDV sample 'CUJ-Rob-030' test

Verifies:
 * Service bundle subscriber can detect unavailability of SOME/IP publisher.
 * Cross-vm pub/sub stops when the SOME/IP publisher is stopped.
 * Cross-vm pub/sub resumes when the SOME/IP publisher is restarted.
"""

import logging

from sdv_test_fw.test_execution import sdv_test_runner

from sdv_someip_robustness import sdv_someip_robustness_base_test

CHECKER_MODE_PROP = "persist.com.sdv.google.sample.checker_mode"
CHECKER_CUJ_PROP = "persist.com.sdv.google.sample.checker_cuj"

class SdvSampleCujRob030Test(sdv_someip_robustness_base_test.SdvSomeIpRobBaseTestClass):
    def setup_class(self):
        super().setup_class()
        self.set_sys_property(CHECKER_MODE_PROP, 1)
        self.set_sys_property(CHECKER_CUJ_PROP, 30)

        self.sdv_device2.reboot_device_and_verify_logcat()

    def test_cuj_rob_030(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )
        self.verify_robustness_bundle_output(cuj_num=30)
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed"
        )

if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
