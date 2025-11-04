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

"""Test of bluetooth hard keys calling
 Steps include:
        1) Precall state check on IVI device and phone devices. (OK)
        3) make a call using hard keys
        4) Assert the calling number
        5) End call on IVI device
        6) Get latest dialed number from the IVI device
        7) Assert dialed number on the IVI device same as dialed number
"""

from bluetooth_test import bluetooth_base_test
import logging
from utilities import constants
from utilities.main_utils import common_main


class BluetoothHardKeysCallingTest(bluetooth_base_test.BluetoothBaseTest):

    def setup_class(self):
        super().setup_class()
        ro_product_name = str(self.target.adb.shell(constants.GET_PRODUCT_NAME))
        self.use_dialer_simulator = constants.CF_X86_64_PHONE in ro_product_name

    def setup_test(self):
        # Pair the devices
        self.bt_utils.pair_primary_to_secondary()
        super().enable_recording()

    def test_bluetooth_hard_keys_answer_end_call(self):
        dialer_test_phone_number = "900900900"

         # call from the unpaired phone to the paired phone
        if self.use_dialer_simulator:
          # simulate incoming call
          self.target.adb.shell(constants.DIALER_SIMULATOR_INCOMING_CALL_COMMAND.format(phone_number="dialer_test_phone_number", name="Jane Doe"))

        #Tests the calling three digits number functionality
        logging.info('Calling from %s calling to %s',self.target.serial,dialer_test_phone_number)

        self.call_utils.wait_with_log(2)
        self.discoverer.mbs.pressReceiveCallKey()
        self.call_utils.wait_with_log(2)
        self.call_utils.is_ongoing_call_displayed_on_home(True)
        self.call_utils.wait_with_log(2)
        self.discoverer.mbs.pressEndCallKey()
        self.call_utils.is_ongoing_call_displayed_on_home(False)


    def teardown_test(self):
        # End call if test failed
        self.call_utils.end_call_using_adb_command(self.target)
        self.call_utils.wait_with_log(5)
        self.call_utils.press_home()
        super().teardown_test()

if __name__ == '__main__':
    common_main()
