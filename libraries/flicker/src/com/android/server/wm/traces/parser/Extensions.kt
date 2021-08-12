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
import android.util.Log
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.Region

internal const val LOG_TAG = "AMWM_FLICKER"

fun Region.toAndroidRegion(): android.graphics.Region {
    return android.graphics.Region(left, top, right, bottom)
}

fun Rect.toAndroidRect(): android.graphics.Rect {
    return android.graphics.Rect(left, top, right, bottom)
}

/**
 * Subtracts [other] region from this [this] region
 */
fun Region.minus(other: Region): android.graphics.Region = minus(other.toAndroidRegion())

/**
 * Subtracts [other] region from this [this] region
 */
fun Region.minus(other: android.graphics.Region): android.graphics.Region {
    val thisRegion = this.toAndroidRegion()
    thisRegion.op(other, android.graphics.Region.Op.XOR)
    return thisRegion
}

/**
 * Adds [other] region to this [this] region
 */
fun Region.plus(other: Region): android.graphics.Region = plus(other.toAndroidRegion())

/**
 * Adds [other] region to this [this] region
 */
fun Region.plus(other: android.graphics.Region): android.graphics.Region {
    val thisRegion = this.toAndroidRegion()
    thisRegion.op(other, android.graphics.Region.Op.XOR)
    return thisRegion
}

/**
 * Obtains the layer name from the component name.
 *
 * See [ComponentName.toWindowName] for additional information
 */
fun ComponentName.toLayerName(): String {
    var result = this.toWindowName()
    if (result.contains("/") && !result.contains("#")) {
        result = "$result#"
    }

    return result
}

/**
 * Obtains the activity name from the component name.
 *
 * See [ComponentName.toWindowName] for additional information
 */
fun ComponentName.toActivityName(): String {
    return when {
        this.packageName.isNotEmpty() && this.className.isNotEmpty() -> this.flattenToShortString()
        this.packageName.isNotEmpty() -> this.packageName
        this.className.isNotEmpty() -> this.className
        else -> error("Component name should have an activity of class name")
    }
}

/**
 * Obtains the window name from the component name.
 *
 * [ComponentName] builds the string representation as PKG/CLASS, however this doesn't
 * work for system components such as IME, NavBar and StatusBar, Toast.
 *
 * If the component doesn't have a package name, assume it's a system component and return only
 * the class name
 */
fun ComponentName.toWindowName(): String {
    return when {
        this.packageName.isNotEmpty() && this.className.isNotEmpty() -> this.flattenToString()
        this.packageName.isNotEmpty() -> this.packageName
        this.className.isNotEmpty() -> this.className
        else -> error("Component name should have an activity of class name")
    }
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

@JvmOverloads
fun getCurrentState(
    uiAutomation: UiAutomation,
    @WmStateDumpFlags dumpFlags: Int = FLAG_STATE_DUMP_FLAG_WM.or(FLAG_STATE_DUMP_FLAG_LAYERS)
): Pair<ByteArray, ByteArray> {
    if (dumpFlags == 0) {
        throw IllegalArgumentException("No dump specified")
    }

    Log.d(LOG_TAG, "Requesting new device state dump")
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

    return Pair(wmTraceData, layersTraceData)
}

@JvmOverloads
fun getCurrentStateDump(
    uiAutomation: UiAutomation,
    @WmStateDumpFlags dumpFlags: Int = FLAG_STATE_DUMP_FLAG_WM.or(FLAG_STATE_DUMP_FLAG_LAYERS)
): DeviceStateDump {
    val currentStateDump = getCurrentState(uiAutomation, dumpFlags)
    val wmTraceData = currentStateDump.first
    val layersTraceData = currentStateDump.second
    return DeviceStateDump.fromDump(wmTraceData, layersTraceData)
}
