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

"""SDV VSIDL VM Discovery Attributes Test"""
from mobly import asserts
import logging
import random
import string
import time

from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner

class SdvVmDiscoveryAttributeTest(sdv_base_test.SdvBaseTestClass):
    # Value published / provided as RPC response by VM1
    VM1_EXPECTED_VALUE = '5'
    # Value published / provided as RPC response by VM2
    VM2_EXPECTED_VALUE = '7'

    def setup_class(self):
        logging.info('Starting SDV VSIDL VM Discovery Attributes Class Setup')
        super().setup_class()
        self.sdv_device_1 = self.get_device('device1')
        self.sdv_device_2 = self.get_device('device2')

        self.clear_all_devices()

        # Setup 1st VM with service bundle 'foo' and published message / RPC response value 5
        self.setup_foo(self.sdv_device_1, SdvVmDiscoveryAttributeTest.VM1_EXPECTED_VALUE)
        # Setup 2nd VM with service bundle 'foo' and published message / RPC response value 7
        self.setup_foo_bar(self.sdv_device_2, SdvVmDiscoveryAttributeTest.VM2_EXPECTED_VALUE)

        logging.info('Completed SDV VSIDL VM Discovery Attributes Class Setup')

    def setup_test(self):
        logging.info('Starting SDV VSIDL VM Discovery Attributes Setup')
        # Do not call super().setup_test(), as tests only grep different aspects of the logs of a single execution
        logging.info('Completed SDV VSIDL VM Discovery Attributes Setup')

    def set_property(self, device, property_name, property_value):
        device.adb().execute_shell_command(['setprop', property_name, property_value])

    def setup_foo_props(self, device, value):
        # Set publishing interval to 1 second
        self.set_property(device, 'persist.com.sdv.google.sample.foo.pub_interval_ms', '50')
        # Set publishing amount to 10
        self.set_property(device, 'persist.com.sdv.google.sample.foo.pub_amount', '3')
        # Set published value
        self.set_property(device, 'persist.com.sdv.google.sample.foo.pub_message_value', value)
        # Set RPC response value
        self.set_property(device, 'persist.com.sdv.google.sample.foo.rpc_response', value)
        # Set publishing load size to 200 KBytes
        self.set_property(device, 'persist.com.sdv.google.sample.foo.load_size', '200')

    def setup_foo(self, device, value):
        # Set orchestrator configuration
        self.set_property(device, 'persist.sdv.orchestrator_config_path', '"etc/orch/vm_foo_orch_config.textproto"')
        # Set application-specific system properties
        self.setup_foo_props(device, value)
        # Reboot device
        device.adb().reboot_device()
        # Wait for device to get back online
        device.adb().wait_for_device_online()

    def setup_foo_bar(self, device, value):
        # Set system property
        self.set_property(device, 'persist.sdv.orchestrator_config_path', '"etc/orch/vm_foo_bar_orch_config.textproto"')
        # Set Foo-specific system properties
        self.setup_foo_props(device, value)
        # Set requests interval to 1 second
        self.set_property(device, 'persist.com.sdv.google.sample.bar.rpc_interval_ms', '50')
        # Set request load size to 200 KBytes
        self.set_property(device, 'persist.com.sdv.google.sample.bar.load_size', '200')
        # Set the VM instance to connect to
        self.set_property(device, 'persist.com.sdv.google.sample.bar.communication_partner_vm_name', 'instance2')
        # Reboot device
        device.adb().reboot_device()
        # Wait for device to get back online
        device.adb().wait_for_device_online()

    def wait_for_logcat(
        self, device, grep_text, logcat_args, timeout=30, poll_interval=0.1
    ):
        """Polls the logcat output for a specific text until found or timeout.

        Args:
            grep_text: The text to search for in the logcat output.
            logcat_args: Arguments to pass to logcat
            timeout: The maximum time (in seconds) to wait.
            poll_interval: The time (in seconds) between polls.
        Returns:
            True if grep matched at least one logcat output
            False if grep matched no logcat output within the timeout
        """
        deadline = time.perf_counter() + timeout
        while time.perf_counter() < deadline:
            logcat_result = device.adb().grep_from_logcat(grep_text, logcat_args=logcat_args)
            if logcat_result != "":
                return True
            time.sleep(poll_interval)
        return False

    def publishes_message_logs(self, device, value):
        return self.wait_for_logcat(device, grep_text='Sent.*FooMessage.*' + value, logcat_args='*:F com_sdv_google_sample_foo_ServiceBundleFoo_instance:*')

    def receives_message_logs(self, device, value):
        return self.wait_for_logcat(device, grep_text='Received.*FooMessage.*' + value, logcat_args='*:F com_sdv_google_sample_bar_ServiceBundleBar_instance:*')

    def receives_rpc_response_logs(self, device, value):
        return self.wait_for_logcat(device, grep_text='GetFooRequest.*GetFooResponse.*response=' + value, logcat_args='*:F com_sdv_google_sample_bar_ServiceBundleBar_instance:*')

    def test_both_vms_send_messages(self):
        logging.info(
            f'{self.get_suite_name()} :: Start Test {self.current_test_info.name}'
        )

        # THEN: 1st VM sends pubsub messages
        asserts.assert_true(
            self.publishes_message_logs(self.sdv_device_1, SdvVmDiscoveryAttributeTest.VM1_EXPECTED_VALUE),
            "VM1 does not publish messages"
        )

        # THEN: 2nd VM sends pubsub messages
        asserts.assert_true(
            self.publishes_message_logs(self.sdv_device_2, SdvVmDiscoveryAttributeTest.VM2_EXPECTED_VALUE),
            "VM2 does not publish messages"
        )

        logging.info(
            f'{self.get_suite_name()} :: End Test {self.current_test_info.name}'
        )

    def test_vm_connects_to_vm_with_specified_vm_name(self):
        logging.info(
            f'{self.get_suite_name()} :: Start Test {self.current_test_info.name}'
        )

        # THEN: 2nd VM receives pubsub messages from VM 2
        asserts.assert_true(
            self.receives_message_logs(self.sdv_device_2, SdvVmDiscoveryAttributeTest.VM2_EXPECTED_VALUE),
            "VM2 does not receive PubSub messages from VM2"
        )

        # THEN: 2nd VM receives RPC results from VM 2
        asserts.assert_true(
            self.receives_rpc_response_logs(self.sdv_device_2, SdvVmDiscoveryAttributeTest.VM2_EXPECTED_VALUE),
            "VM2 has no RPC connection to VM2"
        )

        logging.info(
            f'{self.get_suite_name()} :: End Test {self.current_test_info.name}'
        )

    def test_vm_doesnt_connect_to_vm_not_using_specified_vm_name(self):
        logging.info(
            f'{self.get_suite_name()} :: Start Test {self.current_test_info.name}'
        )

        # THEN: 2nd VM does not receive pubsub messages from VM 1
        asserts.assert_false(
            self.receives_message_logs(self.sdv_device_2, SdvVmDiscoveryAttributeTest.VM1_EXPECTED_VALUE),
            "VM2 has unintended PubSub connection to VM1"
        )

        # THEN: 2nd VM receives no RPC results from VM 1
        asserts.assert_false(
            self.receives_rpc_response_logs(self.sdv_device_2, SdvVmDiscoveryAttributeTest.VM1_EXPECTED_VALUE),
            "VM2 has unintended RPC connection to VM1"
        )

        logging.info(
            f'{self.get_suite_name()} :: End Test {self.current_test_info.name}'
        )

    def teardown_test(self):
        # Clear all devices
        super().teardown_test()

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
