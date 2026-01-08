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

"""Sample Orchestrator test for verifying the three VMs scenario of custom modes.

Tests is on three SDV VM
"""
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods

class SdvSampleOrchestrationCustomModesThreeVmsTest(
    sdv_base_test.SdvBaseTestClass
):
    SAMPLE_VM_1_PROCESS_NAME = 'custom_mode_process_vm_1'
    SAMPLE_VM_2_PROCESS_NAME = 'custom_mode_process_vm_2'
    SAMPLE_VM_3_PROCESS_NAME = 'custom_mode_process_vm_3'
    START_SAMPLE_CHARGING_COMMAND = (
        'orch_custom_mode_sample {mode_name} {state}'
    )
    CHARGING_COMMAND = 'CHARGING'
    ON_COMMAND = 'ON'
    OFF_COMMAND = 'OFF'
    IDLE_COMMAND = 'IDLE'
    SUCCESS_CUSTOM_MODE_LOGCAT_GREP_TEXT_TEMPLATE = (
        'Finished enforcing mode \'Custom\("{mode_name}"\)\' with state'
        ' \'"{state}"\'. successfully'
    )

    def setup_class(self):
        super().setup_class()
        self.sdv_device_vm1 = self.get_device('device1').adb()
        self.sdv_device_vm2 = self.get_device('device2').adb()
        self.sdv_device_vm3 = self.get_device('device3').adb()

    def verify_vms_received_charging(self, charging_value):
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device_vm1,
            grep_text="sdv_orchestration_agent",
            expected_result=self.SUCCESS_CUSTOM_MODE_LOGCAT_GREP_TEXT_TEMPLATE.format(
                mode_name=self.CHARGING_COMMAND, state=charging_value
            ),
            assert_msg=f"VM1 did not enforce CHARGING {charging_value} custom mode",
        )
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device_vm2,
            grep_text="sdv_orchestration_agent",
            expected_result=self.SUCCESS_CUSTOM_MODE_LOGCAT_GREP_TEXT_TEMPLATE.format(
                mode_name=self.CHARGING_COMMAND, state=charging_value
            ),
            assert_msg=f"VM2 did not enforce CHARGING {charging_value} custom mode",
        )
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device_vm3,
            grep_text="sdv_orchestration_agent",
            expected_result=self.SUCCESS_CUSTOM_MODE_LOGCAT_GREP_TEXT_TEMPLATE.format(
                mode_name=self.CHARGING_COMMAND, state=charging_value
            ),
            assert_msg=f"VM3 did not enforce CHARGING {charging_value} custom mode",
        )

    def test_orchestration_custom_modes_three_vms(self):
        logging.info('Start Orchestration Custom Modes Three VMs Test')

        # VM1 sends CHARGING ON and it needs to be received by all 3 vms
        logging.info(
            'Sending custom mode CHARGING ON from device:'
            f' {self.sdv_device_vm1.get_device_serial()}'
        )
        # Since the command is blocking, execute it as subprocess
        self.sdv_device_vm1.execute_shell_command_in_subprocess(
            self.SAMPLE_VM_1_PROCESS_NAME,
            self.START_SAMPLE_CHARGING_COMMAND.format(
                mode_name=self.CHARGING_COMMAND, state=self.ON_COMMAND
            ),
        )
        # Verify CHARGING ON mode was received by the three VMs
        self.verify_vms_received_charging(self.ON_COMMAND)

        # VM2 sends CHARGING OFF and it needs to be received by all 3 vms
        logging.info(
            'Sending custom mode CHARGING OFF from device:'
            f' {self.sdv_device_vm2.get_device_serial()}'
        )
        # Since the command is blocking, execute it as subprocess
        self.sdv_device_vm2.execute_shell_command_in_subprocess(
            self.SAMPLE_VM_2_PROCESS_NAME,
            self.START_SAMPLE_CHARGING_COMMAND.format(
                mode_name=self.CHARGING_COMMAND, state=self.OFF_COMMAND
            ),
        )
        # Verify CHARGING OFF mode was received by the three VMs
        self.verify_vms_received_charging(self.OFF_COMMAND)

        # VM3 sends CHARGING IDLE and it needs to be received by all 3 vms
        logging.info(
            'Sending custom mode CHARGING IDLE from device:'
            f' {self.sdv_device_vm2.get_device_serial()}'
        )
        # Since the command is blocking, execute it as subprocess
        self.sdv_device_vm3.execute_shell_command_in_subprocess(
            self.SAMPLE_VM_3_PROCESS_NAME,
            self.START_SAMPLE_CHARGING_COMMAND.format(
                mode_name=self.CHARGING_COMMAND, state=self.IDLE_COMMAND
            ),
        )
        # Verify CHARGING IDLE mode was received by the three VMs
        self.verify_vms_received_charging(self.IDLE_COMMAND)

        logging.info('End SDV Orchestration Custom Modes Three VMs Test')

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
