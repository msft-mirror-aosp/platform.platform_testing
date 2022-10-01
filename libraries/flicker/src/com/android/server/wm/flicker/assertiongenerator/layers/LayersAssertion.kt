package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.assertions.AssertionsChecker
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.service.assertors.ComponentTypeMatcher
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.traces.common.ITrace
import com.android.server.wm.traces.common.ITraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transition.Transition

class LayersAssertion(
) : Assertion {
    override val assertionsChecker: AssertionsChecker<LayerTraceEntrySubject> = AssertionsChecker()
    override var name: String = ""
    override var assertionString: String = ""

    val needsInitialization: Boolean
    get() {
        if (lateinitComponentMatchers.size == 0) {
            return false
        }
        return true
    }

    val lateinitComponentMatchers: MutableList<ComponentTypeMatcher> = mutableListOf()

    private fun initializeComponentMatchers(transition: Transition) {
        lateinitComponentMatchers.forEach { componentMatcher ->
            componentMatcher.initialize(transition)
        }
    }

    override fun execute(newTrace: ITrace<out ITraceEntry>, transition: Transition) {
        require(newTrace is LayersTrace) {
            "Requires a trace of type 'LayersTrace' to execute LayersAssertion."
        }
        execute(LayersTraceSubject.assertThat(newTrace), transition)
    }

    override fun execute(
        traceSubject: FlickerTraceSubject<out FlickerSubject>,
        transition: Transition
    ) {
        require(traceSubject is LayersTraceSubject) {
            "Requires a traceSubject of type 'LayersTraceSubject' to execute LayersAssertion."
        }
        if (needsInitialization && transition == Transition.emptyTransition()) {
            throw RuntimeException("At least one assertion component matcher needs " +
                "initialization, but the passed transition is empty")
        }
        transition.run{
            initializeComponentMatchers(transition)
        }
        assertionsChecker.assertChanges(traceSubject.subjects)
    }

    override fun toString(): String {
        return assertionString
    }

    override fun toString(newTrace: String): String {
        return "assertThat($newTrace)$assertionString"
    }

    override fun isEqual(other: Any?): Boolean {
        if (other !is LayersAssertion) {
            return false
        }
        return assertionsChecker.isEqual(other.assertionsChecker) &&
            name == other.name &&
            assertionString == other.assertionString
    }
}
