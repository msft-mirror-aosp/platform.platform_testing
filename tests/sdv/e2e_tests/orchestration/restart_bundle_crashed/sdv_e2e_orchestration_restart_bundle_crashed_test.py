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

    SEND_CUSTOM_MODE_COMMAND = 'orch_custom_mode_sample E2E-TESTS {state}'

    CRASHED_RESTARTED_BUNDLE_STARTED_SUCCESS_LOG = r'Request for moving service bundle .*: "com.android.sdv.sample.lifecycle", .*: "LifecycleCppSampleServiceBundle", .*: "crashed-restarted" } to STARTED state was Ok(())'
    CRAHSED_RESTARTED_BUNDLE_NON_RESTARTABLE_LOG = r'Bundle with .*: "com.android.sdv.sample.lifecycle", .*: "LifecycleCppSampleServiceBundle", .*: "crashed-restarted" } has crashed, but it is not restartable.'

    CRASHED_NOTIFICATION_BUNDLE_CRASH_NOTIFICATION_LOG = r'Service bundle crashed for .*: "com.android.sdv.sample.lifecycle", .*: "LifecycleCppSampleServiceBundle", .*: "crashed-notification" }'
    CRASHED_NOTIFICATION_BUNDLE_STARTED_SUCCESS_LOG = r'Request for moving service bundle .*: "com.android.sdv.sample.lifecycle", .*: "LifecycleCppSampleServiceBundle", .*: "crashed-notification" } to STARTED state was Ok(())'

    STARTED_ENFORCING_CUSTOM_MODE_LOG = r'VPM callback received vpm update.*E2E-TESTS.*{state}.*'
    FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_LOG = r'Finished enforcing mode.*E2E-TESTS.*{state}.*successfully.'

    # The same string is used for the custom mode and for the instance name of the bundles.
    # When creating/starting the OrchestratorSampleRustServiceBundle with these instances names,
    # they will crash based on different lifecycles states (created or started) defined in their name.
    MODE_CRASH_ON_START_RESTARTS = "crash-on-start-restarts"
    MODE_CRASH_ON_START_NO_RESTARTS = "crash-on-start-no-restarts"
    MODE_CRASH_ON_CREATE_RESTARTS = "crash-on-create-restarts"
    MODE_CRASH_ON_CREATE_NO_RESTARTS = "crash-on-create-no-restarts"
    MODE_CRASH_ON_START_DEFAULT_RETRIES = "crash-on-start-default-retries"

    LM_TRANSITION_REQUEST_LOG = r'Request for moving service bundle .* "com.android.sdv.test.orchestrator", .*: "OrchestratorSampleRustServiceBundle", .*: "{instance}" }} to {lifecycle} state was .*'
    NOTIFYING_SUBSCRIBERS_INSTANCE_STATE_LOG = r'Notifying subscribers .* "com.android.sdv.test.orchestrator", .*: "OrchestratorSampleRustServiceBundle", .*: "{instance}" .* r#?{recovery} .* r#?{lifecycle}'

    RECOVERY_STATE_RETRYING = "RETRYING"
    RECOVERY_STATE_RETRY_FAILED = "RETRY_FAILED"
    LIFECYCLE_STATE_CREATED = "CREATED"
    LIFECYCLE_STATE_STARTED = "STARTED"

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()

    def setup_test(self):
        super().setup_test()
        self.sdv_device.execute_shell_command_in_subprocess("setup_process", self.SEND_CUSTOM_MODE_COMMAND.format(state="reset"))
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text="sdv_orchestration_agent",
            expected_result=self.FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_LOG.format(state="reset"),
            assert_msg="Custom mode did not reset",
        )
        # Make sure we start the test with clean logcat
        self.sdv_device.clear_logcat()

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
        asserts.assert_is_not_none(result, f'Message "{expected_result}" not found in logcat.')
        return result

    def verify_message_found(self, message):
        """
        Verifies that a log message appears at least once.
        """
        logcat_result = self.sdv_device.grep_from_logcat("sdv_orchestration_agent")
        asserts.assert_regex(
            logcat_result,
            message,
            f"Expected logcat not found: {message}",
        )

    def verify_message_not_found(self, message):
        """
        Verifies that a log message does not appear in the logcat.
        """
        logcat_result = self.sdv_device.grep_from_logcat("sdv_orchestration_agent")
        asserts.assert_not_regex(
            logcat_result,
            message,
            f"Not expected logcat result found: {message}",
        )

    def verify_message_exactly_n_times(self, log_processor, message, n):
        """
        Verifies that a log message appears exactly n times.
        """
        # Find the nth occurrence of the message.
        log = log_processor.nth_message(n, message, regex=True)
        asserts.assert_is_not_none(
            log,
            f"Message '{message}' was not found {n} times."
        )

        # Verify that the (n+1)th occurrence does not exist.
        log = log_processor.nth_message(n+1, message, regex=True)
        asserts.assert_is_none(
            log,
            f"Message '{message}' was unexpectedly found more than {n} times."
        )

    def test_bundle_restarted_if_crashed(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # Trigger initial start of the service bundle
        self.sdv_device.execute_shell_command_in_subprocess("crashed_restarted_process", self.SEND_CUSTOM_MODE_COMMAND.format(state="crashed-restarted"))

        # And verify that the bundle was transitioned to the required state.
        transition_completed_timestamp = self.wait_for_logcat(self.CRASHED_RESTARTED_BUNDLE_STARTED_SUCCESS_LOG)

        # Kill the service bundle
        self.kill_bundle("crashed-restarted")
        # This bundle is not restartable, so we verify that it was not restarted.
        transition_completed_timestamp = self.wait_for_logcat(self.CRAHSED_RESTARTED_BUNDLE_NON_RESTARTABLE_LOG, transition_completed_timestamp)

        # After setting any mode, the internal orchestrator state will be evaluated again and the bundle should be started again.
        # As we are in the same session, we just need to provide the name  and value of the mode, and not the full command.
        self.sdv_device.execute_shell_command_in_subprocess("charging_on_process", "orch_custom_mode_sample CHARGING ON")


        # Verify that the bundle is restarted
        self.wait_for_logcat(self.CRASHED_RESTARTED_BUNDLE_STARTED_SUCCESS_LOG, transition_completed_timestamp)

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_bundle_restarted_with_default_retries_if_crashed_on_start(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # Must be the same value used in boot params for the `ro.boot.sdv.orchestrator.recovery.max_retries`
        retries = 2

        # Make sure we start the test with clean logcat
        self.sdv_device.clear_logcat()

        # Trigger initial start of the service bundle
        self.sdv_device.execute_shell_command_in_subprocess(self.MODE_CRASH_ON_START_DEFAULT_RETRIES, self.SEND_CUSTOM_MODE_COMMAND.format(state=self.MODE_CRASH_ON_START_DEFAULT_RETRIES))
        # Wait until the mode has finished enforcing to make sure all expected logs are
        # already there.
        self.wait_for_logcat(self.FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_LOG.format(state=self.MODE_CRASH_ON_START_DEFAULT_RETRIES))

        lm_request_message = self.LM_TRANSITION_REQUEST_LOG.format(
            instance=self.MODE_CRASH_ON_START_DEFAULT_RETRIES, lifecycle=self.LIFECYCLE_STATE_STARTED
        )
        # Verify that the start request was attempted exactly 3 times: 2 from restart and 1 from
        # initial trigger.
        self.verify_message_exactly_n_times(self.sdv_device.advance_logcat(), lm_request_message, retries + 1)

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_bundle_restarted_after_crash_notification(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # GIVEN
        # Start the bundle that will be killed later in the test
        self.sdv_device.execute_shell_command_in_subprocess("crash_notify_process", self.SEND_CUSTOM_MODE_COMMAND.format(state="crash-notify"))
        transition_completed_timestamp = self.wait_for_logcat(self.CRASHED_NOTIFICATION_BUNDLE_STARTED_SUCCESS_LOG)

        # WHEN the bundle is killed
        self.kill_bundle("crashed-notification")

        # THEN verify that the crash notification from LM was received
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text="sdv_orchestration_agent",
            expected_result=self.CRASHED_NOTIFICATION_BUNDLE_CRASH_NOTIFICATION_LOG,
            assert_msg="Crash notification was not received",
        )

        # THEN verify that the bundle was restarted
        self.wait_for_logcat(self.CRASHED_NOTIFICATION_BUNDLE_STARTED_SUCCESS_LOG, transition_completed_timestamp)

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_bundle_restartable_crashed_on_start_should_be_notified_and_restarted_once(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        self.sdv_device.execute_shell_command_in_subprocess(self.MODE_CRASH_ON_START_RESTARTS, self.SEND_CUSTOM_MODE_COMMAND.format(state=self.MODE_CRASH_ON_START_RESTARTS))
        # Wait until the mode has finished enforcing to make sure all expected logs are already there.
        self.wait_for_logcat(self.FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_LOG.format(state=self.MODE_CRASH_ON_START_RESTARTS))

        log_processor = self.sdv_device.advance_logcat()

        # Verify RETRYING state is notified only once during the transition
        retrying_message = self.NOTIFYING_SUBSCRIBERS_INSTANCE_STATE_LOG.format(
            instance=self.MODE_CRASH_ON_START_RESTARTS, recovery=self.RECOVERY_STATE_RETRYING, lifecycle=self.LIFECYCLE_STATE_STARTED
        )
        self.verify_message_exactly_n_times(log_processor, retrying_message, 1)

        # Verify RETRY_FAILED state is notified during the transition. This can happen multiple times,
        # as we don't handle repeated notifications. But then we check that the restart was applied
        # only once.
        retry_failed_message = self.NOTIFYING_SUBSCRIBERS_INSTANCE_STATE_LOG.format(
            instance=self.MODE_CRASH_ON_START_RESTARTS, recovery=self.RECOVERY_STATE_RETRY_FAILED, lifecycle=self.LIFECYCLE_STATE_STARTED
        )
        self.verify_message_found(retry_failed_message)

        # Verify that the start request was attempted exactly twice (initial + 1 retry)
        lm_request_message = self.LM_TRANSITION_REQUEST_LOG.format(
            instance=self.MODE_CRASH_ON_START_RESTARTS, lifecycle=self.LIFECYCLE_STATE_STARTED
        )
        self.verify_message_exactly_n_times(log_processor, lm_request_message, 2)

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_bundle_non_restartable_crashed_on_start_should_be_notified_and_not_restarted(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        self.sdv_device.execute_shell_command_in_subprocess(self.MODE_CRASH_ON_START_NO_RESTARTS, self.SEND_CUSTOM_MODE_COMMAND.format(state=self.MODE_CRASH_ON_START_NO_RESTARTS))
        # Wait until the mode has finished enforcing to make sure all expected logs are already there.
        self.wait_for_logcat(self.FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_LOG.format(state=self.MODE_CRASH_ON_START_NO_RESTARTS))

        log_processor = self.sdv_device.advance_logcat()

        # Verify RETRYING state is not notified during the transition for a non-restartable bundle
        retrying_message = self.NOTIFYING_SUBSCRIBERS_INSTANCE_STATE_LOG.format(
            instance=self.MODE_CRASH_ON_START_NO_RESTARTS, recovery=self.RECOVERY_STATE_RETRYING, lifecycle=self.LIFECYCLE_STATE_STARTED
        )
        self.verify_message_not_found(retrying_message)

        # Verify RETRY_FAILED state is notified during the transition. This can happen multiple times,
        # as we don't handle repeated notifications. But then we check that the restart was never applied.
        retry_failed_message = self.NOTIFYING_SUBSCRIBERS_INSTANCE_STATE_LOG.format(
            instance=self.MODE_CRASH_ON_START_NO_RESTARTS, recovery=self.RECOVERY_STATE_RETRY_FAILED, lifecycle=self.LIFECYCLE_STATE_STARTED
        )
        self.verify_message_found(retry_failed_message)

        # Verify that the start request was attempted exactly once (initial attempt, no retries)
        lm_request_message = self.LM_TRANSITION_REQUEST_LOG.format(
            instance=self.MODE_CRASH_ON_START_NO_RESTARTS, lifecycle=self.LIFECYCLE_STATE_STARTED
        )
        self.verify_message_exactly_n_times(log_processor, lm_request_message, 1)

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_bundle_restartable_crashed_on_create_should_be_notified_and_restarted_once(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        self.sdv_device.execute_shell_command_in_subprocess(self.MODE_CRASH_ON_CREATE_RESTARTS, self.SEND_CUSTOM_MODE_COMMAND.format(state=self.MODE_CRASH_ON_CREATE_RESTARTS))
        # Wait until the mode has finished enforcing to make sure all expected logs are already there.
        self.wait_for_logcat(self.FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_LOG.format(state=self.MODE_CRASH_ON_CREATE_RESTARTS))

        log_processor = self.sdv_device.advance_logcat()

        # Verify RETRYING state is notified only once during the transition
        retrying_message = self.NOTIFYING_SUBSCRIBERS_INSTANCE_STATE_LOG.format(
            instance=self.MODE_CRASH_ON_CREATE_RESTARTS, recovery=self.RECOVERY_STATE_RETRYING, lifecycle=self.LIFECYCLE_STATE_CREATED
        )
        self.verify_message_exactly_n_times(log_processor, retrying_message, 1)

        # Verify RETRY_FAILED state is notified during the transition. This can happen multiple times,
        # as we don't handle repeated notifications. But then we check that the restart was applied
        # only once.
        retry_failed_message = self.NOTIFYING_SUBSCRIBERS_INSTANCE_STATE_LOG.format(
            instance=self.MODE_CRASH_ON_CREATE_RESTARTS, recovery=self.RECOVERY_STATE_RETRY_FAILED, lifecycle=self.LIFECYCLE_STATE_CREATED
        )
        self.verify_message_found(retry_failed_message)

        # Verify that the create request was attempted exactly twice (initial + 1 retry)
        lm_request_message = self.LM_TRANSITION_REQUEST_LOG.format(
            instance=self.MODE_CRASH_ON_CREATE_RESTARTS, lifecycle=self.LIFECYCLE_STATE_CREATED
        )
        self.verify_message_exactly_n_times(log_processor, lm_request_message, 2)

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_bundle_non_restartable_crashed_on_create_should_be_notified_and_not_restarted(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        self.sdv_device.execute_shell_command_in_subprocess(self.MODE_CRASH_ON_CREATE_NO_RESTARTS, self.SEND_CUSTOM_MODE_COMMAND.format(state=self.MODE_CRASH_ON_CREATE_NO_RESTARTS))
        # Wait until the mode has finished enforcing to make sure all expected logs are already there.
        self.wait_for_logcat(self.FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_LOG.format(state=self.MODE_CRASH_ON_CREATE_NO_RESTARTS))

        log_processor = self.sdv_device.advance_logcat()

        # Verify RETRYING state is not notified during the transition for a non-restartable bundle
        retrying_message = self.NOTIFYING_SUBSCRIBERS_INSTANCE_STATE_LOG.format(
            instance=self.MODE_CRASH_ON_CREATE_NO_RESTARTS, recovery=self.RECOVERY_STATE_RETRYING, lifecycle=self.LIFECYCLE_STATE_CREATED
        )
        self.verify_message_not_found(retrying_message)

        # Verify RETRY_FAILED state is notified during the transition. This can happen multiple times,
        # as we don't handle repeated notifications. But then we check that the restart was applied
        # only once.
        retry_failed_message = self.NOTIFYING_SUBSCRIBERS_INSTANCE_STATE_LOG.format(
            instance=self.MODE_CRASH_ON_CREATE_NO_RESTARTS, recovery=self.RECOVERY_STATE_RETRY_FAILED, lifecycle=self.LIFECYCLE_STATE_CREATED
        )
        self.verify_message_found(retry_failed_message)

        # Verify that the create request was attempted exactly once (initial attempt, no retries)
        lm_request_message = self.LM_TRANSITION_REQUEST_LOG.format(
            instance=self.MODE_CRASH_ON_CREATE_NO_RESTARTS, lifecycle=self.LIFECYCLE_STATE_CREATED
        )
        self.verify_message_exactly_n_times(log_processor, lm_request_message, 1)

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
