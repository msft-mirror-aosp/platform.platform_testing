package com.android.server.wm.flicker.assertiongenerator.common

interface IAssertionProducer {
    fun produce(traceContents: List<TraceContent>): List<Assertion>
}
