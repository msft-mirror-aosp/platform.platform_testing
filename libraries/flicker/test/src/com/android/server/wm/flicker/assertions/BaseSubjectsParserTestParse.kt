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

import com.android.server.wm.flicker.AssertionTag
import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.flicker.io.ResultReader
import com.android.server.wm.flicker.io.ResultWriter
import com.android.server.wm.flicker.io.TraceType
import com.android.server.wm.flicker.newTestResultWriter
import com.android.server.wm.flicker.outputFileName
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.traces.common.Timestamp
import com.google.common.truth.Truth
import java.io.File
import java.nio.file.Files
import org.junit.Before
import org.junit.Test

abstract class BaseSubjectsParserTestParse {
    protected abstract val assetFile: File
    protected abstract val subjectName: String
    protected abstract val expectedStartTime: Timestamp
    protected abstract val expectedEndTime: Timestamp
    protected abstract val traceType: TraceType

    protected abstract fun getTime(timestamp: Timestamp): Long

    protected abstract fun doParseTrace(parser: TestSubjectsParser): FlickerTraceSubject<*>?

    protected abstract fun doParseState(parser: TestSubjectsParser, tag: String): FlickerSubject?

    protected open fun writeTrace(writer: ResultWriter): ResultWriter {
        writer.addTraceResult(traceType, assetFile)
        return writer
    }

    @Before
    fun setup() {
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
    }

    @Test
    fun parseTraceSubject() {
        val writer = writeTrace(newTestResultWriter())
        val result = writer.write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        val parser = TestSubjectsParser(reader)
        val subject = doParseTrace(parser) ?: error("$subjectName not built")

        Truth.assertWithMessage(subjectName).that(subject.subjects).isNotEmpty()
        Truth.assertWithMessage("$subjectName start")
            .that(getTime(subject.subjects.first().timestamp))
            .isEqualTo(getTime(expectedStartTime))
        Truth.assertWithMessage("$subjectName end")
            .that(getTime(subject.subjects.last().timestamp))
            .isEqualTo(getTime(expectedEndTime))
    }

    @Test
    fun parseStateSubjectTagStart() {
        doParseStateSubjectAndValidate(AssertionTag.START, expectedStartTime)
    }

    @Test
    fun parseStateSubjectTagEnd() {
        doParseStateSubjectAndValidate(AssertionTag.END, expectedEndTime)
    }

    @Test
    fun readTraceNullWhenDoesNotExist() {
        val writer = newTestResultWriter()
        val result = writer.write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        val parser = TestSubjectsParser(reader)
        val subject = doParseTrace(parser)

        Truth.assertWithMessage(subjectName).that(subject).isNull()
    }

    private fun doParseStateSubjectAndValidate(tag: String, expectedTime: Timestamp) {
        val writer = writeTrace(newTestResultWriter())
        val result = writer.write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        val parser = TestSubjectsParser(reader)
        val subject = doParseState(parser, tag) ?: error("$subjectName tag=$tag not built")

        Truth.assertWithMessage("$subjectName - $tag")
            .that(getTime(subject.timestamp))
            .isEqualTo(getTime(expectedTime))
    }
}
