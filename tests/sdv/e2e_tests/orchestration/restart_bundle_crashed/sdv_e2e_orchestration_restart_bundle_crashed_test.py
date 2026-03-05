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

"""E2E Orchestrator test for verifying that bundles are restarted after a mode is set if they have crashed

Tests is on one SDV VM
"""
from mobly import asserts
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

class SdvE2EOrchestrationRestartBundleCrashedTest(
    sdv_base_test.SdvBaseTestClass
):

    BUNDLE_STARTED_SUCCESS_LOGCAT_TEXT = r'Request for moving service bundle .*: "com.android.sdv.sample.lifecycle", .*: "LifecycleCppSampleServiceBundle", .*: "crashed-restarted" } to STARTED state was Ok(())'
    BUNDLE_CRASH_NOTIFICATION_LOGCAT_TEXT = r'Service bundle crashed for .*: "com.android.sdv.sample.lifecycle", .*: "LifecycleCppSampleServiceBundle", .*: "crashed-notification" }'
    BUNDLE_NON_RESTARTABLE_LOGCAT_TEXT = r'Bundle with .*: "com.android.sdv.sample.lifecycle", .*: "LifecycleCppSampleServiceBundle", .*: "crashed-restarted" } has crashed, but it is not restartable.'
    FINISHED_STARTING_SERVICE_BUNDLE = r'Request for moving service bundle .*: "com.android.sdv.sample.lifecycle", .*: "LifecycleCppSampleServiceBundle", .*: "crashed-notification" } to STARTED state was Ok(())'

    def kill_bundle(self, instance_name):
        # Process name for service bundle is constructed as: bundle_name:instance_name
        process_id = self.sdv_device.execute_shell_command(f"pgrep -f LifecycleCppSampleServiceBundle:{instance_name}")

        asserts.assert_is_not_none(process_id)

        self.sdv_device.execute_shell_command(
            "kill " + process_id
        )

    def wait_for_logcat(self, expected_result, timestamp=None):
        def grep_with_timestamp(sdv_device, expected_result, timestamp):
            res_timestamp, _ = sdv_device.advance_logcat().find_message_after_timestamp(expected_result, timestamp, regex=True)
            return res_timestamp

        result = polling.wait_and_return_result(grep_with_timestamp, self.sdv_device, expected_result, timestamp)
        asserts.assert_is_not_none(result)
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

    def test_bundle_restarted_if_crashed(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # Trigger initial start of the service bundle
        self.custom_modes_session.send_command('orch_custom_mode_sample E2E-TESTS crashed-restarted')

        # And verify that the bundle was transitioned to the required state.
        transition_completed_timestamp = self.wait_for_logcat(self.BUNDLE_STARTED_SUCCESS_LOGCAT_TEXT)

        # Kill the service bundle
        self.kill_bundle("crashed-restarted")
        # This bundle is not restartable, so we verify that it was not restarted.
        transition_completed_timestamp = self.wait_for_logcat(self.BUNDLE_NON_RESTARTABLE_LOGCAT_TEXT, transition_completed_timestamp)

        # After setting any mode, the internal orchestrator state will be evaluated again and the bundle should be started again.
        # As we are in the same session, we just need to provide the name  and value of the mode, and not the full command.
        self.custom_modes_session.send_command('CHARGING ON')

        # Verify that the bundle is restarted
        self.wait_for_logcat(self.BUNDLE_STARTED_SUCCESS_LOGCAT_TEXT, transition_completed_timestamp)

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_bundle_restarted_after_crash_notification(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # GIVEN
        # Start the bundle that will be killed later in the test
        self.custom_modes_session.send_command('orch_custom_mode_sample E2E-TESTS crash-notify')
        transition_completed_timestamp = self.wait_for_logcat(self.FINISHED_STARTING_SERVICE_BUNDLE)

        # WHEN the bundle is killed
        self.kill_bundle("crashed-notification")

        # THEN verify that the crash notification from LM was received
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text="sdv_orchestration_agent",
            expected_result=self.BUNDLE_CRASH_NOTIFICATION_LOGCAT_TEXT,
            assert_msg="Crash notification was not received",
        )

        # THEN verify that the bundle was restarted
        self.wait_for_logcat(self.FINISHED_STARTING_SERVICE_BUNDLE, transition_completed_timestamp)

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
