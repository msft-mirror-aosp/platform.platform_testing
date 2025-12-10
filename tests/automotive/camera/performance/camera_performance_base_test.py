# Copyright 2025 Google LLC

import logging
import time

from camera_performance.utils import constants
from spectatio_host_tf.core import test_base
from utilities.crystalball_metrics_utils import export_to_crystalball


class CameraPerformanceBaseTestClass(test_base.SpectatioHostBaseTestClass):
  """
   A base test class for camera performance tests.
  """
  _REPEAT = 10
  # time out to send vhal signal to auto device
  _TIME_OUT: float = 3.0

  def setup_class(self) -> None:
    super().setup_class()
    for device in self.context.get_all_devices():
      self.enable_car_service(device)

  def setup_test(self) -> None:
    super().setup_test()
    logging.info(
          f'{self.test_class_name}: Running test: {self.current_test_info.name}')
    for device in self.context.get_all_devices():
      device.adb.root()
      self.inject_vhal_signal(
          device,
          constants.GEAR_VHAL_EVENT,
          constants.GEAR_PARK_VHAL_VALUE,
      )

  def enable_car_service(self,device) -> None:
    """Enables car service and roots the device for a given device.

    Args:
      device: The device object to configure.
    """
    device.adb.execute_shell_command(
        f'cmd car_service enable-feature {constants.CAR_EVS_SERVICE}'
    )
    device.adb.reboot()

  def inject_vhal_signal(self, device, vhal_event, vhal_value):
    """Injects a vhal signal to the device.
    Args:
      device: The device object to inject the vhal signal.
      vhal_event: The vhal event to inject.
      vhal_value: The vhal value to inject.
    """
    device.adb.execute_shell_command(
        f'cmd car_service inject-vhal-event {vhal_event} {vhal_value}'
    )
    time.sleep(self._TIME_OUT)

  def measure_boottime_and_export_metrics(self, properties: list[str]):
    """Measures boottime for a list of properties and exports to crystalball.

    Args:
      properties: A list of string properties to measure.
    """
    boottime_metrics_avg = {prop: 0 for prop in properties}

    for _ in range(self._REPEAT):
      self.device1.adb.reboot()
      self.device1.adb.wait_for_boot_complete()

      for prop in properties:
        boottime = self.device1.adb.getprop(prop)
        logging.info(f'{prop}: {boottime}')
        if boottime:
          boottime_metrics_avg[prop] += int(boottime)

    for prop in properties:
      if self._REPEAT > 0:
        boottime_metrics_avg[prop] /= self._REPEAT

    metrics_in_ms = {
        prop: self.convert_ns_to_ms(avg_boottime)
        for prop, avg_boottime in boottime_metrics_avg.items()
    }
    export_to_crystalball(
        metrics_in_ms, self.log_path, self.current_test_info.name)

  def convert_ns_to_ms(self, time_ns):
    """
     Converts the nanoseconds to milliseconds.
    """
    return int(time_ns / 1000000)
