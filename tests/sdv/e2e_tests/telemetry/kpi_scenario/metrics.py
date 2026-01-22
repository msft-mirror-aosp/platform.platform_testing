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

"""Metrics extraction utilities"""

from itertools import groupby
from typing import Any, Dict, List

import numpy as np
from sdv_perfetto import perfetto_trace_processor


PROCESS_NAME = '/vendor/bin/sdv_telemetry_service_agent'


def calculate_metrics(
    trace: perfetto_trace_processor.PerfettoTraceProcessor,
) -> Dict[str, Any]:
    cpu_mem_metrics = _calc_cpu_and_memory_metrics(trace)
    api_latency_metrics = _calc_api_latency_metrics(trace)

    metrics = dict()
    metrics.update(cpu_mem_metrics)
    metrics.update(api_latency_metrics)
    return metrics


def _calc_cpu_and_memory_metrics(
    trace: perfetto_trace_processor.PerfettoTraceProcessor,
) -> Dict[str, Any]:
    cpu_utilization = _calc_cpu_utilization(trace)
    mem_anon_stats = _calc_memory_consumption(trace, 'mem.rss.anon')
    mem_file_stats = _calc_memory_consumption(trace, 'mem.rss.file')

    return {
        'cpu_utilization(%)': cpu_utilization,
        # heap + stack memory
        'mem_rss_anon(MB)': mem_anon_stats,
        # memory used by mapped files (binary, libraries)
        'mem_rss_file(MB)': mem_file_stats,
    }


def _calc_cpu_utilization(
    trace: perfetto_trace_processor.PerfettoTraceProcessor,
):
    query = "SELECT value FROM stats WHERE name LIKE 'ftrace_cpu_now_ts_begin'"
    nr_cores = len(list(trace.query(query)))

    trace_duration = perfetto_trace_processor.traceMetrics_to_dict(
        trace.get_built_in_metrics(['trace_metadata'])
    )['trace_metadata']['trace_duration_ns']

    cpu_metrics = perfetto_trace_processor.traceMetrics_to_dict(
        trace.get_built_in_metrics(['android_cpu'])
    )
    cpu_metrics = next(
        metric
        for metric in cpu_metrics['android_cpu']['process_info']
        if metric['name'] == PROCESS_NAME
    )
    exec_time_all_cores = sum(
        per_core['metrics']['runtime_ns']
        for thread in cpu_metrics['threads']
        if 'core' in thread
        for per_core in thread['core']
    )

    return exec_time_all_cores / trace_duration / nr_cores * 100


def _calc_memory_consumption(
    trace: perfetto_trace_processor.PerfettoTraceProcessor, mem_type: str
):
    query = (
        'select c.ts, c.value, t.name as counter_name, p.name as proc_name, '
        'p.pid from counter as c left join process_counter_track as t on '
        'c.track_id = t.id left join process as p using (upid) where t.name '
        f"like '{mem_type}' and p.name like '{PROCESS_NAME}'"
    )

    mem_rss = [r.value for r in trace.query(query)]
    stats = _calc_statistics(mem_rss)

    # Convert bytes to megabytes for all stats
    return {
        key: float(round(value / 1000000.0, 2)) for key, value in stats.items()
    }


def _calc_api_latency_metrics(
    trace: perfetto_trace_processor.PerfettoTraceProcessor,
) -> Dict[str, Any]:
    """Extracts latency of Telemtry Service API calls"""

    query_add = """SELECT * FROM slice WHERE name LIKE 'add_metrics_config%'"""
    latency_add = _calc_latency_stats(trace, query_add)

    query_activate = (
        """SELECT * FROM slice WHERE name LIKE 'activate_metrics_config%'"""
    )
    latency_activate = _calc_latency_stats(trace, query_activate)

    query_deactivate = (
        """SELECT * FROM slice WHERE name LIKE 'deactivate_metrics_config%'"""
    )
    latency_deactivate = _calc_latency_stats(trace, query_deactivate)

    query_remove = (
        """SELECT * FROM slice WHERE name LIKE 'remove_metrics_config%'"""
    )
    latency_remove = _calc_latency_stats(trace, query_remove)

    return {
        'add-metrics-config(ms)': latency_add,
        'activate-metrics-config(ms)': latency_activate,
        'deactivate-metrics-config(ms)': latency_deactivate,
        'remove-metrics-config(ms)': latency_remove,
    }


def _calc_latency_stats(
    trace: perfetto_trace_processor.PerfettoTraceProcessor, query: str
) -> Dict[str, int]:
    results = trace.query(query)
    poll_durations = (
        {
            'duration': r.dur,
            'id': int(r.name.split('await_start_timestamp = ', 1)[1]),
        }
        for r in results
    )

    poll_durations_sorted = sorted(poll_durations, key=lambda x: x['id'])
    poll_durations_grouped = groupby(
        poll_durations_sorted, key=lambda x: x['id']
    )

    async_fn_durations = [
        # Sum up duration of slices with the same await_start_timestamp
        sum([poll['duration'] for poll in polls])
        for _, polls in poll_durations_grouped
    ]

    stats = _calc_statistics(async_fn_durations)

    # Convert nanoseconds to milliseconds for all stats
    return {
        key: float(round(value / 1000000.0, 2)) for key, value in stats.items()
    }


def _calc_statistics(
    values: List[int],
) -> Dict[str, float]:
    d_array = np.array(values)
    return {
        'avg': np.mean(d_array),
        'median': np.median(d_array),
        'p95': np.percentile(d_array, 95),
        'max': np.max(d_array),
    }
