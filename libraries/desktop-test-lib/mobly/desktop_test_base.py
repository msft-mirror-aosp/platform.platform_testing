# Copyright (C) 2025 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
import sys
import logging
import enum
from typing import Any, Dict, List, Optional

from mobly import base_instrumentation_test
from mobly import asserts
from mobly.controllers import android_device
from mobly.records import TestResult
from mobly.signals import TestSkip, TestError, TestFailure

from chromiumos.test.lab.api import pasit_host_pb2
from lib.pasit.passport import passport_host
from lib.pasit.android import android_pasit_device

# Set up logging for the module
_LOG = logging.getLogger(__name__)


# LINT.IfChange
class TestOption(enum.Enum):
    """Test option, passed to instrumentation tests module."""
    HOST_DRIVEN_TEST = 'HOST_DRIVEN_TEST'
    KEEP_PERIPHERALS_AFTER_TEST = 'KEEP_PERIPHERALS_AFTER_TEST'
    KEEP_PERIPHERALS_BEFORE_TEST = 'KEEP_PERIPHERALS_BEFORE_TEST'
    ALLOW_DISABLING_DISPLAYS = 'ALLOW_DISABLING_DISPLAYS'
    ENABLE_MANUAL = 'ENABLE_MANUAL'
    ENABLE_AUTOMATED = 'ENABLE_AUTOMATED'
# LINT.ThenChange(../src/platform/test/desktop/interactive/DesktopTestOptionsProvider.kt)


class OverallResult(enum.Enum):
    """Overall result of a single mobly test."""
    PASS = 'PASS'
    ERROR = 'ERROR'
    FAIL = 'FAIL'
    SKIP = 'SKIP'


class DesktopTestBase(base_instrumentation_test.BaseInstrumentationTestClass):
    """Base class for Mobly tests that wrap and execute JUnit/Android Instrumentation tests.

    This class provides utility methods to install an APK and run specific
    instrumentation tests on a registered Android device.
    """

    DEFAULT_APK_PATH = os.path.dirname(os.path.abspath(sys.argv[0]))

    # Class attributes to be set by subclasses or by set_apk_info
    apk_name: Optional[str] = None
    package: Optional[str] = None
    runner: str = 'androidx.test.runner.AndroidJUnitRunner'

    # Class attributes to be initialised by setup_test() and updated by _update_overall_result()
    overall_result: OverallResult = OverallResult.PASS
    overall_details: str = ''
    overall_extras: Dict = {}
    guest_id: str = ''
    primary_user_id: str = ''

    def set_apk_info(self, apk_name: str, package: str, runner: Optional[str] = None) -> None:
        """Sets the APK and instrumentation details for the test class.

        Args:
            apk_name: The name of the APK file.
            package: The package name of the Android application.
            runner: The fully qualified class name of the test runner. Defaults to
                'androidx.test.runner.AndroidJUnitRunner'.
        """
        self.apk_name = apk_name
        self.package = package
        if runner:
            self.runner = runner

    def setup_class(self) -> None:
        """Sets up the test environment before the first test method runs.

        Registers the Android device controller and installs the APK if configured.
        """
        _LOG.info("Setup class")
        self.dut: android_device.AndroidDevice = self.register_controller(android_device)[0]
        self.primary_user_id = str(self.dut.adb.current_user_id)

        if self.apk_name is None:
            return

        # Check if package is set, otherwise installation may fail or be confusing.
        if not self.package:
            _LOG.warning('APK name is set but package name is missing. Installation may fail.')

        apk_file_path = os.path.join(self.DEFAULT_APK_PATH, self.apk_name + ".apk")
        if 'files' in self.user_params and self.apk_name in self.user_params['files']:
            apk_file_path: str = self.user_params['files'][self.apk_name][0]

        if not os.path.exists(apk_file_path):
            raise signals.TestError(
                'Unable to find APK at path "%s"', self.apk_name)

        _LOG.info('Installing APK %s from path: %s', self.apk_name, apk_file_path)
        self.dut.adb.install(['-r', '-g', apk_file_path])

        # If PASIT info is contained in configs, then set up the controller.
        self.passport_host = None
        if 'PassportHost' in self.controller_configs:
            passport_host.update_passport_host_params(
                self.user_params, self.controller_configs
            )
            self.passport_host = self.register_controller(passport_host)[0]
            self.passport_host.save_topology_diagram(self.log_path)

            # Ensure monitor exists in this testbed, else skip test.
            asserts.abort_class_if(
                    not self.passport_host.is_device_type_present(
                        pasit_host_pb2.PasitHost.Device.Type.MONITOR, None
                    ),
                    'No monitor present in testbed'
                )

    def setup_test(self) -> None:
        """Run before every mobly test."""
        self.overall_result = OverallResult.PASS
        self.results = TestResult()
        self.overall_details = ""
        self.overall_extras = {}
        if self.passport_host:
            logging.info('Plugging in external display')
            self.passport_host.activate_device_by_type(pasit_host_pb2.PasitHost.Device.Type.MONITOR)

    def teardown_test(self):
        """Teardown steps after each test is executed."""
        if self.guest_id:
            self.leave_guest_mode()
        if self.passport_host:
            self.passport_host.reset()
        super().teardown_test()

    def assert_overall_result(self):
        """Verifies the overall test status. Usually used at the end of the mobly test."""
        if self.overall_result == OverallResult.PASS:
            return
        if self.overall_result == OverallResult.SKIP:
            raise TestSkip(self.overall_details, self.overall_extras)
        if self.overall_result == OverallResult.ERROR:
            raise TestError(self.overall_details, self.overall_extras)
        if self.overall_result == OverallResult.FAIL:
            raise TestFailure(self.overall_details, self.overall_extras)

    def run_instrumentation_test(self,
            test_name: str,
            options: List[TestOption] = []) -> Dict[str, Any]:
        """Executes a wrapped JUnit test, optionally setting the test options.

        If `options` is empty or None, it runs a standard wrapped test. If `options` is provided,
        it sets the corresponding options to True.

        Args:
            test_name: The fully qualified class name or method name of the JUnit test.
            options: List of TestOption enum values.

        Returns:
            A dictionary containing the instrumentation test results.

        Raises:
            RuntimeError: If the test package name has not been set.
        """

        all_options = self._get_instrumentation_options(test_name, additional_options=options)
        _LOG.info('Running instrumentation test "%s" with options: %s', test_name, all_options)

        if not self.package:
            raise RuntimeError('Test package name has not been set.')

        super().run_instrumentation_test(
            self.dut,
            self.package,
            options=all_options,
            runner=self.runner
        )

        self._update_overall_result()

    def enter_guest_mode(self):
        _LOG.info('Creating guest user on DUT: %s', self.dut.serial)
        self.guest_id = self.dut.adb.shell(['pm', 'create-user', 'Guest', '--guest']).decode().strip().split()[-1]
        _LOG.info('Switching to guest user on DUT: %s %s -> %s', self.dut.serial, self.primary_user_id, self.guest_id)
        self.dut.adb.shell(['am', 'switch-user', '-w', self.guest_id])

    def leave_guest_mode(self):
        if not self.guest_id:
            _LOG.warning('Not in guest mode on DUT: %s', self.dut.serial)
            return
        _LOG.info('Leaving guest mode on DUT: %s %s -> %s', self.dut.serial, self.guest_id, self.primary_user_id)
        self.dut.adb.shell(['am', 'switch-user', '-w', self.primary_user_id])

        result = self.dut.adb.shell(['pm', 'remove-user', self.guest_id]).decode().strip()
        _LOG.info('Removing guest user on DUT: %s result=%s', self.dut.serial, result)
        self.guest_id = ''

    def _get_instrumentation_options(self,
            test_name: str,
            additional_options: List[TestOption]) -> Dict[str, Any]:
        """Private helper method to construct the instrumentation options dictionary.

        This method encapsulates the logic for merging user parameters with test-specific
        and required options.

        Args:
            test_name: The fully qualified class name or method name of the JUnit test.
            additional_options: A list of extra options

        Returns:
            A dictionary of options to be passed to run_instrumentation_test.
        """
        options: Dict[str, Any] = dict()
        options['class'] = test_name
        options[TestOption.HOST_DRIVEN_TEST.value] = True
        for opt in additional_options:
            options[opt.value] = True
        options.update(self.user_params)
        options.pop('files', None)
        return options

    def _update_overall_result(self):
        """Private helper method to update the overall_result of the test, based on the results
        from instrumentation test runs."""
        # Only update result if the overall result (for the current mobly test) is not 'PASS'
        if self.overall_result != OverallResult.PASS:
            return

        if self.results.skipped:
            self.overall_result = OverallResult.SKIP
            self.overall_details = self.results.skipped[-1].details
            self.overall_extras = self.results.skipped[-1].extras
        elif self.results.error:
            self.overall_result = OverallResult.ERROR
            self.overall_details = self.results.error[-1].details
            self.overall_extras = self.results.error[-1].extras
        elif self.results.failed:
            self.overall_result = OverallResult.FAIL
            self.overall_details = self.results.failed[-1].details
            self.overall_extras = self.results.failed[-1].extras
