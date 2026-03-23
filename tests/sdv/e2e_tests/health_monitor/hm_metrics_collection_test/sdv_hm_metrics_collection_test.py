
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

"""SDV Health Monitoring Performance Metrics collection"""

from mobly import asserts
from sdv_perfetto import perfetto_collector, perfetto_trace_processor, collector_config
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from typing import Callable
import time
import numpy as np
from itertools import groupby
import logging


class SdvHmMetricsCollectionTest(sdv_base_test.SdvBaseTestClass):
    CONFIG_REPORT_PERIOD = 40_000

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()
        # Collecting performance metrics. Not using logcat output, thus reduce verbosity:
        self.sdv_device.execute_shell_command('setprop persist.log.tag W')
        self.metrics = {}

        self.sdv_authz_enable_value = self.sdv_device.prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        self.sdv_device.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, "permissions_only")

    def teardown_class(self):
        perfetto_trace_processor.export_to_crystalball(
            data=self.metrics,
            output_dir=self.sdv_device.log_path(),
            test_name="HM performance metric collection test",
            omit_base_name=False
        )
        self.sdv_device.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.sdv_authz_enable_value)
        super().teardown_class()

    def test_hm_trace_processing_and_reporting(self):
        trace_processor = self.get_post_boot_trace(
            trace_config_path="only_atrace_cfg.pbtx",
            pre_boot_step=lambda: HmConfigUtils.set_hm_config(
                self.sdv_device, "hm_config_single_vm_perf.textproto"),
            post_boot_step=lambda: self.sdv_device.execute_shell_command_in_subprocess(
                subprocess_name="dt_sub_process",
                shell_command="test_vm_health_subscriber"
            ),
            cleanup_step=lambda: HmConfigUtils.reset_hm_config(
                self.sdv_device)
        )
        self.metrics.update(
            self.extract_instrumentation_metrics(trace_processor))

    def test_collect_someip_health_report_periodicity(self):
        trace_processor = self.get_post_boot_trace(
            trace_config_path="only_cpp_cfg.pbtx",
            # start vsomeip hm subscriber post boot:
            post_boot_step=lambda: self.sdv_device.execute_shell_command_in_subprocess(
                subprocess_name="someip_sub_process",
                shell_command="VSOMEIP_CONFIGURATION=/vendor/etc/vsomeip/vehicle_health_monitor_sample.json VSOMEIP_BASE_PATH=/data/vendor/vsomeip/ sdv_vsomeip_vehicle_health_monitor_sample_target"
            )
        )
        self.metrics.update(
            self.extract_someip_report_periodicity(trace_processor))

    def test_hm_trace_processing_and_reporting_cpu_mem(self):
        trace_processor = self.get_post_boot_trace(
            trace_config_path="cpu_mem_prof_cfg.pbtx",
            pre_boot_step=lambda: HmConfigUtils.set_hm_config(
                self.sdv_device, "hm_config_single_vm_perf.textproto"),
            cleanup_step=lambda: HmConfigUtils.reset_hm_config(
                self.sdv_device)
        )
        self.metrics.update(self.extract_process_cpu_mem_metrics(
            trace_processor, "/system_ext/bin/sdv_health_monitor"))

    def get_post_boot_trace(self, trace_config_path,
                            pre_boot_step: Callable[[], None] = None,
                            post_boot_step: Callable[[], None] = None,
                            cleanup_step: Callable[[], None] = None,
                            ):
        """
        Collects trace data starting on boot, for approx 30s.
        Accepts additional optional steps as input, in order to setup
        the system under test for the various test cases
        """
        collector = perfetto_collector.PerfettoCollector(
            device=self.sdv_device,
            config=collector_config.CollectorConfig(
                config_path=trace_config_path)
        )
        collector.set_start_trace_on_boot()
        if pre_boot_step:
            pre_boot_step()
        self.sdv_device.reboot_device_and_verify_logcat()
        if post_boot_step:
            post_boot_step()
        # record traces for approx 30s post logcat up and running
        time.sleep(30)
        trace_file_path = collector.stop_trace(read_from_running_process=True)
        if cleanup_step:
            cleanup_step()
        return perfetto_trace_processor.PerfettoTraceProcessor(
            trace_file_path)

    def extract_process_cpu_mem_metrics(self, trace_processor, process_name):
        trace_ts_all_cores = list(trace_processor.query(
            "SELECT value FROM stats WHERE name LIKE 'ftrace_cpu_now_ts_begin'"))
        nr_cores = len(trace_ts_all_cores)

        trace_duration = perfetto_trace_processor.traceMetrics_to_dict(
            trace_processor.get_built_in_metrics(
                ['trace_metadata']
            ))['trace_metadata']['trace_duration_ns']

        cpu_metrics = perfetto_trace_processor.traceMetrics_to_dict(
            trace_processor.get_built_in_metrics(
                ['android_cpu']
            ))

        process_cpu_metrics = next(
            x for x in cpu_metrics["android_cpu"]["process_info"] if x["name"] == process_name)
        process_exec_time_all_cores = sum(
            per_core["metrics"]["runtime_ns"]
            for thread in process_cpu_metrics["threads"] if "core" in thread
            for per_core in thread["core"]
        )
        process_cpu_util = process_exec_time_all_cores / trace_duration / nr_cores * 100

        mem_query = ("select c.ts, c.value, t.name as counter_name, p.name as proc_name, p.pid "
                     "from counter as c left join process_counter_track as t on c.track_id = t.id "
                     "left join process as p using (upid) "
                     f"where t.name like 'mem.rss.anon' and p.name like '{process_name}'")

        mem_rss_anon = [r.value for r in trace_processor.query(mem_query)]
        m_avg, m_max, m_median, m_p90 = self._calculate_avg_max_median_p90_us(
            mem_rss_anon)

        return {
            "agent_cpu_util_full_range_avg_percentage": process_cpu_util,
            "agent_mem_rss_anon_kB": {
                # heap + stack mem
                "avg": m_avg,
                "max": m_max,
                "median": m_median,
                "p90": m_p90,
            }
        }

    def _get_all_bundles_registered_instant(self, trace_processor):
        """ extracts instant in time when all bundles have completed registration.
        Some metrics are computed before and after this instant"""
        QUERY = """SELECT * FROM slice WHERE name LIKE '%register_for_health_monitoring%'"""
        r = trace_processor.query(QUERY)
        return int(max((m.ts + m.dur for m in r)))

    def extract_instrumentation_metrics(self, trace_processor):
        metrics = self._extract_report_vm_health_metrics(
            trace_processor)

        metrics.update(self._extract_hm_bundle_registration_metrics(
            trace_processor))
        metrics.update(self._extract_vm_health_subscriber_metrics(
            trace_processor))
        return metrics

    def _extract_hm_bundle_registration_metrics(self, trace_processor):
        QUERY = """SELECT * FROM slice WHERE name LIKE '%register_for_health_monitoring%'"""
        r = trace_processor.query(QUERY)
        r = list(r)
        for m in r:
            m.te = m.ts + m.dur

        all_bundles_registering_dur = int(
            max((m.te for m in r)) - min((m.ts for m in r))
        )

        def fqin_from_perfetto_entry(entry):
            return entry.name.split('fqin = "')[1].split('",')[0]
        r_sorted_by_fqin = sorted(r, key=fqin_from_perfetto_entry)
        r_grouped_per_fqin = list(map(
            # interested just in grouping, discard grouper:
            lambda x: list(x[1]),
            groupby(
                r_sorted_by_fqin,
                key=fqin_from_perfetto_entry
            )
        ))

        durations_only_running = [
            sum((p.dur for p in polls))
            for polls in r_grouped_per_fqin
        ]
        d_avg, d_max, d_median, d_p90 = self._calculate_avg_max_median_p90_us(
            durations_only_running)

        durations_wall_clock_time = map(
            lambda poll_with_min_ts, poll_with_max_te:
            poll_with_max_te.te - poll_with_min_ts.ts,
            (min(polls, key=lambda poll: poll.ts)
             for polls in r_grouped_per_fqin),
            (max(polls, key=lambda poll: poll.te)
             for polls in r_grouped_per_fqin),
        )
        dwa_avg, dwa_max, dwa_median, dwa_p90 = self._calculate_avg_max_median_p90_us(
            list(durations_wall_clock_time))

        return {
            "all_bundles_hm_registration": {
                # Wall clock time. t_1 - t_0, where t_1: instant when first hm
                # registration event starts, on bundle side. t_0: instant when last hm registration
                # event ends, on bundle side
                "duration_us": all_bundles_registering_dur//1000,
            },
            "bundles_registration_start": {
                # Wall clock time. time elapsed between system clock start and first hm registration event
                "time_start_us": min([m.ts for m in r])//1000,
            },
            "bundle_hm_registration": {
                "execution_time_us": {
                    "avg": d_avg,
                    "max": d_max,
                    "median": d_median,
                    "p90": d_p90
                },
                "wallclock_time_us": {
                    # bundle registration measured by 'wall clock', in microseconds.
                    # Includes external factors: OS scheduling, tokio scheduling etc.
                    "avg": dwa_avg,
                    "max": dwa_max,
                    "median": dwa_median,
                    "p90": dwa_p90,
                }
            }
        }

    def _extract_report_vm_health_metrics(self, trace_processor):
        t_all_registered = self._get_all_bundles_registered_instant(
            trace_processor)
        QUERY = """SELECT * FROM slice WHERE name LIKE '%report_vm_health%'"""
        r = trace_processor.query(QUERY)
        durations = [m.dur for m in r]

        d_avg, d_max, d_median, d_p90 = self._calculate_avg_max_median_p90_us(
            durations)

        r = trace_processor.query(QUERY)
        vm_report_te_pre_all_registered = [
            m.ts + m.dur for m in r if m.ts < t_all_registered]
        te_deltas_pre = [t1 - t0 for t0, t1 in zip(
            vm_report_te_pre_all_registered, vm_report_te_pre_all_registered[1:])]

        r = trace_processor.query(QUERY)
        vm_report_te_post_all_registered = [
            m.ts + m.dur for m in r if m.ts >= t_all_registered]
        te_deltas_post = [t1 - t0 for t0, t1 in zip(
            vm_report_te_post_all_registered, vm_report_te_post_all_registered[1:])]

        td_avg_1, td_max_1, td_median_1, td_p90_1 = self._calculate_avg_max_median_p90_us(
            te_deltas_pre)
        td_avg_2, td_max_2, td_median_2, td_p90_2 = self._calculate_avg_max_median_p90_us(
            te_deltas_post)

        p90_tol_pre = (td_p90_1 - self.CONFIG_REPORT_PERIOD) / \
            self.CONFIG_REPORT_PERIOD * 100
        p90_tol_post = (td_p90_2 - self.CONFIG_REPORT_PERIOD) / \
            self.CONFIG_REPORT_PERIOD * 100

        return {
            "agent_vm_health_reporting_exec_time_us": {
                "avg": d_avg,
                "max": d_max,
                "median": d_median,
                "p90": d_p90,
            },
            "vm_health_report_periodicity": {
                "pre_all_bundles_registered_us": {
                    "avg": td_avg_1,
                    "max": td_max_1,
                    "median": td_median_1,
                    "p90": td_p90_1,
                },
                "post_all_bundles_registered_us": {
                    "avg": td_avg_2,
                    "max": td_max_2,
                    "median": td_median_2,
                    "p90": td_p90_2,
                },
            },
            "p90tol": {
                "pre_all_bundles_registered_perc": p90_tol_pre,
                "post_all_bundles_registered_perc": p90_tol_post
            },
            "configured_health_report_period_us": self.CONFIG_REPORT_PERIOD
        }

    def _extract_vm_health_subscriber_metrics(self, trace_processor):
        """
        Calculates vm health report periodicity, on dt subscriber side.
        In contrast with priodicity calculations on agent side, metrics are
        not split between "pre bundle registration" and "post bundle registration".
        This is because subscriber binary does not get started fast enough to see
        "pre bundle registration" vm health reports
        """
        QUERY = """SELECT * FROM slice WHERE name LIKE '%HM_PERF_TEST, DT_SUBSCRIBER%'"""
        r = trace_processor.query(QUERY)

        # processing events, thus ts=te, dur=0
        ts = [m.ts for m in r]
        logging.info(f"size of ts: {len(ts)}")
        ts_delta = [t1 - t0 for t0, t1 in zip(
            ts, ts[1:])]

        td_avg, td_max, td_median, td_p90 = self._calculate_avg_max_median_p90_us(
            ts_delta)

        p90_tol = (td_p90 - self.CONFIG_REPORT_PERIOD) / \
            self.CONFIG_REPORT_PERIOD * 100

        return {
            "dt_subscriber_vm_health_periodicity": {
                "avg": td_avg,
                "max": td_max,
                "median": td_median,
                "p90": td_p90,
            },
            "dt_subscriber_p90tol": p90_tol
        }

    def extract_someip_report_periodicity(self, trace_processor):
        QUERY = """SELECT * FROM slice WHERE name LIKE '%HM_PERF_TEST, SOMEIP_SUB%'"""
        r = trace_processor.query(QUERY)

        # processing events, thus ts=te, dur=0
        ts = [m.ts for m in r]
        logging.info(f"size of ts: {len(ts)}")
        ts_delta = [t1 - t0 for t0, t1 in zip(
            ts, ts[1:])]

        td_avg, td_max, td_median, td_p90 = self._calculate_avg_max_median_p90_us(
            ts_delta)
        config_report_period = 100_000
        p90_tol = (td_p90 - config_report_period) / \
            config_report_period * 100

        return {
            "someip_subscriber_vm_health_target_periodicity": config_report_period,
            "someip_subscriber_vm_health_periodicity": {
                "avg": td_avg,
                "max": td_max,
                "median": td_median,
                "p90": td_p90,
            },
            "someip_subscriber_p90tol": p90_tol,
        }

    def _calculate_avg_max_median_p90_us(self, d: list[int]) -> tuple[int, int, int, int]:
        """expects duration data in nanoseconds"""

        # sanity
        asserts.assert_true(
            len(d) > 30,
            f"Expecting at least 30 samples to calculate metrics, check sample extraction. size: {len(d)}")

        d_array = np.array(d)
        d_avg = np.mean(d_array)
        d_max = np.max(d_array)
        d_median = np.median(d_array)
        d_p90 = np.percentile(d_array, 90)

        d_avg_us = int(d_avg)//1000
        d_max_us = int(d_max)//1000
        d_median_us = int(d_median)//1000
        d_p90_us = int(d_p90)//1000

        return d_avg_us, d_max_us, d_median_us, d_p90_us


# Alternative config textproto should added to default directory at build time
DEFAULT_HM_CONFIG_DIR = "/system/etc/health_monitor"


class HmConfigUtils:
    def set_hm_config(adb_handle, alternative_config_filename):
        """ Sets a new HM config. HM Agent should be restarted to react to the new config"""
        adb_handle.execute_shell_command(
            f"setprop persist.sdv.health_monitor.config_path {DEFAULT_HM_CONFIG_DIR}/{alternative_config_filename}")

    def reset_hm_config(adb_handle):
        """
        Set property to empty. It will be reset during init.rc to default value
        Device needs to be restarted in order for the reset to take effect
        """
        adb_handle.execute_shell_command(
            f"setprop persist.sdv.health_monitor.config_path \"\"")


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
