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

"""E2E Test to validate Cumulative Elevation Gain functionality"""

from datetime import timedelta
from pathlib import Path
from sdv_telemetry_test_execution import expects, telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner


class SdvE2ETelemetryCumulativeElevationGainTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    SIMULATION_TIME = timedelta(seconds=2)

    METRICS_CONFIG_UUID = "1e82785c-be24-46dc-a385-687884ca722f"

    _SIMULATION_CONFIG_DIR = Path("/data/local/tmp/")
    _SIMULATOR_OUT_DIR = Path("/data/local/tmp/out")
    _METRICS_CONFIG_FILE_NAME = "elevation_gain_config.textproto"
    _METRICS_CONFIG_PATH = _SIMULATION_CONFIG_DIR / _METRICS_CONFIG_FILE_NAME
    _METRICS_PUBLISHER_PATH = (
        _SIMULATION_CONFIG_DIR / "elevation_publisher.textproto"
    )

    def _get_simulator_command(
        self,
        device: sdv_device.SdvDevice,
        simulation_dir: Path,
        simulation_config_path: Path,
        simulation_publisher_path: Path,
        simulator_out_dir: Path,
    ) -> str:
        cd_command = shlex_join(["cd", str(simulation_dir)])
        simulator_command = shlex_join([
            self.get_simulator_binary(device),
            "--max-simulation-time",
            f"seconds:{self.SIMULATION_TIME.total_seconds()}",
            "full-simulation",
            "--metrics-configs",
            str(simulation_config_path),
            "--publisher-configs",
            str(simulation_publisher_path),
            "--output-directory",
            str(simulator_out_dir),
        ])
        return f"{cd_command} && {simulator_command}"

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1")
        self.sdv_device.adb().root_device()

        self.metrics_config = self.parse_textproto_metrics_config(
            Path(self._METRICS_CONFIG_FILE_NAME)
        )

    def teardown_class(self):
        # Custom teardown here
        super().teardown_class()

    def setup_test(self):
        super().setup_test()
        # Custom setup here

    def teardown_test(self):
        # Custom teardown here
        super().teardown_test()

    def test_cumulative_elevation_gain(self):
        self.sdv_device.adb().log().info("Starting Simulator")
        self.sdv_device.adb().execute_shell_command(
            self._get_simulator_command(
                self.sdv_device,
                self._SIMULATION_CONFIG_DIR,
                self._METRICS_CONFIG_PATH,
                self._METRICS_PUBLISHER_PATH,
                self._SIMULATOR_OUT_DIR,
            )
        )
        self.sdv_device.adb().log().info("Simulation finished")

        self._validate_elevation_gain()
        self._validate_elevation_loss()

    def _validate_elevation_gain(self):
        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=self.METRICS_CONFIG_UUID,
            report_name="cumulative_elevation_gain",
            report_number=4,
        )
        report = self.parse_binary_report(report_path)

        # Decode Report
        report_payload = self.decode_report_payload(
            self.metrics_config.descriptor_protos, report
        )

        expects.expect_equal(
            report.metrics_config_uuid,
            self.METRICS_CONFIG_UUID,
            "Unexpected UUID",
        )
        expects.expect_equal(
            report_payload.deltas, [50, 120, 20, 70], "Unexpected gain deltas"
        )
        expects.expect_equal(
            report_payload.cumulative_elevation_gain,
            260,
            "Unexpected cumulative_elevation_gain value",
        )

    def _validate_elevation_loss(self):
        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=self.METRICS_CONFIG_UUID,
            report_name="cumulative_elevation_loss",
            report_number=4,
        )
        report = self.parse_binary_report(report_path)

        # Decode Report
        report_payload = self.decode_report_payload(
            self.metrics_config.descriptor_protos, report
        )

        expects.expect_equal(
            report.metrics_config_uuid,
            self.METRICS_CONFIG_UUID,
            "Unexpected UUID",
        )
        expects.expect_equal(
            report_payload.deltas, [80, 30, 50, 10], "Unexpected loss deltas"
        )
        expects.expect_equal(
            report_payload.cumulative_elevation_loss,
            170,
            "Unexpected cumulative_elevation_loss value",
        )


if __name__ == "__main__":
    sdv_test_runner.run()
