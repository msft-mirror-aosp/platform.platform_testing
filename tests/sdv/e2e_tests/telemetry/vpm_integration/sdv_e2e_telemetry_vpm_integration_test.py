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

"""SDV Telemetry VPM Integration Test"""

from datetime import timedelta
from pathlib import Path
import time
from time import sleep
from typing import List, Optional

from mobly import asserts
from sdv_telemetry_test_execution import expects, telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_test_runner


class SdvE2ETelemetryVpmIntegrationTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    METRICS_CONFIG_UUID = 'aa27883c-96a5-4a67-adad-dcce00c0f12a'
    METRICS_CONFIG_REPORT_NAME = 'values'
    SIMULATION_SUMMARY_FILE_NAME = f'{METRICS_CONFIG_UUID}_all_reports_for_{METRICS_CONFIG_REPORT_NAME.lower()}.txt'
    MAX_RESUMED_AT_JITTER = timedelta(seconds=5)

    # These cannot be in `/data/local/tmp`, because we reboot the VM as part of
    # the test, which would delete these files.
    METRICS_CONFIG_PATH = Path(
        '/data/local/vpm_integration_metrics_config.textproto'
    )
    SIMULATION_PUBLISHER_CONFIG_PATH = Path(
        '/data/local/counting_publisher_config.textproto'
    )

    def get_simulator_command(
        self,
        device: sdv_device.SdvDevice,
        simulator_out_dir: Path,
    ) -> str:
        return shlex_join([
            self.get_simulator_binary(device),
            '--max-simulation-time',
            'seconds:120',
            'full-simulation',
            '--metrics-configs',
            str(self.METRICS_CONFIG_PATH),
            '--publisher-configs',
            str(self.SIMULATION_PUBLISHER_CONFIG_PATH),
            '--max-report-count',
            '1',
            '--no-binary-reports',
            '--output-directory',
            str(simulator_out_dir),
        ])

    def teardown_class(self):
        self.sdv_device.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.original_authz_enable)
        super().teardown_class()

    def setup_class(self):
        super().setup_class()

        self.sdv_device = self.get_device('device1')

        self.original_authz_enable = self.sdv_device.adb().prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        self.sdv_device.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'permissions_only')

        self.sdv_device.adb().root_device()

    def execute_vepsm_command(self, *args: List[str]) -> None:
        def terminate():
            # TODO: b/399673375 - This does not work for whatever reason.
            # self.sdv_device.adb().terminate_subprocess(log_file)

            # TODO: b/428207473 Update the test to use interactive sessions similar to
            # e2e_tests/vpm/sdv-vpm-power-suspend.py
            self.sdv_device.adb().execute_shell_command(
                shlex_join(['pkill', '-f', 'vepsm'])
            )

        while True:
            log_file = (
                self.sdv_device.adb().execute_shell_command_in_subprocess_log(
                    shlex_join(['vepsm', *args])
                )
            )

            for _ in range(10):
                log = self.sdv_device.adb().read_file(log_file)
                if 'Received power-state-report' in log:
                    terminate()
                    return
                else:
                    self.sdv_device.adb().log().info(
                        'Waiting for output of vepsm command. Current'
                        f' output:\n\n{log}'
                    )
                sleep(0.5)

            terminate()

    def suspend_and_resume(self):
        self.execute_vepsm_command('power-state', 'power-on')
        self.execute_vepsm_command('power-state', 'prepare', 'ram')

        # Automatically wake up the VM in 5 seconds from now.
        #
        # TODO: b/393555389 - This could potentially be rewritten to use the
        # equivalent to `cvd powerbtn` instead.
        self.sdv_device.adb().execute_shell_command(
            shlex_join(['rtcwake', '-m', 'no', '-s', '5'])
        )
        # This vepsm command causes the VM to suspend. The rtcwake command above
        # causes the VM to wake up again.
        self.sdv_device.adb().log().info(f"Suspending (uptime: {self.get_uptime().seconds} seconds).")
        self.execute_vepsm_command('power-state', 'finish', 'ram')
        self.sdv_device.adb().log().info(f"Resuming (uptime: {self.get_uptime().seconds} seconds).")

    def run_simulator(self) -> str:
        with self.create_temp_dir(self.sdv_device) as simulator_out_dir:
            self.sdv_device.adb().log().info(f'Starting Simulator (uptime: {self.get_uptime().seconds} seconds).')
            self.sdv_device.adb().execute_shell_command(
                self.get_simulator_command(
                    self.sdv_device,
                    simulator_out_dir,
                )
            )
            self.sdv_device.adb().log().info(f'Simulation finished (uptime: {self.get_uptime().seconds} seconds).')

            return self.sdv_device.adb().read_file(
                simulator_out_dir / self.SIMULATION_SUMMARY_FILE_NAME
            )

    def expect_full_report(self, report: str) -> None:
        # Expect at least 10 seconds worth of values.
        expected_result_regex = (
            'Metrics config "vpm_integration_metrics_config[.]textproto", UUID:'
            f' "{self.METRICS_CONFIG_UUID}"(?s:.*?)Report configuration'
            f' "{self.METRICS_CONFIG_REPORT_NAME}"(?s:.*?)Report #1 created'
            ' on.*?\\n+(\\s*values: \\d+\\n+){5,}'
        )
        expects.expect_regex(
            report,
            expected_result_regex,
            'Report summary does not match expected'
            f' regex:\n{expected_result_regex}\n\nActual'
            f' result:\n{report}',
        )

    def expect_empty_report(self, report: str) -> None:
        # Expect no values.
        expected_result_regex = (
            'Metrics config "vpm_integration_metrics_config[.]textproto", UUID:'
            f' "{self.METRICS_CONFIG_UUID}"(?s:.*?)Report configuration'
            f' "{self.METRICS_CONFIG_REPORT_NAME}"(?s:.*?)Report #1 created'
            ' on[^\\n]*$'
        )
        expects.expect_regex(
            report,
            expected_result_regex,
            'Report summary does not match expected'
            f' regex:\n{expected_result_regex}\n\nActual'
            f' result:\n{report}',
        )

    def get_uptime(self) -> timedelta:
        # The file contains two numbers separated by a space, the first of which
        # is the uptime as a float.
        # https://man7.org/linux/man-pages/man5/proc_uptime.5.html
        uptime_file = self.sdv_device.adb().read_file('/proc/uptime')
        return timedelta(seconds=float(uptime_file.split(' ')[0]))

    def restart_telemetry_service(self) -> None:
        # Restart the Telemetry Service by killing it (it will restart
        # automatically).
        self.sdv_device.adb().execute_shell_command(
            shlex_join([
                'pkill',
                '-f',
                'sdv_telemetry_service_agent',
            ])
        )
        # Give it some time to restart.
        time.sleep(3)

    def expect_within(
        self,
        resumed_at_real: timedelta,
        resumed_at_prop: timedelta,
        delta: timedelta,
    ) -> None:
        expects.expect_less_equal(
            abs(resumed_at_real - resumed_at_prop).total_seconds(),
            delta.total_seconds(),
            'The value of the system property'
            f' {SdvDeviceProperty.TELEMETRY_RESUMED_AT_TIMESTAMP.value}'
            f' ({resumed_at_prop}) should be within {delta} of the uptime at'
            f' the point of resuming ({resumed_at_real})',
        )

    def read_resumed_at_prop(self) -> Optional[timedelta]:
        prop_value = self.sdv_device.adb().prop.get(SdvDeviceProperty.TELEMETRY_RESUMED_AT_TIMESTAMP)
        if prop_value == '':
            return None

        return timedelta(microseconds=int(prop_value))

    def test_vpm_suspend(self):
        # The Metrics config is written in a way where it will only fill the
        # report with values during the first X seconds after boot / resume, and
        # then finish. As such, the report should not be empty when running the
        # simulation right after boot / resume, but it should be empty (and the
        # metrics config should finish immediately) if run a second time.

        self.sdv_device.adb().log().info(f"Rebooting device (uptime: {self.get_uptime().seconds} seconds).")
        self.sdv_device.adb().reboot_device()
        self.sdv_device.adb().log().info(f"Reboot completed (uptime: {self.get_uptime().seconds} seconds).")

        self.expect_full_report(self.run_simulator())
        self.expect_empty_report(self.run_simulator())

        asserts.assert_equal(
            self.read_resumed_at_prop(), None
        )
        self.sdv_device.adb().root_device()
        self.suspend_and_resume()
        resumed_at = self.get_uptime()

        # The Telemetry Service should update the system property after
        # resuming. Give it some time to set the property.
        time.sleep(
            self.MAX_RESUMED_AT_JITTER.total_seconds()
        )
        self.expect_within(
            resumed_at_real=resumed_at,
            resumed_at_prop=self.read_resumed_at_prop(),
            delta=self.MAX_RESUMED_AT_JITTER,
        )

        self.expect_full_report(self.run_simulator())
        self.expect_empty_report(self.run_simulator())

        # Even when we restart the Telemetry Service, the report should still be
        # empty, because the Telemetry Service should re-initialize the last
        # resumed at time from the system property.
        self.restart_telemetry_service()

        self.expect_empty_report(self.run_simulator())
        # The property should remain unchanged.
        self.expect_within(
            resumed_at_real=resumed_at,
            resumed_at_prop=self.read_resumed_at_prop(),
            delta=self.MAX_RESUMED_AT_JITTER,
        )

        # The property should be reset on reboot.
        self.sdv_device.adb().reboot_device()
        asserts.assert_equal(
            self.read_resumed_at_prop(), None
        )


if __name__ == '__main__':
    sdv_test_runner.run()
