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

"""Verify Recovery Strategies for SD/DT/HM

SD/DT/HM are critical services (not device critical), if they crash the system is recoverable only through the VM restart.
OEM controls the VM restart.

The test simulates crashes of critical services, checks that VM health reports are not produced anymore, and simulates recovery through the VM restart.
After the restart, the health report is published again.
"""

from mobly import asserts
from absl.testing import parameterized
import time
import logging

from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling


class SdvE2ERecoveryCriticalServicesTest(sdv_base_test.SdvBaseTestClass, parameterized.TestCase):

    KILL_COMMAND = "pkill -f {agent}"
    PS_COMMAND = "ps -A | grep {agent}"
    KILL_EXPECTED_LOG = "Sending SIGKILL to service '{agent}'"

    ERROR_MESSAGE = "Failed to {command}: {log}"

    VM_HEALTHY_EXPECTED_LOG = "VM is HEALTHY"
    VM_UNHEALTHY_EXPECTED_LOG = "VM is UNHEALTHY"
    HEALTH_EXPECTED_LOG = "VM is"
    LOGCAT_ARGS_FOR_VM_HEALTH_SUB = "-s vm_health_sub"
    DEVICE_NAME = "device1"

    def log_is_empty(self, log):
        return len(self.sdv_device.read_file(log)) == 0

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device(self.DEVICE_NAME).adb()

    def log_enter(self):
        """ Unified logging enter test suit. """
        logging.info(
            f"{self.get_suite_name()} :: Start test {self.current_test_info.name}")

    def log_exit(self):
        """ Unified logging exit test suit. """
        logging.info(
            f"{self.get_suite_name()} :: Finished test {self.current_test_info.name}")

    def verify_kill(self, agent):
        # wait for 1 second to ensure the kill has finished and log has been written.
        time.sleep(1)
        command = self.PS_COMMAND.format(agent=agent)
        log = self.sdv_device.execute_shell_command_in_subprocess_log(command)
        asserts.assert_true(
            self.log_is_empty(log),
            self.ERROR_MESSAGE.format(
                command=command,
                log=self.sdv_device.read_file(log),
            ),
        )

    def test_agent_monitoring_timeout_sys_prop_exists(self):
        asserts.assert_is_not_none(
            self.sdv_device.prop.get(SdvDeviceProperty.HEALTH_MONITOR_AGENT_STARTUP_TIMEOUT_SEC)
        )
        asserts.assert_true(
            self.sdv_device.prop.get(SdvDeviceProperty.HEALTH_MONITOR_AGENT_STARTUP_TIMEOUT_SEC),
            "Health Monitor Agent Startup Timeout Sec is not set"
        )

    @parameterized.named_parameters(
        # autopep8: off
        # go/keep-sorted start
        {"testcase_name": "when_diagnostics_crashes", "agent": "sdv_diagnostics_agent"},
        {"testcase_name": "when_init_open_dice_crashes", "agent": "init_open_dice"},
        {"testcase_name": "when_lm_crashes", "agent": "sdv_lifecycle_agent"},
        {"testcase_name": "when_orchestrator_crashes", "agent": "sdv_orchestration_agent"},
        {"testcase_name": "when_rpc_agent_crashes", "agent": "rpcagent"},
        {"testcase_name": "when_sb_registry_crashes", "agent": "sdv_service_bundles_registry_agent"},
        {"testcase_name": "when_service_discovery_crashes", "agent": "sdv_sd_agent"},
        {"testcase_name": "when_someip_broker_crashes", "agent": "sdv_someip_broker_agent"},
        {"testcase_name": "when_someip_stack_crashes", "agent": "sdv_someip_stack_agent"},
        {"testcase_name": "when_vpm_crashes", "agent": "sdv_vpm_agent"},
        {"testcase_name": "when_vsidl_provider_crashes", "agent": "sdv_vsidl_provider_agent"},
        # go/keep-sorted end
        # autopep8: on
    )
    def test_vm_reported_unhealthy_when_agent_crashes(self, agent):
        self.log_enter()
        health_listener_session = self.sdv_device.interactive_session()
        health_listener_session.send_command("test_vm_health_subscriber")

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.VM_HEALTHY_EXPECTED_LOG,
            logcat_args=self.LOGCAT_ARGS_FOR_VM_HEALTH_SUB,
            assert_msg="VM is not healthy",
        )

        self.sdv_device.execute_shell_command(
            self.KILL_COMMAND.format(agent=agent))
        self.verify_kill(agent)

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.KILL_EXPECTED_LOG.format(agent=agent),
            assert_msg="Can't find the log that agent has been killed",
        )

        self.sdv_device.clear_logcat()
        self.sdv_device.verify_logcat_is_running()

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.VM_UNHEALTHY_EXPECTED_LOG,
            logcat_args=self.LOGCAT_ARGS_FOR_VM_HEALTH_SUB,
            assert_msg="VM is still healthy after killing the agent",
        )

        # Emulate OEM action for restarting a broken VM.
        self.sdv_device.reboot_device_and_verify_logcat()

        # After reboot the VM should be in a healthy state again.
        health_listener_new_session = self.sdv_device.interactive_session()
        health_listener_new_session.send_command("test_vm_health_subscriber")
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.VM_HEALTHY_EXPECTED_LOG,
            logcat_args=self.LOGCAT_ARGS_FOR_VM_HEALTH_SUB,
            assert_msg="VM is not healthy",
        )

        # cleanup
        health_listener_session.close()
        health_listener_new_session.close()
        self.log_exit()

    @parameterized.named_parameters(
        {"testcase_name": "when_data_tunnel_crashes", "agent": "dt_agent"},
        {"testcase_name": "when_hm_crashes", "agent": "sdv_health_monitor"},
    )
    def test_vm_health_report_not_published_when_agent_crashes(self, agent):
        self.log_enter()

        health_listener_session = self.sdv_device.interactive_session()
        health_listener_session.send_command("test_vm_health_subscriber")

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.VM_HEALTHY_EXPECTED_LOG,
            logcat_args=self.LOGCAT_ARGS_FOR_VM_HEALTH_SUB,
            assert_msg="VM is not healthy",
        )

        self.sdv_device.execute_shell_command(
            self.KILL_COMMAND.format(agent=agent))
        self.verify_kill(agent)

        self.sdv_device.clear_logcat()
        self.sdv_device.verify_logcat_is_running()

        # Health reports are generated in the 100 ms range, ensure no reports have appeared in 1s.
        time.sleep(1)
        monitoring_logs = self.sdv_device.grep_from_logcat(
            self.HEALTH_EXPECTED_LOG,
            logcat_args=self.LOGCAT_ARGS_FOR_VM_HEALTH_SUB,
        )
        asserts.assert_false(
            monitoring_logs, f"Health reports were published {monitoring_logs}")

        # Emulate OEM action for restarting a broken VM.
        self.sdv_device.reboot_device_and_verify_logcat()
        self.sdv_device.clear_logcat()

        # After reboot the VM should be in a healthy state again.
        health_listener_new_session = self.sdv_device.interactive_session()
        health_listener_new_session.send_command("test_vm_health_subscriber")

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.VM_HEALTHY_EXPECTED_LOG,
            logcat_args=self.LOGCAT_ARGS_FOR_VM_HEALTH_SUB,
            assert_msg="VM is not healthy",
        )

        # cleanup
        health_listener_session.close()
        health_listener_new_session.close()
        self.log_exit()


if __name__ == "__main__":
    sdv_test_runner.run()
