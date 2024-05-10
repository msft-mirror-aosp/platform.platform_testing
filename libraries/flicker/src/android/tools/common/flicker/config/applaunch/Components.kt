/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.common.flicker.config.applaunch

import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.assertors.ComponentTemplate
import android.tools.common.flicker.isAppTransitionChange
import android.tools.common.traces.component.FullComponentIdMatcher
import android.tools.common.traces.component.IComponentMatcher
import android.tools.common.traces.surfaceflinger.LayersTrace
import android.tools.common.traces.wm.Transition
import android.tools.common.traces.wm.TransitionType
import android.tools.common.traces.wm.WindowManagerTrace

object Components {
    val OPENING_APPS =
        ComponentTemplate("OPENING_APP(S)") { scenarioInstance: ScenarioInstance ->
            openingAppFrom(
                scenarioInstance.associatedTransition ?: error("Missing associated transition"),
                scenarioInstance.reader.readLayersTrace(),
                scenarioInstance.reader.readWmTrace()
            )
        }

    private fun openingAppFrom(
        transition: Transition,
        layersTrace: LayersTrace?,
        wmTrace: WindowManagerTrace?
    ): IComponentMatcher {
        val targetChanges =
            transition.changes.filter {
                (it.transitMode == TransitionType.OPEN ||
                    it.transitMode == TransitionType.TO_FRONT) &&
                    isAppTransitionChange(it, layersTrace, wmTrace)
            }

        val openingAppMatchers =
            targetChanges.map { FullComponentIdMatcher(it.windowId, it.layerId) }

        return openingAppMatchers.reduce<IComponentMatcher, IComponentMatcher> { acc, matcher ->
            acc.or(matcher)
        }
    }
}
