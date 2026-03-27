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


class DisplaySafetyClusterScreenshotTest(
    display_safety_test_base.DisplaySafetyBaseTestClass,
    parameterized.TestCase,
):
  """
    Display Safety Cluster Screenshot Test: Compares the screenshot of Cluster
    display with the golden image for different vehicle states.

    Default Golden Images are added as resources. Each test case has a default
    golden image with the test name as the filename in the resources.

    If Custom Golden Images are required, they can be passed as test arguments
    using --test_args=<test_name>_golden_image_path='/path/to/golden/image.jpg'
  """

  def _take_screenshot_and_compare(
      self, golden_image_name: str, test_image_name: str, diff_image_name: str
  ) -> bool:
    """
      Takes a screenshot of the cluster display and compares it against a
      golden image.
    """
    golden_image_path = self.get_golden_image_path(
        test_name=golden_image_name,
        pkg_name='display_safety_cluster_test_resources',
        image_format='png',
    )
    test_image_path = self.get_output_path_for_image(test_image_name)
    diff_image_path = self.get_output_path_for_image(diff_image_name)

    logging.info(
        f'{self.test_class_name}#{self.current_test_info.name}: Taking'
        ' screenshot of cluster display'
    )

    self.take_cluster_screenshot(test_image_path)

    logging.info(
        f'{self.test_class_name}#{self.current_test_info.name}: Comparing'
        ' screenshot with golden image.'
    )

    return self.compare_images(
        golden_image_path, test_image_path, diff_image_path
    )

  def setup_test(self):
    """
      Sets the vehicle state and prepares paths for the test.
    """
    super().setup_test()
    self.reset_display_safety_ui_to_default_state()

  @parameterized.named_parameters(
      {
          'testcase_name': 'retro_mode',
          'display_mode': 'RETRO',
          'command': '',
          'reset_command': '',
          'verify_after_reset': False,
      },
      {
          'testcase_name': 'modern_mode',
          'display_mode': 'MODERN',
          'command': 'cmd car_service inject-key -d 1 50',
          'reset_command': 'cmd car_service inject-key -d 1 50',
          'verify_after_reset': True,
      },
      {
          'testcase_name': 'night_mode',
          'display_mode': 'NIGHT',
          'command': 'cmd uimode night yes',
          'reset_command': 'cmd uimode night no',
          'verify_after_reset': True,
      },
      {
          'testcase_name': 'color_change',
          'display_mode': 'COLOR_CHANGE',
          'command': 'cmd car_service inject-key -d 1 9',
          'reset_command': 'cmd car_service inject-key -d 1 8',
          'verify_after_reset': True,
      },
  )
  def test_cluster_display(
      self, display_mode: str, command: str, reset_command: str,
      verify_after_reset: bool,
  ):
    """
      Sets the vehicle state, takes a screenshot of the cluster display
      and compares it against a golden image.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')

    # Set display mode if command is provided.
    if command:
      logging.info(
          f'{self.test_class_name}#{self.current_test_info.name}: Setting'
          f' display to {display_mode} mode using command {command}'
      )
      self.device2.adb.execute_shell_command(command)

    # Set vehicle state.
    device1_system_datetime = '2025-10-02T09:00:00'
    speed = 5.0
    gear = self.Gear.DRIVE.value
    engine_rpm = 1200

    logging.info(
        f'{self.test_class_name}#{self.current_test_info.name}: Setting vehicle'
        f' state to speed: {speed}, gear: {gear}, engine_rpm: {engine_rpm}'
    )

    self.device1.adb.execute_shell_command(f'date -s {device1_system_datetime}')
    with self.display_safety_client() as client:
      client.post_vehicle_speed('vehicle-speed', speed)
      client.post_current_gear('current-gear', gear)
      client.post_engine_rpm('engine-rpm', engine_rpm)

    # Wait for the vehicle state to be applied.
    time.sleep(5)

    # Take screenshot and compare with golden image.
    is_display_mode_set_correctly = self._take_screenshot_and_compare(
        self.current_test_info.name,
        f'{self.current_test_info.name}_test_image.png',
        f'{self.current_test_info.name}_diff_image.png'
    )

    # Reset display to default mode if reset command is provided.
    if reset_command:
      logging.info(
          f'{self.test_class_name}#{self.current_test_info.name}: Resetting'
          f' display to default mode using command {reset_command}'
      )
      self.device2.adb.execute_shell_command(reset_command)
      # Wait for the vehicle state to be applied.
      time.sleep(5)

    self.asserts.assert_true(
        is_display_mode_set_correctly,
        f' {self.test_class_name}#{self.current_test_info.name}: Display mode'
        f' {display_mode} is not set correctly. Test image'
        f' {self.current_test_info.name}_test_image.png does not match'
        f' golden image {self.current_test_info.name}.png',
    )

    # Verify display after reset if verify_after_reset is True.
    if verify_after_reset:
      logging.info(
          f'{self.test_class_name}#{self.current_test_info.name}: Verifying'
          ' display after reset'
      )
      is_display_reset_correctly = self._take_screenshot_and_compare(
          'test_cluster_display_after_reset',
          f'{self.current_test_info.name}_after_reset_test_image.png',
          f'{self.current_test_info.name}_after_reset_diff_image.png',
      )
      self.asserts.assert_true(
          is_display_reset_correctly,
          f' {self.test_class_name}#{self.current_test_info.name}: Display was'
          ' not reset correctly. Test image'
          f' {self.current_test_info.name}_after_reset_test_image.png does not'
          ' match golden image test_cluster_display_after_reset.png',
      )

    logging.info(
        f'{self.test_class_name}: Completed test: {self.current_test_info.name}'
    )

  def teardown_test(self):
    """
      Resets the vehicle UI to a default state.
    """
    self.reset_display_safety_ui_to_default_state()
    super().teardown_test()


if __name__ == '__main__':
  test_runner.run()
