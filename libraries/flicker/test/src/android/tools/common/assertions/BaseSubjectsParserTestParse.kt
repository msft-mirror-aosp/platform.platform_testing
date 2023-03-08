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

package android.tools.common.assertions

import android.tools.InitRule
import android.tools.common.Tag
import android.tools.common.Timestamp
import android.tools.common.flicker.subject.FlickerSubject
import android.tools.common.flicker.subject.FlickerTraceSubject
import android.tools.common.io.RunStatus
import android.tools.common.io.TraceType
import android.tools.device.traces.DEFAULT_TRACE_CONFIG
import android.tools.device.traces.deleteIfExists
import android.tools.device.traces.io.ResultReader
import android.tools.device.traces.io.ResultWriter
import android.tools.newTestResultWriter
import android.tools.outputFileName
import com.google.common.truth.Truth
import java.io.File
import org.junit.Before
import org.junit.ClassRule
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
        outputFileName(RunStatus.RUN_EXECUTED).deleteIfExists()
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
        doParseStateSubjectAndValidate(Tag.START, expectedStartTime)
    }

    @Test
    fun parseStateSubjectTagEnd() {
        doParseStateSubjectAndValidate(Tag.END, expectedEndTime)
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

    companion object {
        @ClassRule @JvmField val initRule = InitRule()
    }
}
