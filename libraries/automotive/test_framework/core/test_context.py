# Copyright 2025 Google LLC

import functools
import logging
from typing import Any, Callable

from mobly import base_test
from mobly.controllers import android_device

from spectatio_host_tf.core import test_device
from spectatio_host_tf.core import test_device_manager
from spectatio_host_tf.utils import error_handler
from spectatio_host_tf.utils import test_arg_util


class TestContext:
  """
    A service locator for managing test dependencies and configuration.
  """

  _LOG_TAG = 'TestContext'
  _DEFAULT_NUMBER_OF_DEVICES: int = 1
  _NUMBER_OF_DEVICES_KEY = 'number_of_devices'

  def __init__(self, test_instance: base_test.BaseTestClass) -> None:
    self._test_instance: base_test.BaseTestClass = test_instance
    self._test_args: dict[str, str] = test_arg_util.parse_test_args()
    self._service_registry: dict[str, Any] = {}
    self._service_factories: dict[str, Callable[[], Any]] = {}

    # Register core services
    self.register_service_factory('device_manager', self._create_device_manager)

  def register_service_factory(
      self, name: str, factory: Callable[[], Any]
  ) -> None:
    """
      Registers a function that knows how to create a service.
    """
    self._service_factories[name] = factory

  def __getattr__(self, name: str) -> Any:
    """
      Provides lazy-loaded access to services.

      When a service is accessed for the first time (e.g.,
      `context.service_name`),
      this method checks if an instance already exists. If not, it looks for a
      registered factory to create it.

      Args:
          name: The name of the service to access.

      Returns:
          The service instance.

      Raises:
          AttributeError: If the service is not registered.
    """
    if name not in self._service_registry:
      if name in self._service_factories:
        logging.info(f'{self._LOG_TAG}: Creating service <{name}> on demand.')
        self._service_registry[name] = self._service_factories[name]()
      else:
        raise AttributeError(
            f'{self._LOG_TAG}: Service <{name}> is not registered in the'
            ' TestContext.'
        )
    return self._service_registry[name]

  @functools.lru_cache(maxsize=3)
  def get_test_arg(
      self, name: str, default: Any = None, required: bool = False
  ) -> Any:
    """
      Retrieves a test argument.

      Args:
          name: The name of the argument.
          default: The default value to return if the argument is not found.
          required: If True, raises TestArgumentNotFoundError if not found.

      Returns:
          The value of the test argument.
    """
    value: Any = self._test_args.get(name, default)
    if required and value is None:
      raise error_handler.TestArgumentNotFoundError(
          f'{self._LOG_TAG}:Required test argument "{name}" not provided.'
      )
    return value

  def get_device(self, label: str) -> test_device.TestDevice:
    """
      Provides access to a specific test device.
    """
    return self.device_manager.get_device(label)

  def get_all_devices(self) -> list[test_device.TestDevice]:
    """
      Provides access to all test devices.
    """
    return self.device_manager.get_all_devices()

  def _create_device_manager(self) -> test_device_manager.DeviceManager:
    """
      Factory method for creating the DeviceManager.
    """
    num_devices: int = self._DEFAULT_NUMBER_OF_DEVICES
    if self._NUMBER_OF_DEVICES_KEY in self._test_instance.user_params:
      num_devices = self._test_instance.user_params[self._NUMBER_OF_DEVICES_KEY]
    else:
      logging.warning(
          '%s: %s is not in testbed config. Using default value: %d',
          self._LOG_TAG,
          self._NUMBER_OF_DEVICES_KEY,
          self._DEFAULT_NUMBER_OF_DEVICES,
      )
    logging.info(
        '%s: Registering %d device(s) for test execution',
        self._LOG_TAG,
        num_devices,
    )
    ads: list[android_device.AndroidDevice] = (
        self._test_instance.register_controller(
            android_device, min_number=num_devices
        )
    )
    return test_device_manager.DeviceManager(ads, num_devices)
