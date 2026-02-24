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

"""SDV service bundle memory leak detection test.

Test is based on CORE-CUJ catalog and samples.

Verifies:

 * Memory leaks on the service bundle start and stop.
"""

from mobly import asserts
import logging
import math
import time
from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.verification import polling


class SdvSampleCujMemoryLeakDetection(
    sdv_base_test.SdvBaseTestClass, parameterized.TestCase
):

    FOO_FQN = 'com.android.sdv.sample.foo.ServiceBundleFoo'
    QUX_FQN = 'com.android.sdv.sample.qux.ServiceBundleQux'

    SYS_PROP_FOO_INTERVAL = 'persist.com.android.sdv.sample.foo.pub_interval_ms'
    SYS_PROP_QUX_INTERVAL = 'persist.com.android.sdv.sample.qux.pub_interval_ms'
    SYS_PROP_FOO_LOAD_SIZE = 'persist.com.android.sdv.sample.foo.load_size'
    SYS_PROP_QUX_LOAD_SIZE = 'persist.com.android.sdv.sample.qux.load_size'

    CREATED_LOG = 'Creating instance.*:{fqn}/instance'
    STARTED_LOG = 'Starting instance.*:{fqn}/instance'
    STOPPED_LOG = 'Joined execution thread'

    ASSERT_MESSAGE_START = 'Failed to start service bundle'
    ASSERT_MESSAGE_STOP = 'Failed to stop service bundle'

    SAMPLES_LOGCAT_ARGS = '*:F com_android_sdv_sample_foo_ServiceBundleFoo_instance:* com_android_sdv_sample_qux_ServiceBundleQux_instance:*'
    MSEC_TO_SEC = 0.001
    MEMORY_USAGE_ON_CREATE = 20000
    MEMORY_USAGE_ON_START = 95000
    MEMORY_USAGE_ON_STOP = 25000
    START = 'start'
    STOP = 'create'
    SHUTDOWN = 'destroy'

    ################################################
    ##   Setup and teardown for the test suite.   ##
    ################################################

    def setup_class(self):
        super().setup_class()
        self.sdv_device_foo = self.get_device('device1')
        self.sdv_device_qux = self.get_device('device2')

    def setup_test(self):
        super().setup_test()
        # Shutdown Qux and Foo bundles to avoid issues with starting them as a part of the test.
        self.sdv_service_bundle(
            self.sdv_device_qux, self.QUX_FQN, self.SHUTDOWN, True
        )
        self.sdv_service_bundle(
            self.sdv_device_foo, self.FOO_FQN, self.SHUTDOWN, True
        )

    def teardown_test(self):
        # Shutdown previously started Qux and Foo bundles.
        self.sdv_service_bundle(
            self.sdv_device_qux, self.QUX_FQN, self.SHUTDOWN, True
        )
        self.sdv_service_bundle(
            self.sdv_device_foo, self.FOO_FQN, self.SHUTDOWN, True
        )
        super().teardown_test()

    ################################################
    ##    Utility functions.                      ##
    ################################################

    def log_enter(self):
        """Unified logging enter test suit."""
        logging.info(
            f'{self.get_suite_name()} :: Start test'
            f' {self.current_test_info.name}'
        )

    def log_exit(self):
        """Unified logging exit test suit."""
        logging.info(
            f'{self.get_suite_name()} :: Finished test'
            f' {self.current_test_info.name}'
        )

    def set_sys_property(self, sdv_device, sys_property, value):
        """Set the system property."""
        sdv_device.adb().execute_shell_command(
            f'setprop {sys_property} {value}'
        )

    def sdv_service_bundle(
        self, sdv_device, service_bundle_name, command, ignore_errors=False
    ):
        """Starts the service bundle by the provided name."""
        ignore_command = ' || true' if ignore_errors else ''
        sdv_device.adb().execute_shell_command(
            'sdv_service_bundle'
            f' {command} local-vm:{service_bundle_name}/instance'
            f' {ignore_command}'
        )

    def get_service_bundle_pid(self, sdv_device, grep_text):
        """Parse the process id of the service bundle from logcat.

        Args:
            sdv_device: The device to wait for logcat.
            grep_text: The text to search for the process PID.

        Returns:
            Service bundle PID.
        """
        logcat_grep_result = sdv_device.adb().grep_from_logcat(
            grep_text, self.SAMPLES_LOGCAT_ARGS
        )
        return int(logcat_grep_result.split()[2])

    def create_service_bundle(self, sdv_device, service_bundle_name):
        """Creates the service bundle on the provided device.

        Args:
            sdv_device: The device to wait for logcat.
            service_bundle_name: The FQN name of the service bundle to be
              created.

        Returns:
            Service bundle PID.
        """
        self.sdv_service_bundle(sdv_device, service_bundle_name, 'create')
        created_log_message = self.CREATED_LOG.format(fqn=service_bundle_name)
        polling.wait_and_verify_expected_logs(
            sdv_device.adb(),
            created_log_message,
            logcat_args=self.SAMPLES_LOGCAT_ARGS,
        )
        return self.get_service_bundle_pid(sdv_device, created_log_message)

    def detect_memory_usage(self, sdv_device, pid):
        """Detects the memory usage of the process with {pid}.

        Args:
            sdv_device: The device to wait for logcat.
            pid: Service bundle PID.

        Returns:
            RSS (Resident Set Size) non-swapped physical memory.
        """
        rss_result = sdv_device.adb().execute_shell_command(
            f'ps -Ap {pid} -o rss'
        )
        return int(rss_result.split()[1])

    ################################################
    ##           Assert-like validators.          ##
    ################################################

    def assert_logcat(self, sdv_device, grep_text, assert_msg):
        """Assert-like validator to grep logcat with the `grep_text`.

        Args:
            sdv_device: The device to assert the logcat.
            grep_text: The text to search for in the logcat output.
            assert_msg: The failure assertion message.
        """
        found_logs = polling.wait_and_return_result(
            lambda: sdv_device.adb().grep_from_logcat(grep_text, self.SAMPLES_LOGCAT_ARGS) or None,
        )

        if found_logs is None:
            logcat_grep_result = sdv_device.adb().grep_from_logcat(
                '', self.SAMPLES_LOGCAT_ARGS
            )
            asserts.fail(
                f"{assert_msg}. Samples logcat: {logcat_grep_result}"
            )

    def assert_memory_usage(self, sdv_device, pid, memory_maximum):
        """Assert-like validator to check the memory usage for {pid} on the {sdv_device}`.

        Args:
            sdv_device: The device to search for pid.
            pid: The PID of the process the memory to be validated.
            memory_maximum: The maximum of memory to be validated.

        Returns:
            The {pid} process memory usage.
        """
        memory_usage = self.detect_memory_usage(sdv_device, pid)
        asserts.assert_less_equal(
            memory_usage, memory_maximum
        )
        return memory_usage

    #################################################################
    ## Tests memory usage of Foo and Qux service bundle            ##
    ##   when created - to not exceed the basic minimum,           ##
    ##   when started - to not exceed the allowed maximum,         ##
    ##   when stopped - to come back to the initial memory usage.  ##
    #################################################################
    @parameterized.named_parameters(
        # Simple fast scenario (~10 second long)
        {
            'testcase_name': 'x3_times_restart_to_send_30_messages_1KB_30ms',
            'start_stop_reaped_times': 3,
            'messages_amount': 30,
            'load_size_bytes': 1_000,
            'interval_ms': 30,
        },
        # Basic scenario (~1 minute long)
        {
            'testcase_name': (
                'x15_times_restart_to_send_300_messages_300KB_10ms'
            ),
            'start_stop_reaped_times': 15,
            'messages_amount': 300,
            'load_size_bytes': 300_000,
            'interval_ms': 10,
        },
        # Heavy and long scenario (~7 minutes long)
        {
            'testcase_name': (
                'x25_times_restart_to_send_10_000_messages_950KB_1ms'
            ),
            'start_stop_reaped_times': 25,
            'messages_amount': 10_000,
            'load_size_bytes': 950_000,
            'interval_ms': 1,
        },
    )
    def test_service_bundle_memory_leaks(
        self,
        start_stop_reaped_times,
        messages_amount,
        load_size_bytes,
        interval_ms,
    ):
        self.log_enter()
        # GIVEN - high frequency publishing interval and wide load size.
        self.set_sys_property(
            self.sdv_device_foo, self.SYS_PROP_FOO_INTERVAL, interval_ms
        )
        self.set_sys_property(
            self.sdv_device_foo, self.SYS_PROP_FOO_LOAD_SIZE, load_size_bytes
        )
        self.set_sys_property(
            self.sdv_device_foo, self.SYS_PROP_QUX_INTERVAL, interval_ms
        )
        self.set_sys_property(
            self.sdv_device_foo, self.SYS_PROP_QUX_LOAD_SIZE, load_size_bytes
        )

        # WHEN service bundles are created.
        qux_pid = self.create_service_bundle(self.sdv_device_qux, self.QUX_FQN)
        foo_pid = self.create_service_bundle(self.sdv_device_foo, self.FOO_FQN)
        # THEN the memory usage should be low.
        self.assert_memory_usage(
            self.sdv_device_qux, qux_pid, self.MEMORY_USAGE_ON_CREATE
        )
        self.assert_memory_usage(
            self.sdv_device_foo, foo_pid, self.MEMORY_USAGE_ON_CREATE
        )

        # Iterate {start_stop_reaped_times} times the bundles START-STOP cycle.
        for i in range(start_stop_reaped_times):
            # Logcat cleanup to avoid checking logs from the previous iterations.
            self.sdv_device_qux.adb().clear_logcat()
            self.sdv_device_foo.adb().clear_logcat()

            # WHEN service bundles are STARTED and work for a while (interval_ms * messages_amount).
            self.sdv_service_bundle(
                self.sdv_device_qux, self.QUX_FQN, self.START
            )
            self.assert_logcat(
                self.sdv_device_qux,
                self.STARTED_LOG.format(fqn=self.QUX_FQN),
                self.ASSERT_MESSAGE_START,
            )
            self.sdv_service_bundle(
                self.sdv_device_foo, self.FOO_FQN, self.START
            )
            self.assert_logcat(
                self.sdv_device_foo,
                self.STARTED_LOG.format(fqn=self.FOO_FQN),
                self.ASSERT_MESSAGE_START,
            )
            # Wait for (interval_ms*messages_amount) msecs.
            time.sleep(interval_ms * messages_amount * self.MSEC_TO_SEC)
            # THEN the memory usage should be not exceeding `MEMORY_USAGE_ON_START`.
            self.assert_memory_usage(
                self.sdv_device_qux, qux_pid, self.MEMORY_USAGE_ON_START
            )
            self.assert_memory_usage(
                self.sdv_device_foo, foo_pid, self.MEMORY_USAGE_ON_START
            )

            # WHEN service bundles are STOPPED
            self.sdv_service_bundle(
                self.sdv_device_qux, self.QUX_FQN, self.STOP
            )
            self.assert_logcat(
                self.sdv_device_qux, self.STOPPED_LOG, self.ASSERT_MESSAGE_STOP
            )
            self.sdv_service_bundle(
                self.sdv_device_foo, self.FOO_FQN, self.STOP
            )
            self.assert_logcat(
                self.sdv_device_foo, self.STOPPED_LOG, self.ASSERT_MESSAGE_STOP
            )
            # THEN the memory usage should be not exceeding `MEMORY_USAGE_ON_STOP`.
            self.assert_memory_usage(
                self.sdv_device_qux, qux_pid, self.MEMORY_USAGE_ON_STOP
            )
            self.assert_memory_usage(
                self.sdv_device_foo, foo_pid, self.MEMORY_USAGE_ON_STOP
            )
        self.log_exit()


if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
