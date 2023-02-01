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

package com.android.server.wm.flicker.service.config

import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.assertiongenerator.common.AssertionFactory
import com.android.server.wm.flicker.assertiongenerator.layers.LayersAssertion
import com.android.server.wm.flicker.service.assertors.AssertionData
import com.android.server.wm.flicker.service.assertors.BaseAssertionBuilder
import com.android.server.wm.flicker.service.assertors.Components
import com.android.server.wm.flicker.service.assertors.assertions.AppLayerBecomesVisible
import com.android.server.wm.flicker.service.assertors.assertions.AppLayerIsInvisibleAtEnd
import com.android.server.wm.flicker.service.assertors.assertions.AppLayerIsInvisibleAtStart
import com.android.server.wm.flicker.service.assertors.assertions.AppLayerIsVisibleAtEnd
import com.android.server.wm.flicker.service.assertors.assertions.AppLayerIsVisibleAtStart
import com.android.server.wm.flicker.service.assertors.assertions.AppWindowBecomesTopWindow
import com.android.server.wm.flicker.service.assertors.assertions.AppWindowBecomesVisible
import com.android.server.wm.flicker.service.assertors.assertions.AutomaticallyGeneratedLayersAssertions
import com.android.server.wm.flicker.service.assertors.assertions.AutomaticallyGeneratedWindowManagerAssertions
import com.android.server.wm.flicker.service.assertors.assertions.EntireScreenCoveredAlways
import com.android.server.wm.flicker.service.assertors.assertions.EntireScreenCoveredAtEnd
import com.android.server.wm.flicker.service.assertors.assertions.EntireScreenCoveredAtStart
import com.android.server.wm.flicker.service.assertors.assertions.LayerIsVisibleAlways
import com.android.server.wm.flicker.service.assertors.assertions.LayerIsVisibleAtEnd
import com.android.server.wm.flicker.service.assertors.assertions.LayerIsVisibleAtStart
import com.android.server.wm.flicker.service.assertors.assertions.NonAppWindowIsVisibleAlways
import com.android.server.wm.flicker.service.assertors.assertions.StatusBarLayerPositionAtEnd
import com.android.server.wm.flicker.service.assertors.assertions.StatusBarLayerPositionAtStart
import com.android.server.wm.flicker.service.assertors.assertions.VisibleLayersShownMoreThanOneConsecutiveEntry
import com.android.server.wm.flicker.service.assertors.assertions.VisibleWindowsShownMoreThanOneConsecutiveEntry
import com.android.server.wm.traces.common.service.AssertionInvocationGroup.NON_BLOCKING
import com.android.server.wm.traces.common.service.FlickerServiceScenario
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.service.ScenarioInstance
import com.android.server.wm.traces.common.service.ScenarioType
import com.android.server.wm.traces.common.transition.Transition

object Assertions {
    /**
     * Runs all assertions (hardcoded and generated) for a scenario instance, using assertionFactory
     */
    fun allAssertionsForScenarioInstance(
        scenarioInstance: ScenarioInstance,
        assertionFactory: AssertionFactory
    ): List<AssertionData> {
        return assertionsForScenarioInstance(scenarioInstance) +
            generatedAssertionsForScenarioInstance(scenarioInstance, assertionFactory)
    }

    /** Runs hardcoded assertions for a scenario instance */
    fun assertionsForScenarioInstance(scenarioInstance: ScenarioInstance): List<AssertionData> {
        val hardcodedAssertions = assertionsForScenario(scenarioInstance.scenario)
        return hardcodedAssertions.map {
            AssertionData(scenarioInstance.scenario, it, it.invocationGroup)
        }
    }

    /** Runs generated assertions for a scenario instance, using the given assertionFactory */
    fun generatedAssertionsForScenarioInstance(
        scenarioInstance: ScenarioInstance,
        assertionFactory: AssertionFactory,
        logger: ((String) -> Unit)? = null
    ): List<AssertionData> {
        val generatedAssertions =
            getGeneratedAssertionsForScenario(scenarioInstance, assertionFactory, logger)
        return generatedAssertions.map {
            AssertionData(scenarioInstance.scenario, it, it.invocationGroup)
        }
    }

    fun assertionsForTransition(
        transition: Transition,
        rotation: PlatformConsts.Rotation = PlatformConsts.Rotation.ROTATION_0
    ): List<AssertionData> {
        val assertions: MutableList<AssertionData> = mutableListOf()
        for (scenarioType in ScenarioType.values()) {
            val scenario = FlickerServiceScenario(scenarioType, rotation)
            if (scenarioType.executionCondition.shouldExecute(transition)) {
                for (assertion in assertionsForScenario(scenario)) {
                    assertions.add(AssertionData(scenario, assertion, assertion.invocationGroup))
                }
            }
        }

        return assertions
    }

    private val COMMON_ASSERTIONS =
        listOf(
            LayerIsVisibleAtStart(Components.NAV_BAR) runAs NON_BLOCKING,
            LayerIsVisibleAtEnd(Components.NAV_BAR) runAs NON_BLOCKING,
            NonAppWindowIsVisibleAlways(Components.NAV_BAR) runAs NON_BLOCKING,
            NonAppWindowIsVisibleAlways(Components.STATUS_BAR) runAs NON_BLOCKING,
            LayerIsVisibleAlways(Components.STATUS_BAR) runAs NON_BLOCKING,
            EntireScreenCoveredAtStart() runAs NON_BLOCKING,
            EntireScreenCoveredAtEnd() runAs NON_BLOCKING,
            EntireScreenCoveredAlways() runAs NON_BLOCKING,
            VisibleWindowsShownMoreThanOneConsecutiveEntry() runAs NON_BLOCKING,
            VisibleLayersShownMoreThanOneConsecutiveEntry() runAs NON_BLOCKING,
            StatusBarLayerPositionAtStart() runAs NON_BLOCKING,
            StatusBarLayerPositionAtEnd() runAs NON_BLOCKING,
        )

    private val APP_LAUNCH_ASSERTIONS =
        COMMON_ASSERTIONS +
            listOf(
                AppLayerIsVisibleAtStart(Components.LAUNCHER) runAs NON_BLOCKING,
                AppLayerIsInvisibleAtStart(Components.OPENING_APP) runAs NON_BLOCKING,
                AppLayerIsInvisibleAtEnd(Components.LAUNCHER) runAs NON_BLOCKING,
                AppLayerIsVisibleAtEnd(Components.OPENING_APP) runAs NON_BLOCKING,
                AppLayerBecomesVisible(Components.OPENING_APP) runAs NON_BLOCKING,
                AppWindowBecomesVisible(Components.OPENING_APP) runAs NON_BLOCKING,
                AppWindowBecomesTopWindow(Components.OPENING_APP) runAs NON_BLOCKING,
            )

    private val APP_CLOSE_ASSERTIONS =
        COMMON_ASSERTIONS +
            listOf(
                AppLayerIsVisibleAtStart(Components.CLOSING_APP) runAs NON_BLOCKING,
                AppLayerIsInvisibleAtStart(Components.LAUNCHER) runAs NON_BLOCKING,
                AppLayerIsInvisibleAtEnd(Components.CLOSING_APP) runAs NON_BLOCKING,
                AppLayerIsVisibleAtEnd(Components.LAUNCHER) runAs NON_BLOCKING,
            )

    @VisibleForTesting
    fun getGeneratedAssertionsForScenario(
        scenarioInstance: ScenarioInstance,
        assertionFactory: AssertionFactory,
        logger: ((String) -> Unit)? = null
    ): List<BaseAssertionBuilder> {
        val assertions = assertionFactory.getAssertionsForScenario(scenarioInstance.scenario)
        logger?.invoke("Generated assertions for scenario ${scenarioInstance.scenario}")
        return assertions.map { assertion ->
            logger?.invoke(assertion.toString())
            if (assertion is LayersAssertion) {
                AutomaticallyGeneratedLayersAssertions(assertion) runAs NON_BLOCKING
            } else {
                AutomaticallyGeneratedWindowManagerAssertions(assertion) runAs NON_BLOCKING
            }
        }
    }

    private fun assertionsForScenario(
        scenario: FlickerServiceScenario
    ): List<BaseAssertionBuilder> {
        return when (scenario.scenarioType) {
            ScenarioType.COMMON -> COMMON_ASSERTIONS
            ScenarioType.APP_LAUNCH -> APP_LAUNCH_ASSERTIONS
            ScenarioType.APP_CLOSE -> APP_CLOSE_ASSERTIONS
            ScenarioType.ROTATION -> TODO()
            ScenarioType.IME_APPEAR -> TODO()
            ScenarioType.IME_DISAPPEAR -> TODO()
            ScenarioType.PIP_ENTER -> TODO()
            ScenarioType.PIP_EXIT -> TODO()
        }
    }
}
