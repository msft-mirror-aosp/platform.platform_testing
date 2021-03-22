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

import android.graphics.Rect
import android.graphics.Region
import com.android.server.wm.flicker.assertions.Assertion
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.android.server.wm.flicker.traces.FlickerSubjectException
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.parser.toAndroidRegion
import com.google.common.truth.ExpectFailure
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.FailureStrategy
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject

/**
 * Truth subject for [LayerTraceEntry] objects, used to make assertions over behaviors that
 * occur on a single SurfaceFlinger state.
 *
 * To make assertions over a specific state from a trace it is recommended to create a subject
 * using [LayersTraceSubject.assertThat](myTrace) and select the specific state using:
 *     [LayersTraceSubject.first]
 *     [LayersTraceSubject.last]
 *     [LayersTraceSubject.entry]
 *
 * Alternatively, it is also possible to use [LayerTraceEntrySubject.assertThat](myState) or
 * Truth.assertAbout([LayerTraceEntrySubject.getFactory]), however they will provide less debug
 * information because it uses Truth's default [FailureStrategy].
 *
 * Example:
 *    val trace = LayersTraceParser.parseFromTrace(myTraceFile)
 *    val subject = LayersTraceSubject.assertThat(trace).first()
 *        .contains("ValidLayer")
 *        .notContains("ImaginaryLayer")
 *        .coversExactly(DISPLAY_AREA)
 *        { myCustomAssertion(this) }
 */
class LayerTraceEntrySubject private constructor(
    fm: FailureMetadata,
    val entry: LayerTraceEntry,
    val trace: LayersTraceSubject?
) : FlickerSubject(fm, entry) {
    override val defaultFacts: String = "${trace?.defaultFacts ?: ""}\nEntry: $entry"

    val subjects by lazy {
        entry.flattenedLayers.map { LayerSubject.assertThat(it, this) }
    }

    /**
     * Executes a custom [assertion] on the current subject
     */
    operator fun invoke(assertion: Assertion<LayerTraceEntry>): LayerTraceEntrySubject = apply {
        assertion(this.entry)
    }

    /**
     * Asserts that current entry subject has an [LayerTraceEntry.timestamp] equals to
     * [timestamp]
     */
    fun hasTimestamp(timestamp: Long): LayerTraceEntrySubject = apply {
        check("Wrong  entry timestamp").that(entry.timestamp).isEqualTo(timestamp)
    }

    /**
     * Asserts that the current SurfaceFlinger state doesn't contain layers
     */
    fun isEmpty(): LayerTraceEntrySubject = apply {
        check("Entry should not be empty")
            .that(entry.flattenedLayers)
            .isEmpty()
    }

    /**
     * Asserts that the current SurfaceFlinger state contains layers
     */
    fun isNotEmpty(): LayerTraceEntrySubject = apply {
        check("Entry should not be empty")
            .that(entry.flattenedLayers)
            .isNotEmpty()
    }

    /**
     * Asserts that the current SurfaceFlinger state has [numberLayers] layers
     */
    fun hasLayersSize(numberLayers: Int): LayerTraceEntrySubject = apply {
        check("Wrong number of layers in entry")
            .that(entry.flattenedLayers.size)
            .isEqualTo(numberLayers)
    }

    /**
     * Asserts that the visible area covered by any [Layer] with [Layer.name] containing
     * [layerName] covers at least [testRegion], that is, if its area of the layer's visible
     * region covers each point in the region.
     *
     * @param testRegion Expected covered area
     * @param layerName Name of the layer to search
     */
    @JvmOverloads
    fun coversAtLeast(
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
     * Asserts that the visible area covered by any [Layer] with [Layer.name] containing
     * [layerName] covers at least [testRegion], that is, if its area of the layer's visible
     * region covers each point in the region.
     *
     * @param testRegion Expected covered area
     * @param layerName Name of the layer to search
     */
    @JvmOverloads
    fun coversAtLeast(
        testRegion: com.android.server.wm.traces.common.Region,
        layerName: String = ""
    ): LayerTraceEntrySubject = apply {
        coversAtLeast(testRegion.toAndroidRegion(), layerName)
    }

    /**
     * Asserts that the visible area covered by any [Layer] with [Layer.name] containing
     * [layerName] covers at least [testRect], that is, if its area of the layer's visible
     * region covers each point in the region.
     *
     * @param testRect Expected covered area
     * @param layerName Name of the layer to search
     */
    @JvmOverloads
    fun coversAtLeast(
        testRect: com.android.server.wm.traces.common.Rect,
        layerName: String = ""
    ): LayerTraceEntrySubject = apply {
        coversAtLeast(com.android.server.wm.traces.common.Region(testRect), layerName)
    }

    /**
     * Asserts that the visible area covered by any [Layer] with [Layer.name] containing
     * [layerName] covers at least [testRect], that is, if its area of the layer's visible
     * region covers each point in the region.
     *
     * @param testRect Expected covered area
     * @param layerName Name of the layer to search
     */
    @JvmOverloads
    fun coversAtLeast(
        testRect: Rect,
        layerName: String = ""
    ): LayerTraceEntrySubject = apply {
        coversAtLeast(Region(testRect), layerName)
    }

    /**
     * Asserts that the visible area covered by any [Layer] with [Layer.name] containing
     * [layerName] covers at most [testRegion], that is, if the area of any layer doesn't
     * cover any point outside of [testRegion].
     *
     * @param testRegion Expected covered area
     * @param layerName Name of the layer to search
     */
    @JvmOverloads
    fun coversAtMost(
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
     * Asserts that the visible area covered by any [Layer] with [Layer.name] containing
     * [layerName] covers at most [testRegion], that is, if the area of any layer doesn't
     * cover any point outside of [testRegion].
     *
     * @param testRegion Expected covered area
     * @param layerName Name of the layer to search
     */
    @JvmOverloads
    fun coversAtMost(
        testRegion: com.android.server.wm.traces.common.Region,
        layerName: String = ""
    ): LayerTraceEntrySubject = apply {
        coversAtMost(testRegion.toAndroidRegion(), layerName)
    }

    /**
     * Asserts that the visible area covered by any [Layer] with [Layer.name] containing
     * [layerName] covers at most [testRect], that is, if the area of any layer doesn't
     * cover any point outside of [testRect].
     *
     * @param testRect Expected covered area
     * @param layerName Name of the layer to search
     */
    @JvmOverloads
    fun coversAtMost(
        testRect: com.android.server.wm.traces.common.Rect,
        layerName: String = ""
    ): LayerTraceEntrySubject = apply {
        coversAtMost(com.android.server.wm.traces.common.Region(testRect), layerName)
    }

    /**
     * Asserts that the visible area covered by any [Layer] with [Layer.name] containing
     * [layerName] covers at most [testRect], that is, if the area of any layer doesn't
     * cover any point outside of [testRect].
     *
     * @param testRect Expected covered area
     * @param layerName Name of the layer to search
     */
    @JvmOverloads
    fun coversAtMost(
        testRect: Rect,
        layerName: String = ""
    ): LayerTraceEntrySubject = apply {
        coversAtMost(Region(testRect), layerName)
    }

    /**
     * Obtains the region occupied by all layers with name containing [layerName] and applies
     * [resultPredicate] on the result
     *
     * @param layerName Name of the layer to search
     * @param resultPredicate Predicates to compute a result based on the found layer's region
     */
    @JvmOverloads
    fun covers(
        layerName: String = "",
        resultPredicate: (Region) -> Unit
    ): LayerTraceEntrySubject = apply {
        contains(layerName)
        val filteredLayers = entry.flattenedLayers.filter { it.name.contains(layerName) }

        val jointRegion = Region()
        val visibleLayers = filteredLayers
            .filter { it.isVisible && !it.isHiddenByParent }
        visibleLayers.forEach {
            jointRegion.op(it.visibleRegion.toAndroidRegion(), Region.Op.UNION)
        }

        resultPredicate(jointRegion)
    }

    /**
     * Asserts that a [Layer] with [Layer.name] containing [layerName] has a visible region
     * of exactly [expectedVisibleRegion].
     *
     * @param layerName Name of the layer to search
     * @param expectedVisibleRegion Expected visible region of the layer
     */
    @JvmOverloads
    fun coversExactly(
        expectedVisibleRegion: Region,
        layerName: String = ""
    ): LayerTraceEntrySubject = apply {
        contains(layerName)
        var reason: Fact? = null
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
                check("Incorrect visible region")
                    .that(visibleRegion.toAndroidRegion())
                    .isEqualTo(expectedVisibleRegion)
                break
            }
        }

        if (reason != null) {
            fail(reason)
        }
    }

    /**
     * Asserts that a [Layer] with [Layer.name] containing [layerName] has a visible region
     * of exactly [expectedVisibleRegion].
     *
     * @param layerName Name of the layer to search
     * @param expectedVisibleRegion Expected visible region of the layer
     */
    @JvmOverloads
    fun coversExactly(
        expectedVisibleRegion: com.android.server.wm.traces.common.Region,
        layerName: String = ""
    ): LayerTraceEntrySubject = apply {
        coversExactly(expectedVisibleRegion.toAndroidRegion(), layerName)
    }

    /**
     * Asserts that the SurfaceFlinger state contains a [Layer] with [Layer.name] containing
     * [layerName].
     *
     * @param layerName Name of the layer to search
     */
    fun contains(layerName: String): LayerTraceEntrySubject = apply {
        val found = entry.flattenedLayers.any { it.name.contains(layerName) }
        if (!found) {
            fail("Could not find", layerName)
        }
    }

    /**
     * Asserts that the SurfaceFlinger state doesn't contain a [Layer] with [Layer.name] containing
     *
     * @param layerName Name of the layer to search
     */
    fun notContains(layerName: String): LayerTraceEntrySubject = apply {
        val found = entry.flattenedLayers.none { it.name.contains(layerName) }
        if (!found) {
            fail("Could find", layerName)
        }
    }

    /**
     * Asserts that a [Layer] with [Layer.name] containing [layerName] is visible.
     *
     * @param layerName Name of the layer to search
     */
    fun isVisible(layerName: String): LayerTraceEntrySubject = apply {
        contains(layerName)
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
     * Asserts that a [Layer] with [Layer.name] containing [layerName] doesn't exist or
     * is invisible.
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
     * Obtains a [LayerSubject] for the first occurrence of a [Layer] with [Layer.name]
     * containing [name] in [frameNumber].
     *
     * Always returns a subject, event when the layer doesn't exist. To verify if layer
     * actually exists in the hierarchy use [LayerSubject.exists] or [LayerSubject.doesNotExist]
     *
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
         * Creates a [LayerTraceEntrySubject] to representing a SurfaceFlinger state[entry],
         * which can be used to make assertions.
         *
         * @param entry SurfaceFlinger trace entry
         * @param trace Trace that contains this entry (optional)
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