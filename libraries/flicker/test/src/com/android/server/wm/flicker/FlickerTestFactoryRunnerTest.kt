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

import android.os.Bundle
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [FlickerTestRunnerFactory] tests.
 *
 * To run this test: `atest FlickerLibTest:FlickerTestFacroty`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FlickerTestFactoryRunnerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val defaultRotations = listOf(Surface.ROTATION_0, Surface.ROTATION_90)

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
        val actual = factory.buildTest { cfg -> validateTest(cfg) }
        assertWithMessage("Flicker should create tests for 0 and 90 degrees")
            .that(actual).hasSize(2)
    }

    @Test
    fun checkBuildRotationTest() {
        val factory = FlickerTestRunnerFactory(instrumentation)
        val actual = factory.buildRotationTest { cfg -> validateRotationTest(cfg) }
        assertWithMessage("Flicker should create tests for 0 and 90 degrees")
            .that(actual).hasSize(2)
    }

    @Test
    fun checkBuildCustomRotationsTest() {
        val rotations = listOf(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180,
                Surface.ROTATION_270)
        val factory = FlickerTestRunnerFactory(instrumentation, rotations)
        val actual = factory.buildRotationTest { cfg -> validateRotationTest(cfg, rotations) }
        assertWithMessage("Flicker should create tests for 0/90/180/270 degrees")
            .that(actual).hasSize(12)
    }

    @Test
    fun checkBuildCustomPayloadTest() {
        val factory = FlickerTestRunnerFactory(instrumentation)
        val actual = listOf(Bundle().also { it.putBoolean("test", true) })
        val tests = factory.buildTest(actual) { cfg ->
            validateTest(cfg)
            assertWithMessage("Could not find custom payload data")
                .that(cfg.getBoolean("test", false)).isTrue()
        }
        assertWithMessage("Flicker should create tests for 0 and 90 degrees")
            .that(tests).hasSize(1)
    }
}