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

from mobly import asserts
import time
from sdv_test_fw.device.sdv_property import SdvDeviceProperty

# TODO(almuthanna): re-visit this to figure out if we can have multiple layers
# of inheritance for the BaseTestClass.
# We do not inherit from SdvLongRunningBaseTestClass because we cannot have
# more than one subclass of BaseTestClass per module.
class SdvLongRunningCommon:

    def start_top_command_usage(self, adb_device):
        # Start the top command in order to measure its memory usage over time
        adb_device.execute_shell_command_in_subprocess('top_command', 'top')
        self.set_start_mem_usage(
            self.get_memory_usage(adb_device, 'top'),
            adb_device,
        )

    def finish_top_command_usage(self, adb_device):
        self.set_end_mem_usage(
            self.get_memory_usage(adb_device, 'top'),
            adb_device,
        )
        self.print_memory_usage_summary(adb_device)

    def log_and_verify_device_is_responsive(self, adb_device, log_message):
        adb_device.execute_shell_command(f'log "{log_message}"')
        result = adb_device.grep_from_logcat(log_message)

        asserts.assert_true(
            result,
            f'Message \"{log_message}\" was not logged correctly on device "{adb_device.prop.get(SdvDeviceProperty.INSTANCE_NAME)}"',
        )

    def check_logcat_and_memory_all_vms(self):
        log_message_current_time = time.perf_counter()
        log_message = f'test_message_{log_message_current_time}'
        for device in self.get_device_list().values():
            self.log_and_verify_device_is_responsive(device.adb(), log_message)
            self.log_memory_usage(
                self.get_memory_usage(device.adb(), 'top'),
                device.adb(),
            )
