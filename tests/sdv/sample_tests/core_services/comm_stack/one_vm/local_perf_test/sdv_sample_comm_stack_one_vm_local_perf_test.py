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

"""SDV CommStack - One VM - Local Perf Test"""

from mobly import asserts
import ast
import logging
import helper
from sdv_perfetto import perfetto_collector
from sdv_perfetto import perfetto_trace_processor
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner


class SdvSampleCommStackOneVMLocalPerfTest(sdv_base_test.SdvBaseTestClass):

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

  def test_comm_stack_local_perf_test(self):
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} started'
    )

    # TODO(b/395067075): Update test to work with authz enabled
    # We override within the test body so that only this test is affected.
    self.sdv_device.execute_shell_command('setprop sdv.authz.enable false')

    metrics = {}
    for quantity in helper.QUANTITY_LOCAL_PERF:
      for message_size in helper.MESSAGE_SIZE:
        expected_result_local_perf = helper.LOCAL_PERF_EXPECTED_RESULT.format(quantity, quantity * message_size)
        local_perf_cmd = helper.LOCAL_PERF_COMMAND.format(message_size, quantity)
        result_local_perf = self.sdv_device.execute_shell_command(local_perf_cmd)

        # Assert
        asserts.assert_true(
            helper.is_log_matching(expected_result_local_perf, result_local_perf),
            'Expected result:'
            f' {expected_result_local_perf} \n'
            f' matching failed with actual result: {result_local_perf} \n'
            f' command was: {local_perf_cmd}'
        )

        # Parse metrics
        try:
          result_dict = ast.literal_eval(result_local_perf.strip())
          avg_msg_nsec = result_dict["Result"]["avg_read_write_msg_nsec"]
          avg_mbps = result_dict["Result"]["avg_read_write_mbps"]
          metric_key_nsec = f'LocalPerf_messagesize{message_size}_quantity{quantity}_avg_msg_nsec'
          metric_key_mbps = f'LocalPerf_messagesize{message_size}_quantity{quantity}_avg_mbps'

          metrics[metric_key_nsec] = avg_msg_nsec
          metrics[metric_key_mbps] = avg_mbps

        except (ValueError, SyntaxError, KeyError) as e:
            logging.error(f'Failed to parse result: {result_local_perf}, error: {e}')
            continue

    if metrics:
      perfetto_trace_processor.export_to_crystalball(
          {__class__.__name__ + '#' + 'local_perf_metrics': metrics},
          output_dir=self.sdv_device.log_path(),
          test_name=__class__.__name__ + '#' + 'local_perf_metrics',
          omit_base_name=False,
      )
    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} completed'
    )

  def teardown_test(self):
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
