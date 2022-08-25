package com.android.server.wm.flicker.assertiongenerator.layers

import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.assertiongenerator.common.ITraceLifecycle

class LayersVisibilityAssertionProducer(
) : LayersAssertionProducer {
    /**
     * Return a list with a single assertion corresponding to the chain of visible/invisible asserts
     */
    override fun produce(lifecycles: List<ITraceLifecycle>): List<Assertion> {
        // var assertionChecker =
        // var assertion = LayersAssertion()
        TODO("Choose the lifecycles we actually need and process those")
    }
}
