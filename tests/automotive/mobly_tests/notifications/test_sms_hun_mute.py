#  Copyright (C) 2024 The Android Open Source Project
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

import logging

from bluetooth_sms_test import bluetooth_sms_base_test
from mobly.controllers import android_device
from utilities import constants
from utilities.common_utils import CommonUtils
from utilities.main_utils import common_main


class NotificationsSMSHUNMute(
    bluetooth_sms_base_test.BluetoothSMSBaseTest
):

  def setup_class(self):
    super().setup_class()
    self.common_utils = CommonUtils(self.target, self.discoverer)

  def setup_test(self):
    logging.info("Pairing phone to car head unit.")
    self.bt_utils.pair_primary_to_secondary()
    self.call_utils.wait_with_log(constants.DEVICE_CONNECT_WAIT_TIME)
    self.common_utils.click_on_ui_element_with_text("Allow")

    logging.info("Clearing the sms from the phone.")
    self.call_utils.clear_sms_app(self.target)

    logging.info("Enabling driving mode.")
    self.call_utils.enable_driving_mode()

    logging.info("Removing mbs snippet and rebooting the phone.")
    self.target.unload_snippet('mbs')
    self.target.reboot()
    self.call_utils.wait_with_log(constants.DEVICE_CONNECT_WAIT_TIME)
    self.target.load_snippet('mbs', android_device.MBS_PACKAGE)
    # super().enable_recording()

  def test_sms_mute(self):
    """
    GIVEN a SMS HUN is displayed in the car's head unit,
    WHEN the SMS HUN is muted,
    THEN the notification should dismiss.
    """
    logging.info("Arrange: Get the phone number of phone to send the SMS.")
    target_phone_number = self.target.mbs.getPhoneNumber()
    receiver_phone_number = self.phone_notpaired.mbs.getPhoneNumber()
    sms_text = constants.SMS_TEXT

    logging.info(f"Act: Sending new SMS to {target_phone_number}")
    self.phone_notpaired.mbs.sendSms(target_phone_number, sms_text)

    logging.info("Assert: New SMS is displayed as a heads-up notification.")
    assert self.discoverer.mbs.isHUNDisplayed() is True, (
        "New SMS is not displayed as a heads-up notification."
    )

    logging.info("Act: Mute the SMS in the car's head unit.")
    self.discoverer.mbs.muteSMSHUN()

    logging.info("Assert: SMS HUN is dismissed.")
    assert self.discoverer.mbs.isHUNDisplayed() is False, (
        "SMS HUN is not dismissed."
    )

  def teardown_test(self):
    self.call_utils.press_home()
    try:
      self.call_utils.disable_driving_mode()
    except Exception as e:  # pylint: disable=broad-except
      logging.info("Failed to disable driving mode: %s", e)

    try:
      super().teardown_no_video_recording()
    except Exception as e:  # pylint: disable=broad-except
      logging.info("Failed to teardown test: %s", e)


if __name__ == '__main__':
  common_main()
