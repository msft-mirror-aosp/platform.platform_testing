/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wm.flicker.assertions

/** Subject builder for flicker checks */
class CheckSubjectBuilder(
    private val lazyMessage: () -> String,
    private val subject: FlickerSubject
) {
    fun <T> that(actual: T?): CheckSubject<T> {
        return CheckSubject(actual, lazyMessage, subject)
    }
}

/** Subject for flicker checks */
class CheckSubject<T>(
    private val actualValue: T?,
    private val lazyMessage: () -> String,
    private val subject: FlickerSubject
) {
    fun isEqual(expectedValue: T?) {
        if (actualValue != expectedValue) {
            failWithFactForExpectedValue(Fact("expected to be equal to", expectedValue))
        }
    }

    fun isNotEqual(expectedValue: T?) {
        if (actualValue == expectedValue) {
            failWithFactForExpectedValue(Fact("expected to be different from", expectedValue))
        }
    }

    fun isLower(expectedValue: T?) {
        if (
            actualValue == null ||
                expectedValue == null ||
                (actualValue as Comparable<T>) >= expectedValue
        ) {
            failWithFactForExpectedValue(Fact("expected to be lower than", expectedValue))
        }
    }

    fun isLowerOrEqual(expectedValue: T?) {
        if (
            actualValue == null ||
                expectedValue == null ||
                (actualValue as Comparable<T>) > expectedValue
        ) {
            failWithFactForExpectedValue(Fact("expected to be lower or equal to", expectedValue))
        }
    }

    fun isGreater(expectedValue: T) {
        if (
            actualValue == null ||
                expectedValue == null ||
                (actualValue as Comparable<T>) <= expectedValue
        ) {
            failWithFactForExpectedValue(Fact("expected to be greater than", expectedValue))
        }
    }

    fun isGreaterOrEqual(expectedValue: T) {
        if (
            actualValue == null ||
                expectedValue == null ||
                (actualValue as Comparable<T>) < expectedValue
        ) {
            failWithFactForExpectedValue(Fact("expected to be greater or equal to", expectedValue))
        }
    }

    fun <U> contains(expectedValue: U) {
        if (actualValue !is List<*> || !(actualValue as List<U>).contains(expectedValue)) {
            failWithFactForExpectedValue(Fact("expected to contain", expectedValue))
        }
    }

    private fun failWithFactForExpectedValue(factForExpectedValue: Fact) {
        val facts =
            listOf(
                Fact("Assertion failed", lazyMessage()),
                Fact("Actual value", actualValue),
                factForExpectedValue,
            )
        subject.fail(facts)
    }
}
