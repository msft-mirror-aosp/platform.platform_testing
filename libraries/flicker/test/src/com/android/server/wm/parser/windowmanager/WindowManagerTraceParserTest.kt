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

package com.android.server.wm.parser.windowmanager

import com.android.server.wm.flicker.readAsset
import com.android.server.wm.traces.common.Cache
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test

/** Tests for [WindowManagerTraceParser] */
class WindowManagerTraceParserTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun canParseAllEntries() {
        val trace =
            WindowManagerTraceParser()
                .parse(readAsset("wm_trace_openchrome.pb"), clearCache = false)
        val firstEntry = trace.entries[0]
        Truth.assertThat(firstEntry.timestamp.elapsedNanos).isEqualTo(9213763541297L)
        Truth.assertThat(firstEntry.windowStates.size).isEqualTo(10)
        Truth.assertThat(firstEntry.visibleWindows.size).isEqualTo(5)
        Truth.assertThat(trace.entries[trace.entries.size - 1].timestamp.elapsedNanos)
            .isEqualTo(9216093628925L)
    }
}
