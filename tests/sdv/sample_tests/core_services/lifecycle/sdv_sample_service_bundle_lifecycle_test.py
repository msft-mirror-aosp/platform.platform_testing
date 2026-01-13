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

"""SDV Sample for both cpp and rust Service Bundle Lifecycle Test

Tests is on one SDV VM
"""
from mobly import asserts
import logging
from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleServiceBundleLifecycleTest(
    sdv_base_test.SdvBaseTestClass, parameterized.TestCase
):
    ASSERT_MESSAGE = (
        "Actual Result [{actual_result}] does not match Expected Result"
        " [{expected_result}]"
    )

    SERVICE_BUNDLE_FQIN = "local-vm:com.sdv.google.sample.lifecycle.apex.Lifecycle{lang}SampleServiceBundle/instance-1"

    DUMPSYS_COMMAND = "dumpsys google.sdv.lifecycle.ILifecycleManager/default | grep '{fqin}'"

    LIFECYCLE_EXECUTION_COMMAND = (
        "sdv_service_bundle {start_stop_launch_shutdown_param} "
        + "local-vm:com.sdv.google.sample.lifecycle.apex.Lifecycle{lang}SampleServiceBundle/instance-1"
    )

    def execute_lifecycle_command(self, start_stop_launch_shutdown_param, lang):
        return self.sdv_device.adb().execute_shell_command_in_subprocess(
            "server_process_" + start_stop_launch_shutdown_param + "_" + lang,
            self.LIFECYCLE_EXECUTION_COMMAND.format(
                start_stop_launch_shutdown_param=start_stop_launch_shutdown_param,
                lang=lang,
            ),
        )

    def verify_lifecycle_service(self, expected):
        expected_logcat_grep_text = (
            "lifecycle_manager: sdv_lifecycle_agent::lifecycle_manager::agent:"
            " {service_being_called} called"
        )

        actual_result = self.sdv_device.adb().grep_from_logcat(expected)
        asserts.assert_in(
            expected_logcat_grep_text.format(service_being_called=expected),
            actual_result,
            self.ASSERT_MESSAGE.format(
                actual_result=actual_result,
                expected_result=expected_logcat_grep_text.format(
                    service_being_called=expected
                ),
            ),
        )

    def verify_dumpsys_state(self, expected_state, lang):
        fqin = self.SERVICE_BUNDLE_FQIN.format(lang = lang)
        dumpsys_output = self.sdv_device.adb().execute_shell_command(self.DUMPSYS_COMMAND.format(fqin = fqin))
        service_bundle_state = dumpsys_output.split()[1]
        if service_bundle_state == expected_state:
            return

        asserts.fail(
            f"Service bundle current state {service_bundle_state} doesn't equal expected state {expected_state}"
        )


    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1")

    @parameterized.named_parameters(
        ("cpp", "Cpp"),
        ("rust", "Rust"),
    )
    def test_lifecycle_service(self, lang):
        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} started"
        )

        # Create bundle
        self.execute_lifecycle_command("create", lang)

        self.verify_lifecycle_service("launch_service()")

        self.verify_dumpsys_state("CREATED", lang)

        # Start bundle
        self.execute_lifecycle_command("start", lang)

        self.verify_lifecycle_service("start_service()")

        self.verify_dumpsys_state("STARTED", lang)

        # Stop bundle
        self.execute_lifecycle_command("create", lang)

        self.verify_lifecycle_service("stop_service()")

        self.verify_dumpsys_state("CREATED", lang)

        # Destroy bundle
        self.execute_lifecycle_command("destroy", lang)

        self.verify_lifecycle_service("shutdown_service()")

        logging.info(
            f"{self.get_suite_name()}#{self.current_test_info.name} completed."
        )


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
