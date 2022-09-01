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

package com.android.server.wm.flicker

import android.app.Instrumentation
import android.platform.test.annotations.FlakyTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.annotation.FlickerServiceCompatible
import com.android.server.wm.flicker.dsl.FlickerBuilder
import com.android.server.wm.flicker.helpers.IS_FAAS_ENABLED
import com.android.server.wm.flicker.helpers.SampleAppHelper
import com.android.server.wm.flicker.helpers.isShellTransitionsEnabled
import com.android.server.wm.flicker.monitor.TraceMonitor.Companion.WINSCOPE_EXT
import com.google.common.truth.Truth
import java.io.File
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeBytes
import org.junit.Assert
import org.junit.Assume
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runner.manipulation.Filter
import org.junit.runner.notification.RunNotifier
import org.junit.runners.MethodSorters
import org.junit.runners.Parameterized
import org.junit.runners.model.TestClass
import org.junit.runners.parameterized.TestWithParameters
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

/**
 * Contains [FlickerBlockJUnit4ClassRunnerTest] tests.
 *
 * To run this test: `atest FlickerLibTest:FlickerBlockJUnit4ClassRunnerTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FlickerBlockJUnit4ClassRunnerTest {

    @Test
    fun doesNotRunWithEmptyTestParameter() {
        val testClass = TestClass(SimpleFaasTest::class.java)
        val test = TestWithParameters("[PARAMS]", testClass, listOf())
        try {
            val runner = FlickerBlockJUnit4ClassRunner(test)
            runner.run(RunNotifier())
            throw Throwable("Expected runner to fail but did not")
        } catch (e: Throwable) {
            Truth.assertThat(e).hasMessageThat()
                .contains("No FlickerTestParameter provided for FlickerRunner")
        }
    }

    @Test
    fun doesNotRunWithoutValidFlickerTestParameter() {
        val testClass = TestClass(SimpleFaasTest::class.java)
        val test = TestWithParameters("[PARAMS]", testClass, listOf("invalid param"))
        try {
            val runner = FlickerBlockJUnit4ClassRunner(test)
            runner.run(RunNotifier())
            throw Throwable("Expected runner to fail but did not")
        } catch (e: Throwable) {
            Truth.assertThat(e).hasMessageThat()
                .contains("No FlickerTestParameter provided for FlickerRunner")
        }
    }

    @Test
    fun runsWithValidFlickerTestParameter() {
        val testClass = TestClass(SimpleFaasTest::class.java)
        val parameters = FlickerTestParameterFactory.getInstance()
            .getConfigNonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))
        val runner = FlickerBlockJUnit4ClassRunner(test)
        runner.run(RunNotifier())
    }

    @Test
    fun flakyTestsRunWithNoFilter() {
        val testClass = TestClass(SimpleTestWithFlakyTest::class.java)
        val parameters = FlickerTestParameterFactory.getInstance()
            .getConfigNonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))
        val runner = FlickerBlockJUnit4ClassRunner(test)
        flakyTestRuns = 0
        runner.run(RunNotifier())
        Truth.assertThat(runner.testCount()).isEqualTo(2)
        Truth.assertThat(flakyTestRuns).isEqualTo(1)
    }

    @Test
    fun canFilterOutFlakyTests() {
        val testClass = TestClass(SimpleTestWithFlakyTest::class.java)
        val parameters = FlickerTestParameterFactory.getInstance()
            .getConfigNonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))
        val runner = FlickerBlockJUnit4ClassRunner(test)
        runner.filter(FLAKY_TEST_FILTER)
        flakyTestRuns = 0
        val notifier = mock(RunNotifier::class.java)
        runner.run(notifier)
        Truth.assertThat(runner.testCount()).isEqualTo(1)
        Truth.assertThat(flakyTestRuns).isEqualTo(0)
        verify(notifier, never()).fireTestStarted(argThat { description ->
            description.methodName.contains("flakyTest")
        })
    }

    @Test
    fun injectsFlickerServiceTests() {
        Assume.assumeTrue(isShellTransitionsEnabled)

        val testClass = TestClass(SimpleFaasTest::class.java)
        val parameters = FlickerTestParameterFactory.getInstance()
            .getConfigNonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))
        val runner = FlickerBlockJUnit4ClassRunner(test)
        val notifier = mock(RunNotifier::class.java)
        runner.run(notifier)
        Truth.assertThat(runner.testCount()).isAtLeast(2)
        verify(notifier)
            .fireTestStarted(argThat { it.methodName.contains("test") })
        verify(notifier)
            .fireTestFinished(argThat { it.methodName.contains("test") })
        verify(notifier, atLeast(1))
            .fireTestStarted(argThat { it.methodName.contains("FaaS") })
        verify(notifier, atLeast(1))
            .fireTestFinished(argThat { it.methodName.contains("FaaS") })
    }

    @Test
    fun injectedFlickerTestsAreNotExcludedByFilter() {
        Assume.assumeTrue(isShellTransitionsEnabled)
        Assume.assumeTrue(IS_FAAS_ENABLED)

        val testClass = TestClass(SimpleFaasTestWithFlakyTest::class.java)
        val parameters = FlickerTestParameterFactory.getInstance()
            .getConfigNonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))
        val runner = FlickerBlockJUnit4ClassRunner(test)
        runner.filter(FLAKY_TEST_FILTER)
        val notifier = mock(RunNotifier::class.java)
        runner.run(notifier)
        Truth.assertThat(runner.testCount()).isAtLeast(2)
        verify(notifier)
            .fireTestStarted(argThat { it.methodName.contains("test") })
        verify(notifier)
            .fireTestFinished(argThat { it.methodName.contains("test") })
        verify(notifier, atLeast(1))
            .fireTestStarted(argThat { it.methodName.contains("FaaS") })
        verify(notifier, atLeast(1))
            .fireTestFinished(argThat { it.methodName.contains("FaaS") })
        verify(notifier, never()).fireTestStarted(argThat { description ->
            description.methodName.contains("flakyTest")
        })
    }

    @Test
    fun transitionNotRerunWithFaasEnabled() {
        Assume.assumeTrue(isShellTransitionsEnabled)

        transitionRunCount = 0
        val testClass = TestClass(TransitionRunCounterWithFaasTest::class.java)
        val parameters = FlickerTestParameterFactory.getInstance()
            .getConfigNonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))

        val runner = FlickerBlockJUnit4ClassRunner(test)
        runner.run(RunNotifier())
        Truth.assertThat(parameters[0].flicker.faasEnabled).isTrue()
        val executionError = parameters[0].flicker.result!!.executionError
        Truth.assertWithMessage(
            "No flicker execution errors were expected but got some ::" +
                executionError
        ).that(executionError).isNull()

        Assert.assertEquals(1, transitionRunCount)
        transitionRunCount = 0
    }

    @Test
    fun reportsExecutionErrors() {
        checkTestRunReportsExecutionErrors(AlwaysFailExecutionTestClass::class.java)
    }

    private fun checkTestRunReportsExecutionErrors(klass: Class<*>) {
        val testClass = TestClass(klass)
        val parameters = FlickerTestParameterFactory.getInstance()
            .getConfigNonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))

        val runner = FlickerBlockJUnit4ClassRunner(test)
        val notifier = mock(RunNotifier::class.java)

        runner.run(notifier)
        verify(notifier).fireTestFailure(argThat { failure ->
            failure.message.contains(TRANSITION_FAILURE_MESSAGE) &&
                failure.description.isTest &&
                failure.description.displayName == "test[PARAMS](${klass.name})"
        })
    }

    /**
     * Below are all the mock test classes uses for testing purposes
     */

    @RunWith(Parameterized::class)
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    open class SimpleTest(protected val testSpec: FlickerTestParameter) {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        private val testApp: SampleAppHelper = SampleAppHelper(instrumentation)

        @FlickerBuilderProvider
        open fun buildFlicker(): FlickerBuilder {
            return FlickerBuilder(instrumentation).usingExistingTraces {
                generateTraceFilesFromScenarioTraces("AppLaunch")
            }
        }

        @Test
        fun test() {
            testSpec.assertWm {
                // Random test to make sure flicker transition is executed
                this.visibleWindowsShownMoreThanOneConsecutiveEntry()
            }
        }
    }

    @RunWith(Parameterized::class)
    @FlickerServiceCompatible
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    open class SimpleFaasTest(testSpec: FlickerTestParameter) : SimpleTest(testSpec)

    @RunWith(Parameterized::class)
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    class AlwaysFailExecutionTestClass(private val testSpec: FlickerTestParameter) {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

        @FlickerBuilderProvider
        fun buildFlicker(): FlickerBuilder {
            return FlickerBuilder(instrumentation).apply {
                transitions {
                    throw Exception(TRANSITION_FAILURE_MESSAGE)
                }
            }
        }

        @Test
        fun test() {
            testSpec.assertWm {
                // Random test to make sure flicker transition is executed
                this.visibleWindowsShownMoreThanOneConsecutiveEntry()
            }
        }
    }

    @RunWith(Parameterized::class)
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    open class SimpleTestWithFlakyTest(testSpec: FlickerTestParameter) : SimpleTest(testSpec) {
        @FlakyTest
        @Test
        fun flakyTest() {
            flakyTestRuns++
            testSpec.assertWm {
                // Random test to make sure flicker transition is executed
                this.visibleWindowsShownMoreThanOneConsecutiveEntry()
            }
        }
    }

    @RunWith(Parameterized::class)
    @FlickerServiceCompatible
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    class SimpleFaasTestWithFlakyTest(testSpec: FlickerTestParameter) :
        SimpleTestWithFlakyTest(testSpec)

    @RunWith(Parameterized::class)
    @FlickerServiceCompatible
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    class TransitionRunCounterWithFaasTest(testSpec: FlickerTestParameter) :
        SimpleFaasTest(testSpec) {
        @FlickerBuilderProvider
        override fun buildFlicker(): FlickerBuilder {
            return FlickerBuilder(instrumentation).apply {
                transitions {
                    transitionRunCount++
                }
            }
        }
    }

    companion object {
        const val TRANSITION_FAILURE_MESSAGE = "Transition execution failed"

        val FLAKY_TEST_FILTER = object : Filter() {
            override fun shouldRun(description: Description): Boolean {
                val hasFlakyAnnotation =
                    description.annotations.filterIsInstance<FlakyTest>().isNotEmpty()
                if (hasFlakyAnnotation && description.isTest) {
                    return false // filter out
                }
                return true
            }

            override fun describe(): String {
                return "no flaky tests"
            }
        }

        var transitionRunCount = 0
        var flakyTestRuns = 0

        private fun generateTraceFilesFromScenarioTraces(scenario: String):
            FlickerBuilder.TraceFiles {
            val randomString = (1..10)
                .map { (('A'..'Z') + ('a'..'z')).random() }
                .joinToString("")

            var wmTrace: File? = null
            var layersTrace: File? = null
            var transactionsTrace: File? = null
            var transitionsTrace: File? = null
            val traces = mapOf<String, (File) -> Unit>(
                "wm_trace" to { wmTrace = it },
                "layers_trace" to { layersTrace = it },
                "transactions_trace" to { transactionsTrace = it },
                "transition_trace" to { transitionsTrace = it }
            )
            for ((traceName, resultSetter) in traces.entries) {
                val traceBytes = readTestFile("scenarios/$scenario/$traceName$WINSCOPE_EXT")
                val traceFile = getDefaultFlickerOutputDir()
                    .resolve("${traceName}_$randomString$WINSCOPE_EXT")
                traceFile.parent.createDirectories()
                traceFile.createFile()
                traceFile.writeBytes(traceBytes)
                resultSetter.invoke(traceFile.toFile())
            }

            return FlickerBuilder.TraceFiles(
                wmTrace!!, layersTrace!!, transactionsTrace!!, transitionsTrace!!
            )
        }
    }
}
