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

"""E2E Orchestrator test for verifying binder reconnect mechanism to VPM and LM agents

Tests is on one SDV VM
"""
from mobly import asserts
import logging
from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods


class SdvE2EOrchestrationBinderReconnectTest(
    sdv_base_test.SdvBaseTestClass, parameterized.TestCase
):
    def kill_agent(self, name):
        process_id = self.sdv_device.execute_shell_command("pgrep -f " + name)

        asserts.assert_is_not_none(process_id)

        kill_result = self.sdv_device.execute_shell_command(
            "kill " + process_id
        )

    def verify_agent_reconnected(self, name):
        expected_result = "Successfully fetched " + name + " binder"
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text="sdv_orchestration_agent",
            expected_result=expected_result,
            assert_msg="Orchestrator failed to reconnect to " + name + " agent",
        )

    # Verify that after reconnecting to VPM, orch sets the current power state
    def verify_power_state_updated(self):
        expected_result = 'Setting current power state "POWER_OFF_EXIT".'
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text="sdv_orchestration_agent",
            expected_result=expected_result,
            assert_msg="POWER_OFF_EXIT mode was not set by orchestrator on VPM restart",
        )

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()

    def setup_test(self):
        super().setup_test()
        self.agent_session = self.sdv_device.interactive_session()

    def teardown_test(self):
        self.agent_session.close()
        super().teardown_test()

    @parameterized.named_parameters(
        ("vpm", "sdv_vpm_agent", "VPM"),
        ("lm", "sdv_lifecycle_agent", "LM lifecycle manager"),
    )
    def test_reconnects_after_connection_has_died(
        self, agent_process_name, agent_name_in_logs
    ):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        self.sdv_device.clear_logcat()

        # Kill agent that owns the connection.
        self.kill_agent(agent_process_name)

        # Killed agent NOT restarted by init.rc. Restart it manually:
        self.agent_session.send_command(
            f"/system_ext/bin/{agent_process_name}")

        # Verify that Orchestrator reconnect to it.
        self.verify_agent_reconnected(agent_name_in_logs)

        if agent_name_in_logs == "VPM":
            self.verify_power_state_updated()

        # as a oneshot service was killed, vm needs rebooting:
        self.sdv_device.reboot_device_and_verify_logcat()

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
