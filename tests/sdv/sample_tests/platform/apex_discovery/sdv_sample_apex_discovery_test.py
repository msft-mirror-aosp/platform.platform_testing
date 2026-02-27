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

"""SDV Apex Discovery Test"""

import logging
import time

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

class SdvSampleApexDiscoveryTest(sdv_base_test.SdvBaseTestClass):

    EXPECTED_APEX_DISCOVERY_RESULT = 'sdv_apex_discovery_hello_world::service: Hello World!'
    APEX_LOGCAT_GREP_TEXT = 'com_android_sdv_sample_test1_ApexDiscoveryHelloWorld_instance-1'

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()

    def setup_test(self):
        super().setup_test()
        # We reboot the device as we needed the log right after the device
        # is started and the setup test clear the logcat.
        self.sdv_device.reboot_device()
        self.sdv_device.wait_for_device_online()

    def test_apex_discovery(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )
        polling.wait_and_verify_expected_logs(
            self.sdv_device,
            self.APEX_LOGCAT_GREP_TEXT,
            self.EXPECTED_APEX_DISCOVERY_RESULT,
        )
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} completed'
        )

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
