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

import com.android.server.wm.flicker.assertions.Assertion
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.flicker.traces.region.RegionTraceSubject
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.EdgeExtensionComponentMatcher
import com.android.server.wm.traces.common.IComponentMatcher
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.region.RegionTrace
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.FailureStrategy
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

/**
 * Truth subject for [LayersTrace] objects, used to make assertions over behaviors that occur
 * throughout a whole trace
 *
 * To make assertions over a trace it is recommended to create a subject using
 * [LayersTraceSubject.assertThat](myTrace). Alternatively, it is also possible to use
 * Truth.assertAbout(LayersTraceSubject.FACTORY), however it will provide less debug
 * information because it uses Truth's default [FailureStrategy].
 *
 * Example:
 *    val trace = LayersTraceParser.parseFromTrace(myTraceFile)
 *    val subject = LayersTraceSubject.assertThat(trace)
 *        .contains("ValidLayer")
 *        .notContains("ImaginaryLayer")
 *        .coversExactly(DISPLAY_AREA)
 *        .forAllEntries()
 *
 * Example2:
 *    val trace = LayersTraceParser.parseFromTrace(myTraceFile)
 *    val subject = LayersTraceSubject.assertThat(trace) {
 *        check("Custom check") { myCustomAssertion(this) }
 *    }
 */
class LayersTraceSubject private constructor(
    fm: FailureMetadata,
    val trace: LayersTrace,
    override val parent: LayersTraceSubject?,
    val facts: Collection<Fact>
) : FlickerTraceSubject<LayerTraceEntrySubject>(fm, trace),
    ILayerSubject<LayersTraceSubject, RegionTraceSubject> {

    override val selfFacts by lazy {
        val allFacts = super.selfFacts.toMutableList()
        allFacts.addAll(facts)
        allFacts
    }

    override val subjects by lazy {
        trace.entries.map { LayerTraceEntrySubject.assertThat(it, trace, this) }
    }

    /** {@inheritDoc} */
    override fun then(): LayersTraceSubject = apply { super.then() }

    /** {@inheritDoc} */
    override fun isEmpty(): LayersTraceSubject = apply {
        check("Trace").that(trace).isEmpty()
    }

    /** {@inheritDoc} */
    override fun isNotEmpty(): LayersTraceSubject = apply {
        check("Trace").that(trace).isNotEmpty()
    }

    /** {@inheritDoc} */
    override fun layer(name: String, frameNumber: Long): LayerSubject {
        return subjects
            .map { it.layer(name, frameNumber) }
            .firstOrNull { it.isNotEmpty }
            ?: LayerSubject.assertThat(
                null, this,
                timestamp = subjects.firstOrNull()?.entry?.timestamp ?: 0L
            )
    }

    /**
     * @return List of [LayerSubject]s matching [name] in the order they appear on the trace
     */
    fun layers(name: String): List<LayerSubject> =
        subjects
            .map { it.layer { layer -> layer.name.contains(name) } }
            .filter { it.isNotEmpty }

    /**
     * @return List of [LayerSubject]s matching [predicate] in the order they appear on the trace
     */
    fun layers(predicate: (Layer) -> Boolean): List<LayerSubject> =
        subjects
            .map { it.layer { layer -> predicate(layer) } }
            .filter { it.isNotEmpty }

    /**
     * Checks that all visible layers are shown for more than one consecutive entry
     */
    @JvmOverloads
    fun visibleLayersShownMoreThanOneConsecutiveEntry(
        ignoreLayers: List<IComponentMatcher> = listOf(
            ComponentNameMatcher.SPLASH_SCREEN,
            ComponentNameMatcher.SNAPSHOT,
            ComponentNameMatcher.IME_SNAPSHOT,
            EdgeExtensionComponentMatcher()
        )
    ): LayersTraceSubject = apply {
        visibleEntriesShownMoreThanOneConsecutiveTime { subject ->
            subject.entry.visibleLayers
                .filter {
                    ignoreLayers.none { componentMatcher ->
                        componentMatcher.layerMatchesAnyOf(it)
                    }
                }
                .map { it.name }
                .toSet()
        }
    }

    /** {@inheritDoc} */
    override fun notContains(componentMatcher: IComponentMatcher): LayersTraceSubject =
        notContains(componentMatcher, isOptional = false)

    /**
     * See [notContains]
     */
    fun notContains(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): LayersTraceSubject =
        apply {
            addAssertion("notContains(${componentMatcher.toLayerIdentifier()})", isOptional) {
                it.notContains(componentMatcher)
            }
        }

    /** {@inheritDoc} */
    override fun contains(componentMatcher: IComponentMatcher): LayersTraceSubject =
        contains(componentMatcher, isOptional = false)

    /**
     * See [contains]
     */
    fun contains(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): LayersTraceSubject =
        apply {
            addAssertion("contains(${componentMatcher.toLayerIdentifier()})", isOptional) {
                it.contains(componentMatcher)
            }
        }

    /** {@inheritDoc} */
    override fun isVisible(componentMatcher: IComponentMatcher): LayersTraceSubject =
        isVisible(componentMatcher, isOptional = false)

    /**
     * See [isVisible]
     */
    fun isVisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): LayersTraceSubject =
        apply {
            addAssertion("isVisible(${componentMatcher.toLayerIdentifier()})", isOptional) {
                it.isVisible(componentMatcher)
            }
        }

    /** {@inheritDoc} */
    override fun isInvisible(componentMatcher: IComponentMatcher): LayersTraceSubject =
        isInvisible(componentMatcher, isOptional = false)

    /**
     * See [isInvisible]
     */
    fun isInvisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): LayersTraceSubject =
        apply {
            addAssertion("isInvisible(${componentMatcher.toLayerIdentifier()})", isOptional) {
                it.isInvisible(componentMatcher)
            }
        }

    /** {@inheritDoc} */
    override fun isSplashScreenVisibleFor(
        componentMatcher: IComponentMatcher
    ): LayersTraceSubject =
        isSplashScreenVisibleFor(componentMatcher, isOptional = false)

    /**
     * See [isSplashScreenVisibleFor]
     */
    fun isSplashScreenVisibleFor(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): LayersTraceSubject =
        apply {
            addAssertion(
                "isSplashScreenVisibleFor(${componentMatcher.toLayerIdentifier()})",
                isOptional
            ) {
                it.isSplashScreenVisibleFor(componentMatcher)
            }
        }

    /**
     * See [visibleRegion]
     */
    fun visibleRegion(): RegionTraceSubject =
        visibleRegion(componentMatcher = null, useCompositionEngineRegionOnly = false)

    /**
     * See [visibleRegion]
     */
    fun visibleRegion(componentMatcher: IComponentMatcher?): RegionTraceSubject =
        visibleRegion(componentMatcher, useCompositionEngineRegionOnly = false)

    /** {@inheritDoc} */
    override fun visibleRegion(
        componentMatcher: IComponentMatcher?,
        useCompositionEngineRegionOnly: Boolean
    ): RegionTraceSubject {
        val regionTrace = RegionTrace(componentMatcher,
            subjects.map {
                it.visibleRegion(componentMatcher, useCompositionEngineRegionOnly)
                    .regionEntry
            }.toTypedArray()
        )
        return RegionTraceSubject.assertThat(regionTrace, this)
    }

    /**
     * Executes a custom [assertion] on the current subject
     */
    @JvmOverloads
    operator fun invoke(
        name: String,
        isOptional: Boolean = false,
        assertion: Assertion<LayerTraceEntrySubject>
    ): LayersTraceSubject = apply { addAssertion(name, isOptional, assertion) }

    fun hasFrameSequence(
        name: String,
        frameNumbers: Iterable<Long>
    ): LayersTraceSubject = hasFrameSequence(ComponentNameMatcher("", name), frameNumbers)

    fun hasFrameSequence(
        componentMatcher: IComponentMatcher,
        frameNumbers: Iterable<Long>
    ): LayersTraceSubject = apply {
        val firstFrame = frameNumbers.first()
        val entries = trace.entries.asSequence()
            // map entry to buffer layers with name
            .map { it.getLayerWithBuffer(componentMatcher) }
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
            val message = "Could not find Layer:" + componentMatcher.toLayerIdentifier() +
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
         * Boilerplate Subject.Factory for LayersTraceSubject
         */
        private fun getFactory(
            parent: LayersTraceSubject?,
            facts: Collection<Fact> = emptyList()
        ): Factory<Subject, LayersTrace> =
            Factory { fm, subject -> LayersTraceSubject(fm, subject, parent, facts) }

        /**
         * Creates a [LayersTraceSubject] to representing a SurfaceFlinger trace,
         * which can be used to make assertions.
         *
         * @param trace SurfaceFlinger trace
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            trace: LayersTrace,
            parent: LayersTraceSubject? = null,
            facts: Collection<Fact> = emptyList()
        ): LayersTraceSubject {
            val strategy = FlickerFailureStrategy()
            val subject = StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                .about(getFactory(parent, facts))
                .that(trace) as LayersTraceSubject
            strategy.init(subject)
            return subject
        }

        /**
         * Static method for getting the subject factory (for use with assertAbout())
         */
        @JvmStatic
        fun entries(
            parent: LayersTraceSubject?,
            facts: Collection<Fact> = emptyList()
        ): Factory<Subject, LayersTrace> {
            return getFactory(parent, facts)
        }
    }
}
