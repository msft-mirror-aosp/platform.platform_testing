package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.Utils
import com.android.server.wm.flicker.assertiongenerator.DeviceTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.assertiongenerator.common.TraceContent
import com.android.server.wm.flicker.service.assertors.ComponentTypeMatcher
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.traces.common.ComponentNameMatcher

class LayersVisibilityAssertionProducer() : LayersAssertionProducer {
    var previousIsVisible: Boolean? = null
    var initializedAssertion: Boolean = false
    var assertion = LayersAssertion()
    val visibilityAssertions: Map<Boolean?, LayersAssertionData> =
        mapOf(
            true to LayersAssertionData("isVisible", LayerTraceEntrySubject::isVisible),
            false to LayersAssertionData("isInvisible", LayerTraceEntrySubject::isInvisible),
            null to LayersAssertionData("notContains", LayerTraceEntrySubject::notContains)
        )

    /**
     * Return a list with assertions corresponding to the chains of visible/invisible asserts
     *
     * We receive all lifecycles from the assertion producer, but we only get the first trace of the
     * type we are interested in (layers), because all traces are supposed to depict the exact same
     * scenario
     */
    override fun produce(traceContents: List<TraceContent>): List<Assertion> {
        val layersTraceContents = traceContents.filterIsInstance<LayersTraceContent>()
        if (layersTraceContents.isEmpty()) {
            return listOf()
        }
        val traceContent = layersTraceContents[0]
        val traceLifecycle = traceContent.traceLifecycle
        val traceConfiguration = traceContent.traceConfiguration
        val assertions =
            traceLifecycle.elementIds.map { elementComponentMatcher ->
                assertion = LayersAssertion()
                val componentLifecycle = traceLifecycle[elementComponentMatcher]!!
                previousIsVisible = null
                initializedAssertion = false
                val traceLength = componentLifecycle.traceLength
                for (timeIndex in 0 until traceLength) {
                    produceAssertionForComponentMatcherAtTimeIndex(
                        timeIndex,
                        componentLifecycle,
                        elementComponentMatcher
                    )
                }
                assertion.name =
                    "LayersVisibility_" +
                        "${Utils.componentNameMatcherToStringSimplified(elementComponentMatcher)}"
                assertion
            }
        return assertions
    }

    /**
     * Extracts the actual component matcher from the given elementComponentMatcher if possible. The
     * componentMatcher is going to differ from the elementComponentMatcher if it doesn't have a
     * constant hardcoded name and corresponds to a type found in config (e.g. OPENING_APP,
     * CLOSING_APP)
     *
     * There are two cases when an element is not assertable (and null is returned):
     * 1. The elementComponentMatcher does not correspond to a hardcoded one
     * ```
     *    and traceConfiguration is missing.
     * ```
     * 2. The elementComponentMatcher corresponds to a ¨junk¨ element (neither hardcoded, nor of
     * special type in the configuration).
     */
    private fun getAssertionComponentMatcher(
        elementComponentMatcher: ComponentNameMatcher,
        traceConfiguration: DeviceTraceConfiguration
    ): ComponentNameMatcher? {
        return ComponentTypeMatcher.componentMatcherFromName(
            elementComponentMatcher.toString(),
            traceConfiguration
        )
    }

    private fun addToAssertionString(
        assertionStrToAdd: String,
        componentMatcher: ComponentNameMatcher
    ) {
        if (initializedAssertion) {
            assertion.assertionString += ".then()"
        }
        val componentMatcherStr = Utils.componentNameMatcherToString(componentMatcher)
        assertion.assertionString += ".$assertionStrToAdd($componentMatcherStr)"
    }

    /** Add assertion for pre-computed visibility */
    private fun addAssertionFor(
        visibility: Boolean?,
        componentMatcher: ComponentNameMatcher,
    ) {
        if (componentMatcher is ComponentTypeMatcher) {
            assertion.lateinitComponentMatchers.add(componentMatcher)
        }
        val assertionPair = visibilityAssertions[visibility]
        val assertionStrToAdd = assertionPair!!.assertionStrToAdd
        val assertionFunction = assertionPair.assertionFunction
        val assertionName = Utils.componentNameMatcherToStringSimplified(componentMatcher)
        assertion.assertionsChecker.add("$assertionStrToAdd($assertionName)", isOptional = false) {
            assertionFunction(it, componentMatcher)
        }
        addToAssertionString(assertionStrToAdd, componentMatcher)
    }

    private fun setPrevious(visibility: Boolean?) {
        this.previousIsVisible = visibility
        initializedAssertion = true
    }

    private fun visibilityAtTimeIndex(
        timeIndex: Int,
        componentLifecycle: LayersComponentLifecycle
    ): Boolean? {
        var visibility: Boolean? = null
        componentLifecycle.elementIds.forEach { elementId ->
            val layer = componentLifecycle[elementId]!!.states[timeIndex]
            layer?.run {
                if (this.isVisible) {
                    return true
                }
                visibility = false
            }
        }
        return visibility
    }

    /** Produce assertion for a given assertionComponentMatcher at a given timeIndex */
    private fun produceAssertionForComponentMatcherAtTimeIndex(
        timeIndex: Int,
        componentLifecycle: LayersComponentLifecycle,
        componentMatcher: ComponentNameMatcher,
    ) {
        val previousIsVisible = previousIsVisible
        val visibility: Boolean? = visibilityAtTimeIndex(timeIndex, componentLifecycle)
        if (!initializedAssertion || previousIsVisible != visibility) {
            addAssertionFor(visibility, componentMatcher)
        }
        setPrevious(visibility)
    }
}
