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
