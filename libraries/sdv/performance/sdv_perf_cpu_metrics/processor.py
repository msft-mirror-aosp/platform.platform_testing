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

import logging
from typing import Any, Dict, List, Optional, Tuple

from perfetto.common.query_result_iterator import QueryResultIterator
from sdv_perf_cpu_metrics import aggregator
from sdv_perf_cpu_metrics import common_query
from sdv_perf_cpu_metrics import single_vm_query
from sdv_perf_cpu_metrics import multi_vm_cpu_query
from sdv_perf_cpu_metrics.aggregator import Aggregate
from sdv_perfetto.perfetto_trace_processor import PerfettoTraceProcessor


class CpuMetricsProcessor:
    """
    Class for processing CPU metrics from a Perfetto trace.
    """
    _NANOSECONDS_PER_SECOND = 1e9

    def __init__(self, trace_processor: PerfettoTraceProcessor):
        self.perfetto_trace_processor = trace_processor

    def _query_metrics(
            self,
            busy_sched_with_intervals_query: str,
            metric_calculation_query: str,
            resolution: float = 1.0,
            ts_start: Optional[int] = None,
            ts_end: Optional[int] = None,
            vm_id: Optional[int] = None
    ) -> QueryResultIterator:
        """
        Queries metrics with the given metric calculation query.
        Args:
            metric_calculation_query: The Perfetto SQL query to calculate the metrics.
            resolution: The duration in seconds for each interval over which the load is calculated.
                Larger values result in faster queries (less intervals), but less accurate results.
            ts_start: The start timestamp of the selected time range in nanoseconds.
                'None' for the start of the trace file.
            ts_end: The end timestamp of the selected time range in nanoseconds.
                'None' for the end of the trace file.
            vm_id: The id of the VM, starting at 0. 'None' for single-VM use-case.
        Returns:
            The query result iterator.
        """
        ts_start = self.perfetto_trace_processor.get_trace_start_timestamp() if ts_start is None else ts_start
        ts_end = self.perfetto_trace_processor.get_trace_end_timestamp() if ts_end is None else ts_end
        logging.info(f"Timestamp range is [{ts_start}, {ts_end}].")
        if vm_id is None:
            perfetto_sql_query = single_vm_query.SINGLE_VM_QUERY_BASE_TEMPLATE.format(
                interval_size_ns=resolution*self._NANOSECONDS_PER_SECOND,
                ts_start=ts_start, ts_end=ts_end,
                busy_sched_with_intervals_query=busy_sched_with_intervals_query,
                metric_calculation_query=metric_calculation_query)
        else:
            perfetto_sql_query = multi_vm_cpu_query.MULTI_VM_QUERY_BASE_TEMPLATE.format(
                vm_id=vm_id,
                interval_size_ns=resolution*self._NANOSECONDS_PER_SECOND,
                ts_start=ts_start, ts_end=ts_end,
                busy_sched_with_intervals_query=busy_sched_with_intervals_query,
                metric_calculation_query=metric_calculation_query)
        logging.info(
            f"Full Perfetto SQL query for single-vm cpu utilization computation: \n\n\n{perfetto_sql_query}")
        return self.perfetto_trace_processor.query(perfetto_sql_query)

    def _compute_overall_utilization(
            self,
            busy_sched_with_intervals_query: str,
            metric_calculation_query: str,
            aggregator_keys: List[str],
            vm_id: Optional[int] = None,
            resolution: float = 1.0,
            ts_start: Optional[int] = None,
            ts_end: Optional[int] = None,
            aggregates: List[Aggregate] = [Aggregate.MIN, Aggregate.MEAN, Aggregate.MAX],
    ) -> Tuple[Dict[str, List[float]], Dict[Aggregate, float]]:
        try:
            ts_query_iterator = self._query_metrics(
                    busy_sched_with_intervals_query=busy_sched_with_intervals_query,
                    metric_calculation_query=metric_calculation_query,
                    resolution=resolution,
                    ts_start=ts_start,
                    ts_end=ts_end,
                    vm_id=vm_id)
            ts_data = aggregator.query_iterator_to_dict(ts_query_iterator, aggregator_keys)
            aggregated_metrics = aggregator.compute_aggregated_metrics(
                    ts_data, 'overall_cpu_perc',
                    aggregates,
                    key_suffix=None if vm_id is None else f'vm_{vm_id}')
            return (ts_data, aggregated_metrics)
        except Exception as err:
            raise RuntimeError(f"Failed to compute overall CPU utilization, got exception: {err}")

    def _compute_per_cpu_utilization(
            self,
            busy_sched_with_intervals_query: str,
            metric_calculation_query: str,
            aggregator_keys: List[str],
            vm_id: Optional[int] = None,
            resolution: float = 1.0,
            ts_start: Optional[int] = None,
            ts_end: Optional[int] = None,
            aggregates: List[Aggregate] = [Aggregate.MIN, Aggregate.MEAN, Aggregate.MAX],
    ) -> Tuple[Dict[str, Dict[str, List[Any]]], Dict[Aggregate, float]]:
        try:
            ts_query_iterator = self._query_metrics(
                    busy_sched_with_intervals_query=busy_sched_with_intervals_query,
                    metric_calculation_query=metric_calculation_query,
                    resolution=resolution,
                    ts_start=ts_start,
                    ts_end=ts_end,
                    vm_id=vm_id)
            ts_data = aggregator.query_iterator_to_dict_split_by_value(
                    'cpu', ts_query_iterator, aggregator_keys)
            aggregated_metrics = aggregator.compute_aggregated_metrics(
                    ts_data, 'overall_cpu_perc',
                    aggregates,
                    key_suffix=None if vm_id is None else f'vm_{vm_id}')
            return (ts_data, aggregated_metrics)
        except Exception as err:
            raise RuntimeError(f"Failed to compute per-CPU utilization, got exception: {err}")

    def compute_overall_utilization(
            self,
            vm_id: Optional[int] = None,
            resolution: float = 1.0,
            ts_start: Optional[int] = None,
            ts_end: Optional[int] = None,
            aggregates: List[Aggregate] = [Aggregate.MIN, Aggregate.MEAN, Aggregate.MAX]
    ) -> Tuple[Dict[str, List[float]], Dict[Aggregate, float]]:
        """
        Computes the overall CPU utilization as a percentage for a specific VM in the trace.
        Args:
            vm_id: The id of the VM, starting at 0. 'None' for single-VM use-case.
            resolution: The duration in seconds for each interval over which the load is calculated.
                Larger values result in faster queries (less intervals), but less accurate results.
            ts_start: The start timestamp of the selected time range in nanoseconds.
                'None' for the start of the trace file.
            ts_end: The end timestamp of the selected time range in nanoseconds.
                'None' for the end of the trace file.
            aggregates: List of 'Aggregate' values, such as MIN, MAX. The 'overall_cpu_perc'
                column of the query result will be aggregated according to the specified values.
        Returns:
            - Unmodified query result as a dictionary where each column is represented by a key that
            maps to a list that represents the timeseries of that respective column.
            - The aggregated metrics as a dictionary where each aggregate value type (MIN, MAX, etc.) maps
            to the resulting value. Key is of format 'overall_cpu_perc_(min|max|mean|stdev)_vm_{X}'
            for multi-VM traces and for a single VM trace the 'vm_{X}' suffix is dropped.
        """
        return self._compute_overall_utilization(
            vm_id=vm_id,
            resolution=resolution,
            ts_start=ts_start,
            ts_end=ts_end,
            aggregates=aggregates,
            busy_sched_with_intervals_query=common_query.BUSY_SCHED_WITH_INTERVALS_QUERY,
            metric_calculation_query=common_query.CALCULATION_QUERY_OVERALL_UTILIZATION,
            aggregator_keys=aggregator.KEYS)

    def compute_overall_utilization_of_process(
            self,
            process_name: str,
            vm_id: Optional[int] = None,
            resolution: float = 1.0,
            ts_start: Optional[int] = None,
            ts_end: Optional[int] = None,
            aggregates: List[Aggregate] = [Aggregate.MIN, Aggregate.MEAN, Aggregate.MAX]
    ) -> Tuple[Dict[str, Dict[str, List[Any]]], Dict[Aggregate, float]]:
        """
        Computes the overall CPU utilization of a process as a percentage for a specific VM in the trace.
        Args:
            process_name: The name of the process to compute the overall utilization for.
            vm_id: The id of the VM, starting at 0.
            resolution: The duration in seconds for each interval over which the load is calculated.
                Larger values result in faster queries (less intervals), but less accurate results.
            ts_start: The start timestamp of the selected time range in nanoseconds.
                'None' for the start of the trace file.
            ts_end: The end timestamp of the selected time range in nanoseconds.
                'None' for the end of the trace file.
            aggregates: List of 'Aggregate' values, such as MIN, MAX. The 'overall_cpu_util_perc'
                column of the query result will be aggregated according to the specified values.
        Returns:
            - Unmodified query result as a dictionary where each column is represented by a key that
            maps to a list that represents the timeseries of that respective column.
            - The aggregated metrics as a dictionary where each aggregate value type (MIN, MAX, etc.) maps
            to the resulting value. Key is of format 'overall_cpu_perc_(min|max|mean|stdev)_vm_{X}'
            for multi-VM traces and for a single VM trace the 'vm_{X}' suffix is dropped.
        """
        # TODO(b/465647296): Throw an exception if the process is not found in the trace.
        return self._compute_overall_utilization(
            vm_id=vm_id,
            resolution=resolution,
            ts_start=ts_start,
            ts_end=ts_end,
            aggregates=aggregates,
            busy_sched_with_intervals_query=common_query.BUSY_SCHED_WITH_INTERVALS_OF_PROCESS_QUERY_TEMPLATE.format(process_name=process_name),
            metric_calculation_query=common_query.CALCULATION_QUERY_OVERALL_UTILIZATION,
            aggregator_keys=aggregator.KEYS)

    def compute_per_cpu_utilization(
            self,
            vm_id: Optional[int] = None,
            resolution: float = 1.0,
            ts_start: Optional[int] = None,
            ts_end: Optional[int] = None,
            aggregates: List[Aggregate] = [Aggregate.MIN, Aggregate.MEAN, Aggregate.MAX]
    ) -> Tuple[Dict[str, Dict[str, List[Any]]], Dict[Aggregate, float]]:
        """
        Computes the overall CPU utilization as a percentage for a specific VM in the trace.
        Args:
            vm_id: The id of the VM, starting at 0. 'None' for single-VM use-case.
            resolution: The duration in seconds for each interval over which the load is calculated.
                Larger values result in faster queries (less intervals), but less accurate results.
            ts_start: The start timestamp of the selected time range in nanoseconds.
                'None' for the start of the trace file.
            ts_end: The end timestamp of the selected time range in nanoseconds.
                'None' for the end of the trace file.
            aggregates: List of 'Aggregate' values, such as MIN, MAX. The 'overall_cpu_perc'
                column of the query result will be aggregated according to the specified values.
        Returns:
            - Unmodified query result as a dictionary where each column is represented by a key that
            maps to a list that represents the timeseries of that respective column.
            - The aggregated metrics as a dictionary where each aggregate value type (MIN, MAX, etc.) maps
            to the resulting value. Key is of format 'overall_cpu_perc_cpu_X_(min|max|mean|stdev)_vm_{X}'
            for multi-VM traces and for a single VM trace the 'vm_{X}' suffix is dropped.
        """
        return self._compute_per_cpu_utilization(
            vm_id=vm_id,
            resolution=resolution,
            ts_start=ts_start,
            ts_end=ts_end,
            aggregates=aggregates,
            busy_sched_with_intervals_query=common_query.BUSY_SCHED_WITH_INTERVALS_QUERY,
            metric_calculation_query=common_query.CALCULATION_QUERY_PER_CPU_UTILIZATION,
            aggregator_keys=aggregator.PER_CPU_KEYS)

    def compute_per_cpu_utilization_of_process(
            self,
            process_name: str,
            vm_id: Optional[int] = None,
            resolution: float = 1.0,
            ts_start: Optional[int] = None,
            ts_end: Optional[int] = None,
            aggregates: List[Aggregate] = [Aggregate.MIN, Aggregate.MEAN, Aggregate.MAX]
    ) -> Tuple[Dict[str, Dict[str, List[Any]]], Dict[Aggregate, float]]:
        """
        Computes the per-CPU utilization as a percentage for a specific VM in the trace.
        Args:
            process_name: The name of the process to compute the per-CPU utilization for.
            vm_id: The id of the VM, starting at 0.
            resolution: The duration in seconds for each interval over which the load is calculated.
                Larger values result in faster queries (less intervals), but less accurate results.
            ts_start: The start timestamp of the selected time range in nanoseconds.
                'None' for the start of the trace file.
            ts_end: The end timestamp of the selected time range in nanoseconds.
                'None' for the end of the trace file.
            aggregates: List of 'Aggregate' values, such as MIN, MAX. The 'overall_cpu_perc'
                column of the query result will be aggregated according to the specified values.
        Returns:
            - Unmodified query result as a dictionary where each column is represented by a key that
            maps to a list that represents the timeseries of that respective column.
            - The aggregated metrics as a dictionary where each aggregate value type (MIN, MAX, etc.) maps
            to the resulting value. Key is of format 'overall_cpu_perc_cpu_X_(min|max|mean|stdev)_vm_{X}'
            for multi-VM traces and for a single VM trace the 'vm_{X}' suffix is dropped.
        """
        # TODO(b/465647296): Throw an exception if the process is not found in the trace.
        return self._compute_per_cpu_utilization(
            vm_id=vm_id,
            resolution=resolution,
            ts_start=ts_start,
            ts_end=ts_end,
            aggregates=aggregates,
            busy_sched_with_intervals_query=common_query.BUSY_SCHED_WITH_INTERVALS_OF_PROCESS_QUERY_TEMPLATE.format(process_name=process_name),
            metric_calculation_query=common_query.CALCULATION_QUERY_PER_CPU_UTILIZATION,
            aggregator_keys=aggregator.PER_CPU_KEYS)
