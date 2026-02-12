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

"""SDV Service Bundler Memory Leak Test."""

import logging
import os
import time
from typing import List

from sdv_perfetto import collector_config
from sdv_perfetto import perfetto_collector, perfetto_trace_processor
from sdv_test_fw.device import sdv_adb
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner
from sdv_test_fw.verification import polling

SERVICE_BUNDLE_FOO_FQN = 'com.android.sdv.sample.foo.ServiceBundleFoo'
SERVICE_BUNDLE_QUX_FQN = 'com.android.sdv.sample.qux.ServiceBundleQux'
SYS_PROP_FOO_INTERVAL = 'persist.com.android.sdv.sample.foo.pub_interval_ms'
SYS_PROP_QUX_INTERVAL = 'persist.com.android.sdv.sample.qux.pub_interval_ms'
SYS_PROP_FOO_LOAD_SIZE = 'persist.com.android.sdv.sample.foo.load_size'
SYS_PROP_QUX_LOAD_SIZE = 'persist.com.android.sdv.sample.qux.load_size'
SYS_PROP_FOO_PUB_AMOUNT = 'persist.com.android.sdv.sample.foo.pub_maximum_amount'
SYS_PROP_QUX_PUB_AMOUNT = 'persist.com.android.sdv.sample.qux.pub_maximum_amount'
FOO_APP_NAME = 'ServiceBundleFoo'
QUX_APP_NAME = 'ServiceBundleQux'

SERVICE_BUNDLE_FOO_CREATE_MESSAGE = (
    'Service bundle'
    " 'local-vm:com.android.sdv.sample.foo.ServiceBundleFoo/instance' succeed to"
    " finished command 'Create'"
)
SERVICE_BUNDLE_QUX_CREATE_MESSAGE = (
    'Service bundle'
    " 'local-vm:com.android.sdv.sample.qux.ServiceBundleQux/instance' succeed to"
    " finished command 'Create'"
)
SERVICE_BUNDLE_FOO_START_MESSAGE = (
    'Service bundle'
    " 'local-vm:com.android.sdv.sample.foo.ServiceBundleFoo/instance' succeed to"
    " finished command 'Start'"
)
SERVICE_BUNDLE_QUX_START_MESSAGE = (
    'Service bundle'
    " 'local-vm:com.android.sdv.sample.qux.ServiceBundleQux/instance' succeed to"
    " finished command 'Start'"
)
SERVICE_BUNDLE_FOO_STOP_MESSAGE = (
    'Service bundle'
    " 'local-vm:com.android.sdv.sample.foo.ServiceBundleFoo/instance' succeed to"
    " finished command 'Stop'"
)
SERVICE_BUNDLE_QUX_STOP_MESSAGE = (
    'Service bundle'
    " 'local-vm:com.android.sdv.sample.qux.ServiceBundleQux/instance' succeed to"
    " finished command 'Stop'"
)
FOO_SEND_MESSAGE = (
    'com_android_sdv_sample_foo_ServiceBundleFoo_instance: sample: Sent'
)
QUX_SEND_MESSAGE = (
    'com_android_sdv_sample_qux_ServiceBundleQux_instance: sample: Sent'
)
SERVICE_BUNDLE_START = 'start'
SERVICE_BUNDLE_CREATE = 'create'
SERVICE_BUNDLE_STOP = 'stop'
SERVICE_BUNDLE_SHUTDOWN = 'destroy'
CREATED_LOG = 'Creating instance.*:{fqn}/instance'
STARTED_LOG = 'Starting instance.*:{fqn}/instance'
STOPPED_LOG = 'Joined execution thread'
SAMPLES_LOGCAT_ARGS = (
    '*:F com_android_sdv_sample_foo_ServiceBundleFoo_instance:*'
    ' com_android_sdv_sample_qux_ServiceBundleQux_instance:*'
)


class SdvServiceBundleMemLeakTest(sdv_base_test.SdvBaseTestClass):
  """SDV Service Bundler Memory Leak test class."""

  def setup_class(self):
    super().setup_class()

  def init_devices(self, device_names: List[str]):
    for device_name in device_names:
      self.devices[device_name] = self.get_device(device_name).adb()

  def init_perfetto_collectors(self, device_names: List[str]):
    local_trace_config_path = os.listdir('./')
    logging.info(f'local_trace_config_path: {local_trace_config_path}')
    for device_name in self.devices.keys():
      self.perfetto_collectors[device_name] = (
          perfetto_collector.PerfettoCollector(
              device=self.devices[device_name],
              config=collector_config.CollectorConfig(
                  config_path=self.get_test_arg(
                      'trace_config'
                  ),  # 'service_bundle_mem_trace.pbtxt', #
              ),
          )
      )

  def start_trace(self, device_names: List[str]):
    for device_name in device_names:
      self.perfetto_collectors[device_name].start_trace(start_trace_delay=5)

  def stop_trace(
      self, device_names: List[str]
  ) -> dict[str, perfetto_trace_processor.PerfettoTraceProcessor]:
    trace_processors = {}
    for device_name in device_names:
      trace_file_path = self.perfetto_collectors[device_name].stop_trace(
          stop_trace_delay=30
      )
      trace_processors[device_name] = (
          perfetto_trace_processor.PerfettoTraceProcessor(trace_file_path)
      )
    return trace_processors

  def setup_test(self):
    super().setup_test()
    logging.info(
        f'{self.get_suite_name()} :: Start test {self.current_test_info.name}'
    )
    self.devices = {}
    self.perfetto_collectors = {}
    self.init_devices(['device1', 'device2'])
    self.init_perfetto_collectors(['device1', 'device2'])
    self.sdv_device_foo = self.devices['device1']
    self.sdv_device_qux = self.devices['device2']

    # shutdown service bundles before the test
    self.manage_service_bundle(
        self.sdv_device_foo,
        SERVICE_BUNDLE_FOO_FQN,
        SERVICE_BUNDLE_SHUTDOWN,
        True,
    )
    self.manage_service_bundle(
        self.sdv_device_qux,
        SERVICE_BUNDLE_QUX_FQN,
        SERVICE_BUNDLE_SHUTDOWN,
        True,
    )

  def teardown_test(self):
    # shutdown service bundles after the test
    self.manage_service_bundle(
        self.sdv_device_foo,
        SERVICE_BUNDLE_FOO_FQN,
        SERVICE_BUNDLE_SHUTDOWN,
        True,
    )
    self.manage_service_bundle(
        self.sdv_device_qux,
        SERVICE_BUNDLE_QUX_FQN,
        SERVICE_BUNDLE_SHUTDOWN,
        True,
    )
    super().teardown_test()
    logging.info(
        f'{self.get_suite_name()} :: Finished test'
        f' {self.current_test_info.name}'
    )

  def teardown_class(self):
    super().teardown_class()

  def set_sys_property(
      self, device: sdv_adb.SdvAdb, sys_property: str, value: str
  ):
    """Set the system property."""
    device.execute_shell_command(f'setprop {sys_property} {value}')

  def manage_service_bundle(
      self,
      device: sdv_adb.SdvAdb,
      service_bundle_name: str,
      command: str,
      ignore_errors: bool = False,
  ):
    """Mangages the service bundle by the provided name."""
    ignore_command = ' || true' if ignore_errors else ''
    device.execute_shell_command(
        'sdv_service_bundle'
        f' {command} local-vm:{service_bundle_name}/instance'
        f' {ignore_command}'
    )

  def create_service_bundle(self, device, service_bundle_name):
    """Creates the service bundle on the provided device.

    Args:
          sdv_device: The device to wait for logcat.
          service_bundle_name: The FQN name of the service bundle to be created.
    """
    self.manage_service_bundle(
        device, service_bundle_name, SERVICE_BUNDLE_CREATE
    )
    # Wait for service bundle to be created.
    # polling.wait_and_verify_expected_logs seems not blocking the test,
    # and the test will go to the next step to clear catlog and fail the
    # waiting.
    time.sleep(2)

  def test_memory_leak_scenario(self):
    self.start_trace(['device1', 'device2'])
    # test steps
    # number of times to start and stop service bundles
    start_stop_repeated_times = int(
        self.get_test_arg('start_stop_repeated_times')
    )
    # message size
    load_size_bytes = self.get_test_arg('load_size_bytes')
    # send messages interval
    interval_ms = int(self.get_test_arg('interval_ms'))
    message_amount = int(self.get_test_arg('message_amount'))
    allowed_working_duration_s = (
        message_amount / 2 * interval_ms / 1000
    )  # seconds

    logging.info(f'set sys properties on devices')
    self.set_sys_property(
        self.sdv_device_foo, SYS_PROP_FOO_INTERVAL, interval_ms
    )
    self.set_sys_property(
        self.sdv_device_foo, SYS_PROP_FOO_LOAD_SIZE, load_size_bytes
    )
    self.set_sys_property(
        self.sdv_device_qux, SYS_PROP_QUX_INTERVAL, interval_ms
    )
    self.set_sys_property(
        self.sdv_device_qux, SYS_PROP_QUX_LOAD_SIZE, load_size_bytes
    )
    self.set_sys_property(
        self.sdv_device_foo, SYS_PROP_FOO_PUB_AMOUNT, message_amount
    )
    self.set_sys_property(
        self.sdv_device_qux, SYS_PROP_QUX_PUB_AMOUNT, message_amount
    )

    logging.info(f'create service bundles on devices')
    self.create_service_bundle(self.sdv_device_foo, SERVICE_BUNDLE_FOO_FQN)
    self.create_service_bundle(self.sdv_device_qux, SERVICE_BUNDLE_QUX_FQN)
    # Wait for service bundle to be created.
    polling.wait_and_verify_expected_logs(
        sdv_device=self.sdv_device_foo,
        grep_text=SERVICE_BUNDLE_FOO_CREATE_MESSAGE,
        assert_msg=f'Message: {SERVICE_BUNDLE_FOO_CREATE_MESSAGE} not found',
    )
    polling.wait_and_verify_expected_logs(
        sdv_device=self.sdv_device_qux,
        grep_text=SERVICE_BUNDLE_QUX_CREATE_MESSAGE,
        assert_msg=f'Message: {SERVICE_BUNDLE_QUX_CREATE_MESSAGE} not found',
    )
    logging.info(f'start and stop service bundles on devices')
    for _ in range(start_stop_repeated_times):
      self.sdv_device_foo.clear_logcat()
      self.sdv_device_qux.clear_logcat()
      # WHEN service bundles are STARTED and work for a while.
      self.manage_service_bundle(
          self.sdv_device_foo, SERVICE_BUNDLE_FOO_FQN, SERVICE_BUNDLE_START
      )
      self.manage_service_bundle(
          self.sdv_device_qux, SERVICE_BUNDLE_QUX_FQN, SERVICE_BUNDLE_START
      )
      # Wait for service bundle to be started n times.
      polling.wait_and_verify_expected_logs(
          sdv_device=self.sdv_device_foo,
          grep_text=SERVICE_BUNDLE_FOO_START_MESSAGE,
          assert_msg=f'Message: {SERVICE_BUNDLE_FOO_START_MESSAGE} not found',
      )
      polling.wait_and_verify_expected_logs(
          sdv_device=self.sdv_device_foo,
          grep_text=FOO_SEND_MESSAGE,
          assert_msg=f'Message: {FOO_SEND_MESSAGE} not found',
      )
      polling.wait_and_verify_expected_logs(
          sdv_device=self.sdv_device_qux,
          grep_text=SERVICE_BUNDLE_QUX_START_MESSAGE,
          assert_msg=f'Message: {SERVICE_BUNDLE_QUX_START_MESSAGE} not found',
      )
      polling.wait_and_verify_expected_logs(
          sdv_device=self.sdv_device_qux,
          grep_text=QUX_SEND_MESSAGE,
          assert_msg=f'Message: {QUX_SEND_MESSAGE} not found',
      )

      time.sleep(
          allowed_working_duration_s
      )  # Wait for service bundles to work for a while.
      self.manage_service_bundle(
          self.sdv_device_foo, SERVICE_BUNDLE_FOO_FQN, SERVICE_BUNDLE_STOP
      )
      self.manage_service_bundle(
          self.sdv_device_qux, SERVICE_BUNDLE_QUX_FQN, SERVICE_BUNDLE_STOP
      )
      polling.wait_and_verify_expected_logs(
          sdv_device=self.sdv_device_foo,
          grep_text=SERVICE_BUNDLE_FOO_STOP_MESSAGE,
          assert_msg=f'Message: {SERVICE_BUNDLE_FOO_STOP_MESSAGE} not found',
      )
      polling.wait_and_verify_expected_logs(
          sdv_device=self.sdv_device_qux,
          grep_text=SERVICE_BUNDLE_QUX_STOP_MESSAGE,
          assert_msg=f'Message: {SERVICE_BUNDLE_QUX_STOP_MESSAGE} not found',
      )

    self.stop_trace(['device1', 'device2'])


if __name__ == '__main__':
  # Start Test Execution Using SDV Test Framework
  sdv_test_runner.run()
