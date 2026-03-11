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

"""Methods and data class for hardware suspend/resume baseline tests.

This module centralizes shared logic and data structures to avoid code
duplication between single and multiVM testing.

Usage:
    The `SdvBaselineHwSuspendResumeMixin` class is intended to be used as a
    mixin. It requires the consuming test class to also inherit from
    `SdvBaseTestClass`, as it relies on specific methods and constants provided
    by the base test environment.

Note: This file a temporary solution to validate the reliability of the
suspend/resume testing methodology for both single and multi-VM scenarios.

The plan is to integrate this logic and solution into the HW library,
addressing the limitations of the previous approach. Ultimately this logic will
be provided by the framework directly to allow seamless VPM testing.
"""

import dataclasses
import logging
from typing import List

from mobly import asserts
import pexpect
from pexpect import pxssh
import qnx_process_management
from sdv_test_fw.verification import polling


@dataclasses.dataclass()
class QnxVmConfig:
    dev_file: str
    sdv_guest_id: str
    # Because of b/484317350, for suspend resume we need to keep track of the
    # pid of qvm process to ensure it did not silently restart. Its value is
    # handled by the logic itself.
    _qvm_pid: str | None = dataclasses.field(
        init=False, default=None, repr=False
    )

    @property
    def sdv_guest_name(self) -> str:
        return f"sdv-{self.sdv_guest_id}"

    @property
    def daemon_label(self) -> str:
        return f"powerbtn-daemon-{self.sdv_guest_name}"

    @property
    def qvm_pid(self) -> str | None:
        return self._qvm_pid

    def extract_current_qvm_pid(self, qvm_processes: List[str]) -> str | None:
        for qvm_process_info in qvm_processes:
            # We store the pid of the qvm for the corresponding VM.
            if self.sdv_guest_name not in qvm_process_info:
                continue

            qvm_pid = qnx_process_management.get_pid_from_process_info(
                qvm_process_info
            )

            logging.debug(f"{self.sdv_guest_name} qvm pid {qvm_pid}")
            return qvm_pid

        logging.error(f"No qvm process found for {self.sdv_guest_name}")

    @qvm_pid.setter
    def qvm_pid(self, qvm_processes: List[str]):
        self._qvm_pid = self.extract_current_qvm_pid(qvm_processes)


class SdvBaselineHwSuspendResumeMixin:
    # ==========================================================================
    # Hypervisor and Devices Config
    # ==========================================================================
    LOCAL_PORT = "12222"
    LAB_PORT = "22"
    SSH_USERNAME = "root"
    SSH_PASSWORD = "root"

    # Framework setup ensures a static mapping:
    #   device1 -> instance1 (sdv-1)
    #   device2 -> instance3 (sdv-2)
    DEVICE1_VM_CONFIG = QnxVmConfig(dev_file="/dev/ttyp6", sdv_guest_id="1")
    DEVICE2_VM_CONFIG = QnxVmConfig(dev_file="/dev/ttyp7", sdv_guest_id="2")

    # ==========================================================================
    # Setup Commands
    # ==========================================================================

    FAKE_POWERBTN_DAEMON = (
        "on -d -t /dev/null sh -c '(while true; do sleep 98765; done) >"
        " {dev_file}' {daemon_label}"
    )

    ADDRESS_COMMAND = (
        'awk \'/vdev pl011/ {{s=1}} /^$/ {{s=0}} s==1 && $1 == "loc" {{print'
        " $2}}' '/guests/android/sdv-{sdv_guest_id}/sdv-{sdv_guest_id}.conf'"
    )
    ENABLE_WAKEUP = (
        "echo enabled >"
        " /sys/devices/platform/vdevs/{memory_address}.uart/tty/ttyAMA0/power/wakeup"
    )

    # ==========================================================================
    # VPM Commands
    # ==========================================================================

    VEPSM_POWER_STATE = "vepsm power-state {vpm_action}"
    VEPSM_POWER_STATE_OUTPUT = (
        "Received power-state-report from VPM {vpm_status} , reason"
        " HOST_REQUESTED"
    )

    WAKE_UP = "echo >> {dev_file}"

    # ==========================================================================
    # Device Status
    # ==========================================================================

    # The absence of the env file indicates the VM may be rebooting or something
    # happened, so it is not possible to check its status. This is an edge case
    # but we need to ensure the file exists before parsing it to accurately
    # distinguish between 'Suspended' and 'Running' states.
    SDV_VM_STATUS = (
        "[ -e /dev/qvm/{sdv_guest_name}/env ] && {{ "
        "cat /dev/qvm/{sdv_guest_name}/env "
        "| grep 'last_known_ip' "
        "| awk 'NR > 1 {{ if ($5 != \"0\") exit 1 }}' "
        "&& echo 'Suspended' || echo 'Running'; "
        "}} || echo 'Not available'"
    )
    VM_SUSPENDED = "Suspended"
    VM_RUNNING = "Running"
    VM_STATUS_NOT_AVAILABLE = "Not available"

    # ==========================================================================

    # ==========================================================================
    # Hypervisor QNX Interaction
    # ==========================================================================

    def connect_to_hypervisor_qnx(self):
        # Hypervisor QNX is common for all VMs. We take device1 serial it always
        # exists independently of the number of VMs the test requires.
        logging.info(f"Connect to QNX")
        sdv_device1_serial = self.sdv_device1.adb().get_device_serial()
        logging.info(f"device1 serial: {sdv_device1_serial}")

        # connect to QNX
        qnx_ip = sdv_device1_serial.split(":")[0]
        logging.info(f"QNX IP: {qnx_ip}")

        # The port differs locally and in CI/CD
        qnx_port = self.LAB_PORT
        if self.is_local_run():
            qnx_port = self.LOCAL_PORT
        logging.info(f"QNX port: {qnx_port}")

        try:
            self.host_session = pxssh.pxssh()
            self.host_session.login(
                qnx_ip,
                port=qnx_port,
                username=self.SSH_USERNAME,
                password=self.SSH_PASSWORD,
            )
        except pxssh.ExceptionPxssh as e:
            logging.error(f"pxssh failed on login: {e}")
            raise

        logging.info(f"Connection to QNX successful")

    def host_command(
        self, command, timeout=5, output_last_line_only=False
    ) -> list | str:
        try:
            self.host_session.sendline(command)
            self.host_session.prompt(timeout=timeout)

        except pexpect.TIMEOUT:
            logging.error(f"{command} timed out")
            raise

        host_output = self.host_session.before.decode("utf-8")

        logging.debug("Host command sent:")
        logging.debug("START-------------------")
        logging.debug(host_output)
        logging.debug("-------------------END")

        output_lines = host_output.splitlines()

        if output_last_line_only:
            last_line = output_lines[-1] if output_lines else ""
            logging.debug(f"Return only last line: {last_line}")
            return last_line

        return output_lines

    # ==========================================================================
    # Power Button Emulation Setup
    # ==========================================================================

    def _start_fake_powerbtn_daemon(self, vm_config):
        # Only start the daemon if there is not one running already in the
        # hypervisor. This is to avoid spawning multiple processes in the QNX
        # that cannot be killed and have the same purpose. Minimize the number
        # of zombie processes we leave in the hypervisor after the test
        # finalizes.
        logging.info(f"Start daemon {vm_config.daemon_label} in hypervisor")

        if qnx_process_management.processes_are_running(
            command_executor=self.host_command,
            process_identifier=vm_config.daemon_label,
        ):
            logging.info(
                f"Daemon with tag {vm_config.daemon_label} already running"
            )
            return

        self.host_command(
            self.FAKE_POWERBTN_DAEMON.format(
                dev_file=vm_config.dev_file,
                daemon_label=vm_config.daemon_label,
            )
        )

    def enable_fake_powerbtn(self, device, vm_config):
        logging.info(f"Prepare fake powerbtn for {vm_config.sdv_guest_name}")
        self._start_fake_powerbtn_daemon(vm_config)

        # The output is with format 0x1c090000. We are only interested on
        # the value after 0x
        vdevs_memory_location = self.host_command(
            self.ADDRESS_COMMAND.format(sdv_guest_id=vm_config.sdv_guest_id),
            output_last_line_only=True,
        )[2:]

        logging.info(
            f"{vm_config.sdv_guest_name} memory location:"
            f" {vdevs_memory_location}"
        )

        device.adb().execute_shell_command(
            self.ENABLE_WAKEUP.format(memory_address=vdevs_memory_location)
        )
        logging.info(f"Fake powerbtn enabled in {vm_config.sdv_guest_name}")

    # ==========================================================================
    # Device Status Verification
    # ==========================================================================

    def _device_status(self, vm_config):
        status = self.host_command(
            self.SDV_VM_STATUS.format(sdv_guest_name=vm_config.sdv_guest_name),
            output_last_line_only=True,
        )
        logging.debug(f"{vm_config.sdv_guest_name} VM status: {status}")

        # If the VM is not available, we log an error and continue to be able to
        # check if it is a temporary issue and autostart fixes it. We retrieve
        # qvm pid to verify if the VM is running or not.
        if status == self.VM_STATUS_NOT_AVAILABLE:
            logging.error(
                "Not possible to check status of the"
                f" {vm_config.sdv_guest_name} VM"
            )

            qvm_processes_info = qnx_process_management.query_processes_info(
                command_executor=self.host_command, process_identifier="qvm"
            )
            # extract_current_qvm_pid logs the pid of the qvm process so we only
            # need to call it for debugging.
            vm_config.extract_current_qvm_pid(qvm_processes_info)

        return status

    def _device_is_suspended(self, vm_config):
        return self._device_status(vm_config) == self.VM_SUSPENDED

    def _device_is_running(self, vm_config):
        return self._device_status(vm_config) == self.VM_RUNNING

    # ==========================================================================
    # Power Management
    # ==========================================================================

    def pwm_suspend_to_ram(self, pwm_session, vm_config):
        logging.info(
            f"Suspend {vm_config.sdv_guest_name} VM using Power Management"
        )

        # To ensure the verification of suspend resume is reliable, we store
        # the pid of the running VM to verify it has not changed when we resume.
        qvm_processes_info = qnx_process_management.query_processes_info(
            command_executor=self.host_command, process_identifier="qvm"
        )
        vm_config.qvm_pid = qvm_processes_info
        asserts.assert_is_not_none(
            vm_config.qvm_pid,
            msg=(
                "Not possible to obtain the pid for the"
                f" {vm_config.sdv_guest_name} VM"
            ),
        )

        pwm_session.send_command_and_wait_for_outputs(
            self.VEPSM_POWER_STATE.format(vpm_action="power-on"),
            [self.VEPSM_POWER_STATE_OUTPUT.format(vpm_status="ON")],
        )
        pwm_session.send_command_and_wait_for_outputs(
            self.VEPSM_POWER_STATE.format(vpm_action="prepare ram"),
            [
                self.VEPSM_POWER_STATE_OUTPUT.format(
                    vpm_status="SUSPEND_TO_RAM_ENTER"
                ),
                self.VEPSM_POWER_STATE_OUTPUT.format(
                    vpm_status="WAIT_FOR_FINISH"
                ),
            ],
        )

        # The command is expected to hang the session as the device will
        # suspend.
        pwm_session.send_command(
            self.VEPSM_POWER_STATE.format(vpm_action="finish ram")
        )

        # Waiting for the device to suspend before carrying out any other
        # action is vital to avoid unexpected behavior and ensure reliability in
        # suspend and resume testing.
        logging.info(f"Waiting for {vm_config.sdv_guest_name} to suspend.")
        result = polling.wait_for_true(
            self._device_is_suspended,
            vm_config,
            timeout=30,
            assert_msg=f"{vm_config.sdv_guest_name} did not suspend",
        )

        logging.info(f"{vm_config.sdv_guest_name} VM suspended successfully")

    def pwm_resume(self, pwm_session, vm_config):
        logging.info(f"Waking up {vm_config.sdv_guest_name} VM from host")

        self.host_command(self.WAKE_UP.format(dev_file=vm_config.dev_file))

        logging.info(f"Waiting for {vm_config.sdv_guest_name} to wake up.")
        result = polling.wait_for_true(
            self._device_is_running,
            vm_config,
            timeout=30,
            assert_msg=f"{vm_config.sdv_guest_name} did not resume",
        )

        try:
            pwm_session.expect_outputs([
                self.VEPSM_POWER_STATE_OUTPUT.format(
                    vpm_status="SUSPEND_TO_RAM_EXIT"
                )
            ])
        except pexpect.EOF:
            # Extended idle periods of the suspended device may cause the PWM
            # session to disconnect, resulting in a `pexpect.EOF` when reading
            # the output. Since the device has already been verified as
            # online/running, we treat this as a non-fatal issue. We log the
            # disconnection to aid in debugging potential side effects
            # and restart the interactive session so it is available if needed.
            logging.debug(
                f"PWM session for {vm_config.sdv_guest_name} was disconnected"
                " while the device was suspended. It was not possible to"
                " verify power-state-report log after resuming."
            )
            pwm_session.reconnect()

        logging.info(
            f"Verify VM has not been silently restarted to ensure the"
            f" reliability of test"
        )
        qvm_processes_info = qnx_process_management.query_processes_info(
            command_executor=self.host_command, process_identifier="qvm"
        )
        asserts.assert_equal(
            vm_config.extract_current_qvm_pid(qvm_processes_info),
            vm_config.qvm_pid,
            msg=(
                f"{vm_config.sdv_guest_name} VM restarted silently during the"
                " test, so suspend resume cycle cannot be validated. See"
                " b/484317350"
            ),
        )

        logging.info(f"{vm_config.sdv_guest_name} VM resumed successfully")
