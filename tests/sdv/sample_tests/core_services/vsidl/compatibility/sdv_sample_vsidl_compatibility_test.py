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

"""SDV sample 'VSIDL Compatibility' test."""

import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

# ServiceBundle names
LATEST_BUNDLE_NAME = "FullBundle"
STABLE_BUNDLE_NAME = "FullBundleStable"
MULTIPUB_BUNDLE_NAME = "MultipubBundleStable"


class SdvSampleVsidlCompatibilityTest(sdv_base_test.SdvBaseTestClass):

    LATEST_BUNDLE_FQIN = f"local-vm:com.android.sdv.sample.compatibility.latest.{LATEST_BUNDLE_NAME}/instance"
    STABLE_BUNDLE_FQIN = f"local-vm:com.android.sdv.sample.compatibility.stable.{STABLE_BUNDLE_NAME}/instance"
    MULTIPUB_STABLE_BUNDLE_FQIN = f"local-vm:com.android.sdv.sample.compatibility.stable.{MULTIPUB_BUNDLE_NAME}/instance"

    LATEST_TAG = f"com_android_sdv_sample_compatibility_latest_{LATEST_BUNDLE_NAME}_instance"
    STABLE_TAG = f"com_android_sdv_sample_compatibility_stable_{STABLE_BUNDLE_NAME}_instance"
    MULTIPUB_TAG = f"com_android_sdv_sample_compatibility_stable_{MULTIPUB_BUNDLE_NAME}_instance"

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()

    def setup_test(self):
        super().setup_test()
        self.sdv_device.reboot_device()
        self.sdv_device.wait_for_device_online()
        self.sdv_device.root_device()

    def test_vsidl_compatibility(self):
        logging.info(f'{self.get_suite_name()} :: Start Test {self.current_test_info.name}')

        # Start service bundles
        self.sdv_device.execute_shell_command(f"sdv_service_bundle start {self.LATEST_BUNDLE_FQIN}")
        self.sdv_device.execute_shell_command(f"sdv_service_bundle start {self.STABLE_BUNDLE_FQIN}")
        self.sdv_device.execute_shell_command(f"sdv_service_bundle start {self.MULTIPUB_STABLE_BUNDLE_FQIN}")

        # Verify bundles started
        for tag, bundle_name in [
            (self.LATEST_TAG, LATEST_BUNDLE_NAME),
            (self.STABLE_TAG, STABLE_BUNDLE_NAME),
            (self.MULTIPUB_TAG, MULTIPUB_BUNDLE_NAME)
        ]:
            polling.wait_and_verify_expected_logs(
                self.sdv_device,
                grep_text=f"Starting service bundle '{bundle_name}'",
                logcat_args=f"*:F {tag}:*",
                assert_msg=f"Service bundle '{bundle_name}' failed to start"
            )

        # Single-Pub: TirePressure
        logging.info("Verifying TirePressure compatibility...")

        # Latest receives from Stable
        for side in ["FRONT_RIGHT", "REAR_RIGHT"]:
            polling.wait_and_verify_expected_logs(
                self.sdv_device,
                grep_text=f"Received.*TirePressure.*{side}",
                logcat_args=f"*:F {self.LATEST_TAG}:*",
                assert_msg=f"{self.LATEST_BUNDLE_FQIN} failed to receive TirePressure update for {side}."
            )

        # Stable receives from Latest
        for side in ["FRONT_LEFT", "REAR_LEFT"]:
            polling.wait_and_verify_expected_logs(
                self.sdv_device,
                grep_text=f"Received.*TirePressure.*{side}",
                logcat_args=f"*:F {self.STABLE_TAG}:*",
                assert_msg=f"{self.STABLE_BUNDLE_FQIN} failed to receive TirePressure update for {side}."
            )

        # Multi-Pub: CabinTemp
        logging.info("Verifying CabinTemp multi-pub compatibility...")

        # Latest receives from both Stable bundles
        for source in [STABLE_BUNDLE_NAME, MULTIPUB_BUNDLE_NAME]:
            polling.wait_and_verify_expected_logs(
                self.sdv_device,
                grep_text=f"Received.*CabinTemp.*{source}.*",
                logcat_args=f"*:F {self.LATEST_TAG}:*",
                assert_msg=f"Latest bundle failed to receive CabinTemp update from {source}"
            )

        logging.info(f'{self.get_suite_name()} :: End Test {self.current_test_info.name}')

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
