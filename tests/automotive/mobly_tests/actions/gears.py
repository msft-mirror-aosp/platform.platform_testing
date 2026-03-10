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


from spectatio_host_tf.core import test_runner
from functional_test.test_base import functional_test_base


class VhalGears(functional_test_base.FunctionalTestBaseClass):

    def test_transmission(self):
        """Shift the car to park and check the UI for the gear indicator change."""
        self.mbs.shiftToPark()
        self.mbs.shiftToReverse() # check for camera overlay here
        self.mbs.shiftToNeutral()
        self.mbs.shiftToDrive()

        # todo - cluster display's gear indicators change their "selected" property in response
        # to this.  perform ui validation that way

    def test_rpm(self):
        self.mbs.setEngineRpm("1000")
        self.asserts.assert_true(self.mbs.hasUIElementWithText("1.0"), 'RPM set')

    def test_speed(self):
        self.mbs.setVehicleSpeed("30")
        self.mbs.setVehicleSpeed("60")
        # todo - cuttlefish speed display doesn't update in response to this.  bug?

    def test_parking_brake(self):
        # cuttlefish's cluster display doesn't have a parking brake indicator anywhere, so we
        # are just testing the property persistence for now
        self.mbs.setParkingBrake("true")
        self.asserts.assert_true(self.mbs.getParkingBrake(), 'Parking brake engaged')
        self.mbs.setParkingBrake("false")
        self.asserts.assert_false(self.mbs.getParkingBrake(), 'Parking brake disengaged')


if __name__ == '__main__':
    test_runner.run()
