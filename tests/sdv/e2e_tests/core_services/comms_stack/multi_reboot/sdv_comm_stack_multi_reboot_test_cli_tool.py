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

"""SDV Reboot Test"""

from mobly import asserts
import logging
import random
import time

from absl.testing import parameterized
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner


class SdvCommsStackMultiRebootTest(
    sdv_base_test.SdvBaseTestClass, parameterized.TestCase
):
    NUMBER_OF_CLIENT_DEVICE_REBOOTS = 10

    def setup_class(self):
        super().setup_class()

        self.adb_device_server = self.get_device('device1').adb()
        self.adb_device_client = self.get_device('device2').adb()

        self.original_authz_value_server = self.adb_device_server.prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        self.original_authz_value_client = self.adb_device_client.prop.get(SdvDeviceProperty.AUTHZ_ENABLE)

    def test_multi_reboot(self):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        self.adb_device_server.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'false')

        self.adb_device_server.execute_shell_command_in_subprocess('pub', 'sdv_comms_client_rs publish --package-name com.sdv.google.sample.foo --bundle-name ServiceBundleFoo --instance-name instance --service-unit-name com-sdv-google-sample-foo-foo-message-unique --service-unit-type FooMessage --quantity 100000 --interval-msec 500')

        # Wait to ensure the publisher has started and published some messages
        time.sleep(5)

        for _ in range(self.NUMBER_OF_CLIENT_DEVICE_REBOOTS):
            # Add a random sleep to simulate different timings of client reboots and introduce some randomness
            time.sleep(random.uniform(0.0, 5.0))
            self.adb_device_client.reboot_device()
            self.adb_device_client.wait_for_device_online()
            time.sleep(random.uniform(0.0, 1.0))
            self.adb_device_client.reboot_device()
            self.adb_device_client.wait_for_device_online()
            self.adb_device_client.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'false')
            sub_command_output = self.adb_device_client.execute_shell_command('sdv_comms_client_rs subscribe --package-name com.sdv.google.sample.foo --bundle-name ServiceBundleFoo --instance-name instance --service-unit-name com-sdv-google-sample-foo-foo-message-unique --quantity 1')
            asserts.assert_in('"Status":"Ok"', sub_command_output)

        self.adb_device_server.terminate_subprocess('pub')

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} ended'
        )

    def teardown_class(self):
        self.adb_device_server.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.original_authz_value_server)

        self.adb_device_client.prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.original_authz_value_client)



if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
