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

"""SDV Vehicle Power Management Test

Tests Vehicle Power Management For SDV
"""
from mobly import asserts
from enum import Enum
import logging
import time

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from vpm.sdv_vpm import SdvVpm

class SdvSampleVpmTest(sdv_base_test.SdvBaseTestClass):

    EXPECTED_RESPONSE_FOR_VPM_STATE_LISTENER = (
        'Received vehicle state VehicleStateChange {{ vehicle_state: {state}'
    )
    EXPECTED_RESPONSE_FOR_SET_VPM_STATE_COMMAND = (
        'Vehicle mode set successfully {state}'
    )

    def setup_class(self):
        logging.info('Setting Up SDV Vehicle Power Management Test Class')
        super().setup_class()
        # Get the device uisng label
        self.sdv_device = self.get_device('device1')
        self.__vpm = SdvVpm(self.sdv_device.adb())
        logging.info('End Setup Class For SDV Vehicle Power Management')

        # Save the current value of sdv.authz.enable
        self.sdv_authz_enable_value = (
            self.sdv_device.adb().execute_shell_command(
                'getprop sdv.authz.enable'
            )
        )

    # Get Vehicle Power Management
    def vpm(self):
        return self.__vpm

    def setup_test(self):
        # Setup to run tests
        logging.info('Starting SDV Vehicle Power Management Test Setup')

        super().setup_test()

        # Reboot Device to ensure clean state
        self.sdv_device.adb().reboot_device()

        # Root Device
        self.sdv_device.adb().root_device()

        # Enforce SDV Comm Stack authorization
        self.sdv_device.adb().execute_shell_command(
            'setprop sdv.authz.enable true'
        )

        # Start VPM State Listener
        self.vpm().start_vpm_state_listener()

        logging.info('Done SDV Vehicle Power Management Test Setup')

    def test_set_vehicle_to_low_power_mode(self):
        logging.info(
            'Start SDV Vehicle Power Management:'
            ' test_set_vehicle_to_low_power_mode'
        )

        response = self.vpm().set_vehicle_state(
            self.vpm().vpm_states().LOW_POWER
        )

        # Verify the response of set vpm state command
        expected_response = (
            self.EXPECTED_RESPONSE_FOR_SET_VPM_STATE_COMMAND.format(
                state='LowPower'
            )
        )
        asserts.assert_in(
            expected_response,
            response,
            f'Actual Result [{response}] does not match Expected Result'
            f' [{expected_response}]',
        )

        # Verify VPM State Listener Response
        response = self.vpm().get_vpm_state_listener_response()
        expected_response = (
            self.EXPECTED_RESPONSE_FOR_VPM_STATE_LISTENER.format(
                state=self.vpm().vpm_states().LOW_POWER.name
            )
        )
        asserts.assert_in(
            expected_response,
            response,
            f'Actual Result [{response}] does not contain Expected Result'
            f' [{expected_response}]',
        )

        logging.info(
            'End SDV Vehicle Power Management Test:'
            ' test_set_vehicle_to_low_power_mode'
        )

    def test_set_vehicle_to_software_update_mode(self):
        logging.info(
            'Start SDV Vehicle Power Management:'
            ' test_set_vehicle_to_software_update_mode'
        )

        response = self.vpm().set_vehicle_state(
            self.vpm().vpm_states().SOFTWARE_UPDATE
        )

        # Verify the response of set vpm state command
        expected_response = (
            self.EXPECTED_RESPONSE_FOR_SET_VPM_STATE_COMMAND.format(
                state='SoftwareUpdate'
            )
        )
        asserts.assert_in(
            expected_response,
            response,
            f'Actual Result [{response}] does not match Expected Result'
            f' [{expected_response}]',
        )

        # Verify VPM State Listener Response
        response = self.vpm().get_vpm_state_listener_response()
        expected_response = (
            self.EXPECTED_RESPONSE_FOR_VPM_STATE_LISTENER.format(
                state=self.vpm().vpm_states().SOFTWARE_UPDATE.name
            )
        )
        asserts.assert_in(
            expected_response,
            response,
            f'Actual Result [{response}] does not contain Expected Result'
            f' [{expected_response}]',
        )

        logging.info(
            'End SDV Vehicle Power Management Test:'
            ' test_set_vehicle_to_low_power_mode'
        )

    def test_set_vehicle_to_park_mode(self):
        logging.info(
            'Start SDV Vehicle Power Management: test_set_vehicle_to_park_mode'
        )

        response = self.vpm().set_vehicle_state(self.vpm().vpm_states().PARK)

        # Verify the response of set vpm state command
        expected_response = (
            self.EXPECTED_RESPONSE_FOR_SET_VPM_STATE_COMMAND.format(
                state='Park'
            )
        )
        asserts.assert_in(
            expected_response,
            response,
            f'Actual Result [{response}] does not match Expected Result'
            f' [{expected_response}]',
        )

        # Verify VPM State Listener Response
        response = self.vpm().get_vpm_state_listener_response()
        expected_response = (
            self.EXPECTED_RESPONSE_FOR_VPM_STATE_LISTENER.format(
                state=self.vpm().vpm_states().PARK.name
            )
        )
        asserts.assert_in(
            expected_response,
            response,
            f'Actual Result [{response}] does not contain Expected Result'
            f' [{expected_response}]',
        )

        logging.info(
            'End SDV Vehicle Power Management Test:'
            ' test_set_vehicle_to_park_mode'
        )

    def test_set_vehicle_to_life_on_board_mode(self):
        logging.info(
            'Start SDV Vehicle Power Management:'
            ' test_set_vehicle_to_life_on_board_mode'
        )

        response = self.vpm().set_vehicle_state(
            self.vpm().vpm_states().LIFE_ON_BOARD
        )

        # Verify the response of set vpm state command
        expected_response = (
            self.EXPECTED_RESPONSE_FOR_SET_VPM_STATE_COMMAND.format(
                state='LifeOnBoard'
            )
        )
        asserts.assert_in(
            expected_response,
            response,
            f'Actual Result [{response}] does not match Expected Result'
            f' [{expected_response}]',
        )

        # Verify VPM State Listener Response
        response = self.vpm().get_vpm_state_listener_response()
        expected_response = (
            self.EXPECTED_RESPONSE_FOR_VPM_STATE_LISTENER.format(
                state=self.vpm().vpm_states().LIFE_ON_BOARD.name
            )
        )
        asserts.assert_in(
            expected_response,
            response,
            f'Actual Result [{response}] does not contain Expected Result'
            f' [{expected_response}]',
        )

        logging.info(
            'End SDV Vehicle Power Management Test:'
            ' test_set_vehicle_to_life_on_board_mode'
        )

    def test_set_vehicle_to_vehicle_on_mode(self):
        logging.info(
            'Start SDV Vehicle Power Management:'
            ' test_set_vehicle_to_vehicle_on_mode'
        )

        response = self.vpm().set_vehicle_state(
            self.vpm().vpm_states().VEHICLE_ON
        )

        # Verify the response of set vpm state command
        expected_response = (
            self.EXPECTED_RESPONSE_FOR_SET_VPM_STATE_COMMAND.format(
                state='VehicleOn'
            )
        )
        asserts.assert_in(
            expected_response,
            response,
            f'Actual Result [{response}] does not match Expected Result'
            f' [{expected_response}]',
        )

        # Verify VPM State Listener Response
        response = self.vpm().get_vpm_state_listener_response()
        expected_response = (
            self.EXPECTED_RESPONSE_FOR_VPM_STATE_LISTENER.format(
                state=self.vpm().vpm_states().VEHICLE_ON.name
            )
        )
        asserts.assert_in(
            expected_response,
            response,
            f'Actual Result [{response}] does not contain Expected Result'
            f' [{expected_response}]',
        )

        logging.info(
            'End SDV Vehicle Power Management Test:'
            ' test_set_vehicle_to_vehicle_on_mode'
        )

    def test_set_vehicle_to_traction_on_mode(self):
        logging.info(
            'Start SDV Vehicle Power Management:'
            ' test_set_vehicle_to_traction_on_mode'
        )

        response = self.vpm().set_vehicle_state(
            self.vpm().vpm_states().TRACTION_ON
        )

        # Verify the response of set vpm state command
        expected_response = (
            self.EXPECTED_RESPONSE_FOR_SET_VPM_STATE_COMMAND.format(
                state='TractionOn'
            )
        )
        asserts.assert_in(
            expected_response,
            response,
            f'Actual Result [{response}] does not match Expected Result'
            f' [{expected_response}]',
        )

        # Verify VPM State Listener Response
        response = self.vpm().get_vpm_state_listener_response()
        expected_response = (
            self.EXPECTED_RESPONSE_FOR_VPM_STATE_LISTENER.format(
                state=self.vpm().vpm_states().TRACTION_ON.name
            )
        )
        asserts.assert_in(
            expected_response,
            response,
            f'Actual Result [{response}] does not contain Expected Result'
            f' [{expected_response}]',
        )

        logging.info(
            'End SDV Vehicle Power Management Test:'
            ' test_set_vehicle_to_traction_on_mode'
        )

    def teardown_test(self):
        logging.info('Start SDV Vehicle Power Management Test Teardown')

        # Stop VPM State Listener
        self.vpm().stop_vpm_state_listener()

        # Clear subprocess map
        logging.info('Stop all sub processes')
        self.sdv_device.adb().terminate_all_subprocesses()

        super().teardown_test()
        logging.info('Done SDV Vehicle Power Management Test Teardown')

    def teardown_class(self):
        # Reset SDV Comm Stack authorization
        self.sdv_device.adb().execute_shell_command(
            f'setprop sdv.authz.enable {self.sdv_authz_enable_value}'
        )

        super().teardown_class()


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
