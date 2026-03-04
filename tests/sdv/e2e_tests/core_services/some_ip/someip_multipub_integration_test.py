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

"""SDV SOME/IP Multipub Device Integration Test"""

from mobly import asserts
from itertools import groupby
import time
import logging

from sdv_perfetto import perfetto_collector, perfetto_trace_processor
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSomeIpMultipubDeviceIntegrationTest(sdv_base_test.SdvBaseTestClass):
    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()
        self.perfetto_collector = perfetto_collector.PerfettoCollector(
            device=self.sdv_device
        )

    def log_is_empty(self, log):
        return len(self.sdv_device.read_file(log)) == 0

    def assert_bundle_successful_execution(self, action, service, instance):
        PUBLISHER_COMMAND = "sdv_service_bundle {action} local-vm:com.android.sdv.sample.someip.{service}/{instance}"
        ERROR_MESSAGE = "Failed to {action} {service}: {log}"

        log = self.sdv_device.execute_shell_command_in_subprocess_log(
            PUBLISHER_COMMAND.format(
                action=action, service=service, instance=instance)
        )
        # wait for 1 second to ensure the command was executed and log was written.
        time.sleep(1)
        asserts.assert_true(
            self.log_is_empty(log),
            ERROR_MESSAGE.format(
                action=action,
                service=service,
                log=self.sdv_device.read_file(log)
            ),
        )

    def wait_for_logcat(self, grep_text, expected_result, timeout=5, poll_interval=0.1):
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

    def test_someip_multipub_subscriber(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        START = "start"
        STOP = "create"
        TEMPERATURE_SENSOR = "TemperatureSensor"
        INSTANCE1 = "instance1"
        INSTANCE2 = "instance2"

        TEMPERATURE_SENSOR1_LOGCAT_TAG = "com_android_sdv_sample_someip_TemperatureSensor_instance1"
        TEMPERATURE_SENSOR2_LOGCAT_TAG = "com_android_sdv_sample_someip_TemperatureSensor_instance2"

        TEMPERATURE_SENSOR_EXPECTED_LOG = "Starting temperature sensor"

        self.perfetto_collector.start_trace()

        self.assert_bundle_successful_execution(
            action=START, service=TEMPERATURE_SENSOR, instance=INSTANCE1
        )
        temperature_sensor1_log = self.sdv_device.grep_from_logcat(
            TEMPERATURE_SENSOR1_LOGCAT_TAG
        )

        asserts.assert_in(
            TEMPERATURE_SENSOR_EXPECTED_LOG,
            temperature_sensor1_log,
            "Temperature Sensor service instance 1 didn't start",
        )

        self.assert_bundle_successful_execution(
            action=START, service=TEMPERATURE_SENSOR, instance=INSTANCE2
        )
        temperature_sensor2_log = self.sdv_device.grep_from_logcat(
            TEMPERATURE_SENSOR2_LOGCAT_TAG
        )

        asserts.assert_in(
            TEMPERATURE_SENSOR_EXPECTED_LOG,
            temperature_sensor2_log,
            "Temperature Sensor service instance 2 didn't start",
        )

        SOMEIP_CONFIG_FILE = (
            "VSOMEIP_CONFIGURATION=/vendor/etc/vsomeip/temperature_tester.json"
        )
        SOMEIP_BASE_PATH = "VSOMEIP_BASE_PATH=/data/vendor/vsomeip/"
        SOMEIP_TESTER_COMMAND = "/vendor/bin/sdv_vsomeip_temperature_tester"

        self.sdv_device.execute_shell_command_in_subprocess_log(
            SOMEIP_CONFIG_FILE + " " + SOMEIP_BASE_PATH + " " + SOMEIP_TESTER_COMMAND
        )

        SOMEIP_TESTER_LOGCAT_TAG = "sdv_vsomeip_temperature_tester"
        SOMEIP_TESTER_EXPECTED_LOG = "Received messages from 2 separate publishers"

        self.wait_for_logcat(SOMEIP_TESTER_LOGCAT_TAG,
                             SOMEIP_TESTER_EXPECTED_LOG)

        self.assert_bundle_successful_execution(
            action=STOP, service=TEMPERATURE_SENSOR, instance=INSTANCE1
        )
        self.assert_bundle_successful_execution(
            action=STOP, service=TEMPERATURE_SENSOR, instance=INSTANCE2
        )

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed"
        )

    def teardown_test(self):
        trace_file_path = self.perfetto_collector.stop_trace(tag="device1")
        self.process_trace_and_crystalball_upload(trace_file_path)
        super().teardown_test()

    def process_trace_and_crystalball_upload(self, trace_file_path):
        trace = perfetto_trace_processor.PerfettoTraceProcessor(
            trace_file_path)

        QUERY_COMMAND_1 = """SELECT * FROM slice WHERE name LIKE '%get_publication_descriptor, unit_type_name = "Temperature"%'"""
        QUERY_COMMAND_2 = """SELECT * FROM slice WHERE name LIKE '%process_message, unit_type_name = "Temperature"%'"""

        avg_duration_get_publication_descriptor = self.extract_average_duration(
            trace, QUERY_COMMAND_1
        )
        avg_duration_process_message = self.extract_average_duration(
            trace, QUERY_COMMAND_2
        )

        METRIC_NAME_1 = "get_publication_descriptor-duration-avg(µs)"
        METRIC_NAME_2 = "process_message-duration-avg(µs)"
        metrics = {
            METRIC_NAME_1: avg_duration_get_publication_descriptor,
            METRIC_NAME_2: avg_duration_process_message
        }
        TESTNAME = f"{__class__.__name__}#someip_broker_sdv_to_someip_processing"

        perfetto_trace_processor.export_to_crystalball(
            {TESTNAME: metrics},
            output_dir=self.sdv_device.log_path(),
            test_name=TESTNAME,
            omit_base_name=False,
        )

    def extract_average_duration(self, trace, initial_query):
        results = trace.query(initial_query)
        poll_durations = (
            {
                "duration": r.dur,
                "id": int(r.name.split("await_start_timestamp = ", 1)[1]),
            }
            for r in results
        )
        poll_durations_sorted = sorted(poll_durations, key=lambda x: x["id"])
        poll_durations_grouped = groupby(
            poll_durations_sorted,
            key=lambda x: x["id"]
        )
        async_fn_durations = [
            sum([poll["duration"] for poll in polls])
            for _, polls in poll_durations_grouped
        ]
        average_duration = sum(async_fn_durations) / len(async_fn_durations)
        # nanosec to microsec conversion:
        average_duration = int(average_duration / 1000.0)

        logging.info(
            f"average_duration(µs): {average_duration} for initial_query: {initial_query}"
        )

        return average_duration


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
