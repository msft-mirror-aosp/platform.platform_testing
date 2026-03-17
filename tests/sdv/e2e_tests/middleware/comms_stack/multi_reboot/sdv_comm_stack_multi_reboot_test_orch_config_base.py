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

"""SDV Reboot Test"""

import logging
from cuj22_common.sdv_cuj22_base import SdvCuj22Base
import time
import random

class SdvCommsStackMultiRebootTestOrchConfigBase(
    SdvCuj22Base
):
    def run_test_multi_reboot(self, number_of_server_device_reboots, number_of_client_device_reboots, number_of_concurrent_reboots_both_devices):
        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} started'
        )

        self.setup_cuj22_devices()

        for _ in range(number_of_server_device_reboots):
            time.sleep(random.uniform(0.0, 5.0))
            self.adb_device_server.reboot_device()
            self.adb_device_server.wait_for_device_online()
            self.verify_logcat_content()

        for _ in range(number_of_client_device_reboots):
            time.sleep(random.uniform(0.0, 5.0))
            self.adb_device_client.reboot_device()
            self.adb_device_client.wait_for_device_online()
            self.verify_logcat_content()

        for _ in range(number_of_concurrent_reboots_both_devices):
            time.sleep(random.uniform(0.0, 5.0))
            self.adb_device_server.execute_shell_command_in_subprocess('reboot_server', 'reboot')
            self.adb_device_client.execute_shell_command_in_subprocess('reboot_client', 'reboot')
            self.adb_device_server.wait_for_device_online()
            self.adb_device_client.wait_for_device_online()
            self.verify_logcat_content()

        logging.info(
            f'{self.get_suite_name()}#{self.current_test_info.name} ended'
        )

    def verify_logcat_content(self):
        self.verify_logs_pub_sub_foo_message()
        self.verify_increasing_occurrences_of_pub_sub_foo_message()
        self.verify_logs_rpc_foo_in_client()
        self.verify_increasing_occurrences_of_rpc_foo_in_client()
