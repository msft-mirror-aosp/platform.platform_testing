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
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.android.server.wm.flicker.traces.region.RegionSubject
import com.android.server.wm.traces.common.IComponentMatcher
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.layers.BaseLayerTraceEntry
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.LayersTrace
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.FailureStrategy
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

/**
 * Truth subject for [BaseLayerTraceEntry] objects, used to make assertions over behaviors that
 * occur on a single SurfaceFlinger state.
 *
 * To make assertions over a specific state from a trace it is recommended to create a subject using
 * [LayersTraceSubject.assertThat](myTrace) and select the specific state using:
 * ```
 *     [LayersTraceSubject.first]
 *     [LayersTraceSubject.last]
 *     [LayersTraceSubject.entry]
 * ```
 * Alternatively, it is also possible to use [LayerTraceEntrySubject.assertThat](myState) or
 * Truth.assertAbout([LayerTraceEntrySubject.getFactory]), however they will provide less debug
 * information because it uses Truth's default [FailureStrategy].
 *
 * Example:
 * ```
 *    val trace = LayersTraceParser().parse(myTraceFile)
 *    val subject = LayersTraceSubject.assertThat(trace).first()
 *        .contains("ValidLayer")
 *        .notContains("ImaginaryLayer")
 *        .coversExactly(DISPLAY_AREA)
 *        .invoke { myCustomAssertion(this) }
 * ```
 */
class LayerTraceEntrySubject
private constructor(
    fm: FailureMetadata,
    val entry: BaseLayerTraceEntry,
    val trace: LayersTrace?,
    override val parent: FlickerSubject?
) : FlickerSubject(fm, entry), ILayerSubject<LayerTraceEntrySubject, RegionSubject> {
    override val timestamp: Timestamp
        get() = entry.timestamp
    override val selfFacts = listOf(Fact.fact("SF State", entry))

    val subjects by lazy {
        entry.flattenedLayers.map { LayerSubject.assertThat(it, this, timestamp) }
    }

    /** Executes a custom [assertion] on the current subject */
    operator fun invoke(assertion: Assertion<BaseLayerTraceEntry>): LayerTraceEntrySubject = apply {
        assertion(this.entry)
    }

    /** {@inheritDoc} */
    override fun isEmpty(): LayerTraceEntrySubject = apply {
        check("SF State").that(entry.flattenedLayers).isEmpty()
    }

    /** {@inheritDoc} */
    override fun isNotEmpty(): LayerTraceEntrySubject = apply {
        check("SF State").that(entry.flattenedLayers).isNotEmpty()
    }

    /** See [visibleRegion] */
    fun visibleRegion(): RegionSubject =
        visibleRegion(componentMatcher = null, useCompositionEngineRegionOnly = true)

    /** See [visibleRegion] */
    fun visibleRegion(componentMatcher: IComponentMatcher): RegionSubject =
        visibleRegion(componentMatcher, useCompositionEngineRegionOnly = true)

    /** {@inheritDoc} */
    override fun visibleRegion(
        componentMatcher: IComponentMatcher?,
        useCompositionEngineRegionOnly: Boolean
    ): RegionSubject {
        val selectedLayers =
            if (componentMatcher == null) {
                // No filters so use all subjects
                subjects
            } else {
                subjects.filter { it.layer != null && componentMatcher.layerMatchesAnyOf(it.layer) }
            }

        if (selectedLayers.isEmpty()) {
            val str = componentMatcher?.toLayerIdentifier() ?: "<any>"
            fail(
                listOf(
                    Fact.fact(ASSERTION_TAG, "visibleRegion($str)"),
                    Fact.fact("Use composition engine region", useCompositionEngineRegionOnly),
                    Fact.fact("Could not find layers", str)
                )
            )
        }

        val visibleLayers = selectedLayers.filter { it.isVisible }
        return if (useCompositionEngineRegionOnly) {
            val visibleAreas = visibleLayers.mapNotNull { it.layer?.visibleRegion }.toTypedArray()
            RegionSubject.assertThat(visibleAreas, this, timestamp)
        } else {
            val visibleAreas = visibleLayers.mapNotNull { it.layer?.screenBounds }.toTypedArray()
            RegionSubject.assertThat(visibleAreas, this, timestamp)
        }
    }

    /** {@inheritDoc} */
    override fun contains(componentMatcher: IComponentMatcher): LayerTraceEntrySubject = apply {
        val found = componentMatcher.layerMatchesAnyOf(entry.flattenedLayers)
        if (!found) {
            fail(
                Fact.fact(ASSERTION_TAG, "contains(${componentMatcher.toLayerIdentifier()})"),
                Fact.fact("Could not find", componentMatcher.toLayerIdentifier())
            )
        }
    }

    /** {@inheritDoc} */
    override fun notContains(componentMatcher: IComponentMatcher): LayerTraceEntrySubject = apply {
        val layers = subjects.mapNotNull { it.layer }
        val notContainsComponent =
            componentMatcher.check(layers) { matchedLayers -> matchedLayers.isEmpty() }

        if (notContainsComponent) {
            return@apply
        }

        val failedEntries =
            subjects.filter { it.layer != null && componentMatcher.layerMatchesAnyOf(it.layer) }
        val failureFacts =
            mutableListOf(
                Fact.fact(ASSERTION_TAG, "notContains(${componentMatcher.toLayerIdentifier()})")
            )
        failedEntries.forEach { entry -> failureFacts.add(Fact.fact("Found", entry)) }
        failedEntries.firstOrNull()?.fail(failureFacts)
    }

    /** {@inheritDoc} */
    override fun isVisible(componentMatcher: IComponentMatcher): LayerTraceEntrySubject = apply {
        contains(componentMatcher)
        val layers = subjects.mapNotNull { it.layer }
        val hasVisibleSubject =
            componentMatcher.check(layers) { matchedLayers ->
                matchedLayers.any { layer -> layer.isVisible }
            }

        if (hasVisibleSubject) {
            return@apply
        }

        val matchingSubjects =
            subjects.filter { it.layer != null && componentMatcher.layerMatchesAnyOf(it.layer) }
        val failedEntries = matchingSubjects.filter { it.isInvisible }
        val failureFacts =
            mutableListOf(
                Fact.fact(ASSERTION_TAG, "isVisible(${componentMatcher.toLayerIdentifier()})")
            )

        failedEntries.forEach { entry ->
            failureFacts.add(Fact.fact("Is Invisible", entry))
            failureFacts.addAll(entry.visibilityReason.map { Fact.fact("Invisibility reason", it) })
        }
        failedEntries.firstOrNull()?.fail(failureFacts)
    }

    /** {@inheritDoc} */
    override fun isInvisible(componentMatcher: IComponentMatcher): LayerTraceEntrySubject = apply {
        val layers = subjects.mapNotNull { it.layer }
        val hasInvisibleComponent =
            componentMatcher.check(layers) { componentLayers ->
                componentLayers.all { layer ->
                    subjects.first { subject -> subject.layer == layer }.isInvisible
                }
            }

        if (hasInvisibleComponent) {
            return@apply
        }

        val matchingSubjects =
            subjects.filter { it.layer != null && componentMatcher.layerMatchesAnyOf(it.layer) }
        val failedEntries = matchingSubjects.filter { it.isVisible }
        val failureFacts =
            mutableListOf(
                Fact.fact(ASSERTION_TAG, "isInvisible(${componentMatcher.toLayerIdentifier()})")
            )
        failureFacts.addAll(failedEntries.map { Fact.fact("Is Visible", it) })
        failedEntries.firstOrNull()?.fail(failureFacts)
    }

    /** {@inheritDoc} */
    override fun isSplashScreenVisibleFor(
        componentMatcher: IComponentMatcher
    ): LayerTraceEntrySubject = apply {
        var target: FlickerSubject? = null
        var reason: Fact? = null

        val matchingLayer =
            subjects.mapNotNull { it.layer }.filter { componentMatcher.layerMatchesAnyOf(it) }
        val matchingActivityRecords = matchingLayer.mapNotNull { getActivityRecordFor(it) }

        if (matchingActivityRecords.isEmpty()) {
            fail(
                Fact.fact(
                    ASSERTION_TAG,
                    "isSplashScreenVisibleFor(${componentMatcher.toLayerIdentifier()})"
                ),
                Fact.fact(
                    "Could not find Activity Record layer",
                    componentMatcher.toLayerIdentifier()
                )
            )
            return this
        }

        // Check the matched activity record layers for containing splash screens
        for (layer in matchingActivityRecords) {
            val splashScreenContainers = layer.children.filter { it.name.contains("Splash Screen") }
            val splashScreenLayers =
                splashScreenContainers.flatMap {
                    it.children.filter { childLayer -> childLayer.name.contains("Splash Screen") }
                }

            if (splashScreenLayers.all { it.isHiddenByParent || !it.isVisible }) {
                reason = Fact.fact("No splash screen visible for", layer.name)
                target = subjects.first { it.layer == layer }
                continue
            }
            reason = null
            target = null
            break
        }

        reason?.run {
            target?.fail(
                Fact.fact(
                    ASSERTION_TAG,
                    "isSplashScreenVisibleFor(${componentMatcher.toLayerIdentifier()})"
                ),
                reason
            )
        }
    }

    /** See [layer] */
    fun layer(componentMatcher: IComponentMatcher): LayerSubject {
        return layer { componentMatcher.layerMatchesAnyOf(it) }
    }

    /** {@inheritDoc} */
    override fun layer(name: String, frameNumber: Long): LayerSubject {
        return layer(name) { it.name.contains(name) && it.currFrame == frameNumber }
    }

    /**
     * Obtains a [LayerSubject] for the first occurrence of a [Layer] matching [predicate]
     *
     * Always returns a subject, event when the layer doesn't exist. To verify if layer actually
     * exists in the hierarchy use [LayerSubject.exists] or [LayerSubject.doesNotExist]
     *
     * @param predicate to search for a layer
     * @param name Name of the subject to use when not found (optional)
     *
     * @return [LayerSubject] that can be used to make assertions
     */
    @JvmOverloads
    fun layer(name: String = "", predicate: (Layer) -> Boolean): LayerSubject {
        return subjects.firstOrNull { it.layer?.run { predicate(this) } ?: false }
            ?: LayerSubject.assertThat(name, this, timestamp)
    }

    private fun getActivityRecordFor(layer: Layer): Layer? {
        if (layer.name.startsWith("ActivityRecord{")) {
            return layer
        }
        val parent = layer.parent ?: return null
        return getActivityRecordFor(parent)
    }

    override fun toString(): String {
        return "LayerTraceEntrySubject($entry)"
    }

    companion object {
        /** Boilerplate Subject.Factory for LayersTraceSubject */
        private fun getFactory(
            trace: LayersTrace?,
            parent: FlickerSubject?
        ): Factory<Subject, BaseLayerTraceEntry> = Factory { fm, subject ->
            LayerTraceEntrySubject(fm, subject, trace, parent)
        }

        /**
         * Creates a [LayerTraceEntrySubject] to representing a SurfaceFlinger state[entry], which
         * can be used to make assertions.
         *
         * @param entry SurfaceFlinger trace entry
         * @param parent Trace that contains this entry (optional)
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            entry: BaseLayerTraceEntry,
            trace: LayersTrace? = null,
            parent: FlickerSubject? = null
        ): LayerTraceEntrySubject {
            val strategy = FlickerFailureStrategy()
            val subject =
                StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                    .about(getFactory(trace, parent))
                    .that(entry) as LayerTraceEntrySubject
            strategy.init(subject)
            return subject
        }

        /** Static method for getting the subject factory (for use with assertAbout()) */
        @JvmStatic
        @JvmOverloads
        fun entries(
            trace: LayersTrace? = null,
            parent: FlickerSubject? = null
        ): Factory<Subject, BaseLayerTraceEntry> = getFactory(trace, parent)
    }
}
