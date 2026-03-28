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
import statistics

from sdv_perfetto import perfetto_trace_processor


PROCESS_NAME = '/system_ext/bin/sdv_telemetry_service_agent'


def calculate_metrics(
    trace: perfetto_trace_processor.PerfettoTraceProcessor,
) -> Dict[str, Any]:
    cpu_mem_metrics = _calc_cpu_and_memory_metrics(trace)
    api_latency_metrics = _calc_api_latency_metrics(trace)
    comms_stack_metrics = _calc_comms_stack_latency(trace)

    metrics = dict()
    metrics.update(cpu_mem_metrics)
    metrics.update(api_latency_metrics)
    metrics.update(comms_stack_metrics)
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


def _calc_comms_stack_latency(
    trace: perfetto_trace_processor.PerfettoTraceProcessor,
) -> Dict[str, Any]:
    """Extracts latency related to create_subscription and get_latest_message calls"""

    create_subscription_dt, create_subscription_rpc = _calc_create_subscription(
        trace
    )
    get_latest_message_dt, get_latest_message_rpc = _calc_get_latest_message(
        trace
    )

    return {
        'create-subscription-dt(ms)': create_subscription_dt,
        'create-subscription-rpc(ms)': create_subscription_rpc,
        'get-latest-message-dt(ms)': get_latest_message_dt,
        'get-latest-message-rpc(ms)': get_latest_message_rpc,
    }


def _calc_create_subscription(trace):
    """Returns latency statistics for create_subscription calls.

    Calculates the latency between Telemetry Service receives a message
    from a publisher with SUBSCRIPTION connection type and it is being
    received by its internal publisher representation.
    """

    # Extract create_subscription-related tracing events. DT and RPC events
    # represent the timestamp a message has been received by Telemetry Service.
    # Service Publisher events represent the timestamp the message processing
    # has been started. Each event contains a message id which is used to match
    # these two timestamps for latency calculation.
    query_dt = """SELECT * FROM slice WHERE name LIKE 'New message(s) by DT Publisher%'"""
    timestamps_dt = _extract_timestamps(trace, query_dt)

    query_rpc = """SELECT * FROM slice WHERE name LIKE 'New message by RPC Publisher%'"""
    timestamps_rpc = _extract_timestamps(trace, query_rpc)

    query_sp = """SELECT * FROM slice WHERE name LIKE 'Message processed by Service Publisher%'"""
    timestamps_sp = _extract_timestamps(trace, query_sp)

    # Match Service Publisher events with DT/RPC events and get the latency.
    dt_lat, rpc_lat = [], []
    for uuid, ts in timestamps_sp.items():
        if uuid in timestamps_dt:
            dt_lat.append(ts - timestamps_dt[uuid])
        if uuid in timestamps_rpc:
            rpc_lat.append(ts - timestamps_rpc[uuid])

    # Calc avg, median, p95 and max statistics
    dt_stats = _calc_statistics(dt_lat)
    dt_stats = _ns_to_ms(dt_stats)

    rpc_stats = _calc_statistics(rpc_lat)
    rpc_stats = _ns_to_ms(rpc_stats)
    return dt_stats, rpc_stats


def _extract_timestamps(
    trace: perfetto_trace_processor.PerfettoTraceProcessor, query: str
):
    results = trace.query(query)

    timestamps = dict()
    for r in results:
        uuid = r.name.split('message_id = ', 1)[1]
        timestamps[uuid] = r.ts
    return timestamps


def _calc_get_latest_message(
    trace: perfetto_trace_processor.PerfettoTraceProcessor,
) -> Dict[str, Any]:
    """Returns latency statistics for get_latest_message calls"""

    query_dt = """SELECT * FROM slice WHERE name LIKE 'get_latest_message, publisher_type = "dt"%'"""
    latency_dt = _calc_latency_stats(trace, query_dt)

    query_rpc = """SELECT * FROM slice WHERE name LIKE 'get_latest_message, publisher_type = "rpc"%'"""
    latency_rpc = _calc_latency_stats(trace, query_rpc)

    return latency_dt, latency_rpc


def _calc_latency_stats(
    trace: perfetto_trace_processor.PerfettoTraceProcessor, query: str
) -> Dict[str, int]:
    """Returns latency statistics for a given query"""

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
    return _ns_to_ms(stats)


def _calc_statistics(values: List[int]) -> Dict[str, float]:
    """Returns avg, median, p95, max statistics for a given list of durations"""

    if not values:
        return {}

    n = len(values)
    sorted_values = sorted(values)

    # Calculate 95th percentile.
    # TODO: b/449630628 - Use AOSP library when available.
    index = (n - 1) * 0.95
    lower_idx = int(index)
    remainder = index - lower_idx
    if lower_idx + 1 < n:
        p95 = (
            sorted_values[lower_idx] * (1 - remainder)
            + sorted_values[lower_idx + 1] * remainder
        )
    else:
        p95 = sorted_values[lower_idx]

    return {
        'avg': statistics.mean(sorted_values),
        'median': statistics.median(sorted_values),
        'p95': p95,
        'max': sorted_values[-1],
    }


def _ns_to_ms(stats: Dict[str, float]):
    # Convert nanoseconds to milliseconds for all stats
    return {
        key: float(round(value / 1000000.0, 2)) for key, value in stats.items()
    }
