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

package com.android.server.wm.parser.layers

import com.android.server.wm.flicker.readAsset
import com.android.server.wm.traces.common.Cache
import com.android.server.wm.traces.parser.layers.LayersTraceParser
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test

class LayerTraceParserTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun canParseFromTrace() {
        val trace = LayersTraceParser().parse(readAsset("layers_trace_occluded.pb"))
        Truth.assertWithMessage("Trace").that(trace).isNotEmpty()
        Truth.assertWithMessage("Trace contains entry")
            .that(trace.entries.map { it.elapsedTimestamp })
            .contains(1700382131522L)
    }

    @Test
    fun canParseFromDumpWithDisplay() {
        val trace = LayersTraceParser().parse(readAsset("layers_dump_with_display.pb"))
        Truth.assertWithMessage("Dump").that(trace).isNotEmpty()
        Truth.assertWithMessage("Dump contains display")
            .that(trace.first().displays)
            .asList()
            .isNotEmpty()
    }
}
