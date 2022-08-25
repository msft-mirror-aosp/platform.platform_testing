package com.android.server.wm.flicker.assertiongenerator.common

interface IAssertionProducer {
    fun produce(lifecycles: List<ITraceLifecycle>): List<Assertion>
}
