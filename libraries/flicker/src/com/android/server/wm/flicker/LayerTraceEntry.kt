/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.flicker

import android.graphics.Region
import android.surfaceflinger.nano.Layers
import android.util.SparseArray

/** Represents a single Layer trace entry.  */
class LayerTraceEntry constructor(
    override val timestamp: Long, // hierarchical representation of layers
    val rootLayers: List<Layer>
) : ITraceEntry {
    val flattenedLayers by lazy {
        val layers = mutableListOf<Layer>()
        val pendingLayers = rootLayers.toMutableList()
        while (pendingLayers.isNotEmpty()) {
            val layer = pendingLayers.removeAt(0)
            layers.add(layer)
            pendingLayers.addAll(layer.children)
        }
        layers
    }

    /** Checks if a region specified by `testRect` is covered by all visible layers.  */
    fun coversRegion(testRegion: Region): AssertionResult {
        val testRect = testRegion.bounds
        val assertionName = "coversRegion"
        for (x in testRect.left until testRect.right) {
            var y = testRect.top
            while (y < testRect.bottom) {
                var emptyRegionFound = true
                for (layer in flattenedLayers) {
                    if (layer.isInvisible || layer.isHiddenByParent) {
                        continue
                    }
                    if (layer.visibleRegion.contains(x, y)) {
                        y = layer.visibleRegion.bounds.bottom
                        emptyRegionFound = false
                    }
                }
                if (emptyRegionFound) {
                    var reason = ("Region to test: $testRegion"
                            + "\nfirst empty point: $x, $y"
                            + "\nvisible regions:")
                    for (layer in flattenedLayers) {
                        if (layer.isInvisible || layer.isHiddenByParent) {
                            continue
                        }
                        val r = layer.visibleRegion
                        reason += "\n" + layer.name + r.toString()
                    }
                    return AssertionResult(
                            reason,
                            assertionName,
                            timestamp,
                            success = false)
                }
                y++
            }
        }
        return AssertionResult(
                reason = "Region covered: $testRect",
                assertionName = assertionName,
                timestamp = timestamp,
                success = true)
    }

    /**
     * Checks if a layer with name `layerName` has a visible region `expectedVisibleRegion`.
     */
    fun hasVisibleRegion(layerName: String, expectedVisibleRegion: Region): AssertionResult {
        val assertionName = "hasVisibleRegion"
        var reason = "Could not find $layerName"
        for (layer in flattenedLayers) {
            if (layer.nameContains(layerName)) {
                if (layer.isHiddenByParent) {
                    reason = layer.hiddenByParentReason
                    continue
                }
                if (layer.isInvisible) {
                    reason = layer.visibilityReason
                    continue
                }
                val visibleRegion = layer.visibleRegion
                if ((visibleRegion == expectedVisibleRegion)) {
                    return AssertionResult(
                            layer.name + "has visible region " + expectedVisibleRegion,
                            assertionName,
                            timestamp,
                            success = true)
                }
                reason = (layer.name
                        + " has visible region:"
                        + visibleRegion
                        + " "
                        + "expected:"
                        + expectedVisibleRegion)
            }
        }
        return AssertionResult(reason, assertionName, timestamp, success = false)
    }

    /** Checks if a layer with name `layerName` exists in the hierarchy.  */
    fun exists(layerName: String): AssertionResult {
        val assertionName = "exists"
        val reason = "Could not find $layerName"
        for (layer in flattenedLayers) {
            if (layer.nameContains(layerName)) {
                return AssertionResult(
                        layer.name + " exists",
                        assertionName,
                        timestamp,
                        success = true)
            }
        }
        return AssertionResult(reason, assertionName, timestamp, success = false)
    }

    /** Checks if a layer with name `layerName` is visible.  */
    fun isVisible(layerName: String): AssertionResult {
        val assertionName = "isVisible"
        var reason = "Could not find $layerName"
        for (layer in flattenedLayers) {
            if (layer.nameContains(layerName)) {
                if (layer.isHiddenByParent) {
                    reason = layer.hiddenByParentReason
                    continue
                }
                if (layer.isInvisible) {
                    reason = layer.visibilityReason
                    continue
                }
                return AssertionResult(
                        layer.name + " is visible",
                        assertionName,
                        timestamp,
                        success = true)
            }
        }
        return AssertionResult(reason, assertionName, timestamp, success = false)
    }

    fun getVisibleBounds(layerName: String): Region {
        return flattenedLayers.firstOrNull { it.nameContains(layerName) && it.isVisible }
                ?.visibleRegion
                ?: Region()
    }

    companion object {
        /**
         * Determines the id of the root element.
         *
         *
         * On some files, such as the ones used in the FlickerLib testdata, the root nodes are
         * those that have parent=0, on newer traces, the root nodes are those that have parent=-1
         *
         *
         * This function keeps compatibility with both new and older traces by searching for a
         * known root layer (Display Root) and considering its parent Id as overall root.
         */
        private fun getRootLayer(layerMap: SparseArray<Layer>): Layer {
            var knownRoot: Layer? = null
            val numKeys: Int = layerMap.size()
            for (i in 0 until numKeys) {
                val currentLayer = layerMap.valueAt(i)
                if (currentLayer.isRootLayer) {
                    knownRoot = currentLayer
                    break
                }
            }
            if (knownRoot == null) {
                throw IllegalStateException("Display root layer not found.")
            }
            return layerMap.get(knownRoot.parentId)
        }

        /** Constructs the layer hierarchy from a flattened list of layers.  */
        @JvmStatic
        fun fromFlattenedLayers(
                timestamp: Long,
                protos: Array<Layers.LayerProto>,
                orphanLayerCallback: ((Layer) -> Boolean)?
        ): LayerTraceEntry {
            val layerMap = SparseArray<Layer>()
            val orphans = mutableListOf<Layer>()
            for (proto: Layers.LayerProto in protos) {
                val id: Int = proto.id
                val parentId: Int = proto.parent
                var newLayer: Layer? = layerMap.get(id)
                when {
                    newLayer == null -> {
                        newLayer = Layer(proto)
                        layerMap.append(id, newLayer)
                    }
                    newLayer.proto != null -> {
                        throw RuntimeException("Duplicate layer id found:$id")
                    }
                    else -> {
                        newLayer.proto = proto
                        orphans.remove(newLayer)
                    }
                }

                // add parent placeholder
                if (layerMap.get(parentId) == null) {
                    val orphanLayer = Layer(null)
                    layerMap.append(parentId, orphanLayer)
                    orphans.add(orphanLayer)
                }
                val parentLayer = layerMap.get(parentId)
                parentLayer.addChild(newLayer)
                newLayer.addParent(parentLayer)
            }

            // Remove root node
            val rootLayer = getRootLayer(layerMap)
            orphans.remove(rootLayer)
            // Fail if we find orphan layers.
            orphans.forEach { orphan: Layer ->
                // Workaround for b/141326137, ignore the existence of an orphan layer
                if (orphanLayerCallback != null && orphanLayerCallback.invoke(orphan)) {
                    return@forEach
                }
                val childNodes: String = orphan.children
                        .joinToString(", ") { it.id.toString() }

                val orphanId: Int = orphan.children.first().parentId
                throw RuntimeException(
                        ("Failed to parse layers trace. Found orphan layers with parent "
                                + "layer id:"
                                + orphanId
                                + " : "
                                + childNodes))
            }
            return LayerTraceEntry(timestamp, rootLayer.children)
        }
    }
}