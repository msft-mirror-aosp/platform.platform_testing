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

'''
SDV Perfetto tracing test
'''

from mobly import asserts
import logging
import os
import tempfile
import importlib.resources
import time
import glob
import re
import subprocess
from sdv_perfetto import perfetto_trace_processor
from sdv_perfetto import collector_config
from sdv_perfetto import perfetto_collector
from pathlib import Path
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner

class SdvPerfettoTracingTest(sdv_base_test.SdvBaseTestClass):

    TRACE_CONFIG_PATH = "config/trace_cfg.pbtx"
    START_SERVER_CPP_COMMAND = "/apex/com.android.sdv.sample.tracing/bin/sdv_server_cpp_tracing_sample"
    START_CLIENT_CPP_COMMAND = "/apex/com.android.sdv.sample.tracing/bin/sdv_client_cpp_tracing_sample"
    START_SERVER_RUST_COMMAND = "sdv_server_rust_tracing_sample"
    START_CLIENT_RUST_COMMAND = "/apex/com.android.sdv.sample.tracing/bin/sdv_client_rust_tracing_sample"
    TAGS_CPP = ["tracing-sample-server", "tracing-sample-client"]
    TAGS_RUST = ["tracing-sample-rust-server", "tracing-sample-rust-client"]

    trace_cfg_path = ''

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()

    def run_and_check_perfetto_trace(self, client_command, server_command, tags):
        # Start capturing
        perfetto = (
            perfetto_collector.PerfettoCollector(
                device=self.sdv_device,
                config=collector_config.CollectorConfig(config_path=self.TRACE_CONFIG_PATH)
            )
        )
        perfetto.start_trace()

        # Start sample server and client
        self.sdv_device.execute_shell_command_in_subprocess(
            subprocess_name="sample_server",
            shell_command=server_command,
        )
        self.sdv_device.execute_shell_command(shell_command=client_command)

        # Stop perfetto capture
        trace_file_path = perfetto.stop_trace()

        # Check the report has entries for sample server/client
        query=f"select tag, count(*) as cnt from android_logs where tag in {repr(tuple(tags))} group by tag"
        trace_processor = perfetto_trace_processor.PerfettoTraceProcessor(trace_file_path)
        qr_it = trace_processor.query(query)
        query_dict = dict.fromkeys(tags, 0)
        for row in qr_it:
            query_dict[row.tag] = int(row.cnt)
        for tag, cnt in query_dict.items():
            asserts.assert_true(
                tag in tags,
                f"Unexpected tag '{tag}' found in a trace report '{trace_file_path}'")
            asserts.assert_greater(
                cnt,
                0,
                f"No log entries for '{tag}' found in a trace report '{trace_file_path}'")

    def test_perfetto_trace_cpp(self):
        logging.info(f'{self.get_suite_name()}#{self.current_test_info.name} started')
        self.run_and_check_perfetto_trace(
            client_command=self.START_CLIENT_CPP_COMMAND,
            server_command=self.START_SERVER_CPP_COMMAND,
            tags=self.TAGS_CPP)
        logging.info(f'{self.get_suite_name()}#{self.current_test_info.name} completed.')

    def test_perfetto_trace_rust(self):
        logging.info(f'{self.get_suite_name()}#{self.current_test_info.name} started')
        self.run_and_check_perfetto_trace(
            client_command=self.START_CLIENT_RUST_COMMAND,
            server_command=self.START_SERVER_RUST_COMMAND,
            tags=self.TAGS_RUST)
        logging.info(f'{self.get_suite_name()}#{self.current_test_info.name} completed.')




if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
