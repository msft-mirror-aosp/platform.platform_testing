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

from contextlib import contextmanager
import logging
import unittest
from unittest import mock
from mobly import expects as mobly_expects
from mobly import records
from mobly import signals
from sdv_telemetry_test_execution import expects


@mock.patch.object(mobly_expects.recorder, "add_error")
class ExpectsTest(unittest.TestCase):

    @staticmethod
    @contextmanager
    def disable_logger():
        """Temporarily disable the logger.

        `atest` gets confused if log messages are interspersed with the test
        result summary, which causes it to think that the tests didn't pass.
        """
        logger = logging.getLogger()
        was_disabled = logger.disabled
        logger.disabled = True
        try:
            yield
        finally:
            logger.disabled = was_disabled

    def test_basic_expects_success(self, mock_add_error):
        obj = object()
        test_cases = [
            (expects.expect_true, (True,), {"msg": ""}),
            (expects.expect_false, (False,), {"msg": ""}),
            (expects.expect_equal, (1, 1), {}),
            (expects.expect_not_equal, (1, 2), {}),
            (expects.expect_almost_equal, (1.0, 1.0000001), {"places": 5}),
            (expects.expect_not_almost_equal, (1.0, 2.0), {}),
            (expects.expect_in, (1, [1, 2, 3]), {}),
            (expects.expect_not_in, (4, [1, 2, 3]), {}),
            (expects.expect_is, (obj, obj), {}),
            (expects.expect_is_not, (object(), object()), {}),
            (expects.expect_count_equal, ([1, 2, 3], [3, 2, 1]), {}),
            (expects.expect_less, (1, 2), {}),
            (expects.expect_less_equal, (1, 1), {}),
            (expects.expect_greater, (2, 1), {}),
            (expects.expect_greater_equal, (1, 1), {}),
            (expects.expect_is_none, (None,), {}),
            (expects.expect_is_not_none, (1,), {}),
            (expects.expect_is_instance, (1, int), {}),
            (expects.expect_not_is_instance, (1, str), {}),
            (expects.expect_regex, ("hello world", r"hello.*"), {}),
            (expects.expect_not_regex, ("hello world", r"goodbye.*"), {}),
        ]

        for func, args, kwargs in test_cases:
            with self.subTest(func=func.__name__, args=args, kwargs=kwargs):
                mock_add_error.reset_mock()
                with self.disable_logger():
                    func(*args, **kwargs)
                mock_add_error.assert_not_called()

    def test_basic_expects_failure(self, mock_add_error):
        obj = object()
        test_cases = [
            (expects.expect_true, (False,), {"msg": ""}),
            (expects.expect_false, (True,), {"msg": ""}),
            (expects.expect_equal, (1, 2), {}),
            (expects.expect_not_equal, (1, 1), {}),
            (expects.expect_almost_equal, (1.0, 2.0), {}),
            (expects.expect_not_almost_equal, (1.0, 1.0), {}),
            (expects.expect_in, (4, [1, 2, 3]), {}),
            (expects.expect_not_in, (1, [1, 2, 3]), {}),
            (expects.expect_is, (object(), object()), {}),
            (expects.expect_is_not, (obj, obj), {}),
            (expects.expect_count_equal, ([1, 2, 3], [1, 2]), {}),
            (expects.expect_less, (2, 1), {}),
            (expects.expect_less_equal, (2, 1), {}),
            (expects.expect_greater, (1, 2), {}),
            (expects.expect_greater_equal, (1, 2), {}),
            (expects.expect_is_none, (1,), {}),
            (expects.expect_is_not_none, (None,), {}),
            (expects.expect_is_instance, (1, str), {}),
            (expects.expect_not_is_instance, (1, int), {}),
            (expects.expect_regex, ("hello world", r"goodbye.*"), {}),
            (expects.expect_not_regex, ("hello world", r"hello.*"), {}),
        ]

        for func, args, kwargs in test_cases:
            with self.subTest(func=func.__name__, args=args, kwargs=kwargs):
                mock_add_error.reset_mock()
                with self.disable_logger():
                    func(*args, **kwargs)
                mock_add_error.assert_called_once()
                self.assertIsInstance(
                    mock_add_error.call_args.args[0], signals.TestFailure
                )

    def test_expect_no_raises_success(self, mock_add_error):
        with self.disable_logger():
            with expects.expect_no_raises("Should not raise exception"):
                pass
        mock_add_error.assert_not_called()

    def test_expect_no_raises_failure(self, mock_add_error):
        with self.disable_logger():
            with expects.expect_no_raises("Should not raise exception"):
                raise ValueError("Failure")
        mock_add_error.assert_called_once()
        self.assertIsInstance(
            mock_add_error.call_args.args[0], records.ExceptionRecord
        )


if __name__ == "__main__":
    unittest.main(verbosity=2)
