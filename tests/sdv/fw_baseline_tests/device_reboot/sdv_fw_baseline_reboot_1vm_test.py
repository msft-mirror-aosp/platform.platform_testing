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

"""SDV One VM Reboot Test"""

from reboot_common import SdvRebootCommon
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvFWBaselineOneDeviceRebootTest(
    sdv_base_test.SdvBaseTestClass,
    SdvRebootCommon,
):

    def setup_class(self):
        super().setup_class()
        self.adb_device = self.get_device('device1').adb()

    def test_reboot_device_and_verify_logcat(self):
        self.reboot_and_verify_logcat_for_all_devices()

if __name__ == '__main__':
    sdv_test_runner.run()
