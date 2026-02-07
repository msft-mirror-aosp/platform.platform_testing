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
"""SDV User Preferences Test"""

import logging

from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner
from sdv_test_fw.verification import polling


class SdvSampleUserPreferencesTest(sdv_base_test.SdvBaseTestClass):

    HVAC_EXPECTED_LOG = [
        "Registering settings group with name: HVAC,",
        "Version: 1",
        "Started service bundle",
        "Adjusted the following settings:",
        "Key FAN_SPEED       Value Some(Int64(1         ))",
        "Key ENABLED         Value Some(Bool(false     ))",
        "Key TEMPERATURE     Value Some(Int64(16        ))",
        "Adjusted the following settings:",
        "Key TEMPERATURE     Value Some(Int64(32        ))",
        "Adjusted the following settings:",
        "Key ENABLED         Value Some(Bool(false     ))",
        "Key TEMPERATURE     Value Some(Int64(16        ))",
        "Key FAN_SPEED       Value Some(Int64(1         ))",
        "Performed factory reset",
        "Adjusted the following settings:",
        "Key ENABLED         Value Some(Bool(false     ))",
        "Key FAN_SPEED       Value Some(Int64(1         ))",
        "Key TEMPERATURE     Value Some(Int64(16        ))",
        "Adjusted the following settings:",
        "Key TEMPERATURE     Value Some(Int64(24        ))",
    ]
    HMI_EXPECTED_LOG = [
        "Creating an ephemeral user",
        "Subscribing to setting changes",
        "Current HVAC settings",
        'Key ENABLED        , Value: Bool(false), Constraint: \\"None\\"',
        'Key FAN_SPEED      , Value: Int64(1)  , Constraint: \\"Int64Constraints(Int64Constraints { min_value: Some(1), max_value: Some(5), step: None, special_fields: SpecialFields { unknown_fields: UnknownFields { fields: None }, cached_size: CachedSize { size: 0 } } })\\"',
        'Key TEMPERATURE    , Value: Int64(16) , Constraint: \\"Int64Constraints(Int64Constraints { min_value: Some(16), max_value: Some(32), step: None, special_fields: SpecialFields { unknown_fields: UnknownFields { fields: None }, cached_size: CachedSize { size: 0 } } })\\"',
        "Adjusting HVAC temperature to 32",
        "The following settings were changed in settings group instance1:com.sdv.oem.user_preferences.HvacService/default/HVAC",
        "Key TEMPERATURE     Value: Int64(32)",
        "Attempting to Adjust HVAC temperature to invalid value 100",
        "Request to set invalid temperature denied SdvStatusCode::InvalidArgument(3)",
        "Creating another user",
        "The following settings were changed in settings group instance1:com.sdv.oem.user_preferences.HvacService/default/HVAC",
        "Key ENABLED         Value: Bool(false)",
        "Key FAN_SPEED       Value: Int64(1)",
        "Key TEMPERATURE     Value: Int64(16)",
        "Switched to user 2",
        'List of available users: \\"2\\"',
        "Creating another user",
        "Deleted user 3",
        "Unsubscribing from setting changes",
    ]

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()

        # Save the current value of sdv.authz.enable
        self.sdv_authz_enable_value = self.sdv_device.prop.get(SdvDeviceProperty.AUTHZ_ENABLE)

        # Enforce SDV Comm Stack authorization
        self.sdv_device.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, "true")

    def teardown_class(self):
        # Reset SDV Comm Stack authorization
        self.sdv_device.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.sdv_authz_enable_value)
        super().teardown_class()

    def _wait_and_verify_expected_logs(self, log_entries_to_find):
        """Waits for and verifies a sequence of log entries."""
        for log_entry in log_entries_to_find:
            # Use a lambda function to define the check that wait_for_true
            # will poll. The check returns True if grep finds the log entry.
            polling.wait_for_true(
                func=lambda: len(
                    self.sdv_device.grep_from_logcat(log_entry)
                ) != 0,
                assert_msg=f'Could not find "{log_entry}" in logcat'
            )

    def test_persistent_database(self):
        self.sdv_device.prop.set(SdvDeviceProperty.ORCHESTRATOR_CONFIG_PATH, '/etc/orch/vm_user_preferences_sample_orch_config.textproto')
        self.sdv_device.reboot_device()
        self.sdv_device.wait_for_device_online()

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        self._wait_and_verify_expected_logs(self.HVAC_EXPECTED_LOG)
        self._wait_and_verify_expected_logs(self.HMI_EXPECTED_LOG)

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
