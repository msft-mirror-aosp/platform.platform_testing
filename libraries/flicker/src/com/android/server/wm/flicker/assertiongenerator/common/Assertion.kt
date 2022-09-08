package com.android.server.wm.flicker.assertiongenerator.common

import com.android.server.wm.flicker.assertions.AssertionsChecker
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.traces.common.ITrace
import com.android.server.wm.traces.common.ITraceEntry

interface Assertion {
    val assertionsChecker: AssertionsChecker<out FlickerSubject>
    val name: String
    var assertionString: String

    fun execute(newTrace: ITrace<out ITraceEntry>)

    fun execute(traceSubject: FlickerTraceSubject<out FlickerSubject>)

    fun toString(newTrace: String): String

    fun isEqual(other: Any?): Boolean
}
