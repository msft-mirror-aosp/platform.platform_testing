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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.flicker.service.IScenarioInstance
import com.android.server.wm.traces.common.component.matchers.ComponentNameMatcher
import com.android.server.wm.traces.common.component.matchers.FullComponentIdMatcher
import com.android.server.wm.traces.common.component.matchers.IComponentMatcher
import com.android.server.wm.traces.common.transition.Transition

data class ComponentTemplate(
    val name: String,
    val build: (scenarioInstance: IScenarioInstance) -> IComponentMatcher
) {
    override fun equals(other: Any?): Boolean {
        return other is ComponentTemplate && name == other.name && build == other.build
    }

    override fun hashCode(): Int {
        return name.hashCode() * 39 + build.hashCode()
    }
}

object Components {

    val NAV_BAR = ComponentTemplate("Navbar") { ComponentNameMatcher.NAV_BAR }
    val STATUS_BAR = ComponentTemplate("StatusBar") { ComponentNameMatcher.STATUS_BAR }
    val LAUNCHER = ComponentTemplate("Launcher") { ComponentNameMatcher.LAUNCHER }

    val OPENING_APP =
        ComponentTemplate("OPENING_APP") { scenarioInstance: IScenarioInstance ->
            openingAppFrom(
                scenarioInstance.associatedTransition ?: error("Missing associated transition")
            )
        }
    val CLOSING_APP =
        ComponentTemplate("CLOSING_APP") { scenarioInstance: IScenarioInstance ->
            closingAppFrom(
                scenarioInstance.associatedTransition ?: error("Missing associated transition")
            )
        }

    val EMPTY = ComponentTemplate("") { ComponentNameMatcher("", "") }

    // TODO: Extract out common code between two functions below
    private fun openingAppFrom(transition: Transition): IComponentMatcher {
        val targetChanges =
            transition.changes.filter {
                it.transitMode == Transition.Companion.Type.OPEN ||
                    it.transitMode == Transition.Companion.Type.TO_FRONT
            }

        val openingLayerIds = targetChanges.map { it.layerId }
        require(openingLayerIds.size == 1) {
            "Expected 1 opening layer but got ${openingLayerIds.size}"
        }

        val openingWindowIds = targetChanges.map { it.windowId }
        require(openingWindowIds.size == 1) {
            "Expected 1 opening window but got ${openingWindowIds.size}"
        }

        val windowId = openingWindowIds.first()
        val layerId = openingLayerIds.first()
        return FullComponentIdMatcher(windowId, layerId)
    }

    private fun closingAppFrom(transition: Transition): IComponentMatcher {
        val targetChanges =
            transition.changes.filter {
                it.transitMode == Transition.Companion.Type.CLOSE ||
                    it.transitMode == Transition.Companion.Type.TO_BACK
            }

        val closingLayerIds = targetChanges.map { it.layerId }
        require(closingLayerIds.size == 1) {
            "Expected 1 closing layer but got ${closingLayerIds.size}"
        }

        val closingWindowIds = targetChanges.map { it.layerId }
        require(closingWindowIds.size == 1) {
            "Expected 1 closing window but got ${closingWindowIds.size}"
        }

        val windowId = closingWindowIds.first()
        val layerId = closingLayerIds.first()
        return FullComponentIdMatcher(windowId, layerId)
    }

    val byType: Map<String, ComponentTemplate> =
        mapOf("OPENING_APP" to OPENING_APP, "CLOSING_APP" to CLOSING_APP)
}
