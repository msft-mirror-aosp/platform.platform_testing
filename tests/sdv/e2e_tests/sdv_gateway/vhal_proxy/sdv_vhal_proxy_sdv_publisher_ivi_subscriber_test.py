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

"""
SDV VHAL Proxy SDV Publisher IVI Subscriber Test
"""

import logging
import re

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods

class SdvVhalProxySdvPublisherIviSubscriberTest(
    sdv_base_test.SdvBaseTestClass
):
    """
    Test class for VHAL proxy publisher-subscriber communication.
    """

    SDV_TIRE_PRESSURE_CMD = '/vendor/bin/sdv_tire_pressure'
    VHAL_PROXY_LOG_TAG = 'VhalProxy'
    EXPECTED_VHAL_PROXY_LOG = 'New event'

    def setup_class(self):
        """
        Initializes device objects.
        """
        super().setup_class()
        # device1 is the IVI VM, device2 is the SDV Core VM.
        self.ivi_vm_device = self.get_device('device1').adb()
        self.core_vm_device = self.get_device('device2').adb()

    def setup_test(self):
        super().setup_test()

    def test_sdv_publisher_ivi_subscriber(self):
        """
        Verifies that a VHAL property publisher on an SDV Core VM can
        successfully send events to a subscriber on an IVI VM through the
        VHAL proxy.
        """
        # Start the mock tire pressure service on the SDV Core VM in the
        # background.
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.SDV_TIRE_PRESSURE_CMD, self.SDV_TIRE_PRESSURE_CMD
        )

        # Wait for and verify that the VhalProxy on the IVI VM receives new
        # events.
        WaitingMethods.wait_and_verify_expected_logs(
            self.ivi_vm_device,
            logcat_args=f'{self.VHAL_PROXY_LOG_TAG}:V *:S',
            grep_text=self.EXPECTED_VHAL_PROXY_LOG,
            timeout=60,
            assert_msg='VhalProxy did not receive new events from the sdv_tire_pressure publisher.',
        )


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
