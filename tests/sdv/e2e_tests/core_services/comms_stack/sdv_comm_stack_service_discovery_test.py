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

"""SDV Comms Stack Service Discovery Test

Tests Service Discovery between Two SDV VMs
"""

from mobly import asserts
import enum
import json
import logging

from absl.testing import parameterized
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner


class SdvCommsStackServiceDiscoveryTest(
    sdv_base_test.SdvBaseTestClass, parameterized.TestCase
):

  PUBLISHER_PROCESS_NAME = 'publisher_process'

  STATUS_KEY = 'Status'
  ASSERT_STATUS_OK = 'Ok'
  ASSERT_STATUS_ERROR_MESSAGE = (
      'Actual Status: {actual_status} does not '
      'match Expected Status: {expected_status} '
      'for {command_type} command'
  )

  RESULT_KEY = 'Result'

  SERVICES_KEY = 'services'
  ASSERT_SERVICES_ERROR_MESSAGE = (
      'Expected {expected_number_of_services} Services but '
      'Actual Result: {services} contains {number_of_services} Services'
  )

  SERVICES_FQIN_KEY = 'fqin'
  ASSERT_SERVICE_FQIN_ERROR_MESSAGE = (
      'Actual Service FQIN: {actual_service_fqin} Does Not Match '
      'Expected Service FQIN: {expected_service_fqin} '
  )

  UNIT_NAME_KEY = 'unit_name'
  ASSERT_UNIT_NAME_ERROR_MESSAGE = (
      'Actual Unit Name: {actual_unit_name} Does Not Match '
      'Expected Unit Name: {expected_unit_name} '
  )

  SDV_COMMS_CLIENT_BINARY = '/system/bin/sdv_comms_client_rs'

  class SDV_COMMS_COMMAND(enum.Enum):
    PUBLISH = (
        'publish '
        '--instance-name {instance_name} '
        '--package-name {package_name} '
        '--bundle-name {bundle_name} '
        '--service-unit-type {unit_type} '
        '--service-unit-name {unit_name} '
        '--quantity {quantity} '
        '--interval-msec {interval_msec} '
        '--message-size {message_size} '
        '--queue-size {queue_size}'
    )
    DISCOVER = (
        'discover '
        '--package-name {package_name} '
        '--bundle-name {bundle_name} '
        '--{discover_key} {discover_value}'
    )

  SDV_COMMS_COMMAND_FORMAT = '{client_binary} {command}'

  # Test Parameters

  # Test Arg Keys
  PUBLISHER_INSTANCE_NAME_KEY = 'publisher_instance_name'
  PUBLISHER_INTERVAL_MSEC_KEY = 'publisher_interval_msec'
  PUBLISHER_QUANTITY_KEY = 'publisher_quantity'
  PUBLISHER_MESSAGE_SIZE_KEY = 'publisher_message_size'
  PUBLISHER_QUEUE_SIZE_KEY = 'publisher_queue_size'

  SERVICE_UNIT_NAME_KEY = 'service_unit_name'
  SERVICE_UNIT_TYPE_KEY = 'service_unit_type'
  PACKAGE_NAME_KEY = 'package_name'
  BUNDLE_NAME_KEY = 'bundle_name'

  # Default Values
  DEFAULT_PUBLISHER_INSTANCE_NAME = 'publisher_{test_suffix}'
  DEFAULT_PUBLISHER_INTERVAL_MSEC = 500
  DEFAULT_PUBLISHER_QUANTITY = 10
  DEFAULT_PUBLISHER_MESSAGE_SIZE = 16
  DEFAULT_PUBLISHER_QUEUE_SIZE = 32

  DEFAULT_SERVICE_UNIT_NAME = 'unit_name_{test_suffix}'
  DEFAULT_SERVICE_UNIT_TYPE = 'unit_type_{test_suffix}'
  DEFAULT_PACKAGE_NAME = 'test.google.sdv.comms'
  DEFAULT_BUNDLE_NAME = 'SdvCommsTest'

  # ----------------------------------------------------------------------------
  # Before Class
  # ----------------------------------------------------------------------------
  def setup_class(self):
    super().setup_class()
    logging.info(f'{self.get_suite_name()} :: Setup Class And Get Devices')

    # Get the device using label
    self.sdv_device_1 = self.get_device('device1')
    self.sdv_device_2 = self.get_device('device2')

    self.original_authz_value_device_1 = self.sdv_device_1.adb().prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
    logging.info(
        f'Saving device1 original sdv.authz.enable value: {self.original_authz_value_device_1}'
    )
    self.original_authz_value_device_2 = self.sdv_device_2.adb().prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
    logging.info(
        f'Saving device2 original sdv.authz.enable value: {self.original_authz_value_device_2}'
    )
    self.sdv_device_1.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'false')
    self.sdv_device_2.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'false')

    logging.info(f'{self.get_suite_name()} :: End Class Setup')

  # ----------------------------------------------------------------------------
  # After Class
  # ----------------------------------------------------------------------------
  def teardown_class(self):
    # TODO(b/395067075): Update test to work with authz enabled
    logging.info('Restoring original sdv.authz.enable settings')
    self.sdv_device_1.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.original_authz_value_device_1)
    self.sdv_device_2.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.original_authz_value_device_2)
    super().teardown_class()

  # ----------------------------------------------------------------------------
  # Before Test
  # ----------------------------------------------------------------------------
  def setup_test(self):
    logging.info(f'{self.get_suite_name()} :: Start Test Setup')
    self.sdv_device_1.adb().root_device()
    self.sdv_device_2.adb().root_device()
    logging.info(f'{self.get_suite_name()} :: End Test Setup')

  # ----------------------------------------------------------------------------
  # Tests
  # ----------------------------------------------------------------------------

  # ----------------------------------------------------------------------------
  # Test 1 - Two Devices - Discover Publisher By Name And Type
  # And Validate Non-Empty Services List
  # ----------------------------------------------------------------------------
  @parameterized.named_parameters(
      {
          'testcase_name': 'by_name',
          'test_suffix': 'discover_running_publisher_by_name',
          'discovery_method': 'name',
      },
      {
          'testcase_name': 'by_type',
          'test_suffix': 'discover_running_publisher_by_type',
          'discovery_method': 'type',
      },
  )
  def test_discover_running_publisher(self, test_suffix, discovery_method):
    """Test: Getting Service Unit List ( By Name and Type )

    Number of Devices: 2
    Test Steps:
    1. Create Publisher ( Publisher Running Till End of Test )
    2. Get Units ( By Name and Type )
    3. Test Validation ( Since Publisher is running, services are found )
      3.1. Validate Non-Empty Services List On Device 1
      3.2. Validate Non-Empty Services List On Device 2
    """
    test_params = self._get_test_params_from_test_args(test_suffix)
    # Workaround to keep the publisher running till the end of the test
    test_params[self.PUBLISHER_INTERVAL_MSEC_KEY] = 60000  # 60 seconds

    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Start Test'
    )

    # Create Publisher
    publisher_command = self._create_publisher_command(
        instance_name=test_params[self.PUBLISHER_INSTANCE_NAME_KEY],
        package_name=test_params[self.PACKAGE_NAME_KEY],
        bundle_name=test_params[self.BUNDLE_NAME_KEY],
        unit_type=test_params[self.SERVICE_UNIT_TYPE_KEY],
        unit_name=test_params[self.SERVICE_UNIT_NAME_KEY],
        quantity=test_params[self.PUBLISHER_QUANTITY_KEY],
        interval_msec=test_params[self.PUBLISHER_INTERVAL_MSEC_KEY],
        message_size=test_params[self.PUBLISHER_MESSAGE_SIZE_KEY],
        queue_size=test_params[self.PUBLISHER_QUEUE_SIZE_KEY],
    )
    logging.debug(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Executing '
        'Publisher Command:: <%s> on Device_1:: <%s>',
        publisher_command,
        self.sdv_device_1.adb().get_device_serial(),
    )
    # Execute publisher command in subprocess as it is a blocking command
    self.sdv_device_1.adb().execute_shell_command_in_subprocess(
        self.PUBLISHER_PROCESS_NAME, publisher_command
    )

    # Service discovery uses vm_name defined using device property
    # 'ro.boot.sdv.instance_name' when creating the FQIN so it should
    # always be available as it is a required property for SDV devices.
    # It can be retrieved from the device where the publisher is started.
    vm_name = self.sdv_device_1.adb().prop.get(SdvDeviceProperty.INSTANCE_NAME)

    expected_service_fqin = (
        '{vm_name}:{package_name}.{bundle_name}/{publisher_instance_name}'
        .format(
            vm_name=vm_name,
            package_name=test_params[self.PACKAGE_NAME_KEY],
            bundle_name=test_params[self.BUNDLE_NAME_KEY],
            publisher_instance_name=test_params[
                self.PUBLISHER_INSTANCE_NAME_KEY
            ],
        )
    )

    # Discover Units Command
    discover_key = (
        self.SERVICE_UNIT_NAME_KEY
        if discovery_method == 'name'
        else self.SERVICE_UNIT_TYPE_KEY
    )
    discover_command = self._create_discover_command(
        package_name=test_params[self.PACKAGE_NAME_KEY],
        bundle_name=test_params[self.BUNDLE_NAME_KEY],
        discover_key=self._format_string_to_dash_case(discover_key),
        discover_value=test_params[discover_key],
    )

    # Get Units On Device 1
    logging.debug(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Executing '
        'Discover Command: <%s> on Device_1:: <%s>',
        discover_command,
        self.sdv_device_1.adb().get_device_serial(),
    )
    discover_result = self.sdv_device_1.adb().execute_shell_command(
        discover_command
    )
    logging.debug(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Discover'
        ' Result: <%s> on Device_1:: <%s>',
        discover_result,
        self.sdv_device_1.adb().get_device_serial(),
    )

    # Validate Discover Result
    discover_result_json = json.loads(discover_result)

    # Validate Status
    asserts.assert_equal(
        self.ASSERT_STATUS_OK,
        discover_result_json[self.STATUS_KEY],
        self.ASSERT_STATUS_ERROR_MESSAGE.format(
            actual_status=discover_result_json[self.STATUS_KEY],
            expected_status=self.ASSERT_STATUS_OK,
            command_type=self.SDV_COMMS_COMMAND.DISCOVER.name,
        ),
    )

    # Validate Number Of Services
    expected_number_of_services = 1
    asserts.assert_equal(
        expected_number_of_services,
        len(discover_result_json[self.RESULT_KEY][self.SERVICES_KEY]),
        self.ASSERT_SERVICES_ERROR_MESSAGE.format(
            expected_number_of_services=expected_number_of_services,
            services=discover_result_json[self.RESULT_KEY][self.SERVICES_KEY],
            number_of_services=len(
                discover_result_json[self.RESULT_KEY][self.SERVICES_KEY]
            ),
        ),
    )

    # Validate Service
    actual_service_fqin = discover_result_json[self.RESULT_KEY][
        self.SERVICES_KEY
    ][0][self.SERVICES_FQIN_KEY]
    asserts.assert_equal(
        expected_service_fqin,
        actual_service_fqin,
        self.ASSERT_SERVICE_FQIN_ERROR_MESSAGE.format(
            actual_service_fqin=actual_service_fqin,
            expected_service_fqin=expected_service_fqin,
        ),
    )

    # Validate Unit Name
    actual_unit_name = discover_result_json[self.RESULT_KEY][self.SERVICES_KEY][
        0
    ][self.UNIT_NAME_KEY]
    asserts.assert_equal(
        test_params[self.SERVICE_UNIT_NAME_KEY],
        actual_unit_name,
        self.ASSERT_UNIT_NAME_ERROR_MESSAGE.format(
            actual_unit_name=actual_unit_name,
            expected_unit_name=test_params[self.SERVICE_UNIT_NAME_KEY],
        ),
    )

    # Get Units On Device 2
    logging.debug(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Executing '
        'Discover Command: <%s> on Device_2:: <%s>',
        discover_command,
        self.sdv_device_2.adb().get_device_serial(),
    )
    discover_result = self.sdv_device_2.adb().execute_shell_command(
        discover_command
    )
    logging.debug(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Discover'
        ' Result: <%s> on Device_2:: <%s>',
        discover_result,
        self.sdv_device_2.adb().get_device_serial(),
    )

    # Validate Discover Result
    discover_result_json = json.loads(discover_result)

    # Validate Status
    asserts.assert_equal(
        self.ASSERT_STATUS_OK,
        discover_result_json[self.STATUS_KEY],
        self.ASSERT_STATUS_ERROR_MESSAGE.format(
            actual_status=discover_result_json[self.STATUS_KEY],
            expected_status=self.ASSERT_STATUS_OK,
            command_type=self.SDV_COMMS_COMMAND.DISCOVER.name,
        ),
    )

    # Validate Number Of Services
    expected_number_of_services = 1
    asserts.assert_equal(
        expected_number_of_services,
        len(discover_result_json[self.RESULT_KEY][self.SERVICES_KEY]),
        self.ASSERT_SERVICES_ERROR_MESSAGE.format(
            expected_number_of_services=expected_number_of_services,
            services=discover_result_json[self.RESULT_KEY][self.SERVICES_KEY],
            number_of_services=len(
                discover_result_json[self.RESULT_KEY][self.SERVICES_KEY]
            ),
        ),
    )

    # Validate Service
    actual_service_fqin = discover_result_json[self.RESULT_KEY][
        self.SERVICES_KEY
    ][0][self.SERVICES_FQIN_KEY]
    asserts.assert_equal(
        expected_service_fqin,
        actual_service_fqin,
        self.ASSERT_SERVICE_FQIN_ERROR_MESSAGE.format(
            actual_service_fqin=actual_service_fqin,
            expected_service_fqin=expected_service_fqin,
        ),
    )

    # Validate Unit Name
    actual_unit_name = discover_result_json[self.RESULT_KEY][self.SERVICES_KEY][
        0
    ][self.UNIT_NAME_KEY]
    asserts.assert_equal(
        test_params[self.SERVICE_UNIT_NAME_KEY],
        actual_unit_name,
        self.ASSERT_UNIT_NAME_ERROR_MESSAGE.format(
            actual_unit_name=actual_unit_name,
            expected_unit_name=test_params[self.SERVICE_UNIT_NAME_KEY],
        ),
    )

    # Terminate Publisher Process
    self.sdv_device_1.adb().terminate_subprocess(self.PUBLISHER_PROCESS_NAME)

    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: End Test'
    )

  # ----------------------------------------------------------------------------
  # Test 2 - Two Devices - Discover Publisher By Name And Type
  # And Validate Empty Services List
  # ----------------------------------------------------------------------------
  @parameterized.named_parameters(
      {
          'testcase_name': 'by_name',
          'test_suffix': 'discover_exited_publisher_by_name',
          'discovery_method': 'name',
      },
      {
          'testcase_name': 'by_type',
          'test_suffix': 'discover_exited_publisher_by_type',
          'discovery_method': 'type',
      },
  )
  def test_discover_exited_publisher(self, test_suffix, discovery_method):
    """Test: Getting Service Unit List ( By Name and Type )

    Number of Devices: 2
    Test Steps:
    1. Create Publisher ( Run Publisher and Wait for it to Exit  )
    2. Get Units ( By Name and Type )
    3. Test Validation ( Since Publisher exits, services are not found )
      3.1. Validate Empty Services List On Device 1
      3.2. Validate Empty Services List On Device 2
    """
    test_params = self._get_test_params_from_test_args(test_suffix)

    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Start Test'
    )

    # Create Publisher
    publisher_command = self._create_publisher_command(
        instance_name=test_params[self.PUBLISHER_INSTANCE_NAME_KEY],
        package_name=test_params[self.PACKAGE_NAME_KEY],
        bundle_name=test_params[self.BUNDLE_NAME_KEY],
        unit_type=test_params[self.SERVICE_UNIT_TYPE_KEY],
        unit_name=test_params[self.SERVICE_UNIT_NAME_KEY],
        quantity=test_params[self.PUBLISHER_QUANTITY_KEY],
        interval_msec=test_params[self.PUBLISHER_INTERVAL_MSEC_KEY],
        message_size=test_params[self.PUBLISHER_MESSAGE_SIZE_KEY],
        queue_size=test_params[self.PUBLISHER_QUEUE_SIZE_KEY],
    )
    logging.debug(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Executing '
        'Publisher Command:: <%s> on Device_1:: <%s>',
        publisher_command,
        self.sdv_device_1.adb().get_device_serial(),
    )
    # Execute publisher command
    publisher_result = self.sdv_device_1.adb().execute_shell_command(
        publisher_command
    )
    logging.debug(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Publisher'
        ' Result: <%s> on Device_1:: <%s>',
        publisher_result,
        self.sdv_device_1.adb().get_device_serial()
    )

    # Validate Publisher Result
    publisher_result_json = json.loads(publisher_result)

    asserts.assert_equal(
        self.ASSERT_STATUS_OK,
        publisher_result_json[self.STATUS_KEY],
        self.ASSERT_STATUS_ERROR_MESSAGE.format(
            actual_status=publisher_result_json[self.STATUS_KEY],
            expected_status=self.ASSERT_STATUS_OK,
            command_type=self.SDV_COMMS_COMMAND.PUBLISH.name,
        ),
    )

    # Discover Units Command
    discover_key = (
        self.SERVICE_UNIT_NAME_KEY
        if discovery_method == 'name'
        else self.SERVICE_UNIT_TYPE_KEY
    )
    discover_command = self._create_discover_command(
        package_name=test_params[self.PACKAGE_NAME_KEY],
        bundle_name=test_params[self.BUNDLE_NAME_KEY],
        discover_key=self._format_string_to_dash_case(discover_key),
        discover_value=test_params[discover_key],
    )

    # Get Units On Device 1
    logging.debug(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Executing '
        'Discover Command: <%s> on Device_1:: <%s>',
        discover_command,
        self.sdv_device_1.adb().get_device_serial(),
    )
    discover_result = self.sdv_device_1.adb().execute_shell_command(
        discover_command
    )
    logging.debug(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Discover'
        ' Result: <%s> on Device_1:: <%s>',
        discover_result,
        self.sdv_device_1.adb().get_device_serial(),
    )

    # Validate Discover Result
    discover_result_json = json.loads(discover_result)

    # Validate Status
    asserts.assert_equal(
        self.ASSERT_STATUS_OK,
        discover_result_json[self.STATUS_KEY],
        self.ASSERT_STATUS_ERROR_MESSAGE.format(
            actual_status=discover_result_json[self.STATUS_KEY],
            expected_status=self.ASSERT_STATUS_OK,
            command_type=self.SDV_COMMS_COMMAND.DISCOVER.name,
        ),
    )

    # Validate Number Of Services
    expected_number_of_services = 0
    asserts.assert_equal(
        expected_number_of_services,
        len(discover_result_json[self.RESULT_KEY][self.SERVICES_KEY]),
        self.ASSERT_SERVICES_ERROR_MESSAGE.format(
            expected_number_of_services=expected_number_of_services,
            services=discover_result_json[self.RESULT_KEY][self.SERVICES_KEY],
            number_of_services=len(
                discover_result_json[self.RESULT_KEY][self.SERVICES_KEY]
            ),
        ),
    )

    # Get Units On Device 2
    logging.debug(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Executing '
        'Discover Command: <%s> on Device_2:: <%s>',
        discover_command,
        self.sdv_device_2.adb().get_device_serial(),
    )
    discover_result = self.sdv_device_2.adb().execute_shell_command(
        discover_command
    )
    logging.debug(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: Discover'
        ' Result: <%s> on Device_2:: <%s>',
        discover_result,
        self.sdv_device_2.adb().get_device_serial(),
    )

    # Validate Discover Result
    discover_result_json = json.loads(discover_result)

    # Validate Status
    asserts.assert_equal(
        self.ASSERT_STATUS_OK,
        discover_result_json[self.STATUS_KEY],
        self.ASSERT_STATUS_ERROR_MESSAGE.format(
            actual_status=discover_result_json[self.STATUS_KEY],
            expected_status=self.ASSERT_STATUS_OK,
            command_type=self.SDV_COMMS_COMMAND.DISCOVER.name,
        ),
    )

    # Validate Number Of Services
    expected_number_of_services = 0
    asserts.assert_equal(
        expected_number_of_services,
        len(discover_result_json[self.RESULT_KEY][self.SERVICES_KEY]),
        self.ASSERT_SERVICES_ERROR_MESSAGE.format(
            expected_number_of_services=expected_number_of_services,
            services=discover_result_json[self.RESULT_KEY][self.SERVICES_KEY],
            number_of_services=len(
                discover_result_json[self.RESULT_KEY][self.SERVICES_KEY]
            ),
        ),
    )

    logging.info(
        f'{self.get_suite_name()}#{self.current_test_info.name} :: End Test'
    )

  # ----------------------------------------------------------------------------
  # Helper Methods
  # ----------------------------------------------------------------------------
  def _get_test_arg(self, test_arg_key, default_value):
    test_arg_value = default_value
    try:
      test_arg_value = self.get_test_arg(test_arg_key)
    except Exception:
      logging.warning(
          f'{self.get_suite_name()} :: {test_arg_key} not found in test args.'
          f' Using default value: <{default_value}>'
      )
    return test_arg_value

  def _format_string_to_camel_case(self, string):
    return ''.join(x.capitalize() for x in string.lower().split('_'))

  def _format_string_to_dash_case(self, string):
    return string.lower().replace('_', '-')

  def _get_test_params_from_test_args(self, test_suffix):
    test_params = {}
    test_params[self.PUBLISHER_INSTANCE_NAME_KEY] = self._get_test_arg(
        self.PUBLISHER_INSTANCE_NAME_KEY,
        self._format_string_to_dash_case(
            self.DEFAULT_PUBLISHER_INSTANCE_NAME.format(test_suffix=test_suffix)
        ),
    )
    test_params[self.PACKAGE_NAME_KEY] = self._get_test_arg(
        self.PACKAGE_NAME_KEY,
        self.DEFAULT_PACKAGE_NAME,
    )
    test_params[self.BUNDLE_NAME_KEY] = self._get_test_arg(
        self.BUNDLE_NAME_KEY,
        self.DEFAULT_BUNDLE_NAME,
    )
    test_params[self.SERVICE_UNIT_TYPE_KEY] = self._get_test_arg(
        self.SERVICE_UNIT_TYPE_KEY,
        self._format_string_to_camel_case(
            self.DEFAULT_SERVICE_UNIT_TYPE.format(test_suffix=test_suffix)
        ),
    )
    test_params[self.SERVICE_UNIT_NAME_KEY] = self._get_test_arg(
        self.SERVICE_UNIT_NAME_KEY,
        self._format_string_to_dash_case(
            self.DEFAULT_SERVICE_UNIT_NAME.format(test_suffix=test_suffix)
        ),
    )
    test_params[self.PUBLISHER_QUANTITY_KEY] = self._get_test_arg(
        self.PUBLISHER_QUANTITY_KEY, self.DEFAULT_PUBLISHER_QUANTITY
    )
    test_params[self.PUBLISHER_INTERVAL_MSEC_KEY] = self._get_test_arg(
        self.PUBLISHER_INTERVAL_MSEC_KEY, self.DEFAULT_PUBLISHER_INTERVAL_MSEC
    )
    test_params[self.PUBLISHER_MESSAGE_SIZE_KEY] = self._get_test_arg(
        self.PUBLISHER_MESSAGE_SIZE_KEY, self.DEFAULT_PUBLISHER_MESSAGE_SIZE
    )
    test_params[self.PUBLISHER_QUEUE_SIZE_KEY] = self._get_test_arg(
        self.PUBLISHER_QUEUE_SIZE_KEY, self.DEFAULT_PUBLISHER_QUEUE_SIZE
    )
    return test_params

  def _create_publisher_command(
      self,
      instance_name,
      package_name,
      bundle_name,
      unit_type,
      unit_name,
      quantity,
      interval_msec,
      message_size,
      queue_size,
  ):
    publisher_command = self.SDV_COMMS_COMMAND_FORMAT.format(
        client_binary=self.SDV_COMMS_CLIENT_BINARY,
        command=self.SDV_COMMS_COMMAND.PUBLISH.value.format(
            instance_name=instance_name,
            package_name=package_name,
            bundle_name=bundle_name,
            unit_type=unit_type,
            unit_name=unit_name,
            quantity=quantity,
            interval_msec=interval_msec,
            message_size=message_size,
            queue_size=queue_size,
        ),
    )
    return publisher_command

  def _create_discover_command(
      self,
      package_name,
      bundle_name,
      discover_key,
      discover_value,
  ):
    discover_command = self.SDV_COMMS_COMMAND_FORMAT.format(
        client_binary=self.SDV_COMMS_CLIENT_BINARY,
        command=self.SDV_COMMS_COMMAND.DISCOVER.value.format(
            package_name=package_name,
            bundle_name=bundle_name,
            discover_key=discover_key,
            discover_value=discover_value,
        ),
    )
    return discover_command


# ----------------------------------------------------------------------------
# Main
# ----------------------------------------------------------------------------
if __name__ == '__main__':
  # Start Test Execution Using SDV Test Framework ( STF )
  sdv_test_runner.run()
