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

"""E2E Orchestrator test for verifying that bundles are restarted to requested state after LM crashed and restarted

Tests is on one SDV VM
"""
from mobly import asserts
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling


class SdvE2EOrchestrationBundlesRestartedAfterLMCrashTest(
    sdv_base_test.SdvBaseTestClass
):

    RESTARTED_BUNDLE_AFTER_CRASH_SUCCESS_LOGCAT_TEXT = (
        r'Request for moving service bundle .*: "com.sdv.google.sample.lifecycle.apex", .*: "LifecycleCppSampleServiceBundle", .*: "crashed-restarted" } to STARTED state was Ok(())'
    )
    AFTER_CRASH_STARTED_BUNDLE_SUCCESS_LOGCAT_TEXT = (
        r'Request for moving service bundle .*: "com.sdv.google.sample.lifecycle.apex", .*: "LifecycleCppSampleServiceBundle", .*: "recover-custom-mode" } to STARTED state was Ok(())'
    )

    def restart_lm_agent(self):
        process_id = self.sdv_device.execute_shell_command(
            "pgrep -f sdv_lifecycle_agent")
        asserts.assert_is_not_none(process_id)
        self.sdv_device.execute_shell_command(
            "kill " + process_id
        )
        self.sdv_device.execute_shell_command_in_subprocess(
            "Restarted LM agent, will run until instance reboot, in background",
            "/system_ext/bin/sdv_lifecycle_agent"
        )

    def wait_for_logcat(self, expected_result, timestamp=None):
        def grep_with_timestamp(sdv_device, expected_result, timestamp):
            res_timestamp, _ = sdv_device.advance_logcat(
            ).find_message_after_timestamp(expected_result, timestamp, regex=True)
            return res_timestamp

        result = polling.wait_and_return_result(
            grep_with_timestamp, self.sdv_device, expected_result, timestamp)
        asserts.assert_is_not_none(result, f"Logcat result not found within timeout: {expected_result}")
        return result

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()

    def setup_test(self):
        super().setup_test()
        self.custom_modes_session = self.sdv_device.interactive_session()

    def teardown_test(self):
        self.custom_modes_session.close()
        super().teardown_test()

    def test_bundle_restarted_after_lm_crashed(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # Trigger initial start of the service bundle
        self.custom_modes_session.send_command(
            'orch_custom_mode_sample E2E-TESTS crashed-restarted')
        # And verify that the bundle was transitioned to the required state.
        transition_completed_timestamp = self.wait_for_logcat(
            self.RESTARTED_BUNDLE_AFTER_CRASH_SUCCESS_LOGCAT_TEXT)

        # restart LM agent. Service bundle crashes when LM is killed
        self.restart_lm_agent()

        # Verify that the bundle was started again
        transition_completed_timestamp = self.wait_for_logcat(
            self.RESTARTED_BUNDLE_AFTER_CRASH_SUCCESS_LOGCAT_TEXT, transition_completed_timestamp)

        # Verify that we can execute a mode transition after the recovery
        self.custom_modes_session.send_command('E2E-TESTS recover-custom-mode')
        self.wait_for_logcat(
            self.AFTER_CRASH_STARTED_BUNDLE_SUCCESS_LOGCAT_TEXT)

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
