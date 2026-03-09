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

"""SDV VPM SHUTDOWN TEST."""

import time
import logging
import collections

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from vpm.sdv_vpm import SdvVpm


class SdvVpmShutDownTest(sdv_base_test.SdvBaseTestClass):
    VPM_POWER_STATE_CLIENT_POWER_ON = "vepsm power-state power-on"
    VPM_POWER_STATE_CLIENT_PREPARE_SHUT_DOWN = "vepsm power-state prepare shut-down"
    VPM_POWER_STATE_CLIENT_FINISH_SHUT_DOWN = "vepsm power-state finish shut-down"

    VPM_POWER_STATE_LISTENER_PROCESS_NAME = 'vpm_power_state_listener'
    VPM_POWER_STATE_LISTENER_COMMAND = 'vpm_power_state_client'
    VPM_POWER_STATE_LISTENER_LOG_PATH = (
        '/data/local/tmp/vpm_power_state_listener_client.log'
    )
    COMMAND_TO_REMOVE_LOG = 'rm {log_file_path}'
    COMMAND_TO_READ_LOG = 'cat {log_file_path}'

    VPM_POWER_STATE_RESPONSE = "Received power-state-report from VPM {state} , reason HOST_REQUESTED"
    VPM_POWER_STATE_LISTENER_RESPONSE = "VPM client received power state change r#{state}"
    VPM_LISTENER_SUCCESSFUL_SUBSCRIPTION = (
        "Successfully subscribed to vpm, current report r#POWER_OFF_EXIT"
    )

    def setup_class(self):
        """Sets up the test class"""
        super().setup_class()
        self.adb_device = self.get_device("device1").adb()
        self.device_vpm = SdvVpm(self.adb_device)
        self.adb_device.wait_for_device_online()


    def check_power_state_listener_process_alive(self, adb_device):
        return adb_device.is_subprocess_running(
            self.VPM_POWER_STATE_LISTENER_PROCESS_NAME
        )


    def start_vpm_power_state_listener(self, adb_device):
        logging.info(
            'Starting VPM State Listener on device'
            f' [{adb_device.get_device_serial()}]'
        )

        # Since the VPM Listener is blocking, execute it as subprocess
        # Also, redirect the output to a file for verification later
        command_to_start_vpm_state_listener = (
            '{vpm_listener_command} > {vpm_listener_log}'.format(
                vpm_listener_command=self.VPM_POWER_STATE_LISTENER_COMMAND,
                vpm_listener_log=self.VPM_POWER_STATE_LISTENER_LOG_PATH,
            )
        )
        adb_device.execute_shell_command_in_subprocess(
            self.VPM_POWER_STATE_LISTENER_PROCESS_NAME,
            command_to_start_vpm_state_listener,
        )

        if not self.check_power_state_listener_process_alive(adb_device):
            raise Exception(
                'Unable to start VPM State Listener on device'
            )


    def stop_vpm_power_state_listener(self, adb_device):
        """Stops the VPM power state client and cleans up its log."""
        logging.info(
            'Stopping VPM power state client on device'
            f' [{adb_device.get_device_serial()}]'
        )

        if not self.check_power_state_listener_process_alive(adb_device):
            logging.warning(
                'VPM Listener is not running on device [<%s>]',
                adb_device.get_device_serial(),
            )
            return

        adb_device.terminate_subprocess(
            self.VPM_POWER_STATE_LISTENER_PROCESS_NAME
        )

        remove_log_command = self.COMMAND_TO_REMOVE_LOG.format(
            log_file_path=self.VPM_POWER_STATE_LISTENER_LOG_PATH
        )
        adb_device.execute_shell_command(remove_log_command)

        if self.check_power_state_listener_process_alive(adb_device):
            raise Exception(
                'Failed to stop VPM State Listener on device'
                f' {adb_device.get_device_serial()}'
            )

        logging.info(
            'Stopped VPM State Listener on device <%s>',
            adb_device.get_device_serial(),
        )
        return


    def get_vpm_power_state_listener_response(self, adb_device):
        return adb_device.execute_shell_command(
            f'{self.COMMAND_TO_READ_LOG.format(log_file_path=self.VPM_POWER_STATE_LISTENER_LOG_PATH)}')


    def test_shutdown(self):
        """Tests shutdown"""
        self.start_vpm_power_state_listener(self.adb_device)
        self.device_vpm.shutdown_vm()

    def teardown_class(self):
        # Manually destroy controllers.
        # This is necessary because the device goes offline during the test,
        # which prevents the default Mobly controller cleanup mechanism
        # from executing correctly.
        for name, module in self._controller_manager._controller_modules.items():
            logging.debug('Destroying %s.', name)
            try:
                module.destroy(self._controller_manager._controller_objects[name])
            except Exception as e:
                logging.exception(e)
        self._controller_manager._controller_objects = collections.OrderedDict()
        self._controller_manager._controller_modules = {}

        super().teardown_class()


if __name__ == "__main__":
    sdv_test_runner.run()
