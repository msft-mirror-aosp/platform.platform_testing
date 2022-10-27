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

package com.android.server.wm.flicker

import android.util.Log
import com.android.server.wm.flicker.service.ITracesCollector
import com.android.server.wm.flicker.service.ITracesCollector.Companion.Traces
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import java.nio.file.Path

class LegacyFlickerTraceCollector : ITracesCollector {
    private var stopped: Boolean = true

    internal var wmTrace: WindowManagerTrace? = null
        internal set(value) {
            require(stopped || field == null) { "wmTrace already set" }
            field = value
        }

    internal var layersTrace: LayersTrace? = null
        internal set(value) {
            require(stopped || field == null) { "layersTrace already set" }
            field = value
        }

    internal var transitionsTrace: TransitionsTrace? = null
        internal set(value) {
            require(stopped || field == null) { "transitionsTrace already set" }
            field = value
        }

    internal var tracePath: Path? = null
        internal set(value) {
            require(stopped || field == null) { "tracePath already set" }
            field = value
        }

    override fun start() {
        stopped = false
        require(wmTrace == null) { "wmTrace should not already be set" }
        require(layersTrace == null) { "layersTrace should not already be set" }
        require(transitionsTrace == null) { "transitionsTrace should not already be set" }
        require(tracePath == null) { "tracePath should not already be set" }
    }

    override fun stop() {
        stopped = true
    }

    override fun getCollectedTraces(): Traces {
        Log.d("FAAS", "LegacyFlickerTraceCollector#getCollectedTraces")
        val wmTrace = wmTrace
        val layersTrace = layersTrace
        val transitionsTrace = transitionsTrace

        require(wmTrace != null) { "wmTrace not set" }
        require(layersTrace != null) { "layersTrace not set" }
        require(transitionsTrace != null) { "transitionsTrace not set" }

        clear()

        return Traces(wmTrace, layersTrace, transitionsTrace)
    }

    override fun getCollectedTracesPath(): Path {
        val tracePath = tracePath
        require(tracePath != null) { "tracePath not set" }
        return tracePath
    }

    internal fun clear() {
        wmTrace = null
        layersTrace = null
        transitionsTrace = null
    }
}
