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
from utilities.main_utils import common_main


class VhalHvac(base_test.BaseTestClass):
    def setup_class(self):
        actions_setup(self)

    def setup_test(self):
        pass

    def teardown_test(self):
        pass

    def test_ac_by_property(self):
        """Set the AC property and check that the HVAC UI reflects the setting."""
        self.main_device.mbs.showHideHvac()
        self.main_device.mbs.disableHvacAutoMode()

        self.main_device.mbs.turnOnAc()
        asserts.assert_true(self.main_device.mbs.checkAcToggle(), 'AC shows on')
        self.main_device.mbs.turnOffAc()
        asserts.assert_false(self.main_device.mbs.checkAcToggle(), 'AC shows off')

        self.main_device.mbs.showHideHvac()

    def test_ac_by_softkey(self):
        """Tap the AC UI element and check that the property reflects the change."""
        self.main_device.mbs.showHideHvac()
        self.main_device.mbs.disableHvacAutoMode()
        self.main_device.mbs.turnOffAc()

        self.main_device.mbs.clickAcToggle()
        asserts.assert_true(self.main_device.mbs.getAcState(), 'Auto mode property set')
        self.main_device.mbs.clickAcToggle()
        asserts.assert_false(self.main_device.mbs.getAcState(), 'Auto mode property cleared')

        self.main_device.mbs.showHideHvac()

    def test_recirculation_by_property(self):
        """Set the air recirculation property and check that the HVAC UI reflects the setting."""
        self.main_device.mbs.showHideHvac()
        self.main_device.mbs.disableHvacAutoMode()

        self.main_device.mbs.enableAirRecirculation()
        asserts.assert_true(self.main_device.mbs.checkRecirculationToggle(), 'Recirculation set')
        self.main_device.mbs.disableAirRecirculation()
        asserts.assert_false(self.main_device.mbs.checkRecirculationToggle(), 'Recirculation unset')

        self.main_device.mbs.showHideHvac()

    def test_recirculation_by_softkey(self):
        """Tap the air recirculation UI element and check that the property reflects the change."""
        self.main_device.mbs.showHideHvac()
        self.main_device.mbs.disableHvacAutoMode()
        self.main_device.mbs.disableAirRecirculation()

        self.main_device.mbs.clickRecirculationToggle()
        asserts.assert_true(self.main_device.mbs.getAirRecirculation(), 'Recirculation set')
        self.main_device.mbs.clickRecirculationToggle()
        asserts.assert_false(self.main_device.mbs.getAirRecirculation(), 'Recirculation unset')

    def test_defrosters_by_property(self):
        """Set the defroster properties and check that the HVAC UI reflects the setting."""
        self.main_device.mbs.showHideHvac()

        self.main_device.mbs.enableFrontDefrost()
        asserts.assert_true(self.main_device.mbs.checkFrontDefrostToggle(), 'Front defrost set')
        self.main_device.mbs.disableFrontDefrost()
        asserts.assert_false(self.main_device.mbs.checkFrontDefrostToggle(), 'Front defrost unset')

        self.main_device.mbs.enableRearDefrost()
        asserts.assert_true(self.main_device.mbs.checkRearDefrostToggle(), 'Rear defrost set')
        self.main_device.mbs.disableRearDefrost()
        asserts.assert_false(self.main_device.mbs.checkRearDefrostToggle(), 'Rear defrost unset')

        self.main_device.mbs.showHideHvac()

    def test_defrosters_by_softkey(self):
        self.main_device.mbs.showHideHvac()
        self.main_device.mbs.disableFrontDefrost()
        self.main_device.mbs.disableRearDefrost()

        self.main_device.mbs.clickFrontDefrostToggle()
        asserts.assert_true(self.main_device.mbs.getFrontDefrost(), 'Front defrost set')
        self.main_device.mbs.clickFrontDefrostToggle()
        asserts.assert_false(self.main_device.mbs.getFrontDefrost(), 'Front defrost unset')

        self.main_device.mbs.clickRearDefrostToggle()
        asserts.assert_true(self.main_device.mbs.getRearDefrost(), 'Rear defrost set')
        self.main_device.mbs.clickRearDefrostToggle()
        asserts.assert_false(self.main_device.mbs.getRearDefrost(), 'Rear defrost unset')

        self.main_device.mbs.showHideHvac()

    def test_auto_mode_by_property(self):
        """Set the auto mode property and check that the HVAC UI reflects the setting."""
        self.main_device.mbs.showHideHvac()

        self.main_device.mbs.enableHvacAutoMode()
        asserts.assert_true(self.main_device.mbs.checkAutoModeToggle(), 'Auto mode shows on')
        self.main_device.mbs.disableHvacAutoMode()
        asserts.assert_false(self.main_device.mbs.checkAutoModeToggle(), 'Auto mode shows off')

        self.main_device.mbs.showHideHvac()

    def test_auto_mode_by_softkey(self):
        """Tap the auto mode UI element and check that the property reflects the change."""
        self.main_device.mbs.showHideHvac()
        self.main_device.mbs.disableHvacAutoMode()

        self.main_device.mbs.clickAutoModeToggle()
        asserts.assert_true(self.main_device.mbs.getHvacAutoMode(), 'Auto mode property set')
        self.main_device.mbs.clickAutoModeToggle()
        asserts.assert_false(self.main_device.mbs.getHvacAutoMode(), 'Auto mode property cleared')

        self.main_device.mbs.showHideHvac()

    def test_set_driver_temp(self):
        """Set the driver temperature and check the UI for the resulting expected temp."""
        self.main_device.mbs.setDriverHvacTemperature("67")
        asserts.assert_true(self.main_device.mbs.hasUIElementWithText("67"), 'Temperature set')
        self.main_device.mbs.setDriverHvacTemperature("65")
        asserts.assert_true(self.main_device.mbs.hasUIElementWithText("65"), 'Temperature set')

    def test_set_passenger_temp(self):
        """Set the passenger temperature and check the UI for the resulting expected temp."""
        self.main_device.mbs.setPassengerHvacTemperature("69")
        asserts.assert_true(self.main_device.mbs.hasUIElementWithText("69"), 'Temperature set')
        self.main_device.mbs.setPassengerHvacTemperature("64")
        asserts.assert_true(self.main_device.mbs.hasUIElementWithText("64"), 'Temperature set')

    def test_set_driver_temp_softkey(self):
        """Click the driver side +/- buttons and check the property for the expected temp."""
        initial_temp = self.main_device.mbs.getDriverHvacTemperature()

        self.main_device.mbs.driverIncreaseTemperature()
        asserts.assert_equal(
            initial_temp + 1,
            self.main_device.mbs.getDriverHvacTemperature(),
            'Driver temperature increased'
        )
        self.main_device.mbs.driverDecreaseTemperature()
        asserts.assert_equal(
            initial_temp,
            self.main_device.mbs.getDriverHvacTemperature(),
            'Driver temperature decreased'
        )

    def test_set_passenger_temp_softkey(self):
        """Click the passenger side +/- buttons and check the property for the expected temp."""
        initial_temp = self.main_device.mbs.getPassengerHvacTemperature()

        self.main_device.mbs.passengerIncreaseTemperature()
        asserts.assert_equal(
            initial_temp + 1,
            self.main_device.mbs.getPassengerHvacTemperature(),
            'Passenger temperature increased'
        )
        self.main_device.mbs.passengerDecreaseTemperature()
        asserts.assert_equal(
            initial_temp,
            self.main_device.mbs.getPassengerHvacTemperature(),
            'Passenger temperature decreased'
        )

    def test_hvac_display_units(self):
        """Test that the temperature display changes to celsius and back to fahrenheit"""
        self.main_device.mbs.setDriverHvacTemperature("65")
        self.main_device.mbs.setHvacDisplayCelsius()
        asserts.assert_true(self.main_device.mbs.hasUIElementWithText("20.0"), 'Celsius display')
        self.main_device.mbs.setHvacDisplayFahrenheit()
        asserts.assert_true(self.main_device.mbs.hasUIElementWithText("65"), 'Fahrenheit display')

    def test_set_driver_seat_heater(self):
        """Click the seat heater buttons and check the property value."""
        self.main_device.mbs.showHideHvac()

        initial_seat_temp = self.main_device.mbs.getDriverSeatTemperature()
        for i in range(1, 4):
            self.main_device.mbs.clickDriverSeatTemperature()
            new_seat_temp = self.main_device.mbs.getDriverSeatTemperature()
            asserts.assert_equal(new_seat_temp, (initial_seat_temp + i) % 3, 'Seat temp fail')

        self.main_device.mbs.showHideHvac()

if __name__ == '__main__':
    common_main()