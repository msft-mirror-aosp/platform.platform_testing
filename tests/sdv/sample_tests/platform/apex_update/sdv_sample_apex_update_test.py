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

"""SDV sample Apex Update test"""

from mobly import asserts
import logging
import time
import os
import glob
from pathlib import Path
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleApexUpdateTest(sdv_base_test.SdvBaseTestClass):

  UPDATE_FOLDER = "/data/mydir/"
  SDV_UPDATE_MANAGER_CLIENT = "/apex/com.android.sdv.sample.update_manager.client/bin/sdv_update_manager_client"
  test_dir = ''

  def setup_class(self):
    super().setup_class()
    self.sdv_device = self.get_device('device1').adb()
    if self.user_params.get('env') ==  'local':
      android_root = os.environ.get("ANDROID_BUILD_TOP")
      self.test_dir = f"{android_root}/out/host"
    else:
      log_path = Path(self.sdv_device.log_path())
      self.test_dir = str(Path().joinpath(*log_path.parts[:3]))

  def validate_apex_present(self, name):
    stdout = self.sdv_device.execute_shell_command("ls /apex")
    asserts.assert_in(name, stdout, f'The content was not found in the output')

  def validate_log_entry_present(self, entry, timeout=30, poll_interval=0.1):
    deadline = time.perf_counter() + timeout
    while time.perf_counter() < deadline:
        if self.sdv_device.grep_from_logcat(entry) != "":
            return
        time.sleep(poll_interval)
    asserts.fail(f'Log entry "{entry}" not found')

  def validate_communication(self, logs):
    # Validate logcat contains all expected entries
    for entry in logs:
      self.validate_log_entry_present(entry)

  def initiate_apex_update(self, apex_filename):
    apex_path = self._find_apex(apex_filename)
    if not apex_path:
      asserts.fail('APEX file not found')
    logging.debug(f"Using APEX found at {apex_path}")
    self.sdv_device.execute_shell_command(f"mkdir -p {self.UPDATE_FOLDER}")
    self.sdv_device.push([apex_path, self.UPDATE_FOLDER])
    self.sdv_device.execute_shell_command(f"{self.SDV_UPDATE_MANAGER_CLIENT} prepare service-bundle \
      --boot_attempts 2 \
      --apex_path {self.UPDATE_FOLDER}{apex_filename}"
    )
    self.sdv_device.execute_shell_command(f"{self.SDV_UPDATE_MANAGER_CLIENT} activate")

  # CUJ-APEX-008, CUJ-APEX-009, and CUJ-APEX-015
  def test_communication_after_apex_update(self):

    PROVIDER_APEX_NAME = "com.android.sdv.sample.apex.provider"
    CONSUMER_APEX_NAME = "com.android.sdv.sample.apex.consumer"

    PROVIDER_LOG_V1 = ["Provider serving pressure value 992.", "Provider publishing pressure value 42."]
    PROVIDER_LOG_V2 = ["Provider serving pressure value 992 and new field value 1.", "Provider publishing pressure value 42 and new field value 1."]
    CONSUMER_LOG_V1 = ["Consumer consuming tire pressure data: 992", "Consumer reading subscribed data: \[TirePressure "]
    CONSUMER_LOG_V2 = ["Consumer consuming tire pressure data: 992 and new response field data: 1", "Consumer reading subscribed data: \[TirePressure "]

    # Validate communication works prior to any updates
    self.validate_communication(PROVIDER_LOG_V1 + CONSUMER_LOG_V1)

    # Update Provider APEX
    apex_name_and_version = f"{PROVIDER_APEX_NAME}@2"
    self.initiate_apex_update(f"{PROVIDER_APEX_NAME}.v2.apex")
    logging.debug(f"Rebooting the device and validate Provider APEX update")
    self.sdv_device.reboot_device()
    self.sdv_device.wait_for_device_online()
    self.validate_apex_present(apex_name_and_version)
    self.validate_communication(PROVIDER_LOG_V2 + CONSUMER_LOG_V1)
    self.sdv_device.execute_shell_command(f"{self.SDV_UPDATE_MANAGER_CLIENT} commit")
    self.sdv_device.reboot_device()
    self.sdv_device.wait_for_device_online()
    self.validate_apex_present(apex_name_and_version)
    self.validate_communication(PROVIDER_LOG_V2 + CONSUMER_LOG_V1)

    # Update Consumer APEX
    apex_name_and_version = f"{CONSUMER_APEX_NAME}@2"
    self.initiate_apex_update(f"{CONSUMER_APEX_NAME}.v2.apex")
    self.consumer_updated = True
    logging.debug(f"Rebooting the device and validate Consumer APEX update")
    self.sdv_device.reboot_device()
    self.sdv_device.wait_for_device_online()
    self.validate_apex_present(apex_name_and_version)
    self.validate_communication(PROVIDER_LOG_V2 + CONSUMER_LOG_V2)
    self.sdv_device.execute_shell_command(f"{self.SDV_UPDATE_MANAGER_CLIENT} commit")
    self.sdv_device.reboot_device()
    self.sdv_device.wait_for_device_online()
    self.validate_apex_present(apex_name_and_version)
    self.validate_communication(PROVIDER_LOG_V2 + CONSUMER_LOG_V2)

  # CUJ-APEX-003, CUJ-APEX-004, CUJ-APEX-007
  def test_apex_update_persists(self):
    APEX_NAME = "com.android.sdv.sample.test1"
    user_data = "This is new user data!"
    user_data_file = "/data/new_user_data_test_file.txt"
    self.sdv_device.execute_shell_command(f"echo {user_data} > {user_data_file}")
    self.initiate_apex_update(f"{APEX_NAME}.v2.apex")
    logging.debug(f"Rebooting the device and validate Consumer APEX update")
    self.sdv_device.reboot_device()
    self.sdv_device.wait_for_device_online()
    self.validate_apex_present(f"{APEX_NAME}@2")
    self.validate_log_entry_present("Hello World 2!")
    self.sdv_device.execute_shell_command(f"{self.SDV_UPDATE_MANAGER_CLIENT} commit")
    self.sdv_device.reboot_device()
    self.sdv_device.wait_for_device_online()
    self.validate_apex_present(f"{APEX_NAME}@2")
    self.validate_log_entry_present("Hello World 2!")
    printed_user_data = self.sdv_device.execute_shell_command(f"cat {user_data_file}")
    asserts.assert_equal(printed_user_data, user_data, f"User data '{user_data}' not found in {user_data_file}")

  def _find_apex(self, apex):
    apex_pattern = f"**/{apex}"
    search_path = f"{self.test_dir}/{apex_pattern}"
    logging.debug(f"Search path (CI): {search_path}")
    file_names = glob.glob(search_path, recursive=True)
    if not file_names:
      raise Exception(f"Failed to find files matching '{search_path}'")
    return file_names[0]

if __name__ == '__main__':
  sdv_test_runner.run()
