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

package com.android.server.wm.flicker.assertiongenerator.common

import com.android.server.wm.flicker.assertiongenerator.common.AssertionObject.Companion.AssertionFunction
import com.android.server.wm.traces.common.service.Scenario

interface IAssertionGenerator<Trace> {
    val config: Map<Scenario, Array<out Trace>>

    fun getTracesForScenario(scenario: Scenario): Array<out Trace> {
        return config[scenario]!! // should always have a config for all scenarios
    }

    fun getAssertionsForTrace(trace: Trace): Array<AssertionObject> {
        // TO-DO - connect to the whole pipeline of determining assertions b/241905004
        // currently a stub for testing
        val assertionVisible = AssertionObject(mutableListOf(1, 2, 3), AssertionFunction.IS_VISIBLE)
        val assertionMovesRight =
            AssertionObject(mutableListOf(4, 5, 6), AssertionFunction.MOVES_RIGHT)
        return arrayOf(assertionVisible, assertionMovesRight)
    }

    fun getAssertionsForScenario(scenario: Scenario): Array<AssertionObject> {
        val assertions = mutableListOf<AssertionObject>()
        for (trace in getTracesForScenario(scenario)) {
            val traceAssertions = getAssertionsForTrace(trace)
            assertions.addAll(traceAssertions)
        }
        return assertions.toTypedArray()
    }
}
