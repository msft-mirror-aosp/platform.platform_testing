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

from mbs_utils import constants
from mobly import test_runner
from bluetooth_test import bluetooth_base_test


class SMSTimeSyncTest(bluetooth_base_test.BluetoothBaseTest):

    def test_sms_time_sync(self):
        logging.info("Getting the timezone from the phone")
        time_zone_device = self.call_utils.execute_shell_on_device(self.target, constants.DATE_CMD)
        time_zone_split = time_zone_device.split()
        expected_timezone = time_zone_split[4].decode("utf-8")
        logging.info("Time zone captured from the phone: %s", expected_timezone)
        logging.info("Updating time_zone: %s on auto device", constants.TIMEZONE_DICT[expected_timezone])
        self.call_utils.update_device_timezone(constants.TIMEZONE_DICT[expected_timezone])
        #TODO: Add extra verifications after sms unread tests are merged.

if __name__ == '__main__':
    # Pass test arguments after '--' to the test runner.
    # Needed for Mobly Test Runner
    # TODO: to be updated after is b/303452618 is fixed.
    if '--' in sys.argv:
        index = sys.argv.index('--')
        sys.argv = sys.argv[:1] + sys.argv[index + 1:]
    test_runner.main()
