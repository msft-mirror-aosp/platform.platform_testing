package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.assertions.AssertionsChecker
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.traces.common.ITrace
import com.android.server.wm.traces.common.ITraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace

class LayersAssertion(
) : Assertion {
    override val assertionsChecker: AssertionsChecker<LayerTraceEntrySubject> = AssertionsChecker()
    override var name: String = ""
    override var assertionString: String = ""

    override fun execute(newTrace: ITrace<out ITraceEntry>) {
        require(newTrace is LayersTrace) {
            "Requires a trace of type 'LayersTrace' to execute LayersAssertion."
        }
        assertionsChecker.assertChanges(newTrace.entries.toList().map { entry ->
            LayerTraceEntrySubject.assertThat(entry)
        }) // layersTraceSubject.subjects
    }

    override fun execute(traceSubject: FlickerTraceSubject<out FlickerSubject>) {
        require(traceSubject is LayersTraceSubject) {
            "Requires a traceSubject of type 'LayersTraceSubject' to execute LayersAssertion."
        }
        assertionsChecker.assertChanges(traceSubject.subjects)
    }

    override fun toString(newTrace: String): String {
        return "assertThat($newTrace)$assertionString"
    }

    override fun isEqual(other: Any?): Boolean {
        return other is LayersAssertion &&
            assertionsChecker.isEqual(other.assertionsChecker) &&
            name == other.name &&
            assertionString == other.assertionString
    }
}
