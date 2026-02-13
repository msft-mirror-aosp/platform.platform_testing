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

"""SDV E2E Telemetry Data Tunnel Fetch Last Message Test"""

from mobly import asserts
from pathlib import Path
import pprint
from random import randint
import re
from time import sleep
from typing import Any, List, TypedDict
from sdv_telemetry_test_execution import telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner
from system.software_defined_vehicle.telemetry.proto.metrics_configuration.metrics_configuration_pb2 import MetricsConfig, MetricsReport


class Report(TypedDict):
    report: MetricsReport
    payload: Any


class SdvE2ETelemetryDtFetchLastMessageTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
  _DT_PUBLISHER_BINARY = (
        'sdv_telemetry_dt_publisher_for_fetch_last_message_e2e_test'
    )

  _METRICS_CONFIG1_UUID = 'aaf7883c-96a5-4a67-adad-d15e00c0f12a'
  _METRICS_CONFIG2_UUID = 'baf7883c-96a5-4a67-adad-d15e00c0f12a'

  _METRICS_CONFIG_REPORT_NAME_FLM_TRUE = 'report_flm_true'
  _METRICS_CONFIG_REPORT_NAME_FLM_FALSE = 'report_flm_false'

  def get_simulator_command(
        self, device: sdv_device.SdvDevice, simulator_out_dir: Path
    ) -> str:
    return shlex_join([
            self.get_simulator_binary(device),
            '--max-simulation-time',
            'seconds:10',
            'telemetry-client',
            '--metrics-configs',
            str(self.metrics_config1_path),
            str(self.metrics_config2_path),
            '--output-directory',
            str(simulator_out_dir),
        ])

  def setup_class(self):
    super().setup_class()

    self.sdv_device1 = self.get_device('device1')
    self.sdv_device1.adb().root_device()

    self.sdv_device2 = self.get_device('device2')
    self.sdv_device2.adb().root_device()

    metrics_config_template = self.parse_textproto_metrics_config(
            Path('dt_fetch_last_message_metrics_config.textproto')
        )

    self.metrics_config1 = MetricsConfig()
    self.metrics_config1.CopyFrom(metrics_config_template)
    self.metrics_config1.uuid = self._METRICS_CONFIG1_UUID

    self.metrics_config2 = MetricsConfig()
    self.metrics_config2.CopyFrom(metrics_config_template)
    self.metrics_config2.uuid = self._METRICS_CONFIG2_UUID

  def setup_test(self):
    super().setup_test()

    # We need to reboot both VMs before each test, because we must ensure
    # that the Data Tunnel topic is not yet registered on any of the VMs.
    # Otherwise, this test will still pass, but not verify that b/441498744
    # is actually fixed.
    self.sdv_device1.adb().reboot_device()
    self.sdv_device2.adb().reboot_device()

    self.sdv_device1.adb().wait_for_device_online()
    self.sdv_device2.adb().wait_for_device_online()

    # TODO: b/395067075 - Update test to work with authz enabled.
    self.enter_context(self.disable_authz(self.sdv_device1))
    self.enter_context(self.disable_authz(self.sdv_device2))

    with self.create_host_temp_file() as metrics_config1_host_path, self.create_host_temp_file() as metrics_config2_host_path:
      metrics_config1_host_path.write_bytes(
                self.metrics_config1.SerializeToString()
            )
      metrics_config2_host_path.write_bytes(
                self.metrics_config2.SerializeToString()
            )

      device_temp_dir = self.enter_context(
                self.create_temp_dir(self.sdv_device1)
            )
      self.metrics_config1_path = device_temp_dir / 'metrics_config1.pb'
      self.metrics_config2_path = device_temp_dir / 'metrics_config2.pb'

      self.sdv_device1.adb().push([
                str(metrics_config1_host_path),
                str(self.metrics_config1_path),
            ])
      self.sdv_device1.adb().push([
                str(metrics_config2_host_path),
                str(self.metrics_config2_path),
            ])

  def test_fetch_last_message_same_vm(self):
    self.run_test(self.sdv_device1)

  def test_fetch_last_message_cross_vm(self):
    self.run_test(self.sdv_device2)

  def run_test(self, publisher_device):
    UNIT_NAMES = ['flmtrue', 'flmfalse']
    expected_value = randint(1000, 2**32 - 1)

    self.start_dt_publishers(publisher_device, UNIT_NAMES, expected_value)

    mc1_all_reports_flmtrue = list()
    mc1_all_reports_flmfalse = list()
    mc2_all_reports_flmtrue = list()
    mc2_all_reports_flmfalse = list()

    for _ in range(2):
      # The metrics config is always run on the main device.
      (
                mc1_reports_flmtrue,
                mc1_reports_flmfalse,
                mc2_reports_flmtrue,
                mc2_reports_flmfalse,
            ) = self.run_metrics_config(self.sdv_device1)

      self.sdv_device1.adb().log().info(
                f'Metrics Config 1: Reports FLM true: {mc1_reports_flmtrue}'
            )
      self.sdv_device1.adb().log().info(
                f'Metrics Config 1: Reports FLM false: {mc1_reports_flmfalse}'
            )
      self.sdv_device1.adb().log().info(
                f'Metrics Config 2: Reports FLM true: {mc2_reports_flmtrue}'
            )
      self.sdv_device1.adb().log().info(
                f'Metrics Config 2: Reports FLM false: {mc2_reports_flmfalse}'
            )

      mc1_all_reports_flmtrue.append(mc1_reports_flmtrue)
      mc1_all_reports_flmfalse.append(mc1_reports_flmfalse)
      mc2_all_reports_flmtrue.append(mc2_reports_flmtrue)
      mc2_all_reports_flmfalse.append(mc2_reports_flmfalse)

    error_msg = (
            'Unexpected reports received:\n\nMetrics Config 1 FLM'
            f' true:\n\n{pprint.pformat(mc1_all_reports_flmtrue, indent=4, width=120, sort_dicts=False)}\n\nMetrics'
            ' Config 1 FLM'
            f' false:\n\n{pprint.pformat(mc1_all_reports_flmfalse, indent=4, width=120, sort_dicts=False)}\n\nMetrics'
            ' Config 2 FLM'
            f' true:\n\n{pprint.pformat(mc2_all_reports_flmtrue, indent=4, width=120, sort_dicts=False)}\n\nMetrics'
            ' Config 2 FLM'
            f' false:\n\n{pprint.pformat(mc2_all_reports_flmfalse, indent=4, width=120, sort_dicts=False)}'
        )

    asserts.assert_equal(
        [
            (
                (
                    mc1_all_reports_flmtrue[i][0]['payload'].value,
                    mc1_all_reports_flmtrue[i][0]['payload'].HasField('value'),
                ),
                (
                    mc1_all_reports_flmfalse[i][0]['payload'].value,
                    mc1_all_reports_flmfalse[i][0]['payload'].HasField('value'),
                ),
                (
                    mc2_all_reports_flmtrue[i][0]['payload'].value,
                    mc2_all_reports_flmtrue[i][0]['payload'].HasField('value'),
                ),
                (
                    mc2_all_reports_flmfalse[i][0]['payload'].value,
                    mc2_all_reports_flmfalse[i][0]['payload'].HasField('value'),
                ),
            )
            for i in range(2)
        ],
        [
            (
                # Metrics Config 1
                # flmtrue
                (expected_value, True),
                # flmfalse
                (0, False),
                # Metrics Config 2
                # flmtrue
                (expected_value, True),
                # flmfalse
                (0, False),
            )
            for _ in range(2)
        ],
        error_msg,
    )

  def start_dt_publishers(
      self,
      device: sdv_device.SdvDevice,
      unit_names: List[str],
      expected_value: int,
  ) -> None:
    # Publish three messages (77, 12, expected_value). We should never see
    # the first and second message, since fetch_last_message is supposed to
    # only fetch the last message.
    for unit_name in unit_names:
      device.adb().execute_shell_command_in_subprocess(
                f'{self._DT_PUBLISHER_BINARY}_{unit_name}',
                shlex_join([
                    self._DT_PUBLISHER_BINARY,
                    '--unit-name',
                    unit_name,
                    '77',
                    '12',
                    str(expected_value),
                ]),
            )
      self.add_cleanup(
                lambda: device.adb().terminate_subprocess(
                    f'{self._DT_PUBLISHER_BINARY}_{unit_name}',
                )
            )

    # The publisher will idle forever after publishing its messages. The
    # `sleep` below is there for two reasons:
    # 1. We need to ensure that the message was really sent before running
    #    the Metrics Config --- sleeping for a bit reduces potential
    #    flakiness.
    # 2. We want to check that sending the message was successful (= the
    #    publisher did not crash). We do so by checking that it is still
    #    running after sleeping.
    sleep(3)
    for unit_name in unit_names:
      asserts.assert_true(
                device.adb().is_subprocess_running(
                    f'{self._DT_PUBLISHER_BINARY}_{unit_name}',
                ),
                f'Data Tunnel Publisher for unit_name "{unit_name}" should'
                ' still be running.',
            )

  def run_metrics_config(self, sdv_device: sdv_device.SdvDevice) -> tuple[
        List[Report],
        List[Report],
        List[Report],
        List[Report],
    ]:
    with self.create_temp_dir(sdv_device) as simulator_out_dir:
      sdv_device.adb().log().info('Starting Simulator')
      sdv_device.adb().execute_shell_command(
                self.get_simulator_command(sdv_device, simulator_out_dir)
            )
      sdv_device.adb().log().info('Simulation finished')

      return (
                self.get_reports(
                    sdv_device,
                    simulator_out_dir,
                    self.metrics_config1,
                    self._METRICS_CONFIG_REPORT_NAME_FLM_TRUE,
                ),
                self.get_reports(
                    sdv_device,
                    simulator_out_dir,
                    self.metrics_config1,
                    self._METRICS_CONFIG_REPORT_NAME_FLM_FALSE,
                ),
                self.get_reports(
                    sdv_device,
                    simulator_out_dir,
                    self.metrics_config2,
                    self._METRICS_CONFIG_REPORT_NAME_FLM_TRUE,
                ),
                self.get_reports(
                    sdv_device,
                    simulator_out_dir,
                    self.metrics_config2,
                    self._METRICS_CONFIG_REPORT_NAME_FLM_FALSE,
                ),
            )

  def get_reports(
        self,
        sdv_device: sdv_device.SdvDevice,
        simulator_out_dir: Path,
        metrics_config: MetricsConfig,
        report_name: str,
    ) -> List[Report]:
    report_dir = simulator_out_dir / 'reports'

    report_filenames: List[str] = (
            sdv_device.adb()
            .execute_shell_command(shlex_join(['ls', '-1q', str(report_dir)]))
            .strip()
            .split('\n')
        )
    sdv_device.adb().log().info(
            f'Found the following report files: {", ".join(report_filenames)}'
        )

    reports = list()
    for report_filename in report_filenames:
      if not re.match(
                rf'^{re.escape(metrics_config.uuid)}_{re.escape(report_name)}_\d+_.+[.]pb$',
                report_filename,
            ):
        continue

      with self.create_host_temp_file() as temp_file_path:
        sdv_device.adb().pull([
                    str(report_dir / report_filename),
                    str(temp_file_path),
                ])
        report = self.parse_binary_report(temp_file_path)

      reports.append({
                'report': report,
                'payload': self.decode_report_payload(
                    metrics_config.descriptor_protos, report
                ),
            })

    return reports


if __name__ == '__main__':
    sdv_test_runner.run()
