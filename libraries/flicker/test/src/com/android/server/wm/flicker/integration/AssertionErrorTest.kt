/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.wm.flicker.integration

import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.FlickerTest
import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.flicker.TEST_SCENARIO
import com.android.server.wm.flicker.datastore.CachedResultReader
import com.google.common.truth.Truth
import java.nio.file.Files
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Integration tests to ensure assertions fail correctly
 *
 * To run this test: `atest FlickerLibTest:AssertionErrorTest`
 */
class AssertionErrorTest {
    private var assertionExecuted = false
    private val testParam = FlickerTest().also { it.initialize(TEST_SCENARIO.testClass) }

    @Before
    fun setup() {
        assertionExecuted = false
    }

    @Test
    fun executesTransition() {
        Truth.assertWithMessage("Transition executed").that(transitionExecuted).isTrue()
        assertArtifactExists()
    }

    @Test
    fun assertThrowsAssertionError() {
        val result = runCatching {
            testParam.assertLayers {
                assertionExecuted = true
                error(Utils.FAILURE)
            }
        }

        Truth.assertWithMessage("Executed").that(assertionExecuted).isTrue()
        Truth.assertWithMessage("Expected exception").that(result.isSuccess).isFalse()
        Truth.assertWithMessage("Expected exception")
            .that(result.exceptionOrNull())
            .hasMessageThat()
            .contains(Utils.FAILURE)
        val reader = CachedResultReader(TEST_SCENARIO, DEFAULT_TRACE_CONFIG)
        Truth.assertWithMessage("Run status")
            .that(reader.runStatus)
            .isEqualTo(RunStatus.ASSERTION_FAILED)
        assertArtifactExists()
    }

    private fun assertArtifactExists() {
        val reader = CachedResultReader(TEST_SCENARIO, DEFAULT_TRACE_CONFIG)
        Truth.assertWithMessage("Files exist").that(Files.exists(reader.artifactPath)).isTrue()
    }

    companion object {
        private var transitionExecuted = false
        @BeforeClass
        @JvmStatic
        fun runTransition() = Utils.runTransition { transitionExecuted = true }
    }
}
