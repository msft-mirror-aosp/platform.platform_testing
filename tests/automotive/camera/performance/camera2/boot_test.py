# Copyright 2025 Google LLC

import logging

from spectatio_host_tf.core import test_base
from spectatio_host_tf.core import test_runner
from utilities.crystalball_metrics_utils import export_to_crystalball

class Camera2BootTest(test_base.SpectatioHostBaseTestClass):

  """
   A test to measure the performance of the camera2 service boottime.
  """
  _REPEAT = 10

  def setup_test(self) -> None:
    super().setup_test()
    logging.info(
          f'{self.test_class_name}: Running test: {self.current_test_info.name}')

  def test_camera2_boottime(self):
    """
     Tests the performance of the camera2 boottime.
    """
    boottime_camera2_driver_avg = 0
    boottime_camera2_manager_avg = 0
    for unused_i in range(self._REPEAT):
      self.device1.adb.reboot()
      self.device1.adb.wait_for_boot_complete()

      boottime_camera2_driver = self.device1.adb.getprop('ro.boottime.aidl_camera_provider-V1-qti')
      logging.info(f'ro.boottime.aidl_camera_provider-V1-qti: {boottime_camera2_driver}')

      boottime_camera2_manager = self.device1.adb.getprop('ro.boottime.cameraserver')
      logging.info(f'ro.boottime.cameraserver: {boottime_camera2_manager}')

      boottime_camera2_driver_avg += int(boottime_camera2_driver)
      boottime_camera2_manager_avg += int(boottime_camera2_manager)

    boottime_camera2_driver_avg /= self._REPEAT
    boottime_camera2_manager_avg /= self._REPEAT
    metrics = {
        'ro.boottime.aidl_camera_provider-V1-qti': self.convert_ns_to_ms(boottime_camera2_driver_avg),
        'ro.boottime.cameraserver': self.convert_ns_to_ms(boottime_camera2_manager_avg),
    }
    export_to_crystalball(metrics, self.log_path, self.current_test_info.name)

  def convert_ns_to_ms(self, time_ns):
    """
     Converts the nanoseconds to milliseconds.
    """
    return int(time_ns / 1000000)

if __name__ == '__main__':
  test_runner.run()