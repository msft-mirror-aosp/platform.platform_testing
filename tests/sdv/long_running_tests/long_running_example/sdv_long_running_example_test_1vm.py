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

"""SDV Long Running Example Test"""

from long_running_common import SdvLongRunningCommon
from sdv_test_fw.long_running import sdv_long_running_base_class
from sdv_test_fw.test_execution import sdv_test_runner


class SdvOneDeviceLongRunningExampleTest(
    sdv_long_running_base_class.SdvLongRunningBaseTestClass,
    SdvLongRunningCommon,
):

    LOG_INTERVAL_SECONDS = 60 * 60 # one hour


    def setup_class(self):
        super().setup_class()
        self.adb_device = self.get_device('device1').adb()

    def test_example(self):
        # Start the top command in order to measure its memory usage over time
        self.adb_device.execute_shell_command_in_subprocess('top_command', 'top')
        self.set_start_mem_usage(
            self.get_memory_usage(self.adb_device, 'top'),
            self.adb_device,
        )
        self.run_for_duration(
            task_function = self.check_logcat_and_memory_all_vms,
            log_interval_seconds = self.LOG_INTERVAL_SECONDS,
        )
        self.set_end_mem_usage(
            self.get_memory_usage(self.adb_device, 'top'),
            self.adb_device,
        )
        self.print_memory_usage_summary(self.adb_device)

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
