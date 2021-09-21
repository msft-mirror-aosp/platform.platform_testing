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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.flicker.readLayerTraceFromFile
import com.android.server.wm.flicker.readTestFile
import com.android.server.wm.flicker.readWmTraceFromFile
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [RotationAssertions] tests. To run this test:
 * `atest FlickerLibTest:RotationAssertionsTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RotationAssertionsTest {
    private val jsonByteArray = readTestFile("assertors/rotation/config.json")
    private val assertorConfiguration =
        AssertionConfigParser.parseConfigFile(String(jsonByteArray))
            .first { it.name.contains("RotationAssertions") }

    private val rotationAssertor = TransitionAssertor(assertorConfiguration) { }

    @Test
    fun testValidRotationWmTrace() {
        val trace = readWmTraceFromFile("assertors/rotation/WindowManagerTrace.winscope")
        val errorTrace = rotationAssertor.analyzeWmTrace(trace)

        Truth.assertThat(errorTrace).isEmpty()
    }

    @Test
    fun testValidRotationLayersTrace() {
        val trace = readLayerTraceFromFile("assertors/rotation/SurfaceFlingerTrace.winscope")
        val errorTrace = rotationAssertor.analyzeLayersTrace(trace)

        Truth.assertThat(errorTrace).isEmpty()
//        Truth.assertThat(errorTrace.entries[0].errors).isEqualTo(arrayOf(Error(stacktrace = "ksnvds", message = "jsbfdj")))
    }

    @Test
    fun testInvalidRotationLayersTrace() {
        val trace = readLayerTraceFromFile(
        "assertors/rotation/SurfaceFlingerInvalidTrace.winscope"
        )
        val errorTrace = rotationAssertor.analyzeLayersTrace(trace)

        Truth.assertThat(errorTrace).isNotEmpty()
        Truth.assertThat(errorTrace.entries.size).isEqualTo(1)
    }
}