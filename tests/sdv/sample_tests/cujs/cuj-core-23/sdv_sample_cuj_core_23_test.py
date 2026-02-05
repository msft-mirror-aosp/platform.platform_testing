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

"""SDV sample 'CUJ-Core-23' test."""

import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

SD_AGENT_FQIN = 'google.sdv.service_discovery.discovery.IServiceDiscoveryAgent/default'
LC_AGENT_FQIN = 'google.sdv.lifecycle.ILifecycleManager/default'

CUJ23_FQIN_FOO = 'com.sdv.google.sample.someip.ServiceBundleCuj23Foo/instance'
CUJ23_FQIN_BAR = 'com.sdv.google.sample.someip.ServiceBundleCuj23Bar/instance'
CUJ23_UNIT_TYPE_INTERFACE = 'com.sdv.google.sample.someip.Cuj23SdvInterface'
CUJ23_SERVICE_UNIT_INTERFACE = 'com-sdv-google-sample-someip-cuj23-sdv-interface'
CUJ23_UNIT_TYPE_PAYLOAD = 'com.sdv.google.sample.someip.Cuj23SdvPayload'
CUJ23_SERVICE_UNIT_PAYLOAD = 'com-sdv-google-sample-someip-cuj23-sdv-payload-unique'

def _wait_and_verify_expected_logs(adb_device, log_entry_to_find):
    polling.wait_and_verify_expected_logs(
        adb_device,
        grep_text=log_entry_to_find,
        expected_result=log_entry_to_find,
        assert_msg=f'Could not find {log_entry_to_find} in logcat'
    )

class SdvSampleCujCore23Test(sdv_base_test.SdvBaseTestClass):
    def setup_class(self):
        """Sets up the test class"""
        super().setup_class()
        self.adb_device_broker = self.get_device("device1").adb()
        self.adb_device_samples = self.get_device("device2").adb()
        self.adb_device_broker.wait_for_device_online()
        self.adb_device_samples.wait_for_device_online()

    def _check_string_in_broker_file(self, file_name, expected_result):
        file_content = self.adb_device_broker.read_file(file_name)
        if expected_result in file_content:
            return True

    def _start_someip_tester(self):
        """Starts the sdv_vsomeip_cuj_core_023 binary as a SOME/IP source."""

        SOMEIP_CONFIG_FILE = "/vendor/etc/vsomeip/cuj_core_023_someip.json"
        SOMEIP_BASE_PATH = "/data/vendor/vsomeip/"
        SOMEIP_TESTER_COMMAND = "/vendor/bin/sdv_vsomeip_cuj_core_023"
        CHECK_STRINGS_IN_LOG_FILE = [
            'Publishing payload with value',
            'Sending Request to Add Ten for value 300',
            'CujCore23Service::onVsomeipMessage(): method=1600',
            '[SOME/IP Response] [CujCore23SdvPayload] Received response to Add Ten with value 310',
            'CujCore23Service::onVsomeipMessage(): method=36',
            '[EVENT SUBSCRIBER] [CujCore23SdvPayload] Received payload with value',
            '[SOME/IP Request] [CujCore23SomeIpPayload] Received request to double num with value',
        ]

        someip_tester_log_file = self.adb_device_broker.execute_shell_command_in_subprocess_log(
            f"VSOMEIP_CONFIGURATION={SOMEIP_CONFIG_FILE} VSOMEIP_BASE_PATH={SOMEIP_BASE_PATH} {SOMEIP_TESTER_COMMAND}"
        )

        for string_to_check in CHECK_STRINGS_IN_LOG_FILE:
            polling.wait_for_true(
                lambda: self._check_string_in_broker_file(someip_tester_log_file, string_to_check),
                assert_msg=f'"{string_to_check}" not found in {someip_tester_log_file}'
            )

    def _check_fqin_online(self, fqin):
        report_lifecycle_manager = self.adb_device_samples.dumpsys(LC_AGENT_FQIN)
        return (f'STARTED     local-vm:{fqin}' in report_lifecycle_manager)

    def _check_service_bundle_online(self, fqin, unit_type, service_unit):
        report_service_discovery_agent = self.adb_device_samples.dumpsys(SD_AGENT_FQIN)
        return (f'Service Unit: {service_unit}' in report_service_discovery_agent
            and f'FQIN: instance2:{fqin}' in report_service_discovery_agent
            and f'Unit Type: {unit_type}' in report_service_discovery_agent)

    def test_CUJ_CORE_23(self):
        """CUJ-CORE-23 test."""
        logging.info('Start test CUJ-CORE-23')

        #polling.wait_for_true(
        #    lambda: self._check_service_bundle_online(CUJ23_FQIN_FOO, CUJ23_FQIN_BAR, CUJ23_UNIT_TYPE_INTERFACE, CUJ23_SERVICE_UNIT_INTERFACE, CUJ23_UNIT_TYPE_PAYLOAD, CUJ23_SERVICE_UNIT_PAYLOAD),
        #    assert_msg=f"{CUJ23_SERVICE_UNIT_INTERFACE} has not started"
        #)
        polling.wait_for_true(
            lambda: self._check_fqin_online(CUJ23_FQIN_FOO),
            assert_msg=f"{CUJ23_SERVICE_UNIT_INTERFACE} is not online"
        )
        polling.wait_for_true(
            lambda: self._check_fqin_online(CUJ23_FQIN_BAR),
            assert_msg=f"{CUJ23_FQIN_BAR} is not online"
        )
        polling.wait_for_true(
            lambda: self._check_service_bundle_online(CUJ23_FQIN_FOO, CUJ23_UNIT_TYPE_INTERFACE, CUJ23_SERVICE_UNIT_INTERFACE),
            assert_msg=f"{CUJ23_SERVICE_UNIT_INTERFACE} is not online"
        )
        polling.wait_for_true(
            lambda: self._check_service_bundle_online(CUJ23_FQIN_FOO, CUJ23_UNIT_TYPE_PAYLOAD, CUJ23_SERVICE_UNIT_PAYLOAD),
            assert_msg=f"{CUJ23_SERVICE_UNIT_PAYLOAD} is not online"
        )

        self._start_someip_tester()

        _wait_and_verify_expected_logs(self.adb_device_samples, 'Received request for AddTen method from Bundle SomeIpBroker. Request:')
        _wait_and_verify_expected_logs(self.adb_device_samples, 'Sending request to double value')
        _wait_and_verify_expected_logs(self.adb_device_samples, 'Received response')

        logging.info('Finished test CUJ-CORE-23')

if __name__ == "__main__":
    sdv_test_runner.run()