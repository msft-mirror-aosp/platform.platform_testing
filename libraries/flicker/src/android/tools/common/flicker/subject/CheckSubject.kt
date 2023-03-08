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

package android.tools.common.flicker.subject

import android.tools.common.flicker.assertions.Fact

/** Subject for flicker checks */
data class CheckSubject<T>(
    private val actualValue: T?,
    private val subject: FlickerSubject,
    private val lazyMessage: () -> String,
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

    fun isNull() {
        if (actualValue != null) {
            failWithFactForExpectedValue(Fact("expected to be", null))
        }
    }

    fun isNotNull() {
        if (actualValue == null) {
            failWithFactForExpectedValue(Fact("expected not to be", null))
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
