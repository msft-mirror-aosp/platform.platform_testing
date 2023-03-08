/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.common.flicker.subject.wm

import android.tools.InitRule
import android.tools.TestComponents
import android.tools.common.Cache
import android.tools.readWmTraceFromFile
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

class WindowStateSubjectTest {
    private val trace
        get() = readWmTraceFromFile("wm_trace_openchrome.pb", legacyTrace = true)

    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun exceptionContainsDebugInfoImaginary() {
        val foundWindow =
            WindowManagerTraceSubject(trace).first().windowState(TestComponents.IMAGINARY.className)
        Truth.assertWithMessage("${TestComponents.IMAGINARY.className} is not found")
            .that(foundWindow)
            .isNull()
    }

    companion object {
        @ClassRule @JvmField val initRule = InitRule()
    }
}
