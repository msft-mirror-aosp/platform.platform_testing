#  Copyright (C) 2024 The Android Open Source Project
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

"""This Test is to test the Wifi SCC in a general case.

In this case, even though the expected wifi medium is the WFD, but the wifi D2D
could be any technologies, such as WFD, HOTSPOT, WifiLAN; Once the WFD is
failed, other meidums will be tried. Both the D2D and STA are using the same 2G
channel.

The device requirements:
  support 5G: false
The AP requirements:
  wifi channel: 6 (2437)
"""

import datetime
import logging
import os
import sys

# Allows local imports to be resolved via relative path, so the test can be run
# without building.
_betocq_dir = os.path.dirname(os.path.dirname(__file__))
if _betocq_dir not in sys.path:
  sys.path.append(_betocq_dir)

from mobly  import base_test
from mobly import test_runner

from betocq import d2d_performance_test_base
from betocq import nc_constants


class Scc2gAllWifiStaTest(d2d_performance_test_base.D2dPerformanceTestBase):
  """Test class for Wifi SCC 2G test associated with a specified CUJ."""

  def _get_country_code(self) -> str:
    return 'US'

  def setup_class(self):
    super().setup_class()
    self._is_2g_d2d_wifi_medium = True
    self.performance_test_iterations = getattr(
        self.test_scc_2g_all_wifi_sta, base_test.ATTR_REPEAT_CNT
    )
    logging.info(
        'performance test iterations: %s', self.performance_test_iterations
    )

  @base_test.repeat(
      count=nc_constants.SCC_PERFORMANCE_TEST_COUNT,
      max_consecutive_error=nc_constants.SCC_PERFORMANCE_TEST_MAX_CONSECUTIVE_ERROR,
  )
  def test_scc_2g_all_wifi_sta(self):
    """Test the 2G SCC case, both the wifi D2D medium and STA are using 2G."""
    self._test_connection_medium_performance(
        upgrade_medium_under_test=nc_constants.NearbyMedium.UPGRADE_TO_ALL_WIFI,
        wifi_ssid=self.test_parameters.wifi_2g_ssid,
        wifi_password=self.test_parameters.wifi_2g_password)

  def _get_transfer_file_size(self) -> int:
    # For 2G wifi medium
    return nc_constants.TRANSFER_FILE_SIZE_20MB

  def _get_file_transfer_timeout(self) -> datetime.timedelta:
    return nc_constants.WIFI_2G_20M_PAYLOAD_TRANSFER_TIMEOUT

  def _get_file_transfer_failure_tip(self) -> str:
    upgraded_medium_name = None
    if (self._current_test_result.file_transfer_nc_setup_quality_info.upgrade_medium
        is not None):
      upgraded_medium_name = (
          self._current_test_result.file_transfer_nc_setup_quality_info.upgrade_medium.name
      )
    return (
        f'The upgraded wifi medium {upgraded_medium_name} might be broken, '
        f'check the related log; Or {self._get_throughput_low_tip()}'
    )

  def _get_throughput_low_tip(self) -> str:
    upgraded_medium_name = None
    if (self._current_test_result.file_transfer_nc_setup_quality_info.upgrade_medium
        is not None):
      upgraded_medium_name = (
          self._current_test_result.file_transfer_nc_setup_quality_info.upgrade_medium.name
      )
    return (
        f'{self._throughput_low_string}. The upgraded medium is'
        f' {upgraded_medium_name}, this is a 2G SCC case. Check with the wifi'
        ' chip vendor for any FW issue in this mode.'
    )

  def _is_wifi_ap_ready(self) -> bool:
    return True if self.test_parameters.wifi_2g_ssid else False

  def _are_devices_capabilities_ok(self) -> bool:
    return not self.discoverer.supports_5g or not self.advertiser.supports_5g


if __name__ == '__main__':
  test_runner.main()