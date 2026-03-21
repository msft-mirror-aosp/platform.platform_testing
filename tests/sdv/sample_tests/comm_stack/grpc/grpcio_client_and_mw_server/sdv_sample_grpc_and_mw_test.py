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

"""SDV Sample gRPC and MW Test

Tests gRPC TLS Communication between Middleware server and gRPCio client.
This is meant to check wire compatibility between grpcio and Tonic + openssl.
"""
from mobly import asserts
import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleGrpcTest(sdv_base_test.SdvBaseTestClass):

    IN_ASSERT_MESSAGE = (
        'The line "{}" was not found in the log below:\n{}\n'
    )

    CLIENT_USER_PREFERENCES = 'sdv_user_preferences_client_cpp'

    SERVER_USER_PREFERENCES = 'com_android_sdv_sample_user_preferences_UserPreferencesServiceBundle_default'

    SERVER_USER_CREATED_LOG = 'I com_android_sdv_sample_user_preferences_UserPreferencesServiceBundle_default:' \
            ' user_preferences_service_stable::admin_service_impl:' \
            ' Successfully added user: User { id: 1, flags: 1 }'

    SERVER_USER_DELETED_LOG = 'I com_android_sdv_sample_user_preferences_UserPreferencesServiceBundle_default:' \
            ' user_preferences_service_stable::admin_service_impl:' \
            ' User 1 was successfully deleted'

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()

    def setup_test(self):
        # Setup to run tests
        logging.info('Starting SdvSampleGrpcTest Setup')
        super().setup_test()

        # Reboot Device to ensure clean state
        self.sdv_device.reboot_device()

        # Root Device
        self.sdv_device.root_device()

    def test_grpc_and_mw_comms(self):
        logging.info(
            'Run C++ gRPC User Preferences Client'
        )
        self.sdv_device.execute_shell_command(self.CLIENT_USER_PREFERENCES)

        logging.info(
            'Start a new terminal and verify the logs for UP server'
        )
        server_log = self.sdv_device.grep_from_logcat('{}:'.format(self.SERVER_USER_PREFERENCES))

        asserts.assert_in(
            self.SERVER_USER_CREATED_LOG,
            server_log,
            self.IN_ASSERT_MESSAGE.format(self.SERVER_USER_CREATED_LOG, server_log),
        )

        asserts.assert_in(
            self.SERVER_USER_DELETED_LOG,
            server_log,
            self.IN_ASSERT_MESSAGE.format(self.SERVER_USER_DELETED_LOG, server_log),
        )

        logging.info(
            'Start a new terminal and verify the logs for User Preferences C++ client'
        )
        client_log = self.sdv_device.grep_from_logcat(
            f'{self.CLIENT_USER_PREFERENCES}:'
        )

        client_user_created_log = f'I {self.CLIENT_USER_PREFERENCES}: User created successfully'
        asserts.assert_in(
            client_user_created_log,
            client_log,
            self.IN_ASSERT_MESSAGE.format(client_user_created_log, client_log),
        )

        client_user_deleted_log = f'I {self.CLIENT_USER_PREFERENCES}: User deleted successfully'
        asserts.assert_in(
            client_user_deleted_log,
            client_log,
            self.IN_ASSERT_MESSAGE.format(client_user_deleted_log, client_log),
        )

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
