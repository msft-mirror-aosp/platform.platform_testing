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

import logging

from absl.testing import parameterized

from spectatio_host_tf.core import test_base
from spectatio_host_tf.core import test_runner


class SampleSpectatioHostTest(
    test_base.SpectatioHostBaseTestClass, parameterized.TestCase
):
  """
   A sample test demonstrating the features of the Spectatio framework.
  """

  def test_device_serial(self):
    """
     Verifies the device serial number.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')
    expected_device_serial = '0.0.0.0:6520'
    self.asserts.assert_equal(
        self.device1.adb.serial,
        expected_device_serial,
        'Device serial does not match.',
    )

  def test_instance_name(self):
    """
     Verifies the device's instance name property.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')
    instance_name = self.device1.adb.execute_shell_command(
        'getprop ro.boot.sdv.instance_name', raise_exception=False
    )
    expected_instance_name = 'instance1'
    self.asserts.assert_equal(
        instance_name, expected_instance_name, 'Instance name does not match.'
    )

  @parameterized.named_parameters(
      {
          'testcase_name': 'argument_exists',
          'arg_key': 'some_key',
          'expected_value': 'some_value'
      },
      {
          'testcase_name': 'argument_missing',
          'arg_key': 'some_key_missing',
          'expected_value': None
      },
  )
  def test_get_test_arg(self, arg_key, expected_value):
    """
     Tests the retrieval of test arguments.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')
    actual_value = self.get_test_arg(arg_key)
    self.asserts.assert_equal(
        actual_value, expected_value, 'Test argument value is incorrect.'
    )


if __name__ == '__main__':
  test_runner.run()
