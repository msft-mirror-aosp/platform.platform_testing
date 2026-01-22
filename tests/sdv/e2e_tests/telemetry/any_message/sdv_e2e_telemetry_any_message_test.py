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

"""E2E Test to test reporting messages as google.protobuf.any"""

from mobly import asserts
from datetime import timedelta
from math import isclose
from pathlib import Path
from google.protobuf import any_pb2, descriptor_pb2
from sdv_telemetry_test_execution import telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner
from vendor.google_testing.software_defined_vehicle.tests.e2e_tests.telemetry.any_message.message_pb2 import Message


class SdvE2ETelemetryAnyMessageTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    _SIMULATION_TIME = timedelta(seconds=2)
    _SIMULATION_CONFIG_DIR = Path("/data/local/tmp/")
    _SIMULATOR_OUT_DIR = Path("/data/local/tmp/out")

    _METRICS_CONFIG_UUID = "ffb6e2ed-93d9-4fb6-b182-d0582f86764f"
    _METRICS_CONFIG_FILE_NAME = "any_message_config.textproto"
    _METRICS_CONFIG_PATH = _SIMULATION_CONFIG_DIR / _METRICS_CONFIG_FILE_NAME

    _PUBLISHER_PATH = _SIMULATION_CONFIG_DIR / "publisher.textproto"

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

    def test_any_message(self):
        self._run_simulation()

        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=self._METRICS_CONFIG_UUID,
            report_name="message_report",
            report_number=1,
        )
        report = self.parse_binary_report(report_path)

        # Collect required descriptor protos
        any_descriptor_proto = descriptor_pb2.FileDescriptorProto()
        any_pb2.DESCRIPTOR.CopyToProto(any_descriptor_proto)
        descriptor_protos = [any_descriptor_proto] + list(
            self.metrics_config.descriptor_protos
        )

        # Decode Report
        report_payload = self.decode_report_payload(descriptor_protos, report)

        # Unpack report's message_field of type Any into strongly typed Message
        message = Message()
        asserts.assert_true(
            report_payload.message_field.Unpack(message),
            "Unable to unpack Any into Message",
        )

        # Validate correctness of received message
        asserts.assert_equal(
            message.int_value, 42, "Unexpected int_value received"
        )
        asserts.assert_true(
            isclose(message.float_value, -0.32, rel_tol=1e-6, abs_tol=1e-6),
            f"Unexpected float_value received: {message.float_value}, expected:"
            f" {-0.32}",
        )
        asserts.assert_equal(
            message.string_value,
            "Test Message",
            "Unexpected string_value received",
        )

    def _run_simulation(self):
        self.sdv_device.adb().log().info("Starting Simulator")
        self.sdv_device.adb().execute_shell_command(
            self._get_simulator_command(self.sdv_device)
        )
        self.sdv_device.adb().log().info("Simulation finished")

    def _get_simulator_command(self, device: sdv_device.SdvDevice) -> str:
        cd_command = shlex_join(["cd", str(self._SIMULATION_CONFIG_DIR)])
        simulator_command = shlex_join([
            self.get_simulator_binary(device),
            "--max-simulation-time",
            f"seconds:{self._SIMULATION_TIME.total_seconds()}",
            "full-simulation",
            "--metrics-configs",
            str(self._METRICS_CONFIG_PATH),
            "--publisher-configs",
            str(self._PUBLISHER_PATH),
            "--output-directory",
            str(self._SIMULATOR_OUT_DIR),
        ])
        return f"{cd_command} && {simulator_command}"


if __name__ == "__main__":
    sdv_test_runner.run()
