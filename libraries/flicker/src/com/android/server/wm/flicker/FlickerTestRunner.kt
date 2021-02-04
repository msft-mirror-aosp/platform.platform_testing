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
 * @param testSpec Flicker test specification
 */
abstract class FlickerTestRunner(protected val testSpec: FlickerTestRunnerFactory.TestSpec) {
    @get:Rule
    val flickerTestRule = FlickerTestRule(testSpec)

    private fun checkRequirements(onlyFlaky: Boolean) {
        if (testSpec.assertionName.isNotEmpty()) {
            val isTestEnabled = flickerTestRule.flicker.assertions
                .firstOrNull { it.name == testSpec.assertionName }?.enabled ?: false
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
        flickerTestRule.flicker.checkAssertions(testSpec.assertionName, onlyFlaky = false)
    }

    /**
     * Run all trace assertions (including disabled) on the recorded traces.
     */
    @FlakyTest
    @Test
    fun testFlaky() {
        checkRequirements(onlyFlaky = true)
        flickerTestRule.flicker.checkAssertions(testSpec.assertionName, onlyFlaky = true)
    }
}