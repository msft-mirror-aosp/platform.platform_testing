#  Copyright (C) 2025 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import logging
import time

from bluetooth_performance_test import bluetooth_performance_base_test
from utilities.main_utils import common_main
from utilities.crystalball_metrics_utils import export_to_crystalball

class BTPerformanceConnectionTest(bluetooth_performance_base_test.BluetoothPerformanceBaseTest):
  """Test Class for Bluetooth Connection Test."""

  def test_connection(self):
    """Test for connecting/disconnecting bluetooth profiles between a HU and a bluetooth device"""
    metrics = {}
    for i in range(1, self.iterations + 1):
      logging.info(f'Connection iteration {i}')
      try:
        self.bt_utils.connect_profiles()
        self.connection_test_process_per_iteration_metrics(metrics)
      except:
        logging.error(f'Failed to connect bluetooth profiles on iteration {i}')
      try:
        self.bt_utils.disconnect_profiles()
      except:
        logging.error(f'Failed to disconnect bluetooth profiles on iteration {i}')
      time.sleep(self.iteration_delay)
    self.connection_test_process_per_test_metrics(metrics)
    export_to_crystalball(metrics, self.log_path, self.current_test_info.name)

if __name__ == '__main__':
  common_main()
