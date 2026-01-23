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

"""SDV Brand-new APEX install test"""

from mobly import asserts
import glob
import logging
import os

from pathlib import Path

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner

from sdv_test_fw.update.update_manager_client import UpdateManagerClient

class SdvSampleBrandNewApexTest(sdv_base_test.SdvBaseTestClass):
    APEX_NAME = 'com.sdv.google.sample.brand_new_apex'
    APEX_FILE_NAME_BLOCKED = 'com.sdv.google.sample.hello.brand_new.blocked.apex'
    APEX_FILE_NAME_ALLOWED = 'com.sdv.google.sample.hello.brand_new.allowed.apex'
    APEX_DEVICE_PATH_BLOCKED = '/data/test/' + APEX_FILE_NAME_BLOCKED
    APEX_DEVICE_PATH_ALLOWED = '/data/test/' + APEX_FILE_NAME_ALLOWED

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device('device1').adb()
        self.update_client = UpdateManagerClient(self.sdv_device)

    def test_allowed_brand_new_apex_installed(self):
        # Pushes APEX file from host to device
        apex_path = self._find_apex(f'**/{self.APEX_FILE_NAME_ALLOWED}')
        self.sdv_device.push([apex_path, self.APEX_DEVICE_PATH_ALLOWED])

        # Installs the APEX which only takes effect after reboot
        self.update_client.prepare_service_bundle_update([self.APEX_DEVICE_PATH_ALLOWED], 2)
        self.update_client.activate()
        self.sdv_device.reboot_device()
        self.sdv_device.root_device()
        self.update_client.commit()

        # Verifies the APEX is mounted
        discover_result = self.sdv_device.execute_shell_command('ls /apex')
        asserts.assert_in(self.APEX_NAME, discover_result, 'The APEX failed to be mounted')

    def test_blocked_brand_new_apex_fails(self):
        # Pushes APEX from host to device
        apex_path = self._find_apex(f'**/{self.APEX_FILE_NAME_BLOCKED}')
        self.sdv_device.push([apex_path, self.APEX_DEVICE_PATH_BLOCKED])

        # Verifies the APEX fails to be staged
        prepare_result = self.update_client.prepare_service_bundle_update([self.APEX_DEVICE_PATH_BLOCKED], 2)
        asserts.assert_in('No preinstalled apex found for unverified package', prepare_result)

    def _get_apex_version(self, device, apex_name):
        active_apex_file_path = device.execute_shell_command(
            f'find /apex -name {apex_name}@*')
        return int(active_apex_file_path.rsplit('@', 1)[-1])

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
            raise Exception('Failed to find APEX file')

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
