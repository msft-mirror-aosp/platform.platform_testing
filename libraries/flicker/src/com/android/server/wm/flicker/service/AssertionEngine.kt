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

import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.assertiongenerator.ScenarioConfig
import com.android.server.wm.flicker.assertiongenerator.common.AssertionFactory
import com.android.server.wm.flicker.service.assertors.AssertionData
import com.android.server.wm.flicker.service.assertors.AssertionResult
import com.android.server.wm.flicker.service.assertors.ConfigException
import com.android.server.wm.flicker.service.assertors.TransitionAsserter
import com.android.server.wm.flicker.service.config.Assertions.allAssertionsForScenarioInstance
import com.android.server.wm.flicker.service.config.Assertions.assertionsForScenarioInstance
import com.android.server.wm.flicker.service.config.Assertions.generatedAssertionsForScenarioInstance
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.service.Scenario
import com.android.server.wm.traces.common.service.ScenarioInstance
import com.android.server.wm.traces.common.service.ScenarioType
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import java.io.FileNotFoundException

/** Invokes the configured assertors and summarizes the results. */
class AssertionEngine(
    private val configProducer: AssertionGeneratorConfigProducer,
    private val logger: (String) -> Unit
) {
    val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

    lateinit var assertionFactory: AssertionFactory

    enum class AssertionsToUse() {
        HARDCODED,
        GENERATED,
        ALL
    }

    private fun initializeAssertionFactory(): AssertionFactory {
        val config: Map<Scenario, ScenarioConfig> =
            try {
                configProducer.produce()
            } catch (err: FileNotFoundException) {
                logger.invoke("$err, so config was set to empty")
                mapOf()
            }
        return AssertionFactory(config)
    }

    private fun getAssertions(
        assertionsToUse: AssertionsToUse,
        scenarioInstance: ScenarioInstance
    ): List<AssertionData> {
        return try {
            when (assertionsToUse) {
                AssertionsToUse.HARDCODED -> {
                    assertionsForScenarioInstance(scenarioInstance)
                }
                AssertionsToUse.GENERATED -> {
                    generatedAssertionsForScenarioInstance(scenarioInstance, assertionFactory)
                }
                AssertionsToUse.ALL -> {
                    allAssertionsForScenarioInstance(scenarioInstance, assertionFactory)
                }
            }
        } catch (err: ConfigException) {
            logger.invoke(err.toString())
            listOf()
        }
    }

    fun analyze(
        wmTrace: WindowManagerTrace,
        layersTrace: LayersTrace,
        transitionsTrace: TransitionsTrace,
        assertionsToUse: AssertionsToUse = AssertionsToUse.HARDCODED
    ): List<AssertionResult> {
        logger.invoke("AssertionEngine#analyze")

        if (assertionsToUse != AssertionsToUse.HARDCODED) {
            assertionFactory = initializeAssertionFactory()
        }

        val assertionResults = mutableListOf<AssertionResult>()

        val scenarioInstances = mutableListOf<ScenarioInstance>()
        for (scenarioType in ScenarioType.values()) {
            scenarioInstances.addAll(scenarioType.getInstances(transitionsTrace, wmTrace, logger))
        }

        for (scenarioInstance in scenarioInstances) {
            val assertionsToCheck = getAssertions(assertionsToUse, scenarioInstance)
            logger.invoke("${assertionsToCheck.size} assertions to check for $scenarioInstance")

            val result =
                TransitionAsserter(assertionsToCheck, logger)
                    .analyze(scenarioInstance, wmTrace, layersTrace)
            assertionResults.addAll(result)
        }
        return assertionResults
    }
}
