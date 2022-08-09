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

import com.android.server.wm.flicker.service.assertors.AssertionData
import com.android.server.wm.flicker.service.assertors.BaseAssertionBuilder
import com.android.server.wm.flicker.service.assertors.Components
import com.android.server.wm.flicker.service.assertors.common.AppLayerIsInvisibleAtEnd
import com.android.server.wm.flicker.service.assertors.common.AppLayerIsInvisibleAtStart
import com.android.server.wm.flicker.service.assertors.common.AppLayerIsVisibleAtEnd
import com.android.server.wm.flicker.service.assertors.common.AppLayerIsVisibleAtStart
import com.android.server.wm.flicker.service.assertors.common.EntireScreenCoveredAlways
import com.android.server.wm.flicker.service.assertors.common.EntireScreenCoveredAtEnd
import com.android.server.wm.flicker.service.assertors.common.EntireScreenCoveredAtStart
import com.android.server.wm.flicker.service.assertors.common.LayerIsVisibleAlways
import com.android.server.wm.flicker.service.assertors.common.LayerIsVisibleAtEnd
import com.android.server.wm.flicker.service.assertors.common.LayerIsVisibleAtStart
import com.android.server.wm.flicker.service.assertors.common.NonAppWindowIsVisibleAlways
import com.android.server.wm.flicker.service.assertors.common.StatusBarLayerPositionAtEnd
import com.android.server.wm.flicker.service.assertors.common.StatusBarLayerPositionAtStart
import com.android.server.wm.flicker.service.assertors.common.VisibleLayersShownMoreThanOneConsecutiveEntry
import com.android.server.wm.flicker.service.assertors.common.VisibleWindowsShownMoreThanOneConsecutiveEntry
import com.android.server.wm.flicker.service.config.common.AssertionInvocationGroup
import com.android.server.wm.flicker.service.config.common.Scenario
import com.android.server.wm.flicker.service.config.common.ScenarioInstance
import com.android.server.wm.traces.common.transition.Transition

object Assertions {
    fun assertionsForScenarioInstance(scenarioInstance: ScenarioInstance): List<AssertionData> {
        return assertionsForScenario(scenarioInstance.scenario).map {
            AssertionData(scenarioInstance.scenario, it, it.invocationGroup)
        }
    }

    fun assertionsForTransition(transition: Transition): List<AssertionData> {
        val assertions: MutableList<AssertionData> = mutableListOf()
        for (scenario in Scenario.values()) {
            scenario.description
            if (scenario.executionCondition.shouldExecute(transition)) {
                for (assertion in assertionsForScenario(scenario)) {
                    assertions.add(
                        AssertionData(scenario, assertion, assertion.invocationGroup)
                    )
                }
            }
        }

        return assertions
    }

    private val COMMON_ASSERTIONS = listOf(
        LayerIsVisibleAtStart(Components.NAV_BAR) runAs AssertionInvocationGroup.NON_BLOCKING,
        LayerIsVisibleAtEnd(Components.NAV_BAR) runAs AssertionInvocationGroup.NON_BLOCKING,
        NonAppWindowIsVisibleAlways(Components.NAV_BAR) runAs AssertionInvocationGroup.NON_BLOCKING,
        NonAppWindowIsVisibleAlways(Components.STATUS_BAR)
                runAs AssertionInvocationGroup.NON_BLOCKING,
        LayerIsVisibleAlways(Components.STATUS_BAR) runAs AssertionInvocationGroup.NON_BLOCKING,
        EntireScreenCoveredAtStart() runAs AssertionInvocationGroup.NON_BLOCKING,
        EntireScreenCoveredAtEnd() runAs AssertionInvocationGroup.NON_BLOCKING,
        EntireScreenCoveredAlways() runAs AssertionInvocationGroup.NON_BLOCKING,
        VisibleWindowsShownMoreThanOneConsecutiveEntry()
                runAs AssertionInvocationGroup.NON_BLOCKING,
        VisibleLayersShownMoreThanOneConsecutiveEntry() runAs AssertionInvocationGroup.NON_BLOCKING,
        StatusBarLayerPositionAtStart() runAs AssertionInvocationGroup.NON_BLOCKING,
        StatusBarLayerPositionAtEnd() runAs AssertionInvocationGroup.NON_BLOCKING
    )

    private val APP_LAUNCH_ASSERTIONS = COMMON_ASSERTIONS + listOf(
        AppLayerIsVisibleAtStart(Components.LAUNCHER) runAs AssertionInvocationGroup.NON_BLOCKING,
        AppLayerIsInvisibleAtStart(Components.OPENING_APP)
                runAs AssertionInvocationGroup.NON_BLOCKING,

        AppLayerIsInvisibleAtEnd(Components.LAUNCHER) runAs AssertionInvocationGroup.NON_BLOCKING,
        AppLayerIsVisibleAtEnd(Components.OPENING_APP) runAs AssertionInvocationGroup.NON_BLOCKING
    )

    private val APP_CLOSE_ASSERTIONS = COMMON_ASSERTIONS + listOf(
        AppLayerIsVisibleAtStart(Components.CLOSING_APP)
                runAs AssertionInvocationGroup.NON_BLOCKING,
        AppLayerIsInvisibleAtStart(Components.LAUNCHER) runAs AssertionInvocationGroup.NON_BLOCKING,

        AppLayerIsInvisibleAtEnd(Components.CLOSING_APP)
                runAs AssertionInvocationGroup.NON_BLOCKING,
        AppLayerIsVisibleAtEnd(Components.LAUNCHER) runAs AssertionInvocationGroup.NON_BLOCKING
    )

    private fun assertionsForScenario(scenario: Scenario): List<BaseAssertionBuilder> {
        return when (scenario) {
            Scenario.COMMON -> COMMON_ASSERTIONS
            Scenario.APP_LAUNCH -> APP_LAUNCH_ASSERTIONS
            Scenario.APP_CLOSE -> APP_CLOSE_ASSERTIONS
            Scenario.ROTATION -> TODO()
            Scenario.IME_APPEAR -> TODO()
            Scenario.IME_DISAPPEAR -> TODO()
            Scenario.PIP_ENTER -> TODO()
            Scenario.PIP_EXIT -> TODO()
        }
    }
}
