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

"""
SDV Authorization Negative test.

Verifies:

-   CUJ-AuthZ-RPC-Negative:

    A Service bundle A implements a RPC interface and declares an empty ACL
    definition. A local client and a remote client try to discover the RPC
    interface, and doesn't get any information back from SD.

-   CUJ-AuthZ-DT-Negative:

    A Service bundle A implements a DT interface and declares an empty ACL
    definition. A local client and a remote client try to discover the DT
    interface, and doesn't get any information back from SD.

-   AuthZ-RPC-Connect-Negative

    Make sure that not authorized RPC client cannot connect to the RPC server
    even knowing the RPC server SID and service unit name, which are not
    provided by the Service Discovery agent.

    Note: This is not an official CUJ.

-   AuthZ-RPC-Connect-Negative

    Make sure that not authorized DT subscriber cannot subscribe to the DT
    publisher even knowing the DT publisher SID and service unit name, which are
    not provided by the Service Discovery agent.

    Note: This is not an official CUJ.
"""

from mobly import asserts
import logging
import time

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling
from sdv_test_fw.device.sdv_property import SdvDeviceProperty

class SdvAuthzNegativeTest(sdv_base_test.SdvBaseTestClass):

    CREATE_SERVICE_BUNDLE_COMMAND = 'sdv_service_bundle create {service_fqin}'
    START_SERVICE_BUNDLE_COMMAND = 'sdv_service_bundle start {service_fqin}'
    DESTROY_SERVICE_BUNDLE_COMMAND = 'sdv_service_bundle destroy {service_fqin}'

    ACLS_TESTING_SERVICES_LOGCAT_ARGS = '*:F com_android_sdv_test_authz_foo_AuthzTestedService_instance com_android_sdv_test_authz_bar_AuthzTestDriver_instance'
    ACLS_TESTED_SERVICE_FQIN = 'instance1:com.android.sdv.test.authz.foo.AuthzTestedService/instance'
    ACLS_TEST_DRIVER_FQIN = 'instance1:com.android.sdv.test.authz.bar.AuthzTestDriver/instance'

    PERMISSIONS_TESTING_SERVICES_LOGCAT_ARGS = '*:F com_android_sdv_test_permissions_foo_AuthzTestedService_instance com_android_sdv_test_permissions_bar_AuthzTestDriver_instance'
    PERMISSIONS_TESTED_SERVICE_FQIN = 'instance1:com.android.sdv.test.permissions.foo.AuthzTestedService/instance'
    PERMISSIONS_TEST_DRIVER_FQIN = 'instance1:com.android.sdv.test.permissions.bar.AuthzTestDriver/instance'

    ##################################################
    ## Setup/teardown for the test suite and tests. ##
    ##################################################

    def setup_class(self):
        super().setup_class()

        # We don't start the service bundles in setup_class.
        # It's done at the beginning of each test case.
        self.tested_service_device = self.get_device('device1').adb()
        self.test_driver_device = self.get_device('device2').adb()

    def setup_test(self):
        # Avoid parent `clear_all_devices` to store the start up logs from VMs.
        logging.info('Custom Authz negative test setup')
        self.log_enter()

        self.tested_service_device_original_authz = self.tested_service_device.prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        self.test_driver_device_original_authz = self.test_driver_device.prop.get(SdvDeviceProperty.AUTHZ_ENABLE)

        if self.current_test_info.name == 'test_acls':
            self.setup_services_for_test(self.ACLS_TESTED_SERVICE_FQIN, self.ACLS_TEST_DRIVER_FQIN, "acls_only")
        elif self.current_test_info.name == 'test_permissions':
            self.setup_services_for_test(self.PERMISSIONS_TESTED_SERVICE_FQIN, self.PERMISSIONS_TEST_DRIVER_FQIN, "permissions_only")

    def teardown_test(self):
        # Avoid parent `clear_all_devices` to store the start up logs from VMs.
        logging.info('Custom Authz negative test teardown')
        if self.current_test_info.name == 'test_acls':
            self.teardown_services_for_test(self.ACLS_TESTED_SERVICE_FQIN, self.ACLS_TEST_DRIVER_FQIN)
        elif self.current_test_info.name == 'test_permissions':
            self.teardown_services_for_test(self.PERMISSIONS_TESTED_SERVICE_FQIN, self.PERMISSIONS_TEST_DRIVER_FQIN)

        self.tested_service_device.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.tested_service_device_original_authz)
        self.test_driver_device.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.test_driver_device_original_authz)
        self.log_exit()

    def teardown_class(self):
        # Explicitly clear devices, since we avoid it in `setup_test` and `teardown_test`.
        logging.info('Custom Authz negative test class teardown')
        self.clear_all_devices()
        super().teardown_class()

    ################################################
    ##            Utility functions.              ##
    ################################################

    def setup_services_for_test(self, tested_service_fqin, test_driver_fqin, authz_mode):
        """ Setup testing services on VMs. """
        for adb_device, fqin in [
            (self.tested_service_device, tested_service_fqin),
            (self.test_driver_device, test_driver_fqin)
        ]:
            adb_device.wait_for_device_online()
            adb_device.reboot_device()
            adb_device.wait_for_device_online()

            self.tested_service_device.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, authz_mode)
            self.test_driver_device.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, authz_mode)

            adb_device.verify_logcat_is_running()
            adb_device.execute_shell_command(self.CREATE_SERVICE_BUNDLE_COMMAND.format(service_fqin = fqin))
            adb_device.execute_shell_command(self.START_SERVICE_BUNDLE_COMMAND.format(service_fqin = fqin))

    def teardown_services_for_test(self, tested_service_fqin, test_driver_fqin):
        """ Teardown testing services on VMs. """
        for adb_device, fqin in [
            (self.tested_service_device, tested_service_fqin),
            (self.test_driver_device, test_driver_fqin)
        ]:
            adb_device.execute_shell_command(self.DESTROY_SERVICE_BUNDLE_COMMAND.format(service_fqin = fqin))

    def log_enter(self):
        """ Unified logging enter test suit. """
        logging.info(f"{self.get_suite_name()} :: Start test {self.current_test_info.name}")

    def log_exit(self):
        """ Unified logging exit test suit. """
        logging.info(f"{self.get_suite_name()} :: Finished test {self.current_test_info.name}")

    ################################################
    ##          Assert-like validators.           ##
    ################################################

    def assert_logcat(self, sdv_device, logcat_args, grep_text, assert_msg):
        """Assert-like validator to grep logcat with the `grep_text`.

        Args:
            grep_text: The text to search for in the logcat output.
            assert_msg: The failure assertion message.
        """

        if not polling.wait_and_verify_expected_logs(sdv_device, grep_text):
            logcat_grep_result = sdv_device.grep_from_logcat(grep_text, logcat_args)
            asserts.assert_in(
                grep_text,
                logcat_grep_result,
                assert_msg + "\n {logcat_grep_result}"
            )

    ################################################
    ##                 Test Cases                 ##
    ################################################

    def _get_expected_logs(self, authz_type):
        return [
            (
                f'Non-connectable RPC server was not discovered due to {authz_type} configuration as it was expected.',
                f'\n[FAILURE]: RPC client is expected to fail to discover the RPC server due to {authz_type} configuration.'
            ),
            (
                f'Non-connectable RPC server denied connection due to {authz_type} configuration as it was expected.',
                f'\n[FAILURE]: RPC client is expected to fail to connect to the RPC server due to {authz_type} configuration.'
            ),
            (
                f'Non-connectable DT publisher was not discovered due to {authz_type} configuration as it was expected.',
                f'\n[FAILURE]: DT subscriber is expected to fail to discover the DT publisher due to {authz_type} configuration.'
            ),
            (
                f'Non-connectable DT publisher denied connection due to {authz_type} configuration as it was expected.',
                f'\n[FAILURE]: DT subscriber is expected to fail to connect to the DT publisher due to {authz_type} configuration.'
            )
        ]

    def test_acls(self):
        """Tests the Authz ACLs configuration."""

        for grep_text, assert_msg in self._get_expected_logs('ACLs'):
            self.assert_logcat(
                sdv_device=self.test_driver_device,
                logcat_args=self.ACLS_TESTING_SERVICES_LOGCAT_ARGS,
                grep_text=grep_text,
                assert_msg=assert_msg
            )

    def test_permissions(self):
        """Tests the Authz permissions configuration."""

        # 1. Check standard logs for the non-connectable endpoints
        for grep_text, assert_msg in self._get_expected_logs('permissions'):
            self.assert_logcat(
                sdv_device=self.test_driver_device,
                logcat_args=self.PERMISSIONS_TESTING_SERVICES_LOGCAT_ARGS,
                grep_text=grep_text,
                assert_msg=assert_msg
            )

        # 2. Check local VM-denied validations on the tested service (VM 1)
        for grep_text, assert_msg in [
            (
                'Successfully performed local request to VM denied server, response: hello_world',
                '\n[FAILURE]: Tested service is expected to successfully connect locally to the VM-denied server.'
            ),
            (
                'Successfully received message locally from VM denied publisher',
                '\n[FAILURE]: Tested service is expected to successfully subscribe locally to the VM-denied publisher.'
            )
        ]:
            self.assert_logcat(
                sdv_device=self.tested_service_device,
                logcat_args=self.PERMISSIONS_TESTING_SERVICES_LOGCAT_ARGS,
                grep_text=grep_text,
                assert_msg=assert_msg
            )

        # 3. Check cross-VM VM-denied non-discoverability on the test driver (VM 2)
        for grep_text, assert_msg in [
            (
                'VM-denied RPC server was not discovered due to VM permissions configuration as it was expected.',
                '\n[FAILURE]: RPC client is expected to fail to discover the VM-denied RPC server due to VM permissions configuration.'
            ),
            (
                'VM-denied DT publisher was not discovered due to VM permissions configuration as it was expected.',
                '\n[FAILURE]: DT subscriber is expected to fail to discover the VM-denied DT publisher due to VM permissions configuration.'
            )
        ]:
            self.assert_logcat(
                sdv_device=self.test_driver_device,
                logcat_args=self.PERMISSIONS_TESTING_SERVICES_LOGCAT_ARGS,
                grep_text=grep_text,
                assert_msg=assert_msg
            )

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
