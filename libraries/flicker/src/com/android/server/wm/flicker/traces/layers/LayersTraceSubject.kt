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
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.traces.common.layers.LayersTrace
import com.google.common.truth.FailureMetadata
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

/** Truth subject for [LayersTrace] objects.  */
class LayersTraceSubject private constructor(
    fm: FailureMetadata,
    val trace: LayersTrace
) : FlickerTraceSubject<LayerTraceEntrySubject>(fm, trace) {
    override val defaultFacts: String by lazy {
        buildString {
            if (trace.hasSource()) {
                append("Path: ${trace.source}")
                append("\n")
            }
            append("Trace: $trace")
        }
    }

    override val subjects by lazy {
        trace.entries.map { LayerTraceEntrySubject.assertThat(it, this) }
    }

    /**
     * Signal that the last assertion set is complete. The next assertion added will start a new
     * set of assertions.
     *
     * E.g.: checkA().then().checkB()
     *
     * Will produce two sets of assertions (checkA) and (checkB) and checkB will only be checked
     * after checkA passes.
     */
    fun then(): LayersTraceSubject = apply {
        startAssertionBlock()
    }

    /**
     * Ignores the first entries in the trace, until the first assertion passes. If it reaches the
     * end of the trace without passing any assertion, return a failure with the name/reason from
     * the first assertion
     *
     * @return
     */
    fun skipUntilFirstAssertion(): LayersTraceSubject = apply {
        assertionsChecker.skipUntilFirstAssertion()
    }

    fun isEmpty(): LayersTraceSubject = apply {
        check("Trace is empty").that(trace).isEmpty()
    }

    fun isNotEmpty() = apply {
        check("Trace is not empty").that(trace).isNotEmpty()
    }

    /**
     * @return LayerSubject that can be used to make assertions on a single layer matching
     * [name] and [frameNumber].
     */
    fun layer(name: String, frameNumber: Long): LayerSubject {
        return subjects
            .map { it.layer(name, frameNumber) }
            .firstOrNull { it.isNotEmpty }
            ?: LayerSubject.assertThat(null)
    }

    @JvmOverloads
    fun coversAtLeastRegion(
        rect: Rect,
        layerName: String = ""
    ): LayersTraceSubject = this.coversAtLeastRegion(Region(rect), layerName)

    @JvmOverloads
    fun coversAtMostRegion(
        rect: Rect,
        layerName: String = ""
    ): LayersTraceSubject = this.coversAtMostRegion(Region(rect), layerName)

    @JvmOverloads
    fun coversAtLeastRegion(
        region: Region,
        layerName: String = ""
    ): LayersTraceSubject = apply {
        addAssertion("coversAtLeastRegion($region, $layerName)") {
            it.coversAtLeastRegion(region, layerName)
        }
    }

    @JvmOverloads
    fun coversAtMostRegion(
        region: Region,
        layerName: String = ""
    ): LayersTraceSubject = apply {
        addAssertion("coversAtMostRegion($region, $layerName") {
            it.coversAtMostRegion(region, layerName)
        }
    }

    /**
     * Checks that all visible layers are shown for more than one consecutive entry
     */
    @JvmOverloads
    fun visibleLayersShownMoreThanOneConsecutiveEntry(
        ignoreLayers: List<String> = emptyList()
    ): LayersTraceSubject = apply {
        visibleEntriesShownMoreThanOneConsecutiveTime { subject ->
            subject.entry.visibleLayers
                .filter { ignoreLayers.none { layerName -> layerName in it.name } }
                .map { it.name }
                .toSet()
        }
    }

    fun hasVisibleRegion(
        layerName: String,
        size: Region
    ): LayersTraceSubject = apply {
        addAssertion("hasVisibleRegion($layerName$size)") {
            it.hasVisibleRegion(layerName, size)
        }
    }

    fun hasNotLayer(layerName: String): LayersTraceSubject =
        apply {
            addAssertion("hasNotLayer($layerName)") {
                it.notExists(layerName)
            }
        }

    fun hasLayer(layerName: String): LayersTraceSubject =
        apply { addAssertion("hasLayer($layerName)") { it.exists(layerName) } }

    fun showsLayer(layerName: String): LayersTraceSubject =
        apply { addAssertion("showsLayer($layerName)") { it.isVisible(layerName) } }

    fun replaceVisibleLayer(
        previousLayerName: String,
        currentLayerName: String
    ): LayersTraceSubject = apply { hidesLayer(previousLayerName).showsLayer(currentLayerName) }

    fun hidesLayer(layerName: String): LayersTraceSubject =
        apply {
            addAssertion("hidesLayer($layerName)") {
                it.isInvisible(layerName)
            }
        }

    operator fun invoke(
        name: String,
        assertion: Assertion<LayerTraceEntrySubject>
    ): LayersTraceSubject = apply { addAssertion(name, assertion) }

    fun hasFrameSequence(name: String, frameNumbers: Iterable<Long>): LayersTraceSubject = apply {
        val firstFrame = frameNumbers.first()
        val entries = trace.entries.asSequence()
            // map entry to buffer layers with name
            .map { it.getLayerWithBuffer(name) }
            // removing all entries without the layer
            .filterNotNull()
            // removing all entries with the same frame number
            .distinctBy { it.currFrame }
            // drop until the first frame we are interested in
            .dropWhile { layer -> layer.currFrame != firstFrame }

        var numFound = 0
        val frameNumbersMatch = entries.zip(frameNumbers.asSequence()) { layer, frameNumber ->
            numFound++
            layer.currFrame == frameNumber
        }.all { it }
        val allFramesFound = frameNumbers.count() == numFound
        if (!allFramesFound || !frameNumbersMatch) {
            val message = "Could not find Layer:" + name +
                " with frame sequence:" + frameNumbers.joinToString(",") +
                " Found:\n" + entries.joinToString("\n")
            fail(message)
        }
    }

    /**
     * Run the assertions for all trace entries within the specified time range
     */
    fun forRange(startTime: Long, endTime: Long) {
        val subjectsInRange = subjects.filter { it.entry.timestamp in startTime..endTime }
        assertionsChecker.test(subjectsInRange)
    }

    /**
     * User-defined entry point for the trace entry with [timestamp]
     *
     * @param timestamp of the entry
     */
    fun entry(timestamp: Long): LayerTraceEntrySubject =
        subjects.first { it.entry.timestamp == timestamp }

    companion object {
        /**
         * Boiler-plate Subject.Factory for LayersTraceSubject
         */
        private val FACTORY: Factory<Subject, LayersTrace> =
            Factory { fm, subject -> LayersTraceSubject(fm, subject) }

        /**
         * User-defined entry point
         */
        @JvmStatic
        fun assertThat(trace: LayersTrace): LayersTraceSubject {
            val strategy = FlickerFailureStrategy()
            val subject = StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                .about(FACTORY)
                .that(trace) as LayersTraceSubject
            strategy.init(subject)
            return subject
        }

        /**
         * Static method for getting the subject factory (for use with assertAbout())
         */
        @JvmStatic
        fun entries(): Factory<Subject, LayersTrace> {
            return FACTORY
        }
    }
}
