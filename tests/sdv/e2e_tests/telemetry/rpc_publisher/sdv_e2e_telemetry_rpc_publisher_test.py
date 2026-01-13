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

"""SDV E2E Telemetry RPC Publisher Test"""

from mobly import asserts
from datetime import timedelta
from pathlib import Path
from sdv_telemetry_test_execution import telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner


class SdvE2ETelemetryRpcPublisherTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    _SIMULATION_TIME = timedelta(seconds=2)
    _SIMULATION_CONFIG_DIR = Path("/data/local/tmp/")
    _SIMULATOR_OUT_DIR = Path("/data/local/tmp/out")

    _METRICS_CONFIG_UUID = "869d6a70-9e91-4443-be20-647f42fdb4ee"
    _METRICS_CONFIG_FILE_NAME = "rpc_publisher_metrics_config.textproto"
    _METRICS_CONFIG_PATH = _SIMULATION_CONFIG_DIR / _METRICS_CONFIG_FILE_NAME

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1")
        self.sdv_device.adb().root_device()

        self.metrics_config = self.parse_textproto_metrics_config(
            Path(self._METRICS_CONFIG_FILE_NAME)
        )

    def teardown_class(self):
        super().teardown_class()

    def setup_test(self):
        super().setup_test()

    def teardown_test(self):
        super().teardown_test()

    def test_rpc_publisher(self):
        self._run_simulation(self._METRICS_CONFIG_PATH)

        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=self._METRICS_CONFIG_UUID,
            report_name="TirePressureReport",
            report_number=1,
        )
        report = self.parse_binary_report(report_path)

        # Decode report
        report_payload = self.decode_report_payload(
            self.metrics_config.descriptor_protos, report
        )

        asserts.assert_equal(
            report_payload.tire_pressure_fl, 992, "Unexpected report"
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


if __name__ == "__main__":
    sdv_test_runner.run()
