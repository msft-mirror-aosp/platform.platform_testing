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

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.AssertionTag
import com.android.server.wm.flicker.TraceConfig
import com.android.server.wm.flicker.TraceConfigs
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.events.CujTrace
import com.android.server.wm.traces.common.events.EventLog
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.parser.events.EventLogParser
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.layers.LayersTraceParser
import com.android.server.wm.traces.parser.transaction.TransactionsTraceParser
import com.android.server.wm.traces.parser.transition.TransitionsTraceParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerDumpParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Helper class to read results from a flicker artifact
 *
 * @param result to read from
 * @param traceConfig
 */
open class ResultReader(internal var result: ResultData, internal val traceConfig: TraceConfigs) :
    IReader {
    override val artifactPath
        get() = result.artifactPath
    override val runStatus
        get() = result.runStatus
    internal val transitionTimeRange
        get() = result.transitionTimeRange
    override val isFailure
        get() = runStatus.isFailure
    override val executionError
        get() = result.executionError

    private fun withZipFile(predicate: (ZipInputStream) -> Unit) {
        val zipInputStream =
            ZipInputStream(
                BufferedInputStream(ByteArrayInputStream(result.getArtifactBytes()), BUFFER_SIZE)
            )
        try {
            predicate(zipInputStream)
        } finally {
            zipInputStream.closeEntry()
            zipInputStream.close()
        }
    }

    private fun forEachFileInZip(predicate: (ZipEntry) -> Unit) {
        withZipFile {
            var zipEntry: ZipEntry? = it.nextEntry
            while (zipEntry != null) {
                predicate(zipEntry)
                zipEntry = it.nextEntry
            }
        }
    }

    @Throws(IOException::class)
    private fun readFromZip(descriptor: ResultArtifactDescriptor): ByteArray? {
        Log.d(FLICKER_IO_TAG, "Reading descriptor=$descriptor from $result")

        var foundFile = false
        val outByteArray = ByteArrayOutputStream()
        val tmpBuffer = ByteArray(BUFFER_SIZE)
        withZipFile {
            var zipEntry: ZipEntry? = it.nextEntry
            while (zipEntry != null) {
                if (zipEntry.name == descriptor.fileNameInArtifact) {
                    val outputStream = BufferedOutputStream(outByteArray, BUFFER_SIZE)
                    try {
                        var size = it.read(tmpBuffer, 0, BUFFER_SIZE)
                        while (size > 0) {
                            outputStream.write(tmpBuffer, 0, size)
                            size = it.read(tmpBuffer, 0, BUFFER_SIZE)
                        }
                        it.closeEntry()
                    } finally {
                        outputStream.flush()
                        outputStream.close()
                    }
                    foundFile = true
                    break
                }
                zipEntry = it.nextEntry
            }
        }

        return if (foundFile) outByteArray.toByteArray() else null
    }

    /**
     * @return a [WindowManagerTrace] from the dump associated to [tag]
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readWmState(tag: String): WindowManagerTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.WM_DUMP, tag)
        Log.d(FLICKER_IO_TAG, "Reading WM trace descriptor=$descriptor from $result")
        val traceData = readFromZip(descriptor)
        return traceData?.let { WindowManagerDumpParser().parse(it, clearCache = true) }
    }

    /**
     * @return a [WindowManagerTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readWmTrace(): WindowManagerTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.WM)
        return readFromZip(descriptor)?.let {
            val trace =
                WindowManagerTraceParser()
                    .parse(
                        it,
                        from = transitionTimeRange.start,
                        to = transitionTimeRange.end,
                        addInitialEntry = true,
                        clearCache = true
                    )
            val minimumEntries = minimumTraceEntriesForConfig(traceConfig.wmTrace)
            require(trace.entries.size >= minimumEntries) {
                "WM trace contained ${trace.entries.size} entries, " +
                    "expected at least $minimumEntries... :: " +
                    "transition starts at ${transitionTimeRange.start} and " +
                    "ends at ${transitionTimeRange.end}."
            }
            trace
        }
    }

    /**
     * @return a [LayersTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readLayersTrace(): LayersTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.SF)
        return readFromZip(descriptor)?.let {
            val trace =
                LayersTraceParser()
                    .parse(
                        it,
                        transitionTimeRange.start,
                        transitionTimeRange.end,
                        addInitialEntry = true,
                        clearCache = true
                    )
            val minimumEntries = minimumTraceEntriesForConfig(traceConfig.layersTrace)
            require(trace.entries.size >= minimumEntries) {
                "Layers trace contained ${trace.entries.size} entries, " +
                    "expected at least $minimumEntries... :: " +
                    "transition starts at ${transitionTimeRange.start} and " +
                    "ends at ${transitionTimeRange.end}."
            }
            trace
        }
    }

    /**
     * @return a [LayersTrace] from the dump associated to [tag]
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readLayersDump(tag: String): LayersTrace? {
        val descriptor = ResultArtifactDescriptor(TraceType.SF_DUMP, tag)
        val traceData = readFromZip(descriptor)
        return traceData?.let { LayersTraceParser().parse(it, clearCache = true) }
    }

    /**
     * @return a [TransactionsTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readTransactionsTrace(): TransactionsTrace? =
        doReadTransactionsTrace(from = transitionTimeRange.start, to = transitionTimeRange.end)

    private fun doReadTransactionsTrace(from: Timestamp, to: Timestamp): TransactionsTrace? {
        val traceData = readFromZip(ResultArtifactDescriptor(TraceType.TRANSACTION))
        return traceData?.let {
            val trace = TransactionsTraceParser().parse(it, from, to, addInitialEntry = true)
            require(trace.entries.isNotEmpty()) { "Transactions trace cannot be empty" }
            trace
        }
    }

    /**
     * @return a [TransitionsTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readTransitionsTrace(): TransitionsTrace? {
        val traceData = readFromZip(ResultArtifactDescriptor(TraceType.TRANSITION))
        if (traceData == null) {
            return null
        }

        val fullTrace = TransitionsTraceParser().parse(traceData)
        val trace = fullTrace.slice(transitionTimeRange.start, transitionTimeRange.end)
        if (!traceConfig.transitionsTrace.allowNoChange) {
            require(trace.entries.isNotEmpty()) { "Transitions trace cannot be empty" }
        }
        return trace
    }

    private fun minimumTraceEntriesForConfig(config: TraceConfig): Int {
        return if (config.allowNoChange) 1 else 2
    }

    /**
     * @return an [EventLog] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readEventLogTrace(): EventLog? {
        val descriptor = ResultArtifactDescriptor(TraceType.EVENT_LOG)
        return readFromZip(descriptor)?.let {
            EventLogParser()
                .parse(it, from = transitionTimeRange.start, to = transitionTimeRange.end)
        }
    }

    /**
     * @return a [CujTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    override fun readCujTrace(): CujTrace? = readEventLogTrace()?.cujTrace

    /** @return an [IReader] for the subsection of the trace we are reading in this reader */
    override fun slice(startTimestamp: Timestamp, endTimestamp: Timestamp): ResultReader {
        require(startTimestamp.hasAllTimestamps)
        require(endTimestamp.hasAllTimestamps)
        val slicedResult =
            ResultData(
                result.artifactPath,
                TransitionTimeRange(startTimestamp, endTimestamp),
                result.executionError,
                result.runStatus
            )
        return ResultReader(slicedResult, traceConfig)
    }

    override fun toString(): String = "$result"

    /** @return the number of files in the artifact */
    @VisibleForTesting
    fun countFiles(): Int {
        var count = 0
        forEachFileInZip { count++ }
        return count
    }

    /** @return if a file with type [traceType] linked to a [tag] exists in the artifact */
    @VisibleForTesting
    fun hasTraceFile(traceType: TraceType, tag: String = AssertionTag.ALL): Boolean {
        val descriptor = ResultArtifactDescriptor(traceType, tag)
        var found = false
        forEachFileInZip { found = found || (it.name == descriptor.fileNameInArtifact) }
        return found
    }
}
