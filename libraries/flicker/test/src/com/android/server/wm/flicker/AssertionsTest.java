/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.server.wm.flicker;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.runner.AndroidJUnit4;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import kotlin.jvm.functions.Function1;

/**
 * Contains {@link Assertions} tests. To run this test: {@code atest FlickerLibTest:AssertionsTest}
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AssertionsTest {
    @Test
    public void traceEntryAssertionCanNegateResult() {
        final Function1<Integer, AssertionResult> assertNumEquals42 =
                getIntegerTraceEntryAssertion();

        assertThat(assertNumEquals42.invoke(1).getSuccess()).isFalse();
        assertThat(Assertions.negate(assertNumEquals42).invoke(1).getSuccess()).isTrue();

        assertThat(assertNumEquals42.invoke(42).getSuccess()).isTrue();
        assertThat(Assertions.negate(assertNumEquals42).invoke(42).getSuccess()).isFalse();
    }

    @Test
    public void resultCanBeNegated() {
        String reason = "Everything is fine!";
        AssertionResult result = new AssertionResult(reason, "TestAssert", 0, true);
        AssertionResult negatedResult = result.negate();

        assertThat(negatedResult.getSuccess()).isFalse();
        assertThat(negatedResult.getReason()).isEqualTo(reason);
        assertThat(negatedResult.getAssertionName()).isEqualTo("!TestAssert");
    }

    private Function1<Integer, AssertionResult> getIntegerTraceEntryAssertion() {
        return (num) -> {
            if (num == 42) {
                return new AssertionResult("Num equals 42", true);
            }
            return new AssertionResult("Num doesn't equal 42, actual:" + num, false);
        };
    }
}
