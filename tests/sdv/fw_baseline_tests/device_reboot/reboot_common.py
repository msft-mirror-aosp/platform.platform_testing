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
import time
from sdv_test_fw.device.sdv_property import SdvDeviceProperty

class SdvRebootCommon:

    def reboot_and_verify_logcat_for_all_devices(self):
        log_message_current_time = time.perf_counter()
        log_message = f'test_message_{log_message_current_time}'
        for device in self.get_device_list().values():
            device.adb().reboot_device_and_verify_logcat()
            # Logging after the reboot to make sure the device is responsive
            device.adb().execute_shell_command(f'log "{log_message}"')
            result = device.adb().grep_from_logcat(log_message)

            asserts.assert_true(
                result,
                f'Message \"{log_message}\" was not logged correctly on device "{device.adb().prop.get(SdvDeviceProperty.INSTANCE_NAME)}"',
            )
