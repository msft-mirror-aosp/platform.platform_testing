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

"""Mobly base test class for Neaby Connections."""

import dataclasses
import logging
import time

from mobly import asserts
from mobly import base_test
from mobly import records
from mobly import utils
from mobly.controllers import android_device
from mobly.controllers.android_device_lib import errors

from performance_test import nc_constants
from performance_test import setup_utils

NEARBY_SNIPPET_PACKAGE_NAME = 'com.google.android.nearby.mobly.snippet'
NEARBY_SNIPPET_2_PACKAGE_NAME = 'com.google.android.nearby.mobly.snippet.second'


class NCBaseTestClass(base_test.BaseTestClass):
  """Nearby Connection E2E tests."""

  def __init__(self, configs):
    super().__init__(configs)
    self.ads: list[android_device.AndroidDevice] = []
    self.advertiser: android_device.AndroidDevice = None
    self.discoverer: android_device.AndroidDevice = None
    self.test_parameters: nc_constants.TestParameters = None
    self._nearby_snippet_apk_path: str = None
    self._nearby_snippet_2_apk_path: str = None

  def setup_class(self) -> None:
    self.ads = self.register_controller(android_device, min_number=2)
    self.test_parameters = self._get_test_parameter()
    self._nearby_snippet_apk_path = self.user_params.get('files', {}).get(
        'nearby_snippet',[''])[0]
    self._nearby_snippet_2_apk_path = self.user_params.get('files', {}).get(
        'nearby_snippet_2',[''])[0]

    utils.concurrent_exec(
        self._setup_android_device,
        param_list=[[ad] for ad in self.ads],
        raise_on_exception=True,
    )

    try:
      self.discoverer = android_device.get_device(
          self.ads, role='source_device')
      self.advertiser = android_device.get_device(
          self.ads, role='target_device')
    except errors.Error:
      logging.warning('The source,target devices are not specified in testbed;'
                      'The result may not be expected.')
      self.advertiser, self.discoverer = self.ads

  def _setup_android_device(self, ad: android_device.AndroidDevice) -> None:
    asserts.skip_if(
        not ad.is_adb_root,
        'The test only can run on userdebug build.',
    )

    ad.debug_tag = ad.serial + '(' + ad.adb.getprop('ro.product.model') + ')'
    ad.log.info('try to install nearby_snippet_apks')
    if self._nearby_snippet_apk_path:
      setup_utils.install_apk(ad, self._nearby_snippet_apk_path)
    else:
      ad.log.warn('nearby_snippet apk is not specified, '
                  'make sure it is installed in the device')
    if self._nearby_snippet_2_apk_path:
      setup_utils.install_apk(ad, self._nearby_snippet_2_apk_path)
    else:
      ad.log.warn('nearby_snipet_2 apk is not specified, '
                  'make sure it is installed in the device')
    ad.load_snippet('nearby', NEARBY_SNIPPET_PACKAGE_NAME)
    ad.load_snippet('nearby2', NEARBY_SNIPPET_2_PACKAGE_NAME)

    ad.log.info('grant manage external storage permission')
    setup_utils.grant_manage_external_storage_permission(
        ad, NEARBY_SNIPPET_PACKAGE_NAME
    )
    setup_utils.grant_manage_external_storage_permission(
        ad, NEARBY_SNIPPET_2_PACKAGE_NAME
    )

    setup_utils.enable_logs(ad)

    setup_utils.disable_redaction(ad)

    self._disconnect_from_wifi(ad)

    setup_utils.enable_bluetooth_multiplex(ad)

    if (
        self.test_parameters.upgrade_medium
        == nc_constants.NearbyMedium.WIFIAWARE_ONLY.value
    ):
      setup_utils.enable_wifi_aware(ad)

    if self.test_parameters.wifi_country_code:
      setup_utils.set_wifi_country_code(
          ad, self.test_parameters.wifi_country_code
      )

    if not ad.nearby.wifiIsEnabled():
      ad.nearby.wifiEnable()

  def _teardown_device(self, ad: android_device.AndroidDevice) -> None:
    ad.nearby.transferFilesCleanup()
    if self.test_parameters.disconnect_wifi_after_test:
      self._disconnect_from_wifi(ad)
    ad.unload_snippet('nearby')
    ad.unload_snippet('nearby2')

  def teardown_test(self) -> None:
    utils.concurrent_exec(
        lambda d: d.services.create_output_excerpts_all(self.current_test_info),
        param_list=[[ad] for ad in self.ads],
        raise_on_exception=True)

  def teardown_class(self) -> None:
    utils.concurrent_exec(
        self._teardown_device,
        param_list=[[ad] for ad in self.ads],
        raise_on_exception=True,
    )

  def _get_test_parameter(self) -> nc_constants.TestParameters:
    test_parameters_names = {
        field.name for field in dataclasses.fields(nc_constants.TestParameters)
    }
    test_parameters = nc_constants.TestParameters(
        **{key: val for key, val in self.user_params.items(
            ) if key in test_parameters_names}
    )

    return test_parameters

  def on_fail(self, record: records.TestResultRecord) -> None:
    logging.info('take bug report for failure')
    android_device.take_bug_reports(
        self.ads,
        destination=self.current_test_info.output_path,
    )

  def _disconnect_from_wifi(self, ad: android_device.AndroidDevice) -> None:
    ad.nearby.wifiClearConfiguredNetworks()
    time.sleep(nc_constants.WIFI_DISCONNECTION_DELAY.total_seconds())
