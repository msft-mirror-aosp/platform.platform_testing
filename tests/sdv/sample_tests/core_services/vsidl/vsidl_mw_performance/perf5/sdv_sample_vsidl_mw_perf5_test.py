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

"""
SDV sample 'VSIDL Middleware Performance 5' test.

This collects Perfetto traces for measuring publishing
and observing messages using middleware.
"""

import logging
import time
import traceback

from sdv_perfetto import perfetto_collector
from sdv_perfetto import perfetto_trace_processor
from sdv_perf_cpu_metrics import processor
from sdv_perf_cpu_metrics.aggregator import Aggregate
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

class SdvSampleVsidlMwPerf5Test(sdv_base_test.SdvBaseTestClass):

    QUERY_PUBLISH = """
    SELECT
        MIN(CASE WHEN slice.name = 'MW::Publisher<M>::publish' THEN slice.dur ELSE NULL END) AS min_publisher_duration,
        AVG(CASE WHEN slice.name = 'MW::Publisher<M>::publish' THEN slice.dur ELSE NULL END) AS avg_publisher_duration,
        MAX(CASE WHEN slice.name = 'MW::Publisher<M>::publish' THEN slice.dur ELSE NULL END) AS max_publisher_duration
    FROM slice
    JOIN thread_track on slice.track_id = thread_track.id
    JOIN thread using (utid)
    JOIN process using (upid)
    WHERE process.id IN (select id from process WHERE name = 'PerfFirst:instance' UNION select id from process WHERE name = 'PerfSecond:instance') and (slice.name like '%MW::observe%' or slice.name like '%MW::Publisher%');
    """

    QUERY_OBSERVE = """
    SELECT
        MIN(CASE WHEN slice.name = 'MW::observe<M>: Map Message Stream' THEN slice.dur ELSE NULL END) AS min_observer_duration,
        AVG(CASE WHEN slice.name = 'MW::observe<M>: Map Message Stream' THEN slice.dur ELSE NULL END) AS avg_observer_duration,
        MAX(CASE WHEN slice.name = 'MW::observe<M>: Map Message Stream' THEN slice.dur ELSE NULL END) AS max_observer_duration
    FROM slice
    JOIN thread_track on slice.track_id = thread_track.id
    JOIN thread using (utid)
    JOIN process using (upid)
    WHERE process.id IN (select id from process WHERE name = 'PerfFirst:instance' UNION select id from process WHERE name = 'PerfSecond:instance') and (slice.name like '%MW::observe%' or slice.name like '%MW::Publisher%');
    """

    QUERY_POLLING = """
    SELECT
        MIN(CASE WHEN slice.name LIKE 'MW::Subscriber<M>::read_next_messages%' THEN slice.dur ELSE NULL END) AS min_polling_duration,
        AVG(CASE WHEN slice.name LIKE 'MW::Subscriber<M>::read_next_messages%' THEN slice.dur ELSE NULL END) AS avg_polling_duration,
        MAX(CASE WHEN slice.name LIKE 'MW::Subscriber<M>::read_next_messages%' THEN slice.dur ELSE NULL END) AS max_polling_duration
    FROM slice
    JOIN thread_track on slice.track_id = thread_track.id
    JOIN thread using (utid)
    JOIN process using (upid)
    WHERE process.id IN (select id from process WHERE name = 'PerfFirst:instance');
    """

    ##################################################
    ## Setup/teardown for the test suite and tests. ##
    ##################################################

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()
        self.perfetto_collector = perfetto_collector.PerfettoCollector(
            device=self.sdv_device
        )

    def start_trace(self):
        self.perfetto_collector.start_trace()

    def setup_test(self):
        super().setup_test()
        self.sdv_device.reboot_device()
        self.sdv_device.wait_for_device_online()
        self.sdv_device.root_device()
        self.sdv_device.execute_shell_command("sdv_service_bundle start local-vm:com.sdv.google.sample.perf5.PerfFirst/instance")
        self.sdv_device.execute_shell_command("sdv_service_bundle start local-vm:com.sdv.google.sample.perf5.PerfSecond/instance")

    def teardown_test(self):
        if self.current_test_info.name == 'test_record_perf5_traces':
            self.stop_trace_reference_test()
        super().teardown_test()

    def stop_trace_reference_test(self):
        trace_file_path = self.perfetto_collector.stop_trace(tag='device1')
        perfetto_trace = perfetto_trace_processor.PerfettoTraceProcessor(trace_file_path)
        perfetto_trace.export_built_in_metrics_to_crystalball(
            metric_names=['android_cpu', 'android_mem'],
            output_dir=self.sdv_device.log_path(),
            test_name=self.get_suite_name() + '#built_in_metrics',
            omit_base_name=False,
        )
        results_publish = list(perfetto_trace.query(self.QUERY_PUBLISH))
        results_observe = list(perfetto_trace.query(self.QUERY_OBSERVE))
        results_polling = list(perfetto_trace.query(self.QUERY_POLLING))
        cpu_metrics_processor = processor.CpuMetricsProcessor(perfetto_trace)
        _, overall_cpu_perc_metrics = cpu_metrics_processor.compute_overall_utilization(
            vm_id=None,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])
        _, overall_per_cpu_metrics = cpu_metrics_processor.compute_per_cpu_utilization(
            vm_id=None,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        metrics = {}

        metrics[f'MW-Publisher-publish#min'] = results_publish[0].min_publisher_duration
        metrics[f'MW-Publisher-publish#avg'] = results_publish[0].avg_publisher_duration
        metrics[f'MW-Publisher-publish#max'] = results_publish[0].max_publisher_duration

        metrics[f'MW-observe#min'] = results_observe[0].min_observer_duration
        metrics[f'MW-observe#avg'] = results_observe[0].avg_observer_duration
        metrics[f'MW-observe#max'] = results_observe[0].max_observer_duration

        metrics[f'MW-Subscriber-read_next_messages#min'] = results_polling[0].min_polling_duration
        metrics[f'MW-Subscriber-read_next_messages#avg'] = results_polling[0].avg_polling_duration
        metrics[f'MW-Subscriber-read_next_messages#max'] = results_polling[0].max_polling_duration

        metrics[f'overall_cpu_perc_min'] = overall_cpu_perc_metrics['overall_cpu_perc_min']
        metrics[f'overall_cpu_perc_max'] = overall_cpu_perc_metrics['overall_cpu_perc_max']
        metrics[f'overall_cpu_perc_mean'] = overall_cpu_perc_metrics['overall_cpu_perc_mean']
        for key, value in overall_per_cpu_metrics.items():
            metrics[key] = value

        perfetto_trace_processor.export_to_crystalball(
            {__class__.__name__ + '#' + 'processing': metrics},
            output_dir=self.sdv_device.log_path(),
            test_name=__class__.__name__ + '#' + 'processing',
            omit_base_name=False,
        )


    def test_record_perf5_traces(self):
        logging.info(
            f'{self.get_suite_name()} :: Start Test {self.current_test_info.name}'
        )

        self.start_trace()

        # Collect 15 messages
        for i in range(15):
            logging.info(
                f'{self.get_suite_name()} :: Found message {i+1} of 15' # i+1 because counting starts at 0
            )
            self.sdv_device.clear_logcat()
            polling.wait_and_verify_expected_logs(self.sdv_device, grep_text="Read message.", logcat_args="*:F com_sdv_google_sample_perf5_PerfFirst_instance:*")

        logging.info(
            f'{self.get_suite_name()} :: End Test {self.current_test_info.name}'
        )

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
