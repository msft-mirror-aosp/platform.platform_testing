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

package com.android.server.wm.flicker.service

import com.android.server.wm.flicker.service.assertors.AssertionResult
import com.android.server.wm.flicker.service.assertors.TransitionAsserter
import com.android.server.wm.flicker.service.config.Assertions.assertionsForScenarioInstance
import com.android.server.wm.flicker.service.config.common.Scenario
import com.android.server.wm.flicker.service.config.common.ScenarioInstance
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

/**
 * Invokes the configured assertors and summarizes the results.
 */
class AssertionEngine(
    private val logger: (String) -> Unit
) {
    fun analyze(
        wmTrace: WindowManagerTrace,
        layersTrace: LayersTrace,
        transitionsTrace: TransitionsTrace
    ): List<AssertionResult> {
        logger.invoke("AssertionEngine#analyze")

        val assertionResults = mutableListOf<AssertionResult>()

        val scenarioInstances = mutableListOf<ScenarioInstance>()
        for (scenario in Scenario.values()) {
            scenarioInstances.addAll(
                scenario.getInstances(transitionsTrace, logger))
        }

        for (scenarioInstance in scenarioInstances) {
            val assertionsToCheck = assertionsForScenarioInstance(scenarioInstance)
            logger.invoke("${assertionsToCheck.size} assertions to check for $scenarioInstance")

            val result = TransitionAsserter(assertionsToCheck, logger)
                .analyze(scenarioInstance, wmTrace, layersTrace)
            assertionResults.addAll(result)
        }

        return assertionResults
    }
}
