# Copyright 2025 Google LLC

import logging
import re

from mobly.controllers import android_device
from spectatio_host_tf.core import test_device
from spectatio_host_tf.utils import error_handler


class DeviceManager:
  """
    Manages Android device discovery, ordering, and access for Spectatio tests.

    This class encapsulates the logic for handling multiple test devices,
    including a mechanism to ensure consistent device ordering across test runs
    by using a device property. This avoidstest flakiness in CI/CD
    environments where device order is not guaranteed.
  """

  _LOG_TAG = 'DeviceManager'

  def __init__(
      self, ads: list[android_device.AndroidDevice], num_of_devices: int
  ) -> None:
    self._ads: list[android_device.AndroidDevice] = ads
    self._num_of_devices: int = num_of_devices
    self._devices: dict[str, test_device.TestDevice] = (
        self._discover_and_order_devices()
    )

  def get_device(self, device_label: str) -> test_device.TestDevice:
    """
      Retrieves a device by its label.

      Args:
          device_label: The label of the device to retrieve (e.g., 'device1').

      Returns:
          A TestDevice object.

      Raises:
          DeviceNotFoundError: If no device with the given label is found.
    """
    if device_label not in self._devices:
      raise error_handler.DeviceNotFoundError(
          f'Device with label "{device_label}" not available. '
          f'Available devices: {list(self._devices.keys())}'
      )
    return self._devices[device_label]

  def get_all_devices(self) -> list[test_device.TestDevice]:
    """
      Returns a list of all managed TestDevice objects.
    """
    return self._devices.values()

  def _get_instance_name(self, device: test_device.TestDevice) -> str:
    return device.adb.execute_shell_command(
        'getprop ro.boot.sdv.instance_name', raise_exception=False
    )

  def _get_instance_number(self, instance_name: str) -> int:
    return int(instance_name[len('instance') :])

  def _is_instance_name_in_valid_format(self, instance_name: str) -> bool:
    return instance_name and re.search('^instance[0-9]+$', instance_name)

  def _is_instance_number_valid(self, instance_name: str) -> bool:
    instance_number: int = self._get_instance_number(instance_name)
    return 0 < instance_number <= self._num_of_devices

  def _is_device_tag_update_needed(
      self, instance_name: str, device_number: int
  ) -> bool:
    # Check if instance_name exist
    # and if instance_name is in the format of instance{number}
    # and if the instance id is between 1 and self.__num_of_devices,
    # and if the instance id does not match current device number
    # then update the device tag
    return (
        self._is_instance_name_in_valid_format(instance_name)
        and self._is_instance_number_valid(instance_name)
        and self._get_instance_number(instance_name) != device_number
    )

  def _log_assigned_devices(
      self, devices: dict[str, test_device.TestDevice], order_type: str
  ) -> None:
    assigned_devices: dict[str, str] = {
        tag: device.adb.serial for tag, device in devices.items()
    }
    logging.info(
        '%s: %s - devices: %s', self._LOG_TAG, order_type, assigned_devices
    )

  def _discover_and_order_devices(self) -> dict[str, test_device.TestDevice]:
    """
      Discovers and orders devices.

      It attempts to order devices based on the `ro.boot.sdv.instance_name`
      property. If this fails (e.g., duplicate or invalid instance names),
      it falls back to the default device order provided by Mobly.

      Returns:
          A dictionary mapping device tags (e.g., 'device1') to TestDevice
          objects.
    """
    logging.info(
        f'{self._LOG_TAG}: Discovering and ordering devices.'
    )
    logging.debug(
        f'{self._LOG_TAG}: Number of devices: {self._num_of_devices}.'
    )
    default_devices: dict[str, test_device.TestDevice] = {}
    ordered_devices: dict[str, test_device.TestDevice] = {}

    for i in range(self._num_of_devices):
      logging.debug(
          f'{self._LOG_TAG}: Discovering device: {i + 1}.'
      )
      original_tag: str = f'device{i + 1}'
      device: test_device.TestDevice = test_device.TestDevice(
          android_device.get_device(self._ads, label=original_tag)
      )
      default_devices[original_tag] = device

      instance_name: str = self._get_instance_name(device)

      logging.debug(
        f'{self._LOG_TAG}: Instance Name: {instance_name}'
      )

      final_tag: str = original_tag
      if self._is_device_tag_update_needed(instance_name, i + 1):
        logging.debug(
            f'{self._LOG_TAG}: Device Tag Update Needed: Original Tag:'
            f' {original_tag}, Instance Name: {instance_name}'
        )
        final_tag = f'device{self._get_instance_number(instance_name)}'
        logging.debug(
            f'{self._LOG_TAG}: Device Tag Update from Original Tag:'
            f' {original_tag} to Final Tag: {final_tag}'
        )

      ordered_devices[final_tag] = device

    logging.debug(
      f'{self._LOG_TAG}: Default devices: {default_devices.items()}'
    )

    logging.debug(
      f'{self._LOG_TAG}: Ordered devices: {ordered_devices.items()}'
    )

    if len(ordered_devices) != self._num_of_devices:
      logging.warning(
          f'{self._LOG_TAG}: Could not reliably order devices by instance name'
          'due to duplicates or invalid names. Falling back to default order.'
      )
      self._log_assigned_devices(
          default_devices, 'Assigned Device: Default Order'
      )
      return default_devices

    self._log_assigned_devices(
        ordered_devices, 'Assigned Device: Ordered by Instance Name'
    )
    return ordered_devices
