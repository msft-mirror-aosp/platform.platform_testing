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

from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

class SdvAuthzNegativeTest(sdv_base_test.SdvBaseTestClass, parameterized.TestCase):

    TESTING_SERVICES_LOGCAT_ARGS = '*:F com_sdv_google_test_authz_foo_AuthzTestedService_instance com_sdv_google_test_authz_bar_AuthzTestDriver_instance'

    CREATE_SERVICE_BUNDLE_COMMAND = 'sdv_service_bundle create {service_fqin}'
    START_SERVICE_BUNDLE_COMMAND = 'sdv_service_bundle start {service_fqin}'
    DESTROY_SERVICE_BUNDLE_COMMAND = 'sdv_service_bundle destroy {service_fqin}'

    ##################################################
    ## Setup/teardown for the test suite and tests. ##
    ##################################################

    def setup_class(self):
        super().setup_class()
        self.adb_devices = {}
        self.adb_devices['tested_service_device'] = self.setup_device_testing_service(
            'device1', 'instance1:com.sdv.google.test.authz.foo.AuthzTestedService/instance')
        self.adb_devices['test_driver_device'] = self.setup_device_testing_service(
            'device2', 'instance1:com.sdv.google.test.authz.bar.AuthzTestDriver/instance')

    def setup_test(self):
        # Avoid parent `clear_all_devices` to store the start up logs from VMs.
        logging.info('Custom Authz negative test setup')

    def teardown_test(self):
        # Avoid parent `clear_all_devices` to store the start up logs from VMs.
        logging.info('Custom Authz negative test teardown')

    def teardown_class(self):
        # Explicitly clear devices, since we avoid it in `setup_test` and `teardown_test`.
        logging.info('Custom Authz negative test class teardown')

        self.teardown_device_testing_service(
            'device1', 'instance1:com.sdv.google.test.authz.foo.AuthzTestedService/instance')
        self.teardown_device_testing_service(
            'device2', 'instance1:com.sdv.google.test.authz.bar.AuthzTestDriver/instance')

        self.clear_all_devices()
        super().teardown_class()

    ################################################
    ##            Utility functions.              ##
    ################################################

    def setup_device_testing_service(self, device_name, service_fqin):
        """ Setup testing service on VM with `device_name`. """
        sdv_device = self.get_device(device_name)
        adb_device = sdv_device.adb()
        adb_device.wait_for_device_online()
        adb_device.reboot_device()
        adb_device.wait_for_device_online()
        adb_device.verify_logcat_is_running()
        adb_device.execute_shell_command(self.CREATE_SERVICE_BUNDLE_COMMAND.format(service_fqin = service_fqin))
        adb_device.execute_shell_command(self.START_SERVICE_BUNDLE_COMMAND.format(service_fqin = service_fqin))
        return adb_device

    def teardown_device_testing_service(self, device_name, service_fqin):
        """ Teardown testing service on VM with `device_name`. """
        sdv_device = self.get_device(device_name)
        adb_device = sdv_device.adb()
        adb_device.execute_shell_command(self.DESTROY_SERVICE_BUNDLE_COMMAND.format(service_fqin = service_fqin))

    def log_enter(self):
        """ Unified logging enter test suit. """
        logging.info(f"{self.get_suite_name()} :: Start test {self.current_test_info.name}")

    def log_exit(self):
        """ Unified logging exit test suit. """
        logging.info(f"{self.get_suite_name()} :: Finished test {self.current_test_info.name}")

    ################################################
    ##          Assert-like validators.           ##
    ################################################

    def assert_logcat(self, sdv_device, grep_text, assert_msg):
        """Assert-like validator to grep logcat with the `grep_text`.

        Args:
            grep_text: The text to search for in the logcat output.
            assert_msg: The failure assertion message.
        """

        if not polling.wait_and_verify_expected_logs(sdv_device, grep_text):
            logcat_grep_result = sdv_device.grep_from_logcat(grep_text, self.TESTING_SERVICES_LOGCAT_ARGS)
            asserts.assert_in(
                grep_text,
                logcat_grep_result,
                assert_msg + "\n {logcat_grep_result}"
            )

    ################################################
    ##          Test expected log message.        ##
    ################################################
    @parameterized.named_parameters(
        {
            'testcase_name': 'rpc_discovery_negative',
            'grep_text': 'Non-connectable RPC server was not discovered due to ACLs configuration as it was expected.',
            'assert_msg' : '\n[FAILURE]: RPC client is expected to fail to discover the RPC server due to ACLs configuration.'
        },
        {
            'testcase_name': 'rpc_connect_negative',
            'grep_text': 'Non-connectable RPC server denied connection due to ACLs configuration as it was expected.',
            'assert_msg' : '\n[FAILURE]: RPC client is expected to fail to connect to the RPC server due to ACLs configuration.'
        },
        {
            'testcase_name': 'dt_discovery_negative',
            'grep_text': 'Non-connectable DT publisher was not discovered due to ACLs configuration as it was expected.',
            'assert_msg' : '\n[FAILURE]: DT subscriber is expected to fail to discover the DT publisher due to ACLs configuration.'
        },
        {
            'testcase_name': 'dt_connect_negative',
            'grep_text': 'Non-connectable DT publisher denied connection due to ACLs configuration as it was expected.',
            'assert_msg' : '\n[FAILURE]: DT subscriber is expected to fail to connect to the DT publisher due to ACLs configuration.'
        },
    )
    def test_log_message(self, grep_text, assert_msg):
        self.log_enter()

        self.assert_logcat(
            sdv_device = self.adb_devices['test_driver_device'],
            grep_text = grep_text,
            assert_msg = assert_msg,
        )

        self.log_exit()

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
