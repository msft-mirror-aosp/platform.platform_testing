package com.android.server.wm.flicker.assertiongenerator.common

import com.android.server.wm.flicker.assertiongenerator.ScenarioConfig
import com.android.server.wm.flicker.assertiongenerator.layers.LayersVisibilityAssertionProducer
import com.android.server.wm.flicker.assertiongenerator.windowmanager.WmFocusedAppAssertionProducer
import com.android.server.wm.flicker.service.assertors.ConfigException
import com.android.server.wm.traces.common.service.FlickerServiceScenario

class AssertionFactory(val config: Map<FlickerServiceScenario, ScenarioConfig>) {
    private val producers: Set<IAssertionProducer> =
        setOf(LayersVisibilityAssertionProducer(), WmFocusedAppAssertionProducer())

    /**
     * Produce assertions based on the given traces and their configurations. traceDumps and
     * traceConfigurations should correspond to each other (same order)
     */
    private fun produce(scenarioConfig: ScenarioConfig): Array<Assertion> {
        val traceDumps = scenarioConfig.deviceTraceDumps
        val traceConfigurations = scenarioConfig.traceConfigurations
        if (traceDumps.size != traceConfigurations.size) {
            throw RuntimeException(
                "TraceDumps and TraceConfigurations should have the same " +
                    "size, but they have ${traceDumps.size}, " +
                    "respectively ${traceConfigurations.size}"
            )
        }
        val traceContents: List<TraceContent> =
            LifecycleExtractorFactory.extract(traceDumps, traceConfigurations)
        return producers.flatMap { producer -> producer.produce(traceContents) }.toTypedArray()
    }

    private fun getScenarioConfig(scenario: FlickerServiceScenario): ScenarioConfig {
        return config[scenario]
            ?: throw ConfigException("Missing configuration for scenario $scenario")
    }

    fun getAssertionsForScenario(scenario: FlickerServiceScenario): Array<Assertion> {
        val scenarioConfig = getScenarioConfig(scenario)
        return produce(scenarioConfig)
    }
}
