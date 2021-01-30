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

package com.android.server.wm.flicker.traces.layers

import android.graphics.Region
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.android.server.wm.flicker.traces.FlickerSubjectException
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.parser.toAndroidRegion
import com.google.common.truth.ExpectFailure
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Truth

class LayerTraceEntrySubject private constructor(
    fm: FailureMetadata,
    val entry: LayerTraceEntry,
    trace: LayersTraceSubject?
) : FlickerSubject(fm, entry) {
    override val defaultFacts: String = "${trace?.defaultFacts ?: ""}\nEntry: $entry"

    val subjects by lazy {
        entry.flattenedLayers.map { LayerSubject.assertThat(it, this) }
    }

    fun hasTimestamp(timestamp: Long): LayerTraceEntrySubject = apply {
        check("Wrong  entry timestamp").that(entry.timestamp).isEqualTo(timestamp)
    }

    fun isEmpty(): LayerTraceEntrySubject = apply {
        check("Entry should not be empty")
            .that(entry.flattenedLayers)
            .isEmpty()
    }

    fun isNotEmpty(): LayerTraceEntrySubject = apply {
        check("Entry should not be empty")
            .that(entry.flattenedLayers)
            .isNotEmpty()
    }

    fun hasLayersSize(numberLayers: Int): LayerTraceEntrySubject = apply {
        check("Wrong number of layers in entry")
            .that(entry.flattenedLayers.size)
            .isEqualTo(numberLayers)
    }

    /**
     * Checks if all layers layers with name containing [layerName] has a visible area of at
     * least [testRegion], that is, if its area of the layer's visible region covers each point in
     * the region.
     *
     * @param testRegion Expected covered area
     * @param layerName Name of the layer to search
     */
    @JvmOverloads
    fun coversAtLeastRegion(
        testRegion: Region,
        layerName: String = ""
    ): LayerTraceEntrySubject = apply {
        covers(layerName) { jointRegion ->
            val intersection = Region(jointRegion)
            val covers = intersection.op(testRegion, Region.Op.INTERSECT) &&
                !intersection.op(testRegion, Region.Op.XOR)

            if (!covers) {
                fail(Fact.fact("Region to test", testRegion),
                    Fact.fact("Uncovered region", intersection))
            }
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
    @JvmOverloads
    fun coversAtLeastRegion(
        testRegion: com.android.server.wm.traces.common.Region,
        layerName: String = ""
    ): LayerTraceEntrySubject = apply {
        coversAtLeastRegion(testRegion.toAndroidRegion(), layerName)
    }

    /**
     * Checks if all layers layers with name containing [layerName] has a visible area of at
     * most [testRegion], that is, if the region covers each point in the layer's visible region.
     *
     * @param testRegion Expected covered area
     * @param layerName Name of the layer to search
     */
    @JvmOverloads
    fun coversAtMostRegion(
        testRegion: Region,
        layerName: String = ""
    ): LayerTraceEntrySubject = apply {
        covers(layerName) { jointRegion ->
            val testRect = testRegion.bounds
            val intersection = Region(jointRegion)
            val covers = intersection.op(testRect, Region.Op.INTERSECT) &&
                !intersection.op(jointRegion, Region.Op.XOR)

            if (!covers) {
                fail(Fact.fact("Region to test", testRegion),
                    Fact.fact("Out-of-bounds region", intersection))
            }
        }
    }

    /**
     * Checks if all layers layers with name containing [layerName] has a visible area of at
     * most [testRegion], that is, if the region covers each point in the layer's visible region.
     *
     * @param testRegion Expected covered area
     * @param layerName Name of the layer to search
     */
    @JvmOverloads
    fun coversAtMostRegion(
        testRegion: com.android.server.wm.traces.common.Region,
        layerName: String = ""
    ): LayerTraceEntrySubject = apply {
        coversAtMostRegion(testRegion.toAndroidRegion(), layerName)
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
        resultComputation: (Region) -> Unit
    ): LayerTraceEntrySubject = apply {
        exists(layerName)
        val filteredLayers = entry.flattenedLayers.filter { it.name.contains(layerName) }

        val jointRegion = Region()
        val visibleLayers = filteredLayers
            .filter { it.isVisible && !it.isHiddenByParent }
        visibleLayers.forEach {
            jointRegion.op(it.visibleRegion.toAndroidRegion(), Region.Op.UNION)
        }

        resultComputation(jointRegion)
    }

    /**
     * Checks if a layer containing the name [layerName] has a visible region of exactly
     * [expectedVisibleRegion].
     *
     * @param layerName Name of the layer to search
     * @param expectedVisibleRegion Expected visible region of the layer
     */
    fun hasVisibleRegion(
        layerName: String,
        expectedVisibleRegion: Region
    ): LayerTraceEntrySubject = apply {
        exists(layerName)
        var reason: Fact? = Fact.simpleFact("")
        var visibleRegion = com.android.server.wm.traces.common.Region()
        for (layer in entry.flattenedLayers) {
            if (layer.name.contains(layerName)) {
                if (layer.isHiddenByParent) {
                    reason = Fact.fact("Hidden by parent", layer.hiddenByParentReason)
                    continue
                }
                if (layer.isInvisible) {
                    reason = Fact.fact("Is Invisible", layer.visibilityReason)
                    continue
                }
                reason = null
                visibleRegion = layer.visibleRegion
                Truth.assertThat(visibleRegion.toAndroidRegion()).isEqualTo(expectedVisibleRegion)
                break
            }
        }

        if (reason != null) {
            fail(reason)
        }
    }

    /**
     * Checks if a layer containing the name [layerName] has a visible region of exactly
     * [expectedVisibleRegion].
     *
     * @param layerName Name of the layer to search
     * @param expectedVisibleRegion Expected visible region of the layer
     */
    fun hasVisibleRegion(
        layerName: String,
        expectedVisibleRegion: com.android.server.wm.traces.common.Region
    ): LayerTraceEntrySubject = apply {
        hasVisibleRegion(layerName, expectedVisibleRegion.toAndroidRegion())
    }

    /**
     * Checks if a layer containing the name [layerName] exists in the hierarchy.
     *
     * @param layerName Name of the layer to search
     */
    fun exists(layerName: String): LayerTraceEntrySubject = apply {
        val found = entry.flattenedLayers.any { it.name.contains(layerName) }
        if (!found) {
            fail("Could not find", layerName)
        }
    }

    /**
     * Checks if a layer containing the name [layerName] exists in the hierarchy.
     *
     * @param layerName Name of the layer to search
     */
    fun notExists(layerName: String): LayerTraceEntrySubject = apply {
        val found = entry.flattenedLayers.none { it.name.contains(layerName) }
        if (!found) {
            fail("Could find", layerName)
        }
    }

    /**
     * Checks if a layer with name [layerName] is visible.
     *
     * @param layerName Name of the layer to search
     */
    fun isVisible(layerName: String): LayerTraceEntrySubject = apply {
        exists(layerName)
        var reason: Fact? = null
        for (layer in entry.flattenedLayers) {
            if (layer.name.contains(layerName)) {
                if (layer.isHiddenByParent) {
                    reason = Fact.fact("Hidden by parent", layer.hiddenByParentReason)
                    continue
                }
                if (layer.isInvisible) {
                    reason = Fact.fact("Is Invisible", layer.visibilityReason)
                    continue
                }
                reason = null
            }
        }

        if (reason != null) {
            fail(reason)
        }
    }

    /**
     * Checks if a layer with name [layerName] is not visible.
     *
     * @param layerName Name of the layer to search
     */
    fun isInvisible(layerName: String): LayerTraceEntrySubject = apply {
        try {
            isVisible(layerName)
        } catch (e: FlickerSubjectException) {
            val cause = e.cause
            require(cause is AssertionError)
            ExpectFailure.assertThat(cause).factKeys().isNotEmpty()
            return@apply
        }
        fail("Layer is visible", layerName)
    }

    /**
     * @return LayerSubject that can be used to make assertions on a single layer matching
     * [name] and [frameNumber].
     */
    fun layer(name: String, frameNumber: Long): LayerSubject {
        return subjects.firstOrNull {
            it.layer?.name?.contains(name) == true &&
                it.layer.currFrame == frameNumber
        } ?: LayerSubject.assertThat(name, this)
    }

    override fun toString(): String {
        return "LayerTraceEntrySubject($entry)"
    }

    companion object {
        /**
         * Boiler-plate Subject.Factory for LayersTraceSubject
         */
        private fun getFactory(
            trace: LayersTraceSubject? = null
        ): Factory<Subject, LayerTraceEntry> =
            Factory { fm, subject -> LayerTraceEntrySubject(fm, subject, trace) }

        /**
         * User-defined entry point
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            entry: LayerTraceEntry,
            trace: LayersTraceSubject? = null
        ): LayerTraceEntrySubject {
            val strategy = FlickerFailureStrategy()
            val subject = StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                .about(getFactory(trace))
                .that(entry) as LayerTraceEntrySubject
            strategy.init(subject)
            return subject
        }

        /**
         * Static method for getting the subject factory (for use with assertAbout())
         */
        @JvmStatic
        @JvmOverloads
        fun entries(trace: LayersTraceSubject? = null): Factory<Subject, LayerTraceEntry> {
            return getFactory(trace)
        }
    }
}