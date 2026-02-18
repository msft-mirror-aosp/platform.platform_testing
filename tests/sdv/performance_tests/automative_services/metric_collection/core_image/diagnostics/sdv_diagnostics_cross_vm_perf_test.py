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
Test runs two SDV Core instances, and collects metrics related to diagnostics cross VM "RPC" call
"""

import re
import time
from sdv_test_fw.verification import polling
from sdv_perfetto import perfetto_collector, perfetto_trace_processor
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from mobly import asserts


class SdvDiagnosticsCrossVMPerfTest(
    sdv_base_test.SdvBaseTestClass
):
    def setup_class(self):
        super().setup_class()
        self.device1 = self.get_device('device1').adb()
        self.device2 = self.get_device('device2').adb()
        self.metrics = {}

    def _poll_until_condition_met(condition: callable, timeout: int = 10, poll_interval: float = 0.5, failure_message=""):
        deadline = time.perf_counter() + timeout
        while time.perf_counter() < deadline:
            if condition():
                return True
            time.sleep(poll_interval)
        asserts.fail(f"Condition not met within timeout: {failure_message}")

    def dumpsys_diagnostics_found(self):
        dump = self.device1.dumpsys("com.google.sdv.ISdvAgent/diagnostics")
        pattern = fr"Read data item\: {self.device2.prop.get(SdvDeviceProperty.INSTANCE_NAME)}\:com.android.sdv.sample.oem.diagnostics.DiagnosticsProvider1\/default\#pressure"
        return re.search(pattern, dump) is not None

    def test_diagnostics_cross_vm_perf(self):

        collector = perfetto_collector.PerfettoCollector(
            device=self.device1
        )

        collector.start_trace(start_trace_delay=5)  # seconds

        self.device2.prop.set(SdvDeviceProperty.ORCHESTRATOR_CONFIG_PATH, '/etc/orch/vm_diagnostics_provider1_sample_orch_config.textproto')
        self.device2.reboot_device()
        self.device2.wait_for_device_online()

        polling.wait_for_true(
            self.dumpsys_diagnostics_found, assert_msg="Diagnostic agent dumpsys does not report that data item was read", poll_interval=0.5)

        traces = perfetto_trace_processor.PerfettoTraceProcessor(
            collector.stop_trace(tag="device1"))
        self.metrics.update(
            SdvDiagnosticsCrossVMPerfTest._process_traces(traces))

    def teardown_class(self):
        super().teardown_class()
        perfetto_trace_processor.export_to_crystalball(
            data=self.metrics,
            output_dir=self.device1.log_path(),
            test_name="Diagnostics performance metric collection test",
            omit_base_name=False
        )

    def _process_traces(traces):
        (get_diag_rc_exec_time, get_diag_rc_wallclock_time) = SdvDiagnosticsCrossVMPerfTest._async_span_compute_metrics(
            traces,
            """SELECT * FROM slice WHERE name LIKE 'get_diagnostics_declaration_pre_read_data_rpc_call%DiagnosticsProvider1%'""")
        (data_item_rpc_exec_time, data_item_rpc_wallclock_time) = SdvDiagnosticsCrossVMPerfTest._async_span_compute_metrics(
            traces,
            """SELECT * FROM slice WHERE name LIKE 'read_data_item_rpc_call%DiagnosticsProvider1%pressure\"'""")
        (protob_to_uds_exec_time, protob_to_uds_wallclock_time) = SdvDiagnosticsCrossVMPerfTest._async_span_compute_metrics(
            traces,
            """SELECT * FROM slice WHERE name LIKE 'translate_from_protobuf_to_uds%DiagnosticsProvider1%DataItem%pressure\"%'""")
        return {
            "get_diag_rc_exec_time_us": get_diag_rc_exec_time,
            "get_diag_rc_wallclock_time_us": get_diag_rc_wallclock_time,
            "data_item_rpc_exec_time": data_item_rpc_exec_time,
            "data_item_rpc_wallclock_time": data_item_rpc_wallclock_time,
            "protob_to_uds_exec_time_us": protob_to_uds_exec_time,
            "protob_to_uds_wallclock_time_us": protob_to_uds_wallclock_time,
        }

    def _async_span_compute_metrics(traces, query):
        cs = list(traces.query(query))
        durations = [c.dur for c in cs]
        exec_time_us = sum(durations) // 1000
        span_ts_us = min(c.ts for c in cs) // 1000
        span_te_us = max(c.ts + c.dur for c in cs) // 1000
        wallclock_time_us = span_te_us - span_ts_us
        return (exec_time_us, wallclock_time_us)


if __name__ == "__main__":
    sdv_test_runner.run()
