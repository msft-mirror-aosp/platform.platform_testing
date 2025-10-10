# Copyright 2025 Google LLC

import logging
import re

from typing import Any, Callable

from mobly import asserts
from mobly import base_test

from spectatio_host_tf.core import test_context
from spectatio_host_tf.utils import error_handler


class SpectatioHostBaseTestClass(base_test.BaseTestClass):
  """
    Specatio Host-Side Automotive Test Framework Base Test Class

    Each test must extend SpectatioHostBaseTestClass

    Example usage:

        class TestClassName(test_base.SpectatioHostBaseTestClass):
            setup_class
            setup_test
            teardown_test
            test_sometest
  """
  _LOG_TAG = 'SpectatioHostBaseTestClass'
  default_log_level = 'V'  # Default log level for all devices = Verbose

  def setup_class(self) -> None:
    """
      Sets up the test environment by creating the `TestContext`, which
      manages all dependencies.
    """
    logging.info(f'{self._LOG_TAG}: Creating TestContext')
    self.context = test_context.TestContext(self)
    self.asserts = asserts  # Provide direct access for convenience.

    # Configure devices to logcat verbosely and persist logs.
    logging.info(f'{self._LOG_TAG}: Configuring device logcat')
    for device in self.context.get_all_devices():
      device.update_logcat_config_to_persist_for_given_log_level(
          log_level=self.default_log_level
      )

    logging.info(f'{self._LOG_TAG}: Base test class setup complete.')

  def __getattr__(self, name: str) -> Any:
    """
      Dynamically resolves dependencies from the context on demand.

      This allows test authors to use services like `self.gemini` or devices
      like `self.device1` directly, without declaring them beforehand.
    """
    # Check if the requested attribute is a device (e.g., 'device1').
    if re.match(r'^device\d+$', name):
      try:
        return self.context.get_device(name)
      except error_handler.DeviceNotFoundError as e:
        raise AttributeError(
            f'Attempted to access undeclared device "{name}".') from e

    # If not a device, assume it's a service and delegate to the context.
    try:
      return getattr(self.context, name)
    except AttributeError:
      # Raise a standard AttributeError if the service is not found.
      raise AttributeError(
          f"'{self.__class__.__name__}' object has no attribute '{name}'. "
          f'Is "{name}" a registered service in the TestContext?'
      )

  def setup_test(self) -> None:
    logging.info(f'{self._LOG_TAG}: Cleaning devices before test.')
    for device in self.context.get_all_devices():
      device.adb.remove_all_temp_files()
      device.adb.terminate_all_subprocesses()

  def teardown_test(self) -> None:
    logging.info(f'{self._LOG_TAG}: Cleaning devices after test.')
    for device in self.context.get_all_devices():
      device.adb.remove_all_temp_files()
      device.adb.terminate_all_subprocesses()

  def get_test_arg(
      self, name: str, default: Any = None, required: bool = False) -> Any:
    """
      A convenience method to retrieve a test argument from the context.
    """
    return self.context.get_test_arg(name, default=default, required=required)

  def register_service_factory(
      self, name: str, factory: Callable[[], Any]
  ) -> None:
    """
      A convenience method to register serices with test context.
    """
    self.context.register_service_factory(name, factory)

  @property
  def test_class_name(self) -> str:
    """
      Returns the name of the current test class.
    """
    return self.__class__.__name__
