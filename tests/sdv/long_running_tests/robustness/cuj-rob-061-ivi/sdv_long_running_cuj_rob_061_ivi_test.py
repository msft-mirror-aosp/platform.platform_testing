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
SDV sample 'CUJ-Rob-061-IVI' test.
"""

from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_test_runner
from sdv_sb_lifecycle_robustness.sdv_sb_lifecycle_robustness_base_test import (
    LifecycleTransition,
)
from robustness import sdv_ivi_rob_base_test

ROBUSTNESS_REPETITIONS = 100


class SdvLongRunningCujRob061IviTest(
    sdv_ivi_rob_base_test.SdvIviRobBaseTest,
    parameterized.TestCase,
):
    """CUJ-Rob-061-IVI test."""

    ERROR_MESSAGE_FOO_MESSAGE_RECEIVE_GREP_CORE = (
        "ServiceBundleBar did not receive Foo Message from IVI."
    )
    ERROR_MESSAGE_FOO_MESSAGE_UNEXPECTEDLY_RECEIVED_CORE = (
        "ServiceBundleBar unexpectedly received a Foo Message from IVI."
    )

    @parameterized.parameters(range(ROBUSTNESS_REPETITIONS))
    def test_pubsub_after_sub_stop_core_publishes_ivi_receives(self, _number):
        self.log_enter()
        # GIVEN Foo is started.
        self.transition_bundle_lifecycle(
            self.foo_bundle, LifecycleTransition.STARTED
        )
        # AND GIVEN SdvCarMonitorTestApp is running.
        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)
        self.wait_for_ivi_app_foreground()

        # AND GIVEN IVI receives messages
        self.verify_new_message_received_on_ivi()

        # WHEN IVI App is stopped.
        self.close_app_on_ivi()

        # THEN IVI receives no new messages.
        self.verify_no_new_message_received_on_ivi()

        # AND WHEN IVI App is started again.
        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)
        self.wait_for_ivi_app_foreground()

        # THEN IVI receives new messages.
        self.verify_new_message_received_on_ivi()

        self.log_exit()

    @parameterized.parameters(range(ROBUSTNESS_REPETITIONS))
    def test_pubsub_after_sub_stop_ivi_publishes_core_receives(self, _number):
        self.log_enter()
        # GIVEN Bar is started (subscriber).
        self.transition_bundle_lifecycle(self.bar_bundle, LifecycleTransition.STARTED)

        # AND GIVEN SdvCarMonitorTestApp is running (publisher).
        self.ivi_vm_device.execute_shell_command(self.OPEN_APP_CMD)
        self.wait_for_ivi_app_ready()

        # WHEN IVI app publishes messages.
        self.start_continuous_self_ivi_publishing()

        # THEN Bar receives the messages.
        self.verify_new_message_received_on_core(self.bar_bundle)

        # WHEN the Bar is stopped.
        self.transition_bundle_lifecycle(
            self.bar_bundle, LifecycleTransition.STOPPED
        )

        # THEN Bar receives no new messages.
        self.verify_no_new_message_received_on_core(self.bar_bundle)

        # AND WHEN the Bar is started again.
        self.transition_bundle_lifecycle(
            self.bar_bundle, LifecycleTransition.STARTED
        )

        # THEN Bar receives new messages.
        self.verify_new_message_received_on_core(self.bar_bundle)

        self.log_exit()


if __name__ == "__main__":
    sdv_test_runner.run()
