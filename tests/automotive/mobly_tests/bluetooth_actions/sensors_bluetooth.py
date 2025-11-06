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


from bluetooth_test import bluetooth_base_test
from mobly import asserts

from utilities import constants
from utilities.main_utils import common_main


class SensorsBluetooth(bluetooth_base_test.BluetoothBaseTest):
    """Bluetooth Device Connects/Reconnects based on Seat Sensors."""

    def setup_test(self):
        """Setup  before any test is executed."""
        if self.discoverer.is_adb_root:
          self.discoverer.root_adb()

        # Pair the devices
        self.bt_utils.pair_primary_to_secondary()

    def test_bluetooth_device_connects_when_driver_seat_sensor_triggered(self):
        """Tests bluetooth device connected when driver seat sensors triggered"""
        self.discoverer.mbs.setDriverSeatSetOccupancytoVacant()
        self.call_utils.open_bluetooth_palette()
        self.call_utils.wait_with_log(5)
        asserts.assert_true(self.call_utils.is_bluetooth_connected(), 'Bluetooth is not connected')
        self.call_utils.click_bluetooth_button()
        self.call_utils.wait_with_log(5)
        asserts.assert_false(self.call_utils.is_bluetooth_connected(), 'Bluetooth is not disconnected')
        self.discoverer.mbs.setDriverSeatSetOccupancytoOccupied()
        self.call_utils.wait_with_log(5)
        asserts.assert_true(self.call_utils.is_bluetooth_connected(),
            'Bluetooth is not connected after driver seat occupancy sensors triggered')


    def test_bluetooth_device_not_connected_when_passenger_seat_sensor_triggered(self):
        """Tests bluetooth device is not connected when Passenger seat sensors triggered"""
        self.discoverer.mbs.setPassengerSeatSetOccupancytoVacant()
        self.call_utils.open_bluetooth_palette()
        self.call_utils.wait_with_log(5)
        asserts.assert_true(self.call_utils.is_bluetooth_connected(), 'Bluetooth is not Connected')
        self.call_utils.click_bluetooth_button()
        self.call_utils.wait_with_log(5)
        asserts.assert_false(self.call_utils.is_bluetooth_connected(), 'Bluetooth is not Disconnected')
        self.discoverer.mbs.setPassengerSeatSetOccupancytoOccupied()
        self.call_utils.wait_with_log(5)
        asserts.assert_false(self.call_utils.is_bluetooth_connected(),
            'Bluetooth is connected after passengers seat occupancy sensors triggered')

    def teardown_test(self):
        # Go to home screen
        self.call_utils.press_home()
        super().teardown_no_video_recording()






if __name__ == '__main__':
    common_main()
