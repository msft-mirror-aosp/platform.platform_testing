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

from sdv_perfetto import perfetto_trace_processor
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner

class SdvRsRpcPerformanceTest(sdv_base_test.SdvBaseTestClass):

  RPC_SERVER_NAME = 'RPC_server'
  START_RPC_SERVER = 'sdv_comms_sample_rpc_server_rs'
  START_RPC_CLIENT_THROUGHPUT = 'sdv_comms_performance_rpc_client_rs throughput'
  START_RPC_CLIENT_LATENCY = 'sdv_comms_performance_rpc_client_rs latency'

  LOGCAT_EXPECTED_THROUGHPUT_TEXT = 'RPC performance test result: throughput (req/s) is '
  LOGCAT_EXPECTED_LATENCY_TEXT = 'RPC performance test result: latency (microseconds) is'

  LOGCAT_GREP_TEXT = 'rpc_performance_client'

  def setup_class(self):
    super().setup_class()
    self.sdv_device_1 = self.get_device('device1').adb()
    self.sdv_device_2 = self.get_device('device2').adb()

    self.metrics = {}

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

    # Start the rpc server. RPC server should run unlimited time. Because of
    # that start it in sub process to continue execution of the test.
    logging.info('Start RPC server')
    self.sdv_device_2.execute_shell_command_in_subprocess(self.RPC_SERVER_NAME,self.START_RPC_SERVER)

  def test_rs_rpc_cross_vm_throughput_test(self):
    logging.info('Start Cross VM Throughput Test')

    logging.info('Start RPC client for throughput test')
    # Start the client and wait for it to finish execution
    self.sdv_device_1.execute_shell_command(self.START_RPC_CLIENT_THROUGHPUT)

    logging.info('Getting result from logcat')
    logcat_result = self.sdv_device_1.grep_from_logcat(self.LOGCAT_GREP_TEXT)
    throughput = self.get_performance_value(logcat_result, self.LOGCAT_EXPECTED_THROUGHPUT_TEXT)

    logging.info(f'Cross VM throughput from log: {throughput} req/s')
    self.metrics.update({
        "cross_vm_throughput_req_per_second": throughput,
    })

  def test_rs_rpc_same_vm_throughput_test(self):
    logging.info('Start Same VM Throughput Test')

    logging.info('Start RPC client for throughput test')
    # Start the client and wait for it to finish execution
    self.sdv_device_2.execute_shell_command(self.START_RPC_CLIENT_THROUGHPUT)

    logging.info('Getting result from logcat')
    logcat_result = self.sdv_device_2.grep_from_logcat(self.LOGCAT_GREP_TEXT)
    throughput = self.get_performance_value(logcat_result, self.LOGCAT_EXPECTED_THROUGHPUT_TEXT)

    logging.info(f'Same VM throughput from log: {throughput} req/s')
    self.metrics.update({
        "same_vm_throughput_req_per_second": throughput,
    })

  def test_rs_rpc_cross_vm_latency_test(self):
    logging.info('Start Cross VM Latency Test')

    logging.info('Start RPC client for latency test')
    # Start the client and wait for it to finish execution
    self.sdv_device_1.execute_shell_command(self.START_RPC_CLIENT_LATENCY)

    logging.info('Getting result from logcat')
    logcat_result = self.sdv_device_1.grep_from_logcat(self.LOGCAT_GREP_TEXT)
    latency = self.get_performance_value(logcat_result, self.LOGCAT_EXPECTED_LATENCY_TEXT)

    logging.info(f'Cross VM latency from log: {latency} microseconds')
    self.metrics.update({
        "cross_vm_latency_microseconds": latency,
    })

  def test_rs_rpc_same_vm_latency_test(self):
    logging.info('Start Same VM Latency Test')

    logging.info('Start RPC client for latency test')
    # Start the client and wait for it to finish execution
    self.sdv_device_2.execute_shell_command(self.START_RPC_CLIENT_LATENCY)

    logging.info('Getting result from logcat')
    logcat_result = self.sdv_device_2.grep_from_logcat(self.LOGCAT_GREP_TEXT)
    latency = self.get_performance_value(logcat_result, self.LOGCAT_EXPECTED_LATENCY_TEXT)

    logging.info(f'Same VM latency from log: {latency} microseconds')

    self.metrics.update({
        "same_vm_latency_microseconds": latency,
    })

  def teardown_class(self):
    # TODO(b/395067075): Update test to work with authz enabled
    logging.info('Restoring original sdv.authz.enable settings')
    self.sdv_device_1.execute_shell_command(
      f'setprop sdv.authz.enable {self.original_authz_value_d1}'
    )
    self.sdv_device_2.execute_shell_command(
      f'setprop sdv.authz.enable {self.original_authz_value_d2}'
    )

    TESTNAME = f"{__class__.__name__}#perf"
    perfetto_trace_processor.export_to_crystalball(
        {TESTNAME: self.metrics},
        output_dir=self.sdv_device_1.log_path(),
        test_name=TESTNAME,
        omit_base_name=False,
    )

    super().teardown_class()

  def get_performance_value(self, logcat_result, prefix):
    """Asserts that a prefix exists in logs and extracts an integer value."""
    asserts.assert_in(prefix,
                        logcat_result,
                        f'The content [{prefix}] was not found in the log'
                        f' [{logcat_result}]')

    for line in logcat_result.splitlines():
      if prefix in line:
        try:
          value_str = line.split(prefix)[1]
          return int(value_str.strip())
        except (ValueError, IndexError):
          asserts.fail(f'Could not parse integer value after {prefix}; logcat output: {logcat_result}')

if __name__ == '__main__':
  # Start Test Execution Using SDV Test Framework
  sdv_test_runner.run()
