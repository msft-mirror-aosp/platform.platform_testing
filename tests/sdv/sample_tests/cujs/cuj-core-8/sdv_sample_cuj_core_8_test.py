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
SDV sample 'CUJ-Core-8' test.

Test is based on CORE-CUJ catalog and samples.

Verifies:

 * SDV orchestration on VM start-up,
 * Cross-vm publishing and message active pooling,
"""

import logging
import time

from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling

class SdvCujCore8Test(sdv_base_test.SdvBaseTestClass, parameterized.TestCase):

    SAMPLES_LOGCAT_ARGS_ALL_BUNDLES = '*:F com_android_sdv_sample_foo_ServiceBundleFoo_instance:* com_android_sdv_sample_bar_ServiceBundleBar_instance:* com_android_sdv_sample_baz_ServiceBundleBaz_instance:*'
    SAMPLES_LOGCAT_ARGS_LIFECYCLE_MANAGER = '*:F lifecycle_manager:*'
    SAMPLES_LOGCAT_ARGS_FOO = '*:F com_android_sdv_sample_foo_ServiceBundleFoo_instance:*'
    SAMPLES_LOGCAT_ARGS_BAR = '*:F com_android_sdv_sample_bar_ServiceBundleBar_instance:*'
    SAMPLES_LOGCAT_ARGS_BAZ = '*:F com_android_sdv_sample_baz_ServiceBundleBaz_instance:*'

    # Test Parameters - logcat messages.
    STARTING_TEXT = 'Starting {vm_instance}:{package}.{bundle_name}/instance'
    SENT_MESSAGE = 'Sent.*FooMessage.*42'
    RECEIVED_MESSAGE = 'Received.*FooMessage.*42'
    # The " symbol needs to be escaped to \" to work with grep
    LIFECYCLE_STARTED = 'serviceBundleName: \\"{bundle_name}\\", serviceInstanceName: \\"instance\\" }} is started'
    # Test Parameters - error messages.
    ERROR_MESSAGE_LIFECYCLE_BUNDLE_NOT_STARTED = \
        '\n[FAILURE]: Lifecycle manager did not report {bundle_name} to be started.'
    ERROR_MESSAGE_CREATION_GREP = \
        '\n[FAILURE]: Service Bundle expected to be started.'
    ERROR_MESSAGE_FOO_MESSAGE_SEND_GREP = \
        '\n[FAILURE]: Service Bundle expected to send Foo Message.'
    ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP = \
        '\n[FAILURE]: Service Bundle expected to receive Foo Message.'

    ##################################################
    ## Setup/teardown for the test suite and tests. ##
    ##################################################

    def setup_class(self):
        super().setup_class()
        self.adb_devices = {}
        # Foo VM should be started first to avoid Bar failing to find publisher and rpc server.
        self.adb_devices['foo_baz_device'] = self.setup_device('device1')
        self.adb_devices['bar_device'] = self.setup_device('device2')

    def setup_test(self):
        # Avoid parent `clear_all_devices` to store the start up logs from VMs.
        logging.info('Custom CUJ8 test setup')

    def teardown_test(self):
        # Avoid parent `clear_all_devices` to store the start up logs from VMs.
        logging.info('Custom CUJ8 test teardown')

    def teardown_class(self):
        # Explicitly clear devices, since we avoid it in `setup_test` and `teardown_test`.
        logging.info('Custom CUJ8 test class teardown')
        self.clear_all_devices()
        super().teardown_class()

    ################################################
    ##            Utility functions.              ##
    ################################################

    def setup_device(self, device_name):
        """ Setup VM with `device_name` """
        sdv_device = self.get_device(device_name)
        adb_device = sdv_device.adb()
        adb_device.wait_for_device_online()
        # TODO(b/381241303): Device rebooting is needed to allow tests to check early VM boot logs
        # which are not available when the test is started.
        adb_device.reboot_device()
        adb_device.wait_for_device_online()
        adb_device.verify_logcat_is_running()
        return adb_device

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
            'device_name': 'foo_baz_device',
            'vm_instance': 'instance1',
            'package_name': 'com.android.sdv.sample.foo',
            'bundle_name' : 'ServiceBundleFoo'
        },
        {
            'testcase_name': 'baz',
            'device_name': 'foo_baz_device',
            'vm_instance': 'instance1',
            'package_name': 'com.android.sdv.sample.baz',
            'bundle_name' : 'ServiceBundleBaz'
        },
        {
            'testcase_name': 'bar',
            'device_name': 'bar_device',
            'vm_instance': 'instance2',
            'package_name': 'com.android.sdv.sample.bar',
            'bundle_name' : 'ServiceBundleBar'
        },
    )
    def test_service_bundle(self, device_name, vm_instance, package_name, bundle_name):
        self.log_enter()
        # Lifecycle manager reports that the bundle was started.
        polling.wait_and_verify_expected_logs(
            self.adb_devices[device_name],
            logcat_args = self.SAMPLES_LOGCAT_ARGS_LIFECYCLE_MANAGER,
            grep_text = self.LIFECYCLE_STARTED.format(bundle_name = bundle_name),
            assert_msg =  self.ERROR_MESSAGE_LIFECYCLE_BUNDLE_NOT_STARTED.format(bundle_name = bundle_name)
        )

        # Service bundle itself reports starting.
        polling.wait_and_verify_expected_logs(
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
        polling.wait_and_verify_expected_logs(
            sdv_device = self.adb_devices['foo_baz_device'],
            logcat_args = self.SAMPLES_LOGCAT_ARGS_FOO,
            grep_text = self.SENT_MESSAGE,
            assert_msg = self.ERROR_MESSAGE_FOO_MESSAGE_SEND_GREP,
        )
        # FooMessage received by ServiceBundleBaz
        polling.wait_and_verify_expected_logs(
            sdv_device = self.adb_devices['foo_baz_device'],
            logcat_args = self.SAMPLES_LOGCAT_ARGS_BAZ,
            grep_text = self.RECEIVED_MESSAGE,
            assert_msg = self.ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP,
        )
        # FooMessage received by ServiceBundleBar
        polling.wait_and_verify_expected_logs(
            sdv_device = self.adb_devices['bar_device'],
            logcat_args = self.SAMPLES_LOGCAT_ARGS_BAR,
            grep_text = self.RECEIVED_MESSAGE,
            assert_msg = self.ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP,
        )
        self.log_exit()

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
