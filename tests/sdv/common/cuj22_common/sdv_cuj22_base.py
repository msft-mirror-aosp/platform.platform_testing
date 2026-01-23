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

from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods
from vpm.sdv_vpm import SdvVpm

# This base class should be started with two VMs.
# Any test that uses this class must have two VMs and must have the correct orchestration configuration.
# Refer to sample_tests/cujs/cuj-core-22/README.md
class SdvCuj22Base():
    ERROR_MESSAGE_AUTHZ_PUBLISHER_GREP = \
        '\n[FAILURE]: Service Bundle expected to be authorized to publish messages'
    AUTHZ_RPC_SERVICE_UNIT_LOG = 'instance.*:com.sdv.google.sample.foo.ServiceBundleFoo/instance#com-sdv-google-sample-foo-foo-rpc allows access to instance.*:com.sdv.google.sample.bar.ServiceBundleBar/instance'
    ERROR_MESSAGE_AUTHZ_RPC_CLIENT_GREP = \
        '\n[FAILURE]: Service Bundle expected to be authorized to call RPC server'

    DEFAULT_REQUIRES_BOOT_LOGS = False
    def setup_cuj22_devices(self, boot_logs=DEFAULT_REQUIRES_BOOT_LOGS):
        """Setup Service and Client devices
        Args:
        boot_logs: If boot logs are required in the test. Reboots the device. If this flag is not enabled, it cannot verify by default the service started as expected.
        """
        self.setup_server(boot_logs)
        self.setup_client(boot_logs)

    def setup_server(self, boot_logs=DEFAULT_REQUIRES_BOOT_LOGS):
        self.adb_device_server = self.get_device('device1').adb()
        if boot_logs:
           # Accessing boot logs require rebooting the device (b/381241303)
            self.adb_device_server.reboot_device_and_verify_logcat()
            self.verify_server_bundle_started()
        self.server_vpm = SdvVpm(self.adb_device_server)

    def setup_client(self, boot_logs=DEFAULT_REQUIRES_BOOT_LOGS):
        self.adb_device_client = self.get_device('device2').adb()
        if boot_logs:
           # Accessing boot logs require rebooting the device (b/381241303)
            self.adb_device_client.reboot_device_and_verify_logcat()
            self.verify_client_bundle_started()
        self.client_vpm = SdvVpm(self.adb_device_client)


    def verify_server_bundle_started(self):
        self.verify_service_bundle_started(self.adb_device_server, 'instance1', 'com.sdv.google.sample.foo', 'ServiceBundleFoo')

    def verify_client_bundle_started(self):
        self.verify_service_bundle_started(self.adb_device_client, 'instance2', 'com.sdv.google.sample.bar', 'ServiceBundleBar')


    def verify_service_bundle_started(self, device, vm_instance, package_name, bundle_name):
        """
        Verify logs for orchestration configurations.
        Might only be available after adb reboot because debug logs might not be setup yet.

        Args:
            device_name: Either self.adb_device_server or self.adb_device_client
            vm_instance: Typically either 'instance1' or 'instance2'
            package_name: Typically either 'com.sdv.google.sample.foo' or 'com.sdv.google.sample.bar'
            bundle_name: Typically either 'ServiceBundleFoo' or 'ServiceBundleBar'
        """
        starting_text = 'Starting {vm_instance}:{package}.{bundle_name}/instance'
        error_message_creation_grep = \
        '\n[FAILURE]: Service Bundle expected to be started.'

        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = device,
            grep_text = starting_text.format(
                vm_instance = vm_instance,
                package = package_name,
                bundle_name = bundle_name,
            ),
            assert_msg = error_message_creation_grep,
        )

    def verify_logs_pub_sub_foo_message(self):
        """
        Verify logs for Pub/Sub
        """

        sent_message = 'Sent.*FooMessage.*42'
        received_message = 'Received.*FooMessage.*42'
        error_message_foo_message_grep = \
            '\n[FAILURE]: Service Bundle expected to send/receive Foo Message.'

        # FooMessage sent by ServiceBundleFoo
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_device_server,
            grep_text = sent_message,
            assert_msg = error_message_foo_message_grep,
        )
        # FooMessage received by ServiceBundleBar
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_device_client,
            grep_text = received_message,
            assert_msg = error_message_foo_message_grep,
        )


    def verify_logs_rpc_foo_in_client(self):
        """
        Verify logs for RPC
        """
        rpc_request_response = 'Request.*GetFooRequest.*2024.*Response.*GetFooResponse.*2024'
        error_message_foo_rpc_grep = \
            '\n[FAILURE]: Service Bundle expected to call Foo RPC.'


        # FooRPC::foo(GetFooRequest) and GetFooResponse are received.
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_device_client,
            grep_text = rpc_request_response,
            assert_msg = error_message_foo_rpc_grep,
        )

    def verify_logs_authz_message_publisher(self):
        """
        Verify logs that Pub is authorized
        """
        authz_pubsub_unit_type_log = 'com.sdv.google.sample.foo.FooMessage allows access to instance.*:com.sdv.google.sample.foo.ServiceBundleFoo/instance'

        # Publisher is authorized to publish message.
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_device_server,
            grep_text = authz_pubsub_unit_type_log,
            assert_msg = self.ERROR_MESSAGE_AUTHZ_PUBLISHER_GREP,
        )


    def verify_logs_authz_message_subscriber(self):
        """
        Verify logs that Sub is authorized
        """
        authz_pubsub_service_unit_log = 'instance.*:com.sdv.google.sample.foo.ServiceBundleFoo/instance#com-sdv-google-sample-foo-foo-message-core allows access to instance.*:com.sdv.google.sample.bar.ServiceBundleBar/instance'

        # Subscriber is authorized to discover and to subscribe
        # to the publisher.
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_device_client,
            grep_text = authz_pubsub_service_unit_log,
            assert_msg = self.ERROR_MESSAGE_AUTHZ_PUBLISHER_GREP,
        )

    def verify_logs_authz_rpc_server(self):
        """
        Verify logs that RPC server is authorized
        """
        authz_rpc_unit_type_log = 'com.sdv.google.sample.foo.FooRPC allows access to instance.*:com.sdv.google.sample.foo.ServiceBundleFoo/instance'
        error_message_authz_rpc_server_grep = \
            '\n[FAILURE]: Service Bundle expected to be authorized to implement RPC interface'

        # RPC server is authorized to implement RPC interface.
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_device_server,
            grep_text = authz_rpc_unit_type_log,
            assert_msg = error_message_authz_rpc_server_grep,
        )
        # RPC client is authorized to connect to the RPC server.
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_device_server,
            grep_text = self.AUTHZ_RPC_SERVICE_UNIT_LOG,
            assert_msg = self.ERROR_MESSAGE_AUTHZ_RPC_CLIENT_GREP,
        )

    def verify_logs_authz_rpc_client(self):
        """
        Verify logs that RPC client is authorized
        """
        # RPC client is authorized to discover the RPC server.
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_device_client,
            grep_text = self.AUTHZ_RPC_SERVICE_UNIT_LOG,
            assert_msg = self.ERROR_MESSAGE_AUTHZ_RPC_CLIENT_GREP,
        )

    def verify_increasing_occurrences_of_pub_sub_foo_message(self):
        self._wait_for_new_log_entry(
            self.adb_device_server,
            'Sent.*FooMessage.*42'
        )

        self._wait_for_new_log_entry(
            self.adb_device_client,
            'Received.*FooMessage.*42'
        )

    def verify_increasing_occurrences_of_rpc_foo_in_client(self):
        self._wait_for_new_log_entry(
            self.adb_device_client,
            'Request.*GetFooRequest.*2024.*Response.*GetFooResponse.*2024'
        )

    def _wait_for_new_log_entry(self, device_adb, grep_text, n_occurrences=3, timeout=30):
        """Waits for a new log entry matching grep_text to appear 3 times.

        It first gets the current timestamp from the device, then polls logcat
        until 3 matching log entries with a later timestamp are found.

        Args:
            device_adb: The SdvAdb object for the device.
            grep_text: The text/regex to search for in logcat.
            n_occurrences: The number of new log entries to wait for.
            timeout: The maximum time to wait in seconds.
        """
        initial_timestamp = device_adb.get_current_device_timestamp()

        def _check_for_new_log(device_adb, grep_text, timestamp, n_occurrences):
            logcat_processor = device_adb.advance_logcat()
            log_message = logcat_processor.nth_message(
                n_occurrences, grep_text, timestamp=timestamp, regex=True
            )
            return log_message is not None

        WaitingMethods.wait_for_true(
            _check_for_new_log, device_adb, grep_text, initial_timestamp, n_occurrences, timeout=timeout
        )

    def verify_comm_stack_authz_logs(self):
        """Verifies all authorization logs for the communication stack.

        This method checks for specific log messages to confirm that publisher,
        subscriber, RPC server, and RPC client are all properly authorized.

        Note: These authorization logs are debug logs and may only be visible
        after an `adb reboot`.
        """
        self.verify_logs_authz_message_publisher()
        self.verify_logs_authz_message_subscriber()
        self.verify_logs_authz_rpc_server()
        self.verify_logs_authz_rpc_client()

    def check_all_comm_stack_logs(self):
        self.verify_logs_pub_sub_foo_message()
        self.verify_logs_rpc_foo_in_client()
        self.verify_increasing_occurrences_of_pub_sub_foo_message()
        self.verify_increasing_occurrences_of_rpc_foo_in_client()
