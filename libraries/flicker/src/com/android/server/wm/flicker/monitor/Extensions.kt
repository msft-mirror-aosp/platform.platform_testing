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

@file:JvmName("Extensions")
package com.android.server.wm.flicker.monitor

import android.app.Instrumentation
import com.android.server.wm.flicker.DeviceStateDump
import com.android.server.wm.flicker.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.traces.layers.LayersTrace
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTrace

/**
 * Acquire the [WindowManagerTrace] with the device state changes that happen when executing
 * the commands defined in the [predicate].
 *
 * @param instrumentation Instrumentation used to determine the test's temporary folder
 * @param predicate Commands to execute
 * @throws UnsupportedOperationException If tracing is already activated
 */
fun withWMTracing(instrumentation: Instrumentation, predicate: () -> Unit): WindowManagerTrace {
    val outputDir = getDefaultFlickerOutputDir(instrumentation).resolve("withWMTracing")
    return WindowManagerTrace.parseFrom(WindowManagerTraceMonitor(outputDir).withTracing(predicate))
}

/**
 * Acquire the [LayersTrace] with the device state changes that happen when executing
 * the commands defined in the [predicate].
 *
 * @param instrumentation Instrumentation used to determine the test's temporary folder
 * @param traceFlags Flags to indicate tracing level
 * @param predicate Commands to execute
 * @throws UnsupportedOperationException If tracing is already activated
 */
fun withSFTracing(
    instrumentation: Instrumentation,
    traceFlags: Int = LayersTraceMonitor.TRACE_FLAGS,
    predicate: () -> Unit
): LayersTrace {
    val outputDir = getDefaultFlickerOutputDir(instrumentation).resolve("withSFTracing")
    return LayersTrace.parseFrom(LayersTraceMonitor(outputDir, traceFlags).withTracing(predicate))
}

/**
 * Acquire the [WindowManagerTrace] and [LayersTrace] with the device state changes that happen
 * when executing the commands defined in the [predicate].
 *
 * @param instrumentation Instrumentation used to determine the test's temporary folder
 * @param predicate Commands to execute
 * @throws UnsupportedOperationException If tracing is already activated
 */
fun withTracing(instrumentation: Instrumentation, predicate: () -> Unit): DeviceStateDump {
    var wmTraceData = ByteArray(0)
    val layersOutputDir = getDefaultFlickerOutputDir(instrumentation).resolve("withSFTracing")
    val layersTraceData = LayersTraceMonitor(layersOutputDir).withTracing {
        val wmOutputDir = getDefaultFlickerOutputDir(instrumentation).resolve("withWMTracing")
        wmTraceData = WindowManagerTraceMonitor(wmOutputDir).withTracing(predicate)
    }

    return DeviceStateDump.fromTrace(wmTraceData, layersTraceData)
}