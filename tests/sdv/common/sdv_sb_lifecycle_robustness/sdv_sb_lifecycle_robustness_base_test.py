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
The base class defined here is specific to Service Bundle Lifecycle Robustness CUJs.

It's optimized for running fast: low configured intervals to achieve 100 runs within <10 minutes.
It's based on LCM commands as input for manipulating service bundle lifecycle, and uses LCM logs as assertion arguments - not Orchestration.
"""

from mobly import asserts
import logging
import time
from dataclasses import dataclass
from enum import Enum

from sdv_test_fw.device import sdv_adb
from sdv_test_fw.test_execution import sdv_base_test
from sdv_test_fw.verification import polling


@dataclass
class ServiceBundle:
    """Data class for service bundles."""

    adb_device: sdv_adb.SdvAdb
    name: str
    package: str
    fqin: str
    tag: str


class LifecycleTransition(Enum):
    """
    Represents the lifecycle transition of a service bundle.
    The default .value is the command, and .regex holds the logcat pattern.
    """

    def __new__(cls, command_value, regex_pattern):
        member = object.__new__(cls)
        member._value_ = command_value
        member.regex = regex_pattern
        return member

    STARTED = ("start", "is started$|is already started.")
    STOPPED = ("create", "(is stopped|is already created (stopped))")
    DESTROYED = ("destroy", "is (destroyed|not running)")


class SdvSBLifecycleRobustnessTestBase(sdv_base_test.SdvBaseTestClass):
    """Base test class for CUJ-ROB-SB scenarios."""

    # Logcat args
    LOGCAT_ARGS = "*:F {tag}:*"
    LOGCAT_LIFECYCLE_REGEX = "Service bundle.*{bundle_package}.*{bundle_name}.*{regex}"

    # Foo VM properties
    SYS_PROP_FOO_PUB_INTERVAL = "persist.com.sdv.google.sample.foo.pub_interval_ms"
    FOO_PUB_INTERVAL_IN_MS = 200

    # Bar VM properties
    SYS_PROP_BAZ_SUB_INTERVAL = "persist.com.sdv.google.sample.baz.sub_interval_ms"
    BAZ_SUB_INTERVAL_IN_MS = 100

    # Given the comms stack propagation delays used in other tests (500 ms), waiting
    # 1 s to ensure no message has been published seems like a reasonable time.
    WAIT_NO_MESSAGE_RECEIVED_IN_S = 1

    # Log messages
    SENT_MESSAGE = "Sent.*FooMessage.*42"
    RECEIVED_MESSAGE = "Received.*FooMessage.*42"

    # Error messages
    ERROR_LIFECYCLE_BUNDLE = (
        "Lifecycle manager did not report {bundle_name} to be {expected_transition}."
    )
    ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP = (
        "{service_bundle} did not receive Foo Message."
    )
    ERROR_MESSAGE_FOO_MESSAGE_UNEXPECTEDLY_RECEIVED = (
        "{service_bundle} unexpectedly received a Foo Message."
    )

    def setup_class(self, foo_device_name="device1", bar_baz_device_name="device2"):
        super().setup_class()

        # Setup Foo ServiceBundle
        foo_device = self.get_device(foo_device_name).adb()
        self.set_sys_property(
            foo_device,
            self.SYS_PROP_FOO_PUB_INTERVAL,
            self.FOO_PUB_INTERVAL_IN_MS,
        )

        # Defer the reboot after setting up bar_baz service bundle, as they are
        # pointing to the same device
        if foo_device_name != bar_baz_device_name:
            self.reboot_device(foo_device)

        self.foo_bundle = ServiceBundle(
            adb_device=foo_device,
            name="ServiceBundleFoo",
            package="com.sdv.google.sample.foo",
            fqin="local-vm:com.sdv.google.sample.foo.ServiceBundleFoo/instance",
            tag="com_sdv_google_sample_foo_ServiceBundleFoo_instance",
        )

        # Setup Bar ServiceBundle
        bar_baz_device = self.get_device(bar_baz_device_name).adb()
        self.set_sys_property(
            bar_baz_device,
            self.SYS_PROP_BAZ_SUB_INTERVAL,
            self.BAZ_SUB_INTERVAL_IN_MS,
        )
        self.reboot_device(bar_baz_device)
        self.bar_bundle = ServiceBundle(
            adb_device=bar_baz_device,
            name="ServiceBundleBar",
            package="com.sdv.google.sample.bar",
            fqin="local-vm:com.sdv.google.sample.bar.ServiceBundleBar/instance",
            tag="com_sdv_google_sample_bar_ServiceBundleBar_instance",
        )

        # Setup Baz ServiceBundle
        self.baz_bundle = ServiceBundle(
            adb_device=bar_baz_device,
            name="ServiceBundleBaz",
            package="com.sdv.google.sample.baz",
            fqin="local-vm:com.sdv.google.sample.baz.ServiceBundleBaz/instance",
            tag="com_sdv_google_sample_baz_ServiceBundleBaz_instance",
        )

    def set_sys_property(self, adb_device, sys_property, value):
        """Set the system property."""
        adb_device.execute_shell_command(f"setprop {sys_property} {value}")

    def reboot_device(self, adb_device):
        # Reboot to stop all non-configured service bundles
        adb_device.reboot_device_and_verify_logcat()

    def log_enter(self):
        """Unified logging enter test suit."""
        logging.info(
            f"{self.get_suite_name()} :: Start Test {self.current_test_info.name}"
        )

    def log_exit(self):
        """Unified logging exit test suit."""
        logging.info(
            f"{self.get_suite_name()} :: End Test {self.current_test_info.name}"
        )

    def transition_bundle_lifecycle(self, service_bundle, transition):
        """Transitions a service bundle to a new lifecycle state and verifies."""
        service_bundle.adb_device.execute_shell_command(
            f"sdv_service_bundle {transition.value} {service_bundle.fqin}"
        )
        self.verify_bundle_lifecycle(service_bundle, transition)
        # Once confirmed, clear logcat to ensure this transition isn't picked up again later.
        service_bundle.adb_device.clear_logcat()

    def verify_bundle_lifecycle(self, service_bundle, transition):
        """Verifies that a service bundle has reached a specific lifecycle state."""
        polling.wait_and_verify_expected_logs(
            service_bundle.adb_device,
            logcat_args=self.LOGCAT_ARGS.format(tag="lifecycle_manager"),
            grep_args="-E",
            grep_text=self.LOGCAT_LIFECYCLE_REGEX.format(
                bundle_package=service_bundle.package,
                bundle_name=service_bundle.name,
                regex=transition.regex,
            ),
            assert_msg=(
                self.ERROR_LIFECYCLE_BUNDLE.format(
                    bundle_name=service_bundle.name,
                    expected_transition=transition.name.lower(),
                ),
            ),
        )

    def verify_new_message_received(self, service_bundle):
        """Verifies that a service bundle has received a new message."""
        # Ensure no old messages are picked up
        service_bundle.adb_device.clear_logcat()
        # FooMessage received by service_bundle
        polling.wait_and_verify_expected_logs(
            sdv_device=service_bundle.adb_device,
            logcat_args=self.LOGCAT_ARGS.format(tag=service_bundle.tag),
            grep_text=self.RECEIVED_MESSAGE,
            assert_msg=self.ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP.format(
                service_bundle=service_bundle.name
            ),
        )

    def verify_no_new_message_received(self, service_bundle):
        """Verifies that a service bundle has not received any new messages."""
        # Ensure no old messages are picked up
        service_bundle.adb_device.clear_logcat()
        # No new FooMessage received by service_bundle
        time.sleep(self.WAIT_NO_MESSAGE_RECEIVED_IN_S)
        logcat_result = service_bundle.adb_device.grep_from_logcat(
            self.RECEIVED_MESSAGE,
            logcat_args=self.LOGCAT_ARGS.format(tag=service_bundle.tag),
        )

        # assert that the grepped logcat is empty
        asserts.assert_false(
            logcat_result,
            self.ERROR_MESSAGE_FOO_MESSAGE_UNEXPECTEDLY_RECEIVED.format(
                service_bundle=service_bundle.name
            ),
        )
