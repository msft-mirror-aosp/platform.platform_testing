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

from utilities import constants
from utilities.main_utils import common_main
from bluetooth_test import bluetooth_base_test


class ContactSearchByLastNameTest(bluetooth_base_test.BluetoothBaseTest):

  def setup_test(self):
    #Upload contacts to phone device
    file_path = constants.PATH_TO_CONTACTS_VCF_FILE
    #TODO- retest again after (b/308018112) is fixed.
    #contacts were loaded manually for testing
    self.call_utils.upload_vcf_contacts_to_device(self.target, file_path)

    # Pair the devices
    self.bt_utils.pair_primary_to_secondary()
    super().enable_recording()

  def test_sort_contacts_by_last_name(self):
    # Navigate to the Contacts page
    expected_last_name = constants.EXPECTED_CONTACT_LAST_NAME
    self.call_utils.open_phone_app()
    self.call_utils.open_contacts()
    self.call_utils.wait_with_log(constants.DEFAULT_WAIT_TIME_FIVE_SECS)
    self.call_utils.search_contact_by_name(
        expected_last_name
    )
    self.call_utils.verify_search_results_contain_target_search(
        expected_last_name
    )

if __name__ == '__main__':
  common_main()
