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

class SdvVepsmSession:
    VEPSM_COMMAND = "vepsm {command}"

    def __init__(self, device_adb):
        self.interactive_session = device_adb.interactive_session()
        # Launch the client and wait for it to return
        self.command_and_wait_for_output("", "Enter VePSM command, press enter for help")

    def close(self):
        self.interactive_session.close()

    def command(self, command):
        self.interactive_session.send_command(self.VEPSM_COMMAND.format(command=command))

    def command_and_wait_for_output(self, command, expected_output, timeout=10):
        if not isinstance(expected_output, list):
            expected_output = [expected_output]

        self.interactive_session.send_command_and_wait_for_outputs(
            self.VEPSM_COMMAND.format(command=command),
            expected_output,
            timeout,
        )
