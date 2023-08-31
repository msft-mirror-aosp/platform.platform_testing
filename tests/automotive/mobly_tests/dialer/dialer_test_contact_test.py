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


import sys
import logging
import pprint

from mobly import asserts
from mobly import base_test
from mobly import test_runner
from mobly.controllers import android_device

from mbs_utils import constants
from mbs_utils import spectatio_utils
from mbs_utils import bt_utils

# Number of seconds for the target to stay discoverable on Bluetooth.
DISCOVERABLE_TIME = 60

class CallContactTest(base_test.BaseTestClass):

    def setup_class(self):
        # Registering android_device controller module, and declaring that the test
        # requires at least two Android devices.
        self.ads = self.register_controller(android_device, min_number=2)
        # The device used to discover Bluetooth devices.
        self.discoverer = android_device.get_device(
            self.ads, label='auto')
        # Sets the tag that represents this device in logs.
        self.discoverer.debug_tag = 'discoverer'
        # The device that is expected to be discovered
        self.target = android_device.get_device(self.ads, label='phone')
        self.target.debug_tag = 'target'

        self.target.load_snippet('mbs', android_device.MBS_PACKAGE)
        self.discoverer.load_snippet('mbs', android_device.MBS_PACKAGE)

        self.call_utils = (spectatio_utils.CallUtils(self.discoverer))

        self.bt_utils = (bt_utils.BTUtils(self.discoverer, self.target))

    def setup_test(self):
        # Upload contacts to phone device
        file_path = constants.PATH_TO_CONTACTS_VCF_FILE
        self.call_utils.upload_vcf_contacts_to_device(self.target, file_path)
        # TODO: Should this be included in spectiatio_utils, rather than a separate call?

        # Pair the devices
        self.bt_utils.pair_primary_to_secondary()

    def test_call_contact(self):
        # Navigate to the Contacts page
        # If you do this, you win!
        self.call_utils.open_phone_app()
        self.call_utils.open_contacts()
        self.call_utils.wait_with_log(constants.DEFAULT_WAIT_TIME_FIVE_SECS)

        contactToCall = self.discoverer.mbs.getFirstContactFromContactList()
        logging.info("Attempting to call contact: %s", contactToCall)

        self.discoverer.mbs.callContact(contactToCall)

        # end_call() acts as an automatic verifying that a call is underway
        # since end_call() will throw an exception if no end_call button is available.
        self.call_utils.end_call()
        self.call_utils.wait_with_log(constants.WAIT_TWO_SECONDS)

    def teardown_test(self):
        # Turn Bluetooth off on both devices after test finishes.
        self.target.mbs.btDisable()
        self.discoverer.mbs.btDisable()


if __name__ == '__main__':
    # Take test args
    test_runner.main()