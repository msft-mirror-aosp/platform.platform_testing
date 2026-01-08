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

"""SDV Sample Apex Verification Test

Tests Apex Verification on one SDV VM
"""
from mobly import asserts
import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleApexVerificationTest(sdv_base_test.SdvBaseTestClass):

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1')
        # Apex Information
        self.APEX_PATH = '/product/apex/'
        self.APEX_FILTER = 'hello'
        self.EXPECTED_APEX_PACKAGE = 'com.sdv.google.sample.hello.apex.apex'
        self.APEX_EXECUTION_COMMAND = (
            'apex/com.sdv.google.sample.hello.apex/bin/sdv_hello_apex'
        )
        self.EXPECTED_APEX_EXECUTION_RESULT = 'Hello APEX'
        self.NOT_NONE_ASSERT_MESSAGE = '{} is null. Expected result is "{}".'
        self.EQUAL_ASSERT_MESSAGE = (
            'The Value "{}" does not match the expected result "{}".'
        )
        self.IN_ASSERT_MESSAGE = (
            'Execution result "{}" does not contain the expected string "{}".'
        )

    def test_run_sample_apex_binary(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        # Run Installed Apex
        result = self.sdv_device.adb().execute_shell_command(
            self.APEX_EXECUTION_COMMAND
        )

        # Verify Result Not Null
        asserts.assert_is_not_none(
            result,
            self.NOT_NONE_ASSERT_MESSAGE.format(
                'Execution result for installed Apex',
                self.EXPECTED_APEX_EXECUTION_RESULT,
            ),
        )

        # Verify Apex Execution Result
        asserts.assert_in(
            self.EXPECTED_APEX_EXECUTION_RESULT,
            result.strip(),
            self.IN_ASSERT_MESSAGE.format(
                result.strip(), self.EXPECTED_APEX_EXECUTION_RESULT
            ),
        )

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} completed.'
        )

    def test_apex_running_verification(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        # List installed Apex
        installed_apex = self.sdv_device.adb().execute_shell_command(
            f'ls {self.APEX_PATH} | grep {self.APEX_FILTER}'
        )

        # Verify Not Null
        asserts.assert_is_not_none(
            installed_apex,
            self.NOT_NONE_ASSERT_MESSAGE.format(
                'Installed Apex Package', self.EXPECTED_APEX_PACKAGE
            ),
        )

        # Verify Installed Package
        asserts.assert_equal(
            self.EXPECTED_APEX_PACKAGE,
            installed_apex.strip(),
            self.EQUAL_ASSERT_MESSAGE.format(
                installed_apex.strip(), self.EXPECTED_APEX_PACKAGE
            ),
        )

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} completed.'
        )


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
