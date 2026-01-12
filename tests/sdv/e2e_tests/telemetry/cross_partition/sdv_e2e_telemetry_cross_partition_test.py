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

"""SDV E2E Telemetry Cross Partition Test"""

from datetime import timedelta
from pathlib import Path
from sdv_telemetry_test_execution import telemetry_base_test
from sdv_telemetry_test_execution.telemetry_utils import shlex_join
from sdv_test_fw.test_execution import sdv_test_runner


class SdvE2ETelemetryCrossPartitionTest(
    telemetry_base_test.SdvTelemetryBaseTestClass
):

    _TELEMETRY_CLIENT_DIR = Path("/product/bin/")

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1")
        self.sdv_device.adb().root_device()

    def teardown_class(self):
        super().teardown_class()

    def setup_test(self):
        super().setup_test()

    def teardown_test(self):
        super().teardown_test()

    def test_cross_partition(self):
        self.sdv_device.adb().log().info("Starting Cross Partition Test")
        cd_command = shlex_join(["cd", str(self._TELEMETRY_CLIENT_DIR)])
        self.sdv_device.adb().execute_shell_command(
            f"{cd_command} && sdv_telemetry_cross_partition_client"
        )
        self.sdv_device.adb().log().info("Cross Partition Test finished")


if __name__ == "__main__":
    sdv_test_runner.run()
