/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker.rules

import android.app.Instrumentation
import android.content.ComponentName
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.helpers.StandardAppHelper
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Launched an app before the test
 *
 * @param component App to launch
 * @param instrumentation Instrumentation mechanism to use
 */
data class LaunchAppRule @JvmOverloads constructor(
    private val component: ComponentName,
    private val appName: String = "",
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
) : TestWatcher() {
    private val appHelper = StandardAppHelper(instrumentation, appName, component)
    private val wmHelper = WindowManagerStateHelper()

    override fun starting(description: Description?) {
        appHelper.launchViaIntent()
        appHelper.exit(wmHelper)
    }
}