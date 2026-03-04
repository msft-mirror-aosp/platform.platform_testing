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

"""SDV sample 'VSIDL Stable' test."""

import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

class SdvSampleVsidlStableTest(sdv_base_test.SdvBaseTestClass):

    MANAGER_FQIN = "local-vm:com.android.sdv.sample.vsidl.Manager/instance"
    MONITOR_FQIN = "local-vm:com.android.sdv.sample.vsidl.Monitor/instance"

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()

    def setup_test(self):
        super().setup_test()
        self.sdv_device.reboot_device()
        self.sdv_device.wait_for_device_online()
        self.sdv_device.root_device()

    def test_vsidl_stable_communication(self):
        logging.info(f'{self.get_suite_name()} :: Start Test {self.current_test_info.name}')

        # Start service bundles
        self.sdv_device.execute_shell_command(f"sdv_service_bundle start {self.MANAGER_FQIN}")
        self.sdv_device.execute_shell_command(f"sdv_service_bundle start {self.MONITOR_FQIN}")

        # Verify Manager started
        polling.wait_and_verify_expected_logs(
            self.sdv_device,
            grep_text="Starting service bundle 'Manager'",
            logcat_args="*:F com_android_sdv_sample_vsidl_Manager_instance:*"
        )

        # Verify Monitor started
        polling.wait_and_verify_expected_logs(
            self.sdv_device,
            grep_text="Starting service bundle 'Monitor'",
            logcat_args="*:F com_android_sdv_sample_vsidl_Monitor_instance:*"
        )

        # Verify Manager is publishing
        polling.wait_and_verify_expected_logs(
            self.sdv_device,
            grep_text="Publishing on TirePressure#PRESSURE",
            logcat_args="*:F com_android_sdv_sample_vsidl_Manager_instance:*"
        )

        # Verify Monitor received message
        polling.wait_and_verify_expected_logs(
            self.sdv_device,
            grep_text="Received message on TirePressure#PRESSURE",
            logcat_args="*:F com_android_sdv_sample_vsidl_Monitor_instance:*"
        )

        # Verify Monitor is publishing range
        polling.wait_and_verify_expected_logs(
            self.sdv_device,
            grep_text="Publishing on TirePressureRange#RANGE",
            logcat_args="*:F com_android_sdv_sample_vsidl_Monitor_instance:*"
        )

        # Verify RPC: Monitor sends request
        polling.wait_and_verify_expected_logs(
            self.sdv_device,
            grep_text="Sending request on Monitor/TireService",
            logcat_args="*:F com_android_sdv_sample_vsidl_Monitor_instance:*"
        )

        # Verify RPC: Manager receives request
        polling.wait_and_verify_expected_logs(
            self.sdv_device,
            grep_text="Received request on Manager/TireService",
            logcat_args="*:F com_android_sdv_sample_vsidl_Manager_instance:*"
        )

        # Verify RPC: Monitor receives response
        polling.wait_and_verify_expected_logs(
            self.sdv_device,
            grep_text="Received response on Monitor/TireService",
            logcat_args="*:F com_android_sdv_sample_vsidl_Monitor_instance:*"
        )

        logging.info(f'{self.get_suite_name()} :: End Test {self.current_test_info.name}')

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
