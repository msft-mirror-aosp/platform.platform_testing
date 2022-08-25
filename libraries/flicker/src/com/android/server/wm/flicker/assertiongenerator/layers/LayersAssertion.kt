package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.assertions.AssertionsChecker
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject.Companion.assertThat
import com.android.server.wm.traces.common.ITrace
import com.android.server.wm.traces.common.ITraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace

class LayersAssertion(
    override val assertionsChecker: AssertionsChecker<out FlickerSubject>
) : Assertion() {
    override fun execute(newTrace: ITrace<ITraceEntry>) {
        val newLayersTrace: LayersTrace = newTrace as LayersTrace
        val subject = assertThat(newLayersTrace)
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        TODO()
    }
}
