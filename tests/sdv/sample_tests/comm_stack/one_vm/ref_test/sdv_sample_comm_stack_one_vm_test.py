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

"""SDV CommStack - One VM - Reference Test"""

from mobly import asserts
import logging
import helper
from sdv_perfetto import perfetto_collector
from sdv_perfetto import perfetto_trace_processor
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner


class SdvSampleCommStackOneVMTest(sdv_base_test.SdvBaseTestClass):

  def setup_class(self):
    super().setup_class()
    self.sdv_device = self.get_device('device1').adb()
    self.perfetto_collector = perfetto_collector.PerfettoCollector(
        device=self.sdv_device
    )
    # TODO(b/395067075): Update test to work with authz enabled
    self.original_authz_value = self.sdv_device.execute_shell_command(
      'getprop sdv.authz.enable'
    )
    logging.info(
      f'Saving original sdv.authz.enable value: {self.original_authz_value}'
    )

  def start_trace(self):
    self.perfetto_collector.start_trace()

  def stop_trace_reference_test(self):
    trace_file_path = self.perfetto_collector.stop_trace(tag='device1')
    perfetto_trace = perfetto_trace_processor.PerfettoTraceProcessor(trace_file_path)
    perfetto_trace.export_built_in_metrics_to_crystalball(
        metric_names=['android_cpu', 'android_mem'],
        output_dir=self.sdv_device.log_path(),
        test_name=self.get_suite_name() + '#built_in_metrics',
        omit_base_name=False,
    )
    results = perfetto_trace.query(helper.QUERY_COMMAND)
    latency_data = [r.latency for r in results]
    metrics = {}
    for val in range(len(helper.QUANTITY)):
      quantity = helper.QUANTITY[val]
      interval = helper.INTERVAL_MSEC[val]

      for message_size in helper.MESSAGE_SIZE:
        group_size = quantity // 10
        group_entries = []
        if len(latency_data) >= group_size:
          group_entries = [latency_data.pop(0) for _ in range(group_size)]
        else:
          logging.warning(f'Not enough latency data for group {quantity} // 10')
          continue

        if group_entries:
          metrics[
              f'Interval{interval}_messagesize{message_size}_quantity{quantity}_latency_min'
          ] = min(group_entries)
          metrics[
              f'Interval{interval}_messagesize{message_size}_quantity{quantity}_latency_max'
          ] = max(group_entries)
          metrics[
              f'Interval{interval}_messagesize{message_size}_quantity{quantity}_latency_avg'
          ] = sum(group_entries) / len(group_entries)
    perfetto_trace_processor.export_to_crystalball(
        {__class__.__name__ + '#' + 'latency': metrics},
        output_dir=self.sdv_device.log_path(),
        test_name=__class__.__name__ + '#' + 'latency',
        omit_base_name=False,
    )

  def test_comm_stack_reference_test(self):
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} started'
    )

    # TODO(b/395067075): Update test to work with authz enabled
    # We override within the test body so that only this test is affected.
    self.sdv_device.execute_shell_command('setprop sdv.authz.enable false')

    self.start_trace()
    for val in range(len(helper.QUANTITY)):
      for message_size in helper.MESSAGE_SIZE:
        pub_command = helper.COMM_STACK_PUB_COMMAND.format(message_size, helper.QUANTITY[val], helper.INTERVAL_MSEC[val])
        result_pub = self.sdv_device.execute_shell_command_in_subprocess_log(pub_command)

        sub_command = helper.COMM_STACK_SUB_COMMAND.format(helper.QUANTITY[val] // 10)
        result_sub = self.sdv_device.execute_shell_command(sub_command)

        asserts.assert_true(
            helper.is_log_matching(
                helper.COMM_STACK_SUB_EXPECTED_RESULT.format(
                    helper.QUANTITY[val] // 10
                ),
                result_sub,
            ),
            'Expected result :'
            f' {helper.COMM_STACK_SUB_EXPECTED_RESULT.format(helper.QUANTITY[val] // 10)} \n'
            f' matching failed with actual result: {result_sub} \n'
            f'command was: {sub_command}'
        )

        result_last_message = self.sdv_device.execute_shell_command(
            helper.LAST_MESSAGE_COMMAND
        )

        asserts.assert_true(
            helper.is_log_matching(
                helper.LAST_MESSAGE_EXPECTED_RESULT.format(message_size),
                result_last_message,
            ),
            'Expected result :'
            f' {helper.LAST_MESSAGE_EXPECTED_RESULT.format(message_size)} \n'
            f' matching failed with actual result: {result_last_message} \n'
            f' command was: {helper.LAST_MESSAGE_COMMAND}'
        )

        helper.wait_for_condition(
            lambda: self.sdv_device.read_file(result_pub),
            helper.COMM_STACK_PUB_EXPECTED_RESULT.format(helper.QUANTITY[val]),
            error_message=f"Condition not met within timeout. Command was: {pub_command}"
        )
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} completed'
    )

  def teardown_test(self):
    if self.current_test_info.name == 'test_comm_stack_reference_test':
      self.stop_trace_reference_test()
      # TODO(b/395067075): Update test to work with authz enabled
      logging.info(
          f'Restoring sdv.authz.enable to {self.original_authz_value}'
      )
      self.sdv_device.execute_shell_command(
        f'setprop sdv.authz.enable {self.original_authz_value}'
      )

    super().teardown_test()

if __name__ == '__main__':
  # Start Test Execution Using SDV Test Framework
  sdv_test_runner.run()
