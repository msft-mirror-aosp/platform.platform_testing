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
SDV sample 'CUJ-Core-25' test.

Test is based on CORE-CUJ catalog and samples.

Verifies:

 * SDV orchestration on VM start-up,
 * Cross-vm publishing and message active pooling,
"""

import logging
import time

from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods

class SdvCujCore25Test(sdv_base_test.SdvBaseTestClass, parameterized.TestCase):

    SAMPLES_LOGCAT_ARGS_ALL_BUNDLES = '*:F com_sdv_google_sample_foo_ServiceBundleFoo_instance:* com_sdv_google_sample_bar_ServiceBundleBar_instance:* com_sdv_google_sample_baz_ServiceBundleBaz_instance:*'
    SAMPLES_LOGCAT_ARGS_LIFECYCLE_MANAGER = '*:F lifecycle_manager:*'
    SAMPLES_LOGCAT_ARGS_FOO = '*:F com_sdv_google_sample_foo_ServiceBundleFoo_instance:*'
    SAMPLES_LOGCAT_ARGS_BAR = '*:F com_sdv_google_sample_bar_ServiceBundleBar_instance:*'
    SAMPLES_LOGCAT_ARGS_BAZ = '*:F com_sdv_google_sample_baz_ServiceBundleBaz_instance:*'

    # Foo VM properties
    SYS_PROP_FOO_PUB_INTERVAL = "persist.com.sdv.google.sample.foo.pub_interval_ms"
    SYS_PROP_FOO_PUB_INTERVAL_VALUE = 1000
    SYS_PROP_FOO_LOAD_SIZE = "persist.com.sdv.google.sample.foo.load_size"
    SYS_PROP_FOO_LOAD_SIZE_VALUE = 200000
    SYS_PROP_FOO_PUB_AMOUNT = "persist.com.sdv.google.sample.foo.pub_maximum_amount"
    SYS_PROP_FOO_PUB_AMOUNT_VALUE = 10

    # Bar VM properties
    SYS_PROP_BAR_RPC_INTERVAL = "persist.com.sdv.google.sample.bar.rpc_interval_ms"
    SYS_PROP_BAR_RPC_INTERVAL_VALUE = 1000
    SYS_PROP_BAR_LOAD_SIZE = "persist.com.sdv.google.sample.bar.load_size"
    SYS_PROP_BAR_LOAD_SIZE_VALUE = 200000

    # Baz VM properties
    SYS_PROP_BAZ_RPC_INTERVAL = "persist.com.sdv.google.sample.baz.rpc_interval_ms"
    SYS_PROP_BAZ_RPC_INTERVAL_VALUE = 1000
    SYS_PROP_BAZ_LOAD_SIZE = "persist.com.sdv.google.sample.baz.load_size"
    SYS_PROP_BAZ_LOAD_SIZE_VALUE = 200000

    # Test Parameters - logcat messages.
    STARTING_TEXT = 'Starting {vm_instance}:{package}.{bundle_name}/instance'
    SENT_MESSAGE = 'Sent.*FooMessage.*42'
    RECEIVED_MESSAGE = 'Received.*FooMessage.*42'
    LIFECYCLE_STARTED = '{bundle_name} state is STARTED'
    RPC_REQUEST_RESPONSE = "GetFooRequest.*GetFooResponse"
    # Test Parameters - error messages.
    ERROR_MESSAGE_LIFECYCLE_BUNDLE_NOT_STARTED = \
        '\n[FAILURE]: Lifecycle manager did not report {bundle_name} to be started.'
    ERROR_MESSAGE_CREATION_GREP = \
        '\n[FAILURE]: Service Bundle expected to be started.'
    ERROR_MESSAGE_FOO_MESSAGE_SEND_GREP = \
        '\n[FAILURE]: Service Bundle expected to send Foo Message.'
    ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP = \
        '\n[FAILURE]: Service Bundle expected to receive Foo Message.'
    ERROR_MESSAGE_FOO_RPC_GREP = \
        '\n[FAILURE]: Service Bundle expected to call Foo RPC.'



    ##################################################
    ## Setup/teardown for the test suite and tests. ##
    ##################################################

    def setup_class(self):
        super().setup_class()
        self.adb_devices = {}

        # Setup Foo VM
        foo_device = self.get_device('device1').adb()
        # Set publishing interval.
        self.set_sys_property(foo_device, self.SYS_PROP_FOO_PUB_INTERVAL, self.SYS_PROP_FOO_PUB_INTERVAL_VALUE)
        # Set publishing load size.
        self.set_sys_property(foo_device, self.SYS_PROP_FOO_LOAD_SIZE, self.SYS_PROP_FOO_LOAD_SIZE_VALUE)
        # Set publishing amount.
        self.set_sys_property(foo_device, self.SYS_PROP_FOO_PUB_AMOUNT, self.SYS_PROP_FOO_PUB_AMOUNT_VALUE)
        self.reboot_device(foo_device)
        self.adb_devices['foo_device'] = foo_device

        # Setup Bar VM
        bar_device = self.get_device('device2').adb()
        # Set publishing interval.
        self.set_sys_property(bar_device, self.SYS_PROP_BAR_RPC_INTERVAL, self.SYS_PROP_BAR_RPC_INTERVAL_VALUE)
        # Set publishing load size.
        self.set_sys_property(bar_device, self.SYS_PROP_BAR_LOAD_SIZE, self.SYS_PROP_BAR_LOAD_SIZE_VALUE)
        self.reboot_device(bar_device)
        self.adb_devices['bar_device'] = bar_device

        # Setup Baz VM
        baz_device = self.get_device('device3').adb()
        # Set publishing interval.
        self.set_sys_property(baz_device, self.SYS_PROP_BAZ_RPC_INTERVAL, self.SYS_PROP_BAZ_RPC_INTERVAL_VALUE)
        # Set publishing load size.
        self.set_sys_property(baz_device, self.SYS_PROP_BAZ_LOAD_SIZE, self.SYS_PROP_BAZ_LOAD_SIZE_VALUE)
        self.reboot_device(baz_device)
        self.adb_devices['baz_device'] = baz_device


    def setup_test(self):
        # Avoid parent `clear_all_devices` to store the start up logs from VMs.
        logging.info('Custom CUJ25 test setup')

    def teardown_test(self):
        # Avoid parent `clear_all_devices` to store the start up logs from VMs.
        logging.info('Custom CUJ25 test teardown')

    def teardown_class(self):
        # Explicitly clear devices, since we avoid it in `setup_test` and `teardown_test`.
        logging.info('Custom CUJ25 test class teardown')
        self.clear_all_devices()
        super().teardown_class()

    ################################################
    ##            Utility functions.              ##
    ################################################

    def set_sys_property(self, adb_device, sys_property, value):
        """ Set the system property. """
        adb_device.execute_shell_command(f'setprop {sys_property} {value}')

    def reboot_device(self, adb_device):
        """ Reboot VM with `adb_device` """
        adb_device.wait_for_device_online()
        # TODO(b/381241303): Device rebooting is needed to allow tests to check early VM boot logs
        # which are not available when the test is started.
        adb_device.reboot_device()
        adb_device.wait_for_device_online()
        adb_device.verify_logcat_is_running()

    def log_enter(self):
        """ Unified logging enter test suit. """
        logging.info(f"{self.get_suite_name()} :: Start Test {self.current_test_info.name}")

    def log_exit(self):
        """ Unified logging exit test suit. """
        logging.info(f"{self.get_suite_name()} :: Start Test {self.current_test_info.name}")

    ################################################
    ## Test orchestration configurations by       ##
    ## detecting service bundle creation.         ##
    ################################################
    @parameterized.named_parameters(
        {
            'testcase_name': 'foo',
            'device_name': 'foo_device',
            'vm_instance': 'instance1',
            'package_name': 'com.sdv.google.sample.foo',
            'bundle_name' : 'ServiceBundleFoo'
        },
        {
            'testcase_name': 'bar',
            'device_name': 'bar_device',
            'vm_instance': 'instance2',
            'package_name': 'com.sdv.google.sample.bar',
            'bundle_name' : 'ServiceBundleBar'
        },
        {
            'testcase_name': 'baz',
            'device_name': 'baz_device',
            'vm_instance': 'instance3',
            'package_name': 'com.sdv.google.sample.baz',
            'bundle_name' : 'ServiceBundleBaz'
        },
    )
    def test_service_bundle(self, device_name, vm_instance, package_name, bundle_name):
        self.log_enter()
        # Lifecycle manager reports that the bundle was started.
        WaitingMethods.wait_and_verify_expected_logs(
            self.adb_devices[device_name],
            logcat_args = self.SAMPLES_LOGCAT_ARGS_LIFECYCLE_MANAGER,
            grep_text = self.LIFECYCLE_STARTED.format(bundle_name = bundle_name),
            assert_msg =  self.ERROR_MESSAGE_LIFECYCLE_BUNDLE_NOT_STARTED.format(bundle_name = bundle_name)
        )

        # Service bundle itself reports starting.
        WaitingMethods.wait_and_verify_expected_logs(
            self.adb_devices[device_name],
            logcat_args = self.SAMPLES_LOGCAT_ARGS_ALL_BUNDLES,
            grep_text = self.STARTING_TEXT.format(
                vm_instance = vm_instance,
                package = package_name,
                bundle_name = bundle_name,
            ),
            assert_msg = self.ERROR_MESSAGE_CREATION_GREP,
        )

        self.log_exit()


    ################################################
    ##               Test Pub/Sub.                ##
    ################################################
    def test_pub_sub_messages(self):
        self.log_enter()
        # FooMessage sent by ServiceBundleFoo
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_devices['foo_device'],
            logcat_args = self.SAMPLES_LOGCAT_ARGS_FOO,
            grep_text = self.SENT_MESSAGE,
            assert_msg = self.ERROR_MESSAGE_FOO_MESSAGE_SEND_GREP,
        )
        # FooMessage received by ServiceBundleBaz
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_devices['baz_device'],
            logcat_args = self.SAMPLES_LOGCAT_ARGS_BAZ,
            grep_text = self.RECEIVED_MESSAGE,
            assert_msg = self.ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP,
        )
        # FooMessage received by ServiceBundleBar
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_devices['bar_device'],
            logcat_args = self.SAMPLES_LOGCAT_ARGS_BAR,
            grep_text = self.RECEIVED_MESSAGE,
            assert_msg = self.ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP,
        )
        self.log_exit()


    ################################################
    ##                 Test RPC.                  ##
    ################################################
    def test_rpc(self):
        self.log_enter()
        # FooRPC::foo(GetFooRequest) and GetFooResponse are received.
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_devices['bar_device'],
            grep_text = self.RPC_REQUEST_RESPONSE,
            assert_msg = self.ERROR_MESSAGE_FOO_RPC_GREP,
        )

        # FooRPC::foo(GetFooRequest) and GetFooResponse are received.
        WaitingMethods.wait_and_verify_expected_logs(
            sdv_device = self.adb_devices['baz_device'],
            grep_text = self.RPC_REQUEST_RESPONSE,
            assert_msg = self.ERROR_MESSAGE_FOO_RPC_GREP,
        )
        self.log_exit()



if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
