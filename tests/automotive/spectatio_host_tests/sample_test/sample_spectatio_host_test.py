# Copyright 2025 Google LLC

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
