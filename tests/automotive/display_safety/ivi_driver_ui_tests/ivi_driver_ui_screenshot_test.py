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
from typing import Dict

from display_safety_utils import screenshot_util
from file_utils_library import file_util
from image_comparison_library import image_comparison
from spectatio_host_tf.core import test_base
from spectatio_host_tf.core import test_runner
from spectatio_host_tf.utils import error_handler


class IVIDriverUIScreenshotTest(test_base.SpectatioHostBaseTestClass):
  """
    Display Safety IVI Screenshot Test: Compares the screenshot of IVI display
    with the golden image.

    Default Golden Images are added as resources. Each test case has a default
    golden image with the test name as the filename in the resources.

    If Custom Golden Images are required, they can be passed as test arguments
    using --test_args=<test_name>_golden_image_path='/path/to/golden/image.jpg'
  """

  _LOG_TAG = 'IVIDriverUIScreenshotTest'

  _FLAVOR_TO_SCREENSHOT_STRATEGY: Dict[str, str] = {
      '_har_': screenshot_util.DisplaySafetyScreenshotUtil.DisplaySafetyScreenshotStrategy.HAR_DISPLAY_SCREENSHOT_USING_ADB.value,
      '_ivi_': screenshot_util.DisplaySafetyScreenshotUtil.DisplaySafetyScreenshotStrategy.IVI_DISPLAY_SCREENSHOT_USING_ADB.value,
      '_auto_': screenshot_util.DisplaySafetyScreenshotUtil.DisplaySafetyScreenshotStrategy.IVI_DISPLAY_SCREENSHOT_USING_ADB.value,
  }

  _IMAGE_COMPARISON_STRATEGIES = {
      'PIL': '_get_pil_comparator',
      'MSE': '_get_mse_comparator',
  }

  def setup_class(self) -> None:
    super().setup_class()

    logging.info(f'{self._LOG_TAG}: Registering and setting up services.')

    # Register factories for services required by this test suite.
    self.register_service_factory(
        'screenshot', screenshot_util.DisplaySafetyScreenshotUtil
    )

    # Dependencies like `self.device1` are now available on demand.
    self.device1.adb.root()

    # Clear logcat buffer
    logging.info(f'{self._LOG_TAG}: Clearing logcat buffer.')
    self.device1.adb.clear_logcat()

  def get_golden_image_path(
      self, test_name: str, pkg_name: str = 'display_safety_test'
  ) -> str:
    image_path = self.get_test_arg(f'{test_name}_golden_image_path')
    if not image_path:
      image_path = file_util.find_resource_path(
          pkg_name, f'golden_images/{test_name}.jpg'
      )
    if not file_util.is_valid_path(image_path):
      raise FileNotFoundError(f'Golden image not found at: {image_path}')
    return image_path

  def get_output_path_for_image(self, image_name: str) -> str:
    base_dir = self.current_test_info.output_path
    return file_util.join_path(base_dir, image_name)

  def take_device_screenshot(self, screenshot_path: str):
    """
      Takes a screenshot, auto-detecting the correct method by device flavor.
    """
    flavor = self.device1.adb.getprop('ro.build.flavor')
    for (
        flavor_key,
        screenshot_strategy,
    ) in self._FLAVOR_TO_SCREENSHOT_STRATEGY.items():
      if flavor_key in flavor:
        # Access the 'screenshot' service dynamically.
        self.screenshot.take_screenshot(
            screenshot_strategy,
            device=self.device1,
            screenshot_path=screenshot_path,
            display_id=1,
        )
        return
    raise error_handler.SpectatioFrameworkError(
        f'Unsupported device flavor "{flavor}". Cannot take screenshot.'
    )

  def _get_pil_comparator(
      self, golden_image_path: str, test_image_path: str
  ) -> image_comparison.ImageComparator:
    """
      Creates a PIL-based image comparator.
    """
    # For PIL, an area can be excluded from comparison, which is useful for
    # ignoring dynamic elements like dates.
    # The format is a string: '(left,top,right,bottom)'.
    # (720,540,900,560) is the default area where the date is shown.
    exclude_area_str = self.get_test_arg(
        'image_comparison_exclude_area',
        default='(720,540,900,560)',
        required=False,
    )
    exclude_area = tuple(
        int(x.strip()) for x in exclude_area_str.strip('()').split(',')
    )
    return image_comparison.CompareImagesUsingPIL(
        golden_image_path, test_image_path, exclude_area
    )

  def _get_mse_comparator(
      self, golden_image_path: str, test_image_path: str
  ) -> image_comparison.ImageComparator:
    """
      Creates an MSE-based image comparator.
    """
    # For MSE, a diff threshold can be provided, defaulting to 1.0.
    diff_threshold = float(
        self.get_test_arg(
            'image_comparison_diff_threshold', default='1.0', required=False
        )
    )
    return image_comparison.CompareImagesUsingMSE(
        golden_image_path, test_image_path, diff_threshold
    )

  def _create_image_comparator(
      self, golden_image_path: str, test_image_path: str
  ) -> image_comparison.ImageComparator:
    """
      Factory for creating an image comparator based on the test arguments.
    """
    # Use the specified image comparison strategy ('PIL' or 'MSE'),
    # defaulting to 'MSE'.
    strategy_name = self.get_test_arg(
        'image_comparison_strategy', default='MSE', required=False
    )
    logging.info(f'{self._LOG_TAG}: Using {strategy_name} to compare images.')

    comparator_method_name = self._IMAGE_COMPARISON_STRATEGIES.get(
        strategy_name
    )

    if not comparator_method_name:
      raise ValueError(
          f'Unsupported image comparison strategy: {strategy_name}'
      )

    comparator_method = getattr(self, comparator_method_name)
    return comparator_method(golden_image_path, test_image_path)

  def compare_images(
      self,
      golden_image_path: str,
      test_image_path: str,
      diff_image_path: str,
  ) -> bool:
    """
      Compares two images using the specified strategy.

      Args:
          golden_image_path: The path to the golden image.
          test_image_path: The path to the test image.
          diff_image_path: The path to the diff image.

      Returns:
          True if the images are similar, False otherwise.
    """
    logging.info(f'{self._LOG_TAG}: Comparing images.')
    comparator = self._create_image_comparator(
        golden_image_path, test_image_path
    )
    is_similar = comparator.are_images_similar()
    comparator.save_diff_image(diff_image_path)
    return is_similar

  def setup_test(self):
    """
      Prepares paths for the test.
    """
    super().setup_test()
    self.golden_image_path = self.get_golden_image_path(
        test_name=self.current_test_info.name,
        pkg_name='ivi_driver_ui_test_resources',
    )
    self.test_image_path = self.get_output_path_for_image(
        f'{self.current_test_info.name}_test_image.jpg'
    )
    self.diff_image_path = self.get_output_path_for_image(
        f'{self.current_test_info.name}_diff_image.jpg'
    )

  def _set_night_mode(self, enabled: bool):
    mode = 'yes' if enabled else 'no'
    cmd = f'cmd uimode night {mode}'
    logging.info(f'Executing command on IVI: {cmd}')
    self.device1.adb.execute_shell_command(cmd)
    time.sleep(2) # Wait for effect

  def test_driver_ui_visible(self):
    """
      Takes a screenshot of the IVI display and compares it against a
      golden image to ensure DriverUI is visible.
    """
    logging.info(
        f'{self.test_class_name}: Running test: {self.current_test_info.name}')

    # Set light mode to ensure consistent theme for screenshots.
    self._set_night_mode(False)

    # Wait for the UI to be ready.
    time.sleep(5)

    logging.info(
        f'{self.test_class_name}#{self.current_test_info.name}: Taking'
        ' screenshot of IVI display.'
    )

    # Assuming device1 is the IVI VM
    self.take_device_screenshot(self.test_image_path)

    logging.info(
        f'{self.test_class_name}#{self.current_test_info.name}: Comparing'
        ' screenshot with golden image.'
    )

    are_images_similar = self.compare_images(
        self.golden_image_path, self.test_image_path, self.diff_image_path
    )

    self.asserts.assert_true(
        are_images_similar,
        'Device screenshot does not match the golden image. '
        f'Diff found at: {self.diff_image_path}',
    )

    logging.info(
        f'{self.test_class_name}: Completed test: {self.current_test_info.name}'
    )


if __name__ == '__main__':
  test_runner.run()