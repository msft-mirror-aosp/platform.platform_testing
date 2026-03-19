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

"""SDV Multivd IPv4 Test

Tests communication between Two SDV VMs using IPv4
"""
from mobly import asserts
import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleMultivdIpTest(sdv_base_test.SdvBaseTestClass):

    SERVER_PROCESS_NAME = 'server_process'
    VIRT_ADDRESS = '$(getprop ro.boot.virt.address)'
    GET_VIRT_ADDRESS = f'echo {VIRT_ADDRESS}'
    SET_IP = f'ifconfig eth1 192.168.98.{VIRT_ADDRESS}'
    START_SERVER_COMMAND = 'sdv_sample_multivd_ipv4_server'
    LOGCAT_SERVER_GREP_TEXT = 'multivd-ipv4-server'
    EXPECTED_SERVER_RESULT = 'Received a from client.'
    START_CLIENT_COMMAND = (
        'sdv_sample_multivd_ipv4_client'
        ' 192.168.98.{}'
    )
    LOGCAT_CLIENT_GREP_TEXT = 'multivd-ipv4-client'
    EXPECTED_CLIENT_RESULT = 'Received b from server.'

    def setup_class(self):
        logging.info('Setting Up SDV Multivd IPv4 Test Class')
        super().setup_class()
        # Get the device using label
        self.sdv_device_server = self.get_device('device1')
        self.sdv_device_client = self.get_device('device2')
        self.server_virt_address = (
            self.sdv_device_server.adb().execute_shell_command(
                self.GET_VIRT_ADDRESS
            )
        )
        logging.info('End Setup Class For SDV Multivd IPv4 Test')

    def setup_test(self):
        logging.info('SDV Multivd IPv4 Test Setup')
        super().setup_test()
        logging.info('End SDV Multivd Iptables IPv4 Test Setup')

    def test_multivd_ipv4(self):
        logging.info('Start SDV Multivd IPv4 Test: test_multivd_ipv4')

        # Start Server
        logging.info('Starting Server')
        # Set static IP for server
        self.sdv_device_server.adb().execute_shell_command(self.SET_IP)
        # Since the server command is blocking, execute it as subprocess
        self.sdv_device_server.adb().execute_shell_command_in_subprocess(
            self.SERVER_PROCESS_NAME, self.START_SERVER_COMMAND
        )

        # Start Client
        logging.info('Starting Client')
        # Set static IP for server
        self.sdv_device_client.adb().execute_shell_command(self.SET_IP)
        self.sdv_device_client.adb().execute_shell_command(
            self.START_CLIENT_COMMAND.format(self.server_virt_address)
        )

        # Verify Connection Success
        logging.info('Verify connection success message from logcat on client')
        actual_result = self.sdv_device_client.adb().grep_from_logcat(
            self.LOGCAT_CLIENT_GREP_TEXT
        )
        asserts.assert_in(
            self.EXPECTED_CLIENT_RESULT,
            actual_result,
            f'Actual Client Result [{actual_result}] does not contain Expected'
            f' Result [{self.EXPECTED_CLIENT_RESULT}]',
        )

        logging.info('Verify connection success message from logcat on server')
        actual_result = self.sdv_device_server.adb().grep_from_logcat(
            self.LOGCAT_SERVER_GREP_TEXT
        )
        asserts.assert_in(
            self.EXPECTED_SERVER_RESULT,
            actual_result,
            f'Actual Server Result [{actual_result}] does not contain Expected'
            f' Result [{self.EXPECTED_SERVER_RESULT}]',
        )

        # Terminate the server process as it was started as a subprocess
        logging.info('Terminating Server')
        self.sdv_device_server.adb().terminate_subprocess(
            self.SERVER_PROCESS_NAME
        )

        logging.info('End SDV Multivd IPv4 Test: test_multivd_ipv4')

    def teardown_test(self):
        logging.info('SDV Multivd IPv4 Test Teardown')
        self.sdv_device_server.adb().terminate_all_subprocesses()
        super().teardown_test()
        logging.info('End SDV Multivd IPv4 Test Teardown')


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
