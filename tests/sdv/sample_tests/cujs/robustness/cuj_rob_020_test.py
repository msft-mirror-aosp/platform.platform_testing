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
SDV Robustness Test: CUJ-ROB-020
- Suspend server VM
- Power cycle client VM
- Resume server VM
"""

import logging

from robustness import sdv_rob_base_test
from sdv_test_fw.test_execution import sdv_test_runner


class CujRob020(sdv_rob_base_test.SdvRobBaseTest):
    """
    Given a server VM on ECU#1 and client VM on ECU#2,
    when server VM is suspended, client VM is power cycled, and server VM is resumed,
    then SDV Comms is successfully re-established.
    """

    def test_suspend_server_power_cycle_client_resume_server(self):
        """Tests suspend server, power cycle client, resume server."""
        logging.info("Starting test for CUJ-ROB-020.")

        self.suspend_server_vm()

        self.power_cycle_client_vm()

        self.wait_for_server_resume()

        self.assert_comms_working(
            "Comms check after server suspend, client power cycle, server resume."
        )


if __name__ == "__main__":
    sdv_test_runner.run()