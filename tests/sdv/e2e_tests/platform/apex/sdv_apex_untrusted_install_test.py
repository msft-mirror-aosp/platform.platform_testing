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

"""SDV APEX Untrusted Install Test"""

from mobly import asserts
import glob
import logging
import os

from pathlib import Path

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner

from sdv_test_fw.update.update_manager_client import UpdateManagerClient

class SdvApexUntrustedInstallTest(sdv_base_test.SdvBaseTestClass):
    # An earlier version of the apex is pre-installed on the system
    # configured in make file.
    APEX_FILE_NAME_PREINSTALLED = 'com.android.sdv.test.preinstalled_untrusted_v2.apex'
    # It's guaranteed that there is no earlier version of the apex or
    # the corresponding public key pre-installed on the system because
    # neither the apex nor the key is 'installable'.
    APEX_FILE_NAME_BRAND_NEW = 'com.android.sdv.test.untrusted_brand_new.apex'
    APEX_DEVICE_PATH_PREINSTALLED = '/data/test/' + APEX_FILE_NAME_PREINSTALLED
    APEX_DEVICE_PATH_BRAND_NEW = '/data/test/' + APEX_FILE_NAME_BRAND_NEW

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()
        self.update_client = UpdateManagerClient(self.sdv_device)

    def teardown_test(self):
        self.update_client.rollback()
        super().teardown_test()

    def test_untrusted_brand_new_apex_install_fails(self):
        # Pushes APEX file from host to device
        apex_path = self._find_apex(f'**/{self.APEX_FILE_NAME_BRAND_NEW}')
        self.sdv_device.push([apex_path, self.APEX_DEVICE_PATH_BRAND_NEW])

        # Stages the APEX
        res = self.update_client.prepare_service_bundle_update([self.APEX_DEVICE_PATH_BRAND_NEW], 2)
        # The error message covers both brand-new apex case (unverified) and pre-installed apex case (not pre-installed).
        asserts.assert_in('No preinstalled apex found for unverified package com.android.sdv.test.untrusted_brand_new', res)

    def test_untrusted_v2_apex_update_fails(self):
        # Pushes APEX file from host to device
        apex_path = self._find_apex(f'**/{self.APEX_FILE_NAME_PREINSTALLED}')
        self.sdv_device.push([apex_path, self.APEX_DEVICE_PATH_PREINSTALLED])

        # Stages the APEX
        res = self.update_client.prepare_service_bundle_update([self.APEX_DEVICE_PATH_PREINSTALLED], 2)
        asserts.assert_in('public key doesn\'t match the pre-installed one', res)

    def _find_apex(self, pattern):
        if self.user_params.get('env') == 'local':
            android_root = os.environ.get('ANDROID_BUILD_TOP')
            search_path = f'{android_root}/out/host/{pattern}'
            logging.debug(f'Search path (local): {search_path}')
            file_names = glob.glob(search_path, recursive=True)
        else:
            search_path = f'{self._get_test_dir()}/{pattern}'
            logging.debug(f'Search path (CI): {search_path}')
            file_names = glob.glob(search_path, recursive=True)

        if not file_names:
            raise Exception(f'Failed to find APEX file {pattern}')

        apex_path = file_names[0]
        logging.debug(f'Using APEX found at {apex_path}')
        return apex_path

    def _get_test_dir(self):
        log_path = Path(self.sdv_device.log_path())
        logging.debug(f'Using log path to determine test dir: {log_path}')
        # This returns the first two directories eg. //tmp/tf-workdir...
        test_dir = Path().joinpath(*log_path.parts[:3])
        return str(test_dir)

if __name__ == '__main__':
  sdv_test_runner.run()
