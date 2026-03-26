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

"""E2E test that verifies the recovery of service bundles by Orchestrator and reporting by Health Monitor.

Tests is on one SDV VM
"""

from mobly import asserts
import logging
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling


class SdvE2ERecoveryServiceBundlesHmOrchTest(
    sdv_base_test.SdvBaseTestClass
):

    DUMPSYS_HM_COMMAND = "dumpsys com.google.sdv.ISdvAgent/hm"
    RUN_ORCH_CUSTOM_MODE_SAMPLE_COMMAND = 'orch_custom_mode_sample E2E-TESTS {mode}'
    CUSTOM_MODES_PROCESS = "custom_mode_process"

    HEALTH_SAMPLE_LOG_NAME = "oem_sample_vm_health"
    ORCHESTRATION_AGENT_LOG_NAME = "sdv_orchestration_agent"

    HM_MONITORED_SAMPLE_PACKAGE_NAME = "com.android.sdv.sample.oem.health.monitored"
    HM_SAMPLE_BUNDLE_NAME = "SampleHMBundle"
    MONITORED_NON_RESTARTABLE_HM_INSTANCE_NAME = "no-restarts"
    MONITORED_RESTARTABLE_HM_INSTANCE_NAME = "restartable"

    LIFECYCLE_SAMPLE_PACKAGE_NAME = "com.android.sdv.sample.lifecycle"
    LIFECYCLE_SAMPLE_BUNDLE_NAME = "LifecycleCppSampleServiceBundle"
    NOT_MONITORED_NON_RESTARTABLE_LIFECYCLE_INSTANCE = "crashed-restarted"

    ORCHESTRATOR_SAMPLE_PACKAGE_NAME = "com.android.sdv.test.orchestrator"
    ORCHESTRATOR_SAMPLE_BUNDLE_NAME = "OrchestratorSampleRustServiceBundle"
    LONG_RECOVERY_BUNDLE_INSTANCE_NAME = "crash-on-start-long-recovery"

    VM_HEALTHY_HM_LOG = "VM is HEALTHY"
    VM_UNHEALTHY_HM_LOG = "VM is UNHEALTHY"
    ALL_SERVICES_ALIVE_HM_LOG = "All service bundles are ALIVE"
    NOT_ALL_SERVICES_ALIVE_HM_LOG = "Not all service bundles are ALIVE"
    LIST_CRASHING_SERVICES_HM_LOG = r'List of crashing services in VM .*:.*".*:{package}.{bundle}/{instance}".*'
    LIST_RECOVERING_SERVICES_HM_LOG = r'List of recovering services in VM .*:.*".*:{package}.{bundle}/{instance}".*'
    LIST_ALL_SERVICES_HM_LOG = r'List of all services in VM .*:.*".*:{package}.{bundle}/{instance}".*'
    LIST_UNHEALTHY_BUNDLES_HM_LOG = r'List of unhealthy services in VM .*:.*".*:{package}.{bundle}/{instance}".*'

    BUNDLE_CRASHED_ORCH_LOG = r'Service bundle crashed for fqin: .* "{package}", .*: "{bundle}", .*: "{instance}" .*'
    BUNDLE_STARTED_SUCCESS_ORCH_LOG = r'Request for moving service bundle .*: "{package}", .*: "{bundle}", .*: "{instance}" }} to STARTED state was Ok\(\(\)\)'
    NON_RESTARTABLE_BUNDLE_CRASHED_ORCH_LOG = r'Bundle with .*: "{package}", .*: "{bundle}", .*: "{instance}" }} has crashed, but it is not restartable.'
    FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_ORCH_LOG = 'Finished enforcing mode \'Custom\\("E2E-TESTS"\\)\' with state \'"{mode}"\'. successfully'

    RESET_CUSTOM_MODE = "reset"
    HEALTH_MONITORED_BUNDLE_START_MODE = "health-monitored-start"
    HEALTH_MONITORING_AND_MONITORED_BUNDLES_START_MODE = "monitoring-and-monitored-bundles-start"
    MONITORING_AND_RECOVERING_BUNDLES_START_MODE = "monitoring-and-recovering-bundles-start"

    def kill_bundle(self, bundle_name, instance_name):
        # Process name for service bundle is constructed as: bundle_name:instance_name
        process_id = self.sdv_device.execute_shell_command(
            f"pgrep -f {bundle_name}:{instance_name}")

        asserts.assert_is_not_none(process_id)

        self.sdv_device.execute_shell_command(
            "kill " + process_id
        )

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()
        self.device_name = self.sdv_device.prop.get(
            SdvDeviceProperty.INSTANCE_NAME)

    def setup_test(self):
        super().setup_test()
        # Make sure we are in a clean state and all bundles are destroyed.
        self.sdv_device.execute_shell_command_in_subprocess(
            "setup_process", self.RUN_ORCH_CUSTOM_MODE_SAMPLE_COMMAND.format(mode=self.RESET_CUSTOM_MODE))
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.ORCHESTRATION_AGENT_LOG_NAME,
            expected_result=self.FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_ORCH_LOG.format(
                mode=self.RESET_CUSTOM_MODE),
            assert_msg="Custom mode did not reset",
        )
        # Make sure we start the test with clean logcat
        self.sdv_device.clear_logcat()

    def test_hm_reports_crashed_non_restartable_bundle_status(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # Trigger initial start of the service bundle
        self.sdv_device.execute_shell_command_in_subprocess(
            self.CUSTOM_MODES_PROCESS, self.RUN_ORCH_CUSTOM_MODE_SAMPLE_COMMAND.format(mode=self.HEALTH_MONITORED_BUNDLE_START_MODE))
        # And verify that the bundle was transitioned to the required state.
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.ORCHESTRATION_AGENT_LOG_NAME,
            expected_result=self.BUNDLE_STARTED_SUCCESS_ORCH_LOG.format(
                package=self.HM_MONITORED_SAMPLE_PACKAGE_NAME,
                bundle=self.HM_SAMPLE_BUNDLE_NAME,
                instance=self.MONITORED_NON_RESTARTABLE_HM_INSTANCE_NAME),
            assert_msg="Bundle did not start",
        )

        # Kill the non restartable bundle
        self.kill_bundle(self.HM_SAMPLE_BUNDLE_NAME,
                         self.MONITORED_NON_RESTARTABLE_HM_INSTANCE_NAME)

        # Verify bundle has crashed
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.ORCHESTRATION_AGENT_LOG_NAME,
            expected_result=self.NON_RESTARTABLE_BUNDLE_CRASHED_ORCH_LOG.format(
                package=self.HM_MONITORED_SAMPLE_PACKAGE_NAME,
                bundle=self.HM_SAMPLE_BUNDLE_NAME,
                instance=self.MONITORED_NON_RESTARTABLE_HM_INSTANCE_NAME),
            assert_msg="Crash notification was not received",
        )

        # Verify health status of bundles through dumpsys
        expected_dump = (
            f"ID: FQIN: {self.device_name}:{self.HM_MONITORED_SAMPLE_PACKAGE_NAME}.{self.HM_SAMPLE_BUNDLE_NAME}/{self.MONITORED_NON_RESTARTABLE_HM_INSTANCE_NAME}\n"
            "Recovery State: FailedRecovery\n"
            "Lifecycle State: Started\n"
            "Health Status: Unhealthy"
        )

        report = self.sdv_device.execute_shell_command(self.DUMPSYS_HM_COMMAND)
        asserts.assert_in(
            expected_dump, report,
            f"Did not find dump:\n{expected_dump}\n\nin dumpsys report:\n{report}"
        )

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_hm_reports_crashed_restartable_bundle_status(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # Trigger initial start of the service bundle
        self.sdv_device.execute_shell_command_in_subprocess(
            self.CUSTOM_MODES_PROCESS, self.RUN_ORCH_CUSTOM_MODE_SAMPLE_COMMAND.format(mode=self.HEALTH_MONITORED_BUNDLE_START_MODE))
        # And verify that the bundle was transitioned to the required state.
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.ORCHESTRATION_AGENT_LOG_NAME,
            expected_result=self.BUNDLE_STARTED_SUCCESS_ORCH_LOG.format(
                package=self.HM_MONITORED_SAMPLE_PACKAGE_NAME,
                bundle=self.HM_SAMPLE_BUNDLE_NAME,
                instance=self.MONITORED_RESTARTABLE_HM_INSTANCE_NAME),
            assert_msg="Bundle did not start",
        )

        # Clear logcat so we are sure we are fetching the restart after the crash and not the previous start
        self.sdv_device.clear_logcat()

        # Kill the restartable bundle
        self.kill_bundle(self.HM_SAMPLE_BUNDLE_NAME,
                         self.MONITORED_RESTARTABLE_HM_INSTANCE_NAME)

        # Verify that the bundle crashed and restarted
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.ORCHESTRATION_AGENT_LOG_NAME,
            expected_result=self.BUNDLE_CRASHED_ORCH_LOG.format(
                package=self.HM_MONITORED_SAMPLE_PACKAGE_NAME,
                bundle=self.HM_SAMPLE_BUNDLE_NAME,
                instance=self.MONITORED_RESTARTABLE_HM_INSTANCE_NAME),
            assert_msg="Crash notification was not received",
        )
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.ORCHESTRATION_AGENT_LOG_NAME,
            expected_result=self.BUNDLE_STARTED_SUCCESS_ORCH_LOG.format(
                package=self.HM_MONITORED_SAMPLE_PACKAGE_NAME,
                bundle=self.HM_SAMPLE_BUNDLE_NAME,
                instance=self.MONITORED_RESTARTABLE_HM_INSTANCE_NAME),
            assert_msg="Bundle did not start",
        )

        # Verify health status of bundles through dumpsys
        expected_dump = (
            f"ID: FQIN: {self.device_name}:{self.HM_MONITORED_SAMPLE_PACKAGE_NAME}.{self.HM_SAMPLE_BUNDLE_NAME}/{self.MONITORED_RESTARTABLE_HM_INSTANCE_NAME}\n"
            "Recovery State: Normal\n"
            "Lifecycle State: Started\n"
            "Health Status: Healthy"
        )

        report = self.sdv_device.execute_shell_command(self.DUMPSYS_HM_COMMAND)
        asserts.assert_in(
            expected_dump, report, f"Did not find dump:\n{expected_dump}\n\nin dumpsys report:\n{report}"
        )

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_hm_reports_vm_and_bundle_state_on_monitored_bundle_crash(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # Trigger initial start of the service bundles
        self.sdv_device.execute_shell_command_in_subprocess(self.CUSTOM_MODES_PROCESS, self.RUN_ORCH_CUSTOM_MODE_SAMPLE_COMMAND.format(
            mode=self.HEALTH_MONITORING_AND_MONITORED_BUNDLES_START_MODE))

        # Verify that the bundle that will be killed is running
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.LIST_ALL_SERVICES_HM_LOG.format(
                package=self.HM_MONITORED_SAMPLE_PACKAGE_NAME,
                bundle=self.HM_SAMPLE_BUNDLE_NAME,
                instance=self.MONITORED_NON_RESTARTABLE_HM_INSTANCE_NAME),
            assert_msg="Bundle no-restarts instance running not reported",
        )
        # Verify all services are alive
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.ALL_SERVICES_ALIVE_HM_LOG,
            assert_msg="Not all services are alive",
        )
        # Verify that the VM is healthy
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.VM_HEALTHY_HM_LOG,
            assert_msg="VM is not healthy",
        )

        # WHEN the monitored service bundle crashes
        self.kill_bundle(self.HM_SAMPLE_BUNDLE_NAME,
                         self.MONITORED_NON_RESTARTABLE_HM_INSTANCE_NAME)

        # Check that the VM became unhealthy
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.VM_UNHEALTHY_HM_LOG,
            assert_msg="VM did not become Unhealthy",
        )
        # Check not all service bundles are alive
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.NOT_ALL_SERVICES_ALIVE_HM_LOG,
            assert_msg="All services are alive",
        )
        # Clear logcat again. At this point (VM unhealthy) we know the bundle is crashing so it should not show as running.
        self.sdv_device.clear_logcat()

        # Check that the bundle is reported as crashing
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.LIST_CRASHING_SERVICES_HM_LOG.format(
                package=self.HM_MONITORED_SAMPLE_PACKAGE_NAME,
                bundle=self.HM_SAMPLE_BUNDLE_NAME,
                instance=self.MONITORED_NON_RESTARTABLE_HM_INSTANCE_NAME),
            assert_msg="Bundle crashing not reported",
        )
        # Check that the bundle is reported as unhealthy
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.LIST_UNHEALTHY_BUNDLES_HM_LOG.format(
                package=self.HM_MONITORED_SAMPLE_PACKAGE_NAME,
                bundle=self.HM_SAMPLE_BUNDLE_NAME,
                instance=self.MONITORED_NON_RESTARTABLE_HM_INSTANCE_NAME),
            assert_msg="Bundle unhealthy not reported",
        )
        # Verify that bundle is not running
        log = self.sdv_device.grep_from_logcat(self.HEALTH_SAMPLE_LOG_NAME)
        asserts.assert_not_regex(log, self.LIST_ALL_SERVICES_HM_LOG.format(
            package=self.HM_MONITORED_SAMPLE_PACKAGE_NAME,
            bundle=self.HM_SAMPLE_BUNDLE_NAME,
            instance=self.MONITORED_NON_RESTARTABLE_HM_INSTANCE_NAME))

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_hm_reports_vm_and_bundle_state_on_non_monitored_bundle_crash(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # Trigger initial start of the service bundles
        self.sdv_device.execute_shell_command_in_subprocess(self.CUSTOM_MODES_PROCESS, self.RUN_ORCH_CUSTOM_MODE_SAMPLE_COMMAND.format(
            mode=self.HEALTH_MONITORING_AND_MONITORED_BUNDLES_START_MODE))

        # Verify that the bundle is running
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.LIST_ALL_SERVICES_HM_LOG.format(
                package=self.LIFECYCLE_SAMPLE_PACKAGE_NAME,
                bundle=self.LIFECYCLE_SAMPLE_BUNDLE_NAME,
                instance=self.NOT_MONITORED_NON_RESTARTABLE_LIFECYCLE_INSTANCE),
            assert_msg="Bundle running not reported",
        )
        # Verify that all bundles are alive
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.ALL_SERVICES_ALIVE_HM_LOG,
            assert_msg="Not all services are alive",
        )
        # Verify that the VM is healthy
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.VM_HEALTHY_HM_LOG,
            assert_msg="VM is not healthy",
        )

        # WHEN the not monitored service bundle crashes
        self.kill_bundle(self.LIFECYCLE_SAMPLE_BUNDLE_NAME,
                         self.NOT_MONITORED_NON_RESTARTABLE_LIFECYCLE_INSTANCE)

        # Check that the bundle is reported as crashing by orchestrator
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.ORCHESTRATION_AGENT_LOG_NAME,
            expected_result=self.NON_RESTARTABLE_BUNDLE_CRASHED_ORCH_LOG.format(
                package=self.LIFECYCLE_SAMPLE_PACKAGE_NAME,
                bundle=self.LIFECYCLE_SAMPLE_BUNDLE_NAME,
                instance=self.NOT_MONITORED_NON_RESTARTABLE_LIFECYCLE_INSTANCE),
            assert_msg="Crash notification was not received",
        )
        # Check that the bundle is reported as crashing by HM
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.LIST_CRASHING_SERVICES_HM_LOG.format(
                package=self.LIFECYCLE_SAMPLE_PACKAGE_NAME,
                bundle=self.LIFECYCLE_SAMPLE_BUNDLE_NAME,
                instance=self.NOT_MONITORED_NON_RESTARTABLE_LIFECYCLE_INSTANCE),
            assert_msg="Bundle crashing not reported",
        )
        # Clear logcat after we find the bundle is crashing to avoid verifying old logs
        self.sdv_device.clear_logcat()
        # Verify not all services are alive
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.NOT_ALL_SERVICES_ALIVE_HM_LOG,
            assert_msg="All services are alive",
        )
        # Verify that bundle is not running
        log = self.sdv_device.grep_from_logcat(self.HEALTH_SAMPLE_LOG_NAME)
        asserts.assert_not_regex(log, self.LIST_ALL_SERVICES_HM_LOG.format(
            package=self.LIFECYCLE_SAMPLE_PACKAGE_NAME,
            bundle=self.LIFECYCLE_SAMPLE_BUNDLE_NAME,
            instance=self.NOT_MONITORED_NON_RESTARTABLE_LIFECYCLE_INSTANCE))
        #  Check that bundle is not reported as unhealthy as it's not HB monitored
        asserts.assert_not_regex(log, self.LIST_UNHEALTHY_BUNDLES_HM_LOG.format(
            package=self.LIFECYCLE_SAMPLE_PACKAGE_NAME,
            bundle=self.LIFECYCLE_SAMPLE_BUNDLE_NAME,
            instance=self.NOT_MONITORED_NON_RESTARTABLE_LIFECYCLE_INSTANCE))
        # Verify VM is still healthy
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.VM_HEALTHY_HM_LOG,
            assert_msg="VM is not healthy",
        )
        # Check that the VM did not become unhealthy
        asserts.assert_not_in(
            self.VM_UNHEALTHY_HM_LOG,
            log,
            f"Not expected logcat result found: {self.VM_UNHEALTHY_HM_LOG}",
        )

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_hm_dumps_non_restartable_bundle_state_change(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # Trigger initial start of the service bundles
        self.sdv_device.execute_shell_command_in_subprocess(
            self.CUSTOM_MODES_PROCESS, self.RUN_ORCH_CUSTOM_MODE_SAMPLE_COMMAND.format(mode=self.HEALTH_MONITORED_BUNDLE_START_MODE))
        # Wait until mode finished enforcing
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.ORCHESTRATION_AGENT_LOG_NAME,
            expected_result=self.FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_ORCH_LOG.format(
                mode=self.HEALTH_MONITORED_BUNDLE_START_MODE),
            assert_msg="Custom mode was not enforced",
        )

        # Verify that HM dump contains the initial bundle state
        expected_dump_started = (
            f"ID: FQIN: {self.device_name}:{self.HM_MONITORED_SAMPLE_PACKAGE_NAME}.{self.HM_SAMPLE_BUNDLE_NAME}/{self.MONITORED_NON_RESTARTABLE_HM_INSTANCE_NAME}\n"
            "Recovery State: Normal\n"
            "Lifecycle State: Started\n"
            "Health Status: Healthy"
        )
        report = self.sdv_device.execute_shell_command(self.DUMPSYS_HM_COMMAND)
        asserts.assert_in(
            expected_dump_started, report,
            f"Did not find dump:\n{expected_dump_started}\n\nin dumpsys report:\n{report}"
        )

        # Trigger reset mode to destroy all service bundles
        # This needs to be run in separate subprocess to avoid blocking the test
        self.sdv_device.execute_shell_command_in_subprocess(
            "reset_mode_process", self.RUN_ORCH_CUSTOM_MODE_SAMPLE_COMMAND.format(mode=self.RESET_CUSTOM_MODE))
        # Wait until mode finished enforcing
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.ORCHESTRATION_AGENT_LOG_NAME,
            expected_result=self.FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_ORCH_LOG.format(
                mode=self.RESET_CUSTOM_MODE),
            assert_msg="Custom mode did not reset",
        )

        # Verify that HM dump contains the new bundle state
        expected_dump_destroyed = (
            f"ID: FQIN: {self.device_name}:{self.HM_MONITORED_SAMPLE_PACKAGE_NAME}.{self.HM_SAMPLE_BUNDLE_NAME}/{self.MONITORED_NON_RESTARTABLE_HM_INSTANCE_NAME}\n"
            "Recovery State: Normal\n"
            "Lifecycle State: Destroyed\n"
            "Health Status: Healthy"
        )
        report = self.sdv_device.execute_shell_command(self.DUMPSYS_HM_COMMAND)
        asserts.assert_in(
            expected_dump_destroyed, report,
            f"Did not find dump:\n{expected_dump_destroyed}\n\nin dumpsys report:\n{report}"
        )

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_hm_dumps_restartable_bundle_state_change(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # Trigger initial start of the service bundles
        self.sdv_device.execute_shell_command_in_subprocess(
            self.CUSTOM_MODES_PROCESS, self.RUN_ORCH_CUSTOM_MODE_SAMPLE_COMMAND.format(mode=self.HEALTH_MONITORED_BUNDLE_START_MODE))
        # Wait until mode finished enforcing
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.ORCHESTRATION_AGENT_LOG_NAME,
            expected_result=self.FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_ORCH_LOG.format(
                mode=self.HEALTH_MONITORED_BUNDLE_START_MODE),
            assert_msg="Custom mode was not enforced",
        )

        # Verify that HM dump contains the initial bundle state
        expected_dump_started = (
            f"ID: FQIN: {self.device_name}:{self.HM_MONITORED_SAMPLE_PACKAGE_NAME}.{self.HM_SAMPLE_BUNDLE_NAME}/{self.MONITORED_RESTARTABLE_HM_INSTANCE_NAME}\n"
            "Recovery State: Normal\n"
            "Lifecycle State: Started\n"
            "Health Status: Healthy"
        )
        report = self.sdv_device.execute_shell_command(self.DUMPSYS_HM_COMMAND)
        asserts.assert_in(
            expected_dump_started, report,
            f"Did not find dump:\n{expected_dump_started}\n\nin dumpsys report:\n{report}"
        )

        # Trigger reset mode to destroy all service bundles
        # This needs to be run in separate subprocess to avoid blocking the test
        self.sdv_device.execute_shell_command_in_subprocess(
            "reset_mode_process", self.RUN_ORCH_CUSTOM_MODE_SAMPLE_COMMAND.format(mode=self.RESET_CUSTOM_MODE))
        # Wait until mode finished enforcing
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.ORCHESTRATION_AGENT_LOG_NAME,
            expected_result=self.FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_ORCH_LOG.format(
                mode=self.RESET_CUSTOM_MODE),
            assert_msg="Custom mode did not reset",
        )

        # Verify that HM dump contains the new bundle state
        expected_dump_destroyed = (
            f"ID: FQIN: {self.device_name}:{self.HM_MONITORED_SAMPLE_PACKAGE_NAME}.{self.HM_SAMPLE_BUNDLE_NAME}/{self.MONITORED_RESTARTABLE_HM_INSTANCE_NAME}\n"
            "Recovery State: Normal\n"
            "Lifecycle State: Destroyed\n"
            "Health Status: Healthy"
        )
        report = self.sdv_device.execute_shell_command(self.DUMPSYS_HM_COMMAND)
        asserts.assert_in(
            expected_dump_destroyed, report,
            f"Did not find dump:\n{expected_dump_destroyed}\n\nin dumpsys report:\n{report}"
        )

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )

    def test_hm_reports_recovering_bundle_on_crash(self):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # Trigger start of the monitoring bundle that will report the recovering one,
        # and the service bundle that will crash on start with a tiny sleep so that we
        # can capture it as recovering.
        self.sdv_device.execute_shell_command_in_subprocess(self.CUSTOM_MODES_PROCESS, self.RUN_ORCH_CUSTOM_MODE_SAMPLE_COMMAND.format(mode=self.MONITORING_AND_RECOVERING_BUNDLES_START_MODE))
        # Wait until mode finished enforcing so we have all the needed logs.
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.ORCHESTRATION_AGENT_LOG_NAME,
            expected_result=self.FINISHED_ENFORCING_CUSTOM_MODE_SUCCESS_ORCH_LOG.format(mode=self.MONITORING_AND_RECOVERING_BUNDLES_START_MODE),
            assert_msg="Custom mode was not enforced",
        )

        # Check that the bundle has reported as recovering.
        # This bundle is not monitored by HM, but it still shows in the recovering list.
        polling.wait_and_verify_expected_logs(
            sdv_device=self.sdv_device,
            grep_text=self.HEALTH_SAMPLE_LOG_NAME,
            expected_result=self.LIST_RECOVERING_SERVICES_HM_LOG.format(
                package=self.ORCHESTRATOR_SAMPLE_PACKAGE_NAME,
                bundle=self.ORCHESTRATOR_SAMPLE_BUNDLE_NAME,
                instance=self.LONG_RECOVERY_BUNDLE_INSTANCE_NAME),
            assert_msg="Bundle recovering not reported",
        )

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
