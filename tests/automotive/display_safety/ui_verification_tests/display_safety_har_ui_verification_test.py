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
import time

from absl.testing import parameterized

from display_safety_test.test_base import display_safety_test_base
from spectatio_host_tf.core import test_runner


class DisplaySafetyHarUiVerificationTest(
    display_safety_test_base.DisplaySafetyBaseTestClass,
    parameterized.TestCase,
):
  """
    Verifies HAR display UI elements using Gemini.
  """

  _TEST_IMAGE_PATH_TEMPLATE = 'screenshot_{test_name}_{use_case}.jpg'
  _SPEED_QUESTION = 'What is the speed of the car?'
  _TELLTALE_QUESTION_TEMPLATE = 'Is the {telltale_sign} sign visible on the screen?'

  def setup_test(self):
    super().setup_test()
    self.reset_display_safety_ui_to_default_state()

  def _get_test_image_path(self, test_name: str, use_case: str) -> str:
    image_name = self._TEST_IMAGE_PATH_TEMPLATE.format(
        test_name=test_name, use_case=use_case)
    return self.get_output_path_for_image(image_name)

  def _verify_ui_with_gemini(
      self, test_image_path: str, question: str, expect_visible: bool = True
  ):
    self.take_device_screenshot(test_image_path)
    response = self.gemini.verify_ui_element_visibility(
        test_image_path, question)
    is_visible = self.gemini.is_element_visible(response)
    assertion = (
        self.asserts.assert_true
        if expect_visible
        else self.asserts.assert_false
    )
    assertion(
        is_visible,
        f'Unexpected Gemini response: {response}. UI element visibility should'
        f' be {expect_visible}.',
    )
    return response

  def test_har_display_for_speed(self):
    """
      Verifies vehicle speed is correctly displayed.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')
    initial_speed_str = '0'
    test_image_path = self._get_test_image_path(
        self.current_test_info.name, initial_speed_str)

    response = self._verify_ui_with_gemini(
        test_image_path, self._SPEED_QUESTION)

    self.asserts.assert_in(
        initial_speed_str,
        self.gemini.get_short_answer(response),
        f'Initial speed should be "{initial_speed_str}". Response: {response}',
    )

    speed_mph = 20.0
    test_image_path = self._get_test_image_path(
        self.current_test_info.name, f'{int(speed_mph)}mph')

    with self.display_safety_client() as client:
      client.post_current_gear('GEAR', self.Gear.DRIVE.value)
      client.post_vehicle_speed('VEHICLE_SPEED', speed_mph)

    time.sleep(5)

    response = self._verify_ui_with_gemini(
        test_image_path, self._SPEED_QUESTION)

    self.asserts.assert_in(
        str(int(speed_mph)),
        self.gemini.get_short_answer(response),
        f'Updated speed should be "{speed_mph}". Response: {response}',
    )

    logging.info(
        f'{self.test_class_name}: Completed test: {self.current_test_info.name}')

  @parameterized.named_parameters(
      {
          'testcase_name': 'seatbelt_driver',
          'telltale_sign': 'SEATBELT_DRIVER'
      },
      {
          'testcase_name': 'low_tire_pressure',
          'telltale_sign': 'LOW_TIRE_PRESSURE',
      },
  )
  def test_har_display_for_telltale(self, telltale_sign: str):
    """
      Verifies telltale signs are correctly displayed.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')

    question = self._TELLTALE_QUESTION_TEMPLATE.format(
        telltale_sign=telltale_sign)

    test_image_path = self._get_test_image_path(
        self.current_test_info.name, f'no_{telltale_sign}')

    self._verify_ui_with_gemini(test_image_path, question, expect_visible=False)

    test_image_path = self._get_test_image_path(
        self.current_test_info.name, telltale_sign)

    with self.display_safety_client() as client:
      client.post_telltale_status(telltale_sign, is_on=True)

    time.sleep(5)

    self._verify_ui_with_gemini(test_image_path, question, expect_visible=True)

    logging.info(
        f'{self.test_class_name}: Completed test: {self.current_test_info.name}'
    )


if __name__ == '__main__':
  test_runner.run()
