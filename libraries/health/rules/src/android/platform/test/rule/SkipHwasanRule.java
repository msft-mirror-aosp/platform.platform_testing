/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.platform.test.rule;

import android.os.Build;

import androidx.test.InstrumentationRegistry;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * Rule that skips tests marked with @SkipOnHwasan on Platinum test runs on Hwasan targets. This
 * allows moving to Platinum those tests that fail only on Hwasan Platinum targets.
 */
public class SkipHwasanRule implements TestRule {
    @Override
    public Statement apply(Statement base, Description description) {
        // If the target isn't hwasan, this rule is not applicable.
        if (!Build.PRODUCT.contains("hwasan")) return base;

        // If the test is not annotated with @SkipOnHwasan, this rule is not applicable.
        if (!description.getTestClass().isAnnotationPresent(SkipOnHwasan.class)) return base;

        // If the test suite isn't running with
        // "exclude-annotation": "androidx.test.filters.FlakyTest", then this is not a platinum
        // test, and the rule is not applicable.
        final String nonAnnotationArgument =
                InstrumentationRegistry.getArguments().getString("notAnnotation", "");
        if (!Arrays.stream(nonAnnotationArgument.split(","))
                .anyMatch("androidx.test.filters.FlakyTest"::equals)) {
            return base;
        }

        // The test will be skipped upon start.
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                throw new AssumptionViolatedException("Skipping the test on a hwasan target");
            }
        };
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface SkipOnHwasan {}
}
