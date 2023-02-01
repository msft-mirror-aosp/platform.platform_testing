package com.android.server.wm.flicker.service.assertors.assertions

import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.service.assertors.BaseAssertionBuilder
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.traces.common.transition.Transition

class AutomaticallyGeneratedLayersAssertions(val assertion: Assertion) : BaseAssertionBuilder() {
    override val name: String
        get() = "AutomaticallyGenerated_${assertion.name}"

    /** {@inheritDoc} */
    override fun doEvaluate(transition: Transition, layerSubject: LayersTraceSubject) {
        assertion.execute(layerSubject, transition)
    }
}
