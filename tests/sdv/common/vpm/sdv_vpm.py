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

from enum import Enum
import logging
import time
from sdv_test_fw.verification import polling

#TODO(b/430276649): improve the library to the current status quo.
class SdvVpm:

    class __VPM_STATES(Enum):
        LOW_POWER = 'low-power'
        SOFTWARE_UPDATE = 'software-update'
        PARK = 'park'
        LIFE_ON_BOARD = 'life-on-board'
        VEHICLE_ON = 'vehicle-on'
        TRACTION_ON = 'traction-on'

    __VPM_STATE_LISTENER_PROCESS_NAME = 'vpm_state_listener'
    __VPM_STATE_LISTENER_COMMAND = 'vpm_vehicle_state_client'
    __VPM_STATE_LISTENER_LOG_PATH = (
        '/data/local/tmp/vpm_state_listener_client.log'
    )
    __COMMAND_TO_REMOVE_LOG = 'rm {log_file_path}'
    __COMMAND_TO_SET_VEHICLE_STATE = 'vepsm vehicle-state {state}'
    __COMMAND_TO_READ_LOG = 'cat {log_file_path}'
    __VEPSM_PROCESS_NAME = 'vepsm_process'
    __VEPSM_LOG_PATH = '/data/local/tmp/vepsm.log'
    __DEFAULT_RAM_SUSPEND_SECONDS = 5
    __DEFAULT_RAM_RESUME_LOG_TIMEOUT_SECONDS = 30
    __SDV_RESUME_GREP_TEXT="has resumed successfully"
    __SDV_RESUME_EXPECTED_LINE = "sdv_vpm::power_state: sdv_vpm_agent has resumed successfully"
    __VPM_LOGCAT_ARGS = '*:F sdv_vpm_agent:*'

    def __init__(self, device_adb):
        self.__device_adb = device_adb

    def vpm_states(self):
        return self.__VPM_STATES

    # Start VPM State Listener
    def start_vpm_state_listener(self):
        # Start VPM State Listener
        logging.info(
            'Starting VPM State Listener on device'
            f' [{self.__device_adb.get_device_serial()}]'
        )

        # Since the Vehicle Power Management State Listener is blocking, execute it as subprocess
        # Also, redirect the output to a file for verification later
        command_to_start_vpm_state_listener = (
            '{vpm_listener_command} > {vpm_listener_log}'.format(
                vpm_listener_command=self.__VPM_STATE_LISTENER_COMMAND,
                vpm_listener_log=self.__VPM_STATE_LISTENER_LOG_PATH,
            )
        )
        self.__device_adb.execute_shell_command_in_subprocess(
            self.__VPM_STATE_LISTENER_PROCESS_NAME,
            command_to_start_vpm_state_listener,
        )

        if not self.__device_adb.is_subprocess_running(
            self.__VPM_STATE_LISTENER_PROCESS_NAME
        ):
            raise Exception(
                'Unable to start VPM State Listener on device'
                f' [{self.__device_adb.get_device_serial()}]'
            )

    # Stop VPM State Listener
    def stop_vpm_state_listener(self):
        # Stop VPM State Listener
        logging.info(
            'Stopping VPM State Listener on device'
            f' [{self.__device_adb.get_device_serial()}]'
        )

        if not self.__device_adb.is_subprocess_running(
            self.__VPM_STATE_LISTENER_PROCESS_NAME
        ):
            logging.warn(
                'VPM Listener is not running on device [<%s>]',
                self.__device_adb.get_device_serial(),
            )
            return

        self.__device_adb.terminate_subprocess(
            self.__VPM_STATE_LISTENER_PROCESS_NAME
        )

        # Remove VPM State Listener Log
        remove_log_command = self.__COMMAND_TO_REMOVE_LOG.format(
            log_file_path=self.__VPM_STATE_LISTENER_LOG_PATH
        )
        self.__device_adb.execute_shell_command(remove_log_command)

        if self.__device_adb.is_subprocess_running(
            self.__VPM_STATE_LISTENER_PROCESS_NAME
        ):
            raise Exception(
                'Failed to stop VPM State Listener on device'
                f' {self.__device_adb.get_device_serial()}'
            )

        logging.info(
            'Stopped VPM State Listener on device <%s>',
            self.__device_adb.get_device_serial(),
        )

    # Set Vehicle State
    def set_vehicle_state(self, state, timeout=30, poll_interval=0.1):
        # Set Vehicle State
        logging.info(
            f'Setting vehicle state to {state.name} on device'
            f' [{self.__device_adb.get_device_serial()}]'
        )

        # Since the vepsm command is blocking, execute it as subprocess
        # Also, redirect the output to a file for verification later
        command_to_set_vehicle_state = (
            '{set_vehicle_state_command} > {vepsm_log}'.format(
                set_vehicle_state_command=self.__COMMAND_TO_SET_VEHICLE_STATE.format(
                    state=state.value
                ),
                vepsm_log=self.__VEPSM_LOG_PATH,
            )
        )
        self.__device_adb.execute_shell_command_in_subprocess(
            self.__VEPSM_PROCESS_NAME, command_to_set_vehicle_state
        )
        if not self.__device_adb.is_subprocess_running(
            self.__VEPSM_PROCESS_NAME
        ):
            raise Exception(
                'Unable to start VEPSM process on device'
                f' [{self.__device_adb.get_device_serial()}]'
            )

        deadline = time.perf_counter() + timeout
        vepsm_log = ''
        while (
            time.perf_counter() < deadline
            and 'Enter VePSM command, press enter for help or cntl + c to exit!'
            not in vepsm_log
        ):
            vepsm_log = self.__device_adb.execute_shell_command(
                self.__COMMAND_TO_READ_LOG.format(
                    log_file_path=self.__VEPSM_LOG_PATH
                )
            )
            time.sleep(poll_interval)

        self.__device_adb.terminate_subprocess(self.__VEPSM_PROCESS_NAME)
        remove_log_command = self.__COMMAND_TO_REMOVE_LOG.format(
            log_file_path=self.__VEPSM_LOG_PATH
        )
        self.__device_adb.execute_shell_command(remove_log_command)

        if self.__device_adb.is_subprocess_running(self.__VEPSM_PROCESS_NAME):
            raise Exception(
                'Failed to stop VEPSM on device'
                f' {self.__device_adb.get_device_serial()}'
            )

        return vepsm_log

    # Get VPM State Listener Response
    def get_vpm_state_listener_response(self):
        read_log_command = self.__COMMAND_TO_READ_LOG.format(
            log_file_path=self.__VPM_STATE_LISTENER_LOG_PATH
        )
        return self.__device_adb.execute_shell_command(read_log_command)


    def suspend_and_resume_vm_from_ram(self):
        """Suspends the VM to RAM and waits for it to resume.

        This function orchestrates a full suspend-to-RAM and resume cycle.
        It first calls `suspend_vm_for_time()` to initiate the suspension
        with a default duration, which also schedules an automatic wakeup.
        It then blocks by calling `wait_for_vm_to_resume_from_ram()`, which
        waits until it confirms the VM has successfully resumed by checking
        for a specific logcat message.

        Args:
          None.

        Returns:
          None

        Raises:
          Exception: If the suspend or resume operations fail, or if waiting
            for the resume confirmation message times out.
        """
        self.suspend_vm_for_time()
        self.wait_for_vm_to_resume_from_ram()  # Give it more time to resume.

    def wait_for_vm_to_resume_from_ram(self, timeout=__DEFAULT_RAM_RESUME_LOG_TIMEOUT_SECONDS):
        """Waits for the VM to resume from a RAM-suspended state.

        This function polls the device's logcat for a specific message that
        confirms the VPM (Vehicle Power Manager) agent has successfully
        resumed from suspension. It will block until the message is found or
        the timeout is reached.

        Args:
          timeout: int, The maximum time in seconds to wait for the resume
            confirmation message. Defaults to 30.

        Returns:
          None

        Raises:
          Exception: If the timeout is reached before the resume confirmation
            message is found in logcat.
        """
        polling.wait_and_verify_expected_logs(
            sdv_device = self.__device_adb,
            grep_text = self.__SDV_RESUME_GREP_TEXT,
            expected_result = self.__SDV_RESUME_EXPECTED_LINE,
            logcat_args = self.__VPM_LOGCAT_ARGS,
            assert_msg = "VPM resume log not found",
            timeout=timeout,
            poll_interval=1,
        )

    def suspend_vm_for_time(self, time_s= __DEFAULT_RAM_SUSPEND_SECONDS):
        """Suspends the VM to RAM for a specified duration.

        This function uses a sequence of `vepsm` commands to put the VM into a
        RAM-suspended state. It schedules an automatic wakeup after `time_s`
        seconds using the `rtcwake` command. This function does not wait for
        the VM to actually resume.

        Args:
          time_s: int, The duration in seconds to suspend the VM. Defaults to 5.

        Returns:
          None

        Raises:
          Exception: If any of the `vepsm` commands fail.
        """
        session = self.__device_adb.interactive_session()
        session.send_command_and_wait_for_outputs(
            "vepsm power-state power-on",
            [
                "Received power-state-report from VPM ON , reason HOST_REQUESTED",
            ],
        )
        logging.info("vepsm power-state power-on finished")

        session.send_command_and_wait_for_outputs(
            "vepsm power-state prepare ram",
            [
                (
                    "Received power-state-report from VPM WAIT_FOR_FINISH ,"
                    " reason HOST_REQUESTED"
                ),
            ],
        )
        logging.info("vepsm power-state prepare ram done")

        # Automatically wake up the VM in 5 seconds from now.
        #
        # TODO: b/393555389 - This could potentially be rewritten to use the
        # equivalent to `cvd powerbtn` instead.
        resume_session = self.__device_adb.interactive_session()
        resume_session.send_command(f"rtcwake -m no -s {time_s}")
        resume_session.expect_outputs(["#"])

        # The expectations for this command will only be visible once we resume
        session.send_command("vepsm power-state finish ram")
        logging.info("vepsm power-state finish ram done")
        resume_session.close()
        session.close()

    def prepare_shutdown_vm(self):
        """prepare the shutdown of the VM."""
        session = self.__device_adb.interactive_session()
        session.send_command_and_wait_for_outputs(
            'vepsm power-state power-on',
            [
                'Received power-state-report from VPM ON , reason HOST_REQUESTED',
            ],
        )
        logging.info('vepsm power-state power-on finished')

        session.send_command_and_wait_for_outputs(
            'vepsm power-state prepare shut-down',
            [
                'Received power-state-report from VPM POWER_OFF_ENTER , reason HOST_REQUESTED',
                'Received power-state-report from VPM WAIT_FOR_FINISH , reason HOST_REQUESTED',
            ],
        )
        logging.info('vepsm power-state prepare shut-down done')
        session.close()

    def shutdown_vm(self):
        """shutdown the VM."""
        # Prepare to shutdown the VM before sending the shutdown command
        self.prepare_shutdown_vm()
        session = self.__device_adb.interactive_session()
        # Send finish shut down command
        session.send_command('vepsm power-state finish shut-down')
        logging.info('vepsm power-state finish shut-down done')

        # Verify the device is offline after shutdown
        logging.info('Waiting for device to go offline after shutdown sequence...')
        try:
            self.__device_adb.wait_for_device_offline()
        except Exception:
            raise Exception(
                'Device did not go offline within the timeout period after shutdown.'
            )
        logging.info('Device is offline as expected after shutdown.')
        session.close()

    def cancel_shutdown_vm(self):
        """Cancel the shutdown of the VM."""
        session = self.__device_adb.interactive_session()
        session.send_command_and_wait_for_outputs(
            'vepsm power-state cancel',
            [
                'Received power-state-report from VPM SHUTDOWN_CANCELLED , reason HOST_REQUESTED'
            ],
        )
        logging.info('vepsm power-state cancel shut-down done')
        session.close()