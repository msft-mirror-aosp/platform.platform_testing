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
SDV message loss detection test.

Test is based on CORE-CUJ catalog and samples.

Verifies:

 * SDV cross-VM publication messages loss is less than 0.5%.
"""

from mobly import asserts
import logging
import math
import time
from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner

class SdvSampleCujMessageLossDetection(sdv_base_test.SdvBaseTestClass, parameterized.TestCase):

    QUX_FQN = "com.sdv.google.sample.qux.ServiceBundleQux"
    FOO_FQN = "com.sdv.google.sample.foo.ServiceBundleFoo"

    SYS_PROP_INTERVAL = "persist.com.sdv.google.sample.foo.pub_interval_ms"
    SYS_PROP_LOAD_SIZE = "persist.com.sdv.google.sample.foo.load_size"
    SYS_PROP_AMOUNT = "persist.com.sdv.google.sample.foo.pub_maximum_amount"

    SAMPLES_LOGCAT_ARGS = '*:F com_sdv_google_sample_foo_ServiceBundleFoo_instance-1:* com_sdv_google_sample_qux_ServiceBundleQux_instance-1:*'

    QUX_WAITING_FOO_LOG = "Waiting for publisher .*FooMessage"
    FOO_MESSAGES_SENT_LOG = 'Sent {amount} from {amount}'
    FOO_MESSAGES_RECEIVED_LOG = 'Received in total [0-9]\\{1,\\} messages.*FooMessage'
    ASSERT_MESSAGE_FOO_SENT = 'Failed to detect {amount} of sent messages'
    ASSERT_MESSAGE_FOO_RECEIVED = 'Failed to detect at least {amount} of received messages'

    MSEC_TO_SEC = 0.001

    ################################################
    ##   Setup and teardown for the test suite.   ##
    ################################################

    def setup_class(self):
        """Setup test suite."""
        super().setup_class()
        self.sdv_device_foo = self.get_device('device1')
        self.sdv_device_qux = self.get_device('device2')

    def setup_test(self):
        # Stop Qux and Foo bundles to avoid issues with starting them as a part of the test.
        self.stop_service_bundle(self.sdv_device_qux, self.QUX_FQN)
        self.stop_service_bundle(self.sdv_device_foo, self.FOO_FQN)
        super().setup_test()

    def teardown_test(self):
        # Stop started previously Qux and Foo bundles.
        self.stop_service_bundle(self.sdv_device_qux, self.QUX_FQN)
        self.stop_service_bundle(self.sdv_device_foo, self.FOO_FQN)
        super().teardown_test()

    ################################################
    ##    Utility functions.                      ##
    ################################################

    def log_enter(self):
        """ Unified logging enter test suit. """
        logging.info(f"{self.get_suite_name()} :: Start test {self.current_test_info.name}")

    def log_exit(self):
        """ Unified logging exit test suit. """
        logging.info(f"{self.get_suite_name()} :: Finished test {self.current_test_info.name}")

    def set_sys_property(self, sdv_device, sys_property, value):
        """ Set the system property. """
        sdv_device.adb().execute_shell_command(f'setprop {sys_property} {value}')

    def start_service_bundle(self, sdv_device, service_bundle_name):
        """ Starts the service bundle by the provided name. """
        sdv_device.adb().execute_shell_command(
            f'sdv_service_bundle start local-vm:{service_bundle_name}/instance-1')

    def stop_service_bundle(self, sdv_device, service_bundle_name):
        """ Attempts to destroy the service bundle by the provided name. """
        sdv_device.adb().execute_shell_command(
            f'sdv_service_bundle destroy local-vm:{service_bundle_name}/instance-1 || true')

    def log_verification(log_messages, minimal_amount):
        """Verification of log messages containing the received value bigger than {minimal_amount}.
            Example of log string:
                01-01 00:00:00.000  111  222 I com_sdv_google_sample_qux_ServiceBundleQux_instance-1: sample: Received in total 999 messages, 0 errors detected.
        Args:
            log: The logcat messages.
            minimal_amount: The minimal amount of messages to be detected.
        Returns:
            True if the log indicates at least {minimal_amount} of messages.
            False otherwise.
        """
        try:
            last_message = log_messages.rsplit('\n', 1)
            found_value = int(last_message.split(' ')[11])
            if found_value < minimal_amount:
                logging.debug(f"{minimal_amount} is less than {found_value} in '{last_message}'")
            return found_value >= minimal_amount
        except Exception as error:
            logging.error(f"Failed to parse '{last_message}' with error: {error}")
            return False

    # TODO(b/382058513): migrate to framework implementation `wait_for_logcat` when ready.
    def wait_for_logcat(self, adb_device, grep_text, grep_args = None, timeout=30, poll_interval=0.1, extra_verification = None):
        """Polls the logcat output for a specific text until found or timeout.

        Args:
            adb_device: The device to wait for logcat.
            grep_text: The text to search for in the logcat output.
            grep_args: The argument to be passed to grep command.
            timeout: The maximum time (in seconds) to wait.
            poll_interval: The time (in seconds) between polls.
            extra_verification: The verification lambda when the grep result requires additional verification.
        Returns:
            True if grep matched at least one logcat output.
            False if grep matched no logcat output within the timeout.
        """
        deadline = time.perf_counter() + timeout
        while time.perf_counter() < deadline:
            logcat_grep_result = adb_device.grep_from_logcat(
                grep = grep_text,
                logcat_args = self.SAMPLES_LOGCAT_ARGS,
                grep_args = grep_args)
            if logcat_grep_result != '':
                if extra_verification is None:
                    return True
                if extra_verification(logcat_grep_result):
                    return True
                # extra_verification failed, continue waiting
            time.sleep(poll_interval)
        return False

    ################################################
    ##           Assert-like validators.          ##
    ################################################

    def assert_logcat(self, adb_device, grep_text, assert_msg, extra_verification = None):
        """Assert-like validator to grep logcat with the `grep_text`.

        Args:
            adb_device: The device to assert the logcat.
            grep_text: The text to search for in the logcat output.
            assert_msg: The failure assertion message.
            extra_verification: The verification lambda when the grep result requires additional verification.
        """
        if not self.wait_for_logcat(adb_device, grep_text, extra_verification = None):
            samples_logcat = adb_device.grep_from_logcat('', self.SAMPLES_LOGCAT_ARGS)
            with_validation = 'without' if extra_verification is None else 'with'
            asserts.assert_regex(
                grep_text,
                samples_logcat,
                f'{assert_msg}, {with_validation} extra verification. Samples logcat: {samples_logcat}'
            )

    ################################################
    ##    Tests sending FooMessages cross-VM.     ##
    ################################################
    @parameterized.named_parameters(
        # Basic scenario requested by OEM
        {
            'testcase_name': '1000_times_send_300KB_with_30_msec_interval',
            'interval_ms': 30,
            'load_size_bytes': 300_000,
            'amount': 1_000
        },
        # More strict scenarios
        {
            'testcase_name': '2500_times_send_950KB_with_10_msec_interval',
            'interval_ms': 10,
            'load_size_bytes': 950_000,
            'amount': 2_500
        },
        {
            'testcase_name': '5000_times_send_1MB_with_5_msec_interval',
            'interval_ms': 5,
            'load_size_bytes': 1_000_000,
            'amount': 5_000
        },
    )
    def test_pubsub_message_loss(self, interval_ms, load_size_bytes, amount):
        self.log_enter()
        # GIVEN
        # Set publishing interval.
        self.set_sys_property(self.sdv_device_foo, self.SYS_PROP_INTERVAL, interval_ms)
        # Set publishing load size.
        self.set_sys_property(self.sdv_device_foo, self.SYS_PROP_LOAD_SIZE, load_size_bytes)
        # Set publishing amount.
        self.set_sys_property(self.sdv_device_foo, self.SYS_PROP_AMOUNT, amount)
        # We need to detect at least 99.5% of sent messages.
        minimal_amount = math.floor(amount * 0.995)

        # WHEN
        # Start `Qux` bundle receiving and counting `FooMessage`s.
        self.start_service_bundle(self.sdv_device_qux, self.QUX_FQN)
        # Wait until `Qux` is started.
        self.wait_for_logcat(self.sdv_device_qux.adb(), self.QUX_WAITING_FOO_LOG)
        # Start `Foo` bundle publishing `FooMessage`s.
        self.start_service_bundle(self.sdv_device_foo, self.FOO_FQN)

        # Wait for (interval_ms*amount) msecs.
        time.sleep(interval_ms * amount * self.MSEC_TO_SEC)

        # THEN
        # Check that exact amount of messages were sent from Foo VM.
        self.assert_logcat(self.sdv_device_foo.adb(),
            self.FOO_MESSAGES_SENT_LOG.format(amount = amount),
            self.ASSERT_MESSAGE_FOO_SENT.format(amount = amount)
        )
        # THEN
        # The exact {minimal_amount} can be jumped over by the service bundle fetching several messages.
        # We use a verification function to check that received at least {minimal_amount}.
        verification = lambda log : self.log_verification(log, minimal_amount)

        self.assert_logcat(self.sdv_device_qux.adb(),
            self.FOO_MESSAGES_RECEIVED_LOG,
            self.ASSERT_MESSAGE_FOO_RECEIVED.format(amount = minimal_amount),
            extra_verification = verification
        )
        self.log_exit()

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
