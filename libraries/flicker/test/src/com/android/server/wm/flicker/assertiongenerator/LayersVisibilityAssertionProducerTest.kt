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

import com.android.server.wm.flicker.assertFailure
import com.android.server.wm.flicker.assertThrows
import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.assertiongenerator.layers.LayersComponentLifecycle
import com.android.server.wm.flicker.assertiongenerator.layers.LayersTraceLifecycle
import com.android.server.wm.flicker.assertiongenerator.layers.LayersVisibilityAssertionProducer
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.layers.LayersTrace
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
    lateinit var assertions: List<Assertion>
    private lateinit var executeLayersTrace: LayersTrace
    private var transition: Transition? = null

    lateinit var assertionsSameComponentMatcher: List<Assertion>
    private lateinit var executeSameComponentMatcherLayersTrace: LayersTrace
    private var sameComponentMatcherTransition: Transition? = null

    lateinit var assertionFail: Assertion

    private fun produceAssertionsFromTestTrace() {
        val elementLifecycles = listOf(LayersTraceLifecycle(
            ElementLifecycleExtractorTestConst.expectedElementLifecyclesVisibilityAssertionProducer
                as MutableMap<ComponentNameMatcher, LayersComponentLifecycle>))
        val layersVisibilityAssertionProducer = LayersVisibilityAssertionProducer()
        assertions = layersVisibilityAssertionProducer.produce(elementLifecycles)
    }

    private fun createExecuteLayersTrace() {
        executeLayersTrace = ElementLifecycleExtractorTestConst.createTrace_arg(
            ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAssertionProducer
        )
    }

    private fun produceAssertionsSameComponentMatcherFromTestTrace() {
        val elementLifecycles = listOf(LayersTraceLifecycle(
            ElementLifecycleExtractorTestConst.expectedElementLifecycles_SameComponentMatcher
                as MutableMap<ComponentNameMatcher, LayersComponentLifecycle>))
        val layersVisibilityAssertionProducer = LayersVisibilityAssertionProducer()
        assertionsSameComponentMatcher =
            layersVisibilityAssertionProducer.produce(elementLifecycles)
    }

    private fun createExecuteSameComponentMatcherLayersTrace() {
        executeSameComponentMatcherLayersTrace = ElementLifecycleExtractorTestConst.createTrace_arg(
            ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAssertionProducer
        )
    }

    private fun produceAssertionFailFromTestTrace() {
        val elementLifecycles = listOf(LayersTraceLifecycle(
            ElementLifecycleExtractorTestConst.expectedElementLifecyclesAllVisibilityAssertions
                as MutableMap<ComponentNameMatcher, LayersComponentLifecycle>))
        val layersVisibilityAssertionProducer = LayersVisibilityAssertionProducer()
        val assertions = layersVisibilityAssertionProducer.produce(elementLifecycles)
        assertionFail = assertions[0]
    }

    @Before
    fun setup() {

        createExecuteLayersTrace()
        createExecuteSameComponentMatcherLayersTrace()
        produceAssertionsFromTestTrace()
        produceAssertionsSameComponentMatcherFromTestTrace()

        produceAssertionFailFromTestTrace()
    }

    fun produceFromTestTrace_assertions_expected() {
        Truth.assertThat(assertions.size)
            .isEqualTo(AssertionProducerTestConst.expected_layer_visibility_assertions.size)
        assertions.forEachIndexed { index, assertion ->
            try {
                Truth.assertThat(
                    AssertionProducerTestConst.expected_layer_visibility_assertions[index]
                        .isEqual(assertion)).isTrue()
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
            assertion.execute(executeLayersTrace)
        }
        produceFromTestTrace_assertions_expected()
    }

    fun produceFromTestTrace_assertions_sameComponentMatcher_expected() {
        Truth.assertThat(assertionsSameComponentMatcher.size)
            .isEqualTo(AssertionProducerTestConst
                .expected_layer_visibility_assertions_sameComponentMatcher.size)
        assertionsSameComponentMatcher.forEachIndexed { index, assertion ->
            try {
                Truth.assertThat(assertion.isEqual(AssertionProducerTestConst
                    .expected_layer_visibility_assertions_sameComponentMatcher[index])
                ).isTrue()
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
            assertion.execute(executeSameComponentMatcherLayersTrace)
        }
        produceFromTestTrace_assertions_sameComponentMatcher_expected()
    }

    @Test
    fun produceFromTestTrace_assertions_fail1() {
        val layersTrace = ElementLifecycleExtractorTestConst.createTrace_arg(
            ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAllVisibilityAssertions_fail1
        )
        val error = assertThrows(AssertionError::class.java) {
            assertionFail.execute(layersTrace)
        }
        assertFailure(error)
            .factValue("Assertion")
            .contains("notContains(StatusBar)")
        assertFailure(error)
            .factValue("Found")
            .contains("Layer:StatusBar")
    }

    @Test
    fun produceFromTestTrace_assertions_fail2() {
        val layersTrace = ElementLifecycleExtractorTestConst.createTrace_arg(
            ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAllVisibilityAssertions_fail2
        )
        val error = assertThrows(AssertionError::class.java) {
            assertionFail.execute(layersTrace)
        }
        assertFailure(error)
            .factValue("Passed")
            .contains("notContains(StatusBar)")
        assertFailure(error)
            .factValue("Assertion never failed")
            .contains("isVisible(StatusBar)")
        assertFailure(error)
            .factValue("Untested")
            .contains("isInvisible(StatusBar)")
    }

    @Test
    fun produceFromTestTrace_assertions_fail3() {
        val layersTrace = ElementLifecycleExtractorTestConst.createTrace_arg(
            ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAllVisibilityAssertions_fail3
        )
        val error = assertThrows(AssertionError::class.java) {
            assertionFail.execute(layersTrace)
        }
        assertFailure(error)
            .factValue("Assertion never failed")
            .contains("notContains(StatusBar)")
        Truth.assertThat(error).hasMessageThat()
            .contains("Untested              : isVisible(StatusBar)")
    }

    @Test
    fun produceFromTestTrace_assertions_fail4() {
        val layersTrace = ElementLifecycleExtractorTestConst.createTrace_arg(
            ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAllVisibilityAssertions_fail4
        )
        val error = assertThrows(AssertionError::class.java) {
            assertionFail.execute(layersTrace)
        }
        assertFailure(error)
            .factValue("Assertion")
            .contains("isVisible(StatusBar)")
        Truth.assertThat(error).hasMessageThat().contains("Is Invisible")
        Truth.assertThat(error).hasMessageThat().contains("Crop is 0x0")
    }
}
