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

"""SDV Factory Provisioning Test

Tests factory provisioning.
"""
from mobly import asserts
import logging
import re

from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvFactoryProvisioningTest(sdv_base_test.SdvBaseTestClass):
    # The factory trust is the SHA256(uds_pubs) and constructed deterministically based on the
    # secrets and identifiers configured for the VMs in the test mesh.
    EXPECTED_FACTORY_TRUST = 'c779a73d0595a6814ba0414a419e99ad4026ad0feb603f2ad80ee6a9e4d1adb7'
    CLEAR_VVMTRUSTSTORE_DIR = 'rm -rf /vvmtruststore/*'
    PROVISIONING_TOOL_COMMAND = 'sdv_provisioning_tool'
    UDS_PUBS_FILE_PATH = "/vvmtruststore/uds_pubs"
    SHA256_COMMAND = 'sha256sum'
    # Expected SELinux label for the uds_pubs file.
    UDS_PUBS_EXPECTED_SELINUX_LABEL = 'u:object_r:uds_pubs_file:s0'
    # Regular expression to match the full file attributes, including SELinux context.
    # system_ext_sdv_sd user is used on Core, system_ext_sdv_sd_agent on IVI.
    UDS_PUBS_REGEX = r'-rw------- 1 system_ext_sdv_sd(_agent)? system %s.* %s' % (
        UDS_PUBS_EXPECTED_SELINUX_LABEL, UDS_PUBS_FILE_PATH)

    def setup_class(self):
        super().setup_class()
        self.device1 = self.get_device('device1').adb()
        self.device2 = self.get_device('device2').adb()
        self.device3 = self.get_device('device3').adb()

    def setup_test(self):
        # Setup to run tests
        logging.info('Starting SdvFactoryProvisioningTest Setup')
        self.device1.root_device()
        self.device2.root_device()
        self.device3.root_device()
        # In CI environments, a `vvmtruststore` image is pre-provisioned
        # by root, which includes an existing `uds_pubs` file.
        # We remove the files in vvmtruststore/ to allow Service Directory
        # to create a new uds_pubs.
        # For more context, refer to https://buganizer.corp.google.com/issues/399801101#comment40
        self.device1.execute_shell_command(self.CLEAR_VVMTRUSTSTORE_DIR)
        self.device2.execute_shell_command(self.CLEAR_VVMTRUSTSTORE_DIR)
        self.device3.execute_shell_command(self.CLEAR_VVMTRUSTSTORE_DIR)
        super().setup_test()

    def _check_vvmtruststore_empty(self, device):
        """
        Checks if the /vvmtruststore folder on the given device is empty.
        It uses 'ls -A' to list all entries (including hidden ones, excluding . and ..).
        """
        logging.info(f"Checking if /vvmtruststore is empty on {device.prop.get(SdvDeviceProperty.INSTANCE_NAME)}")
        ls_output = device.execute_shell_command("ls -A /vvmtruststore/")

        asserts.assert_equal(
            ls_output.strip(),
            "",
            f"/vvmtruststore on {device.prop.get(SdvDeviceProperty.INSTANCE_NAME)} is NOT empty before test. Content: '{ls_output}'"
        )
        logging.info(f"/vvmtruststore on {device.prop.get(SdvDeviceProperty.INSTANCE_NAME)} is empty. Proceeding.")

    def _check_file_attributes(self, device, file_path, expected_regex):
        """Checks the SELinux policy and permissions of a file."""
        logging.info(
            f"Checking SELinux policy for '{file_path}' on {device.prop.get(SdvDeviceProperty.INSTANCE_NAME)}"
        )
        ls_output = device.execute_shell_command(f'ls -lZ {file_path}')

        asserts.assert_true(
            re.fullmatch(expected_regex, ls_output.strip()),
            f"Unexpected SELinux policy or permissions for '{file_path}'. Found: '{ls_output.strip()}'",
        )
        logging.info(
            f"SELinux policy for '{file_path}' is correct on {device.prop.get(SdvDeviceProperty.INSTANCE_NAME)}."
        )

    def test_provisioning(self):
        logging.info('Run test provisioning')
        self._check_vvmtruststore_empty(self.device1)
        self._check_vvmtruststore_empty(self.device2)
        self._check_vvmtruststore_empty(self.device3)
        output1 = self.device1.execute_shell_command(self.PROVISIONING_TOOL_COMMAND)
        output2 = self.device2.execute_shell_command(self.PROVISIONING_TOOL_COMMAND)
        output3 = self.device3.execute_shell_command(self.PROVISIONING_TOOL_COMMAND)
        # Check that the hashes coincide for all VMs:
        asserts.assert_equal(output1, output2)
        asserts.assert_equal(output2, output3)
        # Compute the SHA256 of the created ups_pubs files:
        uds_pubs_hash1 = self.device1.execute_shell_command(f"{self.SHA256_COMMAND} {self.UDS_PUBS_FILE_PATH}").split()[0]
        uds_pubs_hash2 = self.device2.execute_shell_command(f"{self.SHA256_COMMAND} {self.UDS_PUBS_FILE_PATH}").split()[0]
        uds_pubs_hash3 = self.device3.execute_shell_command(f"{self.SHA256_COMMAND} {self.UDS_PUBS_FILE_PATH}").split()[0]
        # Compare the returned hashes from the provisioning tool to the SHA256 of the created files:
        asserts.assert_equal(output1, uds_pubs_hash1)
        asserts.assert_equal(output2, uds_pubs_hash2)
        asserts.assert_equal(output3, uds_pubs_hash3)
        # Check that uds_pubs was created and has the correct SELinux policy:
        self._check_file_attributes(self.device1, self.UDS_PUBS_FILE_PATH, self.UDS_PUBS_REGEX)
        self._check_file_attributes(self.device2, self.UDS_PUBS_FILE_PATH, self.UDS_PUBS_REGEX)
        self._check_file_attributes(self.device3, self.UDS_PUBS_FILE_PATH, self.UDS_PUBS_REGEX)

        return False

    # Tests that SDV mesh provisioning produces the expected (valid) uds_pubs
    def test_provisioning_produces_expected_uds_pubs(self):
        logging.info('Run test provisioning produces expected uds_pubs')
        self._check_vvmtruststore_empty(self.device1)
        self._check_vvmtruststore_empty(self.device2)
        self._check_vvmtruststore_empty(self.device3)

        # Other tests verify that the mesh provisioning flow constructs the same uds_pubs files and
        # the same factory trust values on all VMs. Thus, in this test, it's sufficient to verify
        # this value on a single device.
        output_factory_trust = self.device1.execute_shell_command(self.PROVISIONING_TOOL_COMMAND)

        # The factory trust is the SHA256(uds_pubs) and both of them are constructed
        # deterministically based on the secrets and identifiers configured for the VMs in the test
        # mesh. Thus, by asserting that the output factory trust matches with an expected factory
        # trust belonging to a known valid uds_pubs, we indirectly test that:
        # 1. the output of the mesh provisioning flow is deterministic
        # 2. the output of the mesh provisioning is valid and functional.
        asserts.assert_equal(output_factory_trust, self.EXPECTED_FACTORY_TRUST)

        return False

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
