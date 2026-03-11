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

"""SDV E2E Telemetry DT Publisher Test"""

from datetime import timedelta
from pathlib import Path
from mobly import asserts
from sdv_telemetry_test_execution import expects, telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner


class SdvE2ETelemetryDtPublisherTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    _SIMULATION_CONFIG_DIR = Path("/data/local/tmp/")
    _SIMULATOR_OUT_DIR = Path("/data/local/tmp/out")

    # ----------------------------
    # ----- SDV DT publisher -----
    # ----------------------------
    _SDV_METRICS_CONFIG_UUID = "5339d0f0-3f21-42e5-93d4-bf97492bb356"
    _SDV_METRICS_CONFIG_FILE_NAME = "dt_publisher_metrics_config.textproto"
    _SDV_METRICS_CONFIG_PATH = (
        _SIMULATION_CONFIG_DIR / _SDV_METRICS_CONFIG_FILE_NAME
    )

    # ----------------------------
    # ----- SDV DT publisher -----
    # ----- w/ VSIDL metadata ----
    # ----------------------------
    _SDV_VSIDL_METRICS_CONFIG_UUID = "6cd5ec74-8895-4b5e-85e6-b6e95a7b1211"
    _SDV_VSIDL_METRICS_CONFIG_FILE_NAME = "sdv_vsidl_metrics_config.textproto"
    _SDV_VSIDL_METRICS_CONFIG_PATH = (
        _SIMULATION_CONFIG_DIR / _SDV_VSIDL_METRICS_CONFIG_FILE_NAME
    )

    # ----------------------------
    # ---- SomeIP DT publisher ---
    # ----------------------------
    _SOMEIP_METRICS_CONFIG_UUID = "d3456cf0-8fde-436a-9f29-c12f16d10c10"
    _SOMEIP_METRICS_CONFIG_FILE_NAME = (
        "someip_dt_publisher_metrics_config.textproto"
    )
    _SOMEIP_METRICS_CONFIG_PATH = (
        _SIMULATION_CONFIG_DIR / _SOMEIP_METRICS_CONFIG_FILE_NAME
    )

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1")
        self.sdv_device.adb().root_device()

        self.sdv_metrics_config = self.parse_textproto_metrics_config(
            Path(self._SDV_METRICS_CONFIG_FILE_NAME)
        )
        self.sdv_vsidl_metrics_config = self.parse_textproto_metrics_config(
            Path(self._SDV_VSIDL_METRICS_CONFIG_FILE_NAME)
        )
        self.someip_metrics_config = self.parse_textproto_metrics_config(
            Path(self._SOMEIP_METRICS_CONFIG_FILE_NAME)
        )

    def teardown_class(self):
        super().teardown_class()

    def setup_test(self):
        super().setup_test()

    def teardown_test(self):
        super().teardown_test()

    def test_sdv_publisher(self):
        self._run_simulation(
            self._SDV_METRICS_CONFIG_PATH, timedelta(seconds=2)
        )

        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=self._SDV_METRICS_CONFIG_UUID,
            report_name="TirePressureReport",
            report_number=1,
        )
        report = self.parse_binary_report(report_path)

        # Decode Report
        report_payload = self.decode_report_payload(
            self.sdv_metrics_config.descriptor_protos, report
        )

        expects.expect_equal(
            report_payload.tire_pressure_fl, 42, "Unexpected report"
        )

    def test_sdv_publisher_with_vsidl_metadata(self):
        # TODO(b/395067075): Update test to work with authz enabled
        self.enter_context(self.disable_authz(self.sdv_device))
        self._start_vsidl_metadata_service()
        self._run_simulation(
            self._SDV_VSIDL_METRICS_CONFIG_PATH, timedelta(seconds=2)
        )

        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=self._SDV_VSIDL_METRICS_CONFIG_UUID,
            report_name="TirePressureReport",
            report_number=1,
        )
        report = self.parse_binary_report(report_path)

        # Decode Report
        report_payload = self.decode_report_payload(
            self.sdv_vsidl_metrics_config.descriptor_protos, report
        )

        expects.expect_greater_equal(
            report_payload.pressure,
            0,
            f"Pressure value {report_payload.pressure} is too low (min 0)",
        )
        expects.expect_less_equal(
            report_payload.pressure,
            100,
            f"Pressure value {report_payload.pressure} is too high (max 100)",
        )

    def test_someip_publisher(self):
        # TODO(b/395067075): Update test to work with authz enabled
        self.enter_context(self.disable_authz(self.sdv_device))
        self._start_someip_service()
        self._run_simulation(
            self._SOMEIP_METRICS_CONFIG_PATH, timedelta(seconds=10)
        )

        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=self._SOMEIP_METRICS_CONFIG_UUID,
            report_name="TirePressureReport",
            report_number=1,
        )
        report = self.parse_binary_report(report_path)

        # Decode Report
        report_payload = self.decode_report_payload(
            self.someip_metrics_config.descriptor_protos, report
        )

        expects.expect_greater_equal(
            report_payload.pressure,
            10,
            f"Pressure value {report_payload.pressure} is too low (min 10)",
        )
        expects.expect_less_equal(
            report_payload.pressure,
            40,
            f"Pressure value {report_payload.pressure} is too high (max 40)",
        )

    def _run_simulation(self, metrics_config_path: Path, duration: timedelta):
        self.sdv_device.adb().log().info("Starting Simulator")
        self.sdv_device.adb().execute_shell_command(
            self._get_simulator_command(
                self.sdv_device, metrics_config_path, duration
            )
        )
        self.sdv_device.adb().log().info("Simulation finished")

    def _get_simulator_command(
        self,
        device: sdv_device.SdvDevice,
        metrics_config_path: Path,
        duration: timedelta,
    ) -> str:
        cd_command = shlex_join(["cd", str(self._SIMULATION_CONFIG_DIR)])
        simulator_command = shlex_join([
            self.get_simulator_binary(device),
            "--max-simulation-time",
            f"seconds:{duration.total_seconds()}",
            "telemetry-client",
            "--metrics-configs",
            str(metrics_config_path),
            "--output-directory",
            str(self._SIMULATOR_OUT_DIR),
        ])
        return f"{cd_command} && {simulator_command}"

    def _start_someip_service(self):
        self.sdv_device.adb().log().info("Starting SOMEIP service")
        self.sdv_device.adb().execute_shell_command_in_subprocess_log(
            "VSOMEIP_CONFIGURATION=/vendor/etc/vsomeip/tire_pressure_sample.json"
            " VSOMEIP_BASE_PATH=/data/vendor/vsomeip/"
            " /vendor/bin/sdv_vsomeip_tire_pressure_sample",
        )

    def _start_vsidl_metadata_service(self):
        self.sdv_device.adb().log().info(
            "Starting a service with VSIDL metadata"
        )
        self.sdv_device.adb().execute_shell_command_in_subprocess_log(
            "telemetry_sample_middleware_publisher"
        )


if __name__ == "__main__":
    sdv_test_runner.run()
