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

"""SDV Robustness Test: CUJ-ROB-002 - Graceful shutdown server VM."""

import logging

from robustness import sdv_rob_base_test
from sdv_test_fw.test_execution import sdv_test_runner


class CujRob002(sdv_rob_base_test.SdvRobBaseTest):
    """
    Given a client VM running on ECU#1 connected to a server VM on ECU#2,
    when ECU#2 gracefully shuts down and restarts,
    then SDV Comms is successfully re-established.
    """

    def test_graceful_shutdown_server_vm(self):
        """Gracefully restarts the server VM and verifies comms."""
        logging.info("Starting test for CUJ-ROB-002.")

        self.graceful_shutdown_server_vm()
        self.power_on_server_vm()

        self.assert_comms_working(
            "Comms check after server VM graceful shutdown and restart."
        )


if __name__ == "__main__":
    sdv_test_runner.run()