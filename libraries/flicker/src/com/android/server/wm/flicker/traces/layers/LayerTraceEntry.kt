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

package com.android.server.wm.flicker.traces.layers

import android.graphics.Region
import android.surfaceflinger.nano.Layers
import android.util.SparseArray
import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.assertions.AssertionResult
import com.android.server.wm.flicker.traces.ITraceEntry

/** Represents a single Layer trace entry.  */
class LayerTraceEntry constructor(
    override val timestamp: Long, // hierarchical representation of layers
    val rootLayers: List<Layer>
) : ITraceEntry {
    private val _opaqueLayers = mutableListOf<Layer>()
    private val _transparentLayers = mutableListOf<Layer>()
    private val _rootScreenBounds by lazy {
        val rootLayerBounds = rootLayers
                .filter { it.proto?.sourceBounds != null }
                .first { it.proto?.name?.startsWith("Root#0") == true }
                .proto?.sourceBounds ?: throw IllegalStateException("Root layer must have bounds")

        Region(0, 0, rootLayerBounds.bottom.toInt(), rootLayerBounds.right.toInt())
    }

    val flattenedLayers by lazy {
        val layers = mutableListOf<Layer>()
        val roots = rootLayers.fillOcclusionState().toMutableList()
        while (roots.isNotEmpty()) {
            val layer = roots.removeAt(0)
            layers.add(layer)
            roots.addAll(layer.children)
        }
        layers.toList()
    }

    private fun List<Layer>.topDownTraversal(): List<Layer> {
        return this
                .sortedBy { it.z }
                .flatMap { it.topDownTraversal() }
    }

    val visibleLayers by lazy { flattenedLayers.filter { it.isVisible && !it.isHiddenByParent } }

    val opaqueLayers: List<Layer> get() = _opaqueLayers

    val transparentLayers: List<Layer> get() = _transparentLayers

    private fun Layer.topDownTraversal(): List<Layer> {
        val traverseList = mutableListOf(this)

        this.children.sortedBy { it.z }
                .forEach { childLayer ->
                    traverseList.addAll(childLayer.topDownTraversal())
                }

        return traverseList
    }

    private fun List<Layer>.fillOcclusionState(): List<Layer> {
        val traversalList = topDownTraversal().reversed()

        traversalList.forEach { layer ->
            val visible = layer.isVisible

            if (visible) {
                layer.occludedBy.addAll(_opaqueLayers.filter { it.contains(layer) })
                layer.partiallyOccludedBy.addAll(_opaqueLayers.filter { it.overlaps(layer) })
                layer.coveredBy.addAll(_transparentLayers.filter { it.overlaps(layer) })

                if (layer.isOpaque) {
                    _opaqueLayers.add(layer)
                } else {
                    _transparentLayers.add(layer)
                }
            }
        }

        return this
    }

    /**
     * Obtains the region occupied by all layers with name containing [layerName].
     *
     * @param layerName Name of the layer to search
     * @param resultComputation Predicate to compute a result based on the found layer's region
     */
    @JvmOverloads
    fun covers(
        layerName: String = "",
        resultComputation: (Region) -> AssertionResult
    ): AssertionResult {
        val assertionName = "coversRegion"
        val filteredLayers = flattenedLayers.filter { it.name.contains(layerName) }

        return if (filteredLayers.isEmpty()) {
            AssertionResult("Could not find $layerName", assertionName, timestamp, success = false)
        } else {
            val jointRegion = Region()
            filteredLayers
                    .filter { it.isVisible && !it.isHiddenByParent }
                    .forEach { jointRegion.op(it.visibleRegion, Region.Op.UNION) }

            resultComputation(jointRegion)
        }
    }

    /**
     * Checks if all layers layers with name containing [layerName] has a visible area of at
     * least [testRegion], that is, if its area of the layer's visible region covers each point in
     * the region.
     *
     * @param testRegion Expected covered area
     * @param layerName Name of the layer to search
     */
    fun coversAtLeastRegion(testRegion: Region, layerName: String = ""): AssertionResult {
        return covers(layerName) { jointRegion ->
            val intersection = Region(jointRegion)
            val covers = intersection.op(testRegion, Region.Op.INTERSECT) &&
                    !intersection.op(testRegion, Region.Op.XOR)

            val reason = if (covers) {
                "Region covered $testRegion"
            } else {
                "Region to test: $testRegion\nUncovered region: $intersection"
            }

            AssertionResult(reason, "coversAtLeastRegion", timestamp, success = covers)
        }
    }

    /**
     * Checks if all layers layers with name containing [layerName] has a visible area of at
     * most [testRegion], that is, if the region covers each point in the layer's visible region.
     *
     * @param testRegion Expected covered area
     * @param layerName Name of the layer to search
     */
    fun coversAtMostRegion(testRegion: Region, layerName: String = ""): AssertionResult {
        return covers(layerName) { jointRegion ->
            val testRect = testRegion.bounds
            val intersection = Region(jointRegion)
            val covers = intersection.op(testRect, Region.Op.INTERSECT) &&
                    !intersection.op(jointRegion, Region.Op.XOR)

            val reason = if (covers) {
                "Region covered $testRegion"
            } else {
                "Region to test: $testRegion\nOut-of-bounds region: $intersection"
            }

            AssertionResult(reason, "coversAtMostRegion", timestamp, success = covers)
        }
    }

    /**
     * Checks if a layer containing the name [layerName] has a visible region of exactly
     * [expectedVisibleRegion].
     *
     * @param layerName Name of the layer to search
     * @param expectedVisibleRegion Expected visible region of the layer
     */
    fun hasVisibleRegion(layerName: String, expectedVisibleRegion: Region): AssertionResult {
        val assertionName = "hasVisibleRegion"
        var reason = "Could not find $layerName"
        for (layer in flattenedLayers) {
            if (layer.name.contains(layerName)) {
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
                reason = layer.name +
                        " has visible region:" +
                        visibleRegion +
                        " " +
                        "expected:" +
                        expectedVisibleRegion
            }
        }
        return AssertionResult(reason, assertionName, timestamp, success = false)
    }

    /**
     * Checks if a layer containing the name [layerName] exists in the hierarchy.
     *
     * @param layerName Name of the layer to search
     */
    fun exists(layerName: String): AssertionResult {
        val assertionName = "exists"
        val reason = "Could not find $layerName"
        for (layer in flattenedLayers) {
            if (layer.name.contains(layerName)) {
                return AssertionResult(
                        layer.name + " exists",
                        assertionName,
                        timestamp,
                        success = true)
            }
        }
        return AssertionResult(reason, assertionName, timestamp, success = false)
    }

    /**
     * Checks if a layer with name [layerName] is visible.
     *
     * @param layerName Name of the layer to search
     */
    fun isVisible(layerName: String): AssertionResult {
        val assertionName = "isVisible"
        var reason = "Could not find $layerName"
        for (layer in flattenedLayers) {
            if (layer.name.contains(layerName)) {
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

    @VisibleForTesting
    fun getVisibleBounds(layerName: String): Region {
        return flattenedLayers.firstOrNull { it.name.contains(layerName) && it.isVisible }
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
                if (orphanLayerCallback == null || orphanLayerCallback.invoke(orphan)) {
                    return@forEach
                }
                val childNodes: String = orphan.children
                        .joinToString(", ") { it.id.toString() }

                val orphanId: Int = orphan.children.first().parentId
                throw RuntimeException(
                        ("Failed to parse layers trace. Found orphan layers with parent " +
                                "layer id:" +
                                orphanId +
                                " : " +
                                childNodes))
            }
            return LayerTraceEntry(timestamp, rootLayer.children)
        }
    }
}