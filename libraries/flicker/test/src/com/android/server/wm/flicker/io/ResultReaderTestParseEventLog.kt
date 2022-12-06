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
import com.android.server.wm.flicker.TestTraces
import com.android.server.wm.flicker.newTestResultWriter
import com.android.server.wm.flicker.outputFileName
import com.google.common.truth.Truth
import java.nio.file.Files
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Tests for [ResultReader] parsing event log */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ResultReaderTestParseEventLog {
    @Before
    fun setup() {
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
    }

    @Test
    fun readEventLog() {
        val writer = newTestResultWriter().addEventLogResult(TestTraces.TEST_EVENT_LOG)
        val result = writer.write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        val actual = reader.readEventLogTrace()
        Truth.assertWithMessage("Event log size").that(actual).hasSize(5)
        Truth.assertWithMessage("Event log")
            .that(actual)
            .containsExactlyElementsIn(TestTraces.TEST_EVENT_LOG)
    }

    @Test
    fun readEventLogAndSliceTraceByTimestamp() {
        val writer =
            newTestResultWriter()
                .setTransitionStartTime(TestTraces.TIME_5)
                .setTransitionEndTime(TestTraces.TIME_10)
                .addEventLogResult(TestTraces.TEST_EVENT_LOG)
        val result = writer.write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        val expected = TestTraces.TEST_EVENT_LOG.drop(1).dropLast(1)
        val actual = reader.readEventLogTrace()
        Truth.assertWithMessage("Event log size").that(actual).hasSize(3)
        Truth.assertWithMessage("Event log").that(actual).containsExactlyElementsIn(expected)
    }
}
