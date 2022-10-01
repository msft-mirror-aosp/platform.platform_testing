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

import android.util.Log;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;

import kotlin.Unit;

/**
 * Generates a "Baseline Profile" for the provided package using the wrapped test statements.
 *
 * <p>To use this, simply apply it as a {@link org.junit.ClassRule} and pass the required arguments.
 * {@link org.junit.rules.TestRule}s aren't aware of whether they're being applied as a {@link
 * org.junit.Rule} or a {@link org.junit.ClassRule}. The {@code BaselineProfileRule} is best used as
 * at the top class- or suite-level, though unenforcible. Side-effects of using this for each {@link
 * org.junit.Test} or for each class within a {@link org.junit.runners.Suite} are not documented.
 *
 * <p>For more information on what Baseline Profiles are, how they work, and how they help, {@see
 * https://d.android.com/topic/performance/baselineprofiles}.
 */
public class BaselineProfileRule extends TestWatcher {
    private static final String LOG_TAG = BaselineProfileRule.class.getSimpleName();
    // If true, generates a Baseline Profile. If not, or unspecified, doesn't generate one.
    private static final String GEN_BASELINE_PROFILE_OPTION = "generate-baseline-profile";

    private final String mBaselineProfilePackage;

    public BaselineProfileRule(String baselineProfilePackage) {
        mBaselineProfilePackage = baselineProfilePackage;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        if (!"true".equals(getArguments().getString(GEN_BASELINE_PROFILE_OPTION))) {
            Log.d(
                    LOG_TAG,
                    String.format(
                            "Baseline Profile generation is currently disabled. Provide the '%s'"
                                    + "option to enable it.",
                            GEN_BASELINE_PROFILE_OPTION));
            return base;
        }

        // This class can't extend androidx.benchmark.macro.junit4.BaselineProfileRule, because it's
        // final; however that would be a preferable way to more cleanly interact with it.
        androidx.benchmark.macro.junit4.BaselineProfileRule innerRule =
                new androidx.benchmark.macro.junit4.BaselineProfileRule();
        return innerRule.apply(
                new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        innerRule.collectBaselineProfile(
                                mBaselineProfilePackage,
                                new ArrayList<>(),
                                (scope) -> {
                                    // Evaluating the base Statement may throw a Throwable, which
                                    // is checked and not compatible with the lambda without a
                                    // try-catch statement.
                                    try {
                                        base.evaluate();
                                        return Unit.INSTANCE;
                                    } catch (Throwable e) {
                                        throw new RuntimeException(
                                                "Caught checked exception in parent statement.", e);
                                    }
                                });
                    }
                },
                description);
    }
}
