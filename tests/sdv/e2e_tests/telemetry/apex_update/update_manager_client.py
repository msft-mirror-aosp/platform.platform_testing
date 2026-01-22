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

from itertools import chain
from pathlib import Path
from typing import List

from sdv_telemetry_test_execution.telemetry_utils import shlex_join


class UpdateManagerClient:
    UPDATE_CLIENT_PATH = "/apex/com.sdv.google.sample.update_manager.client/bin/sdv_update_manager_client"

    def __init__(self, adb):
        self.adb = adb

    def command(self, *args: List[str]) -> str:
        return self.adb.execute_shell_command(
            shlex_join([self.UPDATE_CLIENT_PATH, *args])
        )

    def status(self) -> str:
        return self.command("status")

    def prepare_service_bundle_update(
        self, apex_paths: List[Path], boot_attempts: int
    ) -> str:
        return self.command(
            "prepare",
            "service-bundle",
            "--boot_attempts",
            str(boot_attempts),
            *chain.from_iterable(
                [["--apex_path", str(apex_path)] for apex_path in apex_paths]
            )
        )

    def activate(self) -> str:
        return self.command("activate")

    def rollback(self) -> str:
        return self.command("rollback")
