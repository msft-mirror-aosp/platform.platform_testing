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
This test verifies the multi-VM health monitoring functionality and collects
Perfetto traces during the interaction.

Two VMs are launched, each running a Health Monitor Agent instance.

One VM also hosts a Vehicle Health Monitor, which consumes VM health reports
from both Health Monitor instances, via data tunnel subscription.

Metrics regarding periodicity of the data tunnel publication of the cross VM health report
are extracted, and exported to crystalball
"""


import time
import logging
import numpy as np
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_perfetto import perfetto_collector, perfetto_trace_processor


class SdvHmMetricsCollectionTwoVMTest(
    sdv_base_test.SdvBaseTestClass
):
    def setup_class(self):
        super().setup_class()
        self.device1 = self.get_device('device1').adb()
        self.device1_name = self.device1.prop.get(SdvDeviceProperty.INSTANCE_NAME)
        self.perfetto_collector_device1 = perfetto_collector.PerfettoCollector(
            self.device1)
        self.metrics = {}

    def teardown_class(self):
        perfetto_trace_processor.export_to_crystalball(
            data=self.metrics,
            output_dir=self.device1.log_path(),
            test_name="HM Metrics Collection Two VM Test",
            omit_base_name=False
        )
        super().teardown_class()

    def test_collect_health_monitor_metrics(self):
        self.device1.execute_shell_command_in_subprocess(
            subprocess_name="dt_sub_process",
            shell_command="test_vm_health_subscriber"
        )

        self.perfetto_collector_device1.start_trace()
        logging.info("Collecting traces for 30 seconds...")
        time.sleep(30)

        trace_path_device1 = self.perfetto_collector_device1.stop_trace()

        trace_processor = perfetto_trace_processor.PerfettoTraceProcessor(
            trace_path_device1)
        self.metrics.update(
            self._extract_cross_vm_dt_health_report_periodicity(trace_processor))

    def _extract_cross_vm_dt_health_report_periodicity(self, trace_processor):
        QUERY = """SELECT * FROM slice WHERE name LIKE '%instance2:HM_PERF_TEST, DT_SUBSCRIBER: Received health report%' ORDER BY ts"""
        r = trace_processor.query(QUERY)

        # processing events, thus ts=te, dur=0
        ts = [m.ts for m in r]
        ts_delta = [t1 - t0 for t0, t1 in zip(
            ts, ts[1:])]

        td_avg, td_max, td_median, td_p90 = self._calculate_avg_max_median_p90_us(
            ts_delta)
        config_report_period = 100_000  # should match default HM config for SDV core image
        p90_tol = (td_p90 - config_report_period) / \
            config_report_period * 100

        return {
            "dt_subscriber_vm_health_cross_vm_periodicity": {
                "avg": td_avg,
                "max": td_max,
                "median": td_median,
                "p90": td_p90,
            },
            "dt_subscriber_cross_vm_target100ms_p90tol": p90_tol,
        }

    def _calculate_avg_max_median_p90_us(self, d: list[int]) -> tuple[int, int, int, int]:
        """expects duration data in nanoseconds"""

        # sanity
        if len(d) < 30:
            raise Exception(
                f"Expecting at least 30 samples to calculate metrics, check sample extraction. size: {len(d)}"
            )

        logging.info(f"DEBUGGY distribution: {d}")
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


if __name__ == "__main__":
    sdv_test_runner.run()
