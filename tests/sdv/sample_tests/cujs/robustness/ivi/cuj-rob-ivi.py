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

"""Sample test for SDV Robustness e2e tests."""

import logging
import time

from absl.testing import parameterized
from robustness import sdv_ivi_rob_base_test
from sdv_test_fw.test_execution import sdv_base_test, sdv_test_runner
from sdv_test_fw.waiting_methods.waiting_methods import WaitingMethods


SHUTDOWN_POWER_ON_DELAY_SECONDS = 60
SUSPEND_RESUME_DELAY_SECONDS = 15


class SdvIviRobCujTest(
    sdv_ivi_rob_base_test.SdvIviRobBaseTest, parameterized.TestCase
):

    def test_cuj_001_core_pub_ivi_sub(self):
        self.start_app_on_ivi()
        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

        self.power_cycle_core_vm()

        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

    def test_cuj_001_core_sub_ivi_pub(self):
        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_dt_subscriber_on_core()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

        self.power_cycle_ivi_vm()

        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

    def test_cuj_002_core_pub_ivi_sub(self):
        self.start_app_on_ivi()
        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

        self.graceful_shutdown_core_vm()
        time.sleep(SHUTDOWN_POWER_ON_DELAY_SECONDS)
        self.power_on_core_vm()

        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

    def test_cuj_002_core_sub_ivi_pub(self):
        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_dt_subscriber_on_core()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

        self.graceful_shutdown_ivi_vm()
        time.sleep(SHUTDOWN_POWER_ON_DELAY_SECONDS)
        self.power_on_ivi_vm()

        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

    def test_cuj_003_core_pub_ivi_sub(self):
        self.start_app_on_ivi()
        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

        self.power_cycle_ivi_vm()

        self.start_app_on_ivi()
        self.verify_new_message_received_on_ivi()

    def test_cuj_003_core_sub_ivi_pub(self):
        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_dt_subscriber_on_core()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

        self.power_cycle_core_vm()

        self.start_dt_subscriber_on_core()
        self.verify_ivi_publisher_functioning()

    def test_cuj_004_core_pub_ivi_sub(self):
        self.start_app_on_ivi()
        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

        self.graceful_shutdown_ivi_vm()
        time.sleep(SHUTDOWN_POWER_ON_DELAY_SECONDS)
        self.power_on_ivi_vm()

        self.start_app_on_ivi()
        self.verify_new_message_received_on_ivi()

    def test_cuj_004_core_sub_ivi_pub(self):
        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_dt_subscriber_on_core()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

        self.graceful_shutdown_core_vm()
        time.sleep(SHUTDOWN_POWER_ON_DELAY_SECONDS)
        self.power_on_core_vm()

        self.start_dt_subscriber_on_core()
        self.verify_ivi_publisher_functioning()

    def test_cuj_010_core_pub_ivi_sub(self):
        self.start_app_on_ivi()
        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

        self.suspend_ivi_vm()
        self.power_cycle_core_vm()
        self.wait_for_ivi_resume()

        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

    def test_cuj_010_core_sub_ivi_pub(self):
        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_dt_subscriber_on_core()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

        self.suspend_core_vm()
        self.power_cycle_ivi_vm()
        self.wait_for_core_resume()

        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

    def test_cuj_011_core_pub_ivi_sub(self):
        self.start_app_on_ivi()
        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

        self.suspend_ivi_vm()
        self.graceful_shutdown_core_vm()
        time.sleep(SUSPEND_RESUME_DELAY_SECONDS)
        self.wait_for_ivi_resume()
        self.power_on_core_vm()

        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

    def test_cuj_011_core_sub_ivi_pub(self):
        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_dt_subscriber_on_core()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

        self.suspend_core_vm()
        self.graceful_shutdown_ivi_vm()
        self.wait_for_core_resume()
        self.power_on_ivi_vm()

        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

    def test_cuj_012_core_pub_ivi_sub(self):
        self.start_app_on_ivi()
        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

        self.graceful_shutdown_core_vm()
        self.suspend_ivi_vm()
        time.sleep(SUSPEND_RESUME_DELAY_SECONDS)
        self.wait_for_ivi_resume()
        self.power_on_core_vm()

        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

    def test_cuj_012_core_sub_ivi_pub(self):
        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_dt_subscriber_on_core()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

        self.graceful_shutdown_ivi_vm()
        self.suspend_core_vm()
        self.wait_for_core_resume()
        self.power_on_ivi_vm()

        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

    def test_cuj_013_core_pub_ivi_sub(self):
        self.start_app_on_ivi()
        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

        self.graceful_shutdown_core_vm()
        self.suspend_ivi_vm()
        time.sleep(SUSPEND_RESUME_DELAY_SECONDS)
        self.power_on_core_vm()
        self.wait_for_ivi_resume()

        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

    def test_cuj_013_core_sub_ivi_pub(self):
        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_dt_subscriber_on_core()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

        self.graceful_shutdown_ivi_vm()
        self.suspend_core_vm()
        self.power_on_ivi_vm()
        self.wait_for_core_resume()

        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

    def test_cuj_020_core_pub_ivi_sub(self):
        self.start_app_on_ivi()
        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

        self.suspend_core_vm()
        self.power_cycle_ivi_vm()
        self.wait_for_core_resume()

        self.start_app_on_ivi()
        self.verify_new_message_received_on_ivi()

    def test_cuj_020_core_sub_ivi_pub(self):
        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_dt_subscriber_on_core()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

        self.suspend_ivi_vm()
        self.power_cycle_core_vm()
        self.wait_for_ivi_resume()

        self.start_dt_subscriber_on_core()
        self.verify_ivi_publisher_functioning()

    def test_cuj_021_core_pub_ivi_sub(self):
        self.start_app_on_ivi()
        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

        self.suspend_core_vm()
        self.graceful_shutdown_ivi_vm()
        self.wait_for_core_resume()
        self.power_on_ivi_vm()

        self.start_app_on_ivi()
        self.verify_new_message_received_on_ivi()

    def test_cuj_021_core_sub_ivi_pub(self):
        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_dt_subscriber_on_core()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

        self.suspend_ivi_vm()
        self.graceful_shutdown_core_vm()
        self.wait_for_ivi_resume()
        self.power_on_core_vm()

        self.start_dt_subscriber_on_core()
        self.verify_ivi_publisher_functioning()

    def test_cuj_022_core_pub_ivi_sub(self):
        self.start_app_on_ivi()
        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

        self.graceful_shutdown_ivi_vm()
        self.suspend_core_vm()
        self.wait_for_core_resume()
        self.power_on_ivi_vm()

        self.start_app_on_ivi()
        self.verify_new_message_received_on_ivi()

    def test_cuj_022_core_sub_ivi_pub(self):
        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_dt_subscriber_on_core()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

        self.graceful_shutdown_core_vm()
        self.suspend_ivi_vm()
        self.wait_for_ivi_resume()
        self.power_on_core_vm()

        self.start_dt_subscriber_on_core()
        self.verify_ivi_publisher_functioning()

    def test_cuj_023_core_pub_ivi_sub(self):
        self.start_app_on_ivi()
        self.start_dt_publisher_on_core()
        self.verify_new_message_received_on_ivi()

        self.graceful_shutdown_ivi_vm()
        self.suspend_core_vm()
        self.power_on_ivi_vm()
        self.wait_for_core_resume()

        self.start_app_on_ivi()
        self.verify_new_message_received_on_ivi()

    def test_cuj_023_core_sub_ivi_pub(self):
        self.start_app_on_ivi()
        self.wait_for_ivi_app_ready()
        self.start_dt_subscriber_on_core()
        self.start_continuous_self_ivi_publishing()
        self.verify_ivi_publisher_functioning()

        self.graceful_shutdown_core_vm()
        self.suspend_ivi_vm()
        self.power_on_core_vm()
        self.wait_for_ivi_resume()

        self.start_dt_subscriber_on_core()
        self.verify_ivi_publisher_functioning()


if __name__ == '__main__':
    sdv_test_runner.run()
