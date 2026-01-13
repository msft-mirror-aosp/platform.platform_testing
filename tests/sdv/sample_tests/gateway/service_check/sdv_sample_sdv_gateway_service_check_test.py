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
SDV Gateway Service Check Test
"""

from mobly import asserts
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods

class SdvSampleSdvGatewayServiceCheckTest(sdv_base_test.SdvBaseTestClass):
    SDV_GATEWAY_QUERY_COMMAND = "service check google.sdv.gateway.ISdvGateway/default"
    EXPECTED_SDV_GATEWAY_SERVICE_RESPONSE = "Service google.sdv.gateway.ISdvGateway/default: found"
    ERROR_SDV_GATEWAY_SERVICE = "Could not find sdv gateway as a running service"

    SDV_GATEWAY_NETWORKING_LOG_TAG = "SdvGatewayNetworking"
    EXPECTED_POWER_STATE_LISTENER_LOG = "Added PowerPolicyListener for CarPowerManager"
    # 2 is CarPowerState::SUSPEND_ENTER
    EXPECTED_POWER_STATE_SUSPEND_LOG = "Car power state changed: 2"
    # 6 is CarPowerState::ON
    EXPECTED_POWER_STATE_POWER_ON_LOG = "Car power state changed: 6"

    def setup_class(self):
        super().setup_class()
        self.ivi_vm_device = self.get_device("device1").adb()

    def test_sdv_gateway_service_check(self):
        gateway_query_result = self.ivi_vm_device.execute_shell_command(self.SDV_GATEWAY_QUERY_COMMAND)
        asserts.assert_equal(self.EXPECTED_SDV_GATEWAY_SERVICE_RESPONSE, gateway_query_result, self.ERROR_SDV_GATEWAY_SERVICE)

    def test_sdv_gateway_power_state_handling(self):
        self.ivi_vm_device.clear_logcat()

        self.ivi_vm_device.execute_shell_command(
            "cmd car_service suspend --simulate --wakeup-after 5")

        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.SDV_GATEWAY_NETWORKING_LOG_TAG,
            expected_result=self.EXPECTED_POWER_STATE_SUSPEND_LOG,
            assert_msg="Log not found for power state change to 2",
        )

        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.SDV_GATEWAY_NETWORKING_LOG_TAG,
            expected_result=self.EXPECTED_POWER_STATE_POWER_ON_LOG,
            assert_msg="Log not found for power state change to 6",
        )

if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
