package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.Utils
import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.assertiongenerator.common.ITraceLifecycle
import com.android.server.wm.flicker.service.assertors.ComponentTypeMatcher
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject

class LayersVisibilityAssertionProducer(
) : LayersAssertionProducer {
    var previousIsVisible: Boolean? = null
    var initializedAssertion: Boolean = false
    var assertion = LayersAssertion()
    val visibilityAssertions: Map<Boolean?, LayersAssertionData> = mapOf(
        true to LayersAssertionData("isVisible", LayerTraceEntrySubject::isVisible),
        false to LayersAssertionData("isInvisible", LayerTraceEntrySubject::isInvisible),
        null to LayersAssertionData("notContains", LayerTraceEntrySubject::notContains)
    )

    /**
     * Return a list with a single assertion corresponding to the chain of visible/invisible asserts
     *
     * We receive all lifecycles from the assertion producer,
     * but we only get the first trace of the type we are interested in (layers),
     * because all traces are supposed to depict the exact same scenario
     */
    override fun produce(lifecycles: List<ITraceLifecycle>): List<Assertion> {
        val traceLifecycle = lifecycles.filterIsInstance<LayersTraceLifecycle>()[0]
        val assertions = traceLifecycle.elementIds.map { elementComponentMatcher ->
            assertion = LayersAssertion()
            val componentLifecycle = traceLifecycle[elementComponentMatcher]
            previousIsVisible = null
            initializedAssertion = false
            val name = componentLifecycle!!.getName()
            val traceLength = componentLifecycle.traceLength
            for (timeIndex in 0 until traceLength) {
                produceAssertionForComponentMatcherAtTimeIndex(
                    timeIndex,
                    componentLifecycle,
                    name
                )
            }
            if (initializedAssertion) {
                assertion
            } else {
                null
            }
        }
        return assertions.filterNotNull()
    }

    private fun addAssertionFor(visibility: Boolean?, name: String) {
        Utils.componentNameMatcherFromName(name) ?: return
        val componentMatcher = ComponentTypeMatcher(name)
        try {
            componentMatcher.earlyInitialize()
        } catch (err: RuntimeException) {
            assertion.needsInitialization = true
        }
        assertion.componentMatchers.add(componentMatcher)
        val assertionPair = visibilityAssertions[visibility]
        val assertionStrToAdd = assertionPair!!.assertionStrToAdd
        val assertionFunction = assertionPair.assertionFunction
        assertion.assertionsChecker.add("$assertionStrToAdd($name)", isOptional = false) {
            assertionFunction(it, componentMatcher)
        }
        assertion.assertionStringsToAdd.add(assertionStrToAdd)
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
        componentLifecycle.elementIds.forEach{ elementId ->
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

    private fun produceAssertionForComponentMatcherAtTimeIndex(
        timeIndex: Int,
        componentLifecycle: LayersComponentLifecycle,
        name: String
    ) {
        val previousIsVisible = previousIsVisible
        val componentMatcher = Utils.componentNameMatcherFromName(name) ?: return
        val visibility: Boolean? = visibilityAtTimeIndex(
            timeIndex,
            componentLifecycle
        )
        if (!initializedAssertion || previousIsVisible != visibility) {
            addAssertionFor(visibility, name)
        }
        setPrevious(visibility)
    }
}
