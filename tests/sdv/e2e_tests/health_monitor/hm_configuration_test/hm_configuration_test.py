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

"""Verify Health Monitor configurations."""

import re
import time
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling
from sdv_test_fw.device.sdv_property import SdvDeviceProperty


class SdvHmConfigurationIntegrationTest(sdv_base_test.SdvBaseTestClass):
    HM_CONFIG_PATH_PROPERTY = "persist.sdv.health_monitor.config_path"

    def setup_class(self):
        super().setup_class()
        self.device = self.get_device('device1')
        self.hm_service_name = "sdv_health_monitor"
        self.hm_config_path_property_original_value = self.device.adb().execute_shell_command(
            f"getprop {self.HM_CONFIG_PATH_PROPERTY}"
        )

        self.sdv_authz_enable_value = self.device.adb().prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        self.device.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, "permissions_only")

    def teardown_class(self):
        self.device.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.sdv_authz_enable_value)
        super().teardown_class()

    def teardown_test(self):
        super().teardown_test()
        # Reset to default after each test
        self.set_hm_config_property(
            self.hm_config_path_property_original_value)

    def log_enter(self):
        """ Unified logging enter test suit. """
        logging.info(
            f"{self.get_suite_name()} :: Start test {self.current_test_info.name}")

    def log_exit(self):
        """ Unified logging exit test suit. """
        logging.info(
            f"{self.get_suite_name()} :: Finished test {self.current_test_info.name}")

    def set_hm_config_property(self, value):
        self.device.adb().execute_shell_command(
            f"setprop {self.HM_CONFIG_PATH_PROPERTY} '{value}'")
        self.device.adb().clear_logcat()
        self.device.adb().reboot_device_and_verify_logcat()

    def get_period_ms(self, file_path):
        file_content = self.device.adb(
        ).execute_shell_command(f"cat {file_path}")
        match = re.search(r"period_ms:\s*(\d+)", file_content)
        if match:
            return int(match.group(1))
        else:
            raise Exception("Unable to find period_ms value")

    def test_invalid_config_default_used(self):
        self.log_enter()
        invalid_config_path = "/data/invalid_config.textproto"  # Example invalid path
        # creates an empty file if it doesn't exist, and truncates it if it does
        self.device.adb().execute_shell_command(f"> {invalid_config_path}")
        self.set_hm_config_property(invalid_config_path)

        expected_log = f"Error parsing textproto or invalid config"
        polling.wait_and_verify_expected_logs(
            sdv_device=self.device.adb(),
            grep_text=expected_log,
            assert_msg="Failed to find parsing error for health configuration",
        )
        self.device.adb().clear_logcat()
        self.log_exit()

    def test_invalid_path_default_used(self):
        self.log_enter()
        non_existent_path = "/data/non_existent_config.textproto"
        self.set_hm_config_property(non_existent_path)
        expected_log = f"Error parsing textproto or invalid config"
        polling.wait_and_verify_expected_logs(
            sdv_device=self.device.adb(),
            grep_text=expected_log,
            assert_msg="Failed to find parsing error due to 'No such file error' for health configuration",
        )
        self.log_exit()

    def test_valid_config_used(self):
        self.log_enter()
        expected_period_ms = self.get_period_ms(
            self.hm_config_path_property_original_value)

        self.set_hm_config_property(
            self.hm_config_path_property_original_value)
        expected_log = f"Successfully parsed health monitor config proto. Reporting timeout is {expected_period_ms} ms"
        polling.wait_and_verify_expected_logs(
            sdv_device=self.device.adb(),
            grep_text=expected_log,
            assert_msg="Failed to find message: '" + expected_log + "'",
        )
        self.log_exit()


if __name__ == "__main__":
    sdv_test_runner.run()
