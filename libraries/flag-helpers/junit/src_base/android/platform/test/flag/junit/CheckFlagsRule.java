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

import static org.junit.Assume.assumeTrue;

import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A {@code TestRule} that checks flags condition and skips tests with "assumption failed" if the
 * condition can not be met.
 *
 * <p>This {@code TestRule} is used together with the flag requirement annotations, including {@code
 * RequiresFlagsEnabled} and {@code RequiresFlagsDisabled}. It parses the annotations on test
 * methods and classes, and decides whether the tests should be skipped according to the flag values
 * on the device.
 */
public final class CheckFlagsRule implements TestRule {
    private final IFlagsValueProvider mFlagsValueProvider;

    public CheckFlagsRule(IFlagsValueProvider flagsValueProvider) {
        mFlagsValueProvider = flagsValueProvider;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                AnnotationsRetriever.FlagAnnotations flagAnnotations =
                        AnnotationsRetriever.getFlagAnnotations(description);
                RequiresFlagsEnabled requiresFlagsEnabled = flagAnnotations.mRequiresFlagsEnabled;
                RequiresFlagsDisabled requiresFlagsDisabled =
                        flagAnnotations.mRequiresFlagsDisabled;
                mFlagsValueProvider.setUp();
                try {
                    if (requiresFlagsEnabled != null) {
                        for (String flag : requiresFlagsEnabled.value()) {
                            assumeTrue(
                                    String.format(
                                            "Flag %s required to be enabled, but is disabled",
                                            flag),
                                    mFlagsValueProvider.getBoolean(flag));
                        }
                    }
                    if (requiresFlagsDisabled != null) {
                        for (String flag : requiresFlagsDisabled.value()) {
                            assumeTrue(
                                    String.format(
                                            "Flag %s required to be disabled, but is enabled",
                                            flag),
                                    !mFlagsValueProvider.getBoolean(flag));
                        }
                    }
                } finally {
                    mFlagsValueProvider.tearDownBeforeTest();
                }
                base.evaluate();
            }
        };
    }
}
