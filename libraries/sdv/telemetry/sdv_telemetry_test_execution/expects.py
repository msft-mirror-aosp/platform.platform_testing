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
from typing import Any, Iterable, Optional, Pattern, Union

from mobly import asserts
from mobly import expects
from mobly import signals

# Mobly's `expects` module currently only defines these 4 functions, which we
# simply re-export here.
expect_true = expects.expect_true
expect_false = expects.expect_false
expect_equal = expects.expect_equal
expect_no_raises = expects.expect_no_raises


def expect_not_equal(
    first: Any, second: Any, msg: Optional[str] = None, extras: Any = None
) -> None:
    """Expects that first is not equal to second."""
    try:
        asserts.assert_not_equal(first, second, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to not equal %r, but they are equal.', first, second
        )
        expects.recorder.add_error(e)


def expect_almost_equal(
    first: Any,
    second: Any,
    places: Optional[int] = None,
    msg: Optional[str] = None,
    delta: Optional[float] = None,
    extras: Any = None,
) -> None:
    """Expects that first is almost equal to second."""
    try:
        asserts.assert_almost_equal(
            first, second, places=places, msg=msg, delta=delta, extras=extras
        )
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to be almost equal to %r, but they are not.',
            first,
            second,
        )
        expects.recorder.add_error(e)


def expect_not_almost_equal(
    first: Any,
    second: Any,
    places: Optional[int] = None,
    msg: Optional[str] = None,
    delta: Optional[float] = None,
    extras: Any = None,
) -> None:
    """Expects that first is not almost equal to second."""
    try:
        asserts.assert_not_almost_equal(
            first, second, places=places, msg=msg, delta=delta, extras=extras
        )
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to not be almost equal to %r, but they are.',
            first,
            second,
        )
        expects.recorder.add_error(e)


def expect_in(
    member: Any,
    container: Iterable[Any],
    msg: Optional[str] = None,
    extras: Any = None,
) -> None:
    """Expects that member is in container."""
    try:
        asserts.assert_in(member, container, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to be in %r, but it is not.', member, container
        )
        expects.recorder.add_error(e)


def expect_not_in(
    member: Any,
    container: Iterable[Any],
    msg: Optional[str] = None,
    extras: Any = None,
) -> None:
    """Expects that member is not in container."""
    try:
        asserts.assert_not_in(member, container, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to not be in %r, but it is.', member, container
        )
        expects.recorder.add_error(e)


def expect_is(
    expr1: Any, expr2: Any, msg: Optional[str] = None, extras: Any = None
) -> None:
    """Expects that expr1 is expr2."""
    try:
        asserts.assert_is(expr1, expr2, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to be identical to %r, but it is not.', expr1, expr2
        )
        expects.recorder.add_error(e)


def expect_is_not(
    expr1: Any, expr2: Any, msg: Optional[str] = None, extras: Any = None
) -> None:
    """Expects that expr1 is not expr2."""
    try:
        asserts.assert_is_not(expr1, expr2, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to not be identical to %r, but it is.', expr1, expr2
        )
        expects.recorder.add_error(e)


def expect_count_equal(
    first: Iterable[Any],
    second: Iterable[Any],
    msg: Optional[str] = None,
    extras: Any = None,
) -> None:
    """Expects that two iterables have the same elements, without regard to order."""
    try:
        asserts.assert_count_equal(first, second, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r and %r to have the same elements, but they do not.',
            first,
            second,
        )
        expects.recorder.add_error(e)


def expect_less(
    a: Any, b: Any, msg: Optional[str] = None, extras: Any = None
) -> None:
    """Expects that a < b."""
    try:
        asserts.assert_less(a, b, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to be less than %r, but it is not.', a, b
        )
        expects.recorder.add_error(e)


def expect_less_equal(
    a: Any, b: Any, msg: Optional[str] = None, extras: Any = None
) -> None:
    """Expects that a <= b."""
    try:
        asserts.assert_less_equal(a, b, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to be less than or equal to %r, but it is not.', a, b
        )
        expects.recorder.add_error(e)


def expect_greater(
    a: Any, b: Any, msg: Optional[str] = None, extras: Any = None
) -> None:
    """Expects that a > b."""
    try:
        asserts.assert_greater(a, b, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to be greater than %r, but it is not.', a, b
        )
        expects.recorder.add_error(e)


def expect_greater_equal(
    a: Any, b: Any, msg: Optional[str] = None, extras: Any = None
) -> None:
    """Expects that a >= b."""
    try:
        asserts.assert_greater_equal(a, b, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to be greater than or equal to %r, but it is not.',
            a,
            b,
        )
        expects.recorder.add_error(e)


def expect_is_none(
    obj: Any, msg: Optional[str] = None, extras: Any = None
) -> None:
    """Expects that obj is None."""
    try:
        asserts.assert_is_none(obj, msg, extras)
    except signals.TestSignal as e:
        logging.exception('Expected %r to be None, but it is not.', obj)
        expects.recorder.add_error(e)


def expect_is_not_none(
    obj: Any, msg: Optional[str] = None, extras: Any = None
) -> None:
    """Expects that obj is not None."""
    try:
        asserts.assert_is_not_none(obj, msg, extras)
    except signals.TestSignal as e:
        logging.exception('Expected %r to not be None, but it is.', obj)
        expects.recorder.add_error(e)


def expect_is_instance(
    obj: Any,
    cls: Union[type, tuple[type, ...]],
    msg: Optional[str] = None,
    extras: Any = None,
) -> None:
    """Expects that obj is an instance of cls."""
    try:
        asserts.assert_is_instance(obj, cls, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to be an instance of %r, but it is not.', obj, cls
        )
        expects.recorder.add_error(e)


def expect_not_is_instance(
    obj: Any,
    cls: Union[type, tuple[type, ...]],
    msg: Optional[str] = None,
    extras: Any = None,
) -> None:
    """Expects that obj is not an instance of cls."""
    try:
        asserts.assert_not_is_instance(obj, cls, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to not be an instance of %r, but it is.', obj, cls
        )
        expects.recorder.add_error(e)


def expect_regex(
    text: str,
    expected_regex: Union[str, Pattern[str]],
    msg: Optional[str] = None,
    extras: Any = None,
) -> None:
    """Expects that the text matches the regular expression."""
    try:
        asserts.assert_regex(text, expected_regex, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to match regex %r, but it does not.',
            text,
            expected_regex,
        )
        expects.recorder.add_error(e)


def expect_not_regex(
    text: str,
    unexpected_regex: Union[str, Pattern[str]],
    msg: Optional[str] = None,
    extras: Any = None,
) -> None:
    """Expects that the text does not match the regular expression."""
    try:
        asserts.assert_not_regex(text, unexpected_regex, msg, extras)
    except signals.TestSignal as e:
        logging.exception(
            'Expected %r to not match regex %r, but it does.',
            text,
            unexpected_regex,
        )
        expects.recorder.add_error(e)
