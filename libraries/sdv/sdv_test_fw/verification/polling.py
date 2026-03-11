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
import re
import time
from mobly import asserts


POLL_INTERVAL = 0.1
DEFAULT_TIMEOUT = 30


def wait_and_return_result(
    func, *args, timeout=DEFAULT_TIMEOUT, poll_interval=POLL_INTERVAL
):
    """Waits until a function returns a non-None value within a specified timeout.

    Repeatedly calls func every poll_interval until it returns a value that
    is not None or the timeout is reached.

    Args:
        func: The function to call.
        *args: arguments, passed to function
        timeout: The maximum time in seconds to wait for a non-None return.
          Avoid increasing this unless absolutely necessary
        poll_interval: The time in seconds to wait between consecutive calls to
          func. Consider increasing this for computationally expensive function
          to reduce resource consumption.

    Returns:
        The function's return value if result is not None within the
        timeout,
        or None if the timeout is reached.
    """

    start_time = time.time()
    logging.info("Start waiting for condition")
    while True:
        result = func(*args)
        logging.info(f"result={result}")

        if result is not None:
            return result

        if timeout is not None and time.time() - start_time > timeout:
            logging.info("Timeout reached")
            return None

        time.sleep(poll_interval)


def _poll_until_true(func, args, timeout, poll_interval):
    """Polls a function until it returns True or timeout expires.

    Args:
        func: The function to call.
        args: Arguments to pass to the function.
        timeout: Maximum time to wait in seconds.
        poll_interval: Time to sleep between calls in seconds.

    Returns:
        True if func returns True within timeout, False otherwise.
    """
    start_time = time.time()
    logging.info("Start waiting for condition")
    while time.time() - start_time < timeout:
        if func(*args):
            return True
        time.sleep(poll_interval)
    return False


def wait_for_true(
    func,
    *args,
    timeout=DEFAULT_TIMEOUT,
    assert_msg="Timeout for waiting is reached",
    poll_interval=POLL_INTERVAL,
):
    """Waits until a function returns True within a specified timeout.

    Repeatedly calls func every poll_interval until it returns True or the
    timeout is reached.

    Args:
        func: The function to call (function should return true/false).
        *args: arguments, passed to function.
        timeout: The maximum time in seconds to wait for a True return. Avoid
          increasing this unless absolutely necessary.
        assert_msg: Error message displayed when timeout is reached.
        poll_interval: The time in seconds to wait between consecutive calls to
          func. Consider increasing this for computationally expensive function
          to reduce resource consumption.

    Raises:
        assert.fail if the function does not return True within the timeout.

    Returns:
        True if the function provided returns True within the timeout
    """

    if _poll_until_true(func, args, timeout, poll_interval):
        return True

    asserts.fail(assert_msg)


def _grep_expected_result(
    sdv_device,
    grep_text,
    expected_result=None,
    logcat_args=None,
    grep_args=None,
):
    logcat_result = sdv_device.grep_from_logcat(
        grep_text, logcat_args, grep_args
    )
    logging.info(f"logcat_result={logcat_result}")
    if expected_result is not None:
        return re.search(expected_result, logcat_result) is not None
    else:
        return len(logcat_result) != 0


def wait_and_verify_expected_logs(
    sdv_device,
    grep_text,
    expected_result=None,
    logcat_args=None,
    grep_args=None,
    assert_msg="Message not found in logcat within timeout",
    poll_interval=POLL_INTERVAL,
    timeout=DEFAULT_TIMEOUT,
):
    """Polls the logcat output for a specific text until found or timeout.

    Args:
        sdv_device: SDV VM.
        grep_text: The text to search for in the logcat output.
        expected_result: Expected logs within the retrieved text.
        logcat_args: Arguments for logcat search.
        grep_args: Arguments for grep.
        assert_msg: Error message displayed when logs not found.
        poll_interval: The time in seconds to wait between consecutive calls to
          func. Consider increasing this for computationally expensive function
          to reduce resource consumption.
        timeout: The maximum time in seconds to wait for a non-None return.
          Avoid increasing this unless absolutely necessary.

    Returns:
        True if grep matched at least one logcat output
        False if grep matched no logcat output within the timeout
    """
    wait_for_true(
        _grep_expected_result,
        sdv_device,
        grep_text,
        expected_result,
        logcat_args,
        grep_args,
        timeout=timeout,
        assert_msg=assert_msg,
        poll_interval=poll_interval,
    )


# =================================================================
# Library Internal Methods
# =================================================================
# The following methods are for internal library use to raise Exceptions.
# Tests should use the public methods above for assertion handling.


def wait_for_true_or_raise_exception(
    func,
    *args,
    timeout=DEFAULT_TIMEOUT,
    exception_msg="Timeout for waiting is reached",
    exception_class=Exception,
    poll_interval=POLL_INTERVAL,
):
    """Waits until a function returns True within a specified timeout.

    Repeatedly calls func every poll_interval until it returns True or the
    timeout is reached.

    Args:
        func: The function to call (function should return true/false).
        *args: arguments, passed to function.
        timeout: The maximum time in seconds to wait for a True return. Avoid
          increasing this unless absolutely necessary.
        exception_msg: Error message displayed when timeout is reached.
        exception_class: The exception class to raise on timeout.
        poll_interval: The time in seconds to wait between consecutive calls to
          func. Consider increasing this for computationally expensive function
          to reduce resource consumption.

    Raises:
        exception_class if the function does not return True within the timeout.

    Returns:
        True if the function provided returns True within the timeout
    """
    if _poll_until_true(func, args, timeout, poll_interval):
        return True

    raise exception_class(exception_msg)
