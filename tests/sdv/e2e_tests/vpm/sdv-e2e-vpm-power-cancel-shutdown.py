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

"""SDV VPM CANCEL SHUTDOWN TEST."""

from mobly import asserts
import collections
import logging
import time

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from vpm.sdv_vpm import SdvVpm


class SdvE2EVpmCancelShutDownTest(sdv_base_test.SdvBaseTestClass):

    POWER_STATE_CLIENT_COMMAND = 'vpm_power_state_client'

    def setup_class(self):
        """Sets up the test class"""
        super().setup_class()
        self.adb_device = self.get_device('device1').adb()
        self.device_vpm = SdvVpm(self.adb_device)

    def start_powerstateclient(self):
        self.power_state_client_log = (
            self.adb_device.execute_shell_command_in_subprocess_log(
                self.POWER_STATE_CLIENT_COMMAND
            )
        )

    def check_powerstateclientlog(self):
        expected_log = (
            r'Successfully subscribed to vpm, current report r#\S+\n'
            r'VPM client received power state change r#ON\n'
            r'VPM client received power state change r#POWER_OFF_ENTER\n'
            r'VPM client received power state change r#WAIT_FOR_FINISH\n'
            r'VPM client received power state change r#SHUTDOWN_CANCELLED'
        )
        log = self.adb_device.read_file(self.power_state_client_log)
        asserts.assert_regex(
            log,
            expected_log,
            'Power state changes are not received correctly in the power state'
            ' client',
        )

    def test_cancel_shutdown(self):
        """Tests cancel shutdown"""
        self.start_powerstateclient()
        self.device_vpm.prepare_shutdown_vm()
        self.device_vpm.cancel_shutdown_vm()
        self.check_powerstateclientlog()


if __name__ == '__main__':
    sdv_test_runner.run()
