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

"""E2E Orchestrator test for verifying that after a VM shutdowns and is started again (such as after a reboot or crash), then the custom mode publishers get unregistered and registers again.

After registration, all new custom mode publications should be received by other
VMs.

Tests is on two SDV VM
"""
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods

class SdvE2EOrchestrationUnregistersReconnectsCustomModeTest(
    sdv_base_test.SdvBaseTestClass
):
    SAMPLE_VM_1_PROCESS_NAME = 'custom_mode_process_vm_1'
    SAMPLE_VM_2_PROCESS_NAME = 'custom_mode_process_vm_2'
    START_CUSTOM_MODES_SAMPLE_COMMAND = (
        'su root orch_custom_mode_sample {mode_name} {state}'
    )
    CHARGING_COMMAND = 'CHARGING'
    ON_COMMAND = 'ON'
    OFF_COMMAND = 'OFF'
    SUCCESS_CUSTOM_MODE_LOGCAT_GREP_TEXT_TEMPLATE = (
        'Finished enforcing mode \'Custom\\("{mode_name}"\\)\' with state'
        ' \'"{state}"\'. successfully'
    )
    SUBSCRIBED_CUSTOM_MODE_LOGCAT_TEXT = (
        'Successfully subscribed for custom modes in vm "{vm_name}"'
    )
    UNREGISTERED_CUSTOM_MODE_LOGCAT_TEXT = (
        'Service Unit Definition unregistered for Unit Type "GlobalCustomStateChange" in VM "{vm_name}"'
    )
    VM_INSTANCE_1 = 'instance1'
    VM_INSTANCE_2 = 'instance2'

    def setup_class(self):
        super().setup_class()
        self.sdv_device_vm1 = self.get_device('device1').adb()
        self.sdv_device_vm2 = self.get_device('device2').adb()

    def test_orchestration_unregisters_reconnects_custom_modes(self):
        logging.info(
            'Start Orchestration Unregisters Reconnects Custom Modes Test'
        )

        # We want to check that cross-VM communication for custom modes keeps working after one VM has shutdown and starts again.
        # So the first step for the test is rebooting one VM and then checking that custom modes communication works in both directions.
        # We don't check that cross-VM communication is working before the test because that's already covered by SdvSampleOrchestrationCustomModesTwoVmsTest.
        self.sdv_device_vm1.reboot_device()
        self.sdv_device_vm1.wait_for_device_online()

        # Wait until both VMs have initialised correctly by checking custom modes subscription.
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device_vm1,
            grep_text="sdv_orchestration_agent",
            expected_result=self.SUBSCRIBED_CUSTOM_MODE_LOGCAT_TEXT.format(vm_name=self.VM_INSTANCE_2),
            assert_msg="VM1 did not subscribe to custom modes from VM2",
        )
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device_vm2,
            grep_text="sdv_orchestration_agent",
            expected_result=self.UNREGISTERED_CUSTOM_MODE_LOGCAT_TEXT.format(vm_name=self.VM_INSTANCE_1),
            assert_msg="VM2 did not unregister to custom modes from VM1",
        )
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device_vm2,
            grep_text="sdv_orchestration_agent",
            expected_result=self.SUBSCRIBED_CUSTOM_MODE_LOGCAT_TEXT.format(vm_name=self.VM_INSTANCE_1),
            assert_msg="VM2 did not subscribe to custom modes from VM1",
        )

        # Send custom mode in VM1
        self.sdv_device_vm1.execute_shell_command_in_subprocess(
            self.SAMPLE_VM_1_PROCESS_NAME,
            self.START_CUSTOM_MODES_SAMPLE_COMMAND.format(
                mode_name=self.CHARGING_COMMAND, state=self.OFF_COMMAND
            ),
        )
        # Verify that the custom mode is still received by both VMs
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device_vm1,
            grep_text="sdv_orchestration_agent",
            expected_result=self.SUCCESS_CUSTOM_MODE_LOGCAT_GREP_TEXT_TEMPLATE.format(
                mode_name=self.CHARGING_COMMAND, state=self.OFF_COMMAND
            ),
            assert_msg="VM1 did not enforce CHARGING OFF custom mode",
        )
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device_vm2,
            grep_text="sdv_orchestration_agent",
            expected_result=self.SUCCESS_CUSTOM_MODE_LOGCAT_GREP_TEXT_TEMPLATE.format(
                mode_name=self.CHARGING_COMMAND, state=self.OFF_COMMAND
            ),
            assert_msg="VM2 did not enforce CHARGING OFF custom mode",
        )

        # Send custom mode in VM2
        self.sdv_device_vm2.execute_shell_command_in_subprocess(
            self.SAMPLE_VM_2_PROCESS_NAME,
            self.START_CUSTOM_MODES_SAMPLE_COMMAND.format(
                mode_name=self.CHARGING_COMMAND, state=self.ON_COMMAND
            ),
        )
        # Verify that the custom mode is still received by both VMs
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device_vm1,
            grep_text="sdv_orchestration_agent",
            expected_result=self.SUCCESS_CUSTOM_MODE_LOGCAT_GREP_TEXT_TEMPLATE.format(
                mode_name=self.CHARGING_COMMAND, state=self.ON_COMMAND
            ),
            assert_msg="VM1 did not enforce CHARGING ON custom mode",
        )
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device_vm2,
            grep_text="sdv_orchestration_agent",
            expected_result=self.SUCCESS_CUSTOM_MODE_LOGCAT_GREP_TEXT_TEMPLATE.format(
                mode_name=self.CHARGING_COMMAND, state=self.ON_COMMAND
            ),
            assert_msg="VM2 did not enforce CHARGING ON custom mode",
        )

        logging.info(
            'End SDV Orchestration Unregisters Reconnects Custom Modes Test'
        )


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
