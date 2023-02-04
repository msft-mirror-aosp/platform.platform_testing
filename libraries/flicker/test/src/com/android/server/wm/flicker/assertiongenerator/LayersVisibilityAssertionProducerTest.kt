/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wm.flicker.assertiongenerator

import android.util.Log
import com.android.server.wm.flicker.TraceFileReader
import com.android.server.wm.flicker.assertFailureFact
import com.android.server.wm.flicker.assertThrows
import com.android.server.wm.flicker.assertiongenerator.AssertionGenConfigTestConst.Companion.emptyDeviceTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.AssertionProducerTestConst.Companion.expectedLayersAssertionStringsFileTrace
import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.assertiongenerator.common.TraceContent
import com.android.server.wm.flicker.assertiongenerator.layers.LayersComponentLifecycle
import com.android.server.wm.flicker.assertiongenerator.layers.LayersLifecycleExtractor
import com.android.server.wm.flicker.assertiongenerator.layers.LayersTraceLifecycle
import com.android.server.wm.flicker.assertiongenerator.layers.LayersVisibilityAssertionProducer
import com.android.server.wm.flicker.traces.FlickerSubjectException
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.service.FlickerServiceScenario
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.service.ScenarioInstance
import com.android.server.wm.traces.common.service.ScenarioType
import com.android.server.wm.traces.common.transition.Transition
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test

/**
 * Contains [LayersVisibilityAssertionProducer] tests.
 *
 * To run this test: `atest FlickerLibTest:LayersVisibilityAssertionProducerTest`
 */
class LayersVisibilityAssertionProducerTest {
    private var emptyTransition = Transition.emptyTransition()

    lateinit var assertions: List<Assertion>
    private lateinit var executeLayersTrace: LayersTrace
    private var transition: Transition = emptyTransition

    lateinit var assertionsSameComponentMatcher: List<Assertion>
    private lateinit var executeSameComponentMatcherLayersTrace: LayersTrace
    private var sameComponentMatcherTransition: Transition = emptyTransition

    lateinit var assertionsConfig: List<Assertion>
    private lateinit var executeConfigLayersTrace: LayersTrace
    private var configTransition: Transition = emptyTransition

    lateinit var assertionFail: Assertion
    private var failTransition: Transition = emptyTransition

    private fun produceAssertionsFromTraceDump(
        traceDump: DeviceTraceDump,
        traceConfig: DeviceTraceConfiguration
    ): List<Assertion> {
        val lifecycleExtractor = LayersLifecycleExtractor()
        val layersVisibilityAssertionProducer = LayersVisibilityAssertionProducer()
        return lifecycleExtractor.extract(traceDump, traceConfig)?.let {
            layersVisibilityAssertionProducer.produce(
                listOf(TraceContent.byTraceType(it, traceConfig))
            )
        }
            ?: throw RuntimeException("Layers lifecycle was expected, but is actually null")
    }

    private fun produceAssertionsFromTestTrace() {
        val elementLifecycles =
            listOf(
                LayersTraceLifecycle(
                    ElementLifecycleExtractorTestConst
                        .expectedElementLifecyclesVisibilityAssertionProducer
                        as MutableMap<ComponentNameMatcher, LayersComponentLifecycle>
                )
            )
        val layersVisibilityAssertionProducer = LayersVisibilityAssertionProducer()
        assertions =
            layersVisibilityAssertionProducer.produce(
                elementLifecycles.map { lifecycle ->
                    TraceContent.byTraceType(lifecycle, emptyDeviceTraceConfiguration)
                }
            )
    }

    private fun createExecuteLayersTrace() {
        executeLayersTrace =
            ElementLifecycleExtractorTestConst.createTraceArg(
                ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAssertionProducer
            )
    }

    private fun produceAssertionsSameComponentMatcherFromTestTrace() {
        val elementLifecycles =
            listOf(
                LayersTraceLifecycle(
                    ElementLifecycleExtractorTestConst
                        .expectedElementLifecycles_SameComponentMatcher
                        as MutableMap<ComponentNameMatcher, LayersComponentLifecycle>
                )
            )
        val layersVisibilityAssertionProducer = LayersVisibilityAssertionProducer()
        assertionsSameComponentMatcher =
            layersVisibilityAssertionProducer.produce(
                elementLifecycles.map { lifecycle ->
                    TraceContent.byTraceType(lifecycle, emptyDeviceTraceConfiguration)
                }
            )
    }

    private fun produceAssertionsConfigFromTestTrace() {
        val elementLifecycles =
            listOf(
                LayersTraceLifecycle(
                    ElementLifecycleExtractorTestConst.expectedElementLifecycles_OpenApp
                        as MutableMap<ComponentNameMatcher, LayersComponentLifecycle>
                )
            )
        val layersVisibilityAssertionProducer = LayersVisibilityAssertionProducer()
        assertionsConfig =
            layersVisibilityAssertionProducer.produce(
                elementLifecycles.map { lifecycle ->
                    TraceContent.byTraceType(lifecycle, AssertionProducerTestConst.openAppConfig)
                }
            )
    }

    private fun createExecuteSameComponentMatcherLayersTrace() {
        executeSameComponentMatcherLayersTrace =
            ElementLifecycleExtractorTestConst.createTraceArg(
                ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAssertionProducer
            )
    }

    private fun produceAssertionFailFromTestTrace() {
        val elementLifecycles =
            listOf(
                LayersTraceLifecycle(
                    ElementLifecycleExtractorTestConst
                        .expectedElementLifecyclesAllVisibilityAssertions
                        as MutableMap<ComponentNameMatcher, LayersComponentLifecycle>
                )
            )
        val layersVisibilityAssertionProducer = LayersVisibilityAssertionProducer()
        val assertions =
            layersVisibilityAssertionProducer.produce(
                elementLifecycles.map { lifecycle ->
                    TraceContent.byTraceType(lifecycle, emptyDeviceTraceConfiguration)
                }
            )
        assertionFail = assertions[0]
    }

    @Before
    fun setup() {
        createExecuteLayersTrace()
        createExecuteSameComponentMatcherLayersTrace()
        produceAssertionsFromTestTrace()
        produceAssertionsSameComponentMatcherFromTestTrace()
        produceAssertionFailFromTestTrace()
        produceAssertionsConfigFromTestTrace()
    }

    fun produceFromTestTrace_assertions_expected() {
        Truth.assertThat(assertions.size)
            .isEqualTo(AssertionProducerTestConst.expected_layer_visibility_assertions.size)
        assertions.forEachIndexed { index, assertion ->
            try {
                Truth.assertThat(
                        AssertionProducerTestConst.expected_layer_visibility_assertions[index]
                            .isEqual(assertion)
                    )
                    .isTrue()
            } catch (err: AssertionError) {
                throw RuntimeException(
                    "$err\nExpected:\n" +
                        "${AssertionProducerTestConst
                            .expected_layer_visibility_assertions[index]}" +
                        "\n\nActual:\n$assertion"
                )
            }
        }
    }

    @Test
    fun produceFromTestTrace_assertion_execute() {
        assertions.forEachIndexed { index, assertion ->
            assertion.execute(executeLayersTrace, transition)
        }
        produceFromTestTrace_assertions_expected()
    }

    fun produceFromTestTrace_assertions_sameComponentMatcher_expected() {
        Truth.assertThat(assertionsSameComponentMatcher.size)
            .isEqualTo(
                AssertionProducerTestConst.expected_layer_visibility_assertions_sameComponentMatcher
                    .size
            )
        assertionsSameComponentMatcher.forEachIndexed { index, assertion ->
            try {
                Truth.assertThat(
                        assertion.isEqual(
                            AssertionProducerTestConst
                                .expected_layer_visibility_assertions_sameComponentMatcher[index]
                        )
                    )
                    .isTrue()
            } catch (err: AssertionError) {
                throw RuntimeException(
                    "$err\nExpected:\n" +
                        "${AssertionProducerTestConst
                            .expected_layer_visibility_assertions_sameComponentMatcher[index]}" +
                        "\n\nActual:\n$assertion"
                )
            }
        }
    }

    @Test
    fun produceFromTestTrace_assertionSameComponent_execute() {
        assertionsSameComponentMatcher.forEachIndexed { index, assertion ->
            assertion.execute(
                executeSameComponentMatcherLayersTrace,
                sameComponentMatcherTransition
            )
        }
        produceFromTestTrace_assertions_sameComponentMatcher_expected()
    }

    @Test
    fun produceFromTestTrace_assertionConfig_expected() {
        Truth.assertThat(assertionsConfig.size)
            .isEqualTo(
                AssertionProducerTestConst.expected_layer_visibility_assertions_sameComponentMatcher
                    .size
            )
        assertionsConfig.forEachIndexed { index, assertion ->
            try {
                Truth.assertThat(
                        assertion.isEqual(
                            AssertionProducerTestConst.expected_layer_visibility_assertions_openApp[
                                    index]
                        )
                    )
                    .isTrue()
            } catch (err: AssertionError) {
                throw RuntimeException(
                    "$err\nExpected:\n" +
                        "${AssertionProducerTestConst
                            .expected_layer_visibility_assertions_openApp[index]}" +
                        "\n\nActual:\n$assertion"
                )
            }
        }
    }

    @Test
    fun produceFromTestTrace_assertions_fail1() {
        val layersTrace =
            ElementLifecycleExtractorTestConst.createTraceArg(
                ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAllVisibilityAssertions_fail1
            )
        val error =
            assertThrows<FlickerSubjectException> {
                assertionFail.execute(layersTrace, failTransition)
            }
        assertFailureFact(error, "Assertion").contains("notContains(StatusBar)")
        assertFailureFact(error, "Found").contains("Layer:StatusBar")
    }

    @Test
    fun produceFromTestTrace_assertions_fail2() {
        val layersTrace =
            ElementLifecycleExtractorTestConst.createTraceArg(
                ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAllVisibilityAssertions_fail2
            )
        val error =
            assertThrows<FlickerSubjectException> {
                assertionFail.execute(layersTrace, failTransition)
            }
        assertFailureFact(error, "Passed").contains("notContains(StatusBar)")
        assertFailureFact(error, "Assertion never failed").contains("isVisible(StatusBar)")
        assertFailureFact(error, "Untested").contains("isInvisible(StatusBar)")
    }

    @Test
    fun produceFromTestTrace_assertions_fail3() {
        val layersTrace =
            ElementLifecycleExtractorTestConst.createTraceArg(
                ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAllVisibilityAssertions_fail3
            )
        val error =
            assertThrows<FlickerSubjectException> {
                assertionFail.execute(layersTrace, failTransition)
            }
        assertFailureFact(error, "Assertion never failed").contains("notContains(StatusBar)")
        Truth.assertThat(error).hasMessageThat().contains("Untested: isVisible(StatusBar)")
    }

    @Test
    fun produceFromTestTrace_assertions_fail4() {
        val layersTrace =
            ElementLifecycleExtractorTestConst.createTraceArg(
                ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAllVisibilityAssertions_fail4
            )
        val error =
            assertThrows<FlickerSubjectException> {
                assertionFail.execute(layersTrace, failTransition)
            }
        assertFailureFact(error, "Assertion").contains("isVisible(StatusBar)")
        Truth.assertThat(error).hasMessageThat().contains("Is Invisible")
        Truth.assertThat(error).hasMessageThat().contains("Crop is 0x0")
    }

    @Test
    fun produceFromTraceFile_assertion_execute() {
        val configDir = "/assertiongenerator_config_test"
        val goldenTracesConfig = TraceFileReader.getGoldenTracesConfig(configDir)
        val scenario =
            FlickerServiceScenario(ScenarioType.APP_LAUNCH, PlatformConsts.Rotation.ROTATION_0)
        val traceDump = goldenTracesConfig[scenario]!!.deviceTraceDumps[0]
        val traceConfiguration = goldenTracesConfig[scenario]!!.traceConfigurations[0]
        val layersTrace = traceDump.layersTrace
        val wmTrace = traceDump.wmTrace!!
        val transitionsTrace = traceDump.transitionsTrace!!

        val scenarioInstances = mutableListOf<ScenarioInstance>()
        for (scenarioType in ScenarioType.values()) {
            scenarioInstances.addAll(
                scenarioType.getInstances(transitionsTrace, wmTrace) { m ->
                    Log.d("AssertionEngineTest", m)
                }
            )
        }

        layersTrace?.run {
            val assertions = produceAssertionsFromTraceDump(traceDump, traceConfiguration)
            Truth.assertThat(assertions.isNotEmpty()).isTrue()
            for (scenarioInstance in scenarioInstances) {
                assertions.forEach { assertion ->
                    assertion.execute(layersTrace, scenarioInstance.associatedTransition)
                }
                if (scenarioInstance.scenario.scenarioType == ScenarioType.APP_LAUNCH) {
                    val assertionsStrings =
                        assertions.map { assertion -> assertion.assertionString }
                    Truth.assertThat(assertionsStrings)
                        .isEqualTo(expectedLayersAssertionStringsFileTrace)
                }
            }
        }
            ?: throw RuntimeException("LayersTrace is null")
    }
}
