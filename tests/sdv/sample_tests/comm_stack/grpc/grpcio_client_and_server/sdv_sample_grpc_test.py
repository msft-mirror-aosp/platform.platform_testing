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

"""SDV gRPC Test

Tests gRPC Communication between Two SDV VMs
"""
from mobly import asserts
import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleGrpcTest(sdv_base_test.SdvBaseTestClass):

    IN_ASSERT_MESSAGE = (
        'The line "{}" was not found in the log below:\n' + '{}\n'
    )

    SERVER_CLUSTER = 'sdv_mw_cluster_server'
    SERVER_CLUSTER_LOG_TAG = 'cluster_server: sdv_cluster_server:'
    SERVER_TPMS = 'sdv_mw_tpms_server'
    SERVER_TPMS_LOG_TAG = 'tpms_server: sdv_tpms_server:'
    CLIENT_CLUSTER = 'sdv_mw_cluster_client'
    CLIENT_CLUSTER_LOG_TAG = 'cluster_client: sdv_cluster_client:'
    CLIENT_TPMS = 'sdv_mw_tpms_client'
    CLIENT_TPMS_LOG_TAG = 'tpms_client: sdv_tpms_client:'

    def setup_class(self):
        super().setup_class()
        self.sdv_device_1 = self.get_device('device1').adb()
        self.sdv_device_2 = self.get_device('device2').adb()
        self.sdv_device_1.root_device()
        self.sdv_device_2.root_device()

    def test_grpc(self):
        # We need to run server in core and client in IVI for now.
        # In CI/CD device 1 is IVI and device 2 is core.
        self.sdv_device_1.reboot_device()
        self.sdv_device_2.reboot_device()
        self.grpc(self.sdv_device_2, self.sdv_device_1)

    # TODO(b/354933944#comment5): We should add a test that covers the case of
    # the IVI device being the server and the core device being the client when
    # it is supported.

    def grpc(self, server_device, client_device):
        logging.info(
            'Start a new terminal and run cluster server on 1st VM'
        )
        server_device.execute_shell_command_in_subprocess(
            'cluster_server_process',
            self.SERVER_CLUSTER,
        )

        logging.info(
            'Start a new terminal and run tpms server on 1st VM'
        )
        server_device.execute_shell_command_in_subprocess(
            'tpms_server_process',
            self.SERVER_TPMS,
        )

        logging.info(
            'Start a new terminal and verify the logs for cluster server on'
            ' 1st VM'
        )
        cluster_server_logs = server_device.grep_from_logcat(
            self.SERVER_CLUSTER_LOG_TAG
        )
        self.assert_contains_items(
            cluster_server_logs,
            [
                f'I {self.SERVER_CLUSTER_LOG_TAG} Cluster server service bundle started.'
            ],
        )

        logging.info(
            'Start a new terminal and verify the logs for tpms server on 1st VM'
        )
        tpms_server_logs = server_device.grep_from_logcat(
            self.SERVER_TPMS_LOG_TAG
        )
        self.assert_contains_items(
            tpms_server_logs,
            [
                f'I {self.SERVER_TPMS_LOG_TAG} Tpms server service bundle started.'
            ],
        )

        logging.info(
            'Start a new terminal and run cluster client on 2nd VM'
        )
        client_device.execute_shell_command_in_subprocess(
            'cluster_client_process',
            self.CLIENT_CLUSTER,
        )

        logging.info(
            'Start a new terminal and run tpms client on 2nd VM'
        )
        client_device.execute_shell_command_in_subprocess(
            'tpms_client_process',
            self.CLIENT_TPMS,
        )

        logging.info('Verify the logs for cluster client on 2nd VM')
        cluster_client_logs = client_device.grep_from_logcat(
            self.CLIENT_CLUSTER_LOG_TAG
        )
        self.assert_contains_items(
            cluster_client_logs,
            [
                (
                    f'I {self.CLIENT_CLUSTER_LOG_TAG} Cluster client service bundle started.'
                ),
                (
                    f'I {self.CLIENT_CLUSTER_LOG_TAG} GetHealthCheckState: valid: false, '
                    '''message: "Can't perform health check while vehicle is not parked"'''
                ),
            ],
        )
        self.assert_contains_regexes(
            cluster_client_logs,
            [
                (
                    rf'I {self.CLIENT_CLUSTER_LOG_TAG} GetDrivingState: driving_state:'
                    r' DRIVING_STATE_(PARK|REVERSE|NEUTRAL|DRIVE|LOW)'
                ),
            ],
        )

        logging.info('Verify the logs for tpms client on 2nd VM')
        tpms_client_logs = client_device.grep_from_logcat(
            self.CLIENT_TPMS_LOG_TAG
        )
        self.assert_contains_items(
            tpms_client_logs,
            [
                (
                    f'I {self.CLIENT_TPMS_LOG_TAG} Tpms client service bundle started.'
                ),
                (
                    f'I {self.CLIENT_TPMS_LOG_TAG} GetTpmsState: tpms_state: TPMS_STATE_LOW'
                ),
            ],
        )
        self.assert_contains_regexes(
            tpms_client_logs,
            [
                (
                    rf'I {self.CLIENT_TPMS_LOG_TAG} GetLowTires: tires: \[.*\]'
                ),
            ],
        )

    def assert_contains_items(self, text, expected_results_list):
        for exp_result in expected_results_list:
            asserts.assert_in(
                exp_result,
                text,
                self.IN_ASSERT_MESSAGE.format(exp_result, text),
            )

    def assert_contains_regexes(self, text, expected_regex_list):
        for exp_regex in expected_regex_list:
            asserts.assert_regex(
                text,
                r'.*' + exp_regex + r'.*',
                self.IN_ASSERT_MESSAGE.format(exp_regex, text),
            )


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
