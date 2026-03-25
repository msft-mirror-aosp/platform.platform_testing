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

from sdv_test_fw.feature_flags import sdv_feature_flag_annotation
from mobly import signals

# Define a minimal TestClass that the decorator expects 'self' to be
class MockTestClass(unittest.TestCase):
    """A mock test class to be used with the decorator."""

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.feature_flags = None

    def mock_test_method(self):
        return "Test Executed"

class TestSkipIfFeatureDisabled(unittest.TestCase):

    def test_feature_enabled_runs_test(self):
        """Test that the decorated method runs when the feature is enabled."""

        mock_flags = mock.MagicMock()
        mock_flags.is_feature_enabled.return_value = True

        TEST_FEATURE_NAME = 'my_test_feature'
        decorated_method = sdv_feature_flag_annotation.skip_if_feature_disabled(TEST_FEATURE_NAME)(
            MockTestClass.mock_test_method
        )

        test_instance = MockTestClass()
        test_instance.feature_flags = mock_flags
        result = decorated_method(test_instance)

        self.assertEqual(result, "Test Executed")
        mock_flags.is_feature_enabled.assert_called_once_with(TEST_FEATURE_NAME)

    def test_feature_disabled_skips_test(self):
        """Test that the decorated method skips when the feature is disabled."""

        mock_flags = mock.MagicMock()
        mock_flags.is_feature_enabled.return_value = False

        TEST_FEATURE_NAME = 'my_disabled_feature'
        decorated_method = sdv_feature_flag_annotation.skip_if_feature_disabled(TEST_FEATURE_NAME)(
            MockTestClass.mock_test_method
        )

        test_instance = MockTestClass()
        test_instance.feature_flags = mock_flags
        expected_message = (
            f"Skipping test: feature '{TEST_FEATURE_NAME}' is disabled on the device."
        )

        with self.assertRaisesRegex(signals.TestSkip, expected_message):
            decorated_method(test_instance)

        mock_flags.is_feature_enabled.assert_called_once_with(TEST_FEATURE_NAME)

    def test_no_flags_initialized_skips_test(self):
        """Test that the decorated method skips when no flag source is initialized."""

        TEST_FEATURE_NAME = 'another_feature'
        decorated_method = sdv_feature_flag_annotation.skip_if_feature_disabled(TEST_FEATURE_NAME)(
            MockTestClass.mock_test_method
        )

        test_instance = MockTestClass()
        test_instance.feature_flags = None

        expected_message = (
            f"Skipping test: Feature flags not initialized in MockTestClass."
        )

        with self.assertRaisesRegex(signals.TestSkip, expected_message):
            decorated_method(test_instance)

if __name__ == '__main__':
    unittest.main()
