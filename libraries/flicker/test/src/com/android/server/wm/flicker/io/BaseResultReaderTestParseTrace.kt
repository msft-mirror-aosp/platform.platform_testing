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
import com.android.server.wm.flicker.assertExceptionMessage
import com.android.server.wm.flicker.assertThrows
import com.android.server.wm.flicker.newTestResultWriter
import com.android.server.wm.flicker.outputFileName
import com.android.server.wm.traces.common.ITrace
import com.android.server.wm.traces.common.Timestamp
import com.google.common.truth.Truth
import java.io.File
import java.nio.file.Files
import org.junit.Before
import org.junit.Test

/** Base class for [ResultReader] tests parsing traces */
abstract class BaseResultReaderTestParseTrace {
    protected abstract val assetFile: File
    protected abstract val traceName: String
    protected abstract val startTimeTrace: Timestamp
    protected abstract val endTimeTrace: Timestamp
    protected abstract val validSliceTime: Timestamp
    protected abstract val invalidSliceTime: Timestamp
    protected abstract val traceType: TraceType
    protected abstract val expectedSlicedTraceSize: Int
    protected open val invalidSizeMessage: String
        get() = "$traceName contained 0 entries, expected at least 2"

    protected abstract fun doParse(reader: ResultReader): ITrace<*>?
    protected abstract fun getTime(traceTime: Timestamp): Long

    protected open fun setupWriter(writer: ResultWriter): ResultWriter {
        writer.addTraceResult(traceType, assetFile)
        return writer
    }

    @Before
    fun setup() {
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
    }

    @Test
    fun readTrace() {
        val writer = setupWriter(newTestResultWriter())
        val result = writer.write()

        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        val trace = doParse(reader) ?: error("$traceName not built")

        Truth.assertWithMessage(traceName).that(trace.entries).asList().isNotEmpty()
        Truth.assertWithMessage("$traceName start")
            .that(getTime(trace.entries.first().timestamp))
            .isEqualTo(getTime(startTimeTrace))
        Truth.assertWithMessage("$traceName end")
            .that(getTime(trace.entries.last().timestamp))
            .isEqualTo(getTime(endTimeTrace))
    }

    @Test
    fun readTraceNullWhenDoesNotExist() {
        val writer = newTestResultWriter()
        val result = writer.write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        val trace = doParse(reader)

        Truth.assertWithMessage(traceName).that(trace).isNull()
    }

    @Test
    fun readTraceAndSliceTraceByTimestamp() {
        val result =
            setupWriter(newTestResultWriter())
                .setTransitionStartTime(startTimeTrace)
                .setTransitionEndTime(validSliceTime)
                .write()
        val reader = ResultReader(result, TestTraces.TEST_TRACE_CONFIG)
        val trace = doParse(reader) ?: error("$traceName not built")

        Truth.assertWithMessage(traceName)
            .that(trace.entries)
            .asList()
            .hasSize(expectedSlicedTraceSize)
        Truth.assertWithMessage("$traceName start")
            .that(getTime(trace.entries.first().timestamp))
            .isEqualTo(getTime(startTimeTrace))
    }

    @Test
    fun readTraceAndSliceTraceByTimestampAndFailInvalidSize() {
        val result =
            setupWriter(newTestResultWriter()).setTransitionEndTime(Timestamp.EMPTY).write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        val exception =
            assertThrows<IllegalArgumentException> {
                doParse(reader) ?: error("$traceName not built")
            }
        assertExceptionMessage(exception, invalidSizeMessage)
    }
}
