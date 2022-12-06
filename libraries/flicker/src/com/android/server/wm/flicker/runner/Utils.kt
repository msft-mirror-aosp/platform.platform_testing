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

package com.android.server.wm.flicker.runner

import android.app.Instrumentation
import android.os.Bundle
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.traces.common.ConditionList
import com.android.server.wm.traces.common.IScenario
import com.android.server.wm.traces.common.WindowManagerConditionsFactory
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import kotlin.system.measureTimeMillis

/** Helper class for flicker transition rules */
object Utils {
    /**
     * Conditions that determine when the UI is in a stable and no windows or layers are animating
     * or changing state.
     */
    private val UI_STABLE_CONDITIONS =
        ConditionList(
            listOf(
                WindowManagerConditionsFactory.isWMStateComplete(),
                WindowManagerConditionsFactory.hasLayersAnimating().negate()
            )
        )
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

    /** Execute [predicate] and log [logMessage] in logcat and notify the host side runner */
    internal fun executeAndNotifyRunner(
        scenario: IScenario,
        logMessage: String,
        predicate: () -> Unit
    ) {
        notifyRunnerProgress(scenario, logMessage)
        measureTimeMillis { predicate() }
            .also { notifyRunnerProgress(scenario, "\tDuration ${it}ms") }
    }

    internal fun doWaitForUiStabilize(wmHelper: WindowManagerStateHelper) {
        wmHelper.StateSyncBuilder().add(UI_STABLE_CONDITIONS).waitFor()
    }

    private fun notifyRunnerProgress(scenario: IScenario, msg: String) {
        Log.d(FLICKER_RUNNER_TAG, "${scenario.key} - $msg")
        val results = Bundle()
        results.putString(Instrumentation.REPORT_KEY_STREAMRESULT, "$msg\n")
        instrumentation.sendStatus(1, results)
    }
}
