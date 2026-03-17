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

import logging

from display_safety_test.test_base import display_safety_test_base
from spectatio_host_tf.core import test_runner


class DisplaySafetyLogVerificationTest(
    display_safety_test_base.DisplaySafetyBaseTestClass
):
  """
    Verifies that vehicle data is correctly processed by checking logs.
  """

  _CLIENT_LOGCAT_GREP_TEXT: str = 'CI_TEST_INFO'

  def setup_class(self):
    self.default_log_level = 'D'
    super().setup_class()

  def setup_test(self):
    super().setup_test()

    # Clear logcat buffer
    logging.info(f'{self._LOG_TAG}: Clearing logcat buffer.')
    self.device1.adb.clear_logcat()

  def _post_and_verify_log(self, client_method, expected_log_msg, **kwargs):
    """
      Calls a client method and verifies the resulting logcat entry.
    """
    # Post data.
    status = client_method(**kwargs)
    success_status = 1
    self.asserts.assert_equal(
        status, success_status, f'Failed to post data with args: {kwargs}')

    # Poll logcat for the expected log message.
    is_found = self.device1.adb.poll_logcat_with_grep_text_and_check_for_expected_result(
        self._CLIENT_LOGCAT_GREP_TEXT,
        expected_log_msg,
        timeout=15,
        poll_interval=0.2,
    )

    self.asserts.assert_true(
        is_found,
        f'Expected log "{expected_log_msg}" not found in logcat',
    )

  def test_vehicle_speed_logs(self):
    """
      Verifies vehicle speed data is correctly logged.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')
    speed = 25.5555
    with self.display_safety_client() as client:
      self._post_and_verify_log(
          client.post_vehicle_speed,
          expected_log_msg=f'"vehicle_speed"={speed}; processed',
          topic='vehicle-speed',
          value=speed,
      )

    logging.info(
        f'{self.test_class_name}: Completed test: {self.current_test_info.name}')

  def test_vehicle_gear_logs(self):
    """
      Verifies vehicle gear data is correctly logged.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')
    gear = self.Gear.DRIVE.value
    with self.display_safety_client() as client:
      self._post_and_verify_log(
          client.post_current_gear,
          expected_log_msg=f'"vehicle_gear"="{gear}"; processed',
          topic='current-gear',
          gear_value=gear,
      )
    logging.info(
        f'{self.test_class_name}: Completed test: {self.current_test_info.name}')

  def test_telltale_logs(self):
    """
      Verifies telltale status data is correctly logged.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')
    telltale = 'seatbelt_passenger'
    is_on = True
    with self.display_safety_client() as client:
      self._post_and_verify_log(
          client.post_telltale_status,
          expected_log_msg=f'"{telltale}"={is_on}; processed'.lower(),
          topic='seatbelt-passenger',
          is_on=is_on,
      )
    logging.info(
        f'{self.test_class_name}: Completed test: {self.current_test_info.name}')

  def test_tire_pressure_logs(self):
    """
      Verifies tire pressure data is correctly logged.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')
    pressure = 36
    with self.display_safety_client() as client:
      status = client.post_tire_pressure(topic='front-left', value=pressure)
      self.asserts.assert_equal(status, 1, 'Failed to post tire pressure.')
    logging.info(
        f'{self.test_class_name}: Completed test: {self.current_test_info.name}')

  def test_engine_rpm_logs(self):
    """
      Verifies engine RPM data is correctly logged.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')
    rpm = 1235
    with self.display_safety_client() as client:
      self._post_and_verify_log(
          client.post_engine_rpm,
          expected_log_msg=f'"engine_rpm"={rpm}; processed',
          topic='engine-rpm',
          value=rpm,
      )
    logging.info(
        f'{self.test_class_name}: Completed test: {self.current_test_info.name}')


if __name__ == '__main__':
  test_runner.run()
