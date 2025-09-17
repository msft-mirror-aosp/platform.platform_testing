# Copyright 2025 Google LLC

import logging

from absl.testing import parameterized  # needed only for parameterized tests

from spectatio_host_tf.core import test_base
from spectatio_host_tf.core import test_runner


class SampleSpectatioHostTest(
    test_base.SpectatioHostBaseTestClass,
    parameterized.TestCase  # needed only for parameterized tests
):
  """
    Sample Spectatio Host-Side Test.
  """

  def setup_class(self):
    super().setup_class()
    self.test_device = self.get_device('device1')

  def setup_test(self):
    super().setup_test()

  def test_device_serial(self):
    logging.info(f'{self.get_test_class_name()}: Start {self.current_test_info.name} Test')
    device_serial = self.test_device.adb().get_device_serial()
    expected_device_serial = '0.0.0.0:6520'
    self.asserts.assert_equal(device_serial, expected_device_serial, 'Device Serial Does Not Match')
    logging.info(f'{self.get_test_class_name()}: End {self.current_test_info.name} Test')

  def test_instance_name(self):
    logging.info(f'{self.get_test_class_name()}: Start {self.current_test_info.name} Test')
    instance_name = self.test_device.adb().execute_shell_command('getprop ro.boot.sdv.instance_name', raise_exception=False)
    expected_instance_name = 'instance1'
    self.asserts.assert_equal(instance_name, expected_instance_name, 'Instance Name Does Not Match')
    logging.info(f'{self.get_test_class_name()}: End {self.current_test_info.name} Test')

  @parameterized.named_parameters(
      {
          'testcase_name': 'available',
          'arg_key': 'some_key',
          'arg_value': 'some_value',
      },
      {
          'testcase_name': 'not_available',
          'arg_key': 'some_key_missing',
          'arg_value': None,
      },
  )
  def test_test_arg(self, arg_key, arg_value):
    logging.info(f'{self.get_test_class_name()}: Start {self.current_test_info.name} Test')
    test_arg_value = self.get_test_arg(arg_key, raise_exception=False)
    self.asserts.assert_equal(test_arg_value, arg_value, 'Test Arg Exist')
    logging.info(f'{self.get_test_class_name()}: End {self.current_test_info.name} Test')

  def teardown_test(self):
    super().teardown_test()

  def teardown_class(self):
    super().teardown_class()


if __name__ == '__main__':
  test_runner.run()