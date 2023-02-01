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
import android.platform.test.rule.ArtifactSaver
import com.android.server.wm.flicker.IFlickerTestData
import com.android.server.wm.flicker.io.ResultWriter
import com.android.server.wm.traces.common.IScenario
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.android.server.wm.traces.parser.withPerfettoTrace
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Test rule to run transition setup and teardown
 *
 * @param flicker test definition
 * @param resultWriter to write
 * @param scenario to run the transition
 * @param instrumentation to interact with the device
 * @param setupCommands to run before the transition
 * @param teardownCommands to run after the transition
 * @param wmHelper to stabilize the UI before/after transitions
 */
class SetupTeardownRule(
    private val flicker: IFlickerTestData,
    private val resultWriter: ResultWriter,
    private val scenario: IScenario,
    private val instrumentation: Instrumentation,
    private val setupCommands: List<IFlickerTestData.() -> Any> = flicker.transitionSetup,
    private val teardownCommands: List<IFlickerTestData.() -> Any> = flicker.transitionTeardown,
    private val wmHelper: WindowManagerStateHelper = flicker.wmHelper
) : TestRule {
    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    doRunTransitionSetup(description)
                    base?.evaluate()
                } finally {
                    doRunTransitionTeardown(description)
                }
            }
        }
    }

    @Throws(TransitionSetupFailure::class)
    private fun doRunTransitionSetup(description: Description?) {
        withPerfettoTrace("doRunTransitionSetup") {
            Utils.notifyRunnerProgress(scenario, "Running transition setup for $description")
            try {
                setupCommands.forEach { it.invoke(flicker) }
                Utils.doWaitForUiStabilize(wmHelper)
            } catch (e: Throwable) {
                ArtifactSaver.onError(Utils.expandDescription(description, "setup"), e)
                throw TransitionSetupFailure(e)
            }
        }
    }

    @Throws(TransitionTeardownFailure::class)
    private fun doRunTransitionTeardown(description: Description?) {
        withPerfettoTrace("doRunTransitionTeardown") {
            Utils.notifyRunnerProgress(scenario, "Running transition teardown for $description")
            try {
                teardownCommands.forEach { it.invoke(flicker) }
                Utils.doWaitForUiStabilize(wmHelper)
            } catch (e: Throwable) {
                ArtifactSaver.onError(Utils.expandDescription(description, "teardown"), e)
                throw TransitionTeardownFailure(e)
            }
        }
    }
}
