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

"""SDV Reboot Test"""

from sdv_comm_stack_multi_reboot_test_orch_config_base import SdvCommsStackMultiRebootTestOrchConfigBase
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner

# different SdvCommsStackMultiRebootTestOrchConfig implementations differ ONLY for THESE VALUES
NUMBER_OF_SERVER_REBOOTS = 10
NUMBER_OF_CLIENT_REBOOTS = 0
NUMBER_OF_CONCURRENT_REBOOTS = 0

class SdvCommsStackMultiRebootTestOrchConfig(
    sdv_base_test.SdvBaseTestClass, SdvCommsStackMultiRebootTestOrchConfigBase
):
    def test_multi_reboot(self):
        self.run_test_multi_reboot(NUMBER_OF_SERVER_REBOOTS, NUMBER_OF_CLIENT_REBOOTS, NUMBER_OF_CONCURRENT_REBOOTS)

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
