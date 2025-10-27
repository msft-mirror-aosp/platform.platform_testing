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


class VhalGears(base_test.BaseTestClass):
    def setup_class(self):
        actions_setup(self)

    def setup_test(self):
        pass

    def teardown_test(self):
        pass

    def test_transmission(self):
        """Shift the car to park and check the UI for the gear indicator change."""
        self.main_device.mbs.shiftToPark()
        self.main_device.mbs.shiftToReverse() # check for camera overlay here
        self.main_device.mbs.shiftToNeutral()
        self.main_device.mbs.shiftToDrive()

        # todo - cluster display's gear indicators change their "selected" property in response
        # to this.  perform ui validation that way

    def test_rpm(self):
        self.main_device.mbs.setEngineRpm("1000")
        asserts.assert_true(self.main_device.mbs.hasUIElementWithText("1.0"), 'RPM set')

    def test_speed(self):
        self.main_device.mbs.setVehicleSpeed("30")
        self.main_device.mbs.setVehicleSpeed("60")
        # todo - cuttlefish speed display doesn't update in response to this.  bug?

    def test_parking_brake(self):
        # cuttlefish's cluster display doesn't have a parking brake indicator anywhere, so we
        # are just testing the property persistence for now
        self.main_device.mbs.setParkingBrake("true")
        asserts.assert_true(self.main_device.mbs.getParkingBrake(), 'Parking brake engaged')
        self.main_device.mbs.setParkingBrake("false")
        asserts.assert_false(self.main_device.mbs.getParkingBrake(), 'Parking brake disengaged')


if __name__ == '__main__':
    common_main()