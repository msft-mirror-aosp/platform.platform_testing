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

package com.android.server.wm.flicker.assertions

import com.android.server.wm.flicker.FlickerRunResult
import java.nio.file.Path
import kotlin.AssertionError

class FlickerAssertionError(
    cause: Throwable,
    val assertion: AssertionData<*>,
    val run: FlickerRunResult,
    var trace: Path?
) : AssertionError(cause) {
    override val message: String
        get() = buildString {
            append("\n")
            append("Test failed: ")
            append(assertion.name)
            append("\n")
            append("Iteration: ")
            append(run.iteration)
            append("\n")
            append("Trace: ")
            append(trace)
            append("\n")
            cause?.message?.let { append(it) }
        }
}