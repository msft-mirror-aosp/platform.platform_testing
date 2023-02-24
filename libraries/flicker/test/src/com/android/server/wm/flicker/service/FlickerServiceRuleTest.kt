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

import com.android.server.wm.flicker.helpers.isShellTransitionsEnabled
import com.android.server.wm.flicker.runner.Consts
import com.android.server.wm.flicker.runner.ExecutionError
import com.android.server.wm.flicker.service.assertors.AssertionResult
import com.android.server.wm.flicker.service.assertors.IAssertionResult
import com.android.server.wm.flicker.service.assertors.IFaasAssertion
import com.android.server.wm.flicker.service.rules.FlickerServiceRule
import com.android.server.wm.flicker.utils.KotlinMockito
import com.android.server.wm.traces.common.service.AssertionInvocationGroup
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.MethodSorters
import org.mockito.Mockito
import org.mockito.Mockito.`when`

/**
 * Contains [FlickerServiceRule] tests. To run this test: `atest
 * FlickerLibTest:FlickerServiceRuleTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FlickerServiceRuleTest {
    @Before
    fun before() {
        Assume.assumeTrue(isShellTransitionsEnabled)
    }

    @Test
    fun startsTraceCollectionOnTestStarting() {
        val mockFlickerServiceResultsCollector =
            Mockito.mock(IFlickerServiceResultsCollector::class.java)
        val testRule = FlickerServiceRule(mockFlickerServiceResultsCollector)
        val mockDescription = Description.createTestDescription("MockClass", "mockTest")

        testRule.starting(mockDescription)
        Mockito.verify(mockFlickerServiceResultsCollector).testStarted(mockDescription)
    }

    @Test
    fun stopsTraceCollectionOnTestFinished() {
        val mockFlickerServiceResultsCollector =
            Mockito.mock(IFlickerServiceResultsCollector::class.java)
        val testRule = FlickerServiceRule(mockFlickerServiceResultsCollector)
        val mockDescription = Description.createTestDescription("MockClass", "mockTest")

        testRule.finished(mockDescription)
        Mockito.verify(mockFlickerServiceResultsCollector).testFinished(mockDescription)
    }

    @Test
    fun reportsFailuresToMetricsCollector() {
        val mockFlickerServiceResultsCollector =
            Mockito.mock(IFlickerServiceResultsCollector::class.java)
        val testRule = FlickerServiceRule(mockFlickerServiceResultsCollector)
        val mockDescription = Description.createTestDescription("MockClass", "mockTest")
        val mockError = Throwable("Mock error")

        testRule.failed(mockError, mockDescription)
        Mockito.verify(mockFlickerServiceResultsCollector)
            .testFailure(
                KotlinMockito.argThat {
                    this.description == mockDescription && this.exception == mockError
                }
            )
    }

    @Test
    fun reportsSkippedToMetricsCollector() {
        val mockFlickerServiceResultsCollector =
            Mockito.mock(IFlickerServiceResultsCollector::class.java)
        val testRule = FlickerServiceRule(mockFlickerServiceResultsCollector)
        val mockDescription = Description.createTestDescription("MockClass", "mockTest")
        val mockAssumptionFailure = AssumptionViolatedException("Mock error")

        testRule.skipped(mockAssumptionFailure, mockDescription)
        Mockito.verify(mockFlickerServiceResultsCollector).testSkipped(mockDescription)
    }

    @Test
    fun doesNotThrowExceptionForFlickerTestFailureIfRequested() {
        val mockFlickerServiceResultsCollector =
            Mockito.mock(IFlickerServiceResultsCollector::class.java)
        val testRule =
            FlickerServiceRule(mockFlickerServiceResultsCollector, failTestOnFaasFailure = false)
        val mockDescription = Description.createTestDescription("MockClass", "mockTest")

        val assertionError = Throwable("Some assertion error")
        `when`(mockFlickerServiceResultsCollector.resultsForTest(mockDescription))
            .thenReturn(listOf(mockFailureAssertionResult(assertionError)))
        `when`(mockFlickerServiceResultsCollector.testContainsFlicker(mockDescription))
            .thenReturn(true)

        testRule.starting(mockDescription)
        testRule.succeeded(mockDescription)
        testRule.finished(mockDescription)
    }

    @Test
    fun throwsExceptionForFlickerTestFailureIfRequested() {
        val mockFlickerServiceResultsCollector =
            Mockito.mock(IFlickerServiceResultsCollector::class.java)
        val testRule =
            FlickerServiceRule(mockFlickerServiceResultsCollector, failTestOnFaasFailure = true)
        val mockDescription = Description.createTestDescription("MockClass", "mockTest")

        val assertionError = Throwable("Some assertion error")
        `when`(mockFlickerServiceResultsCollector.resultsForTest(mockDescription))
            .thenReturn(listOf(mockFailureAssertionResult(assertionError)))
        `when`(mockFlickerServiceResultsCollector.testContainsFlicker(mockDescription))
            .thenReturn(true)

        testRule.starting(mockDescription)
        testRule.succeeded(mockDescription)
        try {
            testRule.finished(mockDescription)
            error("Exception was not thrown")
        } catch (e: Throwable) {
            Truth.assertThat(e).isEqualTo(assertionError)
        }
    }

    @Test
    fun alwaysThrowsExceptionForExecutionErrors() {
        val mockFlickerServiceResultsCollector =
            Mockito.mock(IFlickerServiceResultsCollector::class.java)
        val testRule =
            FlickerServiceRule(mockFlickerServiceResultsCollector, failTestOnFaasFailure = true)
        val mockDescription = Description.createTestDescription("MockClass", "mockTest")

        val executionError = ExecutionError(Throwable(Consts.FAILURE))
        `when`(mockFlickerServiceResultsCollector.executionErrors)
            .thenReturn(listOf(executionError))

        testRule.starting(mockDescription)
        testRule.succeeded(mockDescription)
        try {
            testRule.finished(mockDescription)
            error("Exception was not thrown")
        } catch (e: Throwable) {
            Truth.assertThat(e).isEqualTo(executionError)
        }
    }

    companion object {
        fun mockFailureAssertionResult(error: Throwable): IAssertionResult {
            return AssertionResult(
                object : IFaasAssertion {
                    override val name: String
                        get() = "MockAssertion"
                    override val stabilityGroup: AssertionInvocationGroup
                        get() = AssertionInvocationGroup.BLOCKING
                    override fun evaluate(): IAssertionResult {
                        error("Unimplemented - shouldn't be called")
                    }
                },
                assertionError = error
            )
        }
    }
}
