/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.compatibility.common.util.ZipUtil
import com.android.server.wm.flicker.assertions.FlickerAssertionError
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.dsl.AssertionTag
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Defines the result of a flicker run
 */
class FlickerRunResult private constructor(
    /**
     * Path to the trace files associated with the result (incl. screen recording)
     */
    @JvmField val traceFile: Path?,
    /**
     * Determines which assertions to run (e.g., start, end, all, or a custom tag)
     */
    @JvmField var assertionTag: String,
    /**
     * Truth subject that corresponds to a [WindowManagerTrace] or [WindowManagerState]
     */
    internal val wmSubject: FlickerSubject?,
    /**
     * Truth subject that corresponds to a [LayersTrace] or [LayerTraceEntry]
     */
    internal val layersSubject: FlickerSubject?,
    /**
     * Truth subject that corresponds to a list of [FocusEvent]
     */
    @VisibleForTesting
    val eventLogSubject: EventLogSubject?
) {
    fun getSubjects(): List<FlickerSubject> {
        val result = mutableListOf<FlickerSubject>()

        wmSubject?.run { result.add(this) }
        layersSubject?.run { result.add(this) }
        eventLogSubject?.run { result.add(this) }

        return result
    }

    /**
     * Rename the trace files according to the run status (pass/fail)
     *
     * @param failures List of all failures during the flicker execution
     */
    fun saveTraces(failures: List<FlickerAssertionError>) {
        val containsFailure = containsFailure(failures)
        saveTraceFile(containsFailure)
    }

    private fun saveTraceFile(isFailure: Boolean) {
        if (traceFile == null || !Files.exists(traceFile)) {
            return
        }
        try {
            val prefix = if (isFailure) FAIL_PREFIX else PASS_PREFIX
            val newFileName = prefix + traceFile.fileName.toString()
            val target = traceFile.resolveSibling(newFileName)
            Files.move(traceFile, target, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            Log.e(FLICKER_TAG, "Unable do delete $this", e)
        }
    }

    private fun containsFailure(failures: List<FlickerAssertionError>): Boolean {
        return failures.mapNotNull { it.traceFile }.any { failureTrace ->
            traceFile == failureTrace
        }
    }

    /**
     * Parse a [trace] into a [SubjectType] asynchronously
     *
     * The parsed subject is available in [promise]
     */
    class AsyncSubjectParser<SubjectType : FlickerTraceSubject<*>>(
        val trace: Path,
        parser: ((Path) -> SubjectType?)?
    ) {
        val promise: Deferred<SubjectType?>? = parser?.run { SCOPE.async { parser(trace) } }
    }

    class Builder {
        private var wmTraceData: AsyncSubjectParser<WindowManagerTraceSubject>? = null
        private var layersTraceData: AsyncSubjectParser<LayersTraceSubject>? = null
        var screenRecording: Path? = null

        /**
         * List of focus events, if collected
         */
        var eventLog: List<FocusEvent>? = null

        /**
         * Parses a [WindowManagerTraceSubject]
         *
         * @param path of the trace file to parse
         * @param parser lambda to parse the trace into a [WindowManagerTraceSubject]
         */
        fun setWmTrace(path: Path, parser: (Path) -> WindowManagerTraceSubject?) {
            wmTraceData = AsyncSubjectParser(path, parser)
        }

        /**
         * Parses a [LayersTraceSubject]
         *
         * @param path of the trace file to parse
         * @param parser lambda to parse the trace into a [LayersTraceSubject]
         */
        fun setLayersTrace(path: Path, parser: (Path) -> LayersTraceSubject?) {
            layersTraceData = AsyncSubjectParser(path, parser)
        }

        private fun buildResult(
            assertionTag: String,
            wmSubject: FlickerSubject?,
            layersSubject: FlickerSubject?,
            traceFile: Path? = null,
            eventLogSubject: EventLogSubject? = null
        ): FlickerRunResult {
            return FlickerRunResult(
                traceFile,
                assertionTag,
                wmSubject,
                layersSubject,
                eventLogSubject
            )
        }

        /**
         * Builds a new [FlickerRunResult] for a trace
         *
         * @param assertionTag Tag to associate with the result
         * @param wmTrace WindowManager trace
         * @param layersTrace Layers trace
         */
        fun buildStateResult(
            assertionTag: String,
            wmTrace: WindowManagerTrace?,
            layersTrace: LayersTrace?
        ): FlickerRunResult {
            val wmSubject = wmTrace?.let { WindowManagerTraceSubject.assertThat(it).first() }
            val layersSubject = layersTrace?.let { LayersTraceSubject.assertThat(it).first() }
            return buildResult(assertionTag, wmSubject, layersSubject)
        }

        @VisibleForTesting
        fun buildEventLogResult(): FlickerRunResult {
            val events = eventLog ?: emptyList()
            return buildResult(
                AssertionTag.ALL,
                wmSubject = null,
                layersSubject = null,
                eventLogSubject = EventLogSubject.assertThat(events)
            )
        }

        @VisibleForTesting
        fun buildTraceResults(
            testName: String,
            iteration: Int
        ): List<FlickerRunResult> = runBlocking {
            val wmSubject = wmTraceData?.promise?.await()
            val layersSubject = layersTraceData?.promise?.await()

            val traceFile = compress(testName, iteration)
            val traceResult = buildResult(
                AssertionTag.ALL, wmSubject, layersSubject, traceFile = traceFile)
            val initialStateResult = buildResult(
                AssertionTag.START, wmSubject?.first(), layersSubject?.first())
            val finalStateResult = buildResult(
                AssertionTag.END, wmSubject?.last(), layersSubject?.last())

            listOf(initialStateResult, finalStateResult, traceResult)
        }

        private fun compress(testName: String, iteration: Int): Path? {
            val traceFiles = mutableListOf<File>()
            wmTraceData?.trace?.let { traceFiles.add(it.toFile()) }
            layersTraceData?.trace?.let { traceFiles.add(it.toFile()) }
            screenRecording?.let { traceFiles.add(it.toFile()) }

            val files = traceFiles.filter { it.exists() }
            if (files.isEmpty()) {
                return null
            }

            val firstFile = files.first()
            val compressedFile = firstFile.resolveSibling("${testName}_$iteration.zip")
            ZipUtil.createZip(traceFiles, compressedFile)
            traceFiles.forEach {
                it.delete()
            }

            return compressedFile.toPath()
        }

        fun buildAll(testName: String, iteration: Int): List<FlickerRunResult> {
            val result = buildTraceResults(testName, iteration).toMutableList()
            if (eventLog != null) {
                result.add(buildEventLogResult())
            }

            return result
        }
    }

    companion object {
        private const val PASS_PREFIX = "PASS_"
        private const val FAIL_PREFIX = "FAIL_"
        private val SCOPE = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}
