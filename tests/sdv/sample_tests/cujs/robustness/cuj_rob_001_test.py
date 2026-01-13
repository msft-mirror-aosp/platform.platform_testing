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

"""SDV Robustness Test: CUJ-ROB-001 - Power cycle server VM."""

import logging

from robustness import sdv_rob_base_test
from sdv_test_fw.test_execution import sdv_test_runner


class CujRob001(sdv_rob_base_test.SdvRobBaseTest):
    """
    Given a client VM running on ECU#1 connected to a server VM on ECU#2,
    when ECU#2 power cycles,
    then SDV Comms is successfully re-established.
    """

    def test_power_cycle_server_vm(self):
        """Power cycles the server VM and verifies comms."""
        logging.info("Starting test for CUJ-ROB-001.")

        self.power_cycle_server_vm()

        self.assert_comms_working("Comms check after server VM power cycle.")


if __name__ == "__main__":
    sdv_test_runner.run()