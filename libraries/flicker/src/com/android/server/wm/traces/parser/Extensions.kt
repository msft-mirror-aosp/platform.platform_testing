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

package com.android.server.wm.traces.parser

import android.app.UiAutomation
import android.content.ComponentName
import android.os.ParcelFileDescriptor
import android.os.Trace
import android.util.Log
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.DeviceStateDump
import com.android.server.wm.traces.common.NullableDeviceStateDump
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.layers.BaseLayerTraceEntry
import com.android.server.wm.traces.common.windowmanager.WindowManagerState

internal const val LOG_TAG = "AMWM_FLICKER"

fun Rect.toAndroidRect(): android.graphics.Rect {
    return android.graphics.Rect(left, top, right, bottom)
}

fun android.graphics.Rect.toFlickerRect(): Rect {
    return Rect.from(left, top, right, bottom)
}

private fun executeCommand(uiAutomation: UiAutomation, cmd: String): ByteArray {
    val fileDescriptor = uiAutomation.executeShellCommand(cmd)
    ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor).use { inputStream ->
        return inputStream.readBytes()
    }
}

private fun getCurrentWindowManagerState(uiAutomation: UiAutomation) =
    executeCommand(uiAutomation, "dumpsys window --proto")

private fun getCurrentLayersState(uiAutomation: UiAutomation) =
    executeCommand(uiAutomation, "dumpsys SurfaceFlinger --proto")

/**
 * Gets the current device state dump containing the [WindowManagerState] (optional) and the
 * [BaseLayerTraceEntry] (optional) in raw (byte) data.
 *
 * @param dumpFlags Flags determining which types of traces should be included in the dump
 */
@JvmOverloads
fun getCurrentState(
    uiAutomation: UiAutomation,
    @WmStateDumpFlags dumpFlags: Int = FLAG_STATE_DUMP_FLAG_WM.or(FLAG_STATE_DUMP_FLAG_LAYERS)
): Pair<ByteArray, ByteArray> {
    if (dumpFlags == 0) {
        throw IllegalArgumentException("No dump specified")
    }

    Log.d(LOG_TAG, "Requesting new device state dump")
    val wmTraceData =
        if (dumpFlags.and(FLAG_STATE_DUMP_FLAG_WM) > 0) {
            getCurrentWindowManagerState(uiAutomation)
        } else {
            ByteArray(0)
        }
    val layersTraceData =
        if (dumpFlags.and(FLAG_STATE_DUMP_FLAG_LAYERS) > 0) {
            getCurrentLayersState(uiAutomation)
        } else {
            ByteArray(0)
        }

    return Pair(wmTraceData, layersTraceData)
}

/**
 * Gets the current device state dump containing the [WindowManagerState] (optional) and the
 * [BaseLayerTraceEntry] (optional) parsed
 *
 * @param dumpFlags Flags determining which types of traces should be included in the dump
 * @param clearCacheAfterParsing If the caching used while parsing the proto should be
 * ```
 *                               cleared or remain in memory
 * ```
 */
@JvmOverloads
fun getCurrentStateDumpNullable(
    uiAutomation: UiAutomation,
    @WmStateDumpFlags dumpFlags: Int = FLAG_STATE_DUMP_FLAG_WM.or(FLAG_STATE_DUMP_FLAG_LAYERS),
    clearCacheAfterParsing: Boolean
): NullableDeviceStateDump {
    val currentStateDump = getCurrentState(uiAutomation, dumpFlags)
    return DeviceDumpParser.fromNullableDump(
        currentStateDump.first,
        currentStateDump.second,
        clearCacheAfterParsing = clearCacheAfterParsing
    )
}

fun getCurrentStateDump(
    uiAutomation: UiAutomation,
    clearCacheAfterParsing: Boolean
): DeviceStateDump {
    val currentStateDump = getCurrentState(uiAutomation)
    return DeviceDumpParser.fromDump(
        currentStateDump.first,
        currentStateDump.second,
        clearCacheAfterParsing = clearCacheAfterParsing
    )
}

fun <T> withPerfettoTrace(logMsg: String, predicate: () -> T): T {
    return try {
        Trace.beginSection(logMsg)
        predicate()
    } finally {
        Trace.endSection()
    }
}

/** Converts an Android [ComponentName] into a flicker [ComponentNameMatcher] */
fun ComponentName.toFlickerComponent(): ComponentNameMatcher =
    ComponentNameMatcher(this.packageName, this.className)
