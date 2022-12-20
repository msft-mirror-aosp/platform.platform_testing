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

import android.annotation.SuppressLint
import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.flicker.ScenarioBuilder
import com.android.server.wm.flicker.TEST_SCENARIO
import com.android.server.wm.flicker.TestTraces
import com.android.server.wm.flicker.assertExceptionMessage
import com.android.server.wm.flicker.assertThrows
import com.android.server.wm.flicker.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.newTestResultWriter
import com.android.server.wm.flicker.outputFileName
import com.android.server.wm.traces.common.Timestamp
import com.google.common.truth.Truth
import java.nio.file.Files
import java.nio.file.Path
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Tests for [ResultWriter] */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressLint("VisibleForTests")
class ResultWriterTest {
    @Test
    fun cannotWriteFileWithoutScenario() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                val writer =
                    newTestResultWriter().forScenario(ScenarioBuilder().createEmptyScenario())
                writer.write()
            }

        assertExceptionMessage(exception, "Scenario shouldn't be empty")
    }

    @Test
    fun writesEmptyFile() {
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
        val writer = newTestResultWriter()
        val result = writer.write()
        val path = result.artifactPath
        Truth.assertWithMessage("File exists").that(Files.exists(path)).isTrue()
        Truth.assertWithMessage("Transition start time")
            .that(result.transitionTimeRange.start)
            .isEqualTo(Timestamp.MIN)
        Truth.assertWithMessage("Transition end time")
            .that(result.transitionTimeRange.end)
            .isEqualTo(Timestamp.MAX)
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        Truth.assertWithMessage("File count").that(reader.countFiles()).isEqualTo(0)
    }

    @Test
    fun writesUndefinedFile() {
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
        val writer =
            ResultWriter().forScenario(TEST_SCENARIO).withOutputDir(getDefaultFlickerOutputDir())
        val result = writer.write()
        val path = result.artifactPath
        validateFileName(path, RunStatus.UNDEFINED)
    }

    @Test
    fun writesRunCompleteFile() {
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
        val writer = newTestResultWriter().setRunComplete()
        val result = writer.write()
        val path = result.artifactPath
        validateFileName(path, RunStatus.RUN_EXECUTED)
    }

    @Test
    fun writesRunFailureFile() {
        Files.deleteIfExists(outputFileName(RunStatus.RUN_FAILED))
        val writer = newTestResultWriter().setRunFailed(EXPECTED_FAILURE)
        val result = writer.write()
        val path = result.artifactPath
        validateFileName(path, RunStatus.RUN_FAILED)
        Truth.assertWithMessage("Expected assertion")
            .that(result.executionError)
            .isEqualTo(EXPECTED_FAILURE)
    }

    @Test
    fun writesTransitionTime() {
        val writer =
            newTestResultWriter()
                .setTransitionStartTime(TestTraces.TIME_5)
                .setTransitionEndTime(TestTraces.TIME_10)

        val result = writer.write()
        Truth.assertWithMessage("Transition start time")
            .that(result.transitionTimeRange.start)
            .isEqualTo(TestTraces.TIME_5)
        Truth.assertWithMessage("Transition end time")
            .that(result.transitionTimeRange.end)
            .isEqualTo(TestTraces.TIME_10)
    }

    @Test
    fun writeWMTrace() {
        val writer = newTestResultWriter().addTraceResult(TraceType.WM, TestTraces.WMTrace.FILE)
        val result = writer.write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        Truth.assertWithMessage("File count").that(reader.countFiles()).isEqualTo(1)
        Truth.assertWithMessage("Has file with type")
            .that(reader.hasTraceFile(TraceType.WM))
            .isTrue()
    }

    @Test
    fun writeLayersTrace() {
        val writer = newTestResultWriter().addTraceResult(TraceType.SF, TestTraces.LayerTrace.FILE)
        val result = writer.write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        Truth.assertWithMessage("File count").that(reader.countFiles()).isEqualTo(1)
        Truth.assertWithMessage("Has file with type")
            .that(reader.hasTraceFile(TraceType.SF))
            .isTrue()
    }

    @Test
    fun writeTransactionTrace() {
        val writer =
            newTestResultWriter()
                .addTraceResult(TraceType.TRANSACTION, TestTraces.TransactionTrace.FILE)
        val result = writer.write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        Truth.assertWithMessage("File count").that(reader.countFiles()).isEqualTo(1)
        Truth.assertWithMessage("Has file with type")
            .that(reader.hasTraceFile(TraceType.TRANSACTION))
            .isTrue()
    }

    @Test
    fun writeTransitionTrace() {
        val writer =
            newTestResultWriter()
                .addTraceResult(TraceType.TRANSITION, TestTraces.TransitionTrace.FILE)
        val result = writer.write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        Truth.assertWithMessage("File count").that(reader.countFiles()).isEqualTo(1)
        Truth.assertWithMessage("Has file with type")
            .that(reader.hasTraceFile(TraceType.TRANSITION))
            .isTrue()
    }

    @Test
    fun writeAllTraces() {
        val writer =
            newTestResultWriter()
                .addTraceResult(TraceType.WM, TestTraces.WMTrace.FILE)
                .addTraceResult(TraceType.SF, TestTraces.LayerTrace.FILE)
                .addTraceResult(TraceType.TRANSITION, TestTraces.TransactionTrace.FILE)
                .addTraceResult(TraceType.TRANSACTION, TestTraces.TransitionTrace.FILE)
        val result = writer.write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        Truth.assertWithMessage("File count").that(reader.countFiles()).isEqualTo(4)
        Truth.assertWithMessage("Has file with type")
            .that(reader.hasTraceFile(TraceType.WM))
            .isTrue()
        Truth.assertWithMessage("Has file with type")
            .that(reader.hasTraceFile(TraceType.WM))
            .isTrue()
        Truth.assertWithMessage("Has file with type")
            .that(reader.hasTraceFile(TraceType.TRANSITION))
            .isTrue()
        Truth.assertWithMessage("Has file with type")
            .that(reader.hasTraceFile(TraceType.TRANSACTION))
            .isTrue()
    }

    companion object {
        private val EXPECTED_FAILURE = IllegalArgumentException("Expected test exception")

        private fun validateFileName(filePath: Path, status: RunStatus) {
            Truth.assertWithMessage("File name contains run status")
                .that(filePath.fileName.toString())
                .contains(status.prefix)
        }
    }
}
