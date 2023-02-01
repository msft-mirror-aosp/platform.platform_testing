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
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.flicker.helpers.StandardAppHelper
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.android.server.wm.traces.parser.withPerfettoTrace
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Launches an app before the test
 *
 * @param instrumentation Instrumentation mechanism to use
 * @param wmHelper WM/SF synchronization helper
 * @param appHelper App to launch
 * @param clearCacheAfterParsing If the caching used while parsing the proto should be
 * ```
 *                               cleared or remain in memory
 * ```
 */
class LaunchAppRule
@JvmOverloads
constructor(
    private val appHelper: StandardAppHelper,
    private val instrumentation: Instrumentation = appHelper.mInstrumentation,
    private val clearCacheAfterParsing: Boolean = true,
    private val wmHelper: WindowManagerStateHelper =
        WindowManagerStateHelper(clearCacheAfterParsing = clearCacheAfterParsing)
) : TestWatcher() {
    @JvmOverloads
    constructor(
        componentMatcher: ComponentNameMatcher,
        appName: String = "",
        instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
        clearCache: Boolean = true,
        wmHelper: WindowManagerStateHelper =
            WindowManagerStateHelper(clearCacheAfterParsing = clearCache)
    ) : this(
        StandardAppHelper(instrumentation, appName, componentMatcher),
        instrumentation,
        clearCache,
        wmHelper
    )

    override fun starting(description: Description?) {
        withPerfettoTrace("LaunchAppRule:finished") {
            Log.v(FLICKER_TAG, "Launching app $appHelper")
            appHelper.launchViaIntent()
            appHelper.exit(wmHelper)
        }
    }
}
