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

"""SDV OEM partition

Tests mounting of additional OEM partition.
"""

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner


class SdvSampleOemPartitionTest(sdv_base_test.SdvBaseTestClass):

    OEM_PARTITION_MOUNT_POINT = "/oem"
    EXPECTED_OEM_PARTITION_DEVICE_REGEX = r"\/dev\/block\/vda\d* on \/oem type f2fs \(.*lookup_mode=perf\)"
    EXPECTED_OEM_PARTITION_CONTENT_1 = "/oem/hello_oem.txt"
    EXPECTED_OEM_PARTITION_CONTENT_2 = "/oem/oem.prop"

    OEM_METADATA_PARTITION_MOUNT_POINT = "/oem_metadata"
    EXPECTED_OEM_METADATA_PARTITION_CONTENT = "total 0"

    def setup_class(self):
        super().setup_class()
        self.device1 = self.get_device('device1').adb()

    def setup_test(self):
        self.device1.root_device()
        super().setup_test()

    def test_oem_partition_mount_point(self):
        mount_output = self.device1.execute_shell_command(f"mount | grep {self.OEM_PARTITION_MOUNT_POINT}")
        self.get_test_validator().assert_regex(mount_output, self.EXPECTED_OEM_PARTITION_DEVICE_REGEX, "OEM partition mount regex does not match.")

    def test_oem_partition_content(self):
        ls_output = self.device1.execute_shell_command(f"ls -l {self.OEM_PARTITION_MOUNT_POINT}/*")
        self.get_test_validator().assert_in(self.EXPECTED_OEM_PARTITION_CONTENT_1, ls_output, f"File ${self.EXPECTED_OEM_PARTITION_CONTENT_1} not found in OEM partition")
        self.get_test_validator().assert_in(self.EXPECTED_OEM_PARTITION_CONTENT_2, ls_output, f"File ${self.EXPECTED_OEM_PARTITION_CONTENT_2} not found in OEM partition")

    def test_oem_metadata_partition_content(self):
        ls_output = self.device1.execute_shell_command(f"ls -l {self.OEM_METADATA_PARTITION_MOUNT_POINT}")
        self.get_test_validator().assert_in(self.EXPECTED_OEM_METADATA_PARTITION_CONTENT, ls_output, f"OEM metadata partition was expected to be empty, but found: {ls_output}")


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
