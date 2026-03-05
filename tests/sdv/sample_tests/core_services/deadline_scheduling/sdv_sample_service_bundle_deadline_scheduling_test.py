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

""" SDV Sample that automates deadline scheduling sample. """
from mobly import asserts
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling


class SdvSampleServiceBundleDeadlineSchedulingTest(sdv_base_test.SdvBaseTestClass):

    SCHEDULING_SERVICE_BUNDLE_FULL_NAME = "com.android.sdv.sample.lifecycle.LifecycleRustDeadlineSchedServiceBundle"
    CPU_AFFINITY_SERVICE_BUNDLE_FULL_NAME = "com.android.sdv.sample.lifecycle.LifecycleRustAffinitySchedServiceBundle"

    PROCESS_SCHEDULING_COMMAND = "for pid in $(ps -o tid --pid {pid} | tail -n +3); do chrt -p $pid; done"

    GET_PROCESS_THREADS_COMMAND = "ps -T -p {pid} | grep 'sched_thread'"
    PROCESS_CPU_AFFINITY_COMMAND = "taskset -p {tid}"
    DESIRED_THREAD_AFFINITY_OUTPUT = "pid {tid}'s current affinity mask: 3"


    LOGCAT_TAG = 'oem_deadline_sched_service_bundle_rust_sample::service'

    def start_service_bundle(self, service_bundle_name):
        return self.sdv_device.adb().execute_shell_command(
            f'sdv_service_bundle start local-vm:{service_bundle_name}/instance-1'
        )

    def log_enter(self):
        """ Unified logging enter test suit. """
        logging.info(f"{self.get_suite_name()} :: Start test {self.current_test_info.name}")

    def log_exit(self):
        """ Unified logging exit test suit. """
        logging.info(f"{self.get_suite_name()} :: Finished test {self.current_test_info.name}")

    def verify_deadline_scheduling(self, grep_text, timeout=30, poll_interval=0.1):
        """ Verifies that the deadline scheduling configuration was successfully applied. """

        logcat_grep_result = polling.wait_and_return_result(
            lambda: self.sdv_device.adb().grep_from_logcat(
                grep=f'{self.LOGCAT_TAG}: .*{grep_text}'
            ) or None,
            timeout=timeout,
            poll_interval=poll_interval
        )
        asserts.assert_is_not_none(logcat_grep_result, "Failed to find logcat entry")
        pid = logcat_grep_result.split()[2]
        logging.info(f"Scheduling configuration applied to the process with id: {pid}")
        sched_output = self.sdv_device.adb().execute_shell_command(self.PROCESS_SCHEDULING_COMMAND.format(pid = pid))
        logging.info(f"Scheduling configuration for the process {pid}: {sched_output}")
        asserts.assert_in(
            "SCHED_DEADLINE",
            sched_output,
            'Failed to verify that scheduling configuration was set'
        )
        logging.info("Deadline scheduling configuration was successfully applied")

    def verify_cpu_affinity(self, grep_text, timeout=30, poll_interval=0.1):
        """ Verifies that the CPU affinity was successfully applied. """

        logcat_grep_result = polling.wait_and_return_result(
            lambda: self.sdv_device.adb().grep_from_logcat(
                grep=f'{self.LOGCAT_TAG}: .*{grep_text}'
            ) or None,
            timeout=timeout,
            poll_interval=poll_interval
        )
        asserts.assert_is_not_none(logcat_grep_result, "Failed to find logcat entry")

        pid = logcat_grep_result.split()[2]
        logging.info(f"CPU Affinity service bundle has process id {pid}")
        process_threads_output = self.sdv_device.adb().execute_shell_command(self.GET_PROCESS_THREADS_COMMAND.format(pid = pid))
        tid = process_threads_output.split()[2]
        logging.info(f"CPU affinity applied to the thread {tid} that belongs to the process {pid}")
        cpu_affinity_output = self.sdv_device.adb().execute_shell_command(self.PROCESS_CPU_AFFINITY_COMMAND.format(tid = tid))
        logging.info(f"CPU affinity for the thread {tid}: {cpu_affinity_output}")
        desired_thread_affinity_output = self.DESIRED_THREAD_AFFINITY_OUTPUT.format(tid = tid)
        asserts.assert_equal(
            desired_thread_affinity_output,
            cpu_affinity_output,
            'Failed to verify that CPU affinity was set'
        )
        logging.info("CPU affinity was successfully applied")

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1")


    def test_deadline_scheduling(self):
        self.log_enter()

        self.start_service_bundle(self.SCHEDULING_SERVICE_BUNDLE_FULL_NAME)
        self.verify_deadline_scheduling("Setting thread scheduling parameters result: 0, errno: Success (os error 0)")

        self.log_exit()

    def test_cpu_affinity(self):
        self.log_enter()

        self.start_service_bundle(self.CPU_AFFINITY_SERVICE_BUNDLE_FULL_NAME)
        self.verify_cpu_affinity("Setting CPU affinity result: Ok(())")

        self.log_exit()


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
