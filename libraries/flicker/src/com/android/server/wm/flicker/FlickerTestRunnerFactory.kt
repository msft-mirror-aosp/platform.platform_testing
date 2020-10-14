/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.view.Surface
import com.android.server.wm.flicker.dsl.FlickerBuilder

/**
 * Factory for creating JUnit4 compatible tests based on the flicker DSL
 *
 * This class recreates behavior from JUnit5 TestFactory that is not available on JUnit4
 */
class FlickerTestRunnerFactory @JvmOverloads constructor(
    private val instrumentation: Instrumentation,
    private val supportedRotations: List<Int> = listOf(Surface.ROTATION_0, Surface.ROTATION_90),
    private val repetitions: Int = 1
) {
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
    fun buildTest(
        deviceConfigurations: List<Bundle> = getConfigNonRotationTests(),
        testSpecification: FlickerBuilder.(Bundle) -> Any
    ): List<Array<Any>> {
        return deviceConfigurations.map {
            val builder = FlickerBuilder(instrumentation)
            val flickerTests = builder.apply { testSpecification(it) }.build()

            flickerTests
        }.map { arrayOf(it.toString(), it) }
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
    fun buildRotationTest(
        deviceConfigurations: List<Bundle> = getConfigRotationTests(),
        testSpecification: FlickerBuilder.(Bundle) -> Any
    ): List<Array<Any>> {
        return deviceConfigurations.map {
            val builder = FlickerBuilder(instrumentation)
            val flickerTests = builder.apply { testSpecification(it) }.build()

            flickerTests
        }.map { arrayOf(it.toString(), it) }
    }

    /**
     * Gets a list of test configurations.
     *
     * Each configurations has a start orientation.
     */
    fun getConfigNonRotationTests() = supportedRotations
        .map { rotation -> Bundle().also {
            it.putInt(START_ROTATION, rotation)
            it.putInt(REPETITIONS, repetitions)
        } }

    /**
     * Gets a list of test configurations.
     *
     * Each configurations has a start and end orientation.
     */
    fun getConfigRotationTests(): List<Bundle> {
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
}