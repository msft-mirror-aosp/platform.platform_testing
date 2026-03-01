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

class UpdateManagerClient:
    def __init__(self, adb):
        self.adb = adb
        self.UPDATE_CLIENT_PATH = "/apex/com.android.sdv.sample.update_manager.client/bin/sdv_update_manager_client"

    def command(self, command: str) -> str:
        return self.adb.execute_shell_command(f"{self.UPDATE_CLIENT_PATH} {command}")

    def status(self) -> str:
        return self.command("status")

    def prepare_service_bundle_update(self, apex_paths, boot_attempts) -> str:
        apex_path_arg = " ".join([f"--apex_path {a}" for a in apex_paths])
        return self.command(f"prepare service-bundle --boot_attempts {boot_attempts} {apex_path_arg}")

    def prepare_system_update(self, prepare_args, skip_unsubscribe) -> str:
        command = f"prepare system {prepare_args}"

        if skip_unsubscribe:
            command = "--skip-unsubscribe " + command

        return self.command(command)

    def activate(self) -> str:
        return self.command("activate")

    def rollback(self) -> str:
        return self.command("rollback")

    def commit(self) -> str:
        return self.command("commit")

    def uninstall_apex(self, apexes) -> str:
        if not isinstance(apexes, list):
            apexes = [apexes]
        apex_args = " ".join([f"--apex {a}" for a in apexes])
        self.command(f"uninstall-apex {apex_args}")

    def resume(self) -> str:
        return self.command("resume")
