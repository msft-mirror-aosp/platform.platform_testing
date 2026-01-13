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
SDV Update Manager Agent Dropped Subscriber Test

Check that an unavailable subscriber is automatically removed.
"""
from mobly import asserts
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.update.update_manager_base_class import UpdateManagerBaseClass, InterruptAction


class SdvE2EUMDroppedSubscriber(sdv_base_test.SdvBaseTestClass, UpdateManagerBaseClass):
    APEX_NAME = "com.sdv.google.sample.apex.provider"
    FAILED_CALLBACK_LOG = "Failed to send callback to subscriber"

    def setup_class(self):
        super().setup_class()
        self.init_update_manager_base_class(self.APEX_NAME)
        self.sdv_device.adb().root_device()
        self.upload_system_update_payload()

    def test_dropped_subscriber(self):
        initial_current_slot, initial_active_boot_slot = self.get_boot_slot_info()

        pre_prepare_timestamp = self.sdv_device.adb().get_current_device_timestamp()
        self.prepare_system_update(interrupt_threshold=self.TEST_INTERRUPT_THRESHOLD, skip_unsubscribe=True)
        logcat = self.sdv_device.adb().advance_logcat()
        failed_callback_log = logcat.find_message_after_timestamp(self.FAILED_CALLBACK_LOG, pre_prepare_timestamp)
        asserts.assert_is_none(
            failed_callback_log[0])

if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
