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

"""SDV SOME/IP Stack Load Indicators Test"""

import time
import logging
import re

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

class SdvSomeIpStackLoadIndicatorsTest(sdv_base_test.SdvBaseTestClass):
    GREP_TEXT = r"""someip_load_indicators value"""
    EXPECTED_LOG = r"""someip_load_indicators value = [0-9]+"""

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()

    def test_someip_stack_receives_load_indicators(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.GREP_TEXT,
            expected_result=self.EXPECTED_LOG,
            assert_msg="SOME/IP Stack cannot receive someip_load_indicators",
        )


        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed"
        )


if __name__ == "__main__":
    sdv_test_runner.run()
