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

"""
SDV HM Ram Suspend Resume test: tests that VM is not transiently reported as unhealthy,
post resume from ram
"""

from mobly import asserts
import logging
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvHmRamSuspendResumeTest(sdv_base_test.SdvBaseTestClass):
    def setup_class(self):
        super().setup_class()
        self.sdv_device_adb = self.get_device('device1').adb()
        self.sdv_device_adb.execute_shell_command(
            'setprop persist.sdv.orchestrator_config_path /product/etc/orch/hm_suspend_to_ram_test_global_orch_config.textproto')
        self.sdv_device_adb.reboot_device_and_verify_logcat()

    def suspend_resume_device(self):
        session = self.sdv_device_adb.interactive_session()
        session.send_command_and_wait_for_outputs(
            "vepsm power-state power-on",
            ["Received power-state-report from VPM ON , reason HOST_REQUESTED",]
        )
        session.send_command_and_wait_for_outputs(
            "vepsm power-state prepare ram",
            ["Received power-state-report from VPM WAIT_FOR_FINISH",],
        )
        self.sdv_device_adb.execute_shell_command("rtcwake -m no -s 5")
        session.send_command_and_wait_for_outputs(
            "vepsm power-state finish ram",
            ["Received power-state-report from VPM SUSPEND_TO_RAM_EXIT , reason HOST_REQUESTED",],
        )
        session.close()

    def test_no_transient_unhealthy_vm_report_across_suspend_resume_ram_cycles(self):
        self.sdv_device_adb.execute_shell_command_in_subprocess(
            "vm_health_report_publication_consumer_process",
            "test_vm_health_subscriber"
        )

        NR_CYCLES = 10
        grep_re = "Received VM health report of vm.*report =.*"
        unhealthy_log = "all_monitored_service_bundles_healthy: false"
        healthy_log = "all_monitored_service_bundles_healthy: true"
        for i in range(NR_CYCLES):
            logging.info(f"Starting cycle {i}/{NR_CYCLES}")
            self.sdv_device_adb.clear_logcat()
            self.suspend_resume_device()

            WaitingMethods.wait_and_verify_expected_logs(
                self.sdv_device_adb,
                grep_re,
                expected_result=healthy_log,
                grep_args="-E",
                assert_msg="Message not found in logcat within timeout",
            )
            logs = self.sdv_device_adb.grep_from_logcat(
                grep_re, grep_args="-E")
            asserts.assert_false(
                unhealthy_log in logs, msg=f"Found vm unhealthy! logs: {logs}")

        # TODO b/399673375: replace reboot with kill_subprocesses once it works
        self.sdv_device_adb.reboot_device()


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
