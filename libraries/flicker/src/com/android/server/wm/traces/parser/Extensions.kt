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
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.Region

internal const val LOG_TAG = "AMWM_FLICKER"

fun Region.toAndroidRegion(): android.graphics.Region {
    return android.graphics.Region(bounds.left, bounds.top, bounds.right, bounds.bottom)
}

fun Rect.toAndroidRect(): android.graphics.Rect {
    return android.graphics.Rect(left, top, right, bottom)
}

fun ComponentName.toActivityName(): String = this.flattenToShortString()

fun ComponentName.toWindowName(): String = this.flattenToString()

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

@JvmOverloads
fun getCurrentState(
    uiAutomation: UiAutomation,
    @WmStateDumpFlags dumpFlags: Int = FLAG_STATE_DUMP_FLAG_WM.or(FLAG_STATE_DUMP_FLAG_LAYERS)
): DeviceStateDump {
    if (dumpFlags == 0) {
        throw IllegalArgumentException("No dump specified")
    }

    val wmTraceData = if (dumpFlags.and(FLAG_STATE_DUMP_FLAG_WM) > 0) {
        getCurrentWindowManagerState(uiAutomation)
    } else {
        ByteArray(0)
    }
    val layersTraceData = if (dumpFlags.and(FLAG_STATE_DUMP_FLAG_LAYERS) > 0) {
        getCurrentLayersState(uiAutomation)
    } else {
        ByteArray(0)
    }
    return DeviceStateDump.fromDump(wmTraceData, layersTraceData)
}
