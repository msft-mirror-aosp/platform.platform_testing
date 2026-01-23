# Copyright 2025 Google LLC

import enum
import logging
import re
from typing import Any

from screenshot_util_library import screenshot_util


class DisplaySafetyScreenshotUtil(screenshot_util.ScreenshotUtil):
  """
    A utility for taking screenshots using for display safety.
  """

  _LOG_TAG = 'DisplaySafetyScreenshotUtil'

  class DisplaySafetyScreenshotStrategy(enum.Enum):
    """
      Enum for display safety screenshot strategies.
    """
    HAR_DISPLAY_SCREENSHOT_USING_ADB = 'har_display_screenshot_using_adb'
    IVI_DISPLAY_SCREENSHOT_USING_ADB = 'ivi_display_screenshot_using_adb'

  @screenshot_util.register_screenshot_strategy(
      DisplaySafetyScreenshotStrategy.HAR_DISPLAY_SCREENSHOT_USING_ADB.value
  )
  def _take_har_screenshot(self, device: Any, screenshot_path: str, **kwargs):
    """
      Takes a screenshot on a HAR device.
    """
    logging.info(f'{self._LOG_TAG}: Taking HAR screenshot.')
    path_on_device = device.adb.execute_shell_command('sdv_screencap')
    device.adb.pull([path_on_device, screenshot_path])
    device.adb.remove_file(path_on_device)

  def _get_physical_display_id(self, device: Any, display_id: int) -> str:
    """
      Gets the physical display ID for the given logical display ID.
    """
    output = device.adb.execute_shell_command(
        'dumpsys SurfaceFlinger --display-id'
    )
    # Look for "Display <id> (HWC display <display_id>)"
    # Example: Display 4619827353912518657 (HWC display 1): ...
    match = re.search(f'Display (\d+) \(HWC display {display_id}\)', output)
    if match:
      return match.group(1)
    logging.warning(
        f'{self._LOG_TAG}: Could not find physical ID for display {display_id}.'
        f' output: {output}'
    )
    return str(display_id)

  @screenshot_util.register_screenshot_strategy(
      DisplaySafetyScreenshotStrategy.IVI_DISPLAY_SCREENSHOT_USING_ADB.value
  )
  def _take_ivi_screenshot(self, device: Any, screenshot_path: str, **kwargs):
    """
      Takes a screenshot on an IVI device.
    """
    display_id = kwargs.get('display_id', 0)
    physical_display_id = self._get_physical_display_id(device, display_id)
    logging.info(
        f'{self._LOG_TAG}: Taking IVI screenshot on display {display_id}'
        f' (physical ID: {physical_display_id}).'
    )
    path_on_device = f'/sdcard/ivi_screenshot_{display_id}.png'
    device.adb.execute_shell_command(
        f'screencap -d {physical_display_id} -p {path_on_device}'
    )
    device.adb.pull([path_on_device, screenshot_path])
    device.adb.remove_file(path_on_device)
