package com.android.server.wm.flicker.assertiongenerator.common

import com.android.server.wm.flicker.assertiongenerator.layers.LayersVisibilityAssertionProducer
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.service.Scenario

class AssertionFactory {
    companion object {
        private val producers: Set<IAssertionProducer> = setOf(
            LayersVisibilityAssertionProducer()
        )

        var config: Map<Scenario, Array<DeviceTraceDump>> = mapOf()

        private fun produce(traceDumps: Array<DeviceTraceDump>): Array<Assertion> {
            val lifecycles: List<ITraceLifecycle> =
                LifecycleExtractorFactory.extract(traceDumps)
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
}
