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

"""SDV Service Discovery Test

Tests Service Discovery on One SDV VM
"""
from mobly import asserts
import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleServiceDiscoveryTest(sdv_base_test.SdvBaseTestClass):

    SERVER_PROCESS_NAME = 'server_process'
    START_ROCHAMBEAU_COMMAND = 'rochambeau_{server_client}_rs'
    EXPECTED_PLAYER_RESULT = 'Rochambeau Rust server starting...'

    # The judge result must contain either "rock","paper" or "scissors",
    # case insensitive.
    EXPECTED_JUDGE_RESULT_REGEX = r'(?i)\b(rock|paper|scissors)\b'

    ASSERT_IN_ERROR_MESSAGE = (
        'Actual Result [{actual_result}] does not contain Expected Result'
        ' [{expected_result}]'
    )

    def setup_class(self):
        super().setup_class()
        self.sdv_device_server_adb = self.get_device('device1').adb()

        # TODO(b/395067075): Update test to work with authz enabled
        self.original_authz_value_device1 = self.sdv_device_server_adb.execute_shell_command(
            'getprop sdv.authz.enable'
        )
        logging.info(
            f'Saving foo original sdv.authz.enable value: {self.original_authz_value_device1}'
        )
        self.sdv_device_server_adb.execute_shell_command('setprop sdv.authz.enable false')

    def teardown_class(self):
        # TODO(b/395067075): Update test to work with authz enabled
        logging.info('Restoring original sdv.authz.enable settings')
        self.sdv_device_server_adb.execute_shell_command(
            f'setprop sdv.authz.enable {self.original_authz_value_device1}'
        )
        super().teardown_class()

    def test_Service_discovery(self):
        player_log = (
            self.sdv_device_server_adb.execute_shell_command_in_subprocess_log(
                self.START_ROCHAMBEAU_COMMAND.format(server_client='player')
            )
        )

        asserts.assert_in(
            self.EXPECTED_PLAYER_RESULT,
            self.sdv_device_server_adb.read_file(player_log),
            self.ASSERT_IN_ERROR_MESSAGE.format(
                actual_result=self.sdv_device_server_adb.read_file(player_log),
                expected_result=self.EXPECTED_PLAYER_RESULT,
            ),
        )

        judge_result = self.sdv_device_server_adb.execute_shell_command(
            self.START_ROCHAMBEAU_COMMAND.format(server_client='judge'),
        )

        asserts.assert_regex(
            judge_result,
            self.EXPECTED_JUDGE_RESULT_REGEX
        )


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
