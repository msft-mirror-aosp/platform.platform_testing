package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.Utils
import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.assertiongenerator.common.ITraceLifecycle
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.traces.common.layers.Layer

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
        val assertions = traceLifecycle.elementIds.map { elementId ->
            assertion = LayersAssertion()
            val elementLifecycle = traceLifecycle[elementId]
            previousIsVisible = null
            initializedAssertion = false
            val name = elementLifecycle?.let { Utils.getLayersElementLifecycleName(it) }
            // create the assertionsChecker for a lifecycle
            for (state in elementLifecycle?.states!!) {
                produceAssertionForState(state, name!!)
            }
            assertion
        }
        return assertions.filter { it.assertionString != ""}
    }

    private fun addToAssertionString(assertionStrToAdd: String, componentMatcherStr: String) {
        if (initializedAssertion) {
            assertion.assertionString += ".then()"
        }
        assertion.assertionString += ".$assertionStrToAdd($componentMatcherStr)"
    }

    private fun addAssertionFor(state: Layer?, name: String) {
        val componentMatcher = Utils.componentNameMatcherFromName(name) ?: return
        val componentMatcherStr = Utils.componentNameMatcherToString(componentMatcher)
        val visibility = state?.isVisible
        val assertionPair = visibilityAssertions[visibility]
        val assertionStrToAdd = assertionPair!!.assertionStrToAdd
        val assertionFunction = assertionPair.assertionFunction
        assertion.assertionsChecker.add("$assertionStrToAdd($name)", isOptional = false) {
            assertionFunction(it, componentMatcher)
        }
        addToAssertionString(assertionStrToAdd, componentMatcherStr)
    }

    private fun setPrevious(visibility: Boolean?) {
        this.previousIsVisible = visibility
        initializedAssertion = true
    }

    private fun produceAssertionForState(
        state: Layer?,
        name: String
    ) {
        val previousIsVisible = previousIsVisible
        val componentMatcher = Utils.componentNameMatcherFromName(name) ?: return
        val visibility = state?.isVisible
        if (!initializedAssertion || previousIsVisible != visibility) {
            addAssertionFor(state, name)
        }
        setPrevious(visibility)
    }
}
