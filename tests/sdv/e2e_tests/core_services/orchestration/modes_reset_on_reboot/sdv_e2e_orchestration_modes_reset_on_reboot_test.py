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

"""E2E Orchestrator test for verifying that after a VM is rebooted, the orchestrator
does not persist any previously stored mode.

Tests is on one SDV VM
"""
from mobly import asserts
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

class SdvE2EOrchestrationModesResetOnRebootTest(
    sdv_base_test.SdvBaseTestClass
):
    FINISHED_PROCESSING_VEHICLE_PARK = 'Finished enforcing mode \'Vehicle\' with state \'"PARK"\'. successfully.'
    FINISHED_PROCESSING_POWER_ON = 'Finished enforcing mode \'Power\' with state \'"ON"\'. successfully'
    FINISHED_PROCESSING_CUSTOM_MODE = 'Finished enforcing mode \'Custom\\("E2E-TESTS"\\)\' with state \'"recover-custom-mode"\'. successfully'
    FINISHED_PROCESSING_DEFAULT = 'Finished enforcing mode \'Default\' with state \'"Requesting state for bundles with no conditions"\'. successfully.'
    FINISHED_STARTING_RECOVER_CUSTOM_MODE_SERVICE = r'Request for moving service bundle .*: "com.sdv.google.sample.lifecycle.apex", .*: "LifecycleCppSampleServiceBundle", .*: "recover-custom-mode" } to STARTED state was Ok(())'
    FINISHED_STARTING_RECOVER_POWER_MODE_SERVICE = r'Request for moving service bundle .*: "com.sdv.google.sample.lifecycle.apex", .*: "LifecycleCppSampleServiceBundle", .*: "recover-power-mode" } to STARTED state was Ok(())'
    FINISHED_STARTING_RECOVER_VEHICLE_MODE_SERVICE = r'Request for moving service bundle .*: "com.sdv.google.sample.lifecycle.apex", .*: "LifecycleCppSampleServiceBundle", .*: "recover-vehicle-mode" } to STARTED state was Ok(())'

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()

    def setup_test(self):
        super().setup_test()
        self.vpm_session = self.sdv_device.interactive_session()
        self.custom_mode_session = self.sdv_device.interactive_session()

    def teardown_test(self):
        self.vpm_session.close()
        self.custom_mode_session.close()
        super().teardown_test()

    def not_in_logcat(self, not_expected_result):
        grep_text = "sdv_orchestration_agent:"

        logcat_result = self.sdv_device.grep_from_logcat(grep_text)
        asserts.assert_not_in(
            not_expected_result,
            logcat_result,
            f"Not expected logcat result found: {not_expected_result}",
        )

    def test_modes_reset_on_reboot(
        self
    ):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # We set power, vehicle and custom modes to verify afterwards that they were not persisted.
        self.vpm_session.send_command('vepsm power-state power-on')
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text="sdv_orchestration_agent",
            expected_result=self.FINISHED_PROCESSING_POWER_ON,
            assert_msg="Power ON mode was never enforced",
        )
        self.vpm_session.send_command('vepsm vehicle-state park')
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text="sdv_orchestration_agent",
            expected_result=self.FINISHED_PROCESSING_VEHICLE_PARK,
            assert_msg="Vehicle PARK mode was never enforced",
        )
        self.custom_mode_session.send_command('orch_custom_mode_sample E2E-TESTS recover-custom-mode')
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text="sdv_orchestration_agent",
            expected_result=self.FINISHED_PROCESSING_CUSTOM_MODE,
            assert_msg="Custom mode was never enforced",
        )

        # Reboot the VM
        self.sdv_device.reboot_device()

        # Wait until we enforced the default mode to verify that none of the modes was enforced (meaning set on subscription)
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text="sdv_orchestration_agent",
            expected_result=self.FINISHED_PROCESSING_DEFAULT,
            assert_msg="Default mode was never enforced",
        )
        self.not_in_logcat(self.FINISHED_PROCESSING_POWER_ON)
        self.not_in_logcat(self.FINISHED_PROCESSING_VEHICLE_PARK)
        self.not_in_logcat(self.FINISHED_PROCESSING_CUSTOM_MODE)
        # Verify that the bundles were not started (as they could have been started through default mode)
        self.not_in_logcat(self.FINISHED_STARTING_RECOVER_CUSTOM_MODE_SERVICE)
        self.not_in_logcat(self.FINISHED_STARTING_RECOVER_POWER_MODE_SERVICE)
        self.not_in_logcat(self.FINISHED_STARTING_RECOVER_VEHICLE_MODE_SERVICE)

        # Verify through dumpysys that none of the previously set commands was persisted after a reboot
        not_expected_dump = [
                r'Vehicle\s+PARK\s+-',
                r'Power\s+ON\s+-',
                r'Custom\("E2E-TESTS"\)\s+recover-custom-mode\s+\d+ \(scs\) \d+ \(ns\)',
                r'Started\s+com.sdv.google.sample.lifecycle.apex/LifecycleCppSampleServiceBundle/recover-custom-mode',
                r'Started\s+com.sdv.google.sample.lifecycle.apex/LifecycleCppSampleServiceBundle/recover-vehicle-mode',
                r'Started\s+com.sdv.google.sample.lifecycle.apex/LifecycleCppSampleServiceBundle/recover-power-mode',
        ]
        dump_report = self.sdv_device.execute_shell_command(
                "dumpsys com.google.sdv.ISdvAgent/orch"
            )
        # Verify that modes were not persisted in dumpsys
        for dump_line in not_expected_dump:
            asserts.assert_not_regex(dump_report, dump_line, f"Not expected dump line found: '{dump_line}' in dump report: {dump_report}")

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
