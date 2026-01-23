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

"""SDV Sample Telemetry Configurable Publisher Test

Tests Telemetry Configurable Publisher on one SDV VM
"""
from mobly import asserts
from pathlib import Path

from sdv_telemetry_test_execution import telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner


class SdvSampleTelemetryConfigurablePublisherTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    METRICS_CONFIG_UUID = 'c6005cea-8fde-436a-9f29-c12f06a10c10'
    METRICS_CONFIG_REPORT_NAME = 'SampleReport'
    SIMULATION_SUMMARY_FILE_NAME = f'{METRICS_CONFIG_UUID}_all_reports_for_{METRICS_CONFIG_REPORT_NAME.lower()}.txt'

    CONFIGURABLE_PUBLISHER_COMMAND = 'telemetry_sample_publisher'

    def _get_simulator_command(
        self,
        device: sdv_device.SdvDevice,
        simulator_out_dir: Path,
    ) -> str:
        return shlex_join([
            self.get_simulator_binary(device),
            '--max-simulation-time',
            'seconds:20',
            'telemetry-client',
            '--metrics-configs',
            '/data/local/tmp/config.textproto',
            '--no-binary-reports',
            '--max-report-count',
            '1',
            '--output-directory',
            str(simulator_out_dir),
        ])

    def setup_class(self):
        super().setup_class()

        self.sdv_device = self.get_device('device1')
        self.sdv_device.adb().root_device()

    def setup_test(self):
        super().setup_test()

        self.start_telemetry_service_if_ivi(self.sdv_device)

    def test_run_configurable_publisher(self):
        self.sdv_device.adb().log().info('Starting configurable publisher')
        configurable_publisher_log_file = (
            self.sdv_device.adb().execute_shell_command_in_subprocess_log(
                self.CONFIGURABLE_PUBLISHER_COMMAND,
            )
        )

        with self.create_temp_dir(self.sdv_device) as simulator_out_dir:
            self.sdv_device.adb().log().info('Starting Simulator Client')
            self.sdv_device.adb().execute_shell_command(
                self._get_simulator_command(self.sdv_device, simulator_out_dir)
            )

            self.sdv_device.adb().log().info(
                'Checking status of configurable publisher'
            )
            asserts.assert_true(
                self.sdv_device.adb().is_subprocess_running(
                    configurable_publisher_log_file
                ),
                'Configurable publisher should not have exited.'
                f' Log:\n{self.sdv_device.adb().read_file(configurable_publisher_log_file)}',
            )

            actual_result = self.sdv_device.adb().read_file(
                simulator_out_dir / self.SIMULATION_SUMMARY_FILE_NAME
            )

        expected_result_regex = (
            r'Metrics config "config[.]textproto", UUID:'
            rf' "{self.METRICS_CONFIG_UUID}"(?s:.*?)'
            rf'Report configuration "{self.METRICS_CONFIG_REPORT_NAME}"(?s:.*?)'
            r'Report #1 created on.*?\n+'
            r'\s*sample_data: \d+'
        )
        asserts.assert_regex(
            actual_result,
            expected_result_regex,
            'Unexpected report summary received.',
        )


if __name__ == '__main__':
    sdv_test_runner.run()
