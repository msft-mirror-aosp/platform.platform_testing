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
import android.platform.test.rule.NavigationModeRule
import android.platform.test.rule.PressHomeRule
import android.platform.test.rule.UnlockScreenRule
import com.android.server.wm.flicker.IFlickerTestData
import com.android.server.wm.flicker.datastore.CachedResultWriter
import com.android.server.wm.flicker.helpers.MessagingAppHelper
import com.android.server.wm.flicker.io.ResultData
import com.android.server.wm.flicker.io.ResultWriter
import com.android.server.wm.flicker.rules.ChangeDisplayOrientationRule
import com.android.server.wm.flicker.rules.LaunchAppRule
import com.android.server.wm.flicker.rules.RemoveAllTasksButHomeRule
import com.android.server.wm.traces.common.IScenario
import com.android.server.wm.traces.parser.withPerfettoTrace
import org.junit.rules.RuleChain
import org.junit.runner.Description

/**
 * Transition runner that executes a default device setup (based on [scenario]) as well as the
 * flicker setup/transition/teardown
 */
class TransitionRunner(
    private val scenario: IScenario,
    private val instrumentation: Instrumentation,
    private val resultWriter: ResultWriter = CachedResultWriter()
) {
    /** Executes [flicker] transition and returns the result */
    fun execute(flicker: IFlickerTestData, description: Description?): ResultData {
        return withPerfettoTrace("TransitionRunner:execute") {
            resultWriter.forScenario(scenario).withOutputDir(flicker.outputDir)

            val ruleChain = buildTestRuleChain(flicker)
            try {
                ruleChain.apply(/* statement */ null, description).evaluate()
                resultWriter.setRunComplete()
            } catch (e: Throwable) {
                resultWriter.setRunFailed(e)
            }
            resultWriter.write()
        }
    }

    /**
     * Create the default flicker test setup rules. In order:
     * - unlock device
     * - change orientation
     * - change navigation mode
     * - launch an app
     * - remove all apps
     * - go home
     *
     * (b/186740751) An app should be launched because, after changing the navigation mode, the
     * first app launch is handled as a screen size change (similar to a rotation), this causes
     * different problems during testing (e.g. IME now shown on app launch)
     */
    private fun buildTestRuleChain(flicker: IFlickerTestData): RuleChain {
        return RuleChain.outerRule(UnlockScreenRule())
            .around(NavigationModeRule(scenario.navBarMode.value))
            .around(
                LaunchAppRule(MessagingAppHelper(instrumentation), clearCacheAfterParsing = false)
            )
            .around(RemoveAllTasksButHomeRule())
            .around(
                ChangeDisplayOrientationRule(scenario.startRotation, clearCacheAfterParsing = false)
            )
            .around(PressHomeRule())
            .around(
                TraceMonitorRule(
                    flicker.traceMonitors,
                    scenario,
                    flicker.wmHelper,
                    resultWriter,
                    instrumentation
                )
            )
            .around(SetupTeardownRule(flicker, resultWriter, scenario, instrumentation))
            .around(TransitionExecutionRule(flicker, resultWriter, scenario, instrumentation))
    }
}
