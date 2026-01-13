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

"""SDV One Device Availability Test"""

from mobly import asserts
from device_availability_common import SdvDeviceAvailability
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvOneDeviceAvailabilityTest(
    sdv_base_test.SdvBaseTestClass, SdvDeviceAvailability
):
    LOGCAT_EXAMPLE_LOG = 'Example Log'
    LOG_GREP = 'I log '

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1')

    def test_device_availability(self):
        self.log_test_info(self.TEST_START_LOG)

        # Check Availability of Device
        self.verify_device_online(self.sdv_device)
        self.verify_boot_completed(self.sdv_device)

        self.log_test_info(self.TEST_END_LOG)

    def test_verify_boot_args(self):
        self.log_test_info(self.TEST_START_LOG)

        sdv_expected_boot_properties = {
            'ro.boot.sdv.instance_name': 'instance1',
            'ro.boot.virt.address': '3'
        }

        self.verify_properties(self.sdv_device, sdv_expected_boot_properties)

        self.log_test_info(self.TEST_END_LOG)

    def test_logcat_availability_after_reboot(self):
        """This test is to ensure the reboot_device_and_verify_logcat works as

        expected. We only need to test in oneVM as it should be applicable to
        all scenarios.
        """
        self.log_test_info(self.TEST_START_LOG)

        self.sdv_device.adb().log().info(
            'Reboot and ensure logcat is running after'
        )
        self.sdv_device.adb().reboot_device_and_verify_logcat()

        self._send_log(self.LOGCAT_EXAMPLE_LOG)

        self.sdv_device.adb().log().info(
            'Check that the example messages appears'
        )

        asserts.assert_in(
            self.LOGCAT_EXAMPLE_LOG,
            self.sdv_device.adb().grep_from_logcat(self.LOG_GREP),
        )

        self.log_test_info(self.TEST_END_LOG)

    def _send_log(self, message):
        self.sdv_device.adb().log().info(f'Send to logcat: {message}')
        self.sdv_device.adb().execute_shell_command(
            f'log -p i "{message}"',
        )


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
