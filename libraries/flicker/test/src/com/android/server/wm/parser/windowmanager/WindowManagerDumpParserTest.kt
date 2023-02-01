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

import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.readAsset
import com.android.server.wm.traces.common.Cache
import com.android.server.wm.traces.parser.FLAG_STATE_DUMP_FLAG_WM
import com.android.server.wm.traces.parser.getCurrentState
import com.android.server.wm.traces.parser.windowmanager.WindowManagerDumpParser
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test

/** Tests for [WindowManagerDumpParser] */
class WindowManagerDumpParserTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun canParseFromStoredDump() {
        val trace = WindowManagerDumpParser().parse(readAsset("wm_trace_dump.pb"))
        Truth.assertWithMessage("Unable to parse dump").that(trace).hasSize(1)
    }

    @Test
    fun canParseFromNewDump() {
        val data =
            getCurrentState(
                InstrumentationRegistry.getInstrumentation().uiAutomation,
                FLAG_STATE_DUMP_FLAG_WM
            )
        val trace = WindowManagerDumpParser().parse(data.first)
        Truth.assertWithMessage("Unable to parse dump").that(trace).hasSize(1)
    }
}
