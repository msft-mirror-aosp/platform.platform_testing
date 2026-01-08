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

"""SDV Network configuration test"""

from mobly import asserts
import time
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvE2EPlatformNetconfigTest(sdv_base_test.SdvBaseTestClass):


    # Check for existence of vlan 'sdv_rpc'
    COMMAND_CHECK_RPC_VLAN = "ip -d -o link show sdv_rpc | grep vlan"

    # Check network namespaces exists
    COMMAND_CHECK_DT_NAMESPACE = "ip netns exec sdv_dt_ns true"
    COMMAND_CHECK_SD_NAMESPACE = "ip netns exec sd_mesh_ns true"

    # Verifying port range for eth1
    COMMAND_CHECK_VERIFY_PORT_RANGE_ETH1 = "sysctl net.ipv4.ip_local_port_range | grep -E '[[:space:]]4000[[:space:]]+5999$'"

    # Verifying port range for DT network namespace
    COMMAND_CHECK_VERIFY_PORT_RANGE_DT = "ip netns exec sdv_dt_ns sysctl net.ipv4.ip_local_port_range | grep -E '[[:space:]]6000[[:space:]]+7999$'"

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()

    def run_command_and_verify_exit_code(self, command):
        asserts.assert_equal(
            self.sdv_device.execute_shell_command(command + " >/dev/null; echo $?"),
            "0",
            f"Command `{command}` failed"
        )

    def test_rpc_vlan(self):
        self.run_command_and_verify_exit_code(self.COMMAND_CHECK_RPC_VLAN)

    def test_namespaces(self):
        self.run_command_and_verify_exit_code(self.COMMAND_CHECK_DT_NAMESPACE)
        self.run_command_and_verify_exit_code(self.COMMAND_CHECK_SD_NAMESPACE)

    def test_port_range_eth1(self):
        self.run_command_and_verify_exit_code(self.COMMAND_CHECK_VERIFY_PORT_RANGE_ETH1)

    def test_port_range_dt(self):
        self.run_command_and_verify_exit_code(self.COMMAND_CHECK_VERIFY_PORT_RANGE_DT)


if __name__ == "__main__":
    sdv_test_runner.run()
