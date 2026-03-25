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

import unittest
from unittest import mock
from sdv_test_fw.feature_flags import sdv_feature_flags

class TestSdvFeatureFlags(unittest.TestCase):

    def setUp(self):
        # Mock the device and the ADB interface
        self.mock_device = mock.MagicMock()
        self.mock_adb = self.mock_device.adb.return_value

        # Sample output that aflags might return
        self.sample_output = (
            "com.android.sdv.feature_one enabled\n"
            "com.android.sdv.feature_two disabled\n"
        )

    def test_init_loads_flags_successfully(self):
        """Test that __init__ calls refresh and populates the dictionary."""
        self.mock_adb.execute_shell_command.return_value = self.sample_output

        flags_helper = sdv_feature_flags.SdvFeatureFlags(self.mock_device)

        # Verify ADB command execution
        self.mock_adb.execute_shell_command.assert_called_with('aflags list | grep com.android.sdv')

        # Verify internal dictionary state
        expected_flags = {
            "com.android.sdv.feature_one": "enabled",
            "com.android.sdv.feature_two": "disabled",
        }
        self.assertEqual(flags_helper.flags, expected_flags)

    def test_refresh_with_empty_output(self):
        """Test behavior when the ADB command returns nothing."""
        self.mock_adb.execute_shell_command.return_value = ""
        flags_helper = sdv_feature_flags.SdvFeatureFlags(self.mock_device)
        self.assertEqual(flags_helper.flags, {})

    def test_refresh_parsing_error(self):
        """Test that malformed lines are handled and logged without crashing."""
        # Line 2 is missing a value
        malformed_output = "com.android.sdv.valid enabled\ncom.android.sdv.invalid_line"
        self.mock_adb.execute_shell_command.return_value = malformed_output

        flags_helper = sdv_feature_flags.SdvFeatureFlags(self.mock_device)

        # Valid line should still be processed
        self.assertEqual(flags_helper.get_flag("com.android.sdv.valid"), "enabled")
        # Logger should have been called for the malformed line
        self.assertTrue(self.mock_adb.log().error.called)

    def test_get_flag(self):
        """Test basic flag retrieval."""
        self.mock_adb.execute_shell_command.return_value = self.sample_output
        flags_helper = sdv_feature_flags.SdvFeatureFlags(self.mock_device)

        self.assertEqual(flags_helper.get_flag("com.android.sdv.feature_one"), "enabled")
        self.assertEqual(flags_helper.get_flag("non.existent.flag"), None)

    def test_is_feature_enabled(self):
        """Test boolean check for 'enabled' status."""
        self.mock_adb.execute_shell_command.return_value = self.sample_output
        flags_helper = sdv_feature_flags.SdvFeatureFlags(self.mock_device)

        # Case: is enabled
        self.assertTrue(flags_helper.is_feature_enabled("com.android.sdv.feature_one"))
        # Case: is disabled
        self.assertFalse(flags_helper.is_feature_enabled("com.android.sdv.feature_two"))
        # Case: doesn't exist
        self.assertFalse(flags_helper.is_feature_enabled("missing.flag"))

if __name__ == '__main__':
    unittest.main()
