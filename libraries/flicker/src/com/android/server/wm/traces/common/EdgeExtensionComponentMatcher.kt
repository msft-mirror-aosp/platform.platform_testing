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

class EdgeExtensionComponentMatcher : IComponentMatcher {
    /** {@inheritDoc} */
    override fun windowMatchesAnyOf(windows: Array<WindowState>): Boolean {
        // Doesn't have a window component only layers
        return false
    }

    /** {@inheritDoc} */
    override fun activityMatchesAnyOf(activities: Array<Activity>): Boolean {
        // Doesn't have a window component only layers
        return false
    }

    /** {@inheritDoc} */
    override fun layerMatchesAnyOf(layers: Array<Layer>): Boolean {
        return layers.any {
            if (it.name.contains("bbq-wrapper")) {
                val parent = it.parent ?: return false
                return layerMatchesAnyOf(parent)
            }

            return it.name.contains("Left Edge Extension") ||
                    it.name.contains("Right Edge Extension")
        }
    }

    /** {@inheritDoc} */
    override fun toActivityIdentifier(): String {
        throw NotImplementedError(
                "toActivityIdentifier() is not implemented on EdgeExtensionComponentMatcher")
    }

    /** {@inheritDoc} */
    override fun toWindowIdentifier(): String {
        throw NotImplementedError(
                "toWindowName() is not implemented on EdgeExtensionComponentMatcher")
    }

    /** {@inheritDoc} */
    override fun toLayerIdentifier(): String {
        return "EdgeExtensionLayer"
    }
}
