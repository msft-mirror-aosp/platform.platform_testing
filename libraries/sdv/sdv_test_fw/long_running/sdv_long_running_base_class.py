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
import logging

from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test

class SdvLongRunningBaseTestClass(sdv_base_test.SdvBaseTestClass):

    LONG_TEST_DEFAULT_TIMEOUT = 60 * 60 * 10 # ten hours
    TASK_INTERVAL_DEFAULT = 60 # one minute
    LOG_INTERVAL_DEFAULT = 0 # log messages between iterations turned off by default

    def setup_class(self):
        super().setup_class()
        self.start_time = None
        self.memory_usage = {}
        for device in self.get_device_list().values():
            self.memory_usage[device.adb().prop.get(SdvDeviceProperty.INSTANCE_NAME)] = {
                'start': None,
                'end': None,
                'min': None,
                'max': None,
            }

    def setup_test(self):
        super().setup_test()
        self.start_time = time.perf_counter()

    def teardown_test(self):
        self.start_time = None # reset the start time
        super().teardown_test()

    def get_memory_usage(self, device, command_name):
        """Gets the memory usage of a process on a device.

        Args:
            device: The device object to execute commands on.
            command_name: The name of the process to get the memory usage for.

        Returns:
            The memory usage in kilobytes (KB) as a float, it fails if the
            process is not found.
        """
        pid = device.execute_shell_command(f'pidof -s {command_name}', raise_exception=False).strip()
        result = device.execute_shell_command(f'top -bn1 -p {pid} | sed -n \'$p\'', raise_exception=False)
        asserts.assert_true(result, f'Unable to find process info for \"{command_name}\"')
        # This cuts out the actual value of the memory usage for the provided command and converts it into a number
        command_memory_kb = float(result.split()[5][:-1])
        return command_memory_kb

    def set_start_mem_usage(self, value, device):
        """Sets the start memory usage value."""
        self.memory_usage[device.prop.get(SdvDeviceProperty.INSTANCE_NAME)]['start'] = value

    def set_end_mem_usage(self, value, device):
        """Sets the end memory usage value."""
        self.memory_usage[device.prop.get(SdvDeviceProperty.INSTANCE_NAME)]['end'] = value

    def log_memory_usage(self, value, device):
        """Logs the memory usage value, updating min and max values."""
        device_memory_usage = self.memory_usage[device.prop.get(SdvDeviceProperty.INSTANCE_NAME)]
        if not device_memory_usage['max'] or device_memory_usage['max'] < value:
            device_memory_usage['max'] = value
        if not device_memory_usage['min'] or device_memory_usage['min'] > value:
            device_memory_usage['min'] = value

    def _time_elapsed(self, current_time_in_loop):
        elapsed_time = current_time_in_loop - self.start_time

        hours = int(elapsed_time // 3600)
        minutes = int((elapsed_time % 3600) // 60)
        seconds = int(elapsed_time % 60)
        return f'({hours:02d}h {minutes:02d}m {seconds:02d}s passed)'

    def print_memory_usage_summary(self, device):
        """Prints the memory usage summary."""
        device_name = device.prop.get(SdvDeviceProperty.INSTANCE_NAME)
        device.log().info(f"Finished memory monitoring for 'top' on device '{device_name}'.")
        device.log().info("--- Memory Usage Summary ---")
        device.log().info(f"Start Memory: {self.memory_usage[device_name]['start']} KB")
        device.log().info(f"End Memory:   {self.memory_usage[device_name]['end']} KB")
        device.log().info(f"Min Memory:   {self.memory_usage[device_name]['min']} KB")
        device.log().info(f"Max Memory:   {self.memory_usage[device_name]['max']} KB")
        device.log().info("----------------------------")

    def run_for_duration(
        self,
        task_function,
        task_interval_seconds = TASK_INTERVAL_DEFAULT,
        log_interval_seconds = LOG_INTERVAL_DEFAULT,
        duration_seconds = LONG_TEST_DEFAULT_TIMEOUT,
    ):
        """Runs the given task function periodically for a specified duration.

        Args:
            task_function: The function to be executed periodically.
            task_interval_seconds: The interval in seconds between task_function calls.
            log_interval_seconds: The interval in seconds for logging elapsed time.
                                    If 0, logging between iterations is disabled.
            duration_seconds: The total duration in seconds for which the task
                                function should be run.
        """
        next_task_time = self.start_time + task_interval_seconds
        next_log_time = self.start_time + log_interval_seconds
        deadline = self.start_time + duration_seconds
        current_time = time.perf_counter()
        while current_time < deadline:
            current_time = time.perf_counter()
            if log_interval_seconds > 0 and current_time >= next_log_time:
                logging.info(self._time_elapsed(time.perf_counter()))
                next_log_time += log_interval_seconds

            if current_time >= next_task_time:
                task_function()
                next_task_time += task_interval_seconds
