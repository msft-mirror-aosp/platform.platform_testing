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

import jank_collector
import logging
import time

from bluetooth_test import bluetooth_base_test
from utilities.main_utils import common_main
from utilities.crystalball_metrics_utils import export_to_crystalball

ITERATIONS_PARAM_NAME = 'iterations'
METRIC_KEYS = 'metricKeys'
DEFAULT_ITERATIONS = 10
DEFAULT_ITERATION_DELAY_S = 2
DEFAULT_METRIC_KEYS = ['gfxinfo_com.android.car.dialer_janky_frames_percent-mean']

class BTPerformanceDialerJankTest(bluetooth_base_test.BluetoothBaseTest):
  """Test Class for Bluetooth Dialer Jank Test."""

  def setup_class(self):
    super().setup_class()
    self.call_utils.upload_vcf_contacts_to_device(self.target)
    self.bt_utils.pair_primary_to_secondary()
    self.iterations = DEFAULT_ITERATIONS
    self.iteration_delay = DEFAULT_ITERATION_DELAY_S
    self.metric_keys = DEFAULT_METRIC_KEYS
    if ITERATIONS_PARAM_NAME in self.user_params:
      self.iterations = self.user_params[ITERATIONS_PARAM_NAME]
    else:
      logging.info(f'{ITERATIONS_PARAM_NAME} is not in testbed config. Using default value')
    if METRIC_KEYS in self.user_params:
      self.metric_keys = self.user_params[METRIC_KEYS]
    else:
      logging.info(f'{METRIC_KEYS} is not in testbed config. Using default value')
    logging.info(f'Setup {self.__class__.__name__} with {METRIC_KEYS} = {self.metric_keys}, {ITERATIONS_PARAM_NAME} = {self.iterations} and iteration delay = {self.iteration_delay}')

  def setup_test(self):
    self.discoverer.services.register('jank_collector', jank_collector.JankCollector, jank_collector.JankCollectorConfig(tracked_packages=["com.android.car.dialer"]))

  def test_dialer_jank(self):
    self.bt_utils.allow_permissions_after_pairing()
    metrics = {}
    dialer_janky_frames_percent_list = []
    for i in range(1, self.iterations + 1):
      logging.info(f'test iteration {i}')
      self.discoverer.services.jank_collector.start_collecting()
      self.call_utils.open_phone_app()
      self.call_utils.open_contacts()
      self.bt_utils.allow_permissions_after_pairing()
      self.call_utils.scroll_down_one_page()
      self.call_utils.scroll_up_one_page()
      metric = self.discoverer.services.jank_collector.get_metrics()
      keys = list(metric.keys())
      for key in keys:
        if 'dialer_janky_frames_percent' in key:
          dialer_janky_frames_percent_list.append(metric[key])
        metric[str(i) + '_' + key] = metric.pop(key)
      metrics.update(metric)
      time.sleep(self.iteration_delay)
    for key in self.metric_keys:
      if 'dialer_janky_frames_percent-mean' in key:
        dialer_janky_frames_percent_mean = sum(dialer_janky_frames_percent_list) / len(dialer_janky_frames_percent_list)
        metrics[key] = round(dialer_janky_frames_percent_mean, 2)
    export_to_crystalball(metrics, self.log_path, self.current_test_info.name)

  def teardown_test(self):
    super().teardown_no_video_recording()

if __name__ == '__main__':
  common_main()