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

import androidx.test.filters.FlakyTest
import com.google.common.truth.Truth
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
 */
abstract class FlickerTestRunner(testName: String, private val flickerSpec: Flicker) {
    @get:Rule
    val flickerTestRule = FlickerTestRule(flickerSpec)

    /**
     * Tests if the transition executed successfully
     */
    @Test
    fun checkTransition() {
        Truth.assertWithMessage(flickerSpec.error?.message).that(flickerSpec.error).isNull()
    }

    /**
     * Run only the enabled assertions on the recorded traces.
     */
    @Test
    fun checkAssertions() {
        flickerSpec.checkAssertions(includeFlakyAssertions = false)
    }

    /**
     * Run all trace assertions (including disabled) on the recorded traces.
     */
    @FlakyTest
    @Test
    fun checkFlakyAssertions() {
        flickerSpec.checkAssertions(includeFlakyAssertions = true)
    }
}