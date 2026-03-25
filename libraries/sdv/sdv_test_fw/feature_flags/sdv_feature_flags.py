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

"""SDV Feature Flags Helper"""

class SdvFeatureFlags:
    """Helper to read and store SDV feature flags."""

    def __init__(self, sdv_device):
        self.sdv_device = sdv_device
        # Structure: { 'flag_name': 'value' }
        self.flags = {}
        self.refresh()

    def refresh(self):
        """Reads flags from the device via ADB."""
        # Force a refresh of the flags map.
        self.flags.clear()
        out = self.sdv_device.adb().execute_shell_command(
            'aflags list | grep com.android.sdv'
        )

        # Output format is: namespace.flag value
        for line in out.splitlines():
            if not line.strip():
                continue
            try:
                line_parts = line.split()
                self.flags[line_parts[0]] = line_parts[1]
            except IndexError as e:
                self.sdv_device.adb().log().error(
                    f"Failed to parse flag line '{line}': {e}"
                )

    def get_flag(self, flag_name):
        """Returns the value of the flag, or None if not found."""
        return self.flags.get(flag_name)

    def is_feature_enabled(self, flag_name):
        """Returns True if the flag value is 'enabled'."""
        return self.get_flag(flag_name) == 'enabled'
