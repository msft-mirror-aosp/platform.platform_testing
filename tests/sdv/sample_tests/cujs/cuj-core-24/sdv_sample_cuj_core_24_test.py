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

"""SDV sample 'CUJ-Core-24' test."""

import logging
import time

from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.test_execution import sdv_test_runner
from cuj22_common.sdv_cuj22_base import SdvCuj22Base


class SdvSampleCujCore24Test(sdv_base_test.SdvBaseTestClass, SdvCuj22Base):
    """Tests CUJ Core 24 (Suspend/Resume)."""

    def setup_class(self):
        """Sets up the test class by setting orchestrator config and starting VMs.
        """
        super().setup_class()
        self.setup_cuj22_devices()

    def setup_test(self):
        # Avoid parent `clear_all_devices` to store the start up logs from VMs.
        logging.info('Custom CUJ24 test setup')


    def test_cross_vm_communication_after_resume(self):
        """Tests cross-VM communication after resuming both VMs."""
        # Suspend the first VM
        self.server_vpm.suspend_and_resume_vm_from_ram()
        self.verify_server_bundle_started()
        self.check_all_comm_stack_logs()

        # Suspend and resume the second VM
        self.client_vpm.suspend_and_resume_vm_from_ram()
        self.verify_client_bundle_started()
        self.check_all_comm_stack_logs()


if __name__ == "__main__":
    sdv_test_runner.run()
