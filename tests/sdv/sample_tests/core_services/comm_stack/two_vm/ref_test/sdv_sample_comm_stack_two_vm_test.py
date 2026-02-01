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

"""SDV CommStack Test Two VM - Reference Test"""

from mobly import asserts
import logging
import helper
from sdv_perfetto import perfetto_collector, collector_config
from sdv_perfetto import perfetto_trace_processor
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleCommStackTwoVMTest(sdv_base_test.SdvBaseTestClass):
  def setup_class(self):
    super().setup_class()
    self.sdv_device_1 = self.get_device('device1').adb()
    self.sdv_device_2 = self.get_device('device2').adb()
    # TODO(b/395067075): Update test to work with authz enabled
    self.original_authz_value_d1 = self.sdv_device_1.execute_shell_command(
        'getprop sdv.authz.enable'
    )
    logging.info(
      f'Saving d1 original sdv.authz.enable value: {self.original_authz_value_d1}'
    )
    self.original_authz_value_d2 = self.sdv_device_2.execute_shell_command(
        'getprop sdv.authz.enable'
    )
    logging.info(
          f'Saving d2 original sdv.authz.enable value: {self.original_authz_value_d2}'
    )
    self.sdv_device_1.execute_shell_command('setprop sdv.authz.enable false')
    self.sdv_device_2.execute_shell_command('setprop sdv.authz.enable false')

  def setup_test(self):
    super().setup_test()
    self.start_trace()

  def start_trace(self):
    self.perfetto_collector_1 = (
        perfetto_collector.PerfettoCollector(
            device=self.sdv_device_1,
            config=collector_config.CollectorConfig(
                config_path='comm_stack_trace_cfg.pbtx',
                multi_vm_tracing=True,
                multi_vm_tracing_vsock=False,
                secondary_devices=[self.sdv_device_2])
        )
    )

    self.perfetto_collector_1.start_trace(start_trace_delay=5)

  def stop_trace(self):
    trace_file_path = self.perfetto_collector_1.stop_trace(stop_trace_delay=30)
    logging.info(f'trace_file_path: {trace_file_path}')
    perfetto_trace = perfetto_trace_processor.PerfettoTraceProcessor(trace_file_path)
    perfetto_trace.export_built_in_metrics_to_crystalball(
        metric_names=['android_cpu', 'android_mem'],
        output_dir=self.sdv_device_1.log_path(),
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
        percentiles_metrics = {}
        if len(latency_data) >= group_size:
          group_entries = [latency_data.pop(0) for _ in range(group_size)]
          percentiles_metrics = helper.percentiles(group_entries, [90, 95, 99])
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
          metrics[
              f'Interval{interval}_messagesize{message_size}_quantity{quantity}_latency_90'
          ] = percentiles_metrics['90']
          metrics[
              f'Interval{interval}_messagesize{message_size}_quantity{quantity}_latency_95'
          ] = percentiles_metrics['95']
          metrics[
              f'Interval{interval}_messagesize{message_size}_quantity{quantity}_latency_99'
          ] = percentiles_metrics['99']
    perfetto_trace_processor.export_to_crystalball(
        {__class__.__name__ + '#' + 'latency': metrics},
        output_dir=self.sdv_device_1.log_path(),
        test_name=__class__.__name__ + '#' + 'latency',
        omit_base_name=False,
    )

  def test_comm_stack_two_vm_test(self):
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} started'
    )
    for val in range(len(helper.QUANTITY)):
      for message_size in helper.MESSAGE_SIZE:
        result_pub = self.sdv_device_1.execute_shell_command_in_subprocess_log(
            helper.COMM_STACK_PUB_COMMAND.format(message_size, helper.QUANTITY[val], helper.INTERVAL_MSEC[val])
        )
        result_sub_1 = self.sdv_device_1.execute_shell_command(
            helper.COMM_STACK_SUB_COMMAND.format(helper.QUANTITY[val] // 10)
        )
        result_sub_2 = self.sdv_device_2.execute_shell_command(
            helper.COMM_STACK_SUB_COMMAND.format(helper.QUANTITY[val] // 10)
        )

        asserts.assert_true(
            helper.is_log_matching(helper.COMM_STACK_SUB_EXPECTED_RESULT.format(
                    helper.QUANTITY[val] // 10
                ), result_sub_1),
            f'Expected result : {helper.COMM_STACK_SUB_EXPECTED_RESULT} \n matching failed with actual result: {result_sub_1}'
        )
        asserts.assert_true(
            helper.is_log_matching(helper.COMM_STACK_SUB_EXPECTED_RESULT.format(
                    helper.QUANTITY[val] // 10
                ), result_sub_2),
            f'Expected result : {helper.COMM_STACK_SUB_EXPECTED_RESULT} \n matching failed with actual result: {result_sub_2}'
        )

        result_last_message_1 = self.sdv_device_1.execute_shell_command(
            helper.LAST_MESSAGE_COMMAND
        )
        result_last_message_2 = self.sdv_device_2.execute_shell_command(
            helper.LAST_MESSAGE_COMMAND
        )

        asserts.assert_true(
            helper.is_log_matching(helper.LAST_MESSAGE_EXPECTED_RESULT.format(message_size), result_last_message_1),
            f'Expected result : {helper.LAST_MESSAGE_EXPECTED_RESULT.format(message_size)} \n matching failed with actual result: {result_last_message_1}'
        )
        asserts.assert_true(
            helper.is_log_matching(helper.LAST_MESSAGE_EXPECTED_RESULT.format(message_size), result_last_message_2),
            f'Expected result : {helper.LAST_MESSAGE_EXPECTED_RESULT.format(message_size)} \n matching failed with actual result: {result_last_message_2}'
        )
        helper.wait_for_condition(lambda: self.sdv_device_1.read_file(result_pub), helper.COMM_STACK_PUB_EXPECTED_RESULT.format(helper.QUANTITY[val]))

    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} completed'
    )

  def teardown_test(self):
    self.stop_trace()
    super().teardown_test()

  def teardown_class(self):
    # TODO(b/395067075): Update test to work with authz enabled
    logging.info('Restoring original sdv.authz.enable settings')
    self.sdv_device_1.execute_shell_command(
      f'setprop sdv.authz.enable {self.original_authz_value_d1}'
    )
    self.sdv_device_2.execute_shell_command(
      f'setprop sdv.authz.enable {self.original_authz_value_d2}'
    )
    super().teardown_class()

if __name__ == '__main__':
  # Start Test Execution Using SDV Test Framework
  sdv_test_runner.run()
