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
import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.dsl.FlickerBuilder

/**
 * Factory for creating JUnit4 compatible tests based on the flicker DSL
 *
 * This class recreates behavior from JUnit5 TestFactory that is not available on JUnit4
 */
open class FlickerTestRunnerFactory {
    protected val cachedTests = mutableMapOf<String, Flicker>()

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
        require(cachedTests[flicker.testName] == null) {
            "A test spec with name ${flicker.testName} already exists"
        }
        cachedTests[flicker.testName] = flicker
        Log.v(FLICKER_TAG, "Adding ${flicker.testName} to cache. " +
            "Current cache size: ${cachedTests.size}")
        val assertionsList = flicker.assertions
        val lastAssertionIdx = assertionsList.lastIndex

        val result = mutableListOf(TestSpec(flicker.testName))
        result.addAll(
            assertionsList.map { assertion ->
                TestSpec(flicker.testName, assertion.name)
            }
        )

        result.add(TestSpec(flicker.testName, "CLEANUP", cleanUp = true))
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

    /**
     * Removes all entries from the cache.
     *
     * Using during self-tests of the library
     */
    @VisibleForTesting
    fun removeAll() {
        Log.v(FLICKER_TAG, "Removing all tags from cache")
        val keys = cachedTests.keys.toSet()
        keys.forEach { remove(it, isCleanUp = true) }
    }

    /**
     * Fetches a test case [testSpecification] from the cache
     *
     * @param testSpecification Test to fetch
     * @throws IllegalStateException if the test doesn't exist
     */
    open fun get(testSpecification: TestSpec): Flicker? {
        val testName = testSpecification.testName
        return cachedTests[testName]
    }

    /**
     * Cleanup all tests from the cache.
     *
     * This is necessary because JUnit's ParameterizedRunner keeps a reference to the list of
     * test cases until the whole test suite finishes, this prevents GC from removing executed
     * traces
     *
     * @param testSpecification Test to remove
     */
    open fun remove(testSpecification: TestSpec) {
        remove(testSpecification.testName, testSpecification.cleanUp)
    }

    protected open fun remove(testName: String, isCleanUp: Boolean) {
        val flickerSpec = cachedTests.remove(testName)
        Log.v(FLICKER_TAG, "Cleaning up $testName")
        require(isCleanUp || flickerSpec?.testName == testName) {
            "Unable to remove test $testName from cache"
        }
        flickerSpec?.clear()
    }

    /**
     * Ensure that none of only 1 test has a result associated with it (cached)
     *
     * This method is used during execution to prevent tests from skipping the cleanup step,
     * allowing GC to remove tests from memory before the test suite finishes
     *
     * @throws IllegalStateException if the more than 1 test have a result
     */
    fun assertUpToOneTestExecuted() {
        Log.v(FLICKER_TAG, "Ensuring up to one test in the cache is executed")
        var alreadyExecuted = ""
        cachedTests.forEach { (testName, flicker) ->
            if (flicker.result != null) {
                require(alreadyExecuted.isEmpty()) {
                    "Test ${flicker.testName} wants " +
                        "to execute but $alreadyExecuted was not cleaned up"
                }
                alreadyExecuted = testName
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
        @JvmField val testName: String,
        @JvmField val assertionName: String = "",
        @JvmField val cleanUp: Boolean = false
    ) {
        override fun toString(): String {
            return buildString {
                append(testName)

                if (assertionName.isNotEmpty()) {
                    append("_$assertionName")
                }
            }
        }
    }
}