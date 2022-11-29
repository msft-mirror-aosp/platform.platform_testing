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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.flicker.helpers.TimeFormatter
import com.android.server.wm.flicker.helpers.format
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.service.ScenarioInstance
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.google.common.truth.Fact

data class FaasData(
    val scenarioInstance: ScenarioInstance,
    val entireWmTrace: WindowManagerTrace,
    val entireLayersTrace: LayersTrace
) {
    fun toFacts(): Collection<Fact> {
        return listOf(
            Fact.fact("Extracted from WM trace start", entireWmTrace.first().timestamp.format()),
            Fact.fact("Extracted from WM trace end", entireWmTrace.first().timestamp.format()),
            Fact.fact(
                "Extracted from SF trace start",
                entireLayersTrace.first().timestamp.format()
            ),
            Fact.fact("Extracted from SF trace end", entireLayersTrace.first().timestamp.format()),
            Fact.fact("Scenario type", scenarioInstance.scenario.scenarioType),
            Fact.fact("Scenario rotation", scenarioInstance.scenario.rotation),
            Fact.fact(
                "Scenario start",
                "${TimeFormatter.format(scenarioInstance.startTimestamp)} " +
                    "(timestamp=${scenarioInstance.startTimestamp})"
            ),
            Fact.fact(
                "Scenario end",
                "${TimeFormatter.format(scenarioInstance.endTimestamp)} " +
                    "(timestamp=${scenarioInstance.endTimestamp})"
            ),
            Fact.fact("Associated transition type", scenarioInstance.associatedTransition.type),
            Fact.fact(
                "Associated transition changes",
                scenarioInstance.associatedTransition.changes.joinToString("\n  -", "\n  -") {
                    "${it.transitMode} ${it.windowName}"
                }
            )
        )
    }
}
