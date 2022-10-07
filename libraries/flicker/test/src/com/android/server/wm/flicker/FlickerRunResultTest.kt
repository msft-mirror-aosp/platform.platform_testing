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

package com.android.server.wm.flicker

import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.android.server.wm.flicker.helpers.BrowserAppHelper
import com.android.server.wm.flicker.helpers.MessagingAppHelper
import com.android.server.wm.flicker.helpers.isShellTransitionsEnabled
import com.android.server.wm.flicker.helpers.wakeUpAndGoToHomeScreen
import com.android.server.wm.flicker.monitor.TransactionsTraceMonitor
import com.android.server.wm.flicker.monitor.TransitionsTraceMonitor
import com.android.server.wm.flicker.rules.RemoveAllTasksButHomeRule
import com.android.server.wm.traces.common.transition.Transition
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.Test

/**
 * Contains [FlickerRunResultTest] tests.
 *
 * To run this test: `atest FlickerLibTest:FlickerRunResultTest`
 */
class FlickerRunResultTest {

    val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    val device: UiDevice = UiDevice.getInstance(instrumentation)
    val testApp = MessagingAppHelper(instrumentation)
    val testApp2 = BrowserAppHelper(instrumentation)
    val wmHelper = WindowManagerStateHelper(instrumentation, clearCacheAfterParsing = false)

    @Test
    fun canLoadCroppedTransitionsTrace() {
        Assume.assumeTrue(isShellTransitionsEnabled)

        val runResult = FlickerRunResult("testName")

        val transactionsTraceMonitor = TransactionsTraceMonitor()
        val transitionsTraceMonitor = TransitionsTraceMonitor()
        val traceMonitors = listOf(transactionsTraceMonitor, transitionsTraceMonitor)

        // Start tracing
        traceMonitors.forEach { it.start() }

        // Setup
        device.wakeUpAndGoToHomeScreen()
        testApp2.launchViaIntent()
        RemoveAllTasksButHomeRule.removeAllTasksButHome()
        wmHelper.StateSyncBuilder().add(TransitionRunner.UI_STABLE_CONDITIONS).waitFor()

        // Transition
        runResult.notifyTransitionStarting()
        testApp.launchViaIntent()
        wmHelper.StateSyncBuilder().add(TransitionRunner.UI_STABLE_CONDITIONS).waitFor()
        runResult.notifyTransitionEnded()

        // Teardown
        testApp.exit()
        wmHelper.StateSyncBuilder().add(TransitionRunner.UI_STABLE_CONDITIONS).waitFor()

        // Stop tracing & set trace results
        traceMonitors.forEach { it.stop() }
        traceMonitors.forEach { runResult.setResultsFromMonitor(it) }
        runResult.lock()

        val transitionsTrace = runResult.transitionsTrace
        requireNotNull(transitionsTrace) { "Expected transitionsTrace to not be null" }

        Truth.assertThat(transitionsTrace.entries).isNotEmpty()
        Truth.assertThat(transitionsTrace.entries).hasLength(1)

        Truth.assertThat(transitionsTrace.entries.first().type)
            .isEqualTo(Transition.Companion.Type.OPEN)
        Truth.assertThat(
            transitionsTrace.entries.first().changes.any {
                it.windowName.contains(testApp.componentMatcher.toString()) &&
                    it.transitMode == Transition.Companion.Type.OPEN
            }
        )

        runResult.clearFromMemory()
    }
}
