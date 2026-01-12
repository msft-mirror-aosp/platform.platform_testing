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

from cuj22_common.sdv_cuj22_base import SdvCuj22Base

SETPROP_PERSIST_ORCH_FOO_SERVER = 'setprop persist.sdv.orchestrator_config_path "etc/orch/vm_foo_orch_config.textproto"'
SETPROP_PERSIST_ORCH_BAR_CLIENT = 'setprop persist.sdv.orchestrator_config_path "etc/orch/vm_bar_orch_config.textproto"'


class SdvCuj22BaseHW(SdvCuj22Base):
    SERVER_DEVICE_QNX_ID = 1
    CLIENT_DEVICE_QNX_ID = 2

    def setup_cuj22_devices_hw(self):
        self.adb_device_server = self.get_device('device1').adb()
        self.adb_device_client = self.get_device('device2').adb()

        self.adb_device_server.execute_shell_command(
            SETPROP_PERSIST_ORCH_FOO_SERVER
        )
        self.adb_device_server.reboot_device_and_verify_logcat()

        self.adb_device_client.execute_shell_command(
            SETPROP_PERSIST_ORCH_BAR_CLIENT
        )
        self.adb_device_client.reboot_device_and_verify_logcat()

    def verify_logcat_content_suspend_resume_tests(self):
        self.verify_logs_pub_sub_foo_message()
        self.verify_increasing_occurrences_of_pub_sub_foo_message()
        self.verify_logs_rpc_foo_in_client()
        self.verify_increasing_occurrences_of_rpc_foo_in_client()
