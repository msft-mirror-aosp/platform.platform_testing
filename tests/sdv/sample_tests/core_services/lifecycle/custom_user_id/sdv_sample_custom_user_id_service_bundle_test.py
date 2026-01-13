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

"""SDV Sample for running service bundle with custom user id.

Tests runs on single SDV VM.
"""
from mobly import asserts
import logging
import time
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleCustomUserIdTest(
    sdv_base_test.SdvBaseTestClass
):
    ASSERT_MESSAGE = "Current process user id 7050. Expected user id to be 7050"
    ERROR_MESSAGE = '\n[FAILURE]: Current process user id message is not found'

    SERVICE_BUNDLE_FQIN = "sdv_service_bundle start local-vm:com.sdv.google.sample.lifecycle.userid.apex.LifecycleCustomUserIdSampleServiceBundle/instance-1"

    SAMPLES_LOGCAT_ARGS = "oem_service_bundle_custom_userid_sample::service:"

    def create_service_bundle(self):
        return self.sdv_device.adb().execute_shell_command(self.SERVICE_BUNDLE_FQIN)

    def wait_for_logcat(self, sdv_device, grep_text, timeout=30, poll_interval=0.1):
        """Polls the logcat output for a specific text until found or timeout.

        Args:
            grep_text: The text to search for in the logcat output.
            timeout: The maximum time (in seconds) to wait.
            poll_interval: The time (in seconds) between polls.
        Returns:
            True if grep matched at least one logcat output
            False if grep matched no logcat output within the timeout
        """
        deadline = time.perf_counter() + timeout
        while time.perf_counter() < deadline:
            logcat_grep_result = sdv_device.adb().grep_from_logcat(
                grep = grep_text)
            if logcat_grep_result != "":
                return True
            time.sleep(poll_interval)
        return False

    def assert_logcat(self, grep_text, assert_msg):
        """Assert-like validator to grep logcat with the `grep_text`.

        Args:
            grep_text: The text to search for in the logcat output.
            assert_msg: The failure assertion message.
        """
        if not self.wait_for_logcat(self.sdv_device, grep_text):
            logcat_grep_result = self.sdv_device.adb().grep_from_logcat(grep_text, self.SAMPLES_LOGCAT_ARGS)
            asserts.assert_in(
                grep_text,
                logcat_grep_result,
                assert_msg + "\n {logcat_grep_result}"
            )

    def verify_user_id(self):
        self.assert_logcat(grep_text = self.ASSERT_MESSAGE, assert_msg = self.ERROR_MESSAGE)


    def log_enter(self):
        """ Unified logging enter test suit. """
        logging.info(f"{self.get_suite_name()} :: Start test {self.current_test_info.name}")

    def log_exit(self):
        """ Unified logging exit test suit. """
        logging.info(f"{self.get_suite_name()} :: Finished test {self.current_test_info.name}")

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1")

    def test_custom_user_id(self):
        self.log_enter()

        self.create_service_bundle()
        self.verify_user_id()

        self.log_exit()


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
