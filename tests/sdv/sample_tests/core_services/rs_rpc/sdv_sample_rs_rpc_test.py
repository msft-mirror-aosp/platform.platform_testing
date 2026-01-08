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

"""SDV CommStack RS RPC Test"""

from mobly import asserts
import logging
import helper

from sdv_perfetto import perfetto_collector
from sdv_perfetto import perfetto_trace_processor
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleRsRpcTest(sdv_base_test.SdvBaseTestClass):

  RPC_SERVER_NAME = 'RPC_server'
  RPC_CLIENT_NAME = 'RPC_client'
  START_RPC_CLIENT = 'sdv_comms_sample_rpc_client_rs'
  START_RPC_SERVER = 'sdv_comms_sample_rpc_server_rs'
  LOGCAT_EXPECTED_TEXT = 'sample_rpc_client: Received response: hello_world'
  LOGCAT_GREP_TEXT = 'sample_rpc_client'
  IN_ASSERT_MESSAGE = 'The line "{}" was not found in the log below:\n' + '{}\n'


  def setup_class(self):
    super().setup_class()
    self.sdv_device_1 = self.get_device('device1').adb()
    self.sdv_device_2 = self.get_device('device2').adb()
    self.perfetto_collector = perfetto_collector.PerfettoCollector(
        device=self.sdv_device_1
    )
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

  def start_trace(self):
    self.perfetto_collector.start_trace()

  def stop_trace(self):
    trace_file_path = self.perfetto_collector.stop_trace(tag='device1')
    perfetto_trace = perfetto_trace_processor.PerfettoTraceProcessor(trace_file_path)
    perfetto_trace.export_built_in_metrics_to_crystalball(
        metric_names=['android_cpu'],
        output_dir=self.sdv_device_1.log_path(),
        test_name=self.get_suite_name() + '#built_in_metrics',
        omit_base_name=False,
    )
    results = perfetto_trace.query(helper.QUERY_COMMAND)
    metrics = {}
    for r in results:
        metrics['create-client'] = r.dur

    perfetto_trace_processor.export_to_crystalball(
        {__class__.__name__ + '#' + 'duration': metrics},
        output_dir=self.sdv_device_1.log_path(),
        test_name=__class__.__name__ + '#' + 'duration',
        omit_base_name=False,
    )

  def test_rs_rpc_test(self):
    self.start_trace()

    logging.info('Start SdvSampleRsRPC Test')
    # start the rpc server
    logging.info('Start RPC server')
    self.sdv_device_2.execute_shell_command_in_subprocess(self.RPC_SERVER_NAME,self.START_RPC_SERVER)

    # start the rpc client
    logging.info('Start RPC client')
    self.sdv_device_1.execute_shell_command_in_subprocess(self.RPC_CLIENT_NAME,self.START_RPC_CLIENT)

    # Check the log to see if the rpc client has started
    logging.info('checking logcat if the rpc client has started')
    logcat_result = self.sdv_device_1.grep_from_logcat(self.LOGCAT_GREP_TEXT)
    asserts.assert_in(
          self.LOGCAT_EXPECTED_TEXT,
          logcat_result,
          f'The content [{self.LOGCAT_EXPECTED_TEXT}] was not found in the log'
          f' [{logcat_result}]',
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
