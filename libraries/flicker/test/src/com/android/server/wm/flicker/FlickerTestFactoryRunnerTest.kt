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

import android.os.Bundle
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.dsl.FlickerBuilder
import com.google.common.truth.Truth.assertWithMessage
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [FlickerTestRunnerFactory] tests.
 *
 * To run this test: `atest FlickerLibTest:FlickerTestFactoryRunnerTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FlickerTestFactoryRunnerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val defaultRotations = listOf(Surface.ROTATION_0, Surface.ROTATION_90)
    private val testFactory = FlickerTestRunnerFactory.getInstance()

    private fun FlickerBuilder.setDefaultTestCfg(cfg: Bundle) = apply {
        withTestName { "${cfg.startRotationName}_${cfg.endRotationName}_" }
        assertions {
            layersTrace { all("layers") { fail("First assertion") } }
        }
    }

    private fun validateRotationTest(actual: Bundle, rotations: List<Int> = defaultRotations) {
        assertWithMessage("Rotation tests should not have the same start and end rotation")
            .that(actual.startRotation).isNotEqualTo(actual.endRotation)
        assertWithMessage("Invalid start rotation value ${actual.startRotation}")
            .that(actual.startRotation).isIn(rotations)
        assertWithMessage("Invalid end rotation value ${actual.endRotation}")
            .that(actual.endRotation).isIn(rotations)
    }

    private fun validateTest(actual: Bundle, rotations: List<Int> = defaultRotations) {
        assertWithMessage("Tests should have the same start and end rotation")
            .that(actual.startRotation).isEqualTo(actual.endRotation)
        assertWithMessage("Invalid rotation value ${actual.startRotation}")
            .that(actual.startRotation).isIn(rotations)
    }

    @Test
    fun checkBuildTest() {
        val actual = testFactory.buildTest(instrumentation) { cfg ->
            this.setDefaultTestCfg(cfg)
            validateTest(cfg)
        }
        // Should have 1 for the assertion and 1 for cleanup in each orientation
        assertWithMessage("Flicker should create tests for 0 and 90 degrees")
            .that(actual).hasSize(4)
    }

    @Test
    fun checkBuildRotationTest() {
        val actual = testFactory.buildRotationTest(instrumentation) { cfg ->
                this.setDefaultTestCfg(cfg)
                validateRotationTest(cfg)
            }
        // Should have 1 for each assertion and 1 for cleanup in each orientation
        assertWithMessage("Flicker should create tests for 0 and 90 degrees")
            .that(actual).hasSize(4)
    }

    @Test
    fun checkBuildCustomRotationsTest() {
        val rotations = listOf(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180,
                Surface.ROTATION_270)
        val actual = testFactory.buildRotationTest(instrumentation,
            supportedRotations = rotations) { cfg ->
                this.setDefaultTestCfg(cfg)
                validateRotationTest(cfg, rotations)
            }
            .map { it.first() as FlickerTestRunnerFactory.TestSpec }
        // Should have 1 for the assertions in each rotation
        assertWithMessage("Flicker should create tests for 0/90/180/270 degrees")
            .that(actual).hasSize(24)

        actual.forEachIndexed { index, flicker ->
            val expectedCleanUp = index % 2 == 1
            assertWithMessage("Test $index should be cleanup")
                .that(flicker.cleanUp)
                .isEqualTo(expectedCleanUp)
        }
    }

    @Test
    fun checkBuildCustomRotationsTestCleanup() {
        val rotations = listOf(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180,
            Surface.ROTATION_270)
        val actual = testFactory.buildRotationTest(instrumentation,
            supportedRotations = rotations) { cfg ->
            this.setDefaultTestCfg(cfg)
            assertions { windowManagerTrace { start("start") { fail("Fail WM assertion") } } }
            validateRotationTest(cfg, rotations)
        }.map { it.first() as FlickerTestRunnerFactory.TestSpec }
        // Should have 1 for the assertions in each rotation
        assertWithMessage("Flicker should create tests for 0/90/180/270 degrees")
            .that(actual).hasSize(36)

        actual.forEachIndexed { index, flicker ->
            val expectedCleanUp = index % 3 == 2
            assertWithMessage("Test $index should be cleanup")
                .that(flicker.cleanUp)
                .isEqualTo(expectedCleanUp)
        }
    }

    @Test
    fun checkBuildCustomPayloadTest() {
        val actual = listOf(Bundle().also { it.putBoolean("test", true) })
        val tests = testFactory.buildTest(instrumentation,
            deviceConfigurations = actual) { cfg ->
            this.setDefaultTestCfg(cfg)
            validateTest(cfg)
            assertWithMessage("Could not find custom payload data")
                .that(cfg.getBoolean("test", false)).isTrue()
        }
        // Should have 1 for each assertion and 1 fo rcleanup in each orientation
        assertWithMessage("Flicker should create 2 for assertion")
            .that(tests).hasSize(2)
    }

    private fun assertHasAssertion(flicker: Flicker, assertionName: String) {
        assertWithMessage("Should have 1 assertion")
            .that(flicker.assertions.filter { it.name == assertionName })
            .hasSize(1)
    }

    @Test
    fun checkBuildOneTestPerAssertion() {
        val tests = testFactory.buildTest(instrumentation,
            supportedRotations = listOf(Surface.ROTATION_0)) {
            assertions {
                layersTrace { all("layers") { fail("First assertion") } }
                windowManagerTrace { all("wm") { fail("Second assertion") } }
                eventLog { all("eventLog") { fail("This assertion") } }
            }
        }.map { it.first() as FlickerTestRunnerFactory.TestSpec }

        assertWithMessage("Factory should have created 4 tests," +
            " 3 with a single assertion each and 1 for cleanup")
            .that(tests)
            .hasSize(4)

        tests.forEach { testSpec ->
            val test = testSpec.test
            if (!testSpec.cleanUp && testSpec.assertion.name.isNotEmpty()) {
                assertHasAssertion(test, testSpec.assertion.name)
            }
        }

        assertWithMessage("Last test should be cleanup")
            .that(tests.last().cleanUp).isTrue()
    }

    @Test
    fun mergeTestConfiguration() {
        val base: FlickerBuilder.(Bundle) -> Any = {
            assertions {
                windowManagerTrace {
                    start("wm") { it.isEmpty }
                }
            }
        }

        val extension: FlickerBuilder.(Bundle) -> Any = {
            assertions {
                windowManagerTrace {
                    start("wm2") { it.isEmpty }
                }
            }
        }

        val tests = testFactory.buildTest(instrumentation,
            base, extension, supportedRotations = listOf(Surface.ROTATION_0))
            .map { it.first() as FlickerTestRunnerFactory.TestSpec }
        assertWithMessage("Factory should have created 2 tests with a single assertion each," +
            " and 1 test for cleanup")
                .that(tests)
                .hasSize(3)

        tests.forEach { testSpec ->
            if (!testSpec.cleanUp) {
                assertHasAssertion(testSpec.test, testSpec.assertion.name)
            }
        }
        assertWithMessage("Last test should be cleanup")
            .that(tests.last().cleanUp).isTrue()
    }

    @Test
    fun checkCleanUp() {
        val actual = testFactory.buildTest(instrumentation) { cfg ->
            this.setDefaultTestCfg(cfg)
            validateTest(cfg)
        }.map { it.first() as FlickerTestRunnerFactory.TestSpec }

        actual.forEachIndexed { index, testSpec ->
            val expectedCleanUp = index % 2 == 1
            assertWithMessage("Entry $index should cleanup")
                .that(testSpec.cleanUp)
                .isEqualTo(expectedCleanUp)
        }
    }

    @Test
    fun checkTransitionRunner() {
        val actual = testFactory.buildTest(instrumentation) { cfg ->
            this.setDefaultTestCfg(cfg)
            validateTest(cfg)
        }.map { it.first() as FlickerTestRunnerFactory.TestSpec }

        val tests = actual.map { it.test }

        (1 until tests.size).forEach { index ->
            val prevTest = tests[index - 1]
            val currTest = tests[index]

            // Only the middle should change
            if (index != tests.size / 2) {
                assertWithMessage("Test ${index - 1} (${prevTest.testName}) and " +
                    "$index (${currTest.testName}) tests should  share a runner")
                    .that(currTest.runner)
                    .isEqualTo(prevTest.runner)
            } else {
                assertWithMessage("Test ${index - 1} (${prevTest.testName}) and " +
                    "$index (${currTest.testName}) tests should not share a runner")
                    .that(currTest.runner)
                    .isNotEqualTo(prevTest.runner)
            }
        }

        assertWithMessage("First and last tests should not share a runner")
            .that(tests.first().runner)
            .isNotEqualTo(tests.last().runner)
    }
}