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
SDV Update Manager Agent APEX Cleanup Test

Check that inactive APEX is cleaned up as a result of apexservice::markBootCompleted call on every boot.
"""

from mobly import asserts
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.update.update_manager_base_class import UpdateManagerBaseClass
from sdv_test_fw.verification import polling


class SdvE2EUMApexCleanupTest(sdv_base_test.SdvBaseTestClass, UpdateManagerBaseClass):
    APEX_NAME = "com.android.sdv.sample.apex.provider"
    APEX_FILE_NAME = f"{APEX_NAME}.apex"
    ACTIVE_APEX_DIR = "/data/apex/active"
    ACTIVE_APEX_PATH = f"{ACTIVE_APEX_DIR}/{APEX_FILE_NAME}"
    PREPARE_ACTIVE_APEX_COMMAND = (
        f"cp /product/apex/{APEX_FILE_NAME} {ACTIVE_APEX_PATH}"
    )
    LIST_ACTIVE_APEX_COMMAND = f"ls {ACTIVE_APEX_DIR}"
    EXPECTED_APEX_CLEANUP_LOG = f"Removing inactive data APEX {ACTIVE_APEX_PATH}"

    def setup_class(self):
        super().setup_class()
        self.init_update_manager_base_class(self.APEX_NAME)

    def setup_test(self):
        self.sdv_device.adb().reboot_device()
        self.sdv_device.adb().root_device()

    def test_apex_cleanup_on_ready(self):
        self.sdv_device.adb().execute_shell_command(self.PREPARE_ACTIVE_APEX_COMMAND)
        res_before_reboot = self.sdv_device.adb().execute_shell_command(
            self.LIST_ACTIVE_APEX_COMMAND
        )
        asserts.assert_in(self.APEX_FILE_NAME, res_before_reboot)

        self.sdv_device.adb().reboot_device()
        self.sdv_device.adb().root_device()

        polling.wait_and_verify_expected_logs(
            self.sdv_device.adb(), "apexd", self.EXPECTED_APEX_CLEANUP_LOG
        )
        res_after_reboot = self.sdv_device.adb().execute_shell_command(
            self.LIST_ACTIVE_APEX_COMMAND, raise_exception=False
        )
        asserts.assert_equal(
            "",
            res_after_reboot,
            f"The non-mounted APEX {self.APEX_FILE_NAME} is not cleaned up from {self.ACTIVE_APEX_DIR}",
        )

    def test_apex_cleanup_on_activate_post_reboot_rollback_complete(self):
        self.upload_service_bundle_update_payload()
        self.prepare_service_bundle_update()
        self.assert_in_status("PREPARE_COMPLETE")
        self.client.activate()
        self.assert_in_status("ACTIVATE_PRE_REBOOT_COMPLETE")

        self.sdv_device.adb().reboot_device()
        self.sdv_device.adb().root_device()

        self.assert_in_status("ACTIVATE_POST_REBOOT_COMPLETE")
        self.sdv_device.adb().execute_shell_command(self.PREPARE_ACTIVE_APEX_COMMAND)
        res_before_rollback = self.sdv_device.adb().execute_shell_command(
            self.LIST_ACTIVE_APEX_COMMAND
        )
        asserts.assert_in(self.APEX_FILE_NAME, res_before_rollback)

        self.client.rollback()
        self.assert_in_status("ACTIVATE_POST_REBOOT_ROLLBACK_COMPLETE")

        polling.wait_and_verify_expected_logs(
            self.sdv_device.adb(), "apexd", self.EXPECTED_APEX_CLEANUP_LOG
        )
        res_after_rollback = self.sdv_device.adb().execute_shell_command(
            self.LIST_ACTIVE_APEX_COMMAND, raise_exception=False
        )
        asserts.assert_equal(
            "",
            res_after_rollback,
            f"The non-mounted APEX {self.APEX_FILE_NAME} is not cleaned up from {self.ACTIVE_APEX_DIR}",
        )


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
