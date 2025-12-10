# Copyright 2025 Google LLC

from camera_performance import camera_performance_base_test
from spectatio_host_tf.core import test_runner


class EvsBootTimeTest(
    camera_performance_base_test.CameraPerformanceBaseTestClass
):
  """
   A test to measure the performance of the EVS service boottime.
  """

  def test_evs_boottime(self):
    """
     Tests the performance of the EVS boottime.
    """
    evs_manager_boottime_prop = 'ro.boottime.evsmanagerd'
    evs_driver_boottime_prop = 'ro.boottime.evs_driver'

    self.measure_boottime_and_export_metrics(
        [evs_manager_boottime_prop, evs_driver_boottime_prop]
    )

if __name__ == '__main__':
  test_runner.run()
