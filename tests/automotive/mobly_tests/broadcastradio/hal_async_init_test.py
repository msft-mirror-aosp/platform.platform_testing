#  Copyright (C) 2026 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import time
from mobly import asserts
from mobly import base_test
from mobly import test_runner
from mobly import signals
from mobly.controllers import android_device
from mobly.controllers.android_device_lib.adb import AdbTimeoutError
from mobly.controllers.android_device_lib.errors import DeviceError

MAX_EXPECTED_BOOTUP_SECONDS = 40
HAL_DELAY_MS = 50 * 1000


class BroadcastRadioHALAsyncInitTest(base_test.BaseTestClass):
    def setup_class(self):
        self.is_setup_complete = False
        self.ads = self.register_controller(android_device)
        self.dut = self.ads[0]

        build_type = self.dut.adb.shell(['getprop', 'ro.build.type']).strip().decode()

        if build_type == "user":
            raise signals.TestAbortClass(
                f"Skipping {self.__class__.__name__}: "
                f"Device is running a '{build_type}' build, but root is required."
            )

        self.dut.adb.shell(['setprop', 'persist.vendor.broadcastradio.mock.init_delay_ms', str(HAL_DELAY_MS)]) # Persistent HAL delay
        self.is_setup_complete = True


    def teardown_class(self):
        if not getattr(self, 'is_setup_complete', True):
            return

        self.dut.root_adb()  # Gain root access to modiyf the set property
        self.dut.adb.shell(['setprop', 'persist.vendor.broadcastradio.mock.init_delay_ms', '0'])

    def test_boot_completed_received(self):
        # Reboot the device and wait for it to have the property set
        self.dut.adb.reboot()

        # Wait for the device to complete booting
        self.dut.log.info("Waiting for boot completion...")
        try:
            self.dut.wait_for_boot_completion(timeout=MAX_EXPECTED_BOOTUP_SECONDS)
        except (AdbTimeoutError, DeviceError) as err:
            asserts.fail(f"Boot did not complete on time when setting a delay in BroadcastRadio HAL: {err}")
        self.dut.log.info("Boot completed successfully on time!")

if __name__ == '__main__':
    test_runner.main()

