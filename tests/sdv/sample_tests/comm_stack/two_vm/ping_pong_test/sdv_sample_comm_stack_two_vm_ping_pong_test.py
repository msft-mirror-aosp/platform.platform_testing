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

"""SDV CommStack Test Two VM - Ping-Pong Notifications Latency"""

from mobly import asserts
import ast
import logging
import helper
from typing import Dict

from sdv_perfetto import perfetto_trace_processor
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleCommStackTwoVMPingPongTest(sdv_base_test.SdvBaseTestClass):
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

  def test_comm_stack_ping_pong_latency_test(self):
    def _parse_result_to_metrics(result: Dict[str, int], metrics: Dict[str, int], suffix_key: str, msg_size: int, quantity) -> None:
      full_metric_key = f'PingPong_messagesize{msg_size}_quantity{quantity}_{suffix_key}'
      metrics[full_metric_key] = result["Result"][suffix_key]

    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} started'
    )

    # TODO(b/395067075): Update test to work with authz enabled
    # We override within the test body so that only this test is affected.
    self.sdv_device_1.execute_shell_command('setprop sdv.authz.enable false')
    self.sdv_device_2.execute_shell_command('setprop sdv.authz.enable false')

    metrics = {}
    for quantity in helper.QUANTITY_PING_PONG:
      for message_size in helper.MESSAGE_SIZE:
        server_cmd = helper.PING_PONG_SERVER_COMMAND.format(message_size, quantity, message_size, quantity)
        server_cmd_tmp_file_path = self.sdv_device_1.execute_shell_command_in_subprocess_log(server_cmd)

        client_cmd = helper.PING_PONG_CLIENT_COMMAND.format(message_size, quantity, message_size, quantity, quantity, message_size)
        client_cmd_result = self.sdv_device_2.execute_shell_command(client_cmd)

        client_expected = helper.PING_PONG_EXPECTED_CLIENT_RESULT.format(message_size, quantity, message_size * quantity)
        asserts.assert_true(
          helper.is_log_matching(client_expected, client_cmd_result),
          'Expected result:'
          f' {client_expected} \n'
          f' matching failed with actual result: {client_cmd_result} \n'
          f' command was: {client_cmd}'
        )
        server_expected = helper.PING_PONG_EXPECTED_SERVER_RESULT.format(quantity)
        helper.wait_for_condition(lambda: self.sdv_device_1.read_file(server_cmd_tmp_file_path),
                                  server_expected,
                                  error_message=f'Condition not met within timeout. Command was {server_cmd}'
        )

        try:
          result_ping_pong_latency = ast.literal_eval(client_cmd_result.strip())

          _parse_result_to_metrics(result_ping_pong_latency, metrics, "min_round_trip_time_usec", message_size, quantity)
          _parse_result_to_metrics(result_ping_pong_latency, metrics, "max_round_trip_time_usec", message_size, quantity)
          _parse_result_to_metrics(result_ping_pong_latency, metrics, "avg_round_trip_time_usec", message_size, quantity)
        except (ValueError, SyntaxError, KeyError) as e:
          logging.error(f'Failed to parse result: {result_ping_pong_latency}, error: {e}')
          continue

    if metrics:
      perfetto_trace_processor.export_to_crystalball(
          {__class__.__name__ + '#' + 'ping_pong_latency_metrics': metrics},
          output_dir=self.sdv_device_2.log_path(),
          test_name=__class__.__name__ + '#' + 'ping_pong_latency_metrics',
          omit_base_name=False,
      )

    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} completed'
    )

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
