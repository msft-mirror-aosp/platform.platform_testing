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

package com.android.server.wm.flicker.monitor

import android.util.Log
import com.android.compatibility.common.util.SystemUtil
import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.flicker.IResultSetter
import com.android.server.wm.flicker.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.helpers.SECOND_AS_NANOSECONDS
import com.android.server.wm.flicker.io.TraceType
import com.android.server.wm.flicker.now
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.events.EventLog
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

/** Collects event logs during transitions. */
open class EventLogMonitor(
    outputDir: Path = getDefaultFlickerOutputDir(),
    sourceFile: Path = getDefaultFlickerOutputDir().resolve(TraceType.EVENT_LOG.fileName)
) : TransitionMonitor(outputDir, sourceFile), IResultSetter {
    override val traceType: TraceType
        get() = TraceType.EVENT_LOG

    override var isEnabled: Boolean = false

    private var traceStartTime: Timestamp? = null

    override fun startTracing() {
        require(!isEnabled) { "Trace already running" }
        isEnabled = true
        traceStartTime = now()
    }

    override fun stopTracing() {
        require(isEnabled) { "Trace not running" }
        isEnabled = false
        val sinceTime =
            nanosToLogFormat(traceStartTime?.unixNanos ?: error("Missing start timestamp"))

        traceStartTime = null

        Files.deleteIfExists(sourceFile)
        sourceFile.toFile().parentFile.mkdirs()
        sourceFile.toFile().createNewFile()

        FileOutputStream(sourceFile.toFile()).use {
            it.write("${EventLog.MAGIC_NUMBER}\n".toByteArray())
            val command =
                "logcat -b events -v threadtime -v printable -v uid -v nsec " +
                    "-v epoch -t $sinceTime >> $sourceFile"
            Log.d(FLICKER_TAG, "Running '$command'")
            val eventLogString = SystemUtil.runShellCommandOrThrow(command)
            it.write(eventLogString.toByteArray())
        }
    }

    private fun nanosToLogFormat(timestampNanos: Long): String {
        val seconds = timestampNanos / SECOND_AS_NANOSECONDS
        val nanos = timestampNanos % SECOND_AS_NANOSECONDS
        return "$seconds.${nanos.toString().padStart(9, '0')}"
    }
}
