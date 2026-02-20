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

"""E2E Orchestrator test for verifying the dumpsys functionality

Test is on one SDV VM
"""
from mobly import asserts
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

class SdvE2EOrchestrationDumpsysTest(
    sdv_base_test.SdvBaseTestClass
):
    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()

    def setup_test(self):
        super().setup_test()
        self.vpm_session = self.sdv_device.interactive_session()

    def teardown_test(self):
        self.vpm_session.close()
        super().teardown_test()

    def test_orchestration_dumpsys(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        expected_dump = [
                r'AGENT NAME: SDV Agent dump - Orchestrator',
                r'AGENT FQIN: .+:com.android.sdv.orchestrator.OrchestratorServiceBundle/default',
                r'AGENT STATE: See orchestrator state below.',
                r'MODE\s+VALUE\s+TIMESTAMP \(scs, ns\)',
                r'Vehicle\s+PARK\s+-',
                r'Power\s+POWER_OFF_EXIT\s+-',
                r'Custom\("TIRE_PRESSURE"\)\s+front-left\s+\d+ \(scs\) \d+ \(ns\)',
                r'Modes allowed to publish by bundle \(FQIN: modes\):',
                r'com.android.sdv.sample.orchestrator\/CustomModeControlBundle: TIRE_PRESSURE',
                r'Requested state for instances:\nSTATE\s+FQIN',
                r'Started\s+com.android.sdv.sample.orchestrator\/CustomModeControlBundle\/always-started-instance',
                r'Started\s+com.android.sdv.sample.orchestrator\/OrchestratedServiceBundle\/my-instance',
                r'Last mode enforced was Vehicle with value "PARK"',
                r'Next modes to process: \[\]'
        ]

        # We set a vehicle mode for the following reasons:
        # 1. To verify its value is dumped correctly.
        # 2. Make the test more deterministic by having a final value set to verify in dump report.
        # 3. To have a way to wait until orch finished initialising and set the park value. This is needed because logcat is cleared by default and we cannot verify initial setup without rebooting.
        self.vpm_session.send_command('vepsm vehicle-state park')

        # Wait until orch finished transition the park (this means that the initial power and custom more were set)
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text="sdv_orchestration_agent",
            expected_result='Finished processing mode update Vehicle: "PARK"',
            assert_msg="PARK mode was never set",
        )

        # Trigger the dumpsys
        dump_report = self.sdv_device.execute_shell_command(
                "dumpsys com.google.sdv.ISdvAgent/orch"
            )

        # Verify dumpsys output
        for dump_line in expected_dump:
            asserts.assert_regex(dump_report, dump_line, f"Did not find: '{dump_line}' in dump report: {dump_report}")

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
