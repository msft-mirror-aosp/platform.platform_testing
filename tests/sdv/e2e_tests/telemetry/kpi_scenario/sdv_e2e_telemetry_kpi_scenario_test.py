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

"""SDV Sample Telemetry KPI Scenario Test

Tests Telemetry KPI Scenario on one SDV VM
"""

from mobly import asserts
from datetime import timedelta
from itertools import chain
from pathlib import Path
import tempfile
from typing import List
from metrics import calculate_metrics
from sdv_perfetto import perfetto_collector, perfetto_trace_processor
from sdv_telemetry_scenario_generator.generate import WORST_CASE_SCENARIO, generate
from sdv_telemetry_test_execution import telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner


class SdvE2ETelemetryKpiScenarioTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    SIMULATION_TIME = timedelta(minutes=2)
    EXPECTED_REPORT_COUNT = (
        SIMULATION_TIME.total_seconds()
        * WORST_CASE_SCENARIO['AVERAGE_REPORTS_PER_SECOND']
    )

    _simulation_config_dir: Path
    _metrics_config_names: List[str]
    _simulation_publisher_names: List[str]
    _simulation_actions_file_name: str

    def _get_simulator_command(
        self,
        device: sdv_device.SdvDevice,
        simulation_config_dir: Path,
        simulator_out_dir: Path,
        metrics_config_file_names: List[str],
        simulation_publisher_file_names: List[str],
        simulation_actions_file_name: str,
    ) -> str:
        cd_command = shlex_join(['cd', str(simulation_config_dir)])
        simulator_command = shlex_join([
            self.get_simulator_binary(device),
            '--max-simulation-time',
            f'seconds:{self.SIMULATION_TIME.total_seconds()}',
            'full-simulation',
            '--metrics-configs',
            *metrics_config_file_names,
            '--publisher-configs',
            *simulation_publisher_file_names,
            '--simulation-actions',
            str(simulation_actions_file_name),
            '--output-directory',
            str(simulator_out_dir),
        ])
        return f'{cd_command} && {simulator_command}'

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1')
        self.sdv_device.adb().root_device()
        self.perfetto_collector = perfetto_collector.PerfettoCollector(
            device=self.sdv_device.adb()
        )

    def setup_test(self):
        super().setup_test()

        self._simulation_config_dir = self.enter_context(
            self.create_temp_dir(self.sdv_device)
        )

        with tempfile.TemporaryDirectory() as host_temp_dir:
            host_temp_dir = Path(host_temp_dir)

            self.sdv_device.adb().log().info(
                f'Generating sample scenario in {host_temp_dir}'
            )
            generate(host_temp_dir, self.SIMULATION_TIME)

            self.sdv_device.adb().log().info(
                f'Uploading sample scenario to {self._simulation_config_dir}'
            )
            metrics_config_paths = list(
                host_temp_dir.glob('metrics_config_*.textproto')
            )
            simulation_publisher_paths = list(
                host_temp_dir.glob('simulation_publisher_*.textproto')
            )
            simulation_actions_path = (
                host_temp_dir / 'simulation_actions.textproto'
            )

            self._metrics_config_names = [
                path.name for path in metrics_config_paths
            ]
            self._simulation_publisher_names = [
                path.name for path in simulation_publisher_paths
            ]
            self._simulation_actions_file_name = simulation_actions_path.name

            self.sdv_device.adb().push([
                str(path)
                for path in [
                    *chain(
                        metrics_config_paths,
                        simulation_publisher_paths,
                        host_temp_dir.glob('simulation_publisher_*.csv'),
                        [simulation_actions_path],
                    ),
                    self._simulation_config_dir,
                ]
            ])

    def teardown_test(self):
        trace_file_path = self.perfetto_collector.stop_trace(tag='device1')
        self._export_metrics_to_crystalball(trace_file_path)
        super().teardown_test()

    def teardown_class(self):
        # Custom teardown here
        super().teardown_class()

    def test_run_telemetry_kpi_scenario(self):
        with self.create_temp_dir(self.sdv_device) as simulator_out_dir:
            self.sdv_device.adb().log().info('Starting Simulator')
            self.perfetto_collector.start_trace()
            self.sdv_device.adb().execute_shell_command(
                self._get_simulator_command(
                    self.sdv_device,
                    self._simulation_config_dir,
                    simulator_out_dir,
                    self._metrics_config_names,
                    self._simulation_publisher_names,
                    self._simulation_actions_file_name,
                )
            )
            self.sdv_device.adb().log().info('Simulation finished')

            report_count = len(
                self.find_report_paths(self.sdv_device, simulator_out_dir)
            )
            self.sdv_device.adb().log().info(
                f'Simulation produced {report_count} reports'
            )

        # Since there is some randomness involved, we treat a report count
        # within 10% of the expected value as passing.
        asserts.assert_greater_equal(
            report_count,
            self.EXPECTED_REPORT_COUNT * 0.9,
            f'report_count {report_count} should be'
            f' {self.EXPECTED_REPORT_COUNT * 0.9} or more.',
        )
        asserts.assert_less_equal(
            report_count,
            self.EXPECTED_REPORT_COUNT * 1.1,
            f'report_count {report_count} should be'
            f' {self.EXPECTED_REPORT_COUNT * 1.1} or less.',
        )

    def _export_metrics_to_crystalball(self, trace_file_path):
        trace = perfetto_trace_processor.PerfettoTraceProcessor(trace_file_path)

        metrics = calculate_metrics(trace)
        self.sdv_device.adb().log().info(f'Metrics: {metrics}')

        TESTNAME = f'{__class__.__name__}'
        perfetto_trace_processor.export_to_crystalball(
            {TESTNAME: metrics},
            output_dir=self.sdv_device.adb().log_path(),
            test_name=TESTNAME,
            omit_base_name=False,
        )


if __name__ == '__main__':
    sdv_test_runner.run()
