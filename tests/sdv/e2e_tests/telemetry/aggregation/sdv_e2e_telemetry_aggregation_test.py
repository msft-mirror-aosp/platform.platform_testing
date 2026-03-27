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

"""E2E Test to test all Metrics Config Aggregations"""

from datetime import timedelta
from pathlib import Path
from sdv_telemetry_test_execution import expects, telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_test_runner


class SdvE2ETelemetryAggregationTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    SIMULATION_TIME = timedelta(seconds=1)

    METRICS_CONFIG_UUID = 'be6bf4a0-4cc1-46ca-91c0-da43db3f0c90'

    _METRICS_CONFIG_FILE_NAME = 'aggregations.textproto'
    _simulator_out_dir = Path('/data/local/tmp/out')

    _simulation_config_dir = Path('/data/local/tmp/')
    _metrics_config_path = _simulation_config_dir / _METRICS_CONFIG_FILE_NAME
    _metrics_publisher_path = (
        _simulation_config_dir / 'speed_publisher.textproto'
    )

    def _get_simulator_command(
        self,
        device: sdv_device.SdvDevice,
        simulation_dir: Path,
        simulation_config_path: Path,
        simulation_publisher_path: Path,
        simulator_out_dir: Path,
    ) -> str:
        cd_command = shlex_join(['cd', str(simulation_dir)])
        simulator_command = shlex_join([
            self.get_simulator_binary(device),
            '--max-simulation-time',
            f'seconds:{self.SIMULATION_TIME.total_seconds()}',
            'full-simulation',
            '--metrics-configs',
            str(simulation_config_path),
            '--publisher-configs',
            str(simulation_publisher_path),
            '--output-directory',
            str(simulator_out_dir),
        ])
        return f'{cd_command} && {simulator_command}'

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1')

        self.original_authz_enable = self.sdv_device.adb().prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        self.sdv_device.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'permissions_only')

        self.sdv_device.adb().root_device()
        self.metrics_config = self.parse_textproto_metrics_config(
            Path(self._METRICS_CONFIG_FILE_NAME)
        )

    def teardown_class(self):
        # Custom teardown here
        self.sdv_device.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.original_authz_enable)
        super().teardown_class()

    def setup_test(self):
        super().setup_test()

        # Custom setup here

    def teardown_test(self):
        # Custom teardown here

        super().teardown_test()

    def _fetch_and_decode_report(self, report_number: int):
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._simulator_out_dir,
            config_uuid=self.METRICS_CONFIG_UUID,
            report_name=self.metrics_config.metrics_report_configs[0].name,
            report_number=report_number,
        )
        report = self.parse_binary_report(report_path)
        return self.decode_report_payload(
            self.metrics_config.descriptor_protos, report
        )

    def test_aggregations(self):
        self.sdv_device.adb().log().info('Starting Simulator')
        self.sdv_device.adb().execute_shell_command(
            self._get_simulator_command(
                self.sdv_device,
                self._simulation_config_dir,
                self._metrics_config_path,
                self._metrics_publisher_path,
                self._simulator_out_dir,
            )
        )
        self.sdv_device.adb().log().info('Simulation finished')

        # Verify Report 1 (empty)
        report_1 = self._fetch_and_decode_report(report_number=1)

        absent_fields = [
            'meters_per_hour',
            'max_meters_per_hour',
            'min_meters_per_hour',
            'avg_meters_per_hour',
            'stddev_meters_per_hour',
            'delta_meters_per_hour',
        ]
        for field in absent_fields:
            expects.expect_false(
                report_1.HasField(field), f'Unexpected field: {field}'
            )

        # Technically, `vec_meters_per_hour` should also not be present, but
        # it's a repeated field which can't be checked with `HasField`.
        expects.expect_equal(
            report_1.vec_meters_per_hour,
            [],
            'Unexpected value for vec_meters_per_hour',
        )
        expects.expect_true(
            report_1.HasField('count_meters_per_hour'),
            'Field count_meters_per_hour should be present',
        )
        expects.expect_equal(
            report_1.count_meters_per_hour,
            0,
            'Unexpected value for count_meters_per_hour',
        )
        expects.expect_true(
            report_1.HasField('sum_meters_per_hour'),
            'Field sum_meters_per_hour should be present',
        )
        expects.expect_equal(
            report_1.sum_meters_per_hour,
            0,
            'Unexpected value for sum_meters_per_hour',
        )

        # Verify Report 2
        report_2 = self._fetch_and_decode_report(report_number=2)

        expected_values = {
            'meters_per_hour': -20.0,
            'vec_meters_per_hour': [30.0, 10.0, -10.0, -20.0],
            'max_meters_per_hour': 30.0,
            'min_meters_per_hour': -20.0,
            'avg_meters_per_hour': 2.5,
            'count_meters_per_hour': 4,
            'sum_meters_per_hour': 10,
            'delta_meters_per_hour': -10.0,
        }

        for field, expected_val in expected_values.items():
            actual_val = getattr(report_2, field)
            expects.expect_equal(
                actual_val, expected_val, f'Unexpected value for {field}'
            )

        # Validate stddev separately as it requires rounding
        expects.expect_equal(
            round(report_2.stddev_meters_per_hour, 2),
            19.20,
            'Unexpected value for stddev_meters_per_hour',
        )


if __name__ == '__main__':
    sdv_test_runner.run()
