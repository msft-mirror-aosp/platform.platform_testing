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

"""Base Class for Device Availability tests"""

from mobly import asserts
from typing import Optional
import logging

from sdv_test_fw.test_execution.test_imports import AdbTimeoutError


class SdvDeviceAvailability:

    TEST_START_LOG = 'started'
    TEST_END_LOG = 'completed'


    def verify_device_online(self, device):
        device.adb().log().info('Verifying device is online')

        try:
            device.adb().wait_for_device_online()
        except AdbTimeoutError:
            asserts.fail(
                f'Device <{device.adb().get_device_serial()}> is not online'
            )

    def verify_boot_completed(self, device):
        device.adb().log().info('Verifying boot is completed')

        try:
            device.adb().wait_for_boot_complete()
        except AdbTimeoutError:
            asserts.fail(
                'Boot flag is not set for Device'
                f' <{device.adb().get_device_serial()}>'
            )

    def verify_properties(self, device, expected_boot_properties: dict[str, Optional[str]]):
        device.adb().log().info('Verifying boot properties')

        for boot_property, expected_value in expected_boot_properties.items():
            boot_property_value = device.adb().execute_shell_command(
                f'getprop {boot_property}')
            if expected_value is None:
                # Confirm that the property is set, do not check its actual value
                asserts.assert_not_equal(
                    # Unset properties return an empty string
                    "",
                    boot_property_value,
                    f"for boot property '{boot_property}' - expected some value, not an empty string, property is unset.")
            else:
                asserts.assert_equal(
                    expected_value,
                    boot_property_value,
                    f"for boot property '{boot_property}'"
                )
