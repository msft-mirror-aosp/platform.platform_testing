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
SDV VHAL Proxy SDV Subscriber IVI Publisher Test
"""

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling


class SdvVhalProxySdvSubscriberIviPublisherTest(
    sdv_base_test.SdvBaseTestClass
):
    """
    Test class for VHAL proxy publisher-subscriber communication.
    """

    SDV_RANGE_REMAINING_SUBSCRIBER_CMD = (
        '/vendor/bin/sdv_range_remaining_subscriber'
    )
    IVI_SET_PROPERTY_CMD = (
        'cmd car_service set-property-value RANGE_REMAINING 0 {set_value}'
    )
    LOG_TAG = 'SdvRangeRemaining'
    EXPECTED_VHAL_PROXY_LOG = 'Some(F32({expect_value}))'
    TEST_VALUE = 23.3233

    def setup_class(self):
        """
        Initializes device objects.
        """
        super().setup_class()
        # device1 is the IVI VM, device2 is the SDV Core VM.
        self.ivi_vm_device = self.get_device('device1').adb()
        self.core_vm_device = self.get_device('device2').adb()

    def test_sdv_subscriber_ivi_publisher(self):
        """
        Verifies that a VHAL property publisher on an IVI VM can
        successfully send events to a subscriber on an SDV Core VM through the
        VHAL proxy.
        """
        # Start the mock range remaining subscriber on the SDV Core VM in the
        # background.
        self.core_vm_device.execute_shell_command_in_subprocess(
            self.SDV_RANGE_REMAINING_SUBSCRIBER_CMD,
            self.SDV_RANGE_REMAINING_SUBSCRIBER_CMD,
        )

        # Set the RANGE_REMAINING property on the IVI VM.
        set_property_cmd = self.IVI_SET_PROPERTY_CMD.format(
            set_value=self.TEST_VALUE
        )
        self.ivi_vm_device.execute_shell_command(set_property_cmd)

        # Wait for and verify that the SdvRangeRemaining subscriber on the
        # SDV Core VM receives the new event with the correct value.
        expected_log = self.EXPECTED_VHAL_PROXY_LOG.format(
            expect_value=self.TEST_VALUE
        )
        polling.wait_and_verify_expected_logs(
            self.core_vm_device,
            grep_text=expected_log,
            timeout=60,
            assert_msg=(
                'SdvRangeRemaining subscriber did not receive new events from'
                ' the IVI publisher.'
            ),
        )

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()