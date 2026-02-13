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

"""SDV Service Bundles Registry (SBR) samples Test.

Verifies:

 * SBR sample apexes are mounted and .so libraries are available,
 * Lifecycle can start these .so libraries base on the service bundle name (aka
 FQIN).
"""

from mobly import asserts
import logging
from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleSBRTest(sdv_base_test.SdvBaseTestClass, parameterized.TestCase):

    SERVICE_BUNDLE_START_COMMAND = (
        'sdv_service_bundle start'
        ' local-vm:{package}.{service_bundle}/instance-1'
    )
    FILE_NOT_FOUND_MESSAGE = 'No such file or directory'
    LIFECYCLE_LOG_MESSAGE = 'lifecycle_manager:'
    SERVICE_BUNDLE_EXECUTION_REGEXP = (
        'Executing.*{service_bundle_library_full_path}'
    )
    LS_ASSERTION_FILE_NOT_FOUND_ERROR_MESSAGE = (
        '\n[FAILURE]: ls states that the file not found:'
        ' {service_bundle_library_full_path}.\n ls: output:\n\t{ls_result}'
    )
    LS_ASSERTION_FILE_LISTING_ERROR_MESSAGE = (
        '\n[FAILURE]: File {service_bundle_library_full_path} is not listed.'
        '\n ls: output:\n\t{ls_result}'
    )
    LIFECYCLE_LOG_GREP_ERROR_MESSAGE = (
        '\n[FAILURE]: Lifecycle expected to execute {service_bundle} as'
        ' {service_bundle_library_full_path}.\n logcat grep'
        ' output:{logcat_result}'
    )

    ################################################
    ##   Setup and teardown for the test suite.   ##
    ################################################

    def setup_class(self):
        """Setup test suite."""
        super().setup_class()
        self.sdv_device = self.get_device('device1')

    ################################################
    ##            Utility functions.              ##
    ################################################

    def info(self, log_msg):
        """Unified info logging for the test suit."""
        logging.info('SDV Service Bundles Registry (SBR) Test: %s', log_msg)

    def debug(self, log_msg):
        """Unified info logging for the test suit."""
        logging.debug('SDV Service Bundles Registry (SBR) Test: %s', log_msg)

    def build_apex_path(self, sample_name, library):
        """Build the full path from apex 'sample' name and 'library'."""
        return f'/apex/com.android.sdv.sample.service_bundles_registry.{sample_name}/lib64/{library}'

    ################################################
    ##           Assert-like validators.          ##
    ################################################

    def assert_service_bundle_library_exist(self, sample_name, library):
        """Validates that the specific 'library'.so file exists in the specified 'sample_name'."""
        # GIVEN the full path of the 'library'
        service_bundle_library_full_path = self.build_apex_path(
            sample_name, library
        )

        # WHEN attempting to listing the library files
        ls_result = self.sdv_device.adb().execute_shell_command(
            f'ls {service_bundle_library_full_path}'
        )

        # THEN we expect the file exists
        asserts.assert_not_in(
            self.FILE_NOT_FOUND_MESSAGE,
            ls_result,
            self.LS_ASSERTION_FILE_NOT_FOUND_ERROR_MESSAGE.format(
                service_bundle_library_full_path=service_bundle_library_full_path,
                ls_result=ls_result,
            ),
        )
        # THEN the library is listed
        asserts.assert_equal(
            service_bundle_library_full_path,
            ls_result,
            self.LS_ASSERTION_FILE_LISTING_ERROR_MESSAGE.format(
                service_bundle_library_full_path=service_bundle_library_full_path,
                ls_result=ls_result,
            ),
        )

    def assert_service_bundle_starting(
        self, service_bundle, sample_name, library
    ):
        """Validates that the 'service_bundle' is started from `sample_name` apex as provided 'library'."""
        # GIVEN the full path of the 'library'
        service_bundle_library_full_path = self.build_apex_path(
            sample_name, library
        )

        # WHEN Lifecycle starts the `service_bundle`
        self.info(
            'Requesting Lifecycle to start service bundle by name'
            f' "{service_bundle}".'
        )
        self.sdv_device.adb().execute_shell_command(
            self.SERVICE_BUNDLE_START_COMMAND.format(
                package=f'com.android.sdv.sample.service_bundles_registry.{sample_name}',
                service_bundle=service_bundle,
            )
        )

        # THEN Lifecycle executes the right binary for the `service_bundle`
        self.info(
            'Verifying that Lifecycle loads'
            f' {service_bundle_library_full_path}.'
        )
        logcat_result = self.sdv_device.adb().grep_from_logcat(
            self.LIFECYCLE_LOG_MESSAGE
        )
        asserts.assert_regex(
            logcat_result,
            self.SERVICE_BUNDLE_EXECUTION_REGEXP.format(
                service_bundle_library_full_path=service_bundle_library_full_path
            ),
            self.LIFECYCLE_LOG_GREP_ERROR_MESSAGE.format(
                service_bundle=service_bundle,
                service_bundle_library_full_path=service_bundle_library_full_path,
                logcat_result='\n  ' + logcat_result.replace('\n', '\n  '),
            ),
        )

    ################################################
    ##        Tests mounted SBR apexes.           ##
    ################################################

    @parameterized.named_parameters(
        (
            'cpp',
            'apex_with_one_service_bundle_cpp',
            'libservice_bundle_cpp_for_service_bundles_registry.so',
        ),
        (
            'rust',
            'apex_with_one_service_bundle_rust',
            'libservice_bundle_rust_for_service_bundles_registry.so',
        ),
    )
    def test_mounted_apex_with_one_service_bundle(self, sample_name, library):
        self.assert_service_bundle_library_exist(sample_name, library)

    def test_mounted_apex_with_two_service_bundles(self):
        self.assert_service_bundle_library_exist(
            'apex_with_two_service_bundles_cpp_rust',
            'libservice_bundle_cpp_for_service_bundles_registry.so',
        )
        self.assert_service_bundle_library_exist(
            'apex_with_two_service_bundles_cpp_rust',
            'libservice_bundle_rust_for_service_bundles_registry.so',
        )

    ################################################
    ##  Tests starting service bundle from apex.  ##
    ################################################

    @parameterized.named_parameters(
        (
            'cpp',
            'SampleCppForServiceBundlesRegistry',
            'apex_with_one_service_bundle_cpp',
            'libservice_bundle_cpp_for_service_bundles_registry.so',
        ),
        (
            'rust',
            'SampleRustForServiceBundlesRegistry',
            'apex_with_one_service_bundle_rust',
            'libservice_bundle_rust_for_service_bundles_registry.so',
        ),
    )
    def test_apex_with_one_service_bundle(
        self, bundle_name, sample_name, library
    ):
        self.assert_service_bundle_starting(bundle_name, sample_name, library)

    def test_apex_with_two_service_bundles(self):
        self.assert_service_bundle_starting(
            'AdditionalSampleCpp',
            'apex_with_two_service_bundles_cpp_rust',
            'libservice_bundle_cpp_for_service_bundles_registry.so',
        )
        self.assert_service_bundle_starting(
            'AdditionalSampleRust',
            'apex_with_two_service_bundles_cpp_rust',
            'libservice_bundle_rust_for_service_bundles_registry.so',
        )


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
