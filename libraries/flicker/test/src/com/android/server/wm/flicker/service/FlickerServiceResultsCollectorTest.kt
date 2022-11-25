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

package com.android.server.wm.flicker.service

import android.device.collectors.DataRecord
import com.android.server.wm.flicker.service.assertors.AssertionResult
import com.android.server.wm.flicker.utils.KotlinMockito
import com.android.server.wm.flicker.utils.MockLayersTraceBuilder
import com.android.server.wm.flicker.utils.MockWindowManagerTraceBuilder
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.service.AssertionInvocationGroup
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.service.Scenario
import com.android.server.wm.traces.common.service.ScenarioType
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runners.MethodSorters
import org.mockito.Mockito

/**
 * Contains [FlickerServiceResultsCollector] tests. To run this test: `atest
 * FlickerLibTest:FlickerServiceResultsCollectorTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FlickerServiceResultsCollectorTest {
    @Test
    fun reportsMetricsOnlyForPassingTestsIfRequested() {
        val mockTraceCollector = Mockito.mock(ITracesCollector::class.java)
        Mockito.`when`(mockTraceCollector.getCollectedTraces())
            .thenReturn(
                ITracesCollector.Companion.Traces(
                    wmTrace = MockWindowManagerTraceBuilder().build(),
                    layersTrace = MockLayersTraceBuilder().build(),
                    transitionsTrace = TransitionsTrace(emptyArray()),
                )
            )
        val mockFlickerService = Mockito.mock(IFlickerService::class.java)
        Mockito.`when`(
                mockFlickerService.process(
                    KotlinMockito.any(WindowManagerTrace::class.java),
                    KotlinMockito.any(LayersTrace::class.java),
                    KotlinMockito.any(TransitionsTrace::class.java)
                )
            )
            .thenReturn(
                listOf(
                    AssertionResult(
                        "assertionName",
                        Scenario(ScenarioType.APP_LAUNCH, PlatformConsts.Rotation.ROTATION_0),
                        AssertionInvocationGroup.BLOCKING,
                        null
                    )
                )
            )

        val collector =
            FlickerServiceResultsCollector(
                tracesCollector = mockTraceCollector,
                flickerService = mockFlickerService,
                reportOnlyForPassingTests = true,
            )

        val runData = DataRecord()
        val runDescription = Description.createSuiteDescription("TestSuite")
        val testData = DataRecord()
        val testDescription = Description.createTestDescription("TestClass", "TestName")

        collector.onTestRunStart(runData, runDescription)
        collector.onTestStart(testData, testDescription)
        collector.onTestFail(testData, testDescription, Mockito.mock(Failure::class.java))
        collector.onTestEnd(testData, testDescription)
        collector.onTestRunEnd(runData, Mockito.mock(org.junit.runner.Result::class.java))

        Truth.assertThat(collector.executionErrors).isEmpty()
        Truth.assertThat(collector.assertionResultsByTest[testDescription]).isNull()
        Truth.assertThat(runData.hasMetrics()).isFalse()
    }

    @Test
    fun reportsMetricsForFailingTestsIfRequested() {
        val mockTraceCollector = Mockito.mock(ITracesCollector::class.java)
        Mockito.`when`(mockTraceCollector.getCollectedTraces())
            .thenReturn(
                ITracesCollector.Companion.Traces(
                    wmTrace = MockWindowManagerTraceBuilder().build(),
                    layersTrace = MockLayersTraceBuilder().build(),
                    transitionsTrace = TransitionsTrace(emptyArray()),
                )
            )
        val mockFlickerService = Mockito.mock(IFlickerService::class.java)
        Mockito.`when`(
                mockFlickerService.process(
                    KotlinMockito.any(WindowManagerTrace::class.java),
                    KotlinMockito.any(LayersTrace::class.java),
                    KotlinMockito.any(TransitionsTrace::class.java)
                )
            )
            .thenReturn(
                listOf(
                    AssertionResult(
                        "assertionName",
                        Scenario(ScenarioType.APP_LAUNCH, PlatformConsts.Rotation.ROTATION_0),
                        AssertionInvocationGroup.BLOCKING,
                        null
                    )
                )
            )
        val collector =
            FlickerServiceResultsCollector(
                tracesCollector = mockTraceCollector,
                flickerService = mockFlickerService,
                reportOnlyForPassingTests = false,
            )

        val runData = DataRecord()
        val runDescription = Description.createSuiteDescription("TestSuite")
        val testData = DataRecord()
        val testDescription = Description.createTestDescription("TestClass", "TestName")

        collector.onTestRunStart(runData, runDescription)
        collector.onTestStart(testData, testDescription)
        collector.onTestFail(testData, testDescription, Mockito.mock(Failure::class.java))
        collector.onTestEnd(testData, testDescription)
        collector.onTestRunEnd(runData, Mockito.mock(org.junit.runner.Result::class.java))

        Truth.assertThat(collector.executionErrors).isEmpty()
        Truth.assertThat(collector.resultsForTest(testDescription)).isNotEmpty()
        Truth.assertThat(testData.hasMetrics()).isTrue()
    }

    @Test
    fun collectsMetricsForEachTestIfRequested() {
        val mockTraceCollector = Mockito.mock(ITracesCollector::class.java)
        Mockito.`when`(mockTraceCollector.getCollectedTraces())
            .thenReturn(
                ITracesCollector.Companion.Traces(
                    wmTrace = MockWindowManagerTraceBuilder().build(),
                    layersTrace = MockLayersTraceBuilder().build(),
                    transitionsTrace = TransitionsTrace(emptyArray()),
                )
            )
        val mockFlickerService = Mockito.mock(IFlickerService::class.java)
        Mockito.`when`(
                mockFlickerService.process(
                    KotlinMockito.any(WindowManagerTrace::class.java),
                    KotlinMockito.any(LayersTrace::class.java),
                    KotlinMockito.any(TransitionsTrace::class.java)
                )
            )
            .thenReturn(
                listOf(
                    AssertionResult(
                        "assertionName",
                        Scenario(ScenarioType.APP_LAUNCH, PlatformConsts.Rotation.ROTATION_0),
                        AssertionInvocationGroup.BLOCKING,
                        null
                    )
                )
            )
        val collector =
            FlickerServiceResultsCollector(
                tracesCollector = mockTraceCollector,
                flickerService = mockFlickerService,
                collectMetricsPerTest = true,
            )

        val runData = DataRecord()
        val runDescription = Description.createSuiteDescription("TestSuite")
        val testData = DataRecord()
        val testDescription = Description.createTestDescription("TestClass", "TestName")

        collector.onTestRunStart(runData, runDescription)
        collector.onTestStart(testData, testDescription)
        collector.onTestEnd(testData, testDescription)
        collector.onTestRunEnd(runData, Mockito.mock(org.junit.runner.Result::class.java))

        Truth.assertThat(collector.executionErrors).isEmpty()
        Truth.assertThat(collector.resultsForTest(testDescription)).isNotEmpty()
        Truth.assertThat(testData.hasMetrics()).isTrue()
    }

    @Test
    fun collectsMetricsForEntireTestRunIfRequested() {
        val mockTraceCollector = Mockito.mock(ITracesCollector::class.java)
        Mockito.`when`(mockTraceCollector.getCollectedTraces())
            .thenReturn(
                ITracesCollector.Companion.Traces(
                    wmTrace = MockWindowManagerTraceBuilder().build(),
                    layersTrace = MockLayersTraceBuilder().build(),
                    transitionsTrace = TransitionsTrace(emptyArray()),
                )
            )
        val mockFlickerService = Mockito.mock(IFlickerService::class.java)
        Mockito.`when`(
                mockFlickerService.process(
                    KotlinMockito.any(WindowManagerTrace::class.java),
                    KotlinMockito.any(LayersTrace::class.java),
                    KotlinMockito.any(TransitionsTrace::class.java)
                )
            )
            .thenReturn(
                listOf(
                    AssertionResult(
                        "assertionName",
                        Scenario(ScenarioType.APP_LAUNCH, PlatformConsts.Rotation.ROTATION_0),
                        AssertionInvocationGroup.BLOCKING,
                        null
                    )
                )
            )
        val collector =
            FlickerServiceResultsCollector(
                tracesCollector = mockTraceCollector,
                flickerService = mockFlickerService,
                collectMetricsPerTest = false,
            )

        val runData = DataRecord()
        val runDescription = Description.createSuiteDescription("TestSuite")
        val testData = DataRecord()
        val testDescription = Description.createTestDescription("TestClass", "TestName")

        collector.onTestRunStart(runData, runDescription)
        collector.onTestStart(testData, testDescription)
        collector.onTestEnd(testData, testDescription)
        collector.onTestRunEnd(runData, Mockito.mock(org.junit.runner.Result::class.java))

        Truth.assertThat(collector.executionErrors).isEmpty()
        Truth.assertThat(collector.assertionResults).isNotEmpty()
        Truth.assertThat(runData.hasMetrics()).isTrue()
    }
}
