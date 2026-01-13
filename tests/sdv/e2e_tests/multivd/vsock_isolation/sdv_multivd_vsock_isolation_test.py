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

"""SDV Multivd Vsock Isolation Test

Tests demonstrating vsock isolation between Two SDV VMs
Ensure that vhost_user_vsock flag is set to true at
~/device/google/sdv/sdv_core_cf/android-info.txt
"""
from mobly import asserts
import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvMultivdVsockIsolationTest(sdv_base_test.SdvBaseTestClass):

    SERVER_PROCESS_NAME = 'server_process'
    START_SERVER_COMMAND = (
        'apex/com.sdv.google.sample.multivd.vsock/bin/sdv_sample_multivd_vsock_server'
        ' 1234'
    )
    LOGCAT_SERVER_GREP_TEXT = 'multivd-vsock-server'
    EXPECTED_SERVER_RESULT = 'Received a from client.'
    START_CLIENT_COMMAND = (
        'apex/com.sdv.google.sample.multivd.vsock/bin/sdv_sample_multivd_vsock_client'
        ' 3 1234'
    )
    LOGCAT_CLIENT_GREP_TEXT = 'multivd-vsock-client'
    EXPECTED_CLIENT_RESULT1 = 'Received b from server.'
    EXPECTED_CLIENT_RESULT2 = 'connect(): Connection timed out'

    def setup_class(self):
        logging.info('Setting Up SDV Multivd Vsock Isolation Test Class')
        super().setup_class()
        # Get the device uisng label
        self.sdv_device_server = self.get_device('device1')
        self.sdv_device_client1 = self.get_device('device2')
        self.sdv_device_client2 = self.get_device('device3')
        logging.info('End Setup Class For SDV Multivd Vsock Isolation Test')

    def setup_test(self):
        logging.info('SDV Multivd Vsock Isolation Test Setup')
        super().setup_test()
        logging.info('End SDV Multivd Vsock Isolation Test Setup')

    def test_multivd_vsock_isolation(self):
        logging.info(
            'Start SDV Multivd Isolation Vsock Test:'
            ' test_multivd_vsock_isolation'
        )

        # Start Server
        logging.info('Starting Server')
        # Since the server command is blocking, execute it as subprocess
        self.sdv_device_server.adb().execute_shell_command_in_subprocess(
            self.SERVER_PROCESS_NAME, self.START_SERVER_COMMAND
        )

        # Verify Connection Success
        logging.info('Starting Client 1')
        actual_result = self.sdv_device_client1.adb().execute_shell_command(
            self.START_CLIENT_COMMAND, raise_exception=False
        )
        logging.info('Verify connection success message on client 1')
        asserts.assert_in(
            self.EXPECTED_CLIENT_RESULT1,
            actual_result,
            f'Actual Client Result [{actual_result}] does not contain Expected'
            f' Result [{self.EXPECTED_CLIENT_RESULT1}]',
        )

        # Verify Connection Failure
        logging.info('Starting Client 2')
        actual_result = self.sdv_device_client2.adb().execute_shell_command(
            self.START_CLIENT_COMMAND, raise_exception=False
        )
        logging.info('Verify connection error message on client 2')
        asserts.assert_in(
            self.EXPECTED_CLIENT_RESULT2,
            actual_result,
            f'Actual Client Result [{actual_result}] does not contain Expected'
            f' Result [{self.EXPECTED_CLIENT_RESULT2}]',
        )

        # Terminate the server process as it was started as a subprocess
        logging.info('Terminating Server')
        self.sdv_device_server.adb().terminate_subprocess(
            self.SERVER_PROCESS_NAME
        )

        logging.info(
            'End SDV Multivd Vsock Isolation Test: test_multivd_vsock_isolation'
        )

    def teardown_test(self):
        logging.info('SDV Multivd Vsock Isolation Test Teardown')
        self.sdv_device_server.adb().terminate_all_subprocesses()
        super().teardown_test()
        logging.info('End SDV Multivd Vsock Isolation Test Teardown')


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
