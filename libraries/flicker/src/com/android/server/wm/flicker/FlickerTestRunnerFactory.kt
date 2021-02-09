/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.os.Bundle
import android.support.test.launcherhelper.ILauncherStrategy
import android.support.test.launcherhelper.LauncherStrategyFactory
import android.util.Log
import android.view.Surface
import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.dsl.FlickerBuilder

/**
 * Factory for creating JUnit4 compatible tests based on the flicker DSL
 *
 * This class recreates behavior from JUnit5 TestFactory that is not available on JUnit4
 */
open class FlickerTestRunnerFactory {
    /**
     * Creates multiple instances of the same test, running on different device orientations
     *
     * @param deviceConfigurations Configurations to run the test (e.g. orientations, rotations).
     *      By default tests are executed twice, once with 0 degrees and once with 90 degrees
     *
     * @param testSpecification Test specification, e.g., setup, teardown, transitions and
     *      assertions
     */
    @JvmOverloads
    open fun buildTest(
        instrumentation: Instrumentation,
        launcherStrategy: ILauncherStrategy =
            LauncherStrategyFactory.getInstance(instrumentation).launcherStrategy,
        supportedRotations: List<Int> = listOf(Surface.ROTATION_0, Surface.ROTATION_90),
        repetitions: Int = 1,
        deviceConfigurations: List<Bundle> =
            getConfigNonRotationTests(supportedRotations, repetitions),
        testSpecification: FlickerBuilder.(Bundle) -> Any
    ): List<Array<Any>> {
        return deviceConfigurations.flatMap {
            val builder = FlickerBuilder(instrumentation, launcherStrategy)
            val flickerTests = buildIndividualTests(builder.apply { testSpecification(it) })

            flickerTests
        }.map { arrayOf(it as Any) }
    }

    /**
     * Creates multiple instances of the same test, running on different configuration
     *
     * @param testSpecification Segments of the test specification, if any
     */
    @JvmOverloads
    open fun buildTest(
        instrumentation: Instrumentation,
        vararg testSpecification: FlickerBuilder.(Bundle) -> Any,
        launcherStrategy: ILauncherStrategy =
            LauncherStrategyFactory.getInstance(instrumentation).launcherStrategy,
        supportedRotations: List<Int> = listOf(Surface.ROTATION_0, Surface.ROTATION_90),
        repetitions: Int = 1,
        deviceConfigurations: List<Bundle> =
            getConfigNonRotationTests(supportedRotations, repetitions)
    ): List<Array<Any>> {
        val newTestSpecification: FlickerBuilder.(Bundle) -> Any = { configuration ->
            testSpecification.forEach {
                this.apply { it(configuration) }
            }
        }

        return buildTest(instrumentation, launcherStrategy, supportedRotations, repetitions,
            deviceConfigurations) { newTestSpecification(it) }
    }

    /**
     * Creates multiple instances of the same test, rotating the device
     *
     * @param deviceConfigurations Configurations to run the test (e.g. orientations, rotations).
     *      By default tests are executed twice, once starting in 0 degrees and rotating to
     *      90 degrees, and once starting at 90 degrees and rotating to 0 degrees.
     *
     * @param testSpecification Test specification, e.g., setup, teardown, transitions and
     *      assertions
     */
    @JvmOverloads
    open fun buildRotationTest(
        instrumentation: Instrumentation,
        launcherStrategy: ILauncherStrategy =
            LauncherStrategyFactory.getInstance(instrumentation).launcherStrategy,
        supportedRotations: List<Int> = listOf(Surface.ROTATION_0, Surface.ROTATION_90),
        repetitions: Int = 1,
        deviceConfigurations: List<Bundle> =
            getConfigRotationTests(supportedRotations, repetitions),
        testSpecification: FlickerBuilder.(Bundle) -> Any
    ): List<Array<Any>> {
        return deviceConfigurations.flatMap {
            val builder = FlickerBuilder(instrumentation, launcherStrategy)
            buildIndividualTests(builder.apply { testSpecification(it) })
        }.map { arrayOf(it as Any) }
    }

    /**
     * Creates multiple instances of the same test, running on different configuration
     *
     * @param testSpecification Segments of the test specification, if any
     */
    @JvmOverloads
    open fun buildRotationTest(
        instrumentation: Instrumentation,
        vararg testSpecification: FlickerBuilder.(Bundle) -> Any,
        launcherStrategy: ILauncherStrategy =
            LauncherStrategyFactory.getInstance(instrumentation).launcherStrategy,
        supportedRotations: List<Int> = listOf(Surface.ROTATION_0, Surface.ROTATION_90),
        repetitions: Int = 1,
        deviceConfigurations: List<Bundle> =
            getConfigRotationTests(supportedRotations, repetitions)
    ): List<Array<Any>> {
        val newTestSpecification: FlickerBuilder.(Bundle) -> Any = { configuration ->
            testSpecification.forEach {
                this.apply { it(configuration) }
            }
        }

        return buildRotationTest(instrumentation, launcherStrategy, supportedRotations,
            repetitions, deviceConfigurations) { newTestSpecification(it) }
    }

    /**
     * Creates multiple flicker tests.
     *
     * Each test contains a single assertion, but all tests share the same setup, transition
     * and results
     */
    protected open fun buildIndividualTests(
        builder: FlickerBuilder
    ): List<TestSpec> {
        val flicker = builder.build(runner = TransitionRunnerCached())
        Log.v(FLICKER_TAG, "Creating ${flicker.testName}.")
        val assertionsList = flicker.assertions

        val result = assertionsList.map { assertion ->
            TestSpec(flicker, assertion)
        }.toMutableList()

        result.add(TestSpec(flicker, _assertion = null, cleanUp = true))
        return result
    }

    /**
     * Gets a list of test configurations.
     *
     * Each configurations has a start orientation.
     */
    @JvmOverloads
    open fun getConfigNonRotationTests(
        supportedRotations: List<Int> = listOf(Surface.ROTATION_0, Surface.ROTATION_90),
        repetitions: Int = 1
    ) = supportedRotations
        .map { rotation ->
            Bundle().also {
                it.putInt(START_ROTATION, rotation)
                it.putInt(REPETITIONS, repetitions)
            }
        }

    /**
     * Gets a list of test configurations.
     *
     * Each configurations has a start and end orientation.
     */
    @JvmOverloads
    open fun getConfigRotationTests(
        supportedRotations: List<Int> = listOf(Surface.ROTATION_0, Surface.ROTATION_90),
        repetitions: Int = 1
    ): List<Bundle> {
        return supportedRotations
            .flatMap { start -> supportedRotations.map { end -> start to end } }
            .filter { (start, end) -> start != end }
            .map { (start, end) ->
                Bundle().also {
                    it.putInt(START_ROTATION, start)
                    it.putInt(END_ROTATION, end)
                    it.putInt(REPETITIONS, repetitions)
                }
            }
    }

    companion object {
        private lateinit var instance: FlickerTestRunnerFactory

        @JvmStatic
        fun getInstance(): FlickerTestRunnerFactory {
            if (!::instance.isInitialized) {
                instance = FlickerTestRunnerFactory()
            }

            return instance
        }
    }

    /**
     * Specification of a flicker test for JUnit ParameterizedRunner class
     * @param testName Name of the test. Appears on log outputs and test dashboards
     * @param assertionName Name of the assertion to test
     * @param cleanUp If this test should delete the traces and screen recording files if it passes
     */
    data class TestSpec(
        @JvmField val test: Flicker,
        private val _assertion: AssertionData?,
        @JvmField val cleanUp: Boolean = false
    ) {
        val assertion: AssertionData get() = _assertion ?: error("No assertion specified")

        override fun toString(): String {
            return buildString {
                append(test.testName)

                if (_assertion?.name?.isNotEmpty() == true) {
                    append("_${assertion.name}")
                }
            }
        }
    }
}