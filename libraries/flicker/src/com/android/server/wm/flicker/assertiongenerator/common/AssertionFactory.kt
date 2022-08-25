package com.android.server.wm.flicker.assertiongenerator.common

import com.android.server.wm.flicker.assertiongenerator.layers.LayersVisibilityAssertionProducer
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.service.Scenario

class AssertionFactory(
    val config: Map<Scenario, Array<DeviceTraceDump>>,
    val lifecycleExtractorFactory: LifecycleExtractorFactory
) {
    val producers: Set<IAssertionProducer> = setOf(
        LayersVisibilityAssertionProducer()
    )

    private fun produce(traceDumps: Array<DeviceTraceDump>): Array<Assertion> {
        val lifecycles: List<ITraceLifecycle> =
            lifecycleExtractorFactory.extract(traceDumps)
        return producers.flatMap { producer ->
            producer.produce(lifecycles)
        }.toTypedArray()
    }

    private fun getTraceDumpsForScenario(scenario: Scenario): Array<DeviceTraceDump> {
        return config[scenario] ?: error("Missing configuration for scenario $scenario")
    }

    fun getAssertionsForScenario(scenario: Scenario): Array<Assertion> {
        val traceDumps = getTraceDumpsForScenario(scenario)
        return produce(traceDumps)
    }
}
