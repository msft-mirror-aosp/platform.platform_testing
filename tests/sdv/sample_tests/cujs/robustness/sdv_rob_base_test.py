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

"""Base class for SDV Robustness e2e tests."""


import logging



from cuj22_common.sdv_cuj22_base import SdvCuj22Base
from sdv_test_fw.test_execution import sdv_base_test
from vpm.sdv_vpm import SdvVpm
import mobly.utils as utils
from sdv_test_fw.device.sdv_property import SdvDeviceProperty


class SdvRobBaseTest(sdv_base_test.SdvBaseTestClass, SdvCuj22Base):
    """Base class for SDV Robustness tests."""
    RESTART_COMMAND = 'cvd -instance-name \"{}\" restart'
    SUSPEND_DURATION_S = 90
    SUSPEND_RESUME_BUFFER_DURATION_S = 30


    def setup_class(self):
        """Sets up the test class."""
        super().setup_class()
        # Use CUJ22 base to set up a standard server (device1) and client (device2).
        self.setup_cuj22_devices()
        self.client_instance_name = self.adb_device_client.prop.get(SdvDeviceProperty.INSTANCE_NAME)
        self.server_instance_name = self.adb_device_server.prop.get(SdvDeviceProperty.INSTANCE_NAME)

    def setup_test(self):
        """Sets up each test."""
        # Before each test, we ensure communication is working.
        self.assert_comms_working(
            "Initial communication check before test execution."
        )

    def assert_comms_working(self, context_message):
        """
        Asserts that cross-VM communication is working.
        This is a placeholder that should be implemented by checking
        for specific log messages from a sample pub/sub or RPC application,
        similar to what is described in CUJ-CORE-22/23/26 READMEs.
        """
        logging.info(
            "Verifying cross-VM communication using CUJ-22 checks:"
            f" {context_message}"
        )
        self.check_all_comm_stack_logs()

    def power_cycle_server_vm(self):
        """Power cycles the server VM using adb reboot."""
        self._power_cycle_vm(self.adb_device_server, "server")

    def power_cycle_client_vm(self):
        """Power cycles the client VM using adb reboot."""
        self._power_cycle_vm(self.adb_device_client, "client")

    def _power_cycle_vm(self, device, role: str):
        """Power cycles a VM using adb reboot."""
        device.log().info(
            f"Power cycling {role} VM {device} with 'adb reboot'."
        )
        device.reboot_device_and_verify_logcat()
        logging.info(f"{role.capitalize()} VM {device} is back online.")

    def graceful_shutdown_server_vm(self):
        """Gracefully shuts down the server VM using VPM."""
        self._graceful_shutdown_vm(self.adb_device_server, self.server_vpm, "server")

    def graceful_shutdown_client_vm(self):
        """Gracefully shuts down the client VM using VPM."""
        self._graceful_shutdown_vm(self.adb_device_client, self.client_vpm, "client")

    def _graceful_shutdown_vm(self, device, device_vpm, role: str):
        """Gracefully shuts down a VM using VPM."""
        device.log().info(
            f"Gracefully shutting down {role} VM {device} using VPM."
        )
        device_vpm.shutdown_vm()
        device.wait_for_device_offline(timeout=60)
        logging.info(f"{role.capitalize()} VM {device} is offline.")

    def power_on_server_vm(self):
        """Powers on the server VM."""
        self._power_on_vm(
            self.server_instance_name, self.adb_device_server, "server"
        )

    def power_on_client_vm(self):
        """Powers on the client VM."""
        self._power_on_vm(
            self.client_instance_name, self.adb_device_client, "client"
        )

    def _power_on_vm(self, instance_name: str, device, role: str):
        """Powers on a VM."""
        utils.run_command(
            self.RESTART_COMMAND.format(instance_name), shell=True
        )
        # Wait for device to be online.
        device.wait_for_device_online()
        device.log().info(f"Powering on {role} VM {instance_name}.")
        device.verify_logcat_is_running()
        device.log().info(f"{role.capitalize()} VM {instance_name} is back online.")

    def suspend_server_vm(self):
        """Suspends the server VM for a fixed duration."""
        self.adb_device_server.log().info(
            f"Suspending server VM for {self.SUSPEND_DURATION_S} seconds.")
        self._suspend_vm(self.server_vpm)

    def suspend_client_vm(self):
        """Suspends the client VM for a fixed duration."""
        self.adb_device_client.log().info(
            f"Suspending client VM for {self.SUSPEND_DURATION_S} seconds.")
        self._suspend_vm(self.client_vpm)

    def _suspend_vm(self, device_pm: SdvVpm):
        """Suspends a VM for a fixed duration."""
        device_pm.suspend_vm_for_time(str(self.SUSPEND_DURATION_S))

    def wait_for_server_resume(self):
        """Waits for the server VM to resume from suspension."""
        self.adb_device_server.log().info(f"Waiting for server VM to resume...")
        self._wait_for_resume(self.server_vpm)

    def wait_for_client_resume(self):
        """Waits for the client VM to resume from suspension."""
        self.adb_device_client.log().info(f"Waiting for client VM to resume...")
        self._wait_for_resume(self.client_vpm)

    def _wait_for_resume(self, device_pm: SdvVpm):
        """Waits for a VM to resume for the suspend duration."""
        device_pm.wait_for_vm_to_resume_from_ram(timeout=self.SUSPEND_DURATION_S + self.SUSPEND_RESUME_BUFFER_DURATION_S)
