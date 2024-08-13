"""
  Copyright (C) 2023 The Android Open Source Project

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.



  Test Steps:
1. Call to Paired Mobile Device from remote phone call
2. Verify that HUN appears to 'Accept' or 'Decline' the call
3. Tap 'Decline' to end  the call

"""

from bluetooth_sms_test import bluetooth_sms_base_test
import logging
from utilities import constants
from utilities import phone_device_utils
from utilities.main_utils import common_main

from mobly import asserts

class BTDialerDeclineCall(bluetooth_sms_base_test.BluetoothSMSBaseTest):

    def setup_class(self):
        super().setup_class()
        self.phone_utils = (phone_device_utils.PhoneDeviceUtils(self.phone_notpaired))

    def setup_test(self):
        # Pair the devices
        self.bt_utils.pair_primary_to_secondary()
        super().enable_recording()


    def test_reject_incoming_call(self):
        # setup phones
        # call from the unpaired phone to the paired phone
        callee_number = self.target.mbs.getPhoneNumber()
        self.phone_utils.call_number_from_home_screen(callee_number)
        self.call_utils.wait_with_log(constants.DEFAULT_WAIT_TIME_FIFTEEN_SECS)

        # Confirm the 'Decline' call button is onscreen
        asserts.assert_true(
            self.discoverer.mbs.hasUIElementWithText(constants.DECLINE_CALL_TEXT),
            "Expected \'Decline\' button to be in HU while call was incoming, but found none.")

        # reject the call
        self.discoverer.mbs.clickUIElementWithText(constants.DECLINE_CALL_TEXT)
        self.call_utils.wait_with_log(constants.WAIT_FOR_LOAD)

        # Confirm that no call is ongoing
        asserts.assert_false(
            self.discoverer.mbs.isOngoingCallDisplayedOnHome(),
            "Expected no ongoing call to be displayed on home after Declining call, but found one."
        )

    def teardown_test(self):
        # End call if test failed
        self.call_utils.end_call_using_adb_command(self.phone_notpaired)
        super().teardown_test()

if __name__ == '__main__':
    common_main()