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

    private fun FlickerBuilder.setDefaultTestCfg() = apply {
        assertions {
            layersTrace { all { fail("First assertion") } }
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
        val factory = FlickerTestRunnerFactory(instrumentation)
        val actual = factory.buildTest { cfg ->
            this.setDefaultTestCfg()
            validateTest(cfg)
        }
        // Should have 1 test for transition and 1 for the assertions in each orientation
        assertWithMessage("Flicker should create tests for 0 and 90 degrees")
            .that(actual).hasSize(4)
    }

    @Test
    fun checkBuildRotationTest() {
        val factory = FlickerTestRunnerFactory(instrumentation)
        val actual = factory.buildRotationTest { cfg ->
            this.setDefaultTestCfg()
            validateRotationTest(cfg)
        }
        // Should have 1 test for transition and 1 for the assertions in each orientation
        assertWithMessage("Flicker should create tests for 0 and 90 degrees")
            .that(actual).hasSize(4)
    }

    @Test
    fun checkBuildCustomRotationsTest() {
        val rotations = listOf(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180,
                Surface.ROTATION_270)
        val factory = FlickerTestRunnerFactory(instrumentation, rotations)
        val actual = factory.buildRotationTest { cfg ->
            this.setDefaultTestCfg()
            validateRotationTest(cfg, rotations)
        }
        // Should have 1 test for transition and 1 for the assertions in each rotation
        assertWithMessage("Flicker should create tests for 0/90/180/270 degrees")
            .that(actual).hasSize(24)
    }

    @Test
    fun checkBuildCustomPayloadTest() {
        val factory = FlickerTestRunnerFactory(instrumentation)
        val actual = listOf(Bundle().also { it.putBoolean("test", true) })
        val tests = factory.buildTest(actual) { cfg ->
            this.setDefaultTestCfg()
            validateTest(cfg)
            assertWithMessage("Could not find custom payload data")
                .that(cfg.getBoolean("test", false)).isTrue()
        }
        // Should have 1 test for transition and 1 for the assertions in each orientation
        assertWithMessage("Flicker should create 1 test for transition and 1 for assertion")
            .that(tests).hasSize(2)
    }

    private fun assertIsEmpty(producer: () -> Flicker) {
        val spec = producer.invoke()
        assertWithMessage("Should not have assertions")
            .that(spec.assertions)
            .isEmpty()
    }

    private fun assertHasSingleAssertion(producer: () -> Flicker) {
        val spec = producer.invoke()
        assertWithMessage("Should have 1 assertion")
            .that(spec.assertions)
            .hasSize(1)
    }

    @Test
    fun checkBuildOneTestPerAssertion() {
        val factory = FlickerTestRunnerFactory(instrumentation,
            supportedRotations = listOf(Surface.ROTATION_0))
        val tests = factory.buildTest {
            assertions {
                layersTrace { all { fail("First assertion") } }
                windowManagerTrace { all { fail("Second assertion") } }
                eventLog { all { fail("This assertion") } }
            }
        }

        assertWithMessage("Factory should have created 4 tests, one for transition and " +
            "3 with a single assertion each")
            .that(tests)
            .hasSize(4)

        assertIsEmpty(tests.first()[1] as () -> Flicker)
        tests.drop(1).forEach { (_, producer, _) ->
            assertHasSingleAssertion(producer as () -> Flicker)
        }
    }

    @Test
    fun mergeTestConfiguration() {
        val base: FlickerBuilder.(Bundle) -> Unit = {
            assertions {
                windowManagerTrace {
                    start { it.isEmpty }
                }
            }
        }

        val extension: FlickerBuilder.(Bundle) -> Unit = {
            assertions {
                windowManagerTrace {
                    start { it.isEmpty }
                }
            }
        }

        val factory = FlickerTestRunnerFactory(instrumentation,
                supportedRotations = listOf(Surface.ROTATION_0))
        val tests = factory.buildTest(base, extension)
        assertWithMessage("Factory should have created 3 tests, 1 for transition and 2 " +
            "tests with a single assertion each")
                .that(tests)
                .hasSize(3)

        assertIsEmpty(tests.first()[1] as () -> Flicker)
        tests.drop(1).forEach { (_, producer, _) ->
            assertHasSingleAssertion(producer as () -> Flicker)
        }
    }

    @Test
    fun checkCleanUp() {
        val factory = FlickerTestRunnerFactory(instrumentation)
        val actual = factory.buildTest { cfg ->
            this.setDefaultTestCfg()
            validateTest(cfg)
        }

        actual.forEachIndexed { index, entry ->
            val expectedCleanUp = index % 2 > 0
            val actualCleanUp = entry[2]
            val specProducer = entry[1] as () -> Flicker
            val spec = specProducer.invoke()

            assertWithMessage("Entry $index should${if (expectedCleanUp) "" else " not"} cleanup")
                .that(actualCleanUp)
                .isEqualTo(expectedCleanUp)
        }
    }

    @Test
    fun checkTransitionRunner() {
        val factory = FlickerTestRunnerFactory(instrumentation)
        val actual = factory.buildTest { cfg ->
            this.setDefaultTestCfg()
            validateTest(cfg)
        }

        val first = (actual[0][1] as () -> Flicker).invoke()
        val second = (actual[1][1] as () -> Flicker).invoke()
        val third = (actual[2][1] as () -> Flicker).invoke()
        val fourth = (actual[3][1] as () -> Flicker).invoke()

        assertWithMessage("First and second tests should share a runner")
            .that(first.runner)
            .isEqualTo(second.runner)

        assertWithMessage("Third and fourth tests should share a runner")
            .that(third.runner)
            .isEqualTo(fourth.runner)

        assertWithMessage("First and third tests should not share a runner")
            .that(first.runner)
            .isNotEqualTo(third.runner)
    }
}