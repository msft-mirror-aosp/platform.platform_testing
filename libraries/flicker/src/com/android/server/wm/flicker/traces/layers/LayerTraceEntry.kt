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

import android.surfaceflinger.nano.Layers
import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.common.AssertionResult

/** Represents a single Layer trace entry.  */
class LayerTraceEntry constructor(
    override val timestamp: Long, // hierarchical representation of layers
    rootLayers: List<Layer>
) : com.android.server.wm.flicker.common.traces.layers
    .LayerTraceEntry<Layer>(timestamp, rootLayers) {

    /**
     * Obtains the region occupied by all layers with name containing [layerName].
     *
     * @param layerName Name of the layer to search
     * @param resultComputation Predicate to compute a result based on the found layer's region
     */
    @JvmOverloads
    fun covers(
        layerName: String = "",
        resultComputation: (android.graphics.Region) -> AssertionResult
    ): AssertionResult {
        val assertionName = "coversRegion"
        val filteredLayers = flattenedLayers.filter { it.name.contains(layerName) }

        return if (filteredLayers.isEmpty()) {
            AssertionResult("Could not find $layerName", assertionName, timestamp, success = false)
        } else {
            val jointRegion = android.graphics.Region()
            val visibleLayers = filteredLayers
                    .filter { it.isVisible && !it.isHiddenByParent }
            visibleLayers.forEach {
                jointRegion.op(it.visibleRegion.toAndroidRegion(), android.graphics.Region.Op.UNION)
            }

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
    fun coversAtLeastRegion(testRegion: android.graphics.Region, layerName: String = ""):
        AssertionResult {
        return covers(layerName) { jointRegion ->
            val intersection = android.graphics.Region(jointRegion)
            val covers = intersection.op(testRegion, android.graphics.Region.Op.INTERSECT) &&
                    !intersection.op(testRegion, android.graphics.Region.Op.XOR)

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
    fun coversAtMostRegion(testRegion: android.graphics.Region, layerName: String = ""):
        AssertionResult {
        return covers(layerName) { jointRegion ->
            val testRect = testRegion.bounds
            val intersection = android.graphics.Region(jointRegion)
            val covers = intersection.op(testRect, android.graphics.Region.Op.INTERSECT) &&
                    !intersection.op(jointRegion, android.graphics.Region.Op.XOR)

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
    fun hasVisibleRegion(layerName: String, expectedVisibleRegion: android.graphics.Region):
        AssertionResult {
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
                if ((visibleRegion.toAndroidRegion() == expectedVisibleRegion)) {
                    return AssertionResult(
                            layer.name + "has visible region " + expectedVisibleRegion,
                            assertionName,
                            timestamp,
                            success = true)
                }
                reason = (layer.name +
                        " has visible region:" +
                        visibleRegion.toAndroidRegion() +
                        " " +
                        "expected:" +
                        expectedVisibleRegion)
            }
        }
        return AssertionResult(reason, assertionName, timestamp, success = false)
    }

    @VisibleForTesting
    fun getVisibleBounds(layerName: String): android.graphics.Region {
        return flattenedLayers.firstOrNull { it.name.contains(layerName) && it.isVisible }
                ?.visibleRegion?.toAndroidRegion()
                ?: android.graphics.Region()
    }

    private fun com.android.server.wm.flicker.common.Region.toAndroidRegion():
        android.graphics.Region {
        return android.graphics.Region(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

    companion object {
        fun fromFlattenedProtoLayers(
            timestamp: Long,
            protos: Array<Layers.LayerProto>,
            orphanLayerCallback: ((Layer) -> Boolean)?
        ): LayerTraceEntry {
            val layers = protos.map {
                Layer(it)
            }

            val trace = fromFlattenedLayers(timestamp, layers.toTypedArray(), orphanLayerCallback)

            return LayerTraceEntry(trace.timestamp, trace.rootLayers.map { it })
        }
    }
}