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

package android.platform.test.rule;

import org.junit.runner.Description;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** This rule converts a hot startup test to a warm startup one. */
public class WarmAppStartRule extends TestWatcher {
    private final Pattern mAppActivityRecordPattern;

    public WarmAppStartRule(String appPackageName) {
        mAppActivityRecordPattern =
                Pattern.compile(
                        "ActivityRecord\\{.*"
                                + appPackageName.replace(".", "\\.")
                                + "/.*\\st([0-9]+)\\}");
    }

    @Override
    protected void starting(Description description) {
        // Remove the stack of the app.
        final Matcher appActivityMatcher =
                mAppActivityRecordPattern.matcher(
                        executeShellCommand("dumpsys activity activities"));
        if (appActivityMatcher.find()) {
            executeShellCommand("am stack remove " + appActivityMatcher.group(1));
        }

        super.starting(description);
    }
}
