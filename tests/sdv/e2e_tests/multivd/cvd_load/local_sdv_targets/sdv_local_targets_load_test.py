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

"""Cvd Load Test for Local SDV Targets

Tests SDV device availability which are started using cvd load
"""
from mobly import asserts
import logging

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvLocalTargetsLoadTest(sdv_base_test.SdvBaseTestClass):

    def setup_class(self):
        logging.info('Setting Up SDV Local Targets Load Test Class')
        super().setup_class()
        # Get the device uisng label
        self.sdv_device_server = self.get_device('device1')
        self.sdv_device_client = self.get_device('device2')
        logging.info('End Setup Class For SDV Local Targets Load Test')

    def setup_test(self):
        logging.info('Start SDV Local Targets Load Test Setup')
        super().setup_test()
        logging.info('End SDV Local Targets Load Test Setup')

    def test_sdv_local_target_load(self):
        logging.info('Start SDV Local Targets Load Test: test_cvd_load_local')

        product_device_server = (
            self.sdv_device_server.adb().execute_shell_command(
                'getprop | grep -i ro.product.device '
            )
        )
        asserts.assert_in(
            'sdv_core_cf',
            product_device_server,
            f'ro.product.device [{product_device_server}] does not contain'
            ' Expected Result [sdv_core_cf]',
        )

        product_device_client = (
            self.sdv_device_client.adb().execute_shell_command(
                'getprop | grep -i ro.product.device '
            )
        )
        asserts.assert_in(
            'sdv_core_cf',
            product_device_client,
            f'ro.product.device [{product_device_client}] does not contain'
            ' Expected Result [sdv_core_cf]',
        )

        logging.info('End SDV Local Targets Load Test: test_cvd_load_local')

    def teardown_test(self):
        logging.info('Start SDV Local Targets Load Test Teardown')
        super().teardown_test()
        logging.info('End SDV Local Targets Load Test Teardown')


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
