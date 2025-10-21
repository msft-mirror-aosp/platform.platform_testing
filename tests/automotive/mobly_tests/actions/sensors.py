#  Copyright (C) 2025 The Android Open Source Project
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


from actions_common import actions_setup
from mobly import asserts, base_test
from mobly.controllers import android_device
from mobly.controllers.android_device_lib.snippet_client_v2 import Config
from utilities.main_utils import common_main, get_test_args


class VhalSensors(base_test.BaseTestClass):
    def setup_class(self):
        actions_setup(self)

    def setup_test(self):
        pass

    def teardown_test(self):
        pass

    def test_night_mode(self):
        """Turn night mode on and off and check that the HUD responds."""
        self.main_device.mbs.setNightMode("true")
        # TODO: check screenshot against night mode golden

        self.main_device.mbs.setNightMode("false")
        # TODO: check screenshot against day mode golden


if __name__ == '__main__':
    common_main()