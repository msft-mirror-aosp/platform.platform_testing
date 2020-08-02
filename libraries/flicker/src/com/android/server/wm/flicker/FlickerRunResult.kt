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
import com.android.server.wm.flicker.helpers.FLICKER_TAG
import com.android.server.wm.flicker.traces.FocusEvent
import com.android.server.wm.flicker.traces.layers.LayersTrace
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTrace
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Defines the result of a flicker run
 */
data class FlickerRunResult (
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
    val eventLog: List<FocusEvent>
) {
    var failed = false

    /**
     * Obtain the [WindowManagerTrace] that corresponds to [wmTraceFile], or null if the
     * path is invalid
     */
    val wmTrace: WindowManagerTrace? by lazy {
        wmTraceFile?.let {
            val traceData = Files.readAllBytes(it)
            WindowManagerTrace.parseFrom(traceData)
        }
    }

    /**
     * Obtain the [LayersTrace] that corresponds to [layersTrace], or null if the
     * path is invalid
     */
    val layersTrace: LayersTrace? by lazy {
        layersTraceFile?.let {
            val traceData = Files.readAllBytes(it)
            LayersTrace.parseFrom(traceData)
        }
    }

    private fun Path?.tryDelete() {
        try {
            this?.let { Files.deleteIfExists(it) }
        } catch (e: IOException) {
            Log.e(FLICKER_TAG, "Unable do delete $this", e)
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

    class Builder(private val iteration: Int) {
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

        fun build() = FlickerRunResult(iteration, wmTraceFile, layersTraceFile, screenRecording, eventLog)
    }
}