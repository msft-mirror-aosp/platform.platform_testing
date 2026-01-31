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

"""E2E Test to validate operators used in Telemetry Service expression evaluations"""

from mobly import asserts
from datetime import timedelta
from pathlib import Path
from sdv_telemetry_test_execution import telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner


class SdvE2ETelemetryOperatorsTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    SIMULATION_TIME = timedelta(seconds=2)

    METRICS_CONFIG_UUID = "f910f4ef-573f-40c6-b75e-289ce8f67b1d"

    _SIMULATION_CONFIG_DIR = Path("/data/local/tmp/")
    _SIMULATOR_OUT_DIR = Path("/data/local/tmp/out")
    _METRICS_CONFIG_FILE_NAME = "operators_metrics_config.textproto"
    _METRICS_CONFIG_PATH = _SIMULATION_CONFIG_DIR / _METRICS_CONFIG_FILE_NAME
    _METRICS_PUBLISHER_PATH = (
        _SIMULATION_CONFIG_DIR / "operators_publisher.textproto"
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

    def test_operators(self):
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

        self._validate_integer_report()
        self._validate_integer_list_report()
        self._validate_float_report()
        self._validate_boolean_report()

    def _validate_integer_report(self):
        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=self.METRICS_CONFIG_UUID,
            report_name="integer_report",
            report_number=1,
        )
        report = self.parse_binary_report(report_path)

        # Decode Report
        report_payload = self.decode_report_payload(
            self.metrics_config.descriptor_protos, report
        )

        # Evaluate payload
        asserts.assert_equal(
            report.metrics_config_uuid,
            self.METRICS_CONFIG_UUID,
            "Unexpected UUID",
        )
        # value of integer
        asserts.assert_equal(
            report_payload.integer, 30, "Unexpected value of integer"
        )

        # Relational operators
        # integer == 30.0
        asserts.assert_equal(
            report_payload.relational_equal,
            True,
            "Unexpected evaluation of operator 'Equal'",
        )
        # integer != 30
        asserts.assert_equal(
            report_payload.relational_not_equal,
            False,
            "Unexpected evaluation of operator 'Not Equal'",
        )
        # integer > 30
        asserts.assert_equal(
            report_payload.relational_greater_than,
            False,
            "Unexpected evaluation of operator 'Greater'",
        )
        # 30 >= integer
        asserts.assert_equal(
            report_payload.relational_greater_equal_than,
            True,
            "Unexpected evaluation of operator 'Greater or Equal'",
        )
        # integer < 30
        asserts.assert_equal(
            report_payload.relational_less_than,
            False,
            "Unexpected evaluation of operator 'Less'",
        )
        # integer <= 30
        asserts.assert_equal(
            report_payload.relational_less_equal_than,
            True,
            "Unexpected evaluation of operator 'Less or Equal'",
        )

        # Arithmetic operators
        # integer + 2.5
        asserts.assert_equal(
            report_payload.arithmetic_add,
            32.5,
            "Unexpected evaluation of operator 'Add'",
        )
        # 10 - integer
        asserts.assert_equal(
            report_payload.arithmetic_subtract,
            -20,
            "Unexpected evaluation of operator 'Subtract'",
        )
        # 7 * integer
        asserts.assert_equal(
            report_payload.arithmetic_multiply,
            210,
            "Unexpected evaluation of operator 'Multiply'",
        )
        # integer / 20
        asserts.assert_equal(
            report_payload.arithmetic_divide,
            1.5,
            "Unexpected evaluation of operator 'Divide'",
        )
        # -integer
        asserts.assert_equal(
            report_payload.arithmetic_unary_minus,
            -30,
            "Unexpected evaluation of operator 'Unary Minus'",
        )
        # integer % 7
        asserts.assert_equal(
            report_payload.arithmetic_modulo_trunc,
            2,
            "Unexpected evaluation of operator 'Modulo Trunc'",
        )
        # -42 % (-integer)
        asserts.assert_equal(
            report_payload.arithmetic_modulo_trunc_negative,
            -12,
            "Unexpected evaluation of operator 'Modulo Trunc' with negative"
            " arguments",
        )
        # integer ** 2
        asserts.assert_equal(
            report_payload.arithmetic_power_2,
            900,
            "Unexpected evaluation of operator 'Power' with exponent 2",
        )
        # integer ** 1.5
        asserts.assert_equal(
            round(report_payload.arithmetic_power_1_5, 2),
            164.32,
            "Unexpected evaluation of operator 'Power' with exponent 1.5",
        )
        # integer ** (-1)
        asserts.assert_equal(
            round(report_payload.arithmetic_power_minus_1, 3),
            0.033,
            "Unexpected evaluation of operator 'Power' with exponent -1",
        )
        # abs(integer)
        asserts.assert_equal(
            report_payload.arithmetic_absolute_positive,
            30,
            "Unexpected evaluation of operator 'Absolute' with positive"
            " argument",
        )
        # abs(-integer)
        asserts.assert_equal(
            report_payload.arithmetic_absolute_negative,
            30,
            "Unexpected evaluation of operator 'Absolute' with negative"
            " argument",
        )

    def _validate_integer_list_report(self):
        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=self.METRICS_CONFIG_UUID,
            report_name="integer_list_report",
            report_number=1,
        )
        report = self.parse_binary_report(report_path)

        # Decode Report
        report_payload = self.decode_report_payload(
            self.metrics_config.descriptor_protos, report
        )

        # Evaluate payload
        asserts.assert_equal(
            report.metrics_config_uuid,
            self.METRICS_CONFIG_UUID,
            "Unexpected UUID",
        )
        # value of integer_list
        asserts.assert_equal(
            report_payload.integer_list,
            [30],
            "Unexpected value of integer_list",
        )
        # contains(integer_list, 30)
        asserts.assert_equal(
            report_payload.relational_contains_30,
            True,
            "Unexpected evaluation of operator 'Contains' with argument 30",
        )
        # contains(integer_list, 30.0)
        asserts.assert_equal(
            report_payload.relational_contains_30_0,
            False,
            "Unexpected evaluation of operator 'Contains' with argument 30.0",
        )
        # contains(integer_list, 10)
        asserts.assert_equal(
            report_payload.relational_contains_10,
            False,
            "Unexpected evaluation of operator 'Contains' with argument 10",
        )
        # doesnotcontain(integer_list, 30)
        asserts.assert_equal(
            report_payload.relational_does_not_contain,
            False,
            "Unexpected evaluation of operator 'Does Not Contain'",
        )
        # allequal(integer_list, 30)
        asserts.assert_equal(
            report_payload.relational_all_equal_to,
            True,
            "Unexpected evaluation of operator 'All Equal'",
        )

    def _validate_float_report(self):
        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=self.METRICS_CONFIG_UUID,
            report_name="float_report",
            report_number=1,
        )
        report = self.parse_binary_report(report_path)

        # Decode Report
        report_payload = self.decode_report_payload(
            self.metrics_config.descriptor_protos, report
        )

        # Evaluate payload
        asserts.assert_equal(
            report.metrics_config_uuid,
            self.METRICS_CONFIG_UUID,
            "Unexpected UUID",
        )
        # value of float
        asserts.assert_equal(
            report_payload.float, 30.5, "Unexpected value of float"
        )

        # Rounding operators
        # floor(float)
        asserts.assert_equal(
            report_payload.rounding_floor,
            30,
            "Unexpected evaluation of operator 'Floor'",
        )
        # round(float)
        asserts.assert_equal(
            report_payload.rounding_round,
            31,
            "Unexpected evaluation of operator 'Round'",
        )
        # ceil(float)
        asserts.assert_equal(
            report_payload.rounding_ceil,
            31,
            "Unexpected evaluation of operator 'Ceil'",
        )

    def _validate_boolean_report(self):
        # Load Report
        report_path = self.pull_report(
            device=self.sdv_device,
            simulator_out_dir=self._SIMULATOR_OUT_DIR,
            config_uuid=self.METRICS_CONFIG_UUID,
            report_name="boolean_report",
            report_number=1,
        )
        report = self.parse_binary_report(report_path)

        # Decode Report
        report_payload = self.decode_report_payload(
            self.metrics_config.descriptor_protos, report
        )

        # Evaluate payload
        asserts.assert_equal(
            report.metrics_config_uuid,
            self.METRICS_CONFIG_UUID,
            "Unexpected UUID",
        )
        # value of boolean
        asserts.assert_equal(
            report_payload.boolean, True, "Unexpected value of boolean"
        )

        # Logical Operators
        # NOT boolean
        asserts.assert_equal(
            report_payload.logical_not_true,
            False,
            "Unexpected evaluation of `NOT true`",
        )
        # NOT (NOT boolean)
        asserts.assert_equal(
            report_payload.logical_not_false,
            True,
            "Unexpected evaluation of `NOT false`",
        )
        # In the metrics configuration, expressions below are defined using `boolean` message field as True, and `!boolean` as False
        # true AND true
        asserts.assert_equal(
            report_payload.logical_and_true_true,
            True,
            "Unexpected evaluation of `true AND true`",
        )
        # true AND false
        asserts.assert_equal(
            report_payload.logical_and_true_false,
            False,
            "Unexpected evaluation of `true AND false`",
        )
        # false AND true
        asserts.assert_equal(
            report_payload.logical_and_false_true,
            False,
            "Unexpected evaluation of `false AND true`",
        )
        # false AND false
        asserts.assert_equal(
            report_payload.logical_and_false_false,
            False,
            "Unexpected evaluation of `false AND false`",
        )
        # true OR true
        asserts.assert_equal(
            report_payload.logical_or_true_true,
            True,
            "Unexpected evaluation of `true OR true`",
        )
        # true OR false
        asserts.assert_equal(
            report_payload.logical_or_true_false,
            True,
            "Unexpected evaluation of `true OR false`",
        )
        # false OR true
        asserts.assert_equal(
            report_payload.logical_or_false_true,
            True,
            "Unexpected evaluation of `false OR true`",
        )
        # false OR false
        asserts.assert_equal(
            report_payload.logical_or_false_false,
            False,
            "Unexpected evaluation of `false OR false`",
        )
        # true XOR true
        asserts.assert_equal(
            report_payload.logical_xor_true_true,
            False,
            "Unexpected evaluation of `true XOR true`",
        )
        # true XOR false
        asserts.assert_equal(
            report_payload.logical_xor_true_false,
            True,
            "Unexpected evaluation of `true XOR false`",
        )
        # false XOR true
        asserts.assert_equal(
            report_payload.logical_xor_false_true,
            True,
            "Unexpected evaluation of `false XOR true`",
        )
        # false XOR false
        asserts.assert_equal(
            report_payload.logical_xor_false_false,
            False,
            "Unexpected evaluation of `false XOR false`",
        )


if __name__ == "__main__":
    sdv_test_runner.run()
