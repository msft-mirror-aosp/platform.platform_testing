#  Copyright (C) 2023 The Android Open Source Project
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

"""Stress tests for Neaby Connections used by the quick start flow."""

import datetime
import os
import logging
import sys
import time

# check the python version
if sys.version_info < (3,10):
  logging.error('The test only can run on python 3.10 and above')
  exit()

from mobly import asserts
from mobly import base_test
from mobly import test_runner

# Allows local imports to be resolved via relative path, so the test can be run
# without building.
_performance_test_dir = os.path.dirname(os.path.dirname(__file__))
if _performance_test_dir not in sys.path:
  sys.path.append(_performance_test_dir)

from performance_test import nc_base_test
from performance_test import nc_constants
from performance_test import nearby_connection_wrapper
from performance_test import setup_utils

_PERFORMANCE_TEST_REPEAT_COUNT = 100
_PERFORMANCE_TEST_MAX_CONSECUTIVE_ERROR = 10

_DELAY_BETWEEN_EACH_TEST_CYCLE = datetime.timedelta(seconds=5)
_TRANSFER_FILE_SIZE_1MB = 1024
_TRANSFER_FILE_SIZE_1GB = 1024 * 1024


class QuickStartStressTest(nc_base_test.NCBaseTestClass):
  """Nearby Connection E2E stress tests for quick start flow."""

  performance_test_iterations: int

  def __init__(self, configs):
    super().__init__(configs)
    self._test_result: nc_constants.SingleTestResult = (
        nc_constants.SingleTestResult())
    self._quick_start_test_metrics: nc_constants.QuickStartTestMetrics = (
        nc_constants.QuickStartTestMetrics())

  def _reset_nearby_connections(self) -> None:
    """Resets all nearby connections."""
    self.discoverer.nearby.stopDiscovery()
    self.discoverer.nearby.stopAllEndpoints()
    self.advertiser.nearby.stopAdvertising()
    self.advertiser.nearby.stopAllEndpoints()
    self.discoverer.nearby2.stopDiscovery()
    self.discoverer.nearby2.stopAllEndpoints()
    self.advertiser.nearby2.stopAdvertising()
    self.advertiser.nearby2.stopAllEndpoints()
    time.sleep(nc_constants.NEARBY_RESET_WAIT_TIME.total_seconds())

  def _reset_wifi_connection(self) -> None:
    """Resets wifi connections on both devices."""
    self.discoverer.nearby.wifiClearConfiguredNetworks()
    self.advertiser.nearby.wifiClearConfiguredNetworks()
    time.sleep(nc_constants.WIFI_DISCONNECTION_DELAY.total_seconds())

  def setup_test(self):
    super().setup_test()
    self._reset_wifi_connection()
    self._reset_nearby_connections()
    if self.test_parameters.toggle_airplane_mode_target_side:
      setup_utils.toggle_airplane_mode(self.advertiser)

  def setup_class(self):
    super().setup_class()
    self.performance_test_iterations = getattr(
        self.test_quick_start_performance, base_test.ATTR_REPEAT_CNT)
    logging.info('performance test iterations: %s',
                 self.performance_test_iterations)

  def teardown_class(self):
    super().teardown_class()
    # handle summary results
    self._summary_test_results()

  @base_test.repeat(
      count=_PERFORMANCE_TEST_REPEAT_COUNT,
      max_consecutive_error=_PERFORMANCE_TEST_MAX_CONSECUTIVE_ERROR)
  def test_quick_start_performance(self) -> None:
    """Stress test for quick start flow."""
    try:
      self._mimic_quick_start_test(
          self.discoverer,
          self.advertiser,
          wifi_ssid=self.test_parameters.wifi_ssid,
          wifi_password=self.test_parameters.wifi_password,
      )
    finally:
      self._write_current_test_report()
      self._collect_current_test_metrics()
      time.sleep(_DELAY_BETWEEN_EACH_TEST_CYCLE.total_seconds())

  def _mimic_quick_start_test(
      self,
      discoverer,
      advertiser,
      wifi_ssid: str = '',
      wifi_password: str = '',
  ) -> None:
    """Mimics quick start flow test with 2 nearby connections."""
    # 1. discoverer connect to wifi wlan
    self._test_result = nc_constants.SingleTestResult()
    if wifi_ssid:
      discoverer_wifi_latency = (
          setup_utils.connect_to_wifi_wlan_till_success(
              discoverer, wifi_ssid, wifi_password))
      discoverer.log.info(
          'connecting to wifi in '
          f'{round(discoverer_wifi_latency.total_seconds())} s')
      self._test_result.discoverer_wifi_wlan_expected = True
      self._test_result.discoverer_wifi_wlan_latency = discoverer_wifi_latency

    # 2. set up 1st connection
    nearby_snippet_1 = nearby_connection_wrapper.NearbyConnectionWrapper(
        advertiser,
        discoverer,
        advertiser.nearby,
        discoverer.nearby,
        advertising_discovery_medium=nc_constants.NearbyMedium.BLE_ONLY,
        connection_medium=nc_constants.NearbyMedium.BT_ONLY,
        upgrade_medium=nc_constants.NearbyMedium.BT_ONLY,
    )
    first_connection_setup_timeouts = nc_constants.ConnectionSetupTimeouts(
        nc_constants.FIRST_DISCOVERY_TIMEOUT,
        nc_constants.FIRST_CONNECTION_INIT_TIMEOUT,
        nc_constants.FIRST_CONNECTION_RESULT_TIMEOUT)

    try:
      nearby_snippet_1.start_nearby_connection(
          timeouts=first_connection_setup_timeouts,
          medium_upgrade_type=nc_constants.MediumUpgradeType.NON_DISRUPTIVE)
    finally:
      self._test_result.first_connection_setup_quality_info = (
          nearby_snippet_1.connection_quality_info
      )

    # 3. transfer file through bluetooth
    file_1_mb = _TRANSFER_FILE_SIZE_1MB
    self._test_result.first_bt_transfer_throughput_kbs = (
        nearby_snippet_1.transfer_file(
            file_1_mb, nc_constants.FILE_1M_PAYLOAD_TRANSFER_TIMEOUT,
            nc_constants.PayloadType.FILE))

    # second Wifi connection and transfer
    # 4. advertiser connect to wifi wlan
    if wifi_ssid:
      advertiser_wlan_latency = setup_utils.connect_to_wifi_wlan_till_success(
          advertiser, wifi_ssid, wifi_password)
      advertiser.log.info('connecting to wifi in '
                          f'{round(advertiser_wlan_latency.total_seconds())} s')
      self._test_result.advertiser_wifi_wlan_expected = True
      self._test_result.advertiser_wifi_wlan_latency = advertiser_wlan_latency

    # 5. set up 2nd connection
    nearby_snippet_2 = nearby_connection_wrapper.NearbyConnectionWrapper(
        advertiser,
        discoverer,
        advertiser.nearby2,
        discoverer.nearby2,
        advertising_discovery_medium=nc_constants.NearbyMedium.BLE_ONLY,
        connection_medium=nc_constants.NearbyMedium.BT_ONLY,
        upgrade_medium=nc_constants.NearbyMedium.UPGRADE_TO_ALL_WIFI,
    )
    second_connection_setup_timeouts = nc_constants.ConnectionSetupTimeouts(
        nc_constants.SECOND_DISCOVERY_TIMEOUT,
        nc_constants.SECOND_CONNECTION_INIT_TIMEOUT,
        nc_constants.SECOND_CONNECTION_RESULT_TIMEOUT)
    try:
      nearby_snippet_2.start_nearby_connection(
          timeouts=second_connection_setup_timeouts,
          medium_upgrade_type=nc_constants.MediumUpgradeType.DISRUPTIVE,
      )
    finally:
      self._test_result.second_connection_setup_quality_info = (
          nearby_snippet_2.connection_quality_info
      )
      self._test_result.second_connection_setup_quality_info.medium_upgrade_expected = (
          True
      )

    # 6. transfer file through wifi
    file_1_gb = _TRANSFER_FILE_SIZE_1GB
    self._test_result.second_wifi_transfer_throughput_kbs = (
        nearby_snippet_2.transfer_file(
            file_1_gb, nc_constants.FILE_1G_PAYLOAD_TRANSFER_TIMEOUT,
            self.test_parameters.payload_type))

    # 7. disconnect 1st connection
    nearby_snippet_1.disconnect_endpoint()
    # 8. disconnect 2nd connection
    nearby_snippet_2.disconnect_endpoint()

  def _write_current_test_report(self) -> None:
    """Writes test report for each iteration."""

    quality_info = {
        '1st connection': str(
            self._test_result.first_connection_setup_quality_info),
        'bt_kBps': str(
            self._test_result.first_bt_transfer_throughput_kbs),
        '2nd connection': str(
            self._test_result.second_connection_setup_quality_info),
        'wifi_kBps': str(
            self._test_result.second_wifi_transfer_throughput_kbs),
    }

    if self._test_result.discoverer_wifi_wlan_expected:
      quality_info['src_wifi_connection'] = str(
          round(self._test_result.discoverer_wifi_wlan_latency.total_seconds())
      )
    if self._test_result.advertiser_wifi_wlan_expected:
      quality_info['tgt_wifi_connection'] = str(
          round(self._test_result.advertiser_wifi_wlan_latency.total_seconds())
      )
    test_report = {'quality_info': str(quality_info)}

    self.discoverer.log.info(test_report)
    self.record_data({
        'Test Class': self.TAG,
        'Test Name': self.current_test_info.name,
        'sponge_properties': test_report,
    })

  def _collect_current_test_metrics(self) -> None:
    """Collects test result metrics for each iteration."""
    self._quick_start_test_metrics.first_discovery_latencies.append(
        self._test_result.first_connection_setup_quality_info.discovery_latency
    )
    self._quick_start_test_metrics.first_connection_latencies.append(
        self._test_result.first_connection_setup_quality_info.connection_latency
    )
    self._quick_start_test_metrics.bt_transfer_throughputs_kbs.append(
        self._test_result.first_bt_transfer_throughput_kbs
    )

    self._quick_start_test_metrics.second_discovery_latencies.append(
        self._test_result.second_connection_setup_quality_info.discovery_latency
    )
    self._quick_start_test_metrics.second_connection_latencies.append(
        self._test_result.second_connection_setup_quality_info.connection_latency
    )
    self._quick_start_test_metrics.second_medium_upgrade_latencies.append(
        self._test_result.second_connection_setup_quality_info.medium_upgrade_latency
    )
    self._quick_start_test_metrics.wifi_transfer_throughputs_kbs.append(
        self._test_result.second_wifi_transfer_throughput_kbs
    )
    self._quick_start_test_metrics.discoverer_wifi_wlan_latencies.append(
        self._test_result.discoverer_wifi_wlan_latency)
    self._quick_start_test_metrics.advertiser_wifi_wlan_latencies.append(
        self._test_result.advertiser_wifi_wlan_latency)

  def _stats_throughput_result(
      self,
      throughput_indicators: list[float],
      throughput_benchmark: float,
      target_reach_percentile: int,
  ) -> nc_constants.ResultStats:
    """Statistics the throughput test result of all iterations."""
    n = self.performance_test_iterations
    reach_count = 0
    success_count = 0
    for throughput in throughput_indicators:
      if round(throughput, 1) != nc_constants.UNSET_THROUGHPUT_KBS:
        success_count += 1
        if round(throughput, 1) >= throughput_benchmark:
          reach_count += 1
    reach_rate = round(reach_count * 100.0 / n, 1)
    success_rate = round(success_count * 100.0 / n, 1)
    reach_target = True
    if reach_rate < target_reach_percentile:
      reach_target = False
    return nc_constants.ResultStats(reach_target, reach_rate, success_rate)

  def _summary_test_results(self) -> None:
    """Summarizes test results of all iterations."""
    fail_targets: list[nc_constants.FailTargetSummary] = []
    reach_target: bool = True

    first_bt_transfer_stats = self._stats_throughput_result(
        self._quick_start_test_metrics.bt_transfer_throughputs_kbs,
        self.test_parameters.bt_transfer_throughput_benchmark_kbs,
        self.test_parameters.bt_transfer_throughput_kbs_percentile,
    )
    reach_target &= first_bt_transfer_stats.reach_target
    if not first_bt_transfer_stats.reach_target:
      fail_targets.append(
          nc_constants.FailTargetSummary(
              'first_bt_transfer_reach_rate',
              first_bt_transfer_stats.reach_rate,
              self.test_parameters.bt_transfer_throughput_kbs_percentile))

    second_wifi_transfer_stats = self._stats_throughput_result(
        self._quick_start_test_metrics.wifi_transfer_throughputs_kbs,
        self.test_parameters.wifi_transfer_throughput_benchmark_kbs,
        self.test_parameters.wifi_transfer_throughput_kbs_percentile,
    )
    reach_target &= second_wifi_transfer_stats.reach_target
    if not second_wifi_transfer_stats.reach_target:
      fail_targets.append(
          nc_constants.FailTargetSummary(
              'second_wifi_transfer_reach_rate',
              second_wifi_transfer_stats.reach_rate,
              self.test_parameters.wifi_transfer_throughput_kbs_percentile))

    self.record_data({
        'Test Class': self.TAG,
        'sponge_properties': {
            'test_report_alias_name': (
                self.test_parameters.test_report_alias_name),
            'source_device_serial': self.discoverer.serial,
            'target_device_serial': self.advertiser.serial,
            'source_GMS_version': setup_utils.dump_gms_version(
                self.discoverer),
            'target_GMS_version': setup_utils.dump_gms_version(
                self.advertiser),
            'all_iterations': self.performance_test_iterations,
            'all_qualities_reach_target': reach_target,
            '1st_bt_transfer_reach_rate': first_bt_transfer_stats.reach_rate,
            '1st_bt_transfer_throughput_benchmark_kbs': (
                self.test_parameters.bt_transfer_throughput_benchmark_kbs),
            '1st_bt_transfer_throughput_kbs_percentile': (
                self.test_parameters.bt_transfer_throughput_kbs_percentile),
            '1st_bt_transfer_success_rate': (
                first_bt_transfer_stats.success_rate),

            '2nd_wifi_transfer_reach_rate': (
                second_wifi_transfer_stats.reach_rate),
            '2nd_wifi_transfer_throuput_benchmark_kbs': (
                self.test_parameters.wifi_transfer_throughput_benchmark_kbs),
            '2nd_wifi_transfer_throughput_percentile': (
                self.test_parameters.wifi_transfer_throughput_kbs_percentile),
            '2nd_wifi_transfer_success_rate': (
                second_wifi_transfer_stats.success_rate),
            }
        })
    if not reach_target:
      asserts.fail(self._generate_target_fail_message(fail_targets))

  def _generate_target_fail_message(
      self,
      fail_targets: list[nc_constants.FailTargetSummary]) -> str:
    error_msg = 'Failed due to:\n'
    for fail_target in fail_targets:
      error_msg += f'metric: {fail_target.name} is {fail_target.rate}, '
      error_msg += f'less than {fail_target.goal}.\n'

    return error_msg


if __name__ == '__main__':
  test_runner.main()
