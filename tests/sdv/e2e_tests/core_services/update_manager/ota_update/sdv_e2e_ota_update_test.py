# Copyright (C) 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
SDV OTA Update Test
Tests OTA Update on one SDV VM
"""

from mobly import asserts
import logging
import mobly.utils as utils
import glob

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.update.update_manager_base_class import UpdateManagerBaseClass


class SdvE2EOtaUpdateTest(sdv_base_test.SdvBaseTestClass, UpdateManagerBaseClass):
    OTA_UPDATE_EXECUTION_COMMAND = 'ota_from_target_files \"{}\" ota_update.zip'
    GREP_TEXT = 'update_verifier'
    EXPECTED_GREP_RESULT = 'Booting slot 0: isSlotMarkedSuccessful=1'
    IN_ASSERT_MESSAGE = \
            'Actual result \"{}\" does not contain Expected result \"{}\".'
    TARGET_FILES_PATTERN = '**target_files**'
    LOCAL_DIR = 'dist'
    TEST_DIR_REMOTE = '//tmp//tf-workfolder*'

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1')
        self.init_update_manager_base_class(None)

    def test_ota_update(self):
        logging.info(f'{self.get_suite_name()}#{self.current_test_info.name} started')

        file_name = self.get_ota_update_path()

        logging.info('Run update command')
        # run_command requires shell=True to be set for propoer execution of the command
        ret_code, _, stderr = utils.run_command(self.OTA_UPDATE_EXECUTION_COMMAND.format(file_name), shell=True)

        asserts.assert_not_equal(ret_code, 0, f'Ota update command failed with error: {stderr}')

        self.sdv_device.adb().reboot_device_and_verify_logcat()

        actual_result = self.sdv_device.adb().grep_from_logcat(self.GREP_TEXT)
        asserts.assert_in(
            self.EXPECTED_GREP_RESULT,
            actual_result
        )
        logging.info(f'{self.get_suite_name()}#{self.current_test_info.name} completed.')

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()