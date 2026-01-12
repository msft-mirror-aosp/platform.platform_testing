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
from unittest.mock import patch

from sdv_test_fw.feature_flags.sdv_feature_flag_annotation import skip_if_feature_disabled
from mobly import signals

# Define a minimal TestClass that the decorator expects 'self' to be
class MockTestClass(unittest.TestCase):
    """A mock test class to be used with the decorator."""

    def mock_test_method(self):
        return "Test Executed"

class TestSkipIfFeatureDisabled(unittest.TestCase):

    @patch('sdv_test_fw.feature_flags.sdv_feature_flag_annotation.SdvFeatureFlag')
    def test_feature_enabled_runs_test(self, MockSdvFeatureFlag):
        """Test that the decorated method runs when the feature is True."""

        # Setup the mock to return True
        mock_instance = MockSdvFeatureFlag.return_value
        mock_instance.get_flag_value.return_value = True

        TEST_FEATURE_NAME = 'my_test_feature'
        decorated_method = skip_if_feature_disabled(TEST_FEATURE_NAME)(
            MockTestClass.mock_test_method
        )

        test_instance = MockTestClass()
        result = decorated_method(test_instance)

        self.assertEqual(result, "Test Executed")

        MockSdvFeatureFlag.assert_called_once()
        mock_instance.get_flag_value.assert_called_once_with(TEST_FEATURE_NAME)


    @patch('sdv_test_fw.feature_flags.sdv_feature_flag_annotation.SdvFeatureFlag')
    def test_feature_disabled_skips_test(self, MockSdvFeatureFlag):
        """Test that the decorated method skips when the feature is disabled."""

        # Setup the mock to return a disabled state
        mock_instance = MockSdvFeatureFlag.return_value
        mock_instance.get_flag_value.return_value = False

        TEST_FEATURE_NAME = 'my_disabled_feature'
        decorated_method = skip_if_feature_disabled(TEST_FEATURE_NAME)(
            MockTestClass.mock_test_method
        )

        test_instance = MockTestClass()
        expected_message = (
            f"Skipping test: feature '{TEST_FEATURE_NAME}' is disabled in the config."
        )

        with self.assertRaisesRegex(signals.TestSkip, expected_message):
            decorated_method(test_instance)

        MockSdvFeatureFlag.assert_called_once()
        mock_instance.get_flag_value.assert_called_once_with(TEST_FEATURE_NAME)

    @patch('sdv_test_fw.feature_flags.sdv_feature_flag_annotation.SdvFeatureFlag')
    def test_feature_none_skips_test(self, MockSdvFeatureFlag):
        """Test that the decorated method skips when the feature returns None (e.g., not set)."""

        # Setup the mock to return None
        mock_instance = MockSdvFeatureFlag.return_value
        mock_instance.get_flag_value.return_value = None

        TEST_FEATURE_NAME = 'another_feature'
        decorated_method = skip_if_feature_disabled(TEST_FEATURE_NAME)(
            MockTestClass.mock_test_method
        )

        test_instance = MockTestClass()
        expected_message = (
            f"Skipping test: feature '{TEST_FEATURE_NAME}' is disabled in the config."
        )

        with self.assertRaisesRegex(signals.TestSkip, expected_message):
            decorated_method(test_instance)

if __name__ == '__main__':
    unittest.main()
