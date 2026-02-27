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

"""E2E Orchestrator test for verifying that after orchestrator crashed and restarted again,
the modes that were set before the crash are persisted on the restart. We also check that the last power
and vehicle modes are read on subscription.

Tests is on one SDV VM
"""
from mobly import asserts
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling


class SdvE2EOrchestrationModesRecoveredAfterCrashTest(
    sdv_base_test.SdvBaseTestClass
):
    FINISHED_PROCESSING_VEHICLE_PARK = 'Finished enforcing mode \'Vehicle\' with state \'"PARK"\'. successfully.'
    FINISHED_PROCESSING_POWER_ON = 'Finished enforcing mode \'Power\' with state \'"ON"\'. successfully'
    FINISHED_PROCESSING_CUSTOM_MODE = 'Finished enforcing mode \'Custom("E2E-TESTS")\' with state \'"recover-custom-mode"\'. successfully'
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
        self.new_orch_process_session = self.sdv_device.interactive_session()

    def teardown_test(self):
        self.vpm_session.close()
        self.custom_mode_session.close()
        self.new_orch_process_session.close()
        super().teardown_test()

    def kill_orch_agent(self):
        process_id = self.sdv_device.execute_shell_command(
            "pgrep -f sdv_orchestration_agent")
        asserts.assert_is_not_none(process_id)
        self.sdv_device.execute_shell_command(
            "kill " + process_id
        )

    def wait_for_logcat(self, expected_result, timestamp=None, regex=False):
        def grep_with_timestamp(sdv_device, expected_result, timestamp):
            res_timestamp, _ = sdv_device.advance_logcat(
            ).find_message_after_timestamp(expected_result, timestamp, regex)
            return res_timestamp

        result = polling.wait_and_return_result(
            grep_with_timestamp, self.sdv_device, expected_result, timestamp)
        asserts.assert_is_not_none(result, f"Logcat result not found within timeout: {expected_result}")
        return result

    def test_modes_recovered_after_orch_crash(
        self
    ):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        self.vpm_session.send_command('vepsm power-state power-on')
        # Get the timestamp of the transitions completion so that we verify that the orchestrator recovers after the restart
        power_transition_completed_timestamp = self.wait_for_logcat(
            self.FINISHED_PROCESSING_POWER_ON)
        self.vpm_session.send_command('vepsm vehicle-state park')
        vehicle_transition_completed_timestamp = self.wait_for_logcat(
            self.FINISHED_PROCESSING_VEHICLE_PARK)
        self.custom_mode_session.send_command(
            'orch_custom_mode_sample E2E-TESTS recover-custom-mode')
        custom_transition_completed_timestamp = self.wait_for_logcat(
            self.FINISHED_PROCESSING_CUSTOM_MODE)

        # Kill orchestrator agent
        self.kill_orch_agent()

        # Start orchestrator agent
        self.new_orch_process_session.send_command(
            "/system/system_ext/bin/sdv_orchestration_agent")

        # Verify that the last vehicle and power modes are read on subscription
        power_transition_completed_after_orch_crash = self.wait_for_logcat(
            self.FINISHED_PROCESSING_POWER_ON, timestamp=power_transition_completed_timestamp)
        vehicle_transition_completed_after_orch_crash = self.wait_for_logcat(
            self.FINISHED_PROCESSING_VEHICLE_PARK, timestamp=vehicle_transition_completed_timestamp)
        # Now we check that the vehicle and power modes were started from the persisted file reading (when enforcing the Default mode)
        # and not from when the modes were retrieved on subscribe.
        started_vehicle_bundle_timestamp = self.wait_for_logcat(
            self.FINISHED_STARTING_RECOVER_VEHICLE_MODE_SERVICE, timestamp=vehicle_transition_completed_timestamp, regex=True)
        asserts.assert_less(
            started_vehicle_bundle_timestamp,
            vehicle_transition_completed_after_orch_crash,
            f"Vehicle mode was started {started_vehicle_bundle_timestamp} after the vehicle subscription {vehicle_transition_completed_after_orch_crash}"
        )
        started_power_bundle_timestamp = self.wait_for_logcat(
            self.FINISHED_STARTING_RECOVER_POWER_MODE_SERVICE, timestamp=power_transition_completed_timestamp, regex=True)
        asserts.assert_less(
            started_power_bundle_timestamp,
            power_transition_completed_after_orch_crash,
            f"Power mode was started {started_power_bundle_timestamp} after the power subscription {power_transition_completed_after_orch_crash}"
        )

        # With custom modes we cannot check if they were enforced as separate modes (as opposed to power-vehicle modes that we get the values on subscribe)
        # because they will be stored in the config, but enforced in the Default mode set.
        # We can verify that the bundle that is configured to start based on the custom mode was in fact started. This should have happened before the
        # power and vehicle mode subscriptions were started (after the crash).
        started_custom_bundle_timestamp = self.wait_for_logcat(
            self.FINISHED_STARTING_RECOVER_CUSTOM_MODE_SERVICE, timestamp=custom_transition_completed_timestamp, regex=True)
        asserts.assert_less(started_custom_bundle_timestamp, power_transition_completed_after_orch_crash,
                                              f"Custom mode was started ({started_custom_bundle_timestamp}) after the power subscription ({power_transition_completed_after_orch_crash})")
        asserts.assert_less(started_custom_bundle_timestamp, vehicle_transition_completed_after_orch_crash,
                                              f"Custom mode was started ({started_custom_bundle_timestamp}) after the vehicle subscription ({vehicle_transition_completed_after_orch_crash})")

        # We also verify that all the correct modes are stored inside the orch config by using dumpsys.
        # This is not enough though, because the values could have been stored but not triggered through the Default mode.
        # That's why we need to verify the enforcement and the dumpsys.
        expected_dump = [
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
        # Verify dumpsys output
        for dump_line in expected_dump:
            asserts.assert_regex(dump_report, dump_line,
                                                   f"Did not find: '{dump_line}' in dump report: {dump_report}")

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
