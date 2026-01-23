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

"""SDV Service Discovery Test"""
from mobly import asserts
import logging
import random
import string

from sdv_service_discovery_test_client.client import service_discovery_client
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvServiceDiscoveryTest(sdv_base_test.SdvBaseTestClass):

    ASSERT_MESSAGE_FORMAT = (
        'Actual Result :: <{actual_result}> does not contain Expected Result ::'
        ' <{expected_result}>'
    )

    # Test Parameters
    SDV_PACKAGE_NAME = 'com.sdv.google.test.e2e.mobly'
    SDV_BUNDLE_NAME = 'SdvServiceDiscoveryTest'
    SDV_INSTANCE_NAME = 'main'
    UNIT_TYPE_NAME = 'ServiceDiscoveryTestUnitType'
    SERVICE_UNIT_NAME = 'service-discovery-test-service-unit'
    APP_METADATA_VERSION = 1
    APP_METADATA_VALUE_HOLDER = 'app_metadata_{test_name}:1'
    TRANSPORT_METADATA_VALUE_HOLDER = 'transport_metadata_{test_name}:1'

    def __generate_random_public_key(self):
        key_length = 32
        public_key = ''.join(random.choices(string.digits, k=key_length))
        return public_key

    def __get_port(self, test_arg_key, default_value):
        port = default_value
        try:
            port = self.get_test_arg(test_arg_key)
        except Exception:
            logging.warning(
                f'{self.get_suite_name()} :: {test_arg_key} not found in test'
                f' args. Using default value: <{default_value}>'
            )
        return port

    def setup_class(self):
        super().setup_class()
        self.sdv_device_1 = self.get_device('device1')
        self.sdv_device_2 = self.get_device('device2')
        self.device_port = self.__get_port('device_port', default_value='31425')
        self.host_port_device_1 = self.__get_port(
            'host_port_device_1', default_value='31425'
        )
        self.host_port_device_2 = self.__get_port(
            'host_port_device_2', default_value='31426'
        )

    def setup_test(self):
        self.client_1 = service_discovery_client.SdvServiceDiscoveryClient(
            self.sdv_device_1,
            host_port=self.host_port_device_1,
            device_port=self.device_port,
        )
        self.client_2 = service_discovery_client.SdvServiceDiscoveryClient(
            self.sdv_device_2,
            host_port=self.host_port_device_2,
            device_port=self.device_port,
        )

        # TODO(b/395067075): Update test to work with authz enabled
        self.original_authz_value_device_1 = self.sdv_device_1.adb().execute_shell_command(
            'getprop sdv.authz.enable'
        )
        logging.info(
            f'Saving device 1 original sdv.authz.enable value: {self.original_authz_value_device_1}'
        )
        self.original_authz_value_device_2 = self.sdv_device_2.adb().execute_shell_command(
            'getprop sdv.authz.enable'
        )
        logging.info(
            f'Saving device 2 original sdv.authz.enable value: {self.original_authz_value_device_2}'
        )
        self.sdv_device_1.adb().execute_shell_command('setprop sdv.authz.enable false')
        self.sdv_device_2.adb().execute_shell_command('setprop sdv.authz.enable false')

    def teardown_test(self):
        # TODO(b/395067075): Update test to work with authz enabled
        logging.info('Restoring original sdv.authz.enable settings')
        self.sdv_device_1.adb().execute_shell_command(
            f'setprop sdv.authz.enable {self.original_authz_value_device_1}'
        )
        self.sdv_device_2.adb().execute_shell_command(
            f'setprop sdv.authz.enable {self.original_authz_value_device_2}'
        )

    # -----------------------------------------------------------------------------------------------
    # Test - One Device - Get Service Unit By Type
    # -----------------------------------------------------------------------------------------------
    def test_one_device_get_service_unit_by_type(self):
        logging.info(
            f'{self.get_suite_name()} :: Start Test'
            f' {self.current_test_info.name}'
        )
        # 1. Create Identity
        generated_public_key = self.__generate_random_public_key()
        client_response = self.client_1.create_identity(
            public_key=generated_public_key,
            sdv_vm_name='',  # Empty String, It takes the name for VM Properties,
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            service_instance_name=self.SDV_INSTANCE_NAME,
        )
        expected_result = 'Create Identity Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        service_identity = client_response[len(expected_result) + 1 : -1]
        # 2. Register Service Unit
        client_response = self.client_1.register_service_unit(
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            type_name=self.UNIT_TYPE_NAME,
            app_metadata_version=self.APP_METADATA_VERSION,
            app_metadata_value_holder=self.APP_METADATA_VALUE_HOLDER.format(
                test_name=self.current_test_info.name
            ),
            service_unit_name=self.SERVICE_UNIT_NAME,
        )
        expected_result = 'Register Service Unit Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # Check Register Service Unit Response Contains Service Identity
        asserts.assert_in(
            service_identity,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=service_identity
            ),
        )
        # 3. Get Service Units By Type - Empty List Because Metadata Not Added
        client_response = self.client_1.get_service_units_by_type(
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            type_name=self.UNIT_TYPE_NAME,
        )
        expected_result = 'Get Service Units By Type Result: Ok([])'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # 4. Add Transport Metadata
        client_response = self.client_1.add_transport_metadata(
            transport_metadata_value_holder=self.TRANSPORT_METADATA_VALUE_HOLDER.format(
                test_name=self.current_test_info.name
            )
        )
        expected_result = 'Add Transport Metadata Result: Ok(())'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # 5. Get Service Units By Type - Non-Empty List Because Metadata Added
        client_response = self.client_1.get_service_units_by_type(
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            type_name=self.UNIT_TYPE_NAME,
        )
        expected_result = 'Get Service Units By Type Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # Check List Of Service Units Not Empty
        unexpected_result = 'Get Service Units By Type Result: Ok([])'
        asserts.assert_true(
            (client_response != unexpected_result),
            f'Actual Result :: <{client_response}> matches unexpected result ::'
            f' <{unexpected_result}>',
        )
        # Check List Of Service Units Contains Service Identity
        asserts.assert_in(
            service_identity,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=service_identity
            ),
        )
        logging.info(
            f'{self.get_suite_name()} :: End Test {self.current_test_info.name}'
        )

    # -----------------------------------------------------------------------------------------------
    # Test - One Device - Get Service Unit By Name
    # -----------------------------------------------------------------------------------------------
    def test_one_device_get_service_unit_by_name(self):
        logging.info(
            f'{self.get_suite_name()} :: Start Test'
            f' {self.current_test_info.name}'
        )
        # 1. Create Identity
        generated_public_key = self.__generate_random_public_key()
        client_response = self.client_1.create_identity(
            public_key=generated_public_key,
            sdv_vm_name='',  # Empty String, It takes the name for VM Properties
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            service_instance_name=self.SDV_INSTANCE_NAME,
        )
        expected_result = 'Create Identity Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        service_identity = client_response[len(expected_result) + 1 : -1]
        # 2. Register Service Unit
        client_response = self.client_1.register_service_unit(
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            type_name=self.UNIT_TYPE_NAME,
            app_metadata_version=self.APP_METADATA_VERSION,
            app_metadata_value_holder=self.APP_METADATA_VALUE_HOLDER.format(
                test_name=self.current_test_info.name
            ),
            service_unit_name=self.SERVICE_UNIT_NAME,
        )
        expected_result = 'Register Service Unit Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # Check Register Service Unit Response Contains Service Identity
        asserts.assert_in(
            service_identity,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=service_identity
            ),
        )
        # 3. Get Service Units By Name - Empty List Because Metadata Not Added
        client_response = self.client_1.get_service_units_by_name(
            sdv_vm_name='',  # Empty String, Optional
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            service_unit_name='',  # Empty String, Optional
        )
        expected_result = 'Get Service Units By Name Result: Ok([])'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # 4. Add Transport Metadata
        client_response = self.client_1.add_transport_metadata(
            transport_metadata_value_holder=self.TRANSPORT_METADATA_VALUE_HOLDER.format(
                test_name=self.current_test_info.name
            )
        )
        expected_result = 'Add Transport Metadata Result: Ok(())'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # 5. Get Service Units By Name - Non-Empty List Because Metadata Added
        client_response = self.client_1.get_service_units_by_name(
            sdv_vm_name='',  # Empty String, Optional
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            service_unit_name='',  # Empty String, Optional
        )
        expected_result = 'Get Service Units By Name Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # Check List Of Service Units Not Empty
        unexpected_result = 'Get Service Units By Name Result: Ok([])'
        asserts.assert_true(
            (client_response != unexpected_result),
            f'Actual Result :: <{client_response}> matches unexpected result ::'
            f' <{unexpected_result}>',
        )
        # Check List Of Service Units Contains Service Identity
        asserts.assert_in(
            service_identity,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=service_identity
            ),
        )
        logging.info(
            f'{self.get_suite_name()} :: End Test {self.current_test_info.name}'
        )

    # -----------------------------------------------------------------------------------------------
    # Test - Two Devices - Get Service Unit By Type
    # -----------------------------------------------------------------------------------------------
    def test_two_devices_get_service_unit_by_type(self):
        logging.info(
            f'{self.get_suite_name()} :: Start Test'
            f' {self.current_test_info.name}'
        )
        # 1. Create Identity
        generated_public_key = self.__generate_random_public_key()
        # On Device 1
        client_response = self.client_1.create_identity(
            public_key=generated_public_key,
            sdv_vm_name='',  # Empty String, It takes the name for VM Properties
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            service_instance_name=self.SDV_INSTANCE_NAME,
        )
        expected_result = 'Create Identity Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        service_identity = client_response[len(expected_result) + 1 : -1]
        # On Device 2
        client_response = self.client_2.create_identity(
            public_key=generated_public_key,
            sdv_vm_name='',  # Empty String, It takes the name for VM Properties
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            service_instance_name=self.SDV_INSTANCE_NAME,
        )
        expected_result = 'Create Identity Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # 2. Register Service Unit On Device 1
        client_response = self.client_1.register_service_unit(
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            type_name=self.UNIT_TYPE_NAME.format(
                test_name=self.current_test_info.name
            ),
            app_metadata_version=self.APP_METADATA_VERSION,
            app_metadata_value_holder=self.APP_METADATA_VALUE_HOLDER.format(
                test_name=self.current_test_info.name
            ),
            service_unit_name=self.SERVICE_UNIT_NAME,
        )
        expected_result = 'Register Service Unit Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # Check Register Service Unit Response Contains Service Identity
        asserts.assert_in(
            service_identity,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=service_identity
            ),
        )
        # On Device 2
        client_response = self.client_2.get_service_units_by_type(
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            type_name=self.UNIT_TYPE_NAME.format(
                test_name=self.current_test_info.name
            ),
        )
        expected_result = 'Get Service Units By Type Result: Ok([])'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # 4. Add Transport Metadata On Device 1
        client_response = self.client_1.add_transport_metadata(
            transport_metadata_value_holder=self.TRANSPORT_METADATA_VALUE_HOLDER.format(
                test_name=self.current_test_info.name
            )
        )
        expected_result = 'Add Transport Metadata Result: Ok(())'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # 5. Get Service Units By Type - Non-Empty List Because Metadata Added
        # On Device 2
        client_response = self.client_2.get_service_units_by_type(
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            type_name=self.UNIT_TYPE_NAME.format(
                test_name=self.current_test_info.name
            ),
        )
        expected_result = 'Get Service Units By Type Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # Check List Of Service Units Not Empty
        unexpected_result = 'Get Service Units By Type Result: Ok([])'
        asserts.assert_true(
            (client_response != unexpected_result),
            f'Actual Result :: <{client_response}> matches unexpected result ::'
            f' <{unexpected_result}>',
        )
        # Check List Of Service Units Contains Service Identity
        asserts.assert_in(
            service_identity,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=service_identity
            ),
        )
        logging.info(
            f'{self.get_suite_name()} :: End Test {self.current_test_info.name}'
        )

    # -----------------------------------------------------------------------------------------------
    # Test - Two Devices - Get Service Unit By Name
    # -----------------------------------------------------------------------------------------------
    def test_two_devices_get_service_unit_by_name(self):
        logging.info(
            f'{self.get_suite_name()} :: Start Test'
            f' {self.current_test_info.name}'
        )
        # 1. Create Identity
        generated_public_key = self.__generate_random_public_key()
        # On Device 1
        client_response = self.client_1.create_identity(
            public_key=generated_public_key,
            sdv_vm_name='',  # Empty String, It takes the name for VM Properties
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            service_instance_name=self.SDV_INSTANCE_NAME,
        )
        expected_result = 'Create Identity Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        service_identity = client_response[len(expected_result) + 1 : -1]
        # On Device 2
        client_response = self.client_2.create_identity(
            public_key=generated_public_key,
            sdv_vm_name='',  # Empty String, It takes the name for VM Properties
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            service_instance_name=self.SDV_INSTANCE_NAME,
        )
        expected_result = 'Create Identity Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # 2. Register Service Unit On Device 1
        client_response = self.client_1.register_service_unit(
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            type_name=self.UNIT_TYPE_NAME.format(
                test_name=self.current_test_info.name
            ),
            app_metadata_version=self.APP_METADATA_VERSION,
            app_metadata_value_holder=self.APP_METADATA_VALUE_HOLDER.format(
                test_name=self.current_test_info.name
            ),
            service_unit_name=self.SERVICE_UNIT_NAME,
        )
        expected_result = 'Register Service Unit Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # Check Register Service Unit Response Contains Service Identity
        asserts.assert_in(
            service_identity,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=service_identity
            ),
        )
        # 3. Get Service Units By Type - Empty List Because Metadata Not Added
        # On Device 2
        client_response = self.client_2.get_service_units_by_name(
            sdv_vm_name='',  # Empty String, Optional
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            service_unit_name='',  # Empty String, Optional
        )
        expected_result = 'Get Service Units By Name Result: Ok([])'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # 4. Add Transport Metadata On Device 1
        client_response = self.client_1.add_transport_metadata(
            transport_metadata_value_holder=self.TRANSPORT_METADATA_VALUE_HOLDER.format(
                test_name=self.current_test_info.name
            )
        )
        expected_result = 'Add Transport Metadata Result: Ok(())'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # 5. Get Service Units By Type - Non-Empty List Because Metadata Added
        # On Device 2
        client_response = self.client_2.get_service_units_by_name(
            sdv_vm_name='',  # Empty String, Optional
            sdv_package_name=self.SDV_PACKAGE_NAME,
            service_bundle_name=self.SDV_BUNDLE_NAME,
            service_unit_name='',  # Empty String, Optional
        )
        expected_result = 'Get Service Units By Name Result: Ok'
        asserts.assert_in(
            expected_result,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=expected_result
            ),
        )
        # Check List Of Service Units Not Empty
        unexpected_result = 'Get Service Units By Name Result: Ok([])'
        asserts.assert_not_equal(client_response, unexpected_result,
            f'Actual Result :: <{client_response}> matches unexpected result ::'
            f' <{unexpected_result}>')
        # Check List Of Service Units Contains Service Identity
        asserts.assert_in(
            service_identity,
            client_response,
            self.ASSERT_MESSAGE_FORMAT.format(
                actual_result=client_response, expected_result=service_identity
            ),
        )
        logging.info(
            f'{self.get_suite_name()} :: End Test {self.current_test_info.name}'
        )

    def teardown_test(self):
        super().teardown_test()
        # Reboot to clean up the devices
        # Discuss with Service Discovery Team and Replace reboot with a better
        # way to clean up the devices like unregister service units,
        # delete identity, etc. Add the change incrementally.
        self.sdv_device_1.adb().reboot_device()
        self.sdv_device_2.adb().reboot_device()


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
