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

import unittest
from unittest import mock

from sdv_perf_cpu_metrics import processor
from sdv_perf_cpu_metrics.aggregator import Aggregate
from sdv_perfetto import perfetto_trace_processor


class ProcessorTest(unittest.TestCase):
    ONE_VM_TRACE_FILE = "test_resources/one_vm_sdv_core_cf.perfetto-trace"
    MULTI_VM_TRACE_FILE = "test_resources/two_vms_sdv_core_cf.perfetto-trace"

    def load_trace_processor(self, trace_file: str):
        """Loads the trace processor with the given trace file."""
        self.trace_processor = perfetto_trace_processor.PerfettoTraceProcessor(trace_file, timeout=30)
        # Initialize the CPU metrics processor with the trace processor.
        self.cpu_metrics_processor = processor.CpuMetricsProcessor(self.trace_processor)

    def test_compute_overall_utilization_exception(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        mock_trace_processor = mock.MagicMock(spec=perfetto_trace_processor.PerfettoTraceProcessor)
        # Simulate an error during the query process within compute_overall_utilization
        mock_trace_processor.query.side_effect = RuntimeError("Mocked trace processor error")

        cpu_metrics_processor = processor.CpuMetricsProcessor(mock_trace_processor)

        with self.assertRaisesRegex(RuntimeError, "Mocked trace processor error"):
            cpu_metrics_processor.compute_overall_utilization(
                vm_id=None,
                aggregates=[Aggregate.MEAN])

    def test_compute_overall_utilization_with_default_time_range(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        # Test with a specific timestamp range
        ts_data, overall_cpu_perc_metrics = self.cpu_metrics_processor.compute_overall_utilization(
            vm_id=None,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for overall_cpu_perc_metrics
        # Expectatios are based on the trace file in the resource directory.
        self.assertGreater(len(ts_data['interval_id']), 200)
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_min'], 0, "overall_cpu_perc_min should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_min'], 100, "overall_cpu_perc_min should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_max'], 0, "overall_cpu_perc_max should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_max'], 100, "overall_cpu_perc_max should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_mean'], 0, "overall_cpu_perc_mean should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_mean'], 100, "overall_cpu_perc_mean should be less than 100")


    def test_compute_overall_utilization_with_selected_timestamp_range(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        # Test with a specific timestamp range
        ts_data, overall_cpu_perc_metrics = self.cpu_metrics_processor.compute_overall_utilization(
            vm_id=None,
            ts_start=self.trace_processor.get_trace_start_timestamp() + 100*1e9,
            ts_end=self.trace_processor.get_trace_end_timestamp() - 100*1e9,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for overall_cpu_perc_metrics
        # Expectatios are based on the trace file in the resource directory.
        self.assertLess(len(ts_data['interval_id']), 50)
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_min'], 0, "overall_cpu_perc_min should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_min'], 100, "overall_cpu_perc_min should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_max'], 0, "overall_cpu_perc_max should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_max'], 100, "overall_cpu_perc_max should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_mean'], 0, "overall_cpu_perc_mean should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_mean'], 100, "overall_cpu_perc_mean should be less than 100")

    def test_compute_overall_utilization_with_only_start_timestamp(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        # Test with a custom resolution
        ts_data, overall_cpu_perc_metrics = self.cpu_metrics_processor.compute_overall_utilization(
            vm_id=None,
            ts_start=self.trace_processor.get_trace_start_timestamp() + 100*1e9,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for overall_cpu_perc_metrics
        # Expectatios are based on the trace file in the resource directory.
        self.assertLess(len(ts_data['interval_id']), 200)
        self.assertGreater(len(ts_data['interval_id']), 50)
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_min'], 0, "overall_cpu_perc_min should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_min'], 100, "overall_cpu_perc_min should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_max'], 0, "overall_cpu_perc_max should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_max'], 100, "overall_cpu_perc_max should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_mean'], 0, "overall_cpu_perc_mean should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_mean'], 100, "overall_cpu_perc_mean should be less than 100")

    def test_compute_overall_utilization_with_only_end_timestamp(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        # Test with a custom resolution
        ts_data, overall_cpu_perc_metrics = self.cpu_metrics_processor.compute_overall_utilization(
            vm_id=None,
            ts_start=None,
            ts_end=self.trace_processor.get_trace_end_timestamp() - 100*1e9,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for overall_cpu_perc_metrics
        # Expectatios are based on the trace file in the resource directory.
        self.assertLess(len(ts_data['interval_id']), 200)
        self.assertGreater(len(ts_data['interval_id']), 50)
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_min'], 0, "overall_cpu_perc_min should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_min'], 100, "overall_cpu_perc_min should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_max'], 0, "overall_cpu_perc_max should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_max'], 100, "overall_cpu_perc_max should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_mean'], 0, "overall_cpu_perc_mean should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_mean'], 100, "overall_cpu_perc_mean should be less than 100")

    def test_compute_per_cpu_utilization_with_default_time_range(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        # Test with a specific timestamp range
        _, per_cpu_perc_metrics = self.cpu_metrics_processor.compute_per_cpu_utilization(
            vm_id=None,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_min'], 0, "overall_cpu_perc_cpu_0_min should be greater than 0")
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_min'], 100, "overall_cpu_perc_cpu_0_min should be less than 100")
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_max'], 0, "overall_cpu_perc_cpu_0_max should be greater than 0")
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_max'], 100, "overall_cpu_perc_cpu_0_max should be less than 100")
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_mean'], 0, "overall_cpu_perc_cpu_0_mean should be greater than 0")
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_mean'], 100, "overall_cpu_perc_cpu_0_mean should be less than 100")
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_1_min'], 0, "overall_cpu_perc_cpu_1_min should be greater than 0")
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_1_min'], 100, "overall_cpu_perc_cpu_1_min should be less than 100")
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_1_max'], 0, "overall_cpu_perc_cpu_1_max should be greater than 0")
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_1_max'], 100, "overall_cpu_perc_cpu_1_max should be less than 100")
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_1_mean'], 0, "overall_cpu_perc_cpu_1_mean should be greater than 0")
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_1_mean'], 100, "overall_cpu_perc_cpu_1_mean should be less than 100")
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_2_min'], 0, "overall_cpu_perc_cpu_2_min should be greater than 0")
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_2_min'], 100, "overall_cpu_perc_cpu_2_min should be less than 100")
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_2_max'], 0, "overall_cpu_perc_cpu_2_max should be greater than 0")
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_2_max'], 100, "overall_cpu_perc_cpu_2_max should be less than 100")
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_2_mean'], 0, "overall_cpu_perc_cpu_2_mean should be greater than 0")
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_2_mean'], 100, "overall_cpu_perc_cpu_2_mean should be less than 100")

    def test_compute_per_cpu_utilization_exception(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        mock_trace_processor = mock.MagicMock(spec=perfetto_trace_processor.PerfettoTraceProcessor)
        # Simulate an error during the query process within compute_overall_utilization
        mock_trace_processor.query.side_effect = RuntimeError("Mocked trace processor error")

        cpu_metrics_processor = processor.CpuMetricsProcessor(mock_trace_processor)

        with self.assertRaisesRegex(RuntimeError, "Mocked trace processor error"):
            cpu_metrics_processor.compute_per_cpu_utilization(
                vm_id=None,
                aggregates=[Aggregate.MEAN])

    def test_compute_per_cpu_utilization_with_selected_timestamp_range(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        # Test with a specific timestamp range
        _, per_cpu_perc_metrics = self.cpu_metrics_processor.compute_per_cpu_utilization(
            vm_id=None,
            ts_start=self.trace_processor.get_trace_start_timestamp() + 100 * 1e9,
            ts_end=self.trace_processor.get_trace_end_timestamp() - 100 * 1e9,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_min'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_min'], 100)
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_max'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_max'], 100)
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_mean'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_mean'], 100)

    def test_compute_per_cpu_utilization_with_only_start_timestamp(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        # Test with only a start timestamp
        _, per_cpu_perc_metrics = self.cpu_metrics_processor.compute_per_cpu_utilization(
            vm_id=None,
            ts_start=self.trace_processor.get_trace_start_timestamp() + 100 * 1e9,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_min'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_min'], 100)
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_max'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_max'], 100)
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_mean'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_mean'], 100)

    def test_compute_per_cpu_utilization_with_only_end_timestamp(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        # Test with only an end timestamp
        _, per_cpu_perc_metrics = self.cpu_metrics_processor.compute_per_cpu_utilization(
            vm_id=None,
            ts_start=None,
            ts_end=self.trace_processor.get_trace_end_timestamp() - 100 * 1e9,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_min'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_min'], 100)
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_max'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_max'], 100)
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_mean'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_mean'], 100)

    def test_compute_overall_utilization_of_process_exception(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        mock_trace_processor = mock.MagicMock(
            spec=perfetto_trace_processor.PerfettoTraceProcessor)
        # Simulate an error during the query process within compute_overall_utilization
        mock_trace_processor.query.side_effect = RuntimeError(
            "Mocked trace processor error")

        cpu_metrics_processor = processor.CpuMetricsProcessor(
            mock_trace_processor)

        with self.assertRaisesRegex(RuntimeError, "Mocked trace processor error"):
            # Testing private method directly to test exception path as public method path is not implemented.
            cpu_metrics_processor._compute_overall_utilization(
                vm_id=None,
                aggregates=[Aggregate.MEAN],
                busy_sched_with_intervals_query="",
                metric_calculation_query="",
                aggregator_keys=[])

    def test_compute_per_cpu_utilization_of_process_exception(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        mock_trace_processor = mock.MagicMock(
            spec=perfetto_trace_processor.PerfettoTraceProcessor)
        # Simulate an error during the query process within compute_overall_utilization
        mock_trace_processor.query.side_effect = RuntimeError(
            "Mocked trace processor error")

        cpu_metrics_processor = processor.CpuMetricsProcessor(
            mock_trace_processor)

        with self.assertRaisesRegex(RuntimeError, "Mocked trace processor error"):
            # Testing private method directly to test exception path as public method path is not implemented.
            cpu_metrics_processor._compute_per_cpu_utilization(
                vm_id=None,
                aggregates=[Aggregate.MEAN],
                busy_sched_with_intervals_query="",
                metric_calculation_query="",
                aggregator_keys=[])

    def test_compute_overall_utilization_of_process_with_default_time_range(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        # This test confirms that the multi-VM (i.e., vm_id is not None) code path
        # is not implemented and returns empty results.
        _, overall_cpu_perc_metrics_of_process = self.cpu_metrics_processor.compute_overall_utilization_of_process(
            process_name="logcat",
            vm_id=None,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for overall_cpu_perc_metrics_of_process
        # Expectatios are based on the trace file in the resource directory.
        self.assertEqual(overall_cpu_perc_metrics_of_process['overall_cpu_perc_min'], 0, "overall_cpu_perc_min should be 0")
        self.assertGreater(overall_cpu_perc_metrics_of_process['overall_cpu_perc_max'], 0, "overall_cpu_perc_max should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics_of_process['overall_cpu_perc_max'], 100, "overall_cpu_perc_max should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics_of_process['overall_cpu_perc_mean'], 0, "overall_cpu_perc_mean should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics_of_process['overall_cpu_perc_mean'], 100, "overall_cpu_perc_mean should be less than 100")

    def test_compute_overall_utilization_of_process_with_selected_timestamp_range(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        # Test with a specific timestamp range
        _, overall_cpu_perc_metrics_of_process = self.cpu_metrics_processor.compute_overall_utilization_of_process(
            process_name="logcat",
            vm_id=None,
            ts_start=self.trace_processor.get_trace_start_timestamp() + 100*1e9,
            ts_end=self.trace_processor.get_trace_end_timestamp() - 100*1e9,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])
        # Assertions for overall_cpu_perc_metrics_of_process
        # Expectatios are based on the trace file in the resource directory.
        self.assertGreater(overall_cpu_perc_metrics_of_process['overall_cpu_perc_max'], 0, "overall_cpu_perc_max should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics_of_process['overall_cpu_perc_max'], 100, "overall_cpu_perc_max should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics_of_process['overall_cpu_perc_mean'], 0, "overall_cpu_perc_mean should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics_of_process['overall_cpu_perc_mean'], 100, "overall_cpu_perc_mean should be less than 100")

    def test_compute_per_cpu_utilization_of_process_with_default_time_range(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        # Test with a specific timestamp range
        _, per_cpu_perc_metrics_of_process = self.cpu_metrics_processor.compute_per_cpu_utilization_of_process(
            process_name="logcat",
            vm_id=None,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for per_cpu_perc_metrics
        # Expectatios are based on the trace file in the resource directory.
        self.assertEqual(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_0_min'], 0, "overall_cpu_perc_cpu_0_min should be 0")
        self.assertGreater(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_0_max'], 0, "overall_cpu_perc_cpu_0_max should be greater than 0")
        self.assertLess(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_0_max'], 100, "overall_cpu_perc_cpu_0_max should be less than 100")
        self.assertGreater(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_0_mean'], 0, "overall_cpu_perc_cpu_0_mean should be greater than 0")
        self.assertLess(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_0_mean'], 100, "overall_cpu_perc_cpu_0_mean should be less than 100")
        self.assertEqual(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_1_min'], 0, "overall_cpu_perc_cpu_1_min should be 0")
        self.assertEqual(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_2_min'], 0, "overall_cpu_perc_cpu_2_min should be 0")
        self.assertEqual(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_3_min'], 0, "overall_cpu_perc_cpu_3_min should be 0")

    def test_compute_per_cpu_utilization_of_process_with_invalid_process_name(self):
        self.load_trace_processor(self.ONE_VM_TRACE_FILE)
        # TODO(b/465647296): Update this test once the exception is implemented.
        # Test with a specific timestamp range
        _, per_cpu_perc_metrics_of_process = self.cpu_metrics_processor.compute_per_cpu_utilization_of_process(
            process_name="invalid_process_name",
            vm_id=None,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for per_cpu_perc_metrics_of_process
        # Expectatios are based on the trace file in the resource directory.
        # Since the process name is invalid, all metrics should be 0.
        self.assertEqual(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_0_min'], 0, "overall_cpu_perc_cpu_0_min should be 0")
        self.assertEqual(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_0_max'], 0, "overall_cpu_perc_cpu_0_max should be 0")
        self.assertEqual(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_0_mean'], 0, "overall_cpu_perc_cpu_0_mean should be 0")


    def test_compute_overall_utilization_vm0(self):
        self.load_trace_processor(self.MULTI_VM_TRACE_FILE)
        # Test with a specific timestamp range
        ts_data, overall_cpu_perc_metrics = self.cpu_metrics_processor.compute_overall_utilization(
            vm_id=0,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for overall_cpu_perc_metrics
        # Expectatios are based on the trace file in the resource directory.
        self.assertGreater(len(ts_data['interval_id']), 200)
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_min_vm_0'], 0, "overall_cpu_perc_min should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_min_vm_0'], 100, "overall_cpu_perc_min should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_max_vm_0'], 0, "overall_cpu_perc_max should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_max_vm_0'], 100, "overall_cpu_perc_max should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_mean_vm_0'], 0, "overall_cpu_perc_mean should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_mean_vm_0'], 100, "overall_cpu_perc_mean should be less than 100")

    def test_compute_overall_utilization_vm1(self):
        self.load_trace_processor(self.MULTI_VM_TRACE_FILE)
        # Test with a specific timestamp range
        ts_data, overall_cpu_perc_metrics = self.cpu_metrics_processor.compute_overall_utilization(
            vm_id=1,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for overall_cpu_perc_metrics
        # Expectatios are based on the trace file in the resource directory.
        self.assertGreater(len(ts_data['interval_id']), 200)
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_min_vm_1'], 0, "overall_cpu_perc_min should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_min_vm_1'], 100, "overall_cpu_perc_min should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_max_vm_1'], 0, "overall_cpu_perc_max should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_max_vm_1'], 100, "overall_cpu_perc_max should be less than 100")
        self.assertGreater(overall_cpu_perc_metrics['overall_cpu_perc_mean_vm_1'], 0, "overall_cpu_perc_mean should be greater than 0")
        self.assertLess(overall_cpu_perc_metrics['overall_cpu_perc_mean_vm_1'], 100, "overall_cpu_perc_mean should be less than 100")

    def test_compute_per_cpu_utilization_vm0(self):
        self.load_trace_processor(self.MULTI_VM_TRACE_FILE)
        # Test with a specific timestamp range
        _, per_cpu_perc_metrics = self.cpu_metrics_processor.compute_per_cpu_utilization(
            vm_id=0,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_min_vm_0'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_min_vm_0'], 100)
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_max_vm_0'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_max_vm_0'], 100)
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_mean_vm_0'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_0_mean_vm_0'], 100)

    def test_compute_per_cpu_utilization_vm1(self):
        self.load_trace_processor(self.MULTI_VM_TRACE_FILE)
        # Test with a specific timestamp range
        _, per_cpu_perc_metrics = self.cpu_metrics_processor.compute_per_cpu_utilization(
            vm_id=1,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_4096_min_vm_1'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_4096_min_vm_1'], 100)
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_4096_max_vm_1'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_4096_max_vm_1'], 100)
        self.assertGreater(per_cpu_perc_metrics['overall_cpu_perc_cpu_4096_mean_vm_1'], 0)
        self.assertLess(per_cpu_perc_metrics['overall_cpu_perc_cpu_4096_mean_vm_1'], 100)

    def test_compute_overall_utilization_of_process_vm0(self):
        self.load_trace_processor(self.MULTI_VM_TRACE_FILE)
        _, overall_cpu_perc_metrics_of_process = self.cpu_metrics_processor.compute_overall_utilization_of_process(
            process_name="logcat",
            vm_id=0,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for overall_cpu_perc_metrics_of_process
        # Expectatios are based on the trace file in the resource directory.
        self.assertGreater(overall_cpu_perc_metrics_of_process['overall_cpu_perc_max_vm_0'], 0)
        self.assertLess(overall_cpu_perc_metrics_of_process['overall_cpu_perc_max_vm_0'], 100)
        self.assertGreater(overall_cpu_perc_metrics_of_process['overall_cpu_perc_mean_vm_0'], 0)
        self.assertLess(overall_cpu_perc_metrics_of_process['overall_cpu_perc_mean_vm_0'], 100)

    def test_compute_overall_utilization_of_process_vm1(self):
        self.load_trace_processor(self.MULTI_VM_TRACE_FILE)
        _, overall_cpu_perc_metrics_of_process = self.cpu_metrics_processor.compute_overall_utilization_of_process(
            process_name="logcat",
            vm_id=1,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for overall_cpu_perc_metrics_of_process
        # Expectatios are based on the trace file in the resource directory.
        self.assertGreater(overall_cpu_perc_metrics_of_process['overall_cpu_perc_max_vm_1'], 0)
        self.assertLess(overall_cpu_perc_metrics_of_process['overall_cpu_perc_max_vm_1'], 100)
        self.assertGreater(overall_cpu_perc_metrics_of_process['overall_cpu_perc_mean_vm_1'], 0)
        self.assertLess(overall_cpu_perc_metrics_of_process['overall_cpu_perc_mean_vm_1'], 100)

    def test_compute_per_cpu_utilization_of_process_vm0(self):
        self.load_trace_processor(self.MULTI_VM_TRACE_FILE)
        # Test with a specific timestamp range
        _, per_cpu_perc_metrics_of_process = self.cpu_metrics_processor.compute_per_cpu_utilization_of_process(
            process_name="logcat",
            vm_id=0,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for per_cpu_perc_metrics
        # Expectatios are based on the trace file in the resource directory.
        self.assertGreater(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_0_max_vm_0'], 0)
        self.assertLess(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_0_max_vm_0'], 100)
        self.assertGreater(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_0_mean_vm_0'], 0)
        self.assertLess(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_0_mean_vm_0'], 100)

    def test_compute_per_cpu_utilization_of_process_vm1(self):
        self.load_trace_processor(self.MULTI_VM_TRACE_FILE)
        # Test with a specific timestamp range
        _, per_cpu_perc_metrics_of_process = self.cpu_metrics_processor.compute_per_cpu_utilization_of_process(
            process_name="logcat",
            vm_id=1,
            ts_start=None,
            ts_end=None,
            aggregates=[Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN])

        # Assertions for per_cpu_perc_metrics
        # Expectatios are based on the trace file in the resource directory.
        self.assertGreater(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_4096_max_vm_1'], 0)
        self.assertLess(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_4096_max_vm_1'], 100)
        self.assertGreater(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_4096_mean_vm_1'], 0)
        self.assertLess(per_cpu_perc_metrics_of_process['overall_cpu_perc_cpu_4096_mean_vm_1'], 100)


if __name__ == "__main__":
    unittest.main()