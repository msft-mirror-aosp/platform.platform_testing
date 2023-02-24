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

package com.android.server.wm.flicker.integration

import android.annotation.SuppressLint
import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import com.android.compatibility.common.util.SystemUtil
import com.android.server.wm.flicker.FlickerBuilder
import com.android.server.wm.flicker.TEST_SCENARIO
import com.android.server.wm.flicker.datastore.DataStore
import com.android.server.wm.flicker.helpers.BrowserAppHelper
import com.android.server.wm.flicker.helpers.MessagingAppHelper
import com.android.server.wm.flicker.runner.TransitionRunner
import org.junit.runner.Description

@SuppressLint("VisibleForTests")
object Utils {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    internal const val TAG = "tag"
    internal const val FAILURE = "Expected failure"
    internal val setupAndTearDownTestApp = BrowserAppHelper(instrumentation)
    internal val transitionTestApp = MessagingAppHelper(instrumentation)

    private fun createFlicker(onExecuted: () -> Unit) =
        FlickerBuilder(instrumentation)
            .apply {
                setup {
                    // Shouldn't be in the trace we run assertions on
                    setupAndTearDownTestApp.launchViaIntent(wmHelper)
                    setupAndTearDownTestApp.exit(wmHelper)
                }
                transitions {
                    // Should be in the trace we run assertions on
                    transitionTestApp.launchViaIntent(wmHelper)
                    createTag(TAG)
                    onExecuted()
                }
                teardown {
                    // Shouldn't be in the trace we run assertions on
                    setupAndTearDownTestApp.launchViaIntent(wmHelper)
                    setupAndTearDownTestApp.exit(wmHelper)
                }
            }
            .build()

    fun runTransition(onExecuted: () -> Unit) {
        DataStore.clear()
        val flicker = createFlicker(onExecuted)

        // Clear the trace output directory
        SystemUtil.runShellCommand("rm -rf ${flicker.outputDir}")

        val runner = TransitionRunner(TEST_SCENARIO, instrumentation)
        runner.execute(flicker, Description.createTestDescription(this::class.java, "test"))
    }
}
