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

class TransitionErrorTest {
    private var assertionExecuted = false
    private val testParam = FlickerTest().also { it.initialize(TEST_SCENARIO.testClass) }

    @Before
    fun setup() {
        assertionExecuted = false
    }

    @Test
    fun failsToExecuteTransition() {
        val reader = CachedResultReader(TEST_SCENARIO, DEFAULT_TRACE_CONFIG)
        Truth.assertWithMessage("Run status").that(reader.runStatus).isEqualTo(RunStatus.RUN_FAILED)
        assertArtifactExists()
    }

    @Test
    fun assertThrowsTransitionError() {
        val results =
            (0..10).map {
                runCatching {
                    testParam.assertLayers {
                        assertionExecuted = true
                        error(WRONG_EXCEPTION)
                    }
                }
            }

        Truth.assertWithMessage("Executed").that(assertionExecuted).isFalse()
        results.forEach { result ->
            Truth.assertWithMessage("Expected exception").that(result.isFailure).isTrue()
            Truth.assertWithMessage("Expected exception")
                .that(result.exceptionOrNull())
                .hasMessageThat()
                .contains(Utils.FAILURE)
        }
        val reader = CachedResultReader(TEST_SCENARIO, DEFAULT_TRACE_CONFIG)
        Truth.assertWithMessage("Run status").that(reader.runStatus).isEqualTo(RunStatus.RUN_FAILED)
        assertArtifactExists()
    }

    private fun assertArtifactExists() {
        val reader = CachedResultReader(TEST_SCENARIO, DEFAULT_TRACE_CONFIG)
        Truth.assertWithMessage("Files exist").that(Files.exists(reader.artifactPath)).isTrue()
    }

    companion object {
        private const val WRONG_EXCEPTION = "Wrong exception"

        @BeforeClass @JvmStatic fun runTransition() = Utils.runTransition { error(Utils.FAILURE) }
    }
}
