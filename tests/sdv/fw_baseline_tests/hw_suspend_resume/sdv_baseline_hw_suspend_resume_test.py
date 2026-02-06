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

"""Baseline test for verifying the hardware suspend/resume testing flow.

This test ensures the reliability and stability of the suspend/resume testing
infrastructure on hardware.

The test verifies:
  1. Connection to the hypervisor.
  2. Responsiveness of the test environment.
  3. Correct hypervisor setup for suspend/resume operations.
  4. Basic suspend and resume functionality.
  5. Suspend and resume with a short idle period to ensure CI/CD support of
     complex scenarios.
"""

import logging
import time
from absl.testing import parameterized
from mobly import asserts
from mobly import expects
from mobly.controllers.android_device_lib.adb import AdbError
import pexpect
from pexpect import pxssh
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner
from sdv_test_fw.verification import polling


class SdvBaselineHwSuspendResumeTest(
    sdv_base_test.SdvBaseTestClass, parameterized.TestCase
):
    LOCAL_PORT = "12222"
    LAB_PORT = "22"
    SSH_USERNAME = "root"
    SSH_PASSWORD = "root"

    VERIFY_CONNECTION_TEXT = "Connection works"

    FAKE_POWERBTN_DAEMON_VM1_ID = "PowerbtnDaemonVM1"
    FAKE_POWERBTN_DAEMON_VM1 = (
        "on -d -t /dev/null sh -c '(while true; do sleep 98765;"
        f" done) > /dev/ttyp6' {FAKE_POWERBTN_DAEMON_VM1_ID}"
    )

    ADDRESS_COMMAND_VM1 = (
        'awk \'/vdev pl011/ {{s=1}} /^$/ {{s=0}} s==1 && $1 == "loc" {{print'
        " $2}}' '/guests/android/sdv-1/sdv-1.conf'"
    )
    ENABLE_WAKEUP_VM1 = (
        "echo enabled >"
        " /sys/devices/platform/vdevs/{memory_address}.uart/tty/ttyAMA0/power/wakeup"
    )

    VEPSM_POWER_STATE = "vepsm power-state {vpm_action}"
    VEPSM_POWER_STATE_OUTPUT = (
        "Received power-state-report from VPM {vpm_status} , reason"
        " HOST_REQUESTED"
    )

    # The absence of the env file indicates the VM may be rebooting or something
    # happened, so it is not possible to check its status. This is an edge case
    # but we need to ensure the file exists before parsing it to acuurately
    # distinguish between 'Suspended' and 'Running' states.
    SDV_VM_STATUS = (
        "[ -e /dev/qvm/{sdv_vm}/env ] && {{ "
        "cat /dev/qvm/{sdv_vm}/env "
        "| grep 'last_known_ip' "
        "| awk 'NR > 1 {{ if ($5 != \"0\") exit 1 }}' "
        "&& echo 'Suspended' || echo 'Running'; "
        "}} || echo 'Not available'"
    )
    VM_SUSPENDED = "Suspended"
    VM_RUNNING = "Running"
    VM_STATUS_NOT_AVAILABLE = "Not available"

    WAKE_UP_VM1 = "echo >> /dev/ttyp6"

    SPAWNED_PROCESSES = "pidin -f aA | grep {process}"

    def _connect_to_hypervisor_qnx(self):
        # Hypervisor QNX is common for all VMs, so it is only necessary
        # to connect once. We take device1 serial it always exists independently
        # of he number of VMs the test requires.
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

    def _start_fake_powerbtn_daemon(self):
        # Only start the daemon if there is not one running already in the
        # hypervisor. This is to avoid spawning multiple processes in the QNX
        # that cannot be killed and have the same purpose. Minimize the number
        # of zombie processes we leave in the hypervisor after the test
        # finalizes.
        logging.info(f"Start daemon in hypervisor")

        if self._processes_are_running(self.FAKE_POWERBTN_DAEMON_VM1_ID):
            logging.info(
                f"Daemon with tag {self.FAKE_POWERBTN_DAEMON_VM1_ID} already"
                " running"
            )
            return

        self._host_command(self.FAKE_POWERBTN_DAEMON_VM1)

    def _enable_fake_powerbtn(self):
        logging.info(f"Prepare fake powerbtn in VM1")
        self._start_fake_powerbtn_daemon()

        self._host_command(self.ADDRESS_COMMAND_VM1)
        # The output is with format 0x1c090000. We are only interested on
        # the value after 0x
        vdevs_vm1_memory_location = self._host_output_last_line()[2:]
        logging.info(f"VM1 memory location: {vdevs_vm1_memory_location}")

        self.sdv_device1.adb().execute_shell_command(
            self.ENABLE_WAKEUP_VM1.format(
                memory_address=vdevs_vm1_memory_location
            )
        )
        logging.info("Fake powerbtn enabled in VM1")

    def _host_command(self, command, timeout=5):
        try:
            self.host_session.sendline(command)
            self.host_session.prompt(timeout=timeout)

            logging.debug("Host command sent:")
            logging.debug("START-------------------")
            logging.debug(self._host_output())
            logging.debug("-------------------END")

        except pexpect.TIMEOUT:
            logging.error(f"{command} timed out")
            raise

    def _host_output(self):
        return self.host_session.before.decode("utf-8")

    def _host_output_last_line(self):
        return self._host_output().splitlines()[-1]

    def _find_spawned_processes(self, process_identifier):
        self._host_command(
            self.SPAWNED_PROCESSES.format(process=process_identifier)
        )
        output_lines = self._host_output().splitlines()

        spawned_processes = []
        for line in output_lines:
            if "pidin" in line or "grep" in line:
                continue

            pidin_output = line.split()
            # The pid is expected to be the first element
            # 000000 process_info
            if pidin_output and pidin_output[0].isdigit():
                spawned_processes.append(pidin_output[0])

        return spawned_processes

    def _processes_are_running(self, process_identifier):
        pids = self._find_spawned_processes(process_identifier)
        if pids:
            return True
        return False

    def _device_status(self):
        self._host_command(self.SDV_VM_STATUS.format(sdv_vm="sdv-1"))
        status = self._host_output_last_line()
        logging.debug(f"VM status: {status}")
        asserts.assert_not_equal(
            status,
            self.VM_STATUS_NOT_AVAILABLE,
            "Not possible to check status of the VM.",
        )
        return status

    def _device_is_suspended(self):
        return self._device_status() == self.VM_SUSPENDED

    def _device_is_running(self):
        return self._device_status() == self.VM_RUNNING

    def setup_class(self):
        super().setup_class()
        self.sdv_device1 = self.get_device("device1")

        self._connect_to_hypervisor_qnx()
        self._enable_fake_powerbtn()

    def setup_test(self):
        super().setup_test()

        # Open session for Power Management
        self.sdv_pwm_session = self.sdv_device1.adb().interactive_session(
            label="PWM"
        )

    def teardown_test(self):
        logging.info("Cleaning up after test case.")
        # end Power Management session
        self.sdv_pwm_session.close()
        super().teardown_test()

    def teardown_class(self):
        logging.info("Cleaning up after test.")
        # Concluding the sleep process makes adb connection to get lost
        # because the device hangs. We cannot clean up the spawned processes
        # in QNX. This is a known limitation of the current approach.

        # End connection to QNX.
        self.host_session.logout()
        super().teardown_class()

    def test_verify_host_connection(self):
        self._host_command(f"echo {self.VERIFY_CONNECTION_TEXT}")

        logging.info(
            f"Verification echo output: {self._host_output_last_line()}"
        )
        asserts.assert_equal(
            self._host_output_last_line(), self.VERIFY_CONNECTION_TEXT
        )

    def test_powerbtn_daemon_is_running_in_host(self):
        asserts.assert_true(
            self._processes_are_running(self.FAKE_POWERBTN_DAEMON_VM1_ID),
            "Daemon to wake up device is not running",
        )

    def test_device_is_responsive(self):
        asserts.assert_true(self._device_is_running(), "Device is not running")

        command = f'echo "{self.VERIFY_CONNECTION_TEXT}"'
        output = self.sdv_device1.adb().execute_shell_command(command)
        asserts.assert_equal(output, self.VERIFY_CONNECTION_TEXT)

    @parameterized.named_parameters(
        {
            "testcase_name": "",
            "idle_seconds": 0,
        },
        {
            "testcase_name": "idle_15_secs",
            "idle_seconds": 15,
        },
    )
    def test_suspend_resume_hw(self, idle_seconds):
        logging.info("Suspend VM using Power Management")

        self.sdv_pwm_session.send_command_and_wait_for_outputs(
            self.VEPSM_POWER_STATE.format(vpm_action="power-on"),
            [self.VEPSM_POWER_STATE_OUTPUT.format(vpm_status="ON")],
        )
        self.sdv_pwm_session.send_command_and_wait_for_outputs(
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
        self.sdv_pwm_session.send_command(
            self.VEPSM_POWER_STATE.format(vpm_action="finish ram")
        )

        logging.info("Waiting for device to suspend.")
        result = polling.wait_for_true(
            self._device_is_suspended,
            timeout=30,
            assert_msg="Device did not suspend",
        )

        logging.info(f"Do nothing for {idle_seconds} seconds.")
        time.sleep(idle_seconds)
        logging.info(f"{idle_seconds} seconds passed.")

        logging.info("Waking up device from host")
        self._host_command(self.WAKE_UP_VM1)

        logging.info("Waiting for device to wake up.")
        result = polling.wait_for_true(
            self._device_is_running,
            timeout=30,
            assert_msg="Device did not resume",
        )

        self.sdv_pwm_session.expect_outputs([
            self.VEPSM_POWER_STATE_OUTPUT.format(
                vpm_status="SUSPEND_TO_RAM_EXIT"
            )
        ])


if __name__ == "__main__":
    sdv_test_runner.run()
