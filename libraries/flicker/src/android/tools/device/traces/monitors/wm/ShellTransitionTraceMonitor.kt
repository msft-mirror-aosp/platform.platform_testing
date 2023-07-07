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

package android.tools.device.traces.monitors.wm

import android.tools.common.CrossPlatform
import android.tools.common.FLICKER_TAG
import android.tools.common.io.TraceType
import android.tools.common.traces.wm.TransitionsTrace
import android.tools.device.traces.executeShellCommand
import android.tools.device.traces.monitors.TraceMonitor
import java.io.File

/** Captures [TransitionsTrace] from SurfaceFlinger. */
open class ShellTransitionTraceMonitor : TraceMonitor() {
    override val traceType = TraceType.SHELL_TRANSITION
    final override var isEnabled = false
        private set

    override fun doStart() {
        require(!isEnabled) { "Trace already running" }
        isEnabled = true
        CrossPlatform.log.d(FLICKER_TAG, "Running '$START_TRACING_COMMAND'")
        executeShellCommand(START_TRACING_COMMAND)
    }

    override fun doStop(): File {
        require(isEnabled) { "Trace not running" }
        isEnabled = false
        CrossPlatform.log.d(FLICKER_TAG, "Running '$START_TRACING_COMMAND'")
        executeShellCommand(STOP_TRACING_COMMAND)

        return TRACE_DIR.resolve(traceType.fileName)
    }

    companion object {
        private const val BASE_TRACING_COMMAND =
            "dumpsys activity service SystemUIService WMShell transitions tracing"
        const val START_TRACING_COMMAND = "$BASE_TRACING_COMMAND start"
        const val STOP_TRACING_COMMAND = "$BASE_TRACING_COMMAND stop"
    }
}
