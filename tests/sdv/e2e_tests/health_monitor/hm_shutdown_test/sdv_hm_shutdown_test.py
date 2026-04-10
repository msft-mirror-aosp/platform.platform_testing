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

"""
SDV HM Shutdown test: tests that no critical agents are reported as dead during a graceful shutdown.
"""

import os
from mobly import asserts, signals
import collections
import logging
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from vpm.sdv_vpm import SdvVpm


class SdvHmShutdownTest(sdv_base_test.SdvBaseTestClass):
    """
    Test class for verifying Health Monitor behavior during VM shutdown.
    """

    FORBIDDEN_LOG_MESSAGE = (
        "sdv_health_monitor::agent_monitor: Critical agent has died"
    )

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()
        self.device_vpm = SdvVpm(self.sdv_device)

        self.sdv_authz_enable_value = self.sdv_device.prop.get(
            SdvDeviceProperty.AUTHZ_ENABLE)
        self.sdv_device.prop.set(
            SdvDeviceProperty.AUTHZ_ENABLE, "permissions_only")

    def teardown_class(self):
        # Manually destroy controllers. This is necessary because the device
        # goes offline during the test, which prevents the default Mobly
        # controller cleanup mechanism from executing correctly.
        for name, module in self._controller_manager._controller_modules.items():
            logging.debug("Destroying %s.", name)
            try:
                if name in self._controller_manager._controller_objects:
                    module.destroy(
                        self._controller_manager._controller_objects[name]
                    )
            except Exception as e:
                logging.exception(e)
        self._controller_manager._controller_objects = (
            collections.OrderedDict()
        )
        self._controller_manager._controller_modules = {}
        super().teardown_class()

    def test_no_critical_agents_die_is_reported_on_shutdown(self):
        """Verifies critical agents are not reported as dead during shutdown."""
        self.sdv_device.wait_for_device_online()
        self.sdv_device.clear_logcat()

        try:
            # Get the host-side logcat path BEFORE shutting down the device.
            # The file path remains valid even after the device is offline.
            log_dir = self.sdv_device.log_path()

            # shutdown_vm waits for device to go offline. This ensures Mobly's
            # logcat capture for this device has stopped.
            logging.info("Initiating VM shutdown. The device will go offline.")
            self.device_vpm.shutdown_vm()

            asserts.assert_true(os.path.isdir(
                log_dir), f"Log directory does not exist on the host machine: {log_dir}")

            # There should be one .txt file containing the logcat
            log_files = os.listdir(log_dir)
            asserts.assert_equal(len(
                log_files), 1, f"Expected 1 log file, but found {len(log_files)} in {log_dir}")

            logcat_filename = log_files[0]
            logcat_path = os.path.join(log_dir, logcat_filename)

            logging.info(f"Reading logcat from host file: {logcat_path}")
            with open(logcat_path, "r", encoding="utf-8") as f:
                full_log = f.read()

            asserts.assert_not_in(self.FORBIDDEN_LOG_MESSAGE,
                                  full_log,
                                  "Found forbidden 'Critical agent has died' log during shutdown.")
        except signals.TestAbortSignal as e:
            # Re-raising the abort signal is important for Mobly's runner.
            raise e


if __name__ == "__main__":
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
