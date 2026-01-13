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

"""E2E test for vvmtruststore partition."""

from mobly import asserts
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner


class SdvVVMTrustStorePartitionTest(sdv_base_test.SdvBaseTestClass):
  LIST_VVMTRUSTSTORE = "ls -al /dev/block/by-name/vvmtruststore"
  MOUNT_VVMTRUSTSTORE_VALIDATION = "mount | grep vvmtruststore"
  PARAM_VALIDATION_GREP = "/dev/block/by-name/vvmtruststore -> /dev/block/vda15"
  MOUNT_VALIDATION_GREP = "/dev/block/vda15 on /vvmtruststore type ext4"

  def setup_class(self):
    super().setup_class()
    self.sdv_device1 = self.get_device("device1").adb()
    self.sdv_device2 = self.get_device("device2").adb()

  # Verifies the parameter given a device
  def _verify_device_parameter(self, device):
    device.log().info("VVMTruststore Parameter Verification")
    command_output = device.execute_shell_command(
        self.LIST_VVMTRUSTSTORE
    )
    asserts.assert_in(
        self.PARAM_VALIDATION_GREP,
        command_output,
    )

  # Verifies the VVMTruststore Partition given a device
  def _verify_device_partition(self, device):
    # Mount Verification
    # ----------------------------------------------------------------------------
    device.log().info("VVM Truststore Mount Verification")
    # Print the mount log to check if the mount is successful
    command_output = device.execute_shell_command(
        self.MOUNT_VVMTRUSTSTORE_VALIDATION
    )
    asserts.assert_in(
        self.MOUNT_VALIDATION_GREP,
        command_output,
    )

  def test_parameter_verification(self):
    # ----------------------------------------------------------------------------
    # Parameter Verification
    # ----------------------------------------------------------------------------
    # Verify parameters Device 1
    self._verify_device_parameter(self.sdv_device1)
    # Verify parameters Device 2
    self._verify_device_parameter(self.sdv_device2)

  def test_mounted_partition(self):
    # ----------------------------------------------------------------------------
    # Mount Verification
    # ----------------------------------------------------------------------------
    # Verify partition in Device 1
    self._verify_device_partition(self.sdv_device1)
    # Verify partition in Device 2
    self._verify_device_partition(self.sdv_device2)

# ----------------------------------------------------------------------------
# Main
# ----------------------------------------------------------------------------
if __name__ == "__main__":
  # Start Test Execution Using SDV Test Framework ( STF )
  sdv_test_runner.run()
