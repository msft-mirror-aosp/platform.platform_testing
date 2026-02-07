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

"""SDV SOME/IP Benchmark Test"""

from mobly import asserts

import logging
import re

from absl.testing import parameterized
from sdv_perfetto import perfetto_trace_processor
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner
from sdv_test_fw.verification import polling


class SdvSomeIpBenchmarkTest(sdv_base_test.SdvBaseTestClass, parameterized.TestCase):

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()
        self.metrics = {}

        self.mode_default = self.sdv_device.prop.get(SdvDeviceProperty.BENCH_MODE_SYS)

        self.nr_bursts_default = self.sdv_device.prop.get(
            SdvDeviceProperty.NR_BURST_SYS)

        self.msg_per_burst_default = self.sdv_device.prop.get(
            SdvDeviceProperty.MSG_PER_BURST_SYS)

        self.burst_interval_default = self.sdv_device.prop.get(
            SdvDeviceProperty.BURST_INTERVAL_SYS)

    def teardown_class(self):
        self._configure_bench(self.mode_default, self.nr_bursts_default,
                              self.msg_per_burst_default, self.burst_interval_default)

        TESTNAME = f"{__class__.__name__}#incoming_someip_event"
        perfetto_trace_processor.export_to_crystalball(
            {TESTNAME: self.metrics},
            output_dir=self.sdv_device.log_path(),
            test_name=TESTNAME,
            omit_base_name=False,
        )

    def _configure_bench(self, mode, nr_bursts, msg_per_burst, burst_interval_ms):
        self.sdv_device.prop.set(SdvDeviceProperty.BENCH_MODE_SYS, mode)
        self.sdv_device.prop.set(SdvDeviceProperty.NR_BURST_SYS, nr_bursts)
        self.sdv_device.prop.set(SdvDeviceProperty.MSG_PER_BURST_SYS, msg_per_burst)
        self.sdv_device.prop.set(SdvDeviceProperty.BURST_INTERVAL_SYS, burst_interval_ms)

    @parameterized.named_parameters(
        {
            'testcase_name': 'collect_metrics_on_burst_of_identical_simple_messages',
            'bench_mode': '0',
            'nr_bursts': '500',
            'msg_per_burst': '1',
            'burst_interval_ms': '0',
            'metric_prefix': 'simple_mode_',
        },
        {
            'testcase_name': 'collect_metrics_on_burst_of_messages_sampled_from_oem_distribution',
            'bench_mode': '2',
            'nr_bursts': '1',
            'msg_per_burst': '500',
            'burst_interval_ms': '0',
            'metric_prefix': 'msg_distr_mode_',
        },
        {
        'testcase_name': 'collect_metrics_on_burst_of_group_messages',
            'bench_mode': '3',
            'nr_bursts': '1',
            'msg_per_burst': '500',
            'burst_interval_ms': '0',
            'metric_prefix': 'msg_group_mode_',
        },
    )
    def test_someip_benchmark(self, bench_mode, nr_bursts, msg_per_burst, burst_interval_ms, metric_prefix):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )
        self._configure_bench(bench_mode, nr_bursts,
                              msg_per_burst, burst_interval_ms)

        self._start_someip_tester()

        self._start_sdv_bundle(
            "local-vm",
            "com.sdv.google.sample.someip.SomeIpBenchmark",
            "instance1")

        polling.wait_and_verify_expected_logs(
            self.sdv_device,
            grep_text="someip_benchmark",
            expected_result=r"Finished test \- messages received",
            assert_msg='Could not find "Finished test - messages received" in logcat'
        )
        self._export_performance_metrics(metric_prefix)

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed"
        )
        self.sdv_device.reboot_device()

    def _start_someip_tester(self):
        """Starts the sdv_vsomeip_pubsub_benchmark binary as a SOME/IP source."""

        SOMEIP_CONFIG_FILE = "/vendor/etc/vsomeip/pubsub_benchmark.json"
        SOMEIP_BASE_PATH = "/data/vendor/vsomeip/"
        SOMEIP_TESTER_COMMAND = "/vendor/bin/sdv_vsomeip_pubsub_benchmark"

        self.sdv_device.execute_shell_command_in_subprocess_log(
            f"VSOMEIP_CONFIGURATION={SOMEIP_CONFIG_FILE} VSOMEIP_BASE_PATH={SOMEIP_BASE_PATH} {SOMEIP_TESTER_COMMAND}"
        )

        polling.wait_and_verify_expected_logs(
            self.sdv_device,
            grep_text="sdv_vsomeip_pubsub_benchmark",
            expected_result=r"\(pubsub_benchmark\) is registered",
            assert_msg='Could not find "(pubsub_benchmark) is registered" in logcat'
        )

    def _export_performance_metrics(self, prefix):
        logcat_processor = self.sdv_device.advance_logcat()
        messages_time_log = logcat_processor.nth_message(
            1,
            r"someip_benchmark_sample:.*Finished test \- messages received",
            regex=True)
        first_message_time_log = logcat_processor.nth_message(
            1,
            r"someip_benchmark_sample:.*Time to First (Message|Batch):",
            regex=True)

        num_messages, duration_ms, wait_time_us = _parse_benchmark_log_lines(
            messages_time_log,
            first_message_time_log,
        )

        asserts.assert_true(
            num_messages and duration_ms and wait_time_us,
            f"Failed to extract benchmark results from log: {messages_time_log}, {first_message_time_log},")

        self.metrics.update({
            f"{prefix}message_processing-avg(µs)": duration_ms / num_messages,
            f"{prefix}roundtrip_time(µs)": wait_time_us,
        })

    def _start_sdv_bundle(self, vm, service, instance):
        return self.sdv_device.execute_shell_command_in_subprocess_log(
            f"sdv_service_bundle start {vm}:{service}/{instance}")


def _convert_time_to_microseconds(time_value, time_unit):
    time_units_to_us = {
        's': 1_000_000.0,
        'ms': 1_000.0,
        'µs': 1.0,
        'us': 1.0,
    }
    conversion_factor = time_units_to_us.get(time_unit.lower())
    if conversion_factor is not None:
        return time_value * conversion_factor
    return None


def _parse_benchmark_log_lines(
    messages_log_lines,
    batch_log_lines,
):
    num_messages = None
    duration_us = None
    wait_time_us = None

    if messages_log_lines:
        pattern_msg = r"messages received = (\d+), in ([\d.]+) *(ms|s|µs|us)"
        match_msg = re.search(pattern_msg, messages_log_lines)
        if match_msg:
            try:
                num_messages = int(match_msg.group(1))
                time_value = float(match_msg.group(2))
                time_unit = match_msg.group(3)
                duration_us = _convert_time_to_microseconds(
                    time_value, time_unit)
                if duration_us is None:
                    num_messages = None
            except (ValueError, IndexError):
                pass

    if batch_log_lines:
        pattern_batch = r"Time to First (Message|Batch): ([\d.]+) *(ms|s|µs|us)"
        match_batch = re.search(pattern_batch, batch_log_lines)
        if match_batch:
            try:
                time_value = float(match_batch.group(2))
                time_unit = match_batch.group(3)
                wait_time_us = _convert_time_to_microseconds(
                    time_value, time_unit)
            except (ValueError, IndexError):
                pass

    return num_messages, duration_us, wait_time_us


if __name__ == "__main__":
    sdv_test_runner.run()
