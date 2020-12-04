/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.flicker

import com.android.server.wm.flicker.assertions.Assertions
import com.android.server.wm.flicker.assertions.Assertions.negate
import com.android.server.wm.traces.common.AssertionResult
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [Assertions] tests. To run this test: `atest FlickerLibTest:AssertionsTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AssertionsTest {
    @Test
    fun traceEntryAssertionCanNegateResult() {
        val assertNumEquals42 = integerTraceEntryAssertion
        Truth.assertThat(assertNumEquals42.invoke(1).success).isFalse()
        Truth.assertThat(assertNumEquals42.negate().invoke(1).success).isTrue()
        Truth.assertThat(assertNumEquals42.invoke(42).success).isTrue()
        Truth.assertThat(assertNumEquals42.negate().invoke(42).success).isFalse()
    }

    @Test
    fun resultCanBeNegated() {
        val reason = "Everything is fine!"
        val result = AssertionResult(reason, "TestAssert", 0, true)
        val (reason1, assertionName, _, success) = result.negate()
        Truth.assertThat(success).isFalse()
        Truth.assertThat(reason1).isEqualTo(reason)
        Truth.assertThat(assertionName).isEqualTo("!TestAssert")
    }

    private val integerTraceEntryAssertion: (Int) -> AssertionResult
        get() = { num: Int ->
            if (num == 42) {
                AssertionResult("Num equals 42", true)
            } else {
                AssertionResult("Num doesn't equal 42, actual:$num", false)
            }
        }
}
