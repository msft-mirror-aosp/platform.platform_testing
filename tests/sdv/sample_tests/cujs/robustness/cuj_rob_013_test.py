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
SDV Robustness Test: CUJ-ROB-013
- Power off server VM
- Suspend client VM
- Power on server VM
- Resume client VM
"""

import logging

from robustness import sdv_rob_base_test
from sdv_test_fw.test_execution import sdv_test_runner



class CujRob013(sdv_rob_base_test.SdvRobBaseTest):
    """
    Given a client VM on ECU#1 and server VM on ECU#2,
    when server is powered off, client is suspended, server is powered on,
    and client is resumed, then SDV Comms is re-established.
    """

    def test_scenario(self):
        """Implements the test case for CUJ-ROB-013."""
        logging.info("Starting test for CUJ-ROB-013.")

        self.graceful_shutdown_server_vm()

        self.suspend_client_vm()

        self.power_on_server_vm()

        self.wait_for_client_resume()

        self.assert_comms_working("Comms check after test sequence.")


if __name__ == "__main__":
    sdv_test_runner.run()