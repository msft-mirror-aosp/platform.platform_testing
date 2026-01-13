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

"""SDV sample 'CUJ-Core-26' test."""

import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods

SD_AGENT_FQIN = 'google.sdv.service_discovery.discovery.IServiceDiscoveryAgent/default'
LC_AGENT_FQIN = 'google.sdv.lifecycle.ILifecycleManager/default'
CUJ26_FQIN = 'com.sdv.google.sample.someip.ServiceBundleCuj26/instance'
CUJ26_UNIT_TYPE = 'com.sdv.google.sample.someip.Cuj26SdvPayload'
CUJ26_SERVICE_UNIT = 'com-sdv-google-sample-someip-cuj26-sdv-payload-unique'

class SdvSampleCujCore26Test(sdv_base_test.SdvBaseTestClass):

    def setup_class(self):
        """Sets up the test class"""
        super().setup_class()
        self.adb_device = self.get_device("device1").adb()
        self.adb_device.wait_for_device_online()

    def _wait_and_verify_expected_logs(self, log_entry_to_find):
        WaitingMethods.wait_and_verify_expected_logs(
            self.adb_device,
            grep_text=log_entry_to_find,
            expected_result=log_entry_to_find,
            assert_msg=f'Could not find {log_entry_to_find} in logcat'
        )

    def _check_string_in_file(self, file_name, expected_result):
        file_content = self.adb_device.read_file(file_name)
        if expected_result in file_content:
            return True

    def _start_someip_tester(self):
        """Starts the sdv_vsomeip_cuj_core_026 binary as a SOME/IP source."""

        SOMEIP_CONFIG_FILE = "/vendor/etc/vsomeip/cuj_core_026_someip.json"
        SOMEIP_BASE_PATH = "/data/vendor/vsomeip/"
        SOMEIP_TESTER_COMMAND = "/vendor/bin/sdv_vsomeip_cuj_core_026"
        CHECK_STRINGS_IN_LOG_FILE = [
            'Publishing payload with size 300011',
            'CujCore26Service::onVsomeipMessage(): method=22',
            '[EVENT SUBSCRIBER] [CujCore26SdvPayload] Received payload with length 300011',
        ]

        someip_tester_log_file = self.adb_device.execute_shell_command_in_subprocess_log(
            f"VSOMEIP_CONFIGURATION={SOMEIP_CONFIG_FILE} VSOMEIP_BASE_PATH={SOMEIP_BASE_PATH} {SOMEIP_TESTER_COMMAND}"
        )

        for string_to_check in CHECK_STRINGS_IN_LOG_FILE:
            WaitingMethods.wait_for_true(
                lambda: self._check_string_in_file(someip_tester_log_file, string_to_check),
                assert_msg=f'"{string_to_check}" not found in {someip_tester_log_file}'
            )

    def _check_service_bundle_online(self, fqin, unit_type, service_unit):
        report_service_discovery_agent = self.adb_device.dumpsys(SD_AGENT_FQIN)
        report_lifecycle_manager = self.adb_device.dumpsys(LC_AGENT_FQIN)
        return (f'Service Unit: {service_unit}' in report_service_discovery_agent
            and f'FQIN: instance1:{fqin}' in report_service_discovery_agent
            and f'Unit Type: {unit_type}' in report_service_discovery_agent
            and f'STARTED     local-vm:{fqin}' in report_lifecycle_manager)

    def test_CUJ_CORE_26(self):
        """CUJ-CORE-26 test."""
        logging.info('Start test CUJ-CORE-26')

        WaitingMethods.wait_for_true(
            lambda: self._check_service_bundle_online(CUJ26_FQIN, CUJ26_UNIT_TYPE, CUJ26_SERVICE_UNIT),
            assert_msg="ServiceBundleCuj26 has not started"
        )

        self._start_someip_tester()

        self._wait_and_verify_expected_logs("CUJ26 SOME/IP payload publisher is registered")
        self._wait_and_verify_expected_logs("So far received 1 messages successfully")
        self._wait_and_verify_expected_logs("So far received 2 messages successfully")
        self._wait_and_verify_expected_logs("So far received 3 messages successfully")
        self._wait_and_verify_expected_logs("So far received 4 messages successfully")
        self._wait_and_verify_expected_logs("Message has value 42 and payload length 300000")

        logging.info('Finished test CUJ-CORE-26')

if __name__ == "__main__":
    sdv_test_runner.run()