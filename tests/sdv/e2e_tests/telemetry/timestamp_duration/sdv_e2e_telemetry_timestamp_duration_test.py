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

"""SDV Telemetry Timestamp and Duration Test"""

from pathlib import Path
from time import sleep

from sdv_telemetry_test_execution import expects, telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner


class SdvE2ETelemetryTimestampDurationTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    METRICS_CONFIG_UUID = 'd1ecd19e-42bc-4da7-99fa-847205015503'
    METRICS_CONFIG_REPORT_NAME = 'time_report'

    METRICS_CONFIG_FILE_NAME = 'timestamp_duration.textproto'
    METRICS_CONFIG_PATH = Path('/data/local/tmp') / METRICS_CONFIG_FILE_NAME

    def get_simulator_command(
        self, device: sdv_device.SdvDevice, simulator_out_dir: Path
    ) -> str:
        return shlex_join([
            self.get_simulator_binary(device),
            '--max-simulation-time',
            'seconds:60',
            'telemetry-client',
            '--max-report-count',
            '1',
            '--metrics-configs',
            str(self.METRICS_CONFIG_PATH),
            '--output-directory',
            str(simulator_out_dir),
        ])

    def test_timestamp_and_duration(self):
        with self.create_temp_dir(self.sdv_device1) as simulator_out_dir:
            self.sdv_device1.adb().log().info('Starting Simulator')
            self.sdv_device1.adb().execute_shell_command(
                self.get_simulator_command(self.sdv_device1, simulator_out_dir)
            )
            self.sdv_device1.adb().log().info('Simulation finished')

            # Load Report
            report_path = self.pull_report(
                device=self.sdv_device1,
                simulator_out_dir=simulator_out_dir,
                config_uuid=self.METRICS_CONFIG_UUID,
                report_name=self.METRICS_CONFIG_REPORT_NAME,
                report_number=1,
            )

        # Parse Binary Report
        report = self.parse_binary_report(report_path)
        # Parse Payload
        report_payload = self.decode_report_payload(
            self.metrics_config.descriptor_protos, report
        )
        # Evaluate payload
        expects.expect_equal(
            report.metrics_config_uuid,
            self.METRICS_CONFIG_UUID,
            'Unexpected UUID',
        )
        expects.expect_equal(
            report_payload.duration_rounded, 1, 'Unexpected Duration'
        )

    def setup_class(self):
        super().setup_class()

        self.sdv_device1 = self.get_device('device1')
        self.sdv_device1.adb().root_device()

        self.metrics_config = self.parse_textproto_metrics_config(
            Path(self.METRICS_CONFIG_FILE_NAME)
        )


if __name__ == '__main__':
    sdv_test_runner.run()
