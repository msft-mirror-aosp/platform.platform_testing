#  Copyright (C) 2023 The Android Open Source Project
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


from utilities import constants
from mobly import asserts
from utilities.main_utils import common_main
from utilities.common_utils import CommonUtils
from bluetooth_sms_test import bluetooth_sms_base_test
from mobly.controllers import android_device


class UxRestrictionPlayUnreadMessageWhileDrivingTest(bluetooth_sms_base_test.BluetoothSMSBaseTest):

  def setup_class(self):
    super().setup_class()
    self.common_utils = CommonUtils(self.target, self.discoverer)

    # pair the devices
    self.bt_utils.pair_primary_to_secondary()

  def setup_test(self):

    # wait for user permissions popup & give contacts and sms permissions
    self.call_utils.wait_with_log(20)
    self.common_utils.click_on_ui_element_with_text('Allow')

    # Clearing the sms from the phone
    self.call_utils.clear_sms_app(self.target)

    # Reboot Phone
    self.target.unload_snippet('mbs')
    self.call_utils.reboot_device(self.target)
    self.call_utils.wait_with_log(30)
    self.target.load_snippet('mbs', android_device.MBS_PACKAGE)

    #set driving mode
    self.call_utils.enable_driving_mode()

  def test_play_new_unread_sms(self):
    # To test that new unread sms plays on HU

    # Open the sms app
    self.call_utils.open_sms_app()

    # Verify that there is no new sms currently
    self.call_utils.verify_sms_app_unread_message(False)

    # Send a new sms
    target_phone_number = self.target.mbs.getPhoneNumber()
    self.phone_notpaired.mbs.sendSms(target_phone_number,constants.SMS_TEXT)
    self.call_utils.wait_with_log(constants.BT_DEFAULT_TIMEOUT)

    # Perform the verifications
    self.call_utils.verify_sms_app_unread_message(True)
    self.call_utils.verify_sms_preview_timestamp(True)

    # Tap on Received Text message to read it aloud
    self.call_utils.tap_to_read_aloud()

    # Check whether the Assistant transcription plate displayed upon tapping
    asserts.assert_true(self.call_utils.is_assistant_sms_transcription_plate_displayed(True),
                        'Assistant SMS Transcription plate has not opened upon tapping the SMS')
    self.call_utils.is_assistant_sms_transcription_plate_displayed(True)
  def teardown_test(self):
    # Go to home screen
    self.call_utils.press_home()
    #Disable driving mode
    self.call_utils.disable_driving_mode()

  def teardown_class(self):
    super().teardown_test()

if __name__ == '__main__':
  common_main()