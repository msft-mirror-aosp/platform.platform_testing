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

"""SDV Two Devices Availability Test"""

from absl.testing import parameterized
from device_availability_common import SdvDeviceAvailability
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvTwoDevicesAvailabilityTest(
    sdv_base_test.SdvBaseTestClass,
    SdvDeviceAvailability,
    parameterized.TestCase,
):

    def setup_class(self):
        super().setup_class()
        self.sdv_device1 = self.get_device('device1')
        self.sdv_device2 = self.get_device('device2')

    def test_devices_availability(self):
        self.log_test_info(self.TEST_START_LOG)

        # Check Availability of Device 1
        self.verify_device_online(self.sdv_device1)
        self.verify_boot_completed(self.sdv_device1)

        # Check Availability of Device 2
        self.verify_device_online(self.sdv_device2)
        self.verify_boot_completed(self.sdv_device2)

        self.log_test_info(self.TEST_END_LOG)

    @parameterized.named_parameters(
        {
            'testcase_name': 'first_device',
            'device': 'device1',
            'instance_name': 'instance1',
            'instance_address': '3',
        },
        {
            'testcase_name': 'second_device',
            'device': 'device2',
            'instance_name': 'instance2',
            'instance_address': '4',
        },
    )
    def test_verify_boot_args(self, device, instance_name, instance_address):
        self.log_test_info(self.TEST_START_LOG)

        sdv_expected_boot_properties = {
            'ro.boot.sdv.instance_name': f"{instance_name}",
            'ro.boot.virt.address': f"{instance_address}",
        }

        # MultiVM test require to run in authentication mesh. See b/389915030.
        sdv_multivm_mesh_auth_bootconfig_args = {
            'ro.boot.sdv.boot_mode': 'locked',
            'ro.boot.sdv.init_open_dice.sample_file': f'dice_handover_{instance_name}',
            # preprovisioned_vvmtruststore can be etc or img. Do not specify
            # just ensure it has been provided.
            'ro.boot.sdv.preprovisioned_vvmtruststore': None,
            'ro.boot.sdv.ignore_avb_state': 'true',
            # vvmfactorytrust and hbk values could be arbitrary and depend on
            # the values provided when the VM is started. We simply verify the
            # boot arguments have been provided.
            'ro.boot.sdv.vvmfactorytrust': None,
            'ro.boot.sdv.keymint.rpc.hbk': None,
        }

        self.verify_properties(self.get_device(device), sdv_expected_boot_properties)
        self.verify_properties(self.get_device(device), sdv_multivm_mesh_auth_bootconfig_args)

        self.log_test_info(self.TEST_END_LOG)


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
