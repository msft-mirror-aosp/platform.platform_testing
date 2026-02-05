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

"""
SDV Car Monitor Test App Sample Test
"""
import re
import logging
import time

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling
from mobly.controllers.android_device_lib.adb import AdbError

class SdvSampleCarMonitorAppTest(sdv_base_test.SdvBaseTestClass):

    OPEN_APP_CMD = 'am start com.android.testapp.sdvcarmonitor/.MainActivity'
    CLOSE_APP_CMD = 'am force-stop com.android.testapp.sdvcarmonitor'
    BROADCAST_STOP_GATEWAY_CLIENT_CMD = 'am broadcast -a com.android.testapp.sdvcarmonitor.ACTION_HANDLE_EVENT --es OPERATION_TYPE "stop_gateway"'
    BROADCAST_START_GATEWAY_CLIENT_CMD = 'am broadcast -a com.android.testapp.sdvcarmonitor.ACTION_HANDLE_EVENT --es OPERATION_TYPE "start_gateway"'
    BROADCAST_CAR_SEATS_EVENT_CMD = 'am broadcast -a com.android.testapp.sdvcarmonitor.ACTION_HANDLE_EVENT --es OPERATION_TYPE "car_seats"'
    BROADCAST_HVAC_TEMPERATURE_INCREASE_EVENT_CMD = 'am broadcast -a com.android.testapp.sdvcarmonitor.ACTION_HANDLE_EVENT --es OPERATION_TYPE "hvac_temperature_increase"'
    BROADCAST_PUBLISH_TIRE_PRESSURE_EVENT_CMD = 'am broadcast -a com.android.testapp.sdvcarmonitor.ACTION_HANDLE_EVENT --es OPERATION_TYPE "tire_pressure" --es OPERATION_VALUE "37"'
    BROADCAST_TEST_INTERNET_CONNECTIVITY_EVENT_CMD = 'am broadcast -a com.android.testapp.sdvcarmonitor.ACTION_HANDLE_EVENT --es OPERATION_TYPE "test_internet_connectivity"'

    CLUSTER_SERVER_CMD = 'sdv_mw_cluster_server'
    TPMS_SERVER_CMD = 'sdv_mw_tpms_server'
    DT_PUBLISHER_CMD = 'sdv_dt_mw_publisher_rs'
    DT_SUBSCRIBER_CMD = 'sdv_dt_mw_subscriber_rs'
    SUNROOF_CLIENT_CMD = 'sdv_mw_sunroof_client'

    NATIVE_CLIENT_LOG_TAG = 'libsdvgatewayclient:'
    CAR_MONITOR_APP_LOG_TAG = 'SdvCarMonitorTestApp:'
    CLUSTER_SERVER_LOG_TAG = 'cluster_server:'
    TPMS_SERVER_LOG_TAG = 'tpms_server:'
    DT_PUBLISHER_LOG_TAG = 'dt_mw_publisher:'
    DT_SUBSCRIBER_LOG_TAG = 'dt_mw_subscriber:'
    SUNROOF_SERVER_LOG_TAG = 'SunroofGrpcServer:'
    SUNROOF_CLIENT_LOG_TAG = 'sdv_sunroof_client:'

    EXPECTED_NATIVE_CLIENT_STOP_LOGS = [
        'libsdvgatewayclient: Destroyed publication',
        'libsdvgatewayclient: Unregistered service unit',
        'libsdvgatewayclient: NotificationClient: Shutdown',
    ]
    EXPECTED_NATIVE_CLIENT_START_LOGS = [
        'libsdvgatewayclient: loopNotificationDispatcher started',
        'libsdvgatewayclient: Bind process to RPC network handle',
        'libsdvgatewayclient: Registered service unit',
        'libsdvgatewayclient: Registered publication',
    ]
    EXPECTED_CAR_MONITOR_APP_STOP_OPERATION_LOG = ['SdvCarMonitorTestApp: Received operationType: stop_gateway']
    EXPECTED_CAR_MONITOR_APP_START_LOG = ['SdvCarMonitorTestApp: Received operationType: start_gateway']
    EXPECTED_CAR_MONITOR_APP_LOG_REGEX = [
        'SdvCarMonitorTestApp: Changing TpmsState to TPMS_STATE_(LOW|GOOD)',
        'SdvCarMonitorTestApp: Changing driving state to DRIVING_STATE_(DRIVE|LOW|NEUTRAL|PARK|REVERSE)',
    ]
    EXPECTED_CLUSTER_SERVER_LOG_REGEX = [
        'cluster_server: sdv_cluster_server: Cluster server set driving state to DRIVING_STATE_(DRIVE|LOW|NEUTRAL|PARK|REVERSE)',
    ]
    EXPECTED_TPMS_SERVER_LOG_REGEX = [
        'tpms_server: sdv_tpms_server: Tpms server set tpms state to TPMS_STATE_(LOW|GOOD)',
        'tpms_server: sdv_tpms_server: Tpms server: TIRE_POSITION_(FL|FR|RL|RR)',
    ]
    EXPECTED_DT_PUBLISHER_LOG = [
        'dt_mw_publisher: main: TirePressure Publisher created!',
        'dt_mw_publisher: main: MirrorPositionAdjust Publisher created!',
        'dt_mw_publisher: main: Published message: TirePressure',
        'dt_mw_publisher: main: Published message: MirrorPositionAdjust',
    ]
    EXPECTED_DT_SUBSCRIBER_LOG_REGEX = [
        r'dt_mw_subscriber: main: Subscribed message from .*received: \[TirePressure { pressure: \d+',
    ]
    EXPECTED_CAR_MONITOR_APP_SUBSCRIBER_LOG = [
        'SdvCarMonitorTestApp: Message received: Mirror adjust = AXIS_',
        'SdvCarMonitorTestApp: Message received: tire pressure = ',
    ]
    EXPECTED_CAR_MONITOR_APP_USER_PREFERENCES_GRPC_SERVER_LOG = [
        'SdvCarMonitorTestApp: UserPreferencesGrpcServer is ready!',
    ]
    EXPECTED_SUNROOF_SERVER_LOG = [
        'SunroofGrpcServer: SunroofService registered through rpcAgent',
    ]
    EXPECTED_SUNROOF_CLIENT_LOG = [
        'sunroof_client: sdv_sunroof_client: Sunroof client service bundle started.',
        'sunroof_client: sdv_sunroof_client: Discovered 1 Sunroof service units',
        'sunroof_client: sdv_sunroof_client: gRPC sample Sunroof client received: SUNROOF_STATE_',
        'sunroof_client: sdv_sunroof_client: gRPC sample Sunroof client received: SUNROOF_STATE_',
    ]
    EXPECTED_UPDATED_SETTINGS_LOG = [
        'SdvCarMonitorTestApp: Updated car seat position:',
        'SdvCarMonitorTestApp: Updated HVAC temperature:',
    ]
    EXPECTED_INTENT_RECEIVER_REGISTERED_LOG = [
        'SdvCarMonitorTestApp: IntentReceiver registered.',
    ]
    EXPECTED_SUCCESSFUL_INTERNET_CONNECTION_LOG = [
        'SdvCarMonitorTestApp: HTTP 204. Successfully connected to https://connectivitycheck.gstatic.com/generate_204',
    ]

    def verify_no_new_subscriber_or_rpc_client_logs(self, duration_seconds=10):
        """
        Verifies that no messages indicating a DT subscription and that
        no messages indicating RPC client are received during a specified duration.
        """
        logging.info(f"VERIFICATION: Ensuring no new DT messages are received for {duration_seconds} seconds")

        self.core_vm_device.execute_shell_command_in_subprocess(
            self.DT_PUBLISHER_CMD, self.DT_PUBLISHER_CMD)
        time.sleep(duration_seconds)
        logcat_output = self.ivi_vm_device.execute_shell_command('logcat -d')

        for log_pattern in self.EXPECTED_CAR_MONITOR_APP_SUBSCRIBER_LOG+self.EXPECTED_CAR_MONITOR_APP_LOG_REGEX:
            assert re.search(log_pattern, logcat_output) is None, \
                f"VERIFICATION FAILED: Found subscriber log '{log_pattern}' when none was expected."

        logging.info("SUCCESS: Verified no new DT messages were received.")

    def setup_class(self):
        super().setup_class()
        self.ivi_vm_device = self.get_device('device1').adb()
        self.core_vm_device = self.get_device('device2').adb()

    def setup_test(self):
        super().setup_test()
        self.ivi_vm_device.execute_shell_command(self.CLOSE_APP_CMD)
        self.ivi_vm_device.clear_logcat()
        self.core_vm_device.root_device()
        # TODO: b/399673375 - remove the following after terminate_subprocess() is fixed, and confirming it's by default called in the teardown method provided by the framework
        self._kill_service(self.core_vm_device, self.CLUSTER_SERVER_CMD)
        self._kill_service(self.core_vm_device, self.TPMS_SERVER_CMD)
        self._kill_service(self.core_vm_device, self.DT_PUBLISHER_CMD)
        self._kill_service(self.core_vm_device, self.DT_SUBSCRIBER_CMD)
        self._kill_service(self.core_vm_device, self.SUNROOF_CLIENT_CMD)
        self.core_vm_device.clear_logcat()

    def teardown_test(self):
        self.core_vm_device.terminate_all_subprocesses()
        super().teardown_test()

    def _kill_service(self, device, service_name):
        """Kills all the processes that are running the service."""
        device.execute_shell_command(f'killall {service_name}', False)

    def test_internet_connectivity(self):
        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=self.EXPECTED_INTENT_RECEIVER_REGISTERED_LOG,
        )
        self.ivi_vm_device.execute_shell_command(
            self.BROADCAST_TEST_INTERNET_CONNECTIVITY_EVENT_CMD
        )
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=self.EXPECTED_SUCCESSFUL_INTERNET_CONNECTION_LOG,
        )

    def test_cluster_server_sample(self):
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.CLUSTER_SERVER_CMD, self.CLUSTER_SERVER_CMD
        )
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.TPMS_SERVER_CMD, self.TPMS_SERVER_CMD
        )
        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)

        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=self.EXPECTED_CAR_MONITOR_APP_LOG_REGEX,
        )
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.core_vm_device,
            grep_text=self.CLUSTER_SERVER_LOG_TAG,
            expected_result=self.EXPECTED_CLUSTER_SERVER_LOG_REGEX,
        )
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.core_vm_device,
            grep_text=self.TPMS_SERVER_LOG_TAG,
            expected_result=self.EXPECTED_TPMS_SERVER_LOG_REGEX,
        )

    def test_dt_publisher_sample(self):
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.DT_PUBLISHER_CMD, self.DT_PUBLISHER_CMD
        )
        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.core_vm_device,
            grep_text=self.DT_PUBLISHER_LOG_TAG,
            expected_result=self.EXPECTED_DT_PUBLISHER_LOG,
        )
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=self.EXPECTED_CAR_MONITOR_APP_SUBSCRIBER_LOG,
        )

    def test_publish_tire_pressure_data_through_dt_sample(self):
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.DT_PUBLISHER_CMD, self.DT_PUBLISHER_CMD
        )
        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.core_vm_device,
            grep_text=self.DT_PUBLISHER_LOG_TAG,
            expected_result=self.EXPECTED_DT_PUBLISHER_LOG,
        )
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.DT_SUBSCRIBER_CMD, self.DT_SUBSCRIBER_CMD
        )
        self.ivi_vm_device.execute_shell_command(
            self.BROADCAST_PUBLISH_TIRE_PRESSURE_EVENT_CMD
        )
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.core_vm_device,
            grep_text=self.DT_SUBSCRIBER_LOG_TAG,
            expected_result=self.EXPECTED_DT_SUBSCRIBER_LOG_REGEX,
        )

    def test_sunroof_client_sample(self):
        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.SUNROOF_SERVER_LOG_TAG,
            expected_result=self.EXPECTED_SUNROOF_SERVER_LOG,
        )
        self.core_vm_device.execute_shell_command(
            self.SUNROOF_CLIENT_CMD, self.SUNROOF_CLIENT_CMD
        )
        self.core_vm_device.execute_shell_command(
            self.SUNROOF_CLIENT_CMD, self.SUNROOF_CLIENT_CMD
        )
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.core_vm_device,
            grep_text=self.SUNROOF_CLIENT_LOG_TAG,
            expected_result=self.EXPECTED_SUNROOF_CLIENT_LOG,
        )

    def test_update_car_seat_and_hvac_settings_sample(self):
        def set_property(device, property_name, property_value):
            device.execute_shell_command(['setprop', property_name, property_value])

        set_property(self.core_vm_device, 'persist.sdv.orchestrator_config_path', '"/etc/orch/vm_user_preferences_sample_orch_config.textproto"')
        self.core_vm_device.reboot_device()
        self.core_vm_device.wait_for_device_online()

        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=self.EXPECTED_CAR_MONITOR_APP_USER_PREFERENCES_GRPC_SERVER_LOG,
        )
        self.ivi_vm_device.execute_shell_command(
            self.BROADCAST_CAR_SEATS_EVENT_CMD
        )
        self.ivi_vm_device.execute_shell_command(
            self.BROADCAST_HVAC_TEMPERATURE_INCREASE_EVENT_CMD
        )
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=self.EXPECTED_UPDATED_SETTINGS_LOG,
        )

    def test_graceful_stop_and_restart_gateway_client(self):
        """
        Automates test 1.3.6 (Stop) and 1.3.4 (Restart) by verifying
        internal logs and functional data transmission.
        """
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.CLUSTER_SERVER_CMD, self.CLUSTER_SERVER_CMD)
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.TPMS_SERVER_CMD, self.TPMS_SERVER_CMD)
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.DT_PUBLISHER_CMD, self.DT_PUBLISHER_CMD)

        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=self.EXPECTED_INTENT_RECEIVER_REGISTERED_LOG)

        self.ivi_vm_device.execute_shell_command(self.BROADCAST_STOP_GATEWAY_CLIENT_CMD)
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=self.EXPECTED_CAR_MONITOR_APP_STOP_OPERATION_LOG
        )
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.NATIVE_CLIENT_LOG_TAG,
            expected_result=self.EXPECTED_NATIVE_CLIENT_STOP_LOGS
        )

        self.ivi_vm_device.clear_logcat()
        self.verify_no_new_subscriber_or_rpc_client_logs()

        self.ivi_vm_device.execute_shell_command(self.BROADCAST_START_GATEWAY_CLIENT_CMD)
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=self.EXPECTED_CAR_MONITOR_APP_START_LOG,
        )

        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.NATIVE_CLIENT_LOG_TAG,
            expected_result=self.EXPECTED_NATIVE_CLIENT_START_LOGS
        )
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=self.EXPECTED_CAR_MONITOR_APP_SUBSCRIBER_LOG+self.EXPECTED_CAR_MONITOR_APP_LOG_REGEX
        )

    def test_force_stop_and_restart_gateway_client(self):
        """
        Tests robustness by using 'am force-stop' and verifies data transmission
        stops and can be resumed after restart.
        """
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.CLUSTER_SERVER_CMD, self.CLUSTER_SERVER_CMD)
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.TPMS_SERVER_CMD, self.TPMS_SERVER_CMD)
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.DT_PUBLISHER_CMD, self.DT_PUBLISHER_CMD)

        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=self.EXPECTED_INTENT_RECEIVER_REGISTERED_LOG)

        self.ivi_vm_device.execute_shell_command(self.CLOSE_APP_CMD)

        self.ivi_vm_device.clear_logcat()
        self.verify_no_new_subscriber_or_rpc_client_logs()

        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)
        SdvSampleCarMonitorAppTest.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=self.EXPECTED_CAR_MONITOR_APP_SUBSCRIBER_LOG+self.EXPECTED_CAR_MONITOR_APP_LOG_REGEX
        )

    @staticmethod
    def __grep_expected_result(
        sdv_device,
        grep_text,
        expected_result=None,
        logcat_args=None,
        grep_args=None,
    ):
        logcat_result = sdv_device.grep_from_logcat(
            grep_text, logcat_args, grep_args
        )
        logging.info(
            f'logcat_result={logcat_result} expected_result={expected_result}'
        )
        if expected_result is not None:
            if isinstance(expected_result, list):
                for expected_item in expected_result:
                    if re.search(expected_item, logcat_result) is None:
                        return False
                return True
            elif isinstance(expected_result, str):
                return re.search(expected_result, logcat_result) is not None
            else:
                logging.info(
                    f'type of expected_result={expected_result} is not a'
                    ' supported'
                )
                return False
        else:
            return len(logcat_result) != 0

    @staticmethod
    def wait_and_verify_expected_logs(
        sdv_device,
        grep_text,
        expected_result=None,
        logcat_args=None,
        grep_args=None,
        assert_msg=None,
        poll_interval=polling.POLL_INTERVAL,
        timeout=polling.DEFAULT_TIMEOUT,
    ):
        """Polls the logcat output for a specific text until found or timeout.

        Args:
            sdv_device: SDV VM.
            grep_text: The text to search for in the logcat output.
            expected_result: Expected log patten(s) within the retrieved text.
              Can be a single string pattern or a list of string patterns that
              must all be found using re.search(...). For a list of patterns:
              all patterns must be matched, but can be matched in any order.
            logcat_args: Arguments for logcat search.
            grep_args: Arguments for grep.
            assert_msg: Error message displayed when logs not found.
            poll_interval: The time in seconds to wait between consecutive calls
              to func. Consider increasing this for computationally expensive
              function to reduce resource consumption.
            timeout: The maximum time in seconds to wait for a non-None return.
              Avoid increasing this unless absolutely necessary.

        Returns:
            True if grep matched at least one logcat output
            False if grep matched no logcat output within the timeout
        """
        if assert_msg is None:
            assert_msg = f'Failed after {timeout} seconds, logcat expected_result not found: {expected_result}'
        polling.wait_for_true(
            SdvSampleCarMonitorAppTest.__grep_expected_result,
            sdv_device,
            grep_text,
            expected_result,
            logcat_args,
            grep_args,
            timeout=timeout,
            assert_msg=assert_msg,
            poll_interval=poll_interval,
        )

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
