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

"""Verify the sdv_uds_certs_provisioner sample

This test verifies the sdv_uds_certs_provisioner sample with its sample SEPolicy setup constructs
the uds_certs file as expected.

"""

from mobly import asserts
import re
import time

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling
from mobly.controllers.android_device_lib.adb import AdbError

class SdvE2EUdsCertsProvisionerTest(sdv_base_test.SdvBaseTestClass):

    CLEAR_VVMTRUSTSTORE_DIR = 'rm -rf /vvmtruststore/*'
    UDS_CERTS_FILE = "/vvmtruststore/uds_certs"

    def setup_class(self):
        super().setup_class()
        self.sdv_device = self.get_device("device1").adb()

    def setup_test(self):
        self.sdv_device.root_device()
        # In CI environments, a `vvmtruststore` image is pre-provisioned
        # by root, which includes an existing `uds_pubs` file.
        # We remove the files in vvmtruststore/ to allow Service Directory
        # to create a new uds_pubs.
        # For more context, refer to https://buganizer.corp.google.com/issues/399801101#comment40
        self.sdv_device.execute_shell_command(self.CLEAR_VVMTRUSTSTORE_DIR)

    def uds_certs_file_attributes(self):
        return self.sdv_device.execute_shell_command(f"ls -lZ {self.UDS_CERTS_FILE}")

    def assert_uds_certs_file(self):
        uds_certs_attributes = self.uds_certs_file_attributes()
        # Verify that modes and the SELinux label are as expected
        asserts.assert_true(
            re.fullmatch(
                "-rw------- 1 system system u:object_r:uds_certs_file:s0.*/vvmtruststore/uds_certs",
                uds_certs_attributes),
            f"Unexpected uds_certs attributes: {uds_certs_attributes}",
        );

    def uds_certs_file_exists(self):
        exists = "exists"
        # Must cover both branches in the command, otherwise an exception is raised
        output = self.sdv_device.execute_shell_command(
            f"[ -f {self.UDS_CERTS_FILE} ] && echo {exists} || echo not {exists}")
        return output == exists

    def wait_for_uds_certs_file(self):
        # wait for 500 ms maximum, creating the file should be instantaneous
        polling.wait_for_true(
            func=self.uds_certs_file_exists,
            timeout=0.5,
        )

    def assert_no_uds_certs_file(self):
        asserts.assert_false(
            self.uds_certs_file_exists(),
            f"{self.UDS_CERTS_FILE} exists"
        )

    def test_uds_certs_provisioned(self):
        self.assert_no_uds_certs_file()

        # trigger the sdv_uds_certs_provisioner_sample service through init
        self.sdv_device.execute_shell_command("start sdv_uds_certs_provisioner_sample")
        # wait a bit for the uds_certs_file to be created
        self.wait_for_uds_certs_file()
        self.assert_uds_certs_file()

if __name__ == "__main__":
    sdv_test_runner.run()
