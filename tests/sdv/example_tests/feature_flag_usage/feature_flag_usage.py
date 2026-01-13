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

"""SDV Feature Flag Usage Test"""

from mobly import asserts
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.feature_flags.sdv_feature_flag_annotation import skip_if_feature_disabled
from sdv_test_fw.test_execution.test_imports import AdbTimeoutError


class SdvFeatureFlagUsageTest(sdv_base_test.SdvBaseTestClass):
    """This test is an example of Feature Flag annotation usage"""

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1')

    @skip_if_feature_disabled("new_feature")
    def test_feature_flag_usage(self):
        try:
            self.sdv_device.adb().wait_for_device_online()
        except AdbTimeoutError:
            asserts.fail(
                f'Device <{self.sdv_device.adb().get_device_serial()}> is not online'
            )
if __name__ == '__main__':
    sdv_test_runner.run()