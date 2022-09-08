package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.Utils
import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.assertiongenerator.common.ITraceLifecycle
import com.android.server.wm.traces.common.layers.Layer

class LayersVisibilityAssertionProducer(
) : LayersAssertionProducer {
    var previousIsVisible: Boolean? = null
    var initializedAssertion: Boolean = false
    var assertion = LayersAssertion()

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

    private fun addThenToAssertionString() {
        if (initializedAssertion) {
            assertion.assertionString += ".then()"
        }
    }

    private fun produceAssertionForState(
        state: Layer?,
        name: String
    ) {
        val previousIsVisible = previousIsVisible
        val componentMatcher = Utils.componentNameMatcherFromName(name) ?: return
        val componentMatcherStr = Utils.componentNameMatcherToString(componentMatcher)
        if (state == null) {
            if (!initializedAssertion || (initializedAssertion && previousIsVisible != null)) {
                assertion.assertionsChecker
                    .add("notContains($name)",
                    isOptional = false) {
                    it.notContains(componentMatcher)
                }
                addThenToAssertionString()
                assertion.assertionString += ".notContains($componentMatcherStr)"
            }
            this.previousIsVisible = null
            initializedAssertion = true
        } else if (state.isVisible) {
            if (!initializedAssertion || (initializedAssertion && previousIsVisible != true)) {
                assertion.assertionsChecker
                    .add("isVisible($name)",
                        isOptional = false) {
                        it.isVisible(componentMatcher)
                    }
                addThenToAssertionString()
                assertion.assertionString += ".isVisible($componentMatcherStr)"
            }
            this.previousIsVisible = true
            initializedAssertion = true
        } else if (!state.isVisible) {
            if (!initializedAssertion || (initializedAssertion && previousIsVisible != false)) {
                assertion.assertionsChecker
                    .add("isInvisible($name)",
                        isOptional = false) {
                        it.isInvisible(componentMatcher)
                    }
                addThenToAssertionString()
                assertion.assertionString += ".isInvisible($componentMatcherStr)"
            }
            this.previousIsVisible = false
            initializedAssertion = true
        }
    }
}
