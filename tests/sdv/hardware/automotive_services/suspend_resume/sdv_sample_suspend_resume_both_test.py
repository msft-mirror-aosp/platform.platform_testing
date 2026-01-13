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

"""SDV Suspend Resume Test"""

import logging
from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from cuj22_common.sdv_cuj22_base_hw import SdvCuj22BaseHW
from common_hw.arm_hardware_suspend_resume import suspend_and_resume_devices, SdvQnxDevice


class SdvSampleSuspendResumeBothTest(
    sdv_base_test.SdvBaseTestClass, parameterized.TestCase, SdvCuj22BaseHW
):
    def setup_class(self):
        super().setup_class()

    @parameterized.named_parameters(
        {
            'testcase_name': '15_seconds_wait',
            'wait_time': 15,
        },
    )
    def test_suspend_resume_both(self, wait_time):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        self.setup_cuj22_devices_hw()
        self.verify_logcat_content_suspend_resume_tests()
        suspend_and_resume_devices(
            [
                SdvQnxDevice(
                    adb_device=self.adb_device_server,
                    qnx_guest_id=self.SERVER_DEVICE_QNX_ID
                ),
                SdvQnxDevice(
                    adb_device=self.adb_device_client,
                    qnx_guest_id=self.CLIENT_DEVICE_QNX_ID
                )
            ], wait_time)
        self.verify_logcat_content_suspend_resume_tests()

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} ended'
        )


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
