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

import androidx.test.filters.FlakyTest
import org.junit.Assume
import org.junit.Rule
import org.junit.Test

/**
 * Flicker test runner compatible with JUnit
 *
 * Executes the test and run setup as well as the transition using @Before and
 * the test and run teardown using @After.
 *
 * All the enabled assertions are created in a single test and all flaky assertions are created on
 * a second test annotated with @FlakyTest
 *
 * @param testName Name of the test. Appears on log outputs and test dashboards
 * @param flickerSpec Flicker test to execute
 * @param cleanUp If this test should delete the traces and screen recording files if it passes
 */
abstract class FlickerTestRunner(
    testName: String,
    private val flickerProvider: () -> Flicker,
    private val cleanUp: Boolean
) {
    private val flickerSpec = flickerProvider.invoke()

    @get:Rule
    val flickerTestRule = FlickerTestRule(flickerSpec)

    private fun checkRequirements(onlyFlaky: Boolean) {
        if (flickerSpec.assertions.size == 1) {
            val isTestEnabled = flickerSpec.assertions.first().enabled
            if (onlyFlaky) {
                Assume.assumeFalse(isTestEnabled)
            } else {
                Assume.assumeTrue(isTestEnabled)
            }
        }
    }

    /**
     * Run only the enabled assertions on the recorded traces.
     */
    @Test
    fun test() {
        checkRequirements(onlyFlaky = false)
        flickerSpec.checkIsExecuted()
        flickerSpec.checkAssertions(onlyFlaky = false)
        if (cleanUp) {
            flickerSpec.cleanUp()
        }
    }

    /**
     * Run all trace assertions (including disabled) on the recorded traces.
     */
    @FlakyTest
    @Test
    fun testFlaky() {
        checkRequirements(onlyFlaky = true)
        flickerSpec.checkIsExecuted()
        flickerSpec.checkAssertions(onlyFlaky = true)
        if (cleanUp) {
            flickerSpec.cleanUp()
        }
    }
}