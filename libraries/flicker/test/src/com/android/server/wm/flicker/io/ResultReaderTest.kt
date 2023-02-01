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

package com.android.server.wm.flicker.io

import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.flicker.newTestResultWriter
import com.android.server.wm.flicker.outputFileName
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import org.junit.Assert
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Tests for [ResultReader] */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ResultReaderTest {
    @Before
    fun setup() {
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
    }

    @Test
    fun failFileNotFound() {
        val data = newTestResultWriter().write()
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
        val reader = ResultReader(data, DEFAULT_TRACE_CONFIG)
        Assert.assertThrows(NoSuchFileException::class.java) {
            reader.readTransitionsTrace() ?: error("Should have failed")
        }
    }
}
