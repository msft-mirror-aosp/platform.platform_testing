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

import logging
import unittest
from unittest import mock

from sdv_perf_common import timer


class TimerTest(unittest.TestCase):
    @mock.patch('time.time')
    @mock.patch('logging.info')
    def test_log_exec_time_logs_execution_time(self, mock_logging_info, mock_time):
        """Tests that the decorator logs the start and end of execution."""
        # Arrange
        start_time = 100.0
        duration = 2.5
        end_time = start_time + duration
        mock_time.side_effect = [start_time, end_time]

        @timer.log_exec_time
        def sample_function():
            return 'done'

        # Act
        result = sample_function()

        # Assert
        self.assertEqual(result, 'done')
        self.assertEqual(mock_logging_info.call_count, 2)
        mock_logging_info.assert_any_call(
            "Executing 'sample_function' to compute metrics..."
        )
        mock_logging_info.assert_any_call(
            "Finished executing 'sample_function' to compute metrics after"
            f" '{duration:.2f}' seconds."
        )

    @mock.patch('time.time')
    @mock.patch('logging.warning')
    @mock.patch('logging.info')
    def test_log_exec_time_logs_warning_when_threshold_exceeded(
        self, mock_logging_info, mock_logging_warning, mock_time
    ):
        """Tests that a warning is logged if the execution time is too long."""
        # Arrange
        duration = timer.DEFAULT_METRIC_COMPUTATION_WARNING_THRESHOLD_SECONDS + 1
        mock_time.side_effect = [
            100.0,
            100.0 + duration,
        ]

        @timer.log_exec_time
        def long_running_function():
            pass

        # Act
        long_running_function()

        # Assert
        mock_logging_warning.assert_called_once()
        self.assertIn('exceeded', mock_logging_warning.call_args[0][0])

    @mock.patch('time.time')
    @mock.patch('logging.warning')
    @mock.patch('logging.info')
    def test_log_exec_time_no_warning_when_threshold_not_exceeded(
        self, mock_logging_info, mock_logging_warning, mock_time
    ):
        """Tests that no warning is logged if the execution time is normal."""
        # Arrange
        duration = timer.DEFAULT_METRIC_COMPUTATION_WARNING_THRESHOLD_SECONDS - 1
        mock_time.side_effect = [
            100.0,
            100.0 + duration,
        ]

        @timer.log_exec_time
        def short_running_function():
            pass

        # Act
        short_running_function()

        # Assert
        mock_logging_warning.assert_not_called()

    @mock.patch('logging.warning')
    @mock.patch('logging.info')
    def test_log_exec_time_with_args_and_kwargs(
        self, mock_logging_info, mock_logging_warning
    ):
        """Tests that the decorator works with functions that have arguments."""

        @timer.log_exec_time
        def function_with_args(a, b, c=None):
            return a + b if c is None else a + b + c

        self.assertEqual(function_with_args(1, 2), 3)
        self.assertEqual(function_with_args(1, 2, c=3), 6)

    @mock.patch('time.time')
    @mock.patch('logging.warning')
    @mock.patch('logging.info')
    def test_log_exec_time_custom_threshold_exceeded(
        self, mock_logging_info, mock_logging_warning, mock_time
    ):
        """Tests the decorator with a custom threshold that is exceeded."""
        # Arrange
        custom_threshold = 5
        duration = custom_threshold + 1
        mock_time.side_effect = [100.0, 100.0 + duration]

        @timer.log_exec_time(threshold=custom_threshold)
        def sample_function():
            pass

        # Act
        sample_function()

        # Assert
        mock_logging_warning.assert_called_once()
        warning_message = mock_logging_warning.call_args[0][0]
        self.assertIn('exceeded', warning_message)
        self.assertIn(str(custom_threshold), warning_message)

    @mock.patch('time.time')
    @mock.patch('logging.warning')
    @mock.patch('logging.info')
    def test_log_exec_time_custom_threshold_not_exceeded(
        self, mock_logging_info, mock_logging_warning, mock_time
    ):
        """Tests the decorator with a custom threshold that is not exceeded."""
        # Arrange
        custom_threshold = 15
        duration = custom_threshold - 1
        mock_time.side_effect = [100.0, 100.0 + duration]

        @timer.log_exec_time(threshold=custom_threshold)
        def sample_function():
            pass

        # Act
        sample_function()

        # Assert
        mock_logging_warning.assert_not_called()


if __name__ == '__main__':
    unittest.main()
