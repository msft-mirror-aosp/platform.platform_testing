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

package com.android.server.wm.traces.common

import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.windowmanager.windows.Activity
import com.android.server.wm.traces.common.windowmanager.windows.WindowState

class ComponentIdMatcher(
    private val windowId: Int,
    private val layerId: Int
) : IComponentMatcher {
    /**
     * @return if any of the [components] matches any of [windows]
     *
     * @param windows to search
     */
    override fun windowMatchesAnyOf(windows: Array<WindowState>) =
        windows.any {
            it.token == windowId.toString(16)
        }

    /**
     * @return if any of the [components] matches any of [activities]
     *
     * @param activities to search
     */
    override fun activityMatchesAnyOf(activities: Array<Activity>) =
        activities.any {
            it.token == windowId.toString(16)
        }

    /**
     * @return if any of the [components] matches any of [layers]
     *
     * @param layers to search
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
