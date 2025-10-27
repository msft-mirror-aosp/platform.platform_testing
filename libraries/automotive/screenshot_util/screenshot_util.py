# Copyright 2025 Google LLC

import base64
import enum
import functools
import io
import logging
from typing import Any, Callable, Dict

from mobly.utils import run_command

from PIL import Image


class ScreenshotError(Exception):
  """
    Base exception for screenshot-related errors.
  """

  pass


_SCREENSHOT_STRATEGIES: Dict[str, Callable[..., Any]] = {}


def register_screenshot_strategy(name: str):
  """
    Decorator for registering screenshot strategies.

    Args:
        name: The name of the screenshot strategy.
    Returns:
        The decorator function.
  """
  def decorator(func: Callable[..., Any]):
    @functools.wraps(func)
    def wrapper(*args: Any, **kwargs: Any) -> Any:
      return func(*args, **kwargs)

    _SCREENSHOT_STRATEGIES[name] = wrapper
    return wrapper

  return decorator


class ScreenshotUtil:
  """
    A utility for taking screenshots using different strategies.
  """

  _LOG_TAG = 'ScreenshotUtil'

  class ScreenshotStrategy(enum.Enum):
    """
      Enum for screenshot strategies.
    """
    DISPLAY_SCREENSHOT_USING_ADB = 'display_screenshot_using_adb'
    DISPLAY_SCREENSHOT_USING_CVD = 'display_screenshot_using_cvd'
    DISPLAY_SCREENSHOT_USING_HOST_ORCHESTRATOR = (
        'display_screenshot_using_host_orchestrator'
    )

  @register_screenshot_strategy(
      ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_ADB.value
  )
  def _take_display_screenshot_using_adb(
      self, device: Any, screenshot_path: str, **kwargs
  ):
    """
      Takes a screenshot on an Android device using ADB.

      Args:
          device: The device object.
          screenshot_path: The path to save the screenshot on the host.
          **kwargs: Additional arguments.
    """
    logging.info(f'{self._LOG_TAG}: Taking device screenshot using ADB.')
    path_on_device = '/data/local/tmp/screenshot.jpg'
    device.adb.execute_shell_command(f'screencap -p {path_on_device}')
    device.adb.pull([path_on_device, screenshot_path])
    device.adb.remove_file(path_on_device)

  @register_screenshot_strategy(
      ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_CVD.value
  )
  def _take_display_screenshot_using_cvd(
      self, screenshot_path: str, instance_name: str, display_id: str, **kwargs
  ):
    """
      Takes a screenshot on the host using CVD.

      Works only for Cuttlefish. Take a screenshot on host where CF instances
      are running. Supports taking screenshots when we have multiple instances
      and multiple displays.

      Args:
          screenshot_path: The path to save the screenshot on the host.
          instance_name: The name of the Cuttlefish instance.
          display_id: The ID of the display to screenshot.
          **kwargs: Additional arguments.
    """
    logging.info(f'{self._LOG_TAG}: Taking local cluster screenshot.')
    command = [
        'cvd',
        f'--instance_name={instance_name}',
        'display',
        'screenshot',
        f'--display={display_id}',
        f'--screenshot_path={screenshot_path}',
    ]
    logging.debug(f'{self._LOG_TAG}: Executing command: {" ".join(command)}.')
    run_command(command)

  @register_screenshot_strategy(
      ScreenshotStrategy.DISPLAY_SCREENSHOT_USING_HOST_ORCHESTRATOR.value
  )
  def _take_display_screenshot_using_host_orchestrator(
      self,
      screenshot_path: str,
      host_orchestrator: Any,
      cvd_index: int,
      display_index: int,
      **kwargs,
  ):
    """
      Use Host Orchestrator to take display screenshot.

      Works only for Cuttlefish. Supports taking screenshots
      when we have CF instances running remotely in a host orchestrator
      managed environment.
    """
    try:
      logging.info(
          f'{self._LOG_TAG}: Taking display screenshot using host orchestrator.'
      )
      screenshot_result  = host_orchestrator.take_screenshot(
          cvd_index=cvd_index, display_index=display_index
      )

      if not screenshot_result.is_success():
        raise ScreenshotError(
            'Failed to take screenshot using host orchestrator:'
            f' {screenshot_result}'
        )

      logging.info(
          f'{self._LOG_TAG}: Screenshot taken successfully. Saving it to'
          f' {screenshot_path}.'
      )

      image_data = base64.b64decode(
          screenshot_result.result['screenshot_bytes']
      )
      image_stream = io.BytesIO(image_data)
      mime_type = screenshot_result.result['screenshot_mime_type']

      image = Image.open(image_stream)

      if 'jpeg' in mime_type or 'jpg' in mime_type:
        save_format = 'JPG'
      elif 'png' in mime_type:
        save_format = 'PNG'
      else:
        # Rely on Pillow to infer from the output_path's extension
        save_format = None

      image.save(screenshot_path, format=save_format)
      logging.info(
          f'{self._LOG_TAG}: Screenshot saved successfully. Path:'
          f' {screenshot_path}.'
      )
    except Exception as e:
      raise ScreenshotError(
          f'Failed to take screenshot using host orchestrator. Error: {e}'
      ) from e

  def take_screenshot(
      self, screenshot_strategy: str, **kwargs: Any
  ) -> None:
    """
      Takes a screenshot using the specified strategy.

      Args:
          screenshot_strategy: The name of the screenshot strategy to execute.
          **kwargs: Arguments to be passed to the screenshot function.
                    Common args: `device`, `screenshot_path`, `instance_name`,
                    `display_id`.

      Raises:
          ScreenshotError: If the screenshot type is not supported or if the
          underlying command fails.
    """
    strategy = _SCREENSHOT_STRATEGIES.get(screenshot_strategy)
    if not strategy:
      raise ScreenshotError(
          f'Unsupported screenshot strategy: {screenshot_strategy}'
      )

    try:
      logging.info(
          f'{self._LOG_TAG}: Executing screenshot strategy'
          f' "{screenshot_strategy}" with args: {kwargs}'
      )
      strategy(self, **kwargs)
    except Exception as e:
      raise ScreenshotError(
          f'Failed to take screenshot with strategy "{screenshot_strategy}"'
      ) from e
