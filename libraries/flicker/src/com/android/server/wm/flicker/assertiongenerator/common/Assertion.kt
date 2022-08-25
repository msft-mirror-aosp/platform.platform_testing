package com.android.server.wm.flicker.assertiongenerator.common

import com.android.server.wm.flicker.assertions.AssertionsChecker
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.traces.common.ITrace
import com.android.server.wm.traces.common.ITraceEntry

abstract class Assertion {
    abstract val assertionsChecker: AssertionsChecker<out FlickerSubject>

    open fun execute(newTrace: ITrace<ITraceEntry>) {
        TODO()
    }

    override fun toString(): String {
        TODO()
    }
}
