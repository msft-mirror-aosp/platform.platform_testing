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


from spectatio_host_tf.core import test_base, test_runner


class VhalHvac(test_base.SpectatioHostBaseTestClass):
    def setup_class(self):
        super().setup_class()
        self.mbs = self.device1.load_bundled_snippets()
        self.device1.adb.root()

    def setup_test(self):
        pass

    def teardown_test(self):
        pass

    def test_ac_by_property(self):
        """Set the AC property and check that the HVAC UI reflects the setting."""
        self.mbs.showHideHvac()
        self.mbs.disableHvacAutoMode()

        self.mbs.turnOnAc()
        self.asserts.assert_true(self.mbs.checkAcToggle(), 'AC shows on')
        self.mbs.turnOffAc()
        self.asserts.assert_false(self.mbs.checkAcToggle(), 'AC shows off')

        self.mbs.showHideHvac()

    def test_ac_by_softkey(self):
        """Tap the AC UI element and check that the property reflects the change."""
        self.mbs.showHideHvac()
        self.mbs.disableHvacAutoMode()
        self.mbs.turnOffAc()

        self.mbs.clickAcToggle()
        self.asserts.assert_true(self.mbs.getAcState(), 'Auto mode property set')
        self.mbs.clickAcToggle()
        self.asserts.assert_false(self.mbs.getAcState(), 'Auto mode property cleared')

        self.mbs.showHideHvac()

    def test_recirculation_by_property(self):
        """Set the air recirculation property and check that the HVAC UI reflects the setting."""
        self.mbs.showHideHvac()
        self.mbs.disableHvacAutoMode()

        self.mbs.enableAirRecirculation()
        self.asserts.assert_true(self.mbs.checkRecirculationToggle(), 'Recirculation set')
        self.mbs.disableAirRecirculation()
        self.asserts.assert_false(self.mbs.checkRecirculationToggle(), 'Recirculation unset')

        self.mbs.showHideHvac()

    def test_recirculation_by_softkey(self):
        """Tap the air recirculation UI element and check that the property reflects the change."""
        self.mbs.showHideHvac()
        self.mbs.disableHvacAutoMode()
        self.mbs.disableAirRecirculation()

        self.mbs.clickRecirculationToggle()
        self.asserts.assert_true(self.mbs.getAirRecirculation(), 'Recirculation set')
        self.mbs.clickRecirculationToggle()
        self.asserts.assert_false(self.mbs.getAirRecirculation(), 'Recirculation unset')

    def test_defrosters_by_property(self):
        """Set the defroster properties and check that the HVAC UI reflects the setting."""
        self.mbs.showHideHvac()

        self.mbs.enableFrontDefrost()
        self.asserts.assert_true(self.mbs.checkFrontDefrostToggle(), 'Front defrost set')
        self.mbs.disableFrontDefrost()
        self.asserts.assert_false(self.mbs.checkFrontDefrostToggle(), 'Front defrost unset')

        self.mbs.enableRearDefrost()
        self.asserts.assert_true(self.mbs.checkRearDefrostToggle(), 'Rear defrost set')
        self.mbs.disableRearDefrost()
        self.asserts.assert_false(self.mbs.checkRearDefrostToggle(), 'Rear defrost unset')

        self.mbs.showHideHvac()

    def test_defrosters_by_softkey(self):
        self.mbs.showHideHvac()
        self.mbs.disableFrontDefrost()
        self.mbs.disableRearDefrost()

        self.mbs.clickFrontDefrostToggle()
        self.asserts.assert_true(self.mbs.getFrontDefrost(), 'Front defrost set')
        self.mbs.clickFrontDefrostToggle()
        self.asserts.assert_false(self.mbs.getFrontDefrost(), 'Front defrost unset')

        self.mbs.clickRearDefrostToggle()
        self.asserts.assert_true(self.mbs.getRearDefrost(), 'Rear defrost set')
        self.mbs.clickRearDefrostToggle()
        self.asserts.assert_false(self.mbs.getRearDefrost(), 'Rear defrost unset')

        self.mbs.showHideHvac()

    def test_auto_mode_by_property(self):
        """Set the auto mode property and check that the HVAC UI reflects the setting."""
        self.mbs.showHideHvac()

        self.mbs.enableHvacAutoMode()
        self.asserts.assert_true(self.mbs.checkAutoModeToggle(), 'Auto mode shows on')
        self.mbs.disableHvacAutoMode()
        self.asserts.assert_false(self.mbs.checkAutoModeToggle(), 'Auto mode shows off')

        self.mbs.showHideHvac()

    def test_auto_mode_by_softkey(self):
        """Tap the auto mode UI element and check that the property reflects the change."""
        self.mbs.showHideHvac()
        self.mbs.disableHvacAutoMode()

        self.mbs.clickAutoModeToggle()
        self.asserts.assert_true(self.mbs.getHvacAutoMode(), 'Auto mode property set')
        self.mbs.clickAutoModeToggle()
        self.asserts.assert_false(self.mbs.getHvacAutoMode(), 'Auto mode property cleared')

        self.mbs.showHideHvac()

    def test_set_driver_temp(self):
        """Set the driver temperature and check the UI for the resulting expected temp."""
        self.mbs.setDriverHvacTemperature("67")
        self.asserts.assert_true(self.mbs.hasUIElementWithText("67"), 'Temperature set')
        self.mbs.setDriverHvacTemperature("65")
        self.asserts.assert_true(self.mbs.hasUIElementWithText("65"), 'Temperature set')

    def test_set_passenger_temp(self):
        """Set the passenger temperature and check the UI for the resulting expected temp."""
        self.mbs.setPassengerHvacTemperature("69")
        self.asserts.assert_true(self.mbs.hasUIElementWithText("69"), 'Temperature set')
        self.mbs.setPassengerHvacTemperature("64")
        self.asserts.assert_true(self.mbs.hasUIElementWithText("64"), 'Temperature set')

    def test_set_driver_temp_softkey(self):
        """Click the driver side +/- buttons and check the property for the expected temp."""
        initial_temp = self.mbs.getDriverHvacTemperature()

        self.mbs.driverIncreaseTemperature()
        self.asserts.assert_equal(
            initial_temp + 1,
            self.mbs.getDriverHvacTemperature(),
            'Driver temperature increased'
        )
        self.mbs.driverDecreaseTemperature()
        self.asserts.assert_equal(
            initial_temp,
            self.mbs.getDriverHvacTemperature(),
            'Driver temperature decreased'
        )

    def test_set_passenger_temp_softkey(self):
        """Click the passenger side +/- buttons and check the property for the expected temp."""
        initial_temp = self.mbs.getPassengerHvacTemperature()

        self.mbs.passengerIncreaseTemperature()
        self.asserts.assert_equal(
            initial_temp + 1,
            self.mbs.getPassengerHvacTemperature(),
            'Passenger temperature increased'
        )
        self.mbs.passengerDecreaseTemperature()
        self.asserts.assert_equal(
            initial_temp,
            self.mbs.getPassengerHvacTemperature(),
            'Passenger temperature decreased'
        )

    def test_hvac_display_units(self):
        """Test that the temperature display changes to celsius and back to fahrenheit"""
        self.mbs.setHvacDisplayFahrenheit()
        self.mbs.setDriverHvacTemperature("65")
        self.mbs.setHvacDisplayCelsius()
        self.asserts.assert_true(self.mbs.hasUIElementWithText("20.0"), 'Celsius display')
        self.mbs.setHvacDisplayFahrenheit()
        self.asserts.assert_true(self.mbs.hasUIElementWithText("65"), 'Fahrenheit display')

    def test_set_driver_seat_heater(self):
        """Click the seat heater buttons and check the property value."""
        self.mbs.showHideHvac()

        initial_seat_temp = self.mbs.getDriverSeatTemperature()
        for i in range(1, 4):
            self.mbs.clickDriverSeatTemperature()
            new_seat_temp = self.mbs.getDriverSeatTemperature()
            self.asserts.assert_equal(new_seat_temp, (initial_seat_temp + i) % 3, 'Seat temp fail')

        self.mbs.showHideHvac()

if __name__ == '__main__':
    test_runner.run()
