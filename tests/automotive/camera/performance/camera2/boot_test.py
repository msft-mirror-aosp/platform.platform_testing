# Copyright 2025 Google LLC

from camera_performance import camera_performance_base_test
from spectatio_host_tf.core import test_runner


class Camera2BootTest(
    camera_performance_base_test.CameraPerformanceBaseTestClass
):
  """
   A test to measure the performance of the camera2 service boottime.
  """

  def test_camera2_boottime(self):
    """
     Tests the performance of the camera2 boottime.
    """
    camera2_driver_boottime_prop = 'ro.boottime.aidl_camera_provider-V1-qti'
    cameraserver_boottime_prop = 'ro.boottime.cameraserver'

    self.measure_boottime_and_export_metrics(
        [camera2_driver_boottime_prop, cameraserver_boottime_prop]
    )


if __name__ == '__main__':
  test_runner.run()