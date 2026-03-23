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

"""SDV Telemetry APEX Update Test"""

from pathlib import Path
import re
from time import sleep

from mobly import asserts
from sdv_telemetry_test_execution import expects, telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.device import sdv_device
from sdv_test_fw.test_execution import sdv_test_runner
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from update_manager_client import UpdateManagerClient


class SdvE2ETelemetryApexUpdateTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):
    APEX_NAME = 'com.android.sdv.sample.apex.provider'
    APEX_NAME_V2 = f'{APEX_NAME}.v2'

    METRICS_CONFIG_UUID = 'aa27883c-96a5-4a67-adad-d15e00c0f12a'
    DT_METRICS_CONFIG_REPORT_NAME = 'dt_tire_pressure'
    DT_SIMULATION_SUMMARY_FILE_NAME = f'{METRICS_CONFIG_UUID}_all_reports_for_{DT_METRICS_CONFIG_REPORT_NAME.lower()}.txt'
    RPC_METRICS_CONFIG_REPORT_NAME = 'rpc_tire_pressure'
    RPC_SIMULATION_SUMMARY_FILE_NAME = f'{METRICS_CONFIG_UUID}_all_reports_for_{RPC_METRICS_CONFIG_REPORT_NAME.lower()}.txt'

    # For some reason, this path cannot be in `/data/local/tmp`, because that
    # produces an error when trying to prepare the APEX:
    #
    # Failed to verify signature of
    # /data/local/tmp/com.android.sdv.sample.apex.provider.v2.apex: Permission
    # denied (os error 13)
    APEX_V2_PATH = Path(f'/data/local/{APEX_NAME_V2}.apex')
    METRICS_CONFIG_PATH = Path(
        '/data/local/tmp/apex_update_metrics_config.textproto'
    )

    def get_simulator_command(
        self, device: sdv_device.SdvDevice, simulator_out_dir: Path
    ) -> str:
        return shlex_join([
            self.get_simulator_binary(device),
            '--max-simulation-time',
            'seconds:15',
            'telemetry-client',
            '--metrics-configs',
            str(self.METRICS_CONFIG_PATH),
            '--no-binary-reports',
            '--output-directory',
            str(simulator_out_dir),
        ])

    def get_apex_version_number(
        self, update_manager_client: UpdateManagerClient
    ) -> int:
        active_apex_file_path = update_manager_client.adb.execute_shell_command(
            shlex_join(['find', '/apex', '-name', f'{self.APEX_NAME}@*'])
        )
        return int(active_apex_file_path.rsplit('@', 1)[-1])

    def assert_update_manager_status(
        self, update_manager_client: UpdateManagerClient, expected_status: str
    ) -> None:
        status_text = update_manager_client.status()
        asserts.assert_in(expected_status, status_text)

    def run_metrics_config(self, sdv_device) -> None:
        with self.create_temp_dir(sdv_device) as simulator_out_dir:
            sdv_device.adb().log().info('Starting Simulator')
            sdv_device.adb().execute_shell_command(
                self.get_simulator_command(sdv_device, simulator_out_dir)
            )
            sdv_device.adb().log().info('Simulation finished')

            dt_result = sdv_device.adb().read_file(
                simulator_out_dir / self.DT_SIMULATION_SUMMARY_FILE_NAME
            )
            rpc_result = sdv_device.adb().read_file(
                simulator_out_dir / self.RPC_SIMULATION_SUMMARY_FILE_NAME
            )

        def make_report_summary_regex(report_name: str) -> str:
            return (
                rf'Metrics config "{re.escape(self.METRICS_CONFIG_PATH.name)}",'
                r' UUID:'
                rf' "{re.escape(self.METRICS_CONFIG_UUID)}"(?s:.*?)'
                rf'Report configuration "{re.escape(report_name)}"(?s:.*?)'
                r'Report #1 created on.*?\n+'
                r'\s*tire_pressure_fl: \d+'
            )

        expects.expect_regex(
            dt_result,
            make_report_summary_regex(self.DT_METRICS_CONFIG_REPORT_NAME),
            'Unexpected DT report summary received.',
        )
        expects.expect_regex(
            rpc_result,
            make_report_summary_regex(self.RPC_METRICS_CONFIG_REPORT_NAME),
            'Unexpected RPC report summary received.',
        )

    def kill_apex_if_it_is_running(self, sdv_device) -> None:
        process_id = None

        # Wait for up to 10 seconds for the apex to be running.
        for _ in range(10):
            try:
                process_id = sdv_device.adb().execute_shell_command(
                    shlex_join(['pgrep', '-f', f'{self.APEX_NAME}.Provider'])
                )
                break
            except:
                sleep(1)
                pass
        if process_id is None:
            # Apex still not running, all good.
            return

        asserts.assert_is_not_none(process_id)

        sdv_device.adb().execute_shell_command(shlex_join(['kill', process_id]))

        # Ensure that the device is rebooted after the test finishes, so that
        # the service is running again for follow-up tests.
        self.add_cleanup(lambda: sdv_device.adb().reboot_device())

    def update_apex_and_reboot(
        self, update_manager_client: UpdateManagerClient
    ) -> None:
        update_manager_client.prepare_service_bundle_update(
            [self.APEX_V2_PATH], 1
        )
        self.assert_update_manager_status(
            update_manager_client, 'PREPARE_COMPLETE'
        )

        update_manager_client.activate()
        self.assert_update_manager_status(
            update_manager_client, 'ACTIVATE_PRE_REBOOT_COMPLETE'
        )

        update_manager_client.adb.reboot_device()
        self.assert_update_manager_status(
            update_manager_client, 'ACTIVATE_POST_REBOOT_COMPLETE'
        )

        # Revert the update on test teardown.
        def cleanup():
            update_manager_client.rollback()
            update_manager_client.adb.reboot_device()

            asserts.assert_equal(
                self.get_apex_version_number(update_manager_client), 1
            )
            self.assert_update_manager_status(update_manager_client, 'READY')

        self.add_cleanup(cleanup)

    def run_test(
        self,
        update_manager_client: UpdateManagerClient,
        kill_apex_device,
    ):
        asserts.assert_equal(
            self.get_apex_version_number(update_manager_client), 1
        )

        # The Telemetry service does currently not support subscribing to a
        # service that runs on two VMs at the same time. Therefore, we kill the
        # service running on one of the VMs before running the metrics config.
        self.kill_apex_if_it_is_running(kill_apex_device)
        # The metrics config is always run on the main device.
        self.run_metrics_config(self.sdv_device1)

        self.update_apex_and_reboot(update_manager_client)

        asserts.assert_equal(
            self.get_apex_version_number(update_manager_client), 2
        )

        # Updating and rebooting may trigger the orchestrator to start the
        # killed apex again.
        self.kill_apex_if_it_is_running(kill_apex_device)
        # The metrics config is always run on the main device.
        self.run_metrics_config(self.sdv_device1)

    def setup_class(self):
        super().setup_class()

        self.sdv_device1 = self.get_device('device1')

        self.original_authz_enable1 = self.sdv_device1.adb().prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        self.sdv_device1.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'permissions_only')

        self.sdv_device1.adb().root_device()

        self.sdv_device2 = self.get_device('device2')

        self.original_authz_enable2 = self.sdv_device2.adb().prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        self.sdv_device2.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'permissions_only')

        self.sdv_device2.adb().root_device()

        self.update_manager_client1 = UpdateManagerClient(
            self.sdv_device1.adb()
        )
        self.update_manager_client2 = UpdateManagerClient(
            self.sdv_device2.adb()
        )

    def teardown_class(self):
        # Custom teardown here
        self.sdv_device1.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.original_authz_enable1)
        self.sdv_device2.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.original_authz_enable2)
        super().teardown_class()

    def setup_test(self):
        super().setup_test()

        # If a test is unexpectedly interrupted, Simulator may not remove
        # metrics configs located in configs directory. In this case, running
        # the next test will read the existing config from disk at Telemetry
        # Service initialization, while the same config will also be added by
        # the test itself leading to duplicate config UUIDs error.
        #
        # We manually clean up configs directory and restart Telemetry Service
        # for each test to avoid that.

        num_configs = self.sdv_device1.adb().execute_shell_command(
            'rm -vf /data/vendor/telemetry/*active_configs/* | wc -l'
        )
        if int(num_configs) != 0:
            self.sdv_device1.adb().log().info(
                f'Removed {num_configs} configs from /data/vendor/telemetry/, '
                'rebooting the device to restart Telemetry Service'
            )
            self.sdv_device1.adb().reboot_device()

    def teardown_test(self):
        # Custom teardown here
        super().teardown_test()

    def test_subscribe_to_apex_publisher_on_same_vm_before_and_after_update(
        self,
    ):
        self.run_test(self.update_manager_client1, self.sdv_device2)

    def test_subscribe_to_apex_publisher_on_other_vm_before_and_after_update(
        self,
    ):
        self.run_test(self.update_manager_client2, self.sdv_device1)


if __name__ == '__main__':
    sdv_test_runner.run()
