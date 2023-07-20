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

package android.platform.test.flag.junit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests for {@code CheckFlagsRule}. Test MUST be ended with '_execute' if it is not going to be
 * skipped.
 */
@RunWith(JUnit4.class)
@RequiresFlagsEnabled("flag1")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CheckFlagsRuleTest {
    private static final List<String> EXPECTED_TESTS_EXECUTED =
            Arrays.stream(CheckFlagsRuleTest.class.getDeclaredMethods())
                    .map(Method::getName)
                    .filter(methodName -> methodName.endsWith("_execute"))
                    .sorted()
                    .collect(Collectors.toList());

    private static final List<String> ACTUAL_TESTS_EXECUTED = new ArrayList<>();

    private final IFlagsValueProvider mFlagsValueProvider =
            new IFlagsValueProvider() {
                @Override
                public boolean getBoolean(String flag) throws FlagReadException {
                    switch (flag) {
                        case "flag1":
                            return true;
                        case "flag2":
                            return false;
                        default:
                            throw new FlagReadException("flag3", "expected boolean but a String");
                    }
                }
            };

    @Rule public final CheckFlagsRule mCheckFlagsRule = new CheckFlagsRule(mFlagsValueProvider);

    @BeforeClass
    public static void clearList() {
        ACTUAL_TESTS_EXECUTED.clear();
    }

    @Test
    public void noAnnotation_execute() {
        ACTUAL_TESTS_EXECUTED.add("noAnnotation_execute");
    }

    @Test
    @RequiresFlagsEnabled("flag2")
    public void methodAnnotationOverrideClassAnnotation_skip() {
        // Should be skipped.
        fail();
    }

    @Test
    @RequiresFlagsEnabled("flag1")
    @RequiresFlagsDisabled("flag2")
    public void requireBothEnabledAndDisabledFlags_execute() {
        ACTUAL_TESTS_EXECUTED.add("requireBothEnabledAndDisabledFlags_execute");
    }

    @Test
    @RequiresFlagsDisabled("flag1")
    public void requiredEnabledFlagDisabled_skip() {
        // Should be skipped.
        fail();
    }

    @Test
    @RequiresFlagsEnabled({"flag1", "flag2"})
    public void requiredDisabledFlagEnabled_skip() {
        // Should be skipped.
        fail();
    }

    @Test
    public void zLastTest_checkExecutedTests() { // Starts the method name with 'z' so that
        // it will be the last test to get executed.
        assertArrayEquals(EXPECTED_TESTS_EXECUTED.toArray(), ACTUAL_TESTS_EXECUTED.toArray());
    }
}
