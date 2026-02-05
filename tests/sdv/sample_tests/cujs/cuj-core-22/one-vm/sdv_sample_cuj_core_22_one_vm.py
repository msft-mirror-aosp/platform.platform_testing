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

"""SDV sample 'CUJ-Core-22' one VM test."""

import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods


class SdvCujCore22OneVMTest(sdv_base_test.SdvBaseTestClass):
    SAMPLES_LOGCAT_ARGS = "*:F com_android_sdv_sample_foo_ServiceBundleFoo_instance:V com_sdv_google_sample_bar_ServiceBundleBar_instance:V SdvServiceManagerServer:*"

    GREP_FOO_MESSAGE_SENT = "Sent.*FooMessage.*42"
    GREP_FOO_MESSAGE_RECEIVED = "Received.*FooMessage.*42"
    GREP_GET_FOO_REQUEST = "GetFooRequest "
    GREP_GET_FOO_RESPONSE = "GetFooResponse "

    ERROR_LOG_NOT_FOUND_TEMPLATE = "\n[FAILURE]: Expected log matching '{}' not found."


    def setup_class(self):
        """Sets up the test class"""
        super().setup_class()
        self.adb_device = self.get_device("device1").adb()
        self.adb_device.wait_for_device_online()


    def test_CUJ_CORE_22_one_VM(self):
        """CUJ-CORE-22 one VM test."""
        logging.info('Start test CUJ-CORE-22 one VM')

        logging.info("Verifying Foo message sent.")
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.adb_device,
            grep_text=self.GREP_FOO_MESSAGE_SENT,
            logcat_args=self.SAMPLES_LOGCAT_ARGS,
            assert_msg=self.ERROR_LOG_NOT_FOUND_TEMPLATE.format(
                self.GREP_FOO_MESSAGE_SENT
            ),
        )

        logging.info("Verifying Foo message received.")
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.adb_device,
            grep_text=self.GREP_FOO_MESSAGE_RECEIVED,
            logcat_args=self.SAMPLES_LOGCAT_ARGS,
            assert_msg=self.ERROR_LOG_NOT_FOUND_TEMPLATE.format(
                self.GREP_FOO_MESSAGE_RECEIVED
            ),
        )

        logging.info("Verifying GetFooRequest.")
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.adb_device,
            grep_text=self.GREP_GET_FOO_REQUEST,
            logcat_args=self.SAMPLES_LOGCAT_ARGS,
            assert_msg=self.ERROR_LOG_NOT_FOUND_TEMPLATE.format(
                self.GREP_GET_FOO_REQUEST
            ),
        )

        logging.info("Verifying GetFooResponse.")
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device=self.adb_device,
            grep_text=self.GREP_GET_FOO_RESPONSE,
            logcat_args=self.SAMPLES_LOGCAT_ARGS,
            assert_msg=self.ERROR_LOG_NOT_FOUND_TEMPLATE.format(
                self.GREP_GET_FOO_RESPONSE
            ),
        )

        logging.info('Finished test CUJ-CORE-22 one VM')


if __name__ == "__main__":
    sdv_test_runner.run()
