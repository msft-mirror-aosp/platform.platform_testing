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
SDV sample 'CUJ-Core-27' test.

Test is based on CORE-CUJ catalog and samples, specifically the README of CUJ-Core-27.

Verifies:
 * SDV orchestration on 3 VMs start-up (2 Qux, 1 Foo).
 * Cross-vm MultiPub Pub/Sub: Two Qux instances publishing, one Foo instance subscribing.
 * ACLs enforcement for cross-VM communication.
"""

import logging
import time

from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods import waiting_methods

class SdvSampleCujCore27Test(sdv_base_test.SdvBaseTestClass, parameterized.TestCase):

    # Logcat arguments tailored for CUJ27 components
    SAMPLES_LOGCAT_ARGS_ALL = '*:F com_sdv_google_sample_foo_ServiceBundleFoo_instance:* com_sdv_google_sample_qux_ServiceBundleQux_instance:*'
    SAMPLES_LOGCAT_ARGS_LIFECYCLE_MANAGER = '*:F lifecycle_manager:*'
    SAMPLES_LOGCAT_ARGS_FOO = '*:F com_sdv_google_sample_foo_ServiceBundleFoo_instance:*'
    SAMPLES_LOGCAT_ARGS_QUX = '*:F com_sdv_google_sample_qux_ServiceBundleQux_instance:*'
    SAMPLES_LOGCAT_ARGS_ACLS = '*:F SdvServiceManagerServer:*' # Assuming ACL logs are here as per CUJ22

    SET_VERBOSE_PROPERTY_COMMAND = 'setprop persist.log.tag V'
    SET_ORCH_PROPERTY_COMMAND = 'setprop persist.sdv.orchestrator_config_path {path}'

    # Test Parameters - logcat messages based on README-Core-27 and examples
    STARTING_TEXT = 'Starting {vm_instance}:{package}.{bundle_name}/instance'
    LIFECYCLE_STARTED_FOO = 'Service bundle.*com.sdv.google.sample.foo.*ServiceBundleFoo.*is started'
    LIFECYCLE_STARTED_QUX = 'Service bundle.*com.sdv.google.sample.qux.*ServiceBundleQux.*is started'
    SENT_MESSAGE = 'Sent.*QuxMessage.*42'
    RECEIVED_MESSAGE = 'Received.*QuxMessage.*42'
    # ACL messages from README-Core-27
    AUTHZ_QUX_PUB_VM1 = 'QuxMessage allows access to instance1'
    AUTHZ_QUX_PUB_VM2 = 'QuxMessage allows access to instance2'
    AUTHZ_FOO_SUB = 'ServiceBundleQux/instance#com-sdv-google-sample-qux-qux-message-unique allows access to instance3'


    # Test Parameters - error messages
    ERROR_MESSAGE_LIFECYCLE_BUNDLE_NOT_STARTED = \
        '\n[FAILURE]: Lifecycle manager did not report {bundle_name} on {vm_instance} to be started.'
    ERROR_MESSAGE_CREATION_GREP = \
        '\n[FAILURE]: Service Bundle {bundle_name} on {vm_instance} expected to be started.'
    ERROR_MESSAGE_QUX_MESSAGE_SEND_GREP = \
        '\n[FAILURE]: Service Bundle Qux on {vm_instance} expected to send Qux Message.'
    ERROR_MESSAGE_QUX_MESSAGE_RECEIVE_GREP = \
        '\n[FAILURE]: Service Bundle Foo on instance3 expected to receive Qux Message.'
    ERROR_MESSAGE_AUTHZ_PUBLISHER_GREP = \
        '\n[FAILURE]: Publisher {bundle_name} on {vm_instance} expected publishing authorization log.'
    ERROR_MESSAGE_AUTHZ_SUBSCRIBER_GREP = \
        '\n[FAILURE]: Subscriber {bundle_name} on {vm_instance} expected subscription authorization log.'

    ##################################################
    ## Setup/teardown for the test suite and tests. ##
    ##################################################

    def setup_class(self):
        """Sets up 3 VMs with specific orchestration configs."""
        super().setup_class()
        self.adb_devices = {}
        # Setup VMs based on CUJ27 README
        self.adb_devices['qux_vm1'] = self.setup_device_orch_config(
            'device1', '/etc/orch/vm_qux_orch_config.textproto') # VM1 - Qux
        self.adb_devices['qux_vm2'] = self.setup_device_orch_config(
            'device2', '/etc/orch/vm_qux_orch_config.textproto') # VM2 - Qux
        self.adb_devices['foo_vm3'] = self.setup_device_orch_config(
            'device3', '/etc/orch/vm_foo_orch_config.textproto') # VM3 - Foo

    def setup_test(self):
        """Custom setup to preserve logs."""
        logging.info('Custom CUJ27 test setup')

    def teardown_test(self):
        """Custom teardown to preserve logs."""
        logging.info('Custom CUJ27 test teardown')

    def teardown_class(self):
        """Explicitly clear devices."""
        logging.info('Custom CUJ27 test class teardown')
        self.clear_all_devices()
        super().teardown_class()

    ################################################
    ##            Utility functions.              ##
    ################################################

    def setup_device_orch_config(self, device_name, path):
        """ Setup VM with `device_name` to use the `path` configuration. """
        sdv_device = self.get_device(device_name)
        adb_device = sdv_device.adb()
        adb_device.wait_for_device_online()
        adb_device.execute_shell_command(self.SET_ORCH_PROPERTY_COMMAND)
        adb_device.execute_shell_command(self.SET_ORCH_PROPERTY_COMMAND.format(path=path))
        adb_device.reboot_device()
        adb_device.wait_for_device_online()
        adb_device.verify_logcat_is_running()
        return adb_device

    def log_enter(self):
        """ Unified logging enter test suit. """
        logging.info(f"{self.get_suite_name()} :: Start test {self.current_test_info.name}")

    def log_exit(self):
        """ Unified logging exit test suit. """
        logging.info(f"{self.get_suite_name()} :: Finished test {self.current_test_info.name}")

    def assert_logcat(self, sdv_device, grep_text, logcat_args, assert_msg):
        """Assert-like validator to grep logcat with the `grep_text`."""
        waiting_methods.WaitingMethods.wait_and_verify_expected_logs(
              sdv_device=sdv_device,
              grep_text=grep_text,
              logcat_args=logcat_args,
              assert_msg=assert_msg,
              timeout=60,
        )

    ################################################
    ## Test orchestration configurations by       ##
    ## detecting service bundle creation.         ##
    ################################################
    @parameterized.named_parameters(
        {
            'testcase_name': 'qux_vm1',
            'device_key': 'qux_vm1',
            'vm_instance': 'instance1',
            'package_name': 'com.sdv.google.sample.qux',
            'bundle_name' : 'ServiceBundleQux',
            'lifecycle_grep': LIFECYCLE_STARTED_QUX
        },
        {
            'testcase_name': 'qux_vm2',
            'device_key': 'qux_vm2',
            'vm_instance': 'instance2',
            'package_name': 'com.sdv.google.sample.qux',
            'bundle_name' : 'ServiceBundleQux',
            'lifecycle_grep': LIFECYCLE_STARTED_QUX
        },
        {
            'testcase_name': 'foo_vm3',
            'device_key': 'foo_vm3',
            'vm_instance': 'instance3',
            'package_name': 'com.sdv.google.sample.foo',
            'bundle_name' : 'ServiceBundleFoo',
            'lifecycle_grep': LIFECYCLE_STARTED_FOO
        },
    )
    def test_service_bundle_startup(self, device_key, vm_instance, package_name, bundle_name, lifecycle_grep):
        """Tests that bundles are started by orchestration."""
        self.log_enter()
        adb_device = self.adb_devices[device_key]

        # Check lifecycle manager log
        self.assert_logcat(
            sdv_device = adb_device,
            grep_text = lifecycle_grep,
            logcat_args = self.SAMPLES_LOGCAT_ARGS_LIFECYCLE_MANAGER,
            assert_msg = self.ERROR_MESSAGE_LIFECYCLE_BUNDLE_NOT_STARTED.format(
                bundle_name = bundle_name, vm_instance = vm_instance),
        )

        # Check service bundle's own startup log
        self.assert_logcat(
            sdv_device = adb_device,
            grep_text = self.STARTING_TEXT.format(
                vm_instance = vm_instance,
                package = package_name,
                bundle_name = bundle_name,
            ),
            logcat_args = self.SAMPLES_LOGCAT_ARGS_ALL, # Check combined logs
            assert_msg = self.ERROR_MESSAGE_CREATION_GREP.format(
                bundle_name=bundle_name, vm_instance=vm_instance),
        )
        self.log_exit()

    ################################################
    ##               Test Pub/Sub.                ##
    ################################################
    def test_pub_sub_messages(self):
        """Tests Qux publishing and Foo subscribing across VMs."""
        self.log_enter()
        # QuxMessage sent by ServiceBundleQux on VM1
        self.assert_logcat(
            sdv_device = self.adb_devices['qux_vm1'],
            grep_text = self.SENT_MESSAGE,
            logcat_args = self.SAMPLES_LOGCAT_ARGS_QUX,
            assert_msg = self.ERROR_MESSAGE_QUX_MESSAGE_SEND_GREP.format(vm_instance='instance1'),
        )
         # QuxMessage sent by ServiceBundleQux on VM2
        self.assert_logcat(
            sdv_device = self.adb_devices['qux_vm2'],
            grep_text = self.SENT_MESSAGE,
            logcat_args = self.SAMPLES_LOGCAT_ARGS_QUX,
            assert_msg = self.ERROR_MESSAGE_QUX_MESSAGE_SEND_GREP.format(vm_instance='instance2'),
        )
        # QuxMessage received by ServiceBundleFoo on VM3
        # Note: We check for *at least one* message received, as the log might not distinguish sources easily.
        self.assert_logcat(
            sdv_device = self.adb_devices['foo_vm3'],
            grep_text = self.RECEIVED_MESSAGE,
            logcat_args = self.SAMPLES_LOGCAT_ARGS_FOO,
            assert_msg = self.ERROR_MESSAGE_QUX_MESSAGE_RECEIVE_GREP,
        )
        self.log_exit()

    ################################################
    ##                 Test ACLs.                 ##
    ################################################
    def test_acls(self):
        """Tests ACL enforcement logs for pub/sub."""
        self.log_enter()
        # Check Qux publish allowed on VM1
        self.assert_logcat(
            sdv_device = self.adb_devices['qux_vm1'],
            grep_text = self.AUTHZ_QUX_PUB_VM1,
            logcat_args = self.SAMPLES_LOGCAT_ARGS_ACLS,
            assert_msg = self.ERROR_MESSAGE_AUTHZ_PUBLISHER_GREP.format(
                bundle_name='Qux', vm_instance='instance1'),
        )
        # Check Qux publish allowed on VM2
        self.assert_logcat(
            sdv_device = self.adb_devices['qux_vm2'],
            grep_text = self.AUTHZ_QUX_PUB_VM2,
            logcat_args = self.SAMPLES_LOGCAT_ARGS_ACLS,
            assert_msg = self.ERROR_MESSAGE_AUTHZ_PUBLISHER_GREP.format(
                bundle_name='Qux', vm_instance='instance2'),
        )
        # Check Foo subscribe allowed on VM3
        self.assert_logcat(
            sdv_device = self.adb_devices['foo_vm3'],
            grep_text = self.AUTHZ_FOO_SUB,
            logcat_args = self.SAMPLES_LOGCAT_ARGS_ACLS,
            assert_msg = self.ERROR_MESSAGE_AUTHZ_SUBSCRIBER_GREP.format(
                bundle_name='Foo', vm_instance='instance3'),
        )
        self.log_exit()


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework
    sdv_test_runner.run()
