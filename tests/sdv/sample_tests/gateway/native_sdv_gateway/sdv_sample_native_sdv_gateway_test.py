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
Native SDV Gateway Sample Test
"""

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

class SdvSampleNativeSdvGatewayTest(sdv_base_test.SdvBaseTestClass):

    RUN_CLUSTER_SERVER_COMMAND = 'sdv_mw_cluster_server'
    RUN_PUBLISHER_COMMAND = 'sdv_dt_mw_publisher_rs'
    RUN_NATIVE_SDV_GATEWAY_COMMAND = 'native_sdv_gateway_test_app'
    RUN_SUNROOF_CLIENT_COMMAND = 'sdv_mw_sunroof_client'
    RUN_SUBSCRIBER_COMMAND = 'sdv_dt_mw_subscriber_rs'

    CLUSTER_SERVER_LOG_TAG = 'sdv_cluster_server'
    NATIVE_SDV_GATEWAY_LOG_TAG = 'native_sdv_gateway_test_app'
    SUNROOF_CLIENT_LOG_TAG = 'sdv_sunroof_client'

    EXPECTED_NATIVE_SDV_GATEWAY_LOG = [
        'native_sdv_gateway_test_app: ASDVGateway_Client_new completed successfully',
        'native_sdv_gateway_test_app: ASDVGateway_Client_initComms completed successfully',
        'native_sdv_gateway_test_app: ASDVGateway_Client_fetchServiceUnitsByType completed successfully',
        'native_sdv_gateway_test_app: Listen for Service Unit Changes for TirePressure',
        'native_sdv_gateway_test_app: ASDVGateway_Client_registerListenerForServiceUnitChangeByType completed successfully',
        'native_sdv_gateway_test_app: ASDVGateway_Client_rpcCredentials completed successfully',
        'native_sdv_gateway_test_app: Created gRPC server listening on port:',
        'native_sdv_gateway_test_app: ASDVGateway_Client_registerRpcServer completed successfully',
        'native_sdv_gateway_test_app: ASDVGateway_Client_findRpcServerByName completed successfully',
        'native_sdv_gateway_test_app: Received DrivingStateResponse:',
        'native_sdv_gateway_test_app: ASDVGateway_Client_createPublication completed successfully',
        'native_sdv_gateway_test_app: createdPublication',
        'native_sdv_gateway_test_app: Publisher thread started',
        'native_sdv_gateway_test_app: ASDVGateway_Client_publishMessages completed successfully',
    ]

    EXPECTED_POST_PUBLISHER_NATIVE_SDV_GATEWAY_LOG = [
        'native_sdv_gateway_test_app: Received callback for ServiceUnit Registered',
        'native_sdv_gateway_test_app: subscribeWithServiceUnitDefinition',
        'native_sdv_gateway_test_app: ASDVGateway_Client_subscribeToPublicationByName completed successfully',
        'native_sdv_gateway_test_app: subscribedTo',
        'native_sdv_gateway_test_app: Got app metadata of size:',
        'native_sdv_gateway_test_app: ASDVGateway_Client_publishMessages completed successfully',
        'native_sdv_gateway_test_app: ASDVGateway_Client_publishMessages completed successfully',
        'native_sdv_gateway_test_app: Read data tunnel message: pressure:',
        'native_sdv_gateway_test_app: location:',
        'native_sdv_gateway_test_app: from publicationId:',
    ]

    EXPECTED_CLUSTER_SERVER_START_LOG = [
        'cluster_server: sdv_cluster_server: Cluster server service bundle started.',
    ]

    EXPECTED_CLUSTER_SERVER_LOG = [
        'cluster_server: sdv_cluster_server: Received request for UpdateDrivingState method from Bundle NativeTestApp',
        'cluster_server: sdv_cluster_server: Cluster server set driving state to DRIVING_STATE_PARK',
    ]

    EXPECTED_SUNROOF_CLIENT_LOG = [
        'sunroof_client: sdv_sunroof_client: Sunroof client service bundle started.',
        'sunroof_client: sdv_sunroof_client: gRPC sample Sunroof client received: SUNROOF_STATE_OPEN as the current Sunroof state',
    ]

    def setup_class(self):
        super().setup_class()
        self.ivi_vm_device = self.get_device('device1').adb()
        self.core_vm_device = self.get_device('device2').adb()

    def test_native_sdv_gateway_sample(self):
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.RUN_CLUSTER_SERVER_COMMAND,
            self.RUN_CLUSTER_SERVER_COMMAND
        )
        polling.wait_for_true(
          self.check_logcat_for_expected_lines,
          self.core_vm_device,
          self.CLUSTER_SERVER_LOG_TAG,
          self.EXPECTED_CLUSTER_SERVER_START_LOG)
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.RUN_PUBLISHER_COMMAND,
            self.RUN_PUBLISHER_COMMAND
        )
        self.ivi_vm_device.execute_shell_command_in_subprocess(
            self.RUN_NATIVE_SDV_GATEWAY_COMMAND,
            self.RUN_NATIVE_SDV_GATEWAY_COMMAND
        )
        polling.wait_for_true(
          self.check_logcat_for_expected_lines,
          self.ivi_vm_device,
          self.NATIVE_SDV_GATEWAY_LOG_TAG,
          self.EXPECTED_NATIVE_SDV_GATEWAY_LOG)
        polling.wait_for_true(
          self.check_logcat_for_expected_lines,
          self.core_vm_device,
          self.CLUSTER_SERVER_LOG_TAG,
          self.EXPECTED_CLUSTER_SERVER_LOG,
        )
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.RUN_SUNROOF_CLIENT_COMMAND,
            self.RUN_SUNROOF_CLIENT_COMMAND
        )
        polling.wait_for_true(
          self.check_logcat_for_expected_lines,
          self.core_vm_device,
          self.SUNROOF_CLIENT_LOG_TAG,
          self.EXPECTED_SUNROOF_CLIENT_LOG,
        )
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.RUN_SUBSCRIBER_COMMAND,
            self.RUN_SUBSCRIBER_COMMAND
        )
        polling.wait_for_true(
          self.check_logcat_for_expected_lines,
          self.ivi_vm_device,
          self.NATIVE_SDV_GATEWAY_LOG_TAG,
          self.EXPECTED_POST_PUBLISHER_NATIVE_SDV_GATEWAY_LOG,
        )

    def check_logcat_for_expected_lines(self, device, grep_text, expected_lines):
      for expected_line in expected_lines:
        if expected_line not in device.grep_from_logcat(grep_text):
          return False
      return True

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()