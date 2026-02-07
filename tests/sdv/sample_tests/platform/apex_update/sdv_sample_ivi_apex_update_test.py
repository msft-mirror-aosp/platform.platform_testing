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

"""SDV/IVI sample Apex Update test"""

from mobly import asserts
import logging

from pathlib import Path

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner

from sdv_test_fw.update.update_manager_base_class import UpdateManagerBaseClass
from sdv_test_fw.verification import polling

class SdvIviSampleApexUpdateTest(sdv_base_test.SdvBaseTestClass, UpdateManagerBaseClass):
  SDV_APEX_NAME = 'com.sdv.google.sample.apex.provider'
  IVI_APEX_NAME = 'com.sdv.google.sample.ivi.service'

  APEX_HOST_DIR = 'out/host/**'

  RUN_IVI_SERVICE_CMD = '/apex/com.sdv.google.sample.ivi.service/bin/ivi_service_bundle_apex_update'
  SERVICE_PROCESS_NAME = 'ivi_service_bundle_apex_update'

  RPC_SERVER_LOG_KEYWORD = 'Provider serving pressure value 992'
  EXPECTED_RPC_SERVER_LOG_V1 = 'sdv_apex_update_provider_service_bundle: Provider serving pressure value 992'
  EXPECTED_RPC_SERVER_LOG_V2 = 'sdv_apex_update_provider_service_bundle_v2: Provider serving pressure value 992 and new field value 1'

  DT_PUBLISHER_LOG_KEYWORD = 'Provider publishing pressure value 42'
  EXPECTED_DT_PUBLISHER_LOG_V1 = 'sdv_apex_update_provider_service_bundle: Provider publishing pressure value 42'
  EXPECTED_DT_PUBLISHER_LOG_V2 = 'sdv_apex_update_provider_service_bundle_v2: Provider publishing pressure value 42 and new field value 1'

  IVI_SERVICE_LOG_KEYWORD = 'ivi_service_bundle_apex_update'
  EXPECTED_RPC_CLIENT_LOG_V1 = 'Received GetTirePressureResponse: 992'
  EXPECTED_RPC_CLIENT_LOG_V2 = 'Received GetTirePressureResponse: 992 and new field value: 1'
  EXPECTED_DT_SUBSCRIBER_LOG_V1 = 'Read data tunnel message: pressure: 42'
  EXPECTED_DT_SUBSCRIBER_LOG_V2 = 'new_topic_field: 1'

  def setup_class(self):
      super().setup_class()
      # Currently only IVI primary and SDV secondary is supported on CI when both are needed.
      self.ivi_device = self.get_device('device1').adb()
      self.sdv_core_device = self.get_device('device2').adb()

      self.init_update_manager_base_class(self.SDV_APEX_NAME, self.get_device('device2'), self.APEX_HOST_DIR)
      self.upload_service_bundle_update_payload()

  def teardown_test(self):
      if self.get_apex_version(self.sdv_core_device, self.SDV_APEX_NAME) == 2:
        self.client.uninstall_apex(f'{self.SDV_APEX_NAME}@2')
        # APEX uninstall will only take effect after reboot
        self.sdv_core_device.reboot_device_and_verify_logcat()
      if self.get_apex_version(self.ivi_device, self.IVI_APEX_NAME) == 2:
        self.ivi_device.execute_shell_command(f'pm uninstall --versionCode 2 {self.IVI_APEX_NAME}')
        # APEX uninstall will only take effect after reboot
        self.ivi_device.reboot_device_and_verify_logcat()

      # TODO: b/399673375 - remove the following after terminate_subprocess() is fixed, and confirming it's by default called in the teardown method provided by the framework
      # Locally it can be checked by making sure 'adb shell pidof ivi_service_bundle_apex_update' returns empty after running the test
      for pid_str in self.check_process_running():
        pid = int(pid_str)
        self.ivi_device.execute_shell_command(f'kill {pid}')
      super().teardown_test()

  def check_process_running(self):
    """Checks if any existing process for the IVI service under test is running and returns its PIDs or an empty list."""
    output = self.ivi_device.execute_shell_command(f'pidof {self.SERVICE_PROCESS_NAME}', raise_exception=False)
    if output:
      pids = output.strip().split()
      return pids
    return []

  def validate_apex_present(self, device, name):
    stdout = device.execute_shell_command('ls /apex')
    asserts.assert_in(name, stdout, 'The content was not found in the output')

  def get_apex_version(self, device, apex_name):
      active_apex_file_path = device.execute_shell_command(
          f"find /apex -name '{apex_name}@*'")
      return int(active_apex_file_path.rsplit('@', 1)[-1])

  def test_v1_services_talk(self):
    # Validates version and log for SDV APEX V1
    self.validate_apex_present(self.sdv_core_device, f'{self.SDV_APEX_NAME}@1')
    polling.wait_and_verify_expected_logs(self.sdv_core_device, self.RPC_SERVER_LOG_KEYWORD, self.EXPECTED_RPC_SERVER_LOG_V1)
    polling.wait_and_verify_expected_logs(self.sdv_core_device, self.DT_PUBLISHER_LOG_KEYWORD, self.EXPECTED_DT_PUBLISHER_LOG_V1)

    # Validates version and log for IVI APEX V1 that talks to SDV APEX V1
    self.validate_apex_present(self.ivi_device, f'{self.IVI_APEX_NAME}@1')
    self.ivi_device.execute_shell_command_in_subprocess(self.RUN_IVI_SERVICE_CMD, self.RUN_IVI_SERVICE_CMD)
    polling.wait_and_verify_expected_logs(self.ivi_device, self.IVI_SERVICE_LOG_KEYWORD, self.EXPECTED_DT_SUBSCRIBER_LOG_V1)
    polling.wait_and_verify_expected_logs(self.ivi_device, self.IVI_SERVICE_LOG_KEYWORD, self.EXPECTED_RPC_CLIENT_LOG_V1)

  def test_v1_service_talk_to_v2_service(self):
    # Updates SDV APEX and reboots which is necessary for the update to take effect
    self.prepare_service_bundle_update()
    self.client.activate()
    logging.debug('Rebooting the device and validate SDV Provider APEX update')
    self.sdv_core_device.reboot_device()
    self.sdv_core_device.wait_for_device_online()
    self.client.commit()
    # Validates version and log for SDV APEX V2
    self.validate_apex_present(self.sdv_core_device, f'{self.SDV_APEX_NAME}@2')
    polling.wait_and_verify_expected_logs(self.sdv_core_device, self.RPC_SERVER_LOG_KEYWORD, self.EXPECTED_RPC_SERVER_LOG_V2)
    polling.wait_and_verify_expected_logs(self.sdv_core_device, self.DT_PUBLISHER_LOG_KEYWORD, self.EXPECTED_DT_PUBLISHER_LOG_V2)

    # Validates log for IVI APEX V1 that talks to SDV APEX V2
    self.ivi_device.execute_shell_command_in_subprocess(self.RUN_IVI_SERVICE_CMD, self.RUN_IVI_SERVICE_CMD)
    # The IVI Service Bundle is already processing the updated Data Tunnel message because it parses the data via reflection
    polling.wait_and_verify_expected_logs(self.ivi_device, self.IVI_SERVICE_LOG_KEYWORD, self.EXPECTED_DT_SUBSCRIBER_LOG_V1)
    polling.wait_and_verify_expected_logs(self.ivi_device, self.IVI_SERVICE_LOG_KEYWORD, self.EXPECTED_DT_SUBSCRIBER_LOG_V2)
    polling.wait_and_verify_expected_logs(self.ivi_device, self.IVI_SERVICE_LOG_KEYWORD, self.EXPECTED_RPC_CLIENT_LOG_V1)

  def test_v2_services_talk(self):
    # Updates SDV APEX and reboots which is necessary for the update to take effect
    self.prepare_service_bundle_update()
    self.client.activate()
    self.sdv_core_device.reboot_device()
    self.sdv_core_device.wait_for_device_online()
    self.client.commit()

    # Updates IVI APEX from V1 to V2
    apex_host_path = self.find_first_matching_file(f'**/{self.IVI_APEX_NAME}.v2*.apex')
    apex_device_path = Path('/data/local/tmp/')
    apex_destination_path = apex_device_path.joinpath(f'{self.IVI_APEX_NAME}.v2.apex')
    apex_destination_path_str = str(apex_destination_path)
    self.ivi_device.push(
            [apex_host_path, apex_destination_path_str])
    self.ivi_device.execute_shell_command(f'pm install --apex --force-non-staged {apex_destination_path_str}')
    # Validates version and log for IVI APEX V2 that talks to SDV APEX V2
    self.validate_apex_present(self.ivi_device, f'{self.IVI_APEX_NAME}@2')
    self.ivi_device.execute_shell_command_in_subprocess(self.RUN_IVI_SERVICE_CMD, self.RUN_IVI_SERVICE_CMD)
    polling.wait_and_verify_expected_logs(self.ivi_device, self.IVI_SERVICE_LOG_KEYWORD, self.EXPECTED_DT_SUBSCRIBER_LOG_V1)
    polling.wait_and_verify_expected_logs(self.ivi_device, self.IVI_SERVICE_LOG_KEYWORD, self.EXPECTED_DT_SUBSCRIBER_LOG_V2)
    polling.wait_and_verify_expected_logs(self.ivi_device, self.IVI_SERVICE_LOG_KEYWORD, self.EXPECTED_RPC_CLIENT_LOG_V2)

if __name__ == '__main__':
  sdv_test_runner.run()
