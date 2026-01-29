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

"""SDV Sample ConCal Server And Client Test"""

from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods


class SdvSampleConCalServerAndClientTest(sdv_base_test.SdvBaseTestClass):

    ASSERT_ERROR_MESSAGE = (
        '[{actual_result}] is not one of the expected results'
        ' [{expected_result}]'
    )
    SERVICE_EXPECTED_LOG = ['ConCal service bundle is running']
    CLIENT_EXPECTED_LOG = [
        "Initialised MW ConCal RPC bindings.",
        "Registering config:",
        "RearViewCamera \\{",
        "    model: \\\"model 1\\\",",
        "    horizontal_resolution: 720,",
        "    vertical_resolution: 720,",
        "    x_axis_field_of_view: 70.0,",
        "    y_axis_field_of_view: 70.0,",
        "    is_rgb: false,",
        "    special_fields: SpecialFields \\{",
        "        unknown_fields: UnknownFields \\{",
        "            fields: None,",
        "        \\},",
        "        cached_size: CachedSize \\{",
        "            size: 0,",
        "        \\},",
        "    \\},",
        "\\}",
        "",
        "Applying the following overrides:",
        "\\[",
        "    ConfigOverride \\{",
        "        override_id: \\\"1\\\",",
        "        config_id: MessageField(",
        "            Some(",
        "                ConfigId \\{",
        "                    service_fqin: MessageField(",
        "                        Some(",
        "                            ServiceFqin \\{",
        "                                vm_name: \\\"instance1\\\",",
        "                                package_name: \\\"com.sdv.oem.sample.concal\\\",",
        "                                service_name: \\\"SampleOemConCalClientServiceBundle\\\",",
        "                                instance_name: \\\"sample\\\",",
        "                                special_fields: SpecialFields \\{",
        "                                    unknown_fields: UnknownFields \\{",
        "                                        fields: None,",
        "                                    \\},",
        "                                    cached_size: CachedSize \\{",
        "                                        size: 0,",
        "                                    \\},",
        "                                \\},",
        "                            \\},",
        "                        ),",
        "                    ),",
        "                    config_name: \\\"config\\\",",
        "                    special_fields: SpecialFields \\{",
        "                        unknown_fields: UnknownFields \\{",
        "                            fields: None,",
        "                        \\},",
        "                        cached_size: CachedSize \\{",
        "                            size: 0,",
        "                        \\},",
        "                    \\},",
        "                \\},",
        "            ),",
        "        ),",
        "        pairs: \\[",
        "            ConfigOverrideKeyValue \\{",
        "                key: \\\"model\\\",",
        "                value: Some(",
        "                    ValueTxtproto(",
        "                         \\\"\\\\\\\\\\\"model 2\\\\\\\\\\\"\\\",",
        "                    ),",
        "                ),",
        "                special_fields: SpecialFields \\{",
        "                    unknown_fields: UnknownFields \\{",
        "                        fields: None,",
        "                    \\},",
        "                    cached_size: CachedSize \\{",
        "                        size: 0,",
        "                    \\},",
        "                \\},",
        "            \\},",
        "            ConfigOverrideKeyValue \\{",
        "                key: \\\"horizontal_resolution\\\",",
        "                value: Some(",
        "                    ValueTxtproto(",
        "                        \\\"1920\\\",",
        "                    ),",
        "                ),",
        "                special_fields: SpecialFields \\{",
        "                    unknown_fields: UnknownFields \\{",
        "                        fields: None,",
        "                    \\},",
        "                    cached_size: CachedSize \\{",
        "                        size: 0,",
        "                    \\},",
        "                \\},",
        "            \\},",
        "            ConfigOverrideKeyValue \\{",
        "                key: \\\"vertical_resolution\\\",",
        "                value: Some(",
        "                    ValueTxtproto(",
        "                        \\\"1080\\\",",
        "                    ),",
        "                ),",
        "                special_fields: SpecialFields \\{",
        "                    unknown_fields: UnknownFields \\{",
        "                        fields: None,",
        "                    \\},",
        "                    cached_size: CachedSize \\{",
        "                        size: 0,",
        "                    \\},",
        "                \\},",
        "            \\},",
        "            ConfigOverrideKeyValue \\{",
        "                key: \\\"x_axis_field_of_view\\\",",
        "                value: Some(",
        "                    ValueTxtproto(",
        "                        \\\"90.0\\\",",
        "                    ),",
        "                ),",
        "                special_fields: SpecialFields \\{",
        "                    unknown_fields: UnknownFields \\{",
        "                        fields: None,",
        "                    \\},",
        "                    cached_size: CachedSize \\{",
        "                        size: 0,",
        "                    \\},",
        "                \\},",
        "            \\},",
        "            ConfigOverrideKeyValue \\{",
        "                key: \\\"y_axis_field_of_view\\\",",
        "                value: Some(",
        "                    ValueTxtproto(",
        "                        \\\"90.0\\\",",
        "                    ),",
        "                ),",
        "                special_fields: SpecialFields \\{",
        "                    unknown_fields: UnknownFields \\{",
        "                        fields: None,",
        "                    \\},",
        "                    cached_size: CachedSize \\{",
        "                        size: 0,",
        "                    \\},",
        "                \\},",
        "            \\},",
        "            ConfigOverrideKeyValue \\{",
        "                key: \\\"is_rgb\\\",",
        "                value: Some(",
        "                    ValueTxtproto(",
        "                        \\\"true\\\",",
        "                    ),",
        "                ),",
        "                special_fields: SpecialFields \\{",
        "                    unknown_fields: UnknownFields \\{",
        "                        fields: None,",
        "                    \\},",
        "                    cached_size: CachedSize \\{",
        "                        size: 0,",
        "                    \\},",
        "                \\},",
        "            \\},",
        "        \\],",
        "        special_fields: SpecialFields \\{",
        "            unknown_fields: UnknownFields \\{",
        "                fields: None,",
        "            \\},",
        "            cached_size: CachedSize \\{",
        "                size: 0,",
        "            \\},",
        "        \\},",
        "    \\},",
        "\\]",
        "",
        "Modified configuration:",
        "RearViewCamera \\{",
        "    model: \\\"model 2\\\",",
        "    horizontal_resolution: 1920,",
        "    vertical_resolution: 1080,",
        "    x_axis_field_of_view: 90.0,",
        "    y_axis_field_of_view: 90.0,",
        "    is_rgb: true,",
        "    special_fields: SpecialFields \\{",
        "        unknown_fields: UnknownFields \\{",
        "            fields: None,",
        "        \\},",
        "        cached_size: CachedSize \\{",
        "            size: 0,",
        "        \\},",
        "    \\},",
        "\\}",
        "",
        "Rolling back previous changes",
        "Config after rollback:",
        "RearViewCamera \\{",
        "    model: \\\"model 1\\\",",
        "    horizontal_resolution: 720,",
        "    vertical_resolution: 720,",
        "    x_axis_field_of_view: 70.0,",
        "    y_axis_field_of_view: 70.0,",
        "    is_rgb: false,",
        "    special_fields: SpecialFields \\{",
        "        unknown_fields: UnknownFields \\{",
        "            fields: None,",
        "        \\},",
        "        cached_size: CachedSize \\{",
        "            size: 0,",
        "        \\},",
        "    \\},",
        "\\}",
    ]

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()
        self.sdv_device.root_device()

        # Save the current value of sdv.authz.enable
        self.sdv_authz_enable_value = self.sdv_device.prop.get(
            SdvDeviceProperty.AUTHZ_ENABLE)

        # Enforce SDV Comm Stack authorization
        self.sdv_device.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, "true")

    def teardown_class(self):
        # Reset SDV Comm Stack authorization
        self.sdv_device.prop.set(
            SdvDeviceProperty.AUTHZ_ENABLE, self.sdv_authz_enable_value)
        super().teardown_class()

    def _wait_and_verify_expected_logs(self, log_entries_to_find):
        """Waits for and verifies a sequence of log entries."""
        for log_entry in log_entries_to_find:
            # Use a lambda function to define the check that wait_for_true
            # will poll. The check returns True if grep finds the log entry.
            WaitingMethods.wait_for_true(
                func=lambda: len(
                    self.sdv_device.grep_from_logcat(log_entry)
                ) != 0,
                assert_msg=f'Could not find "{log_entry}" in logcat'
            )

    def test_concal_service_client(self):
        self.sdv_device.prop.set(SdvDeviceProperty.ORCHESTRATOR_CONFIG_PATH,
                                 '/etc/orch/vm_concal_sample_orch_config.textproto')
        self.sdv_device.reboot_device()
        self.sdv_device.wait_for_device_online()

        self._wait_and_verify_expected_logs(self.SERVICE_EXPECTED_LOG)
        self._wait_and_verify_expected_logs(self.CLIENT_EXPECTED_LOG)


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
