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

import com.android.launcher3.tapl.LauncherInstrumentation;
import com.android.launcher3.tapl.Workspace;

import org.junit.runner.Description;

/** Drags an app to the homescreen for quick access. */
public class AddAppToHomescreenRule extends TestWatcher {
    private final String LOG_TAG = AddAppToHomescreenRule.class.getSimpleName();

    private final LauncherInstrumentation mLauncher = new LauncherInstrumentation();

    private final String mAppName;

    public AddAppToHomescreenRule(String appName) {
        mAppName = appName;
    }

    @Override
    protected void starting(Description description) {
        mLauncher.pressHome();
        Workspace workspace = mLauncher.getWorkspace();
        if (workspace.tryGetWorkspaceAppIcon(mAppName) != null) {
            Log.d(LOG_TAG, "App icon is already on the homescreen.");
        } else {
            Log.d(LOG_TAG, "Adding app icon to home screen.");
            mLauncher
                    .getWorkspace()
                    .switchToAllApps()
                    .getAppIcon(mAppName)
                    .dragToWorkspace(/* startsActivity */ false, /* isWidgetShortcut */ false);
        }
    }
}
