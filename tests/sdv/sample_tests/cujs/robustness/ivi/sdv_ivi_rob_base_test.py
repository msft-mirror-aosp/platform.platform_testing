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

"""Base class for SDV Robustness e2e tests."""

from mobly import asserts
import logging
import re
import threading
import time

import mobly.utils as utils
from sdv_sb_lifecycle_robustness import sdv_sb_lifecycle_robustness_base_test
from sdv_test_fw.device import sdv_adb
from sdv_test_fw.device.sdv_property import SdvDeviceProperty
from sdv_test_fw.verification import polling
from vpm.sdv_vpm import SdvVpm


class SdvIviRobBaseTest(
    sdv_sb_lifecycle_robustness_base_test.SdvSBLifecycleRobustnessTestBase,
):
    """Base class for SDV Robustness tests."""

    CVD_FLEET_COMMAND = "cvd fleet"
    RESTART_COMMAND = 'cvd -instance-name "{}" restart'
    START_COMMAND = 'cvd -instance-name "{}" start'
    IVI_SHUTDOWN_COMMAND = "reboot -p"
    FORCE_POWER_OFF_IVI_COMMAND = 'cvd -instance-name "{}" stop'
    POWER_BUTTON_IVI_COMMAND = 'cvd -instance-name "{}" powerbtn'
    OPEN_APP_CMD = "am start com.android.testapp.sdvcarmonitor/.MainActivity"
    CLOSE_APP_CMD = "am force-stop com.android.testapp.sdvcarmonitor"
    SUSPEND_IVI_SIGNAL_CMD = "cmd car_service suspend --simulate"
    SPSPEND_IVI_ACTION_CMD = "echo mem > /sys/power/state"
    RESUME_IVI_SIGNAL_CMD = "cmd car_service resume"
    DT_PUBLISHER_START_CMD = (
        "sdv_service_bundle start"
        " local-vm:com.sdv.google.sample.foo.ServiceBundleFoo/instance"
    )
    DT_PUBLISHER_STOP_CMD = (
        "sdv_service_bundle stop"
        " local-vm:com.sdv.google.sample.foo.ServiceBundleFoo/instance"
    )
    DT_SUBSCRIBER_START_CMD = (
        "sdv_service_bundle start"
        " local-vm:com.sdv.google.sample.bar.ServiceBundleBar/instance"
    )
    DT_SUBSCRIBER_STOP_CMD = (
        "sdv_service_bundle stop"
        " local-vm:com.sdv.google.sample.bar.ServiceBundleBar/instance"
    )
    KILLALL_IGNORE_ERROR_CMD = "killall {} || true"
    CLEAR_LOG_BUFFER_CMD = "logcat -c"
    SUSPEND_DURATION_S = 90
    SUSPEND_RESUME_BUFFER_DURATION_S = 30
    CAR_MONITOR_APP_LOG_TAG = "SdvCarMonitorTestApp"

    RECEIVED_MESSAGE_ON_IVI_WITH_VALUE = "foo message = {value}"
    ENABLE_SOMEIP_OBSERVER_CMD = (
        "am broadcast -a com.android.testapp.sdvcarmonitor.ACTION_HANDLE_EVENT"
        " --es 'OPERATION_TYPE' 'foo_message_use_observer'"
        " --es 'FOO_PUBLICATION_VARIANT' 'someip'"
    )
    ENABLE_SOMEIP_SUBSCRIBER_CMD = (
        "am broadcast -a com.android.testapp.sdvcarmonitor.ACTION_HANDLE_EVENT"
        " --es 'OPERATION_TYPE' 'foo_message_use_subscriber'"
        " --es 'FOO_PUBLICATION_VARIANT' 'someip'"
    )
    ENABLE_SUBSCRIBER_CMD = (
        "am broadcast -a com.android.testapp.sdvcarmonitor.ACTION_HANDLE_EVENT"
        " --es 'OPERATION_TYPE' 'foo_message_use_subscriber'"
    )
    RECEIVED_MESSAGE_ON_IVI = "foo message = 42"
    ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP_IVI = (
        "SdvCarMonitorTestApp did not receive Foo Message."
    )
    ERROR_MESSAGE_FOO_MESSAGE_UNEXPECTEDLY_RECEIVED_IVI = (
        "SdvCarMonitorTestApp unexpectedly received a Foo Message."
    )

    FOO_MESSAGE_VALUE_IVI_TO_CORE = 84
    PUBLISH_FOO_MESSAGE_CMD = (
        "am broadcast -a com.android.testapp.sdvcarmonitor.ACTION_HANDLE_EVENT"
        " --es 'OPERATION_TYPE' 'foo_message' --es 'OPERATION_VALUE' '{value}'"
    )
    START_CONTINUOUS_PUB_FOO_MESSAGE_CMD = (
        "am broadcast -a com.android.testapp.sdvcarmonitor.ACTION_HANDLE_EVENT"
        " --es 'OPERATION_TYPE' 'start_continuous_foo_message'"
    )
    STOP_APP_CMD = "am force-stop com.android.testapp.sdvcarmonitor"
    RECEIVED_MESSAGE_ON_CORE = (
        f"Received.*FooMessage.*{FOO_MESSAGE_VALUE_IVI_TO_CORE}"
    )

    ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP_CORE = (
        "ServiceBundleBar did not receive Foo Message from IVI."
    )
    ERROR_MESSAGE_FOO_MESSAGE_UNEXPECTEDLY_RECEIVED_CORE = (
        "ServiceBundleBar unexpectedly received a Foo Message from IVI."
    )

    # Logs for checking pubsub working
    DT_SUBSCRIBER_LOG_TAG = "sample_bar"
    SDVGATEWAY_CLIENT_LOG_TAG = "libsdvgatewayclient"

    def setup_class(self, ivi_device_name="device1", core_device_name="device2"):
        """Sets up the test class."""
        super().setup_class(
            foo_device_name=core_device_name, bar_baz_device_name=core_device_name
        )

        self.ivi_vm_device = self.get_device(ivi_device_name).adb()
        self.core_vm_device = self.get_device(core_device_name).adb()
        self.ivi_vm_instance_name = self.ivi_vm_device.prop.get(SdvDeviceProperty.INSTANCE_NAME)
        self.core_vm_instance_name = self.core_vm_device.prop.get(SdvDeviceProperty.INSTANCE_NAME)
        self.core_vm_vpm = SdvVpm(self.core_vm_device)

    def clear_ivi_logcat_buffer(self):
        self.ivi_vm_device.execute_shell_command(self.CLEAR_LOG_BUFFER_CMD)

    def clear_core_logcat_buffer(self):
        self.core_vm_device.execute_shell_command(self.CLEAR_LOG_BUFFER_CMD)

    def start_app_on_ivi(self):
        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)

    def close_app_on_ivi(self):
        self.ivi_vm_device.execute_shell_command(self.CLOSE_APP_CMD)

    def start_dt_publisher_on_core(self):
        self.core_vm_device.execute_shell_command(self.DT_PUBLISHER_START_CMD)

    def stop_dt_publisher_on_core(self):
        self.core_vm_device.execute_shell_command(self.DT_PUBLISHER_STOP_CMD)

    def start_dt_subscriber_on_core(self):
        self.core_vm_device.execute_shell_command(self.DT_SUBSCRIBER_START_CMD)

    def stop_dt_subscriber_on_core(self):
        self.core_vm_device.execute_shell_command(self.DT_SUBSCRIBER_STOP_CMD)

    def verify_core_pub_ivi_sub_work(self):
        self.clear_ivi_logcat_buffer()
        polling.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result=r"Foo message \(Observer\) received:",
        )

    def teardown_test(self):
        self.ivi_vm_device.execute_shell_command(self.CLOSE_APP_CMD + "|| true")
        self.stop_dt_publisher_on_core()

    def find_control_socket_name(self, file):
        # Getting the control socket for wake up (go/cf-s2r)
        control_socket_regexp = re.compile(
            r"--socket=([\S]+crosvm_control\.sock)$"
        )
        with open(file, "r") as fp:
            loglines = fp.readlines()
            for line in loglines:
                rematch = control_socket_regexp.search(line)
                if rematch is not None:
                    return rematch.group(1)

    def power_cycle_ivi_vm(self):
        """Power cycles the ivi VM using adb reboot."""
        self._power_cycle_vm(self.ivi_vm_device, "ivi")

    def power_cycle_core_vm(self):
        """Power cycles the core VM using adb reboot."""
        self._power_cycle_vm(self.core_vm_device, "core")

    def _power_cycle_vm(self, device, role: str):
        """Power cycles a VM using adb reboot."""
        device.log().info(
            f"Power cycling {role} VM {device} with 'adb reboot'."
        )
        device.reboot_device_and_verify_logcat()
        logging.info(f"{role.capitalize()} VM {device} is back online.")

    def graceful_shutdown_ivi_vm(self):
        """Gracefully shuts down the IVI VM adb command."""
        self.ivi_vm_device.log().info(
            "Gracefully shutting down IVI vm using ADB."
        )
        self.ivi_vm_device.execute_shell_command(self.IVI_SHUTDOWN_COMMAND)
        self.ivi_vm_device.wait_for_device_offline()

    def graceful_shutdown_core_vm(self):
        """Gracefully shuts down a VM using VPM."""
        self.core_vm_device.log().info(
            "Gracefully shutting down Core VM using VPM."
        )
        self.core_vm_vpm.shutdown_vm()
        self.core_vm_device.wait_for_device_offline()
        logging.info("CORE VM is offline.")

    def power_on_ivi_vm(self):
        """Powers on the IVI VM."""
        self._power_on_vm(self.ivi_vm_instance_name, self.ivi_vm_device, "ivi")
        self.ivi_vm_device.root_device()

    def power_on_core_vm(self):
        """Powers on the core VM."""
        self._power_on_vm(
            self.core_vm_instance_name, self.core_vm_device, "core"
        )
        self.core_vm_device.root_device()

    def _power_on_vm(self, instance_name: str, device, role: str):
        """Powers on a VM."""
        utils.run_command(
            self.RESTART_COMMAND.format(instance_name), shell=True
        )
        # Wait for device to be online.
        device.wait_for_device_online()
        device.log().info(f"Powering on {role} VM {instance_name}.")
        device.verify_logcat_is_running()
        device.log().info(
            f"{role.capitalize()} VM {instance_name} is back online."
        )

    def suspend_ivi_vm(self):
        """Suspends the IVI VM for a fixed duration."""
        self.ivi_vm_device.log().info(f"Suspending IVI VM.")
        self.ivi_vm_device.execute_shell_command(self.SUSPEND_IVI_SIGNAL_CMD)
        time.sleep(0.1)
        self.ivi_vm_device.execute_shell_command_in_subprocess(
            self.SPSPEND_IVI_ACTION_CMD, self.SPSPEND_IVI_ACTION_CMD
        )

    def suspend_core_vm(self):
        """Suspends the core VM for a fixed duration."""
        self.core_vm_device.log().info(
            f"Suspending core VM for {self.SUSPEND_DURATION_S} seconds."
        )
        self.core_vm_vpm.suspend_vm_for_time(str(self.SUSPEND_DURATION_S))

    def wait_for_ivi_resume(self):
        """Waits for the IVI VM to resume from suspension."""
        self.ivi_vm_device.log().info(f"Waiting for IVI VM to resume...")
        exit_code, out, err = utils.run_command(
            self.POWER_BUTTON_IVI_COMMAND.format(self.ivi_vm_instance_name),
            shell=True,
        )
        asserts.assert_equal(
            exit_code,
            0,
            "crosvm powerbtn failed with"
            f" status={exit_code}\nstdout={out}\nstderr={err}",
        )
        self.ivi_vm_device.wait_for_device_online()
        self.ivi_vm_device.execute_shell_command(self.RESUME_IVI_SIGNAL_CMD)

    def wait_for_core_resume(self):
        """Waits for the core VM to resume from suspension."""
        self.core_vm_device.log().info(f"Waiting for core VM to resume...")
        self.core_vm_vpm.wait_for_vm_to_resume_from_ram(
            timeout=self.SUSPEND_DURATION_S
            + self.SUSPEND_RESUME_BUFFER_DURATION_S
        )

    def _verify_message_reception(
        self,
        adb_device: sdv_adb.SdvAdb,
        log_tag: str,
        grep_text: str,
        assert_msg_received: str,
        assert_msg_not_received: str,
        expect_message: bool,
    ):
        """Verifies that a message was or was not received."""
        adb_device.clear_logcat()
        if expect_message:
            polling.wait_and_verify_expected_logs(
                sdv_device=adb_device,
                logcat_args=self.LOGCAT_ARGS.format(tag=log_tag),
                grep_text=grep_text,
                assert_msg=assert_msg_received,
            )
        else:
            time.sleep(self.WAIT_NO_MESSAGE_RECEIVED_IN_S)
            logcat_result = adb_device.grep_from_logcat(
                grep_text,
                logcat_args=self.LOGCAT_ARGS.format(tag=log_tag),
            )
            asserts.assert_false(
                logcat_result,
                assert_msg_not_received,
            )

    def verify_new_message_received_on_ivi_with_value(self, value):
        """Verifies that the IVI app has received a new message with the specified value."""
        self._verify_message_reception(
            adb_device=self.ivi_vm_device,
            log_tag=self.CAR_MONITOR_APP_LOG_TAG,
            grep_text=self.RECEIVED_MESSAGE_ON_IVI_WITH_VALUE.format(value=value),
            assert_msg_received=self.ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP_IVI,
            assert_msg_not_received=self.ERROR_MESSAGE_FOO_MESSAGE_UNEXPECTEDLY_RECEIVED_IVI,
            expect_message=True,
        )

    def verify_new_message_received_on_ivi(self):
        """Verifies that the IVI app has received a new message."""
        self._verify_message_reception(
            adb_device=self.ivi_vm_device,
            log_tag=self.CAR_MONITOR_APP_LOG_TAG,
            grep_text=self.RECEIVED_MESSAGE_ON_IVI,
            assert_msg_received=self.ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP_IVI,
            assert_msg_not_received=self.ERROR_MESSAGE_FOO_MESSAGE_UNEXPECTEDLY_RECEIVED_IVI,
            expect_message=True,
        )

    def verify_no_new_message_received_on_ivi(self):
        """Verifies that the IVI app has not received any new messages."""
        self._verify_message_reception(
            adb_device=self.ivi_vm_device,
            log_tag=self.CAR_MONITOR_APP_LOG_TAG,
            grep_text=self.RECEIVED_MESSAGE_ON_IVI,
            assert_msg_received=self.ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP_IVI,
            assert_msg_not_received=self.ERROR_MESSAGE_FOO_MESSAGE_UNEXPECTEDLY_RECEIVED_IVI,
            expect_message=False,
        )

    def verify_new_message_received_on_core(self, service_bundle):
        """Verifies that the CORE service bundle has received a new message from IVI."""
        self._verify_message_reception(
            adb_device=service_bundle.adb_device,
            log_tag=service_bundle.tag,
            grep_text=self.RECEIVED_MESSAGE_ON_CORE,
            assert_msg_received=self.ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP_CORE,
            assert_msg_not_received=self.ERROR_MESSAGE_FOO_MESSAGE_UNEXPECTEDLY_RECEIVED_CORE,
            expect_message=True,
        )

    def verify_no_new_message_received_on_core(self, service_bundle):
        """Verifies that the CORE service bundle has not received any new messages from IVI."""
        self._verify_message_reception(
            adb_device=service_bundle.adb_device,
            log_tag=service_bundle.tag,
            grep_text=self.RECEIVED_MESSAGE_ON_CORE,
            assert_msg_received=self.ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP_CORE,
            assert_msg_not_received=self.ERROR_MESSAGE_FOO_MESSAGE_UNEXPECTEDLY_RECEIVED_CORE,
            expect_message=False,
        )

    def _ivi_publish_loop(self, stop_event: threading.Event):
        """Continuously executes the publish command until the stop event is set.

        Runs in a separate thread.
        """
        logging.info("Starting continuous publishing loop...")
        while not stop_event.is_set():
            try:
                self.ivi_vm_device.execute_shell_command(
                    self.PUBLISH_FOO_MESSAGE_CMD.format(
                        value=self.FOO_MESSAGE_VALUE_IVI_TO_CORE
                    )
                )
                # Wait for 100ms before sending the next message
                time.sleep(0.1)
            except Exception as e:
                # This may happen if the connection is lost or the app is stopped.
                # We can log this and exit the loop gracefully.
                logging.info(f"Publishing failed, stopping loop: {e}")
                break
        logging.info("Continuous publishing loop stopped.")

    def _start_continuous_ivi_publishing(
        self,
    ) -> tuple[threading.Event, threading.Thread]:
        """Starts the continuous publishing loop in a background thread.

        :return: A tuple containing the stop event and the thread object.
        """
        stop_event = threading.Event()
        publishing_thread = threading.Thread(
            target=self._ivi_publish_loop, args=(stop_event,)
        )
        publishing_thread.start()
        return stop_event, publishing_thread

    def start_continuous_self_ivi_publishing(self):
        self.ivi_vm_device.execute_shell_command(
            self.START_CONTINUOUS_PUB_FOO_MESSAGE_CMD
        )

    def verify_ivi_publisher_functioning(self):
        self.ivi_vm_device.execute_shell_command(self.PUBLISH_FOO_MESSAGE_CMD)
        polling.wait_and_verify_expected_logs(
            sdv_device=self.core_vm_device,
            grep_text=self.DT_SUBSCRIBER_LOG_TAG,
            expected_result=r"sample: Received: x1 FooMessage\(s\)",
        )

    def wait_for_ivi_app_ready(self):
        polling.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.SDVGATEWAY_CLIENT_LOG_TAG,
            expected_result="Registered service unit, name = tire-pressure",
        )
        polling.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result="IntentReceiver registered.",
        )

    def wait_for_ivi_ready_from_suspend(self):
        polling.wait_and_verify_expected_logs(
            sdv_device=self.ivi_vm_device,
            grep_text=self.CAR_MONITOR_APP_LOG_TAG,
            expected_result="IntentReceiver registered.",
        )
