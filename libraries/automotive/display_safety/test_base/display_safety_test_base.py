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

import contextlib
import enum
import logging
from typing import Dict, Type



from display_safety_test_client.client import display_safety_client
from display_safety_utils import screenshot_util

from file_utils_library import file_util

from host_orchestrator_util_library import host_orchestrator_util

from image_comparison_library import image_comparison

from spectatio_host_tf.core import test_base
from spectatio_host_tf.utils import error_handler


class DisplaySafetyBaseTestClass(test_base.SpectatioHostBaseTestClass):
  """
    Extends the base test class with services specific to Display Safety.
  """

  _LOG_TAG = 'DisplaySafetyBaseTestClass'

  _DEFAULT_HOST_PORT = '7002'
  _DEFAULT_DEVICE_PORT = '7002'
  _DEFAULT_WAIT_TIME_SECS = 10
  _FAKE_VEHICLE_DATA_SERVICE = (
      'local-vm:com.sdv.google.display_safety'
      '.HarSdvFakeVehicleDataPublisherServiceBundle/instance-1'
  )

  _FLAVOR_TO_SCREENSHOT_STRATEGY: Dict[str, str] = {
      '_har_': screenshot_util.DisplaySafetyScreenshotUtil.DisplaySafetyScreenshotStrategy.HAR_DISPLAY_SCREENSHOT_USING_ADB.value,
      '_ivi_': screenshot_util.DisplaySafetyScreenshotUtil.ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_ADB.value,
      '_auto_': screenshot_util.DisplaySafetyScreenshotUtil.ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_ADB.value,
  }

  _EXECUTION_MODE_TO_SCREENSHOT_STRATEGY: Dict[
      str, str
  ] = {
      'local': screenshot_util.DisplaySafetyScreenshotUtil.ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_CVD.value,
      'host_orchestrator': screenshot_util.DisplaySafetyScreenshotUtil.ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_HOST_ORCHESTRATOR.value,
  }

  _IMAGE_COMPARISON_STRATEGIES = {
      'PIL': '_get_pil_comparator',
      'MSE': '_get_mse_comparator',
  }

  TELLTALE_SIGNS = [
      'oil-pressure', 'engine-temp', 'check-engine', 'charging-failure',
      'seatbelt-driver', 'seatbelt-passenger', 'low-tire-pressure', 'airbag',
      'abs', 'brake', 'traction', 'fog-lights', 'park-lights', 'hibeam',
      'lowbeam', 'turn-signal-left', 'turn-signal-right', 'adas',
      'max-speed-displayed', 'speed-limit-displayed', 'emergency-light',
  ]

  class Gear(enum.Enum):
    DRIVE = 'D'
    PARK = 'P'
    REVERSE = 'R'
    NEUTRAL = 'N'

  def _create_gemini_service(self):
    """
      Factory for creating the Gemini utility, configured via test args.
    """
    from ai_util_library import gemini_util
    api_key = self.get_test_arg('gemini_api_key', required=True)
    return gemini_util.GeminiUtil(api_key=api_key)

  def _create_host_orchestrator_service(
      self,
  ) -> host_orchestrator_util.HostOrchestratorUtil:
    """
      Factory for creating the Host Orchestrator utility
    """
    ho_base_url = self.user_params['ho_base_url']
    return host_orchestrator_util.HostOrchestratorUtil(ho_base_url)

  def setup_class(self) -> None:
    super().setup_class()

    logging.info(
        f'{self._LOG_TAG}: Registering and setting up Display Safety services.')

    # Register factories for services required by this test suite.
    self.register_service_factory(
        'screenshot', screenshot_util.DisplaySafetyScreenshotUtil
    )
    self.register_service_factory('gemini', self._create_gemini_service)
    self.register_service_factory(
        'host_orchestrator', self._create_host_orchestrator_service
    )

    # Dependencies like `self.device1` are now available on demand.
    self.device1.adb.root()

    # Stop fake vehicle data service
    logging.info(f'{self._LOG_TAG}: Stop fake vehicle data service.')
    self.device1.adb.execute_shell_command(
        f'sdv_service_bundle stop {self._FAKE_VEHICLE_DATA_SERVICE}'
    )

    # Reset UI to default state
    logging.info(f'{self._LOG_TAG}: Resetting UI to default state.')
    self.is_grpc_available = True
    try:
      self.reset_display_safety_ui_to_default_state()
    except Exception as e:
      logging.warning(f'{self._LOG_TAG}: Failed to reset UI. Tests will be skipped. Error: {e}')
      self.is_grpc_available = False

    # Clear logcat buffer
    logging.info(f'{self._LOG_TAG}: Clearing logcat buffer.')
    self.device1.adb.clear_logcat()

  def setup_test(self):
    if not getattr(self, "is_grpc_available", True):
      from mobly import signals
      raise signals.TestSkip("Fake vehicle data service is not available on this device.")
    if hasattr(super(), "setup_test"):
      super().setup_test()

  @contextlib.contextmanager
  def display_safety_client(self):
    """
      Provides a managed DisplaySafetyClient instance.
    """
    host_port: str = self.get_test_arg(
        'host_port', default=self._DEFAULT_HOST_PORT
    )
    device_port: str = self.get_test_arg(
        'device_port', default=self._DEFAULT_DEVICE_PORT)
    with display_safety_client.DisplaySafetyClient(
        self.device1, host_port=host_port, device_port=device_port
    ) as client:
      yield client

  def get_golden_image_path(
      self, test_name: str, pkg_name: str = 'display_safety_test',
     image_format: str = 'jpg',
  ) -> str:
    image_path = self.get_test_arg(f'{test_name}_golden_image_path')
    if not image_path:
      image_path = file_util.find_resource_path(
          pkg_name, f'golden_images/{test_name}.{image_format}'
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
        )
        return
    raise error_handler.SpectatioFrameworkError(
        f'Unsupported device flavor "{flavor}". Cannot take screenshot.'
    )

  def take_cluster_screenshot(self, screenshot_path: str) -> None:
    """
      Takes cluster screenshot, auto-detecting the correct method based on
      execution mode ( local or remote ).
    """

    if self.user_params.get('ho_base_url'):
      ho_base_url: str = self.user_params['ho_base_url']
      self.screenshot.take_screenshot(
          self._EXECUTION_MODE_TO_SCREENSHOT_STRATEGY['host_orchestrator'],
          screenshot_path=screenshot_path,
          host_orchestrator=self.host_orchestrator,
          cvd_index=int(
              self.get_test_arg('cvd_index', default=1, required=False)
          ),
          display_index=int(
              self.get_test_arg('cvd_display_index', default=1, required=False)
          ),
      )
    else:
      # Since ho_base_url is not provided, test is running locally.
      # Take cluster screenshot locally.
      self.screenshot.take_screenshot(
          self._EXECUTION_MODE_TO_SCREENSHOT_STRATEGY['local'],
          screenshot_path=screenshot_path,
          instance_name=self.get_test_arg(
              'cluster_instance_name', default='vm1', required=False
          ),
          display_id=self.get_test_arg(
              'cluster_display_id', default='1', required=False
          ),
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

  def reset_display_safety_ui_to_default_state(self):
    """
      Resets the vehicle UI to a default state by sending neutral data.
    """
    logging.info(f'{self._LOG_TAG}: Resetting UI to default state.')
    with self.display_safety_client() as client:
      client.post_vehicle_speed('vehicle-speed', 0.0)
      client.post_current_gear('current-gear', self.Gear.PARK.value)
      client.post_engine_rpm('engine-rpm', 0)
      for telltale in self.TELLTALE_SIGNS:
        client.post_telltale_status(telltale, is_on=False)
    logging.info(f'{self._LOG_TAG}: UI reset complete.')

  def teardown_class(self):
    logging.info(f'{self._LOG_TAG}: Restarting fake vehicle data service.')
    try:
      self.device1.adb.execute_shell_command(
          f'sdv_service_bundle start {self._FAKE_VEHICLE_DATA_SERVICE}'
      )
    except Exception as e:
      logging.warning(f'{self._LOG_TAG}: Failed to restart fake vehicle data service: {e}')
    super().teardown_class()
