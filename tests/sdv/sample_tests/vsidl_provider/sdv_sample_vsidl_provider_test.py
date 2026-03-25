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

"""SDV VSIDL Provider Test

Tests VSIDL Provider Communication between Two SDV VMs for different APEX configurations.
"""
from mobly import asserts
import logging
import time
from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling
from sdv_test_fw.device.sdv_property import SdvDeviceProperty


class SdvSampleVsidlProviderTest(
    sdv_base_test.SdvBaseTestClass, parameterized.TestCase
):
    APEX_WITH_PER_BUNDLE_CONFIG_FILE = 'com.android.sdv.sample.vsidl_provider.apex_with_per_bundle_config_file'
    APEX_WITH_SHARED_CONFIG_FILE = 'com.android.sdv.sample.vsidl_provider.apex_with_shared_config_file'

    VSIDL_DIAGNOSTICS_QUERY = (
        'sdv_vsidl_provider_query --server-vm-name {to_vm}'
        ' --service-bundle-package-name {package_name}'
        ' --service-bundle-name DiagService'
        ' --query-variant diagnostics-declaration --client-fqin'
        ' {client_fqin}{timestamp}'
    )
    VSIDL_PUBLICATION_DESCRIPTOR_QUERY = (
        'sdv_vsidl_provider_query --server-vm-name {to_vm}'
        ' --service-bundle-package-name {package_name}'
        ' --service-bundle-name Manager'
        ' --query-variant publication-descriptor  --unit-type-package-name'
        ' com.sdv.google.sample.vsidl_provider --unit-type-name'
        ' TirePressure --client-fqin {client_fqin}{timestamp}'
    )
    VSIDL_RPC_METHOD_DESCRIPTOR_QUERY = (
        'sdv_vsidl_provider_query --server-vm-name {to_vm}'
        ' --service-bundle-package-name {package_name}'
        ' --service-bundle-name HelloService'
        ' --query-variant rpc-method-descriptor --unit-type-package-name'
        ' com.sdv.google.sample.vsidl_provider --unit-type-name'
        ' HelloInterface --method-name SayHello --client-fqin {client_fqin}{timestamp}'
    )
    VSIDL_MESSAGE_DESCRIPTOR_QUERY = (
        'sdv_vsidl_provider_query --server-vm-name {to_vm}'
        ' --service-bundle-package-name {package_name}'
        ' --service-bundle-name Manager'
        ' --query-variant message-descriptor --message-name'
        ' com.sdv.google.sample.vsidl_provider.TirePressure --client-fqin'
        ' {client_fqin}{timestamp}'
    )

    CLIENT_INSTANCE = 'instance1'
    SERVER_INSTANCE = 'instance2'

    EXPECTED_RESULT_DIAGNOSTICS_QUERY = (
        'Querying VSIDL Provider Diagnostics from VM {from_vm} to VM {to_vm}.'
        ' Received response: DiagnosticsDeclaration {{ data_item:'
        ' [DataItem {{ id: "data_item", message_name:'
        ' "com.sdv.google.sample.vsidl_provider.DataItem", is_writable: false,'
        ' special_fields: SpecialFields {{ unknown_fields: UnknownFields {{'
        ' fields: None }}, cached_size: CachedSize {{ size: 0 }} }} }}],'
        ' io_control: [], routine: [Routine {{ id: "routine", start:'
        ' MessageField(Some(Method {{ request:'
        ' "com.sdv.google.sample.vsidl_provider.DataItem", response:'
        ' "google.protobuf.Timestamp", special_fields: SpecialFields {{'
        ' unknown_fields: UnknownFields {{ fields: None }}, cached_size:'
        ' CachedSize {{ size: 0 }} }} }})), stop: MessageField(Some(Method {{'
        ' request: "google.protobuf.Empty", response:'
        ' "com.sdv.google.sample.vsidl_provider.TirePressure", special_fields:'
        ' SpecialFields {{ unknown_fields: UnknownFields {{ fields: None }},'
        ' cached_size: CachedSize {{ size: 0 }} }} }})), result:'
        ' MessageField(Some(Method {{ request: "google.protobuf.Empty",'
        ' response: "google.protobuf.Empty", special_fields: SpecialFields {{'
        ' unknown_fields: UnknownFields {{ fields: None }}, cached_size:'
        ' CachedSize {{ size: 0 }} }} }})), special_fields: SpecialFields {{'
        ' unknown_fields: UnknownFields {{ fields: None }}, cached_size:'
        ' CachedSize {{ size: 0 }} }} }}], event: [], special_fields:'
        ' SpecialFields {{ unknown_fields: UnknownFields {{ fields: None }},'
        ' cached_size: CachedSize {{ size: 0 }} }} }}'
    )
    EXPECTED_RESULT_PUBLICATION_DESCRIPTOR_QUERY = (
        'Querying VSIDL Provider Publication Descriptor from VM {from_vm} to VM'
        ' {to_vm}. Received response: PublicationDescriptor {{'
        ' message_descriptor: MessageDescriptor {{ .. }}, size_metadata:'
        ' SizeMetadata {{ message_size: 14, message_count: 8,'
        ' field_size_constraints: {{}} }} }}'
    )
    EXPECTED_RESULT_RPC_METHOD_DESCRIPTOR_QUERY = (
        'Querying VSIDL Provider RPC Method Descriptor from VM {from_vm} to VM'
        ' {to_vm}. Received response: RpcMethodDescriptor {{'
        ' request_message: Some(MessageDescriptor {{ .. }}), response_message:'
        ' Some(MessageDescriptor {{ .. }}) }}'
    )
    EXPECTED_RESULT_MESSAGE_DESCRIPTOR_QUERY = (
        'Querying VSIDL Provider Message Descriptor from VM {from_vm} to VM'
        ' {to_vm}. Received response: MessageDescriptor {{ .. }}'
    )
    EXPECTED_RESULT_CACHED_CLIENT = 'client_cache list: {to_vm}'

    CACHE_CLEARED_MESSAGE = 'On vm:instance1, removed Client for vm:instance2'
    VSIDL_PROVIDER_AGENT_SUCCESSFUL_LOAD_MESSAGE = 'sdv_vsidl_provider_agent has started successfully'
    VSIDL_PROVIDER_BINDER_NAME = "com.google.sdv.ISdvAgent/vsidl_provider"
    VSIDL_QUERY_TESTER_BINDER_NAME = "com.google.sdv.ISdvAgent/vsidl_provider_test"

    def setup_class(self):
        super().setup_class()
        # Get the device uisng label
        self.sdv_device_client = self.get_device('device1')
        self.sdv_device_server = self.get_device('device2')

        # Save the current values of sdv.authz.enable
        self.sdv_authz_enable_value_server = (
            self.sdv_device_server.adb().prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        )
        self.sdv_authz_enable_value_client = (
            self.sdv_device_client.adb().prop.get(SdvDeviceProperty.AUTHZ_ENABLE)
        )

        # Enforce SDV Comm Stack authorization
        self.sdv_device_server.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'permissions_only')
        self.sdv_device_client.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, 'permissions_only')

    def teardown_class(self):
        # Reset SDV Comm Stack authorization
        self.sdv_device_server.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.sdv_authz_enable_value_server)
        self.sdv_device_client.adb().prop.set(SdvDeviceProperty.AUTHZ_ENABLE, self.sdv_authz_enable_value_client)
        super().teardown_class()

    def check_vsidl_provider_agent_started(self, device):
        polling.wait_for_true(
            self.is_vsidl_provider_agent_started,
            device,
        )

    def is_vsidl_provider_agent_started(self, device):
        report = device.adb().dumpsys(self.VSIDL_PROVIDER_BINDER_NAME)
        if self.VSIDL_PROVIDER_AGENT_SUCCESSFUL_LOAD_MESSAGE in report:
            return True
        return False

    def run_query(self, query, result, client_fqin, package_name):
        self.check_vsidl_provider_agent_started(self.sdv_device_client)
        self.check_vsidl_provider_agent_started(self.sdv_device_server)

        self.sdv_device_client.adb().log().info(
            'Executing VSIDL Diagnostics Query on Client'
        )

        # Execute binary that queries for VSIDL Diagnostics data (once) for xVM
        self.sdv_device_client.adb().execute_shell_command_in_subprocess(
            'vsidl_provider_query_loop_xVM',
            query.format(
                to_vm=self.SERVER_INSTANCE,
                client_fqin=client_fqin,
                timestamp=str(int(time.time())),
                package_name=package_name,
            ),
        )

        # Verify Connection Success
        self.sdv_device_client.adb().log().info(
            'Verify connection success message from dumpsys on client'
        )
        self.check_dumpsys(
            self.sdv_device_client,
            result.format(
                from_vm=self.CLIENT_INSTANCE,
                to_vm=self.SERVER_INSTANCE,
            ),
        )

        # Check that the query created a cached client
        self.check_dumpsys(
            self.sdv_device_client,
            self.EXPECTED_RESULT_CACHED_CLIENT.format(
                to_vm=self.SERVER_INSTANCE
            ),
        )

        self.sdv_device_server.adb().log().info(
            'Executing VSIDL Diagnostics Query on Server'
        )

        # Execute binary that queries for VSIDL Diagnostics data (once) for same VM
        self.sdv_device_server.adb().execute_shell_command_in_subprocess(
            'vsidl_provider_query_loop_sameVM',
            query.format(
                to_vm=self.SERVER_INSTANCE,
                client_fqin=client_fqin,
                timestamp=str(int(time.time())),
                package_name=package_name,
            ),
        )

        # Verify Connection Success
        self.sdv_device_server.adb().log().info(
            'Verify connection success message from dumpsys on server'
        )
        self.check_dumpsys(
            self.sdv_device_server,
            result.format(
                from_vm=self.SERVER_INSTANCE,
                to_vm=self.SERVER_INSTANCE,
            ),
        )

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
            report = device.adb().dumpsys(self.VSIDL_QUERY_TESTER_BINDER_NAME)
            if expected_text in report:
                return
            time.sleep(poll_interval)

        asserts.fail(
            f'Message not found within timeout [{expected_text}] in dumpsys report: {report}'
        )

    def run_looped_query(self, query, result, client_fqin, package_name):
        self.check_vsidl_provider_agent_started(self.sdv_device_client)
        self.check_vsidl_provider_agent_started(self.sdv_device_server)

        # Log the start of the test
        self.sdv_device_client.adb().log().info(
            'Executing VSIDL Diagnostics Query on Client'
        )

        # Execute binary that queries for VSIDL Diagnostics data (repeatedly)
        self.sdv_device_client.adb().execute_shell_command_in_subprocess(
            'vsidl_provider_query_loop',
            query.format(
                to_vm=self.SERVER_INSTANCE,
                client_fqin=client_fqin,
                timestamp=str(int(time.time())),
                package_name=package_name,
            )
            + ' --num-loops 30 --loop-period 1000',
        )

        # Verify Connection Success
        self.sdv_device_client.adb().log().info(
            'Verify connection success message from dumpsys on client'
        )
        check_for_result = result.format(
            from_vm=self.CLIENT_INSTANCE,
            to_vm=self.SERVER_INSTANCE,
        )
        self.check_dumpsys(
            self.sdv_device_client,
            check_for_result,
        )

        # Check that the query created a cached client
        self.check_dumpsys(
            self.sdv_device_client,
            self.EXPECTED_RESULT_CACHED_CLIENT.format(
                to_vm=self.SERVER_INSTANCE
            ),
        )

        # Reboot VM containing server
        # This will test the client cache clearing and recreating
        self.sdv_device_server.adb().reboot_device_and_verify_logcat()

        # Verify that the agent has been loaded on the server after we've killed it
        self.check_vsidl_provider_agent_started(self.sdv_device_server)

        # Verify Connection Success
        self.check_dumpsys(
            self.sdv_device_client,
            check_for_result,
        )

        # Check that the query created a cached client
        self.check_dumpsys(
            self.sdv_device_client,
            self.EXPECTED_RESULT_CACHED_CLIENT.format(
                to_vm=self.SERVER_INSTANCE
            ),
        )

    @parameterized.named_parameters(
        # --- Test Cases for apex_with_per_bundle_config_file ---
        {
            'testcase_name': 'diagnostics_query_per_bundle_config',
            'query': VSIDL_DIAGNOSTICS_QUERY,
            'expected': EXPECTED_RESULT_DIAGNOSTICS_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientDiag/default',
            'package_name': APEX_WITH_PER_BUNDLE_CONFIG_FILE,
        },
        {
            'testcase_name': 'publication_descriptor_query_per_bundle_config',
            'query': VSIDL_PUBLICATION_DESCRIPTOR_QUERY,
            'expected': EXPECTED_RESULT_PUBLICATION_DESCRIPTOR_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientPub/default',
            'package_name': APEX_WITH_PER_BUNDLE_CONFIG_FILE,
        },
        {
            'testcase_name': 'rpc_method_descriptor_query_per_bundle_config',
            'query': VSIDL_RPC_METHOD_DESCRIPTOR_QUERY,
            'expected': EXPECTED_RESULT_RPC_METHOD_DESCRIPTOR_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientRpc/default',
            'package_name': APEX_WITH_PER_BUNDLE_CONFIG_FILE,
        },
        {
            'testcase_name': 'message_descriptor_query_per_bundle_config',
            'query': VSIDL_MESSAGE_DESCRIPTOR_QUERY,
            'expected': EXPECTED_RESULT_MESSAGE_DESCRIPTOR_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientMessage/default',
            'package_name': APEX_WITH_PER_BUNDLE_CONFIG_FILE,
        },
        # --- Test Cases for apex_with_shared_config_file ---
        {
            'testcase_name': 'diagnostics_query_shared_config',
            'query': VSIDL_DIAGNOSTICS_QUERY,
            'expected': EXPECTED_RESULT_DIAGNOSTICS_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientDiag/default',
            'package_name': APEX_WITH_SHARED_CONFIG_FILE,
        },
        {
            'testcase_name': 'publication_descriptor_query_shared_config',
            'query': VSIDL_PUBLICATION_DESCRIPTOR_QUERY,
            'expected': EXPECTED_RESULT_PUBLICATION_DESCRIPTOR_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientPub/default',
            'package_name': APEX_WITH_SHARED_CONFIG_FILE,
        },
        {
            'testcase_name': 'rpc_method_descriptor_query_shared_config',
            'query': VSIDL_RPC_METHOD_DESCRIPTOR_QUERY,
            'expected': EXPECTED_RESULT_RPC_METHOD_DESCRIPTOR_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientRpc/default',
            'package_name': APEX_WITH_SHARED_CONFIG_FILE,
        },
        {
            'testcase_name': 'message_descriptor_query_shared_config',
            'query': VSIDL_MESSAGE_DESCRIPTOR_QUERY,
            'expected': EXPECTED_RESULT_MESSAGE_DESCRIPTOR_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientMessage/default',
            'package_name': APEX_WITH_SHARED_CONFIG_FILE,
        },
    )
    def test_vsidl_provider(
        self, query, expected, client_fqin, package_name
    ):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        self.run_query(
            query,
            expected,
            client_fqin,
            package_name,
        )

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} ended'
        )

    @parameterized.named_parameters(
        # --- Test Cases for apex_with_per_bundle_config_file ---
        {
            'testcase_name': 'diagnostics_query_per_bundle_config',
            'query': VSIDL_DIAGNOSTICS_QUERY,
            'expected': EXPECTED_RESULT_DIAGNOSTICS_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientDiagLooped/default',
            'package_name': APEX_WITH_PER_BUNDLE_CONFIG_FILE,
        },
        {
            'testcase_name': 'publication_descriptor_query_per_bundle_config',
            'query': VSIDL_PUBLICATION_DESCRIPTOR_QUERY,
            'expected': EXPECTED_RESULT_PUBLICATION_DESCRIPTOR_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientPubLooped/default',
            'package_name': APEX_WITH_PER_BUNDLE_CONFIG_FILE,
        },
        {
            'testcase_name': 'rpc_method_descriptor_query_per_bundle_config',
            'query': VSIDL_RPC_METHOD_DESCRIPTOR_QUERY,
            'expected': EXPECTED_RESULT_RPC_METHOD_DESCRIPTOR_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientRpcLooped/default',
            'package_name': APEX_WITH_PER_BUNDLE_CONFIG_FILE,
        },
        {
            'testcase_name': 'message_descriptor_query_per_bundle_config',
            'query': VSIDL_MESSAGE_DESCRIPTOR_QUERY,
            'expected': EXPECTED_RESULT_MESSAGE_DESCRIPTOR_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientMessageLooped/default',
            'package_name': APEX_WITH_PER_BUNDLE_CONFIG_FILE,
        },
        # --- Test Cases for apex_with_shared_config_file ---
        {
            'testcase_name': 'diagnostics_query_shared_config',
            'query': VSIDL_DIAGNOSTICS_QUERY,
            'expected': EXPECTED_RESULT_DIAGNOSTICS_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientDiagLooped/default',
            'package_name': APEX_WITH_SHARED_CONFIG_FILE,
        },
        {
            'testcase_name': 'publication_descriptor_query_shared_config',
            'query': VSIDL_PUBLICATION_DESCRIPTOR_QUERY,
            'expected': EXPECTED_RESULT_PUBLICATION_DESCRIPTOR_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientPubLooped/default',
            'package_name': APEX_WITH_SHARED_CONFIG_FILE,
        },
        {
            'testcase_name': 'rpc_method_descriptor_query_shared_config',
            'query': VSIDL_RPC_METHOD_DESCRIPTOR_QUERY,
            'expected': EXPECTED_RESULT_RPC_METHOD_DESCRIPTOR_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientRpcLooped/default',
            'package_name': APEX_WITH_SHARED_CONFIG_FILE,
        },
        {
            'testcase_name': 'message_descriptor_query_shared_config',
            'query': VSIDL_MESSAGE_DESCRIPTOR_QUERY,
            'expected': EXPECTED_RESULT_MESSAGE_DESCRIPTOR_QUERY,
            'client_fqin': 'ignored:com.sdv.google.sample.vsidl_provider.ClientMessageLooped/default',
            'package_name': APEX_WITH_SHARED_CONFIG_FILE,
        },
    )
    def test_vsidl_provider_with_agent_restart(
        self, query, expected, client_fqin, package_name
    ):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        self.run_looped_query(
            query,
            expected,
            client_fqin,
            package_name,
        )

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} ended'
        )


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
