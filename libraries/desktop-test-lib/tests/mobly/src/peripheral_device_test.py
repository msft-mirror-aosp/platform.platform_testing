# Copyright (C) 2025 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import logging
import unittest
from typing import Any, Dict, List

from desktop_test_lib.mobly.desktop_test_base import DesktopTestBase
from desktop_test_lib.mobly.desktop_test_base import TestOption

from chromiumos.test.lab.api import pasit_host_pb2



# Set up logging for the module
_LOG = logging.getLogger(__name__)


class PeripheralDeviceTest(DesktopTestBase, unittest.TestCase):
    """Verifies peripheral device functionality, including tests that require a device reboot.

    Inherits from DesktopTestBase for instrumentation test execution and
    unittest.TestCase for assertions and structure.
    """

    # The Mobly framework typically passes a dictionary of configurations.
    def __init__(self, configs: Dict[str, Any]) -> None:
        """Initializes the test class and sets the APK details.

        Args:
            configs: The Mobly test configuration dictionary.
        """
        super().__init__(configs)
        # Use the updated and more descriptive method name from the base class
        # assuming 'DesktopTestLibTests' is the file name containing the APK/tests.
        self.set_apk_info(
            apk_name='DesktopTestLibTests',
            package='platform.test.desktop.tests'
        )

    def setup_class(self):
        """Setup steps before any test is executed."""
        super().setup_class()
        self._run_type = self.user_params.get("run_type", TestOption.ENABLE_AUTOMATED)

    def test_reboot_with_physical_display(self) -> None:
        """Tests display state before and after a device reboot.

        The test ensures that display (physical) is
        correctly connected across a full device reboot cycle.
        """
        # Define the full test names for clarity and reuse
        test_before_reboot: str = (
            'platform.test.desktop.PeripheralDeviceTest#testPhysicalDisplay'
        )
        test_after_reboot: str = (
            'platform.test.desktop.PeripheralDeviceTest#testPhysicalDisplay_afterReboot'
        )

        # 1. Run the pre-reboot test
        _LOG.info('Starting before-reboot test: %s', test_before_reboot)
        self.run_instrumentation_test(test_before_reboot, [TestOption.KEEP_PERIPHERALS_AFTER_TEST])
        self.assert_overall_result()

        # 2. Execute the device reboot
        _LOG.info('Executing device reboot on DUT: %s', self.dut.serial)
        self.dut.reboot()
        _LOG.info('Device reboot successful.')

        # 3. Run the post-reboot test
        _LOG.info('Starting after-reboot test: %s', test_after_reboot)
        self.run_instrumentation_test(test_after_reboot, [TestOption.KEEP_PERIPHERALS_BEFORE_TEST])
        self.assert_overall_result()

    def test_reboot_with_physical_or_simulated_display(self) -> None:
        """Tests display state before and after a device reboot.

        The test ensures that display (physical or simulated) is
        correctly connected across a full device reboot cycle.
        """
        # Define the full test names for clarity and reuse
        test_before_reboot: str = (
            'platform.test.desktop.PeripheralDeviceTest#testPhysicalOrSimulatedDisplay'
        )
        test_after_reboot: str = (
            'platform.test.desktop.PeripheralDeviceTest#testPhysicalOrSimulatedDisplay_afterReboot'
        )

        # 1. Run the pre-reboot test
        _LOG.info('Starting before-reboot test: %s', test_before_reboot)
        self.run_instrumentation_test(test_before_reboot, [TestOption.KEEP_PERIPHERALS_AFTER_TEST])
        self.assert_overall_result()

        # 2. Execute the device reboot
        _LOG.info('Executing device reboot on DUT: %s', self.dut.serial)
        self.dut.reboot()
        _LOG.info('Device reboot successful.')

        # 3. Run the post-reboot test
        _LOG.info('Starting after-reboot test: %s', test_after_reboot)
        self.run_instrumentation_test(test_after_reboot, [TestOption.KEEP_PERIPHERALS_BEFORE_TEST])

        self.assert_overall_result()

    def test_with_user_switch(self) -> None:
        """Tests user switch"""
        # Define the full test names
        test_before_user_switch: str = (
            'platform.test.desktop.PeripheralDeviceTest#testExample_beforeUserSwitch'
        )
        test_after_user_switch: str = (
            'platform.test.desktop.PeripheralDeviceTest#testExample_afterUserSwitch'
        )

        # 1. Run the test before user-switch
        _LOG.info('Starting before-user-switch test: %s', test_before_user_switch)
        self.run_instrumentation_test(test_before_user_switch, [TestOption.KEEP_PERIPHERALS_AFTER_TEST])

        # 2. Execute the user switch
        self.enter_guest_mode()
        self.leave_guest_mode()

        # 4. Run the test after user switch
        _LOG.info('Starting after-user-switch test: %s', test_after_user_switch)
        self.run_instrumentation_test(test_after_user_switch, [TestOption.KEEP_PERIPHERALS_BEFORE_TEST])

        self.assert_overall_result()


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
        return super().run_instrumentation_test(
            test_name,
            options + [self._run_type]
        )
