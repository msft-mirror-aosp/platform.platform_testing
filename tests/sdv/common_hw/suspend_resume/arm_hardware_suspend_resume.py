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

from dataclasses import dataclass
import time
import logging
from pexpect import pxssh

from sdv_test_fw.device.sdv_device import SdvDeviceAdb
from sdv_test_fw.device.sdv_property import SdvDeviceProperty

KEEP_TTYS_ALIVE_COMMAND = "on -d -t /dev/null sh -c '(while true; do sleep 99999; done) > /dev/ttyp6'; on -d -t /dev/null sh -c '(while true; do sleep 99999; done) > /dev/ttyp7';"
GET_VDEV_ADDRESS_COMMAND = "awk '/vdev pl011/ {{s=1}} /^$/ {{s=0}} s==1 && $1 == \"loc\" {{print $2}}' '/guests/android/sdv-{id}/sdv-{id}.conf'"
LOC_RESPONSE_REGEX = r'0x[0-9a-fA-F]+'
AWAKE_SUSPENDED_VM_COMMAND = 'echo >> /dev/ttyp{id}'
VEPSM_POWER_STATE_ON_COMMAND = 'vepsm power-state power-on'
VEPSM_POWER_STATE_ON_EXPECTED_OUTPUT = "Received power-state-report from VPM ON , reason HOST_REQUESTED"
VEPSM_POWER_STATE_PREPARE_RAM_COMMAND = 'vepsm power-state prepare ram'
VEPSM_POWER_STATE_PREPARE_RAM_EXPECTED_OUTPUT = "Received power-state-report from VPM WAIT_FOR_FINISH , reason HOST_REQUESTED"
VEPSM_POWER_STATE_FINISH_RAM_COMMAND = 'vepsm power-state finish ram'
ENABLE_UART_WAKE_IP = "echo enabled > /sys/devices/platform/vdevs/{loc}.uart/tty/ttyAMA0/power/wakeup"


def _create_sdv_devlab_hypervisor_ssh_session(adb_device) -> pxssh.pxssh:
    """Note: one should close created ssh connection, pxssh.close() """
    if not adb_device.prop.get(SdvDeviceProperty.CPU_ARCH) == "arm64-v8a":
        raise Exception("Attempting to retrieve hypervisor ip address (for ssh purposes), "
                        "in a non-sdvlab context. Check logic, the standard CI environment"
                        "(crosvm, CF) does not support this")

    hypervisor_ip = adb_device.get_device_serial().split(':')[0]
    logging.info(f"Parsed hypervisor_ip: {hypervisor_ip}")

    s = None
    try:
        # try tunnelled port first. When testing on local machine, local host might have both
        # ports open, and tunnelled one is what points to the device host
        s = pxssh.pxssh()
        ssh_port_when_tunneled_from_local_machine = "12222"
        s.login(hypervisor_ip, port=ssh_port_when_tunneled_from_local_machine,
                username="root", password="root")
    except Exception as e:
        default_sdv_lab_ssh_port = "22"
        logging.info(f"Could not connect to tunneled port:{ssh_port_when_tunneled_from_local_machine},"
                     f"as per SDV instructions for using SDV Lab locally. exception: {e}."
                     f"Trying with port: {default_sdv_lab_ssh_port},"
                     "as it appears that test is ran from CI")
        s.close()
        s = pxssh.pxssh()
        s.login(hypervisor_ip, port=default_sdv_lab_ssh_port,
                username="root", password="root")

    if s:
        s.prompt()

    return s


def _prepare_device_and_host_for_resume(device_adb, device_id, host_ssh):
    # keep TTY alive on host side:
    # TODO: slay spawned process, ideally via test framework support
    host_ssh.sendline(KEEP_TTYS_ALIVE_COMMAND)
    host_ssh.prompt()

    host_ssh.sendline(GET_VDEV_ADDRESS_COMMAND.format(id=device_id))
    host_ssh.prompt()
    logging.info(
        f"GET_VDED_ADDRESS_COMMAND stdout: {host_ssh.before.decode('utf-8')}")
    vdev_memory_location = host_ssh.before.decode(
        'utf-8').splitlines()[-1][2:]
    logging.info(f"Parsed vdem_memory_location: {vdev_memory_location}")

    # Enable TTY input waking up the device:
    device_adb.execute_shell_command(
        ENABLE_UART_WAKE_IP.format(loc=vdev_memory_location))


def _resume_device(host_ssh, device_id):
    """
    Resumes a guest device, by sending input to a virtual device configured to resume device
    when receiving input

    MAGIC_TTY_OFFSET: constant related to vdev setup as part of qnx guest conf file. Details:
    https://android-auto-internal.googlesource.com/qnx-docker/+/670cdaf33cdcb65407f1c296e22c56eb2520ee05
    """
    MAGIC_TTY_OFFSET = 5
    host_ssh.sendline(
        AWAKE_SUSPENDED_VM_COMMAND.format(id=str(device_id+MAGIC_TTY_OFFSET)))
    host_ssh.prompt()


def _trigger_suspend(device_session):
    device_session.send_command_and_wait_for_outputs(
        VEPSM_POWER_STATE_ON_COMMAND,
        [
            VEPSM_POWER_STATE_ON_EXPECTED_OUTPUT,
        ])
    device_session.send_command_and_wait_for_outputs(
        VEPSM_POWER_STATE_PREPARE_RAM_COMMAND,
        [
            VEPSM_POWER_STATE_PREPARE_RAM_EXPECTED_OUTPUT,
        ])
    device_session.send_command(
        VEPSM_POWER_STATE_FINISH_RAM_COMMAND)


@dataclass
class SdvQnxDevice:
    """
    A SDV instance with adb connectivity, hosted by a QNX hypervisor.
    qnx_device_id: corresponds to qvm config used to start the Device: eg: 1 <-> sdv-1.conf 2 <-> sdv-2.conf
    """
    adb_device: SdvDeviceAdb
    qnx_guest_id: int


def suspend_and_resume_devices(devices: list[SdvQnxDevice], extra_wait_in_suspend=0):
    """
    Suspends multiple devices simultaneously and resumes them individually after waiting for
    input wait time.
    """
    # assuming all SDV devices share a hypervisor for the time being. Use device 0 to get host ip
    host_ssh = _create_sdv_devlab_hypervisor_ssh_session(devices[0].adb_device)
    sessions = [d.adb_device.interactive_session() for d in devices]

    for d, device_session in zip(devices, sessions):
        _prepare_device_and_host_for_resume(
            d.adb_device, d.qnx_guest_id, host_ssh)
        _trigger_suspend(device_session)

    # Device suspension is not instantaneous (approx 100ms measured on RPi, from VPM command),
    # and there is no clear log/event (as relevant logs are hw architecture dependent) that marks suspend
    # finalization. Thus, logic must wait before attempting resume from suspend.
    REQUIRED_SLEEP_POST_SUSPEND = 1
    time.sleep(REQUIRED_SLEEP_POST_SUSPEND + extra_wait_in_suspend)

    # resume for all devices and wait for confirmation
    for d, device_session in zip(devices, sessions):
        _resume_device(host_ssh, d.qnx_guest_id)
        device_session.expect_outputs(
            ["Received power-state-report from VPM SUSPEND_TO_RAM_EXIT , reason HOST_REQUESTED"])
        device_session.close()
    host_ssh.close()


def suspend_and_resume_device(adb_device: SdvQnxDevice, extra_wait_in_suspend=0):
    suspend_and_resume_devices([adb_device], extra_wait_in_suspend)
