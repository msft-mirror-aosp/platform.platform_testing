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
SdvVsidlProviderAgentTest

Verifies that an SDV Core VM can create clients for a VSIDL provider server
running on the VSIDL provider agent of an IVI VM.
"""

import time
from mobly import asserts
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling


class SdvVsidlProviderAgentTest(sdv_base_test.SdvBaseTestClass):
    """
    Test class for VSIDL provider agent communication.
    """

    VSIDL_AVAILABILITY_QUERY = (
        'sdv_vsidl_provider_query --server-vm-name {server_vm} --query-variant'
        ' subscribe-availability-change'
    )
    EXPECTED_RESULT_AVAILABILITY_QUERY = (
        'Querying VSIDL Provider availability on VM {server_vm} from VM'
        ' {client_vm}. Received response: Server is reachable'
    )
    VSIDL_QUERY_TESTER_BINDER_NAME = 'com.google.sdv.ISdvAgent/vsidl_provider_test'

    def setup_class(self):
        """
        Initializes device objects.
        """
        super().setup_class()
        # device1 is the IVI VM, device2 is the SDV Core VM.
        self.ivi_vm_device = self.get_device('device1').adb()
        self.core_vm_device = self.get_device('device2').adb()
        self.ivi_vm_name = 'instance1'
        self.sdv_vm_name = 'instance2'

    def check_dumpsys(
        self, device, expected_text, timeout=30, poll_interval=0.1
    ):
        """
        Polls the dumpsys output for a specific text until found or timeout.

        Args:
            device: device on which to check the dumpsys output.
            expected_text: The text to search for.
            timeout: The maximum time (in seconds) to wait.
            poll_interval: The time (in seconds) between polls.
        """

        deadline = time.perf_counter() + timeout
        while time.perf_counter() < deadline:
            report = device.dumpsys(self.VSIDL_QUERY_TESTER_BINDER_NAME)
            if expected_text in report:
                return
            time.sleep(poll_interval)

        asserts.fail(
            f'Message not found within timeout [{expected_text}] in dumpsys'
            f' report: {report}'
        )

    def test_vsidl_provider_agent(self):
        """
        Verifies that the SDV VM can successfully create clients for a VSIDL
        provider server that is running on the VSIDL provider agent of the
        IVI VM.
        """
        self.core_vm_device.execute_shell_command_in_subprocess(
            'vsidl_provider_query_ivi_agent',
            self.VSIDL_AVAILABILITY_QUERY.format(server_vm=self.ivi_vm_name)
        )

        self.check_dumpsys(
            self.core_vm_device,
            self.EXPECTED_RESULT_AVAILABILITY_QUERY.format(
                server_vm=self.ivi_vm_name, client_vm=self.sdv_vm_name
            ),
        )



if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()