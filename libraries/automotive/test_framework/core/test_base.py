# Copyright 2025 Google LLC

import logging
import re

from mobly import asserts
from mobly import base_test
from mobly.controllers import android_device

from spectatio_host_tf.core import test_device
from spectatio_host_tf.utils import test_arg_util


class SpectatioHostBaseTestClass(base_test.BaseTestClass):
  """
  Specatio Host-Side Automotive Test Framework Base Test Class

  Each test must extend SpectatioHostBaseTestClass

  Example usage:

      class TestClassName(test_base.SpectatioHostBaseTestClass):
          setup_class
          setup_test
          teardown_test
          test_sometest
  """
  __LOG_TAG = 'SpectatioHostBaseTestClass'

  __DEFAULT_NUMBER_OF_DEVICES = 1
  __NUMBER_OF_DEVICES = 'number_of_devices'

  def __get_instance_name(self, device):
    return device.adb().execute_shell_command(
        'getprop ro.boot.sdv.instance_name', raise_exception=False
    )

  def __get_instance_number(self, instance_name):
    return int(instance_name[len('instance') :])

  def __is_instance_name_in_valid_format(self, instance_name):
    # Check for instance name format: instance{number}
    return re.search('^instance[0-9]+$', instance_name)

  def __is_instance_number_valid(self, instance_name):
    instance_number = self.__get_instance_number(instance_name)
    return instance_number > 0 and instance_number <= self.__num_of_devices

  def __is_device_tag_update_needed(self, instance_name, device_number):
    # Check if instance_name exist
    # and if instance_name is in the format of instance{number}
    # and if the instance id is between 1 and self.__num_of_devices,
    # and if the instance id does not match current device number
    # then update the device tag
    return (
        instance_name
        and self.__is_instance_name_in_valid_format(instance_name)
        and self.__is_instance_number_valid(instance_name)
        and self.__get_instance_number(instance_name) != device_number
    )

  def __get_device_list(self):
    device_list = {}
    default_device_list = {}

    for index in range(self.__num_of_devices):
      device_tag = f'device{index+1}'

      device = test_device.TestDevice(
          android_device.get_device(self.__ads, label=device_tag)
      )

      default_device_list[device_tag] = device

      instance_name = self.__get_instance_name(device)
      """
        It is not possible to specify the device in a particular order
        for atest. Also, sometimes the order of devices is not maintained
        on CI/CD which causes the test to fail, resulting in flakiness.
        To avoid this, we use the instance name on the device to assign
        the devices in correct order for the test. If the instance name is
        not provided or is not in the correct format, we will use the
        default order.
      """
      if self.__is_device_tag_update_needed(instance_name, index + 1):
        device_tag = f'device{self.__get_instance_number(instance_name)}'

      device_list[device_tag] = device

    # Fail Safe or Fallback: This happens when same instance name is
    # assigned to multiple devices or when the instance names are not
    # assigned correctly. e.g. device1->instance2, device2->no instance name
    if len(device_list) != self.__num_of_devices:
      device_list.clear()
      assigned_devices = [{
          tag: device.adb().get_device_serial()
          for tag, device in default_device_list.items()
      }]
      logging.info(f'{self.__LOG_TAG}: Assigned devices :: {assigned_devices}')
      return default_device_list

    default_device_list.clear()
    assigned_devices = [{
        tag: device.adb().get_device_serial()
        for tag, device in device_list.items()
    }]
    logging.info(f'{self.__LOG_TAG}: Assigned devices :: {assigned_devices}')
    return device_list

  def __clear_all_devices(self):
    for _, device in self.__device_list.items():
      device.adb().remove_all_temp_files()
      device.adb().terminate_all_subprocesses()

  def setup_class(self):
    logging.info('%s: Start Base Test Class Setup', self.__LOG_TAG)

    self.__test_class_name = self.__class__.__name__

    # Get and Register Devices for Test Execution
    self.__num_of_devices = self.__DEFAULT_NUMBER_OF_DEVICES
    if self.__NUMBER_OF_DEVICES in self.user_params:
      self.__num_of_devices = self.user_params[self.__NUMBER_OF_DEVICES]
    else:
      logging.warning(
          '%s: %s is not in testbed config. Using default value: %d',
          self.__LOG_TAG,
          self.__NUMBER_OF_DEVICES,
          self.__DEFAULT_NUMBER_OF_DEVICES,
      )

    logging.info(
        '%s: Registering %d device(s) for test execution',
        self.__LOG_TAG,
        self.__num_of_devices
    )
    self.__ads = self.register_controller(
        android_device, min_number=self.__num_of_devices
    )

    logging.info(
        '%s: Get the list of devices for test execution',
        self.__LOG_TAG
    )
    self.__device_list = self.__get_device_list()

    logging.info(f'{self.__LOG_TAG}: Update Device Logcat Configuration')
    for device in self.__device_list.values():
      device.update_logcat_config_to_verbose_and_persist()

    logging.info(f'{self.__LOG_TAG}: Read And Parse Test Arguments')
    self.__test_args = test_arg_util.parse_test_args()

    logging.info('Initializing Asserts')
    self.__asserts = asserts

    logging.info(f'{self.__LOG_TAG}: End Base Test Class Setup')

  def setup_test(self):
    logging.info(f'{self.__LOG_TAG}: Start Test Setup')
    self.__clear_all_devices()
    logging.info(f'{self.__LOG_TAG}: End Test Setup')

  def teardown_test(self):
    logging.info(f'{self.__LOG_TAG}: Start Test Teardown')
    self.__clear_all_devices()
    logging.info(f'{self.__LOG_TAG}: End Test Teardown')

  def get_device(self, device_label):
    if device_label not in self.__device_list:
      raise Exception(f'Device with label {device_label} not available.')
    return self.__device_list[device_label]

  def get_test_arg(self, test_arg_name, raise_exception=True):
    if test_arg_name in self.__test_args:
      return self.__test_args[test_arg_name]

    if raise_exception:
      raise Exception(f'Test argument {test_arg_name} not available.')

    return None

  @property
  def asserts(self):
    return self.__asserts

  def get_test_class_name(self):
    return self.__test_class_name
