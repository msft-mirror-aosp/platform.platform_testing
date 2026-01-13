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
SDV sample 'CUJ-Rob-051' test.

Test is based on CORE-CUJ catalog and samples.

Verifies:

 * Service bundle can be started/destroyed via LCM commands.
 * Cross-vm pub/sub stops when the publisher is destroyed.
 * Cross-vm pub/sub resumes when the publisher is restarted.
"""

from absl.testing import parameterized
from sdv_test_fw.test_execution import sdv_test_runner
from sdv_sb_lifecycle_robustness import sdv_sb_lifecycle_robustness_base_test
from sdv_sb_lifecycle_robustness.sdv_sb_lifecycle_robustness_base_test import (
    LifecycleTransition,
)

ROBUSTNESS_REPETITIONS = 100


class SdvSampleCujRob051Test(
    sdv_sb_lifecycle_robustness_base_test.SdvSBLifecycleRobustnessTestBase,
    parameterized.TestCase,
):
    @parameterized.parameters(range(ROBUSTNESS_REPETITIONS))
    def test_observe_after_pub_kill(self, _number):
        self.log_enter()
        # GIVEN Foo and Bar are started.
        self.transition_bundle_lifecycle(self.foo_bundle, LifecycleTransition.STARTED)
        self.transition_bundle_lifecycle(self.bar_bundle, LifecycleTransition.STARTED)

        # AND GIVEN Bar receives messages via observer.
        self.verify_new_message_received(self.bar_bundle)

        # WHEN Foo is destroyed.
        self.transition_bundle_lifecycle(self.foo_bundle, LifecycleTransition.DESTROYED)

        # THEN Bar receives no new messages.
        self.verify_no_new_message_received(self.bar_bundle)

        # AND WHEN Foo is started again.
        self.transition_bundle_lifecycle(self.foo_bundle, LifecycleTransition.STARTED)

        # THEN Bar receives new messages via observer.
        self.verify_new_message_received(self.bar_bundle)

        self.log_exit()


if __name__ == "__main__":
    sdv_test_runner.run()
