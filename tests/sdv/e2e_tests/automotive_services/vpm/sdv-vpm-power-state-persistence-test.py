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


from mobly import asserts
import logging
import time
from typing import Optional

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.session.interactive_session import Session


class SdvVpmPowerStateManager:
    """ Helper class that via adb shell interaction allows:
    1. Observing of power state currently reported by VPM agent
    (using power_state_client vpm sample)
    2. Setting system's power state
    (using vepsm vpm sample)
    3. Killing the process of the VPM agent and waiting for restart
    """
    __VPM_POWER_STATE_CLIENT_PROCESS_NAME = 'vpm_power_state_client_process'
    __VPM_POWER_STATE_CLIENT_COMMAND = 'vpm_power_state_client'
    __VPM_POWER_STATE_CLIENT_LOG_PATH = (
        '/data/local/tmp/vpm_power_state_client.log'
    )
    __VPM_VEPSM_LOG_PATH = (
        '/data/local/tmp/vpm_vpsm.log'
    )
    __VPM_VEPSM_SUBPROCESS_NAME = 'VEPSMsubprocess'
    __COMMAND_TO_REMOVE_LOG = 'rm {log_file_path}'
    __COMMAND_TO_READ_LOG = 'cat {log_file_path}'

    def __init__(self, device_adb):
        self.__device_adb = device_adb

    def start_power_state_client(self):
        logging.info(
            'Starting VPM Power Client on device'
            f' [{self.__device_adb.get_device_serial()}]'
        )

        command_to_start_vpm_state_listener = (
            '{vpm_listener_command} > {vpm_listener_log}'.format(
                vpm_listener_command=self.__VPM_POWER_STATE_CLIENT_COMMAND,
                vpm_listener_log=self.__VPM_POWER_STATE_CLIENT_LOG_PATH,
            )
        )
        self.__device_adb.execute_shell_command_in_subprocess(
            self.__VPM_POWER_STATE_CLIENT_PROCESS_NAME,
            command_to_start_vpm_state_listener,
        )

        if not self.__device_adb.is_subprocess_running(
            self.__VPM_POWER_STATE_CLIENT_PROCESS_NAME
        ):
            raise Exception(
                'Unable to start VPM Power state client on device'
                f' [{self.__device_adb.get_device_serial()}]'
            )

    def wait_polling_for_power_state_report(self, expected_state_str, timeout=30, poll_interval=0.5) -> str:
        last_report = ''
        deadline = time.perf_counter() + timeout
        result = False
        while (
            time.perf_counter() < deadline
        ):
            full_report = self.__device_adb.execute_shell_command(
                self.__COMMAND_TO_READ_LOG.format(
                    log_file_path=self.__VPM_POWER_STATE_CLIENT_LOG_PATH
                )
            )
            last_report = full_report.splitlines()[-1]
            if expected_state_str in last_report:
                result = True
                break

            time.sleep(poll_interval)

        return result

    def stop_power_state_client(self) -> str:
        # Stop VPM State Listener
        logging.info(
            'Stopping VPM power state client on device'
            f' [{self.__device_adb.get_device_serial()}]'
        )

        if not self.__device_adb.is_subprocess_running(
            self.__VPM_POWER_STATE_CLIENT_PROCESS_NAME
        ):
            logging.warning(
                'VPM Listener is not running on device [<%s>]',
                self.__device_adb.get_device_serial(),
            )
            return

        self.__device_adb.terminate_subprocess(
            self.__VPM_POWER_STATE_CLIENT_PROCESS_NAME
        )

        # Remove VPM State Listener Log
        remove_log_command = self.__COMMAND_TO_REMOVE_LOG.format(
            log_file_path=self.__VPM_POWER_STATE_CLIENT_LOG_PATH
        )
        self.__device_adb.execute_shell_command(remove_log_command)

        if self.__device_adb.is_subprocess_running(
            self.__VPM_POWER_STATE_CLIENT_PROCESS_NAME
        ):
            raise Exception(
                'Failed to stop VPM State Listener on device'
                f' {self.__device_adb.get_device_serial()}'
            )

        logging.info(
            'Stopped VPM State Listener on device <%s>',
            self.__device_adb.get_device_serial(),
        )

    def run_vepsm_command(self, command, timeout=30, poll_interval=0.5):
        """runs vepsm command in subprocess, waits for expected vepsm output until timeout"""
        command_with_redirect_stdout = f'{command} > {self.__VPM_VEPSM_LOG_PATH}'
        self.__device_adb.execute_shell_command_in_subprocess(
            self.__VPM_VEPSM_SUBPROCESS_NAME, command_with_redirect_stdout
        )
        if not self.__device_adb.is_subprocess_running(
            self.__VPM_VEPSM_SUBPROCESS_NAME
        ):
            raise Exception(
                'Unable to start VEPSM process on device'
                f' [{self.__device_adb.get_device_serial()}]'
            )

        # wait until new power state set:
        deadline = time.perf_counter() + timeout
        vepsm_log = ''
        vepsm_success = False
        while (
            time.perf_counter() < deadline
            and not vepsm_success
        ):
            vepsm_log = self.__device_adb.execute_shell_command(
                self.__COMMAND_TO_READ_LOG.format(
                    log_file_path=self.__VPM_VEPSM_LOG_PATH
                )
            )
            if 'Received power-state-report' in vepsm_log:
                vepsm_success = True
            time.sleep(poll_interval)
        if not vepsm_success:
            raise Exception(
                "Timeout in vepsm command, did not receive expected stdout")

        # cleanup temp file and subprocess:
        self.__device_adb.terminate_subprocess(
            self.__VPM_VEPSM_SUBPROCESS_NAME)
        remove_log_command = self.__COMMAND_TO_REMOVE_LOG.format(
            log_file_path=self.__VPM_VEPSM_LOG_PATH
        )
        self.__device_adb.execute_shell_command(remove_log_command)

        if self.__device_adb.is_subprocess_running(self.__VPM_VEPSM_SUBPROCESS_NAME):
            raise Exception(
                'Failed to stop VEPSM on device'
                f' {self.__device_adb.get_device_serial()}'
            )

    def kill_vpm_agent_and_restart(
        self, timeout=30, poll_interval=0.1
    ) -> Optional[Session]:
        """ Kills vpm agent. Then polls the available binder connection
        for VPM's aidl interface, until restored, marking that VPM has finished restarting.
        Returns:
            session running the new VPM process. Session should be closed
            IF restarting failed, returns None
        """
        # kill
        self.__kill_agent("sdv_vpm_agent")
        session = self.__device_adb.interactive_session()

        # restart manually (VPM is a oneshot service, android init system does not restart it):
        session.send_command('/system/system_ext/bin/sdv_vpm_agent')

        # wait for finished restarting:
        vpm_restored = False
        expected_vpm_binder_name = "vpm.IPowerStateClientApi"
        deadline = time.perf_counter() + timeout
        while time.perf_counter() < deadline:
            logcat_result = self.__device_adb.execute_shell_command(
                "service list")
            if expected_vpm_binder_name in logcat_result:
                vpm_restored = True
                break
            time.sleep(poll_interval)

        if vpm_restored:
            return session
        else:
            session.close()
            return None

    def __kill_agent(self, name):
        process_id = self.__device_adb.execute_shell_command(
            "pgrep -f " + name)

        if process_id is None:
            raise Exception("Could not find agent to kill")

        self.__device_adb.execute_shell_command(
            "kill " + process_id
        )


class SdvVpmPowerStatePersistenceTest(sdv_base_test.SdvBaseTestClass):
    VPM_POWER_STATE_SYS_PROP_NAME = "sdv.vpm.power.state"

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()
        self.sdv_device.reboot_device_and_verify_logcat()
        self.sdv_device.root_device()
        self.vpm_manager = SdvVpmPowerStateManager(self.sdv_device)

    def setup_test(self):
        super().setup_test()
        self.sdv_device.reboot_device()
        self.sdv_device.wait_for_device_online()
        self.vpm_manager.start_power_state_client()

    def __get_persisted_power_state(self):
        return self.sdv_device.execute_shell_command(f"getprop {self.VPM_POWER_STATE_SYS_PROP_NAME}")

    def test_power_state_persistency_through_agent_crash(self):
        initial_power_state_expectation = "POWER_OFF_EXIT"
        second_power_state_expectation = "ON"
        # verify initial power state is whats we expect after cold boot:
        result = self.vpm_manager.wait_polling_for_power_state_report(
            initial_power_state_expectation)
        asserts.assert_true(
            result,
            f"Initial power state is not {initial_power_state_expectation}"
        )
        sys_prop_response = self.__get_persisted_power_state()
        asserts.assert_in(
            initial_power_state_expectation,
            sys_prop_response,
            f"Did not find expected power state {initial_power_state_expectation} in {sys_prop_response}, system properties query"
        )

        # change power state to on, wait for power state client to report power state change:
        self.vpm_manager.run_vepsm_command(
            f"vepsm power-state power-on")

        result = self.vpm_manager.wait_polling_for_power_state_report(
            second_power_state_expectation)
        asserts.assert_true(
            result,
            f"Incorrect power state reported by client, after vepsm change power state command"
        )
        sys_prop_response = self.__get_persisted_power_state()
        asserts.assert_in(
            second_power_state_expectation,
            sys_prop_response,
            f"Did not find expected power state {second_power_state_expectation} in {sys_prop_response}, system properties query"
        )

        # stop client, kill vpm agent, restart client
        self.vpm_manager.stop_power_state_client()
        new_vpm_process_holder = self.vpm_manager.kill_vpm_agent_and_restart()
        self.vpm_manager.start_power_state_client()

        # assert vpm restart was successful:
        asserts.assert_is_not_none(
            new_vpm_process_holder,
            f'VPM not found to be alive until 30s timeout'
        )

        # assert that state is still ON after agent restart:
        result = self.vpm_manager.wait_polling_for_power_state_report(
            second_power_state_expectation)
        asserts.assert_true(
            result,
            f"Incorrect power state reported by client, after vepsm change power state command"
        )
        sys_prop_response = self.__get_persisted_power_state()
        asserts.assert_in(
            second_power_state_expectation,
            sys_prop_response,
            f"Did not find expected power state {second_power_state_expectation} in {sys_prop_response}, system properties query"
        )
        new_vpm_process_holder.close()


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
