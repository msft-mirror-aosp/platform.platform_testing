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

package com.android.server.wm.traces.common.layers

import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.IComponentMatcher
import com.android.server.wm.traces.common.ITraceEntry
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.prettyTimestamp
import kotlin.js.JsName

/** Base class for SF trace entries */
abstract class BaseLayerTraceEntry : ITraceEntry {
    @JsName("hwcBlob") abstract val hwcBlob: String
    @JsName("where") abstract val where: String
    @JsName("displays") abstract val displays: Array<Display>
    @JsName("vSyncId") abstract val vSyncId: Long
    @JsName("stableId")
    val stableId: String
        get() = this::class.simpleName ?: error("Unable to determine class")
    @JsName("name")
    val name: String
        get() = prettyTimestamp(timestamp)

    @JsName("flattenedLayers") abstract val flattenedLayers: Array<Layer>
    @JsName("visibleLayers")
    val visibleLayers: Array<Layer>
        get() = flattenedLayers.filter { it.isVisible }.toTypedArray()

    // for winscope
    @JsName("isVisible") val isVisible: Boolean = true
    @JsName("children")
    val children: Array<Layer>
        get() = flattenedLayers.filter { it.isRootLayer }.toTypedArray()

    @JsName("physicalDisplay")
    val physicalDisplay: Display?
        get() = displays.firstOrNull { !it.isVirtual }
    @JsName("physicalDisplayBounds")
    val physicalDisplayBounds: Rect?
        get() = physicalDisplay?.layerStackSpace

    /**
     * @return A [Layer] matching [componentMatcher] with a non-empty active buffer, or null if no
     * layer matches [componentMatcher] or if the matching layer's buffer is empty
     *
     * @param componentMatcher Components to search
     */
    @JsName("getLayerWithBuffer")
    fun getLayerWithBuffer(componentMatcher: IComponentMatcher): Layer? {
        return flattenedLayers.firstOrNull {
            componentMatcher.layerMatchesAnyOf(it) && it.activeBuffer.isNotEmpty
        }
    }

    /** @return The [Layer] with [layerId], or null if the layer is not found */
    @JsName("getLayerById")
    fun getLayerById(layerId: Int): Layer? = this.flattenedLayers.firstOrNull { it.id == layerId }

    /**
     * Checks if any layer matching [componentMatcher] in the screen is animating.
     *
     * The screen is animating when a layer is not simple rotation, of when the pip overlay layer is
     * visible
     *
     * @param componentMatcher Components to search
     */
    @JsName("isAnimating")
    fun isAnimating(
        prevState: BaseLayerTraceEntry?,
        componentMatcher: IComponentMatcher? = null
    ): Boolean {
        val curLayers =
            visibleLayers.filter {
                componentMatcher == null || componentMatcher.layerMatchesAnyOf(it)
            }
        val currIds = visibleLayers.map { it.id }
        val prevStateLayers =
            prevState?.visibleLayers?.filter { currIds.contains(it.id) } ?: emptyList()
        val layersAnimating =
            curLayers.any { currLayer ->
                val prevLayer = prevStateLayers.firstOrNull { it.id == currLayer.id }
                currLayer.isAnimating(prevLayer)
            }
        val pipAnimating = isVisible(ComponentNameMatcher.PIP_CONTENT_OVERLAY)
        return layersAnimating || pipAnimating
    }

    /**
     * Check if at least one window matching [componentMatcher] is visible.
     *
     * @param componentMatcher Components to search
     */
    @JsName("isVisibleComponent")
    fun isVisible(componentMatcher: IComponentMatcher): Boolean =
        componentMatcher.layerMatchesAnyOf(visibleLayers)

    /** @return A [LayersTrace] object containing this state as its only entry */
    @JsName("asTrace") fun asTrace(): LayersTrace = LayersTrace(arrayOf(this))

    override fun toString(): String {
        return "${prettyTimestamp(timestamp)} (timestamp=$timestamp)"
    }

    override fun equals(other: Any?): Boolean {
        return other is BaseLayerTraceEntry && other.timestamp == this.timestamp
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + hwcBlob.hashCode()
        result = 31 * result + where.hashCode()
        result = 31 * result + displays.contentHashCode()
        result = 31 * result + isVisible.hashCode()
        result = 31 * result + flattenedLayers.contentHashCode()
        return result
    }
}
