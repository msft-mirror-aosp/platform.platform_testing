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
TODO(b/402089309): Migrate the original test and deprecate this test when:
    * adb can be enabled on user build - b/383592250
    * adb and orch configs can be passed by test config to CI/CD - b/403259181

SDV e2e user build basic test.

The test is based on CORE-CUJ catalog and 'CUJ-Core-22' sample.

Verifies:

 * SDV orchestration on VM start-up.
 * Cross-vm receiving message.
 * Cross-vm SDV RPC call request and response.
 * Cross-vm ACLs sharing.
 * Authorization enforcement for Service Discovery, Data Tunnel and RPC server.
"""

from mobly import asserts
import logging
import time

from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner

class SdvCujCore22UserBuildTest(sdv_base_test.SdvBaseTestClass, parameterized.TestCase):

    LOGCAT_ARGS = '*:F com_sdv_google_sample_foo_ServiceBundleFoo_instance:* com_sdv_google_sample_bar_ServiceBundleBar_instance:* SdvServiceManagerServer:*'

    # Test Parameters - logcat messages.
    RECEIVED_MESSAGE = 'Received.*FooMessage.*42'
    RPC_REQUEST_RESPONSE = 'Request.*GetFooRequest.*2024.*Response.*GetFooResponse.*2024'

    # Test Parameters - error messages.
    ERROR_MESSAGE_FOO_MESSAGE_GREP = \
        '\n[FAILURE]: Service Bundle expected to send/receive Foo Message.'
    ERROR_MESSAGE_FOO_RPC_GREP = \
        '\n[FAILURE]: Service Bundle expected to call Foo RPC.'

    ################################################
    ##            Utility functions.              ##
    ################################################

    def log_enter(self):
        """ Unified logging enter test suit. """
        logging.info(f"{self.get_suite_name()} :: Start test {self.current_test_info.name}")

    def log_exit(self):
        """ Unified logging exit test suit. """
        logging.info(f"{self.get_suite_name()} :: Finished test {self.current_test_info.name}")

    # TODO(b/382058513): migrate to framework implementation `wait_for_logcat` when ready.
    def wait_for_logcat(self, sdv_device, grep_text, timeout=30, poll_interval=0.1):
        """Polls the logcat output for a specific text until found or timeout.

        Args:
            grep_text: The text to search for in the logcat output.
            timeout: The maximum time (in seconds) to wait.
            poll_interval: The time (in seconds) between polls.
        Returns:
            True if grep matched at least one logcat output
            False if grep matched no logcat output within the timeout
        """
        deadline = time.perf_counter() + timeout
        while time.perf_counter() < deadline:
            logcat_grep_result = sdv_device.grep_from_logcat(
                grep = grep_text,
                logcat_args = self.LOGCAT_ARGS)
            if logcat_grep_result != "":
                return True
            time.sleep(poll_interval)
        return False

    ################################################
    ##           Assert-like validators.          ##
    ################################################

    def assert_logcat(self, sdv_device, grep_text, assert_msg):
        """Assert-like validator to grep logcat with the `grep_text`.

        Args:
            grep_text: The text to search for in the logcat output.
            assert_msg: The failure assertion message.
        """
        if not self.wait_for_logcat(sdv_device, grep_text):
            logcat_grep_result = sdv_device.grep_from_logcat(grep_text, self.LOGCAT_ARGS)
            asserts.assert_in(
                grep_text,
                logcat_grep_result,
                assert_msg + "\n {logcat_grep_result}"
            )


    ##################################################
    ## Setup/teardown for the test suite and tests. ##
    ##################################################

    def setup_class(self):
        super().setup_class()
        # Get "proxy VM" device which is used for validating the "VM under test" behavior.
        self.bar_adb_devices = self.get_device('device1').adb()

    #############################################################################
    ## Test Pub/Sub.                                                           ##
    ## Test explicitly verifies Bar service bundle subscription on "proxy VM". ##
    ##                                                                         ##
    ## Such behavior implicitly verifies that on the "VM under test":          ##
    ## * Foo service bundle was started by orchestrator configuration,         ##
    ## * Foo service bundle correctly implemented the Service Bundle API,      ##
    ## * Foo metadata was correctly propagated from Service Bundle Registry to ##
    ##    * SDV Lifecycle Agent.                                               ##
    ##    * SDV Orchestrator Agent.                                            ##
    ##    * SDV Authz.                                                         ##
    ## * Foo service bundle registered the Foo topic in SDV Service Discovery, ##
    ## * Foo topic has a valid authz configuration,                            ##
    ## * Cross-vm ACLs were shared.                                            ##
    ## * Foo message was successfully published.                               ##
    #############################################################################

    def test_pub_sub_foo_message(self):
        self.log_enter()
        # FooMessage received by ServiceBundleBar
        self.assert_logcat(
            sdv_device = self.bar_adb_devices,
            grep_text = self.RECEIVED_MESSAGE,
            assert_msg = self.ERROR_MESSAGE_FOO_MESSAGE_GREP,
        )
        self.log_exit()

    #############################################################################
    ## Test RPC.                                                               ##
    ## Test explicitly verifies Bar service bundle RPC call on "proxy VM".     ##
    ##                                                                         ##
    ## Such behavior implicitly verifies that on the "VM under test":          ##
    ## * Foo service bundle was started by orchestrator configuration,         ##
    ## * Foo service bundle correctly implemented the Service Bundle API,      ##
    ## * Foo metadata was correctly propagated from Service Bundle Registry to ##
    ##    * SDV Lifecycle Agent.                                               ##
    ##    * SDV Orchestrator Agent.                                            ##
    ##    * SDV Authz.                                                         ##
    ## * FooRPC was registered in SDV Service Discovery.                       ##
    ## * FooRPC has a valid authz configuration.                               ##
    ## * Cross-vm ACLs were shared.                                            ##
    ## * GetFooRequest was successfully received and GetFooResponse was sent.  ##
    #############################################################################

    def test_rpc_foo(self):
        self.log_enter()
        # FooRPC::foo(GetFooRequest) and GetFooResponse are received.
        self.assert_logcat(
            sdv_device = self.bar_adb_devices,
            grep_text = self.RPC_REQUEST_RESPONSE,
            assert_msg = self.ERROR_MESSAGE_FOO_RPC_GREP,
        )
        self.log_exit()

if __name__ == '__main__':
    # Start Test Execution Using SDV Test Framework ( STF )
    sdv_test_runner.run()
