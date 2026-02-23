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
import os
from typing import Optional, Tuple

from file_utils_library import file_util
from screenshot_util_library.screenshot_util import ScreenshotUtil
from spectatio_host_tf.core import test_base
from image_comparison_library import image_comparison

class FunctionalTestBaseClass(test_base.SpectatioHostBaseTestClass):
  """
    Extends the base test class with services specific to Functional Tests.
  """

  _LOG_TAG = 'FunctionalTestBaseClass'
  strategy = ScreenshotUtil.ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_ADB.value
  _IMAGE_COMPARISON_STRATEGIES = {
      'PIL': '_get_pil_comparator',
      'MSE': '_get_mse_comparator',
  }

  def setup_class(self) -> None:
    super().setup_class()

    self.mbs = self.device1.load_bundled_snippets()
    # Register factories for services required by screenshot tests.
    self.register_service_factory('screenshot', ScreenshotUtil)

    # Dependencies like `self.device1` are now available on demand.
    self.device1.adb.root()

    # Clear logcat buffer
    logging.info(f'{self._LOG_TAG}: Clearing logcat buffer.')
    self.device1.adb.clear_logcat()

  def get_golden_image_path(
      self, golden_image_name: str, pkg_name: str = 'platform_functional_test'
  ) -> str:
    """
     Generates the golden image path based on image name.
    """
    base_name = os.path.splitext(golden_image_name)[0]
    test_arg_key = f'{base_name}_path'
    image_path = self.get_test_arg(test_arg_key)
    if not image_path:
      image_path = file_util.find_resource_path(
          pkg_name, f'golden_images/{golden_image_name}'
      )
    if not file_util.is_valid_path(image_path):
      raise FileNotFoundError(f'Golden image not found at: {image_path}')
    return image_path

  def get_output_path_for_image(self, image_name: str) -> str:
    """
     Generates the output_path path based on image name.
    """
    base_dir = self.current_test_info.output_path
    return file_util.join_path(base_dir, image_name)

  def take_device_screenshot(self, screenshot_path: str):
    """
     Takes a screenshot.
    """
    self.screenshot.take_screenshot(
      screenshot_strategy = self.strategy,
      device = self.device1,
      screenshot_path = screenshot_path,
    )
    return

  def compare_images(self,
      golden_image_path: str,
      test_image_path: str,
      diff_image_path: str,
      exclude_area: Optional[Tuple[int, int, int, int]] = None,
      include_area: Optional[Tuple[int, int, int, int]] = None,
    ) -> bool:
      """
        Takes a screenshot.
      """

      logging.info(f'{self._LOG_TAG}: Comparing images.')
      comparator = self._create_image_comparator(
        golden_image_path, test_image_path, exclude_area, include_area
      )

      is_similar = comparator.are_images_similar()
      comparator.save_diff_image(diff_image_path)
      return is_similar

  def _get_pil_comparator(
      self,
      golden_image_path: str,
      test_image_path: str,
      exclude_area: Optional[Tuple[int, int, int, int]] = None,
      include_area: Optional[Tuple[int, int, int, int]] = None,
  ) -> image_comparison.ImageComparator:
    """
      Creates a PIL-based image comparator.
    """

    return image_comparison.CompareImagesUsingPIL(
        golden_image_path, test_image_path, exclude_area, include_area
    )

  def _get_mse_comparator(
      self,
      golden_image_path: str,
      test_image_path: str,
      exclude_area: Optional[Tuple[int, int, int, int]] = None,
      include_area: Optional[Tuple[int, int, int, int]] = None,
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
          golden_image_path, test_image_path, diff_threshold, exclude_area, include_area
    )

  def _create_image_comparator(self,
      golden_image_path: str,
      test_image_path: str,
      exclude_area: Optional[Tuple[int, int, int, int]] = None,
      include_area: Optional[Tuple[int, int, int, int]] = None,
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
      logging.info(f'{self._LOG_TAG}: Using {comparator_method} to compare images.')
      return comparator_method(golden_image_path, test_image_path,exclude_area,include_area)
