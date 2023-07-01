/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.device.flicker.junit

import android.annotation.SuppressLint
import android.app.Instrumentation
import android.platform.test.annotations.FlakyTest
import android.tools.common.flicker.annotation.FlickerServiceCompatible
import android.tools.common.flicker.assertions.FlickerTest
import android.tools.device.apphelpers.BrowserAppHelper
import android.tools.device.flicker.IS_FAAS_ENABLED
import android.tools.device.flicker.isShellTransitionsEnabled
import android.tools.device.flicker.legacy.FlickerBuilder
import android.tools.device.flicker.legacy.LegacyFlickerTest
import android.tools.device.flicker.legacy.LegacyFlickerTestFactory
import android.tools.getScenarioTraces
import android.tools.rules.CleanFlickerEnvironmentRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.ClassRule
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
 * Contains [LegacyFlickerJUnit4ClassRunner] tests.
 *
 * To run this test: `atest FlickerLibTest:LegacyFlickerJUnit4ClassRunnerTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressLint("VisibleForTests")
class LegacyFlickerJUnit4ClassRunnerTest {
    @Test
    fun doesNotRunWithEmptyTestParameter() {
        val testClass = TestClass(SimpleFaasTest::class.java)
        val test = TestWithParameters("[PARAMS]", testClass, listOf())
        try {
            val runner = createRunner(test)
            runner.run(RunNotifier())
            error("Expected runner to fail but did not")
        } catch (e: Throwable) {
            Truth.assertWithMessage("Expected failure")
                .that(e)
                .hasMessageThat()
                .contains(NO_SCENARIO_MESSAGE)
        }
    }

    @Test
    fun doesNotRunWithoutValidFlickerTest() {
        val testClass = TestClass(SimpleFaasTest::class.java)
        val test = TestWithParameters("[PARAMS]", testClass, listOf("invalid param"))
        try {
            val runner = createRunner(test)
            runner.run(RunNotifier())
            error("Expected runner to fail but did not")
        } catch (e: Throwable) {
            Truth.assertWithMessage("Expected failure")
                .that(e)
                .hasMessageThat()
                .contains(NO_SCENARIO_MESSAGE)
        }
    }

    @Test
    fun runsWithValidFlickerTest() {
        val testClass = TestClass(SimpleFaasTest::class.java)
        val parameters = LegacyFlickerTestFactory.nonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))
        val runner = createRunner(test)
        runner.run(RunNotifier())
    }

    @Test
    fun flakyTestsRunWithNoFilter() {
        val testClass = TestClass(SimpleTestWithFlakyTest::class.java)
        val parameters = LegacyFlickerTestFactory.nonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))
        val runner = createRunner(test)
        flakyTestRuns = 0
        runner.run(RunNotifier())
        Truth.assertThat(runner.testCount()).isEqualTo(2)
        Truth.assertThat(flakyTestRuns).isEqualTo(1)
    }

    @Test
    fun canFilterOutFlakyTests() {
        val testClass = TestClass(SimpleTestWithFlakyTest::class.java)
        val parameters = LegacyFlickerTestFactory.nonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))
        val runner = createRunner(test)
        runner.filter(FLAKY_TEST_FILTER)
        flakyTestRuns = 0
        val notifier = mock(RunNotifier::class.java)
        runner.run(notifier)
        Truth.assertThat(runner.testCount()).isEqualTo(1)
        Truth.assertThat(flakyTestRuns).isEqualTo(0)
        verify(notifier, never())
            .fireTestStarted(
                argThat { description -> description.methodName.contains("flakyTest") }
            )
    }

    @FlakyTest
    @Test
    fun injectsFlickerServiceTests() {
        Assume.assumeTrue(isShellTransitionsEnabled)
        Assume.assumeTrue(IS_FAAS_ENABLED)

        val testClass = TestClass(SimpleFaasTest::class.java)
        val parameters = LegacyFlickerTestFactory.nonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))
        val runner = createRunner(test)
        val notifier = mock(RunNotifier::class.java)
        runner.run(notifier)
        Truth.assertThat(runner.testCount()).isAtLeast(2)
        verify(notifier).fireTestStarted(argThat { it.methodName.contains("test") })
        verify(notifier).fireTestFinished(argThat { it.methodName.contains("test") })
        verify(notifier, atLeast(1)).fireTestStarted(argThat { it.methodName.contains("FaaS") })
        verify(notifier, atLeast(1)).fireTestFinished(argThat { it.methodName.contains("FaaS") })
    }

    /*@Test
    fun injectedFlickerTestsAreNotExcludedByFilter() {
        Assume.assumeTrue(isShellTransitionsEnabled)
        Assume.assumeTrue(IS_FAAS_ENABLED)

        val testClass = TestClass(SimpleFaasTestWithFlakyTest::class.java)
        val parameters = FlickerTestFactory.nonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))
        val runner = FlickerBlockJUnit4ClassRunner(test)
        runner.filter(FLAKY_TEST_FILTER)
        val notifier = mock(RunNotifier::class.java)
        runner.run(notifier)
        val executionErrors = runner.executionErrors()
        if (executionErrors.isNotEmpty()) {
            throw executionErrors.first()
        }
        Truth.assertThat(runner.testCount()).isAtLeast(2)
        verify(notifier).fireTestStarted(argThat { it.methodName.contains("test") })
        verify(notifier).fireTestFinished(argThat { it.methodName.contains("test") })
        verify(notifier, atLeast(1)).fireTestStarted(argThat { it.methodName.contains("FaaS") })
        verify(notifier, atLeast(1)).fireTestFinished(argThat { it.methodName.contains("FaaS") })
        verify(notifier, never())
            .fireTestStarted(
                argThat { description -> description.methodName.contains("flakyTest") }
            )
    }

    @Test
    fun transitionNotRerunWithFaasEnabled() {
        Assume.assumeTrue(isShellTransitionsEnabled)
        Assume.assumeTrue(IS_FAAS_ENABLED)

        transitionRunCount = 0
        val testClass = TestClass(TransitionRunCounterWithFaasTest::class.java)
        val parameters = FlickerTestFactory.nonRotationTests()
        val test = TestWithParameters("[PARAMS]", testClass, listOf(parameters[0]))

        val runner = FlickerBlockJUnit4ClassRunner(test)
        runner.run(RunNotifier())
        Truth.assertThat(parameters[0].flicker.faasEnabled).isTrue()
        val executionError = parameters[0].flicker.result!!.transitionExecutionError
        Truth.assertWithMessage(
                "No flicker execution errors were expected but got some ::$executionError"
            )
            .that(executionError)
            .isNull()

        Assert.assertEquals(1, transitionRunCount)
        transitionRunCount = 0
    }*/

    @Test
    fun reportsExecutionErrors() {
        checkTestRunReportsExecutionErrors(AlwaysFailExecutionTestClass::class.java)
    }

    private fun checkTestRunReportsExecutionErrors(klass: Class<*>) {
        val testClass = TestClass(klass)
        val parameters = LegacyFlickerTestFactory.nonRotationTests()
        val flickerTest = parameters.first() as LegacyFlickerTest
        val test = TestWithParameters("[PARAMS]", testClass, listOf(flickerTest))

        val runner = createRunner(test)
        val notifier = mock(RunNotifier::class.java)

        runner.run(notifier)
        verify(notifier)
            .fireTestFailure(
                argThat { failure ->
                    failure.message.contains(TRANSITION_FAILURE_MESSAGE) &&
                        failure.description.isTest &&
                        failure.description.displayName ==
                            "test[${flickerTest.scenario.description}](${klass.name})"
                }
            )
    }

    /** Below are all the mock test classes uses for testing purposes */
    @RunWith(Parameterized::class)
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    open class SimpleTest(protected val flicker: FlickerTest) {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        private val testApp: BrowserAppHelper = BrowserAppHelper(instrumentation)

        @FlickerBuilderProvider
        open fun buildFlicker(): FlickerBuilder {
            return FlickerBuilder(instrumentation).usingExistingTraces {
                getScenarioTraces("AppLaunch")
            }
        }

        @Test
        fun test() {
            flicker.assertWm {
                // Random test to make sure flicker transition is executed
                this.visibleWindowsShownMoreThanOneConsecutiveEntry()
            }
        }
    }

    @RunWith(Parameterized::class)
    @FlickerServiceCompatible
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    open class SimpleFaasTest(flicker: FlickerTest) : SimpleTest(flicker)

    @RunWith(Parameterized::class)
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    class AlwaysFailExecutionTestClass(private val flicker: FlickerTest) {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

        @FlickerBuilderProvider
        fun buildFlicker(): FlickerBuilder {
            return FlickerBuilder(instrumentation).apply {
                withoutScreenRecorder()
                transitions { error(TRANSITION_FAILURE_MESSAGE) }
            }
        }

        @Test
        fun test() {
            flicker.assertWm {
                // Random test to make sure flicker transition is executed
                this.visibleWindowsShownMoreThanOneConsecutiveEntry()
            }
        }
    }

    @RunWith(Parameterized::class)
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    open class SimpleTestWithFlakyTest(flicker: FlickerTest) : SimpleTest(flicker) {
        @FlakyTest
        @Test
        fun flakyTest() {
            flakyTestRuns++
            flicker.assertWm {
                // Random test to make sure flicker transition is executed
                this.visibleWindowsShownMoreThanOneConsecutiveEntry()
            }
        }
    }

    @RunWith(Parameterized::class)
    @FlickerServiceCompatible
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    class SimpleFaasTestWithFlakyTest(flicker: FlickerTest) : SimpleTestWithFlakyTest(flicker)

    @RunWith(Parameterized::class)
    @FlickerServiceCompatible
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    class TransitionRunCounterWithFaasTest(flicker: FlickerTest) : SimpleFaasTest(flicker) {
        @FlickerBuilderProvider
        override fun buildFlicker(): FlickerBuilder {
            return FlickerBuilder(instrumentation).apply { transitions { transitionRunCount++ } }
        }
    }

    companion object {
        const val TRANSITION_FAILURE_MESSAGE = "Transition execution failed"
        private val NO_SCENARIO_MESSAGE = "Unable to extract ${FlickerTest::class.simpleName}"

        val FLAKY_TEST_FILTER =
            object : Filter() {
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

        private fun createRunner(baseTest: TestWithParameters) =
            FlickerParametersRunnerFactory().createRunnerForTestWithParameters(baseTest)
                as LegacyFlickerJUnit4ClassRunner

        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
