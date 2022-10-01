package com.android.server.wm.flicker.assertiongenerator.common

import com.android.server.wm.flicker.assertions.AssertionsChecker
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.traces.common.ITrace
import com.android.server.wm.traces.common.ITraceEntry
import com.android.server.wm.traces.common.transition.Transition

interface Assertion {
    // Visible for testing
    val assertionsChecker: AssertionsChecker<out FlickerSubject>
    val name: String
    var assertionString: String

    // transition can be null only for testing purposes
    fun execute(newTrace: ITrace<out ITraceEntry>, transition: Transition)

    fun execute(
        traceSubject: FlickerTraceSubject<out FlickerSubject>,
        transition: Transition
    )

    fun toString(newTrace: String): String

    fun isEqual(other: Any?): Boolean
}
