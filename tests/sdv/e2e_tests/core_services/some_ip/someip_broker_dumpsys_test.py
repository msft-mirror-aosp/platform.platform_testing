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

"""SDV SOME/IP Broker Dumpsys Test"""

from mobly import asserts
import time
import logging
import re

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner

class SdvSomeIpBrokerDumpsysTest(sdv_base_test.SdvBaseTestClass):
    EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE = r"""AGENT NAME: SDV Agent dump - SOME/IP Broker
AGENT FQIN: vm:com\.sdv\.someip\.SomeIpBroker/default
AGENT STATE: someip_broker_agent has started successfully
----------------
----------------
INTERNAL STATE REPORTERS:

\*NAME: SOME/IP Broker Translation Benchmark
\*REPORT:
Summary \(accumulated time in window\):"""

    EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE = r"""AGENT NAME: SDV Agent dump - SOME/IP Broker
AGENT FQIN: vm:com\.sdv\.someip\.SomeIpBroker/default
AGENT STATE: someip_broker_agent has started successfully
----------------
----------------
INTERNAL STATE REPORTERS:

\*NAME: SOME/IP Broker Translation Benchmark
\*REPORT:
Statistics for com\.sdv\.someip\.BenchmarkInterface:
Messages Counter = [0-9]+
Sum = [0-9]+ us
Average = [0-9]+ us
Min = [0-9]+ us
Max = [0-9]+ us
Median = [0-9]+ us
P90 = [0-9]+ us
Summary \(accumulated time in window\):
com\.sdv\.someip\.BenchmarkInterface: [0-9]+ us"""

    SOMEIP_BROKER_DUMP_BINDER_NAME = "com.google.sdv.ISdvAgent/someip_broker"

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()

    def test_someip_benchmark(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        dumpsys_report = self.sdv_device.dumpsys(self.SOMEIP_BROKER_DUMP_BINDER_NAME)
        if not re.search(re.compile(self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE), dumpsys_report):
            asserts.fail(
                f'Message not found [{self.EXPECTED_DUMPSYS_BEFORE_EXECUTING_SAMPLE}] in dumpsys report: {dumpsys_report}',
            )

        self._start_someip_tester()

        self._start_sdv_bundle("local-vm", "com.sdv.google.sample.someip.SomeIpBenchmark", "instance1")

        self._wait_for_logcat(
            "someip_benchmark",
            "someip_benchmark_sample: Finished test - messages received",
            timeout=10,
            poll_interval=1,
        )

        dumpsys_report = self.sdv_device.dumpsys(self.SOMEIP_BROKER_DUMP_BINDER_NAME)
        if not re.search(re.compile(self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE), dumpsys_report):
            asserts.fail(
                f'Message not found [{self.EXPECTED_DUMPSYS_AFTER_EXECUTING_SAMPLE}] in dumpsys report: {dumpsys_report}',
            )

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed"
        )

    def _start_someip_tester(self):
        """Starts the sdv_vsomeip_pubsub_benchmark binary as a SOME/IP source."""

        SOMEIP_CONFIG_FILE = "/vendor/etc/vsomeip/pubsub_benchmark.json"
        SOMEIP_BASE_PATH = "/data/vendor/vsomeip/"
        SOMEIP_TESTER_COMMAND = "/vendor/bin/sdv_vsomeip_pubsub_benchmark"

        self.sdv_device.execute_shell_command_in_subprocess_log(
            f"VSOMEIP_CONFIGURATION={SOMEIP_CONFIG_FILE} VSOMEIP_BASE_PATH={SOMEIP_BASE_PATH} {SOMEIP_TESTER_COMMAND}"
        )

        self._wait_for_logcat("sdv_vsomeip_pubsub_benchmark",  "(pubsub_benchmark) is registered")

    def _start_sdv_bundle(self, vm, service, instance):
        return self.sdv_device.execute_shell_command_in_subprocess_log(
            f"sdv_service_bundle start {vm}:{service}/{instance}")

    def _wait_for_logcat(self, grep_text, expected_result, timeout=5, poll_interval=0.1):
        """Polls the logcat output for a specific text until found or timeout.

        Args:
            grep_text: The text to search for in the logcat output.
            expected_result: The expected result to find.
            timeout: The maximum time (in seconds) to wait.
            poll_interval: The time (in seconds) between polls.
        """
        deadline = time.perf_counter() + timeout
        while time.perf_counter() < deadline:
            logcat_result = self.sdv_device.grep_from_logcat(grep_text)
            if expected_result in logcat_result:
                return
            time.sleep(poll_interval)

        asserts.fail(
            f"Logcat result not found within timeout: {expected_result}"
        )


if __name__ == "__main__":
    sdv_test_runner.run()
