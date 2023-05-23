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

package android.tools.device.traces.monitors

import android.tools.common.ScenarioBuilder
import android.tools.common.io.TraceType
import android.tools.device.traces.TRACE_CONFIG_REQUIRE_CHANGES
import android.tools.device.traces.io.IoUtils
import android.tools.device.traces.io.ResultReader
import android.tools.device.traces.io.ResultWriter
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Base class for monitors containing common logic to read the trace as a byte array and save the
 * trace to another location.
 */
abstract class TraceMonitor : ITransitionMonitor {
    abstract val isEnabled: Boolean
    abstract val traceType: TraceType
    protected abstract fun doStart()
    protected abstract fun doStop(): File

    final override fun start() {
        validateStart()
        doStart()
    }

    open fun validateStart() {
        if (this.isEnabled) {
            throw UnsupportedOperationException(
                "Trace already running. " + "This is likely due to chained 'withTracing' calls."
            )
        }
    }

    /** Stops monitor. */
    final override fun stop(writer: ResultWriter) {
        val artifact =
            try {
                val srcFile = doStop()
                moveTraceFileToTmpDir(srcFile)
            } catch (e: Throwable) {
                throw RuntimeException("Could not stop trace", e)
            }
        writer.addTraceResult(traceType, artifact)
    }

    private fun moveTraceFileToTmpDir(sourceFile: File): File {
        val newFile = File.createTempFile(sourceFile.name, "")
        IoUtils.moveFile(sourceFile, newFile)
        require(newFile.exists()) { "Unable to save trace file $newFile" }
        return newFile
    }

    /**
     * Uses [writer] to write the trace generated by executing the commands defined by [predicate].
     *
     * @param writer Write to use to write the collected traces
     * @param predicate Commands to execute
     * @throws UnsupportedOperationException If tracing is already activated
     */
    fun withTracing(writer: ResultWriter, predicate: () -> Unit) {
        try {
            this.start()
            predicate()
        } finally {
            this.stop(writer)
        }
    }

    /**
     * Acquires the trace generated when executing the commands defined in the [predicate].
     *
     * @param predicate Commands to execute
     * @throws UnsupportedOperationException If tracing is already activated
     */
    fun withTracing(predicate: () -> Unit): ByteArray {
        val writer = createWriter()
        withTracing(writer, predicate)
        val result = writer.write()
        val reader = ResultReader(result, TRACE_CONFIG_REQUIRE_CHANGES)
        val bytes = reader.readBytes(traceType) ?: error("Missing trace $traceType")
        result.artifact.deleteIfExists()
        return bytes
    }

    private fun createWriter(): ResultWriter {
        val className = this::class.simpleName ?: error("Missing class name for $this")
        val scenario = ScenarioBuilder().forClass(className).build()
        val tmpDir = createTempDirectory("withTracing").toFile()
        return ResultWriter().forScenario(scenario).withOutputDir(tmpDir)
    }

    companion object {
        @JvmStatic protected val TRACE_DIR = File("/data/misc/wmtrace/")
    }
}
