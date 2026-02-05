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
import time
import unittest
from unittest import mock

from mobly import asserts
from sdv_test_fw.verification import polling


class TestWaitForCondition(unittest.TestCase):

    def test_wait_for_result_success(self):

        def condition_func():
            # Simulate a successful condition
            return 1

        result = polling.wait_and_return_result(condition_func)
        self.assertEqual(result, 1)

    def test_wait_for_result_timeout(self):
        def condition_func():
            # Simulate a condition that never becomes True
            time.sleep(0.2)
            return None

        start_time = time.time()
        result = polling.wait_and_return_result(condition_func, timeout=0.5)
        end_time = time.time()
        self.assertIsNone(result)
        # Check that the timeout was respected
        self.assertGreaterEqual(end_time - start_time, 0.5)

    def test_wait_for_result_with_args(self):
        def condition_func(arg1, arg2):
            return arg1 + arg2

        result = polling.wait_and_return_result(condition_func, 2, 3)
        self.assertEqual(result, 5)

    def test_wait_for_result_eventually_true(self):
        counter = 0

        def condition_func():
            nonlocal counter
            counter += 1
            if counter >= 3:
                return True
            return None

        result = polling.wait_and_return_result(condition_func, timeout=5)
        self.assertTrue(result)

    @mock.patch('time.sleep')
    def test_wait_and_return_result_respects_poll_interval_after_first_poll(
        self, mock_sleep
    ):
        first_call = True

        def condition_func():
            nonlocal first_call
            if first_call:
                first_call = False
                return None
            return 'done'

        result = polling.wait_and_return_result(
            condition_func, poll_interval=0.2
        )

        self.assertEqual(result, 'done')
        mock_sleep.assert_called_once_with(0.2)

    def test_wait_for_true_success(self):
        def test_function():
            return True

        self.assertTrue(polling.wait_for_true(test_function))

    def test_wait_for_true_default_assert_message(self):
        def test_function():
            return False

        with self.assertRaisesRegex(
            asserts.signals.TestFailure, 'Timeout for waiting is reached'
        ):
            polling.wait_for_true(test_function, timeout=1)

    def test_wait_for_true_custom_assert_message(self):
        def test_function():
            return False

        custom_message = 'Custom failure message'
        with self.assertRaisesRegex(
            asserts.signals.TestFailure, custom_message
        ):
            polling.wait_for_true(
                test_function, timeout=1, assert_msg=custom_message
            )

    def test_wait_for_true_with_args(self):
        def test_function_with_args(arg1, arg2):
            return arg1 == arg2

        self.assertTrue(polling.wait_for_true(test_function_with_args, 5, 5))
        with self.assertRaises(asserts.signals.TestFailure):
            polling.wait_for_true(test_function_with_args, 5, 6, timeout=1)

    @mock.patch('time.time')
    @mock.patch('time.sleep')
    def test_wait_for_true_timeout(self, mock_sleep, mock_time):

        mock_time.side_effect = [
            0,
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            12,
            13,
            14,
            15,
            16,
            17,
            18,
            19,
            20,
            21,
            22,
            23,
            24,
            25,
            26,
            27,
            28,
            29,
            30,
            31,
        ]

        def test_function():
            return False

        with self.assertRaises(asserts.signals.TestFailure):
            polling.wait_for_true(test_function, timeout=30)

        mock_sleep.assert_called()

    @mock.patch('time.time')
    @mock.patch('time.sleep')
    def test_wait_for_true_success_after_few_tries(self, mock_sleep, mock_time):

        mock_time.side_effect = [
            0,
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            12,
            13,
            14,
            15,
            16,
            17,
            18,
            19,
            20,
            21,
            22,
            23,
            24,
            25,
            26,
            27,
            28,
            29,
            30,
            31,
        ]

        call_count = 0

        def test_function():
            nonlocal call_count
            call_count += 1
            if call_count > 5:
                return True
            return False

        self.assertTrue(polling.wait_for_true(test_function, timeout=30))
        mock_sleep.assert_called()

    @mock.patch('time.sleep')
    def test_wait_for_true_respects_poll_interval(self, mock_sleep):
        first_call = True

        def condition_func():
            nonlocal first_call
            if first_call:
                first_call = False
                return False
            return True

        self.assertTrue(
            polling.wait_for_true(condition_func, poll_interval=0.2)
        )
        mock_sleep.assert_called_once_with(0.2)


if __name__ == '__main__':
    unittest.main()
