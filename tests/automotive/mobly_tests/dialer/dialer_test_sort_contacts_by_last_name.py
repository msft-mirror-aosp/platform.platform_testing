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
1. Tap on Phone icon from facet rail or App launcher to launch Dialer app
2. Go to Settings under Dialer-> Contact order
3. Select Contact order with 'Last name'
4. Go to contacts and see that contact list sorted with  Lastname, Firstname
  
"""

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




class CallContactSortTest(base_test.BaseTestClass):

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


    def get_contact_names(self, vcf_path):
        """ Reads just the names from the given vcf file,
            returning them in a list of names in format '<last_name>, <first_name>'"""
        vcf_names = []

        with open(vcf_path, mode='r') as vcf_file:
            for line in vcf_file:

                 if line.startswith("N:"):
                     name_line = line.split(':')
                     # Split into first and last name
                     name_last_and_first = name_line[1].split(';')
                     name = '{0}, {1}'.format(name_last_and_first[0], name_last_and_first[1])
                     vcf_names.append(name)

        return vcf_names


    def setup_test(self):
        # Upload contacts to phone device
        file_path = constants.PATH_TO_CONTACTS_VCF_FILE
        self.call_utils.upload_vcf_contacts_to_device(self.target, file_path)

        # Pair the devices
        self.bt_utils.pair_primary_to_secondary()


    def test_sort_contacts(self):
        # Navigate to the Contacts page
        self.call_utils.open_phone_app()
        self.call_utils.open_contacts()
        self.call_utils.wait_with_log(constants.DEFAULT_WAIT_TIME_FIVE_SECS)

        first_contact = self.discoverer.mbs.getFirstContactFromContactList()

        # Sort contacts by last name directly from the contacts file
        # Contact names are reported in '<last>, <first>' format
        vcf_names = self.get_contact_names(constants.PATH_TO_CONTACTS_VCF_FILE)
        vcf_names = sorted(vcf_names)
        top_contact_by_last_name = vcf_names[0]

        asserts.assert_true(
            first_contact == top_contact_by_last_name,
            "When sorting by last name, expected %s, found %s" % (top_contact_by_last_name,
            first_contact))

    def teardown_test(self):
            # Turn Bluetooth off on both devices after test finishes.
            self.target.mbs.btDisable()
            self.discoverer.mbs.btDisable()



if __name__ == '__main__':
    # Take test args
    test_runner.main()