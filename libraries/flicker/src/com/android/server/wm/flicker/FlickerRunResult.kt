/*
 * Copyright (C) 2020 The Android Open Source Project
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
import com.android.server.wm.flicker.assertions.FlickerAssertionError
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.flicker.dsl.AssertionTag
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import com.android.server.wm.traces.parser.layers.LayersTrace
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Defines the result of a flicker run
 */
class FlickerRunResult private constructor(
    /**
     * Determines which assertions to run (e.g., start, end, all, or a custom tag)
     */
    var assertionTag: AssertionTag,
    /**
     * Run identifier
     */
    val iteration: Int,
    /**
     * Path to the WindowManager trace file, if collected
     */
    @JvmField val wmTraceFile: Path?,
    /**
     * Path to the SurfaceFlinger trace file, if collected
     */
    @JvmField val layersTraceFile: Path?,
    /**
     * Path to screen recording of the run, if collected
     */
    @JvmField val screenRecording: Path?,

    /**
     * List of focus events, if collected
     */
    val eventLog: List<FocusEvent>,
    /**
     * Parse a [WindowManagerTrace]
     */
    val parseWmTrace: (() -> WindowManagerTrace?)?,
    /**
     * Parse a [WindowManagerTrace]
     */
    val parseLayersTrace: (() -> LayersTrace?)?
) {
    /**
     * [WindowManagerTrace] that corresponds to [wmTraceFile], or null if the
     * path is invalid
     */
    val wmTrace: WindowManagerTrace? get() = parseWmTrace?.invoke()
    /**
     * [LayersTrace] that corresponds to [layersTrace], or null if the
     * path is invalid
     */
    val layersTrace: LayersTrace? get() = parseLayersTrace?.invoke()

    constructor(
        assertionTag: AssertionTag,
        iteration: Int,
        wmTraceFile: Path?,
        layersTraceFile: Path?,
        screenRecording: Path?,
        eventLog: List<FocusEvent>
    ) : this(
        assertionTag,
        iteration,
        wmTraceFile,
        layersTraceFile,
        screenRecording,
        eventLog,
        parseWmTrace = {
            wmTraceFile?.let {
                val traceData = Files.readAllBytes(it)
                WindowManagerTraceParser.parseFromTrace(traceData)
            }
        },
        parseLayersTrace = {
            layersTraceFile?.let {
                val traceData = Files.readAllBytes(it)
                LayersTrace.parseFrom(traceData)
            }
        }
    )

    constructor(
        assertionTag: AssertionTag,
        iteration: Int,
        wmTraceFile: Path?,
        layersTraceFile: Path?,
        wmTrace: WindowManagerTrace?,
        layersTrace: LayersTrace?
    ) : this(
        assertionTag,
        iteration,
        wmTraceFile,
        layersTraceFile,
        screenRecording = null,
        eventLog = emptyList(),
        parseWmTrace = { wmTrace },
        parseLayersTrace = { layersTrace }
    )

    private fun Path?.tryDelete() {
        try {
            this?.let { Files.deleteIfExists(it) }
        } catch (e: IOException) {
            Log.e(FLICKER_TAG, "Unable do delete $this", e)
        }
    }

    fun canDelete(failures: List<FlickerAssertionError>): Boolean {
        return failures.map { it.trace }.none {
            it == this.wmTraceFile || it == this.layersTraceFile
        }
    }

    /**
     * Delete the trace files collected
     */
    fun cleanUp() {
        wmTraceFile.tryDelete()
        layersTraceFile.tryDelete()
        screenRecording.tryDelete()
    }

    class Builder @JvmOverloads constructor(private val iteration: Int = 0) {
        /**
         * Path to the WindowManager trace file, if collected
         */
        var wmTraceFile: Path? = null

        /**
         * Path to the SurfaceFlinger trace file, if collected
         */
        var layersTraceFile: Path? = null

        /**
         * Path to screen recording of the run, if collected
         */
        var screenRecording: Path? = null

        /**
         * List of focus events, if collected
         */
        var eventLog = listOf<FocusEvent>()

        /**
         * Creates a new run result associated with an assertion tag
         *
         * By default assert all entries
         */
        @JvmOverloads
        fun build(assertionTag: AssertionTag = AssertionTag.ALL): FlickerRunResult {
            return FlickerRunResult(
                    assertionTag,
                    iteration,
                    wmTraceFile,
                    layersTraceFile,
                    screenRecording,
                    eventLog
            )
        }
    }
}