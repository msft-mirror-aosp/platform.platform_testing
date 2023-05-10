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

package android.tools.common.flicker.assertors.assertions

import android.tools.common.flicker.IScenarioInstance
import android.tools.common.flicker.assertors.ComponentTemplate
import android.tools.common.flicker.subject.events.EventLogSubject
import android.tools.common.traces.component.ComponentNameMatcher
import android.tools.common.traces.component.IComponentNameMatcher

val ANY_MATCH_COMPONENT = ComponentTemplate("ANY") { _ -> ComponentNameMatcher("", "") }

class FocusChanges(
    private val fromComponent: ComponentTemplate = ANY_MATCH_COMPONENT,
    private val toComponent: ComponentTemplate = ANY_MATCH_COMPONENT
) : AssertionTemplateWithComponent(fromComponent, toComponent) {

    // TODO: Make parent call this when appropriate
    override fun doEvaluate(scenarioInstance: IScenarioInstance, eventLog: EventLogSubject) {
        val layersTrace = scenarioInstance.reader.readLayersTrace()
        val fromComponent = fromComponent.build(scenarioInstance)
        val toComponent = toComponent.build(scenarioInstance)

        val fromPackage =
            if (fromComponent is IComponentNameMatcher) {
                fromComponent.packageName
            } else {
                requireNotNull(layersTrace) { "Missing layers trace" }
                layersTrace.entries
                    .flatMap { it.flattenedLayers.asList() }
                    .first { fromComponent.layerMatchesAnyOf(it) }
                    .packageName
            }

        val toPackage =
            if (toComponent is IComponentNameMatcher) {
                toComponent.packageName
            } else {
                requireNotNull(layersTrace) { "Missing layers trace" }
                layersTrace.entries
                    .flatMap { it.flattenedLayers.asList() }
                    .first { toComponent.layerMatchesAnyOf(it) }
                    .packageName
            }

        eventLog.focusChanges(fromPackage, toPackage)
    }
}
