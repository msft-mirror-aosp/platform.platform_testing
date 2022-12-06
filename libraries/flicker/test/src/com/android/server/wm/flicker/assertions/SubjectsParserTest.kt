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

package com.android.server.wm.flicker.assertions

import android.annotation.SuppressLint
import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.flicker.assertExceptionMessage
import com.android.server.wm.flicker.assertThrows
import com.android.server.wm.flicker.io.ResultReader
import com.android.server.wm.flicker.newTestResultWriter
import com.android.server.wm.flicker.outputFileName
import java.io.IOException
import java.nio.file.Files
import org.junit.Before
import org.junit.Test

/** Tests for [SubjectsParser] */
@SuppressLint("VisibleForTests")
class SubjectsParserTest {
    @Before
    fun setup() {
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
    }

    @Test
    fun failFileNotFound() {
        val data = newTestResultWriter().write()
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
        val parser = SubjectsParser(ResultReader(data, DEFAULT_TRACE_CONFIG))
        val exception =
            assertThrows(IOException::class.java) {
                parser.readTransitionsTraceForTesting() ?: error("Should have failed")
            }

        assertExceptionMessage(exception, "No such file or directory")
    }
}
