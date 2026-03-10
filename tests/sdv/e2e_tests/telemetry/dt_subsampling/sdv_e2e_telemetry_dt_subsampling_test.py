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

"""SDV E2E Telemetry DT Subsampling Test"""

from datetime import timedelta
from pathlib import Path
from mobly import asserts
from sdv_telemetry_test_execution import expects, telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner


class SdvE2ETelemetryDTSubsamplingTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    _SIMULATION_TIME = timedelta(seconds=13)
    _SIMULATION_CONFIG_DIR = Path("/data/local/tmp/")
    _SIMULATOR_OUT_DIR = Path("/data/local/tmp/out")

    _METRICS_CONFIG_UUID = "666176b1-66d8-4064-afa6-6e004fc39fac"
    _METRICS_CONFIG_FILE_NAME = "dt_subsampling_metrics_config.textproto"
    _METRICS_CONFIG_PATH = _SIMULATION_CONFIG_DIR / _METRICS_CONFIG_FILE_NAME

    _LONGER_INTERVAL_METRICS_CONFIG_UUID = (
        "446b3160-73c2-4bd6-8dbe-2353c0ab72de"
    )
    _LONGER_INTERVAL_METRICS_CONFIG_FILE_NAME = (
        "dt_longer_interval_subsampling_metrics_config.textproto"
    )
    _LONGER_INTERVAL_METRICS_CONFIG_PATH = (
        _SIMULATION_CONFIG_DIR / _LONGER_INTERVAL_METRICS_CONFIG_FILE_NAME
    )

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1")
        self.sdv_device.adb().root_device()

        self.metrics_config = self.parse_textproto_metrics_config(
            Path(self._METRICS_CONFIG_FILE_NAME)
        )
        self.longer_interval_metrics_config = (
            self.parse_textproto_metrics_config(
                Path(self._LONGER_INTERVAL_METRICS_CONFIG_FILE_NAME)
            )
        )

    def teardown_class(self):
        super().teardown_class()

    def setup_test(self):
        super().setup_test()

    def teardown_test(self):
        super().teardown_test()

    def test_dt_subsampling(self):
        self._run_simulation(self._METRICS_CONFIG_PATH)

        report_count = len(
            self.find_report_paths(
                self.sdv_device,
                self._SIMULATOR_OUT_DIR,
                self._METRICS_CONFIG_UUID,
                "TirePressureReport",
            )
        )
        asserts.assert_in(
            report_count,
            [2, 3],
            "Simulation runs for 13s with 5s subsampling interval, only two"
            " or three reports are expected",
        )

        expects.expect_equal(
            self._read_report(
                self._METRICS_CONFIG_UUID, 1, self.metrics_config
            ),
            42,
            "Unexpected report",
        )
        expects.expect_equal(
            self._read_report(
                self._METRICS_CONFIG_UUID, 2, self.metrics_config
            ),
            42,
            "Unexpected report",
        )
        if report_count == 3:
            expects.expect_equal(
                self._read_report(
                    self._METRICS_CONFIG_UUID, 3, self.metrics_config
                ),
                42,
                "Unexpected report",
            )

    def test_longer_interval_dt_subsampling(self):
        self._run_simulation(self._LONGER_INTERVAL_METRICS_CONFIG_PATH)

        report_count = len(
            self.find_report_paths(
                self.sdv_device,
                self._SIMULATOR_OUT_DIR,
                self._LONGER_INTERVAL_METRICS_CONFIG_UUID,
                "TirePressureReport",
            )
        )

        asserts.assert_in(
            report_count,
            [1, 2],
            "Simulation runs for 13s with 10s subsampling interval, only one"
            " or two reports are expected",
        )

        expects.expect_equal(
            self._read_report(
                self._LONGER_INTERVAL_METRICS_CONFIG_UUID,
                1,
                self.longer_interval_metrics_config,
            ),
            42,
            "Unexpected report",
        )

        if report_count == 2:
            expects.expect_equal(
                self._read_report(
                    self._LONGER_INTERVAL_METRICS_CONFIG_UUID,
                    2,
                    self.longer_interval_metrics_config,
                ),
                42,
                "Unexpected report",
            )

    def _run_simulation(self, metrics_config_path: Path):
        self.sdv_device.adb().log().info("Starting Simulator")
        self.sdv_device.adb().execute_shell_command(
            self._get_simulator_command(self.sdv_device, metrics_config_path)
        )
        self.sdv_device.adb().log().info("Simulation finished")

    def _get_simulator_command(
        self, device: sdv_device.SdvDevice, metrics_config_path: Path
    ) -> str:
        cd_command = shlex_join(["cd", str(self._SIMULATION_CONFIG_DIR)])
        simulator_command = shlex_join([
            self.get_simulator_binary(device),
            "--max-simulation-time",
            f"seconds:{self._SIMULATION_TIME.total_seconds()}",
            "telemetry-client",
            "--metrics-configs",
            str(metrics_config_path),
            "--output-directory",
            str(self._SIMULATOR_OUT_DIR),
        ])
        return f"{cd_command} && {simulator_command}"

    def _read_report(self, uuid, idx, metrics_config):
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=uuid,
            report_name="TirePressureReport",
            report_number=idx,
        )
        report = self.parse_binary_report(report_path)
        report_payload = self.decode_report_payload(
            metrics_config.descriptor_protos, report
        )
        return report_payload.tire_pressure_fl


if __name__ == "__main__":
    sdv_test_runner.run()
