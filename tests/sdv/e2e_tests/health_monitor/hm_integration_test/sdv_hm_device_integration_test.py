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

"""SDV Hm Device Integration Test"""

from mobly import asserts
import logging
import time

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling
from sdv_test_fw.device.sdv_property import SdvDeviceProperty


class SdvHmDeviceIntegrationTest(sdv_base_test.SdvBaseTestClass):

    HEALTH_COMMAND = (
        "sdv_service_bundle {action}"
        " instance1:com.android.sdv.sample.oem.health.{service}/first-instance"
    )
    KILL_COMMAND = "pkill -f SampleHMBundle:first-instance"
    PS_COMMAND = "ps -A | grep SampleHMBundle:first-instance"

    ERROR_MESSAGE = "Failed to {command}: {log}"

    START = "start"
    STOP = "create"
    MONITORED = "monitored.SampleHMBundle"
    MONITORING = "monitoring.VmHMBundle"

    MONITORED_EXPECTED_LOG = (
        "Registered health configuration: HealthConfiguration"
    )
    HEARTBEAT_EXPECTED_LOG = "Published a heartbeat ServiceHeartbeat"
    HEALTHY_EXPECTED_LOG = "VM is HEALTHY"
    UNHEALTHY_EXPECTED_LOG = "VM is UNHEALTHY"
    DEVICE_NAME = "device1"

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device(self.DEVICE_NAME).adb()

        self.sdv_authz_enable_value = self.sdv_device.prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        self.sdv_device.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, "permissions_only")

    def teardown_class(self):
        self.sdv_device.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.sdv_authz_enable_value)
        super().teardown_class()

    def log_enter(self):
        """ Unified logging enter test suit. """
        logging.info(
            f"{self.get_suite_name()} :: Start test {self.current_test_info.name}")

    def log_exit(self):
        """ Unified logging exit test suit. """
        logging.info(
            f"{self.get_suite_name()} :: Finished test {self.current_test_info.name}")

    def log_is_empty(self, log):
        return len(self.sdv_device.read_file(log)) == 0

    def assert_successful_execution(self, command):

        log = self.sdv_device.execute_shell_command_in_subprocess_log(command)

        # wait for 1 second to ensure the command was executed and log was written.
        time.sleep(1)
        asserts.assert_true(
            self.log_is_empty(log),
            self.ERROR_MESSAGE.format(
                command=command,
                log=self.sdv_device.read_file(log),
            ),
        )

    def test_hm_vm_healthy_when_single_monitored_and_single_monitoring(self):
        self.log_enter()
        self.sdv_device.clear_logcat()

        self.assert_successful_execution(self.HEALTH_COMMAND.format(
            action=self.START, service=self.MONITORED))

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.MONITORED_EXPECTED_LOG,
            assert_msg="Monitored service didn't start",
        )

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEARTBEAT_EXPECTED_LOG,
            assert_msg="Heartbeat was not published",
        )
        self.sdv_device.clear_logcat()

        self.assert_successful_execution(self.HEALTH_COMMAND.format(
            action=self.START, service=self.MONITORING))

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTHY_EXPECTED_LOG,
            assert_msg="VM is not healthy",
        )
        self.sdv_device.clear_logcat()

        self.assert_successful_execution(self.HEALTH_COMMAND.format(
            action=self.STOP, service=self.MONITORED))
        self.assert_successful_execution(self.HEALTH_COMMAND.format(
            action=self.STOP, service=self.MONITORING))
        self.log_exit()

    def test_hm_reregister_fo_monitoring_after_crash_succeeds(self):
        self.log_enter()
        self.sdv_device.clear_logcat()

        self.assert_successful_execution(self.HEALTH_COMMAND.format(
            action=self.START, service=self.MONITORED))

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.MONITORED_EXPECTED_LOG,
            assert_msg="Monitored service didn't start",
        )

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEARTBEAT_EXPECTED_LOG,
            assert_msg="Heartbeat was not published",
        )
        self.sdv_device.clear_logcat()

        self.assert_successful_execution(self.HEALTH_COMMAND.format(
            action=self.START, service=self.MONITORING))

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTHY_EXPECTED_LOG,
            assert_msg="VM is not healthy",
        )
        self.sdv_device.clear_logcat()

        self.assert_successful_execution(self.KILL_COMMAND)
        self.assert_successful_execution(self.PS_COMMAND)
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.UNHEALTHY_EXPECTED_LOG,
            assert_msg="VM is unexpectedly healthy",
        )
        self.sdv_device.clear_logcat()

        self.assert_successful_execution(self.HEALTH_COMMAND.format(
            action=self.START, service=self.MONITORED))

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.MONITORED_EXPECTED_LOG,
            assert_msg="Monitored service didn't start",
        )

        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTHY_EXPECTED_LOG,
            assert_msg="VM is not healthy",
        )

        self.assert_successful_execution(self.HEALTH_COMMAND.format(
            action=self.STOP, service=self.MONITORED))
        self.assert_successful_execution(self.HEALTH_COMMAND.format(
            action=self.STOP, service=self.MONITORING))
        self.log_exit()

    def _dump_report_contains(self, chunk: str) -> bool:
        return chunk in self.sdv_device.dumpsys(
            "com.google.sdv.ISdvAgent/hm"
        )

    def test_instances_of_same_bundle_are_monitored_separately(self):
        monitored_fqin1 = "instance1:com.android.sdv.sample.oem.health.monitored.SampleHMBundle/instance1"
        monitored_fqin2 = "instance1:com.android.sdv.sample.oem.health.monitored.SampleHMBundle/instance2"

        self.sdv_device.execute_shell_command(
            f"sdv_service_bundle start {monitored_fqin1}"
        )
        self.sdv_device.execute_shell_command(
            f"sdv_service_bundle start {monitored_fqin2}"
        )

        polling.wait_for_true(
            lambda: self._dump_report_contains(
                f"{monitored_fqin1}\nis_healthy: Healthy"),
            timeout=5,
            assert_msg="HM dump report does not report a healthy instance1"
        )
        polling.wait_for_true(
            lambda: self._dump_report_contains(
                f"{monitored_fqin2}\nis_healthy: Healthy"),
            timeout=5,
            assert_msg="HM dump report does not report a healthy instance2"
        )

        self.sdv_device.execute_shell_command(
            f"pkill -f {monitored_fqin2}")

        polling.wait_for_true(
            lambda: self._dump_report_contains(
                f"{monitored_fqin1}\nis_healthy: Healthy"),
            timeout=5,
            assert_msg="HM dump report does not report a healthy instance1,"
            " although was healthy before"
        )
        polling.wait_for_true(
            lambda: self._dump_report_contains(
                f"{monitored_fqin2}\nis_healthy: Unhealthy"),
            timeout=5,
            assert_msg="HM dump report does not report an unhealthy instance2"
            " although we have just killed its process"
        )

        # vm unhealthy, needs reboot
        self.sdv_device.reboot_device()


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
