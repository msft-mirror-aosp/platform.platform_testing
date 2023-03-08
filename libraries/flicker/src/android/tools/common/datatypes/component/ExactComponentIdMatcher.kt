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

package android.tools.common.datatypes.component

import android.tools.common.traces.surfaceflinger.Layer
import android.tools.common.traces.wm.Activity
import android.tools.common.traces.wm.WindowContainer

/**
 * A component matcher that matches the targeted window and layer with ids windowId and layerId
 * respectively.
 *
 * No other windows or layers will be matched, if you want to also match the children of that window
 * and layer then use FullComponentIdMatcher instead.
 */
class ExactComponentIdMatcher(private val windowId: Int, private val layerId: Int) :
    IComponentMatcher {
    /**
     * @param windows to search
     * @return if any of the [components] matches any of [windows]
     */
    override fun windowMatchesAnyOf(windows: Array<WindowContainer>) =
        windows.any { it.token == windowId.toString(16) }

    /**
     * @param activities to search
     * @return if any of the [components] matches any of [activities]
     */
    override fun activityMatchesAnyOf(activities: Array<Activity>) =
        activities.any { it.token == windowId.toString(16) }

    /**
     * @param layers to search
     * @return if any of the [components] matches any of [layers]
     */
    override fun layerMatchesAnyOf(layers: Array<Layer>) = layers.any { it.id == layerId }

    /** {@inheritDoc} */
    override fun check(
        layers: Collection<Layer>,
        condition: (Collection<Layer>) -> Boolean
    ): Boolean = condition(layers.filter { it.id == layerId })

    /** {@inheritDoc} */
    override fun toActivityIdentifier() = toWindowIdentifier()

    /** {@inheritDoc} */
    override fun toWindowIdentifier() = "Window#${windowId.toString(16)}"

    /** {@inheritDoc} */
    override fun toLayerIdentifier(): String = "Layer#$layerId"
}
