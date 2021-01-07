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

import android.os.SystemClock
import android.util.Log
import com.android.compatibility.common.util.SystemUtil
import com.android.server.wm.flicker.FlickerRunResult
import com.android.server.wm.flicker.FLICKER_TAG
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/** Captures screen contents and saves it as a mp4 video file.  */
open class ScreenRecorder @JvmOverloads constructor(
    outputPath: Path,
    private val width: Int = 720,
    private val height: Int = 1280,
    traceFile: String = "transition.mp4"
) : TraceMonitor(outputPath, outputPath.resolve(traceFile)) {
    private fun createRecorderThread(): Thread {
        val command = "screenrecord --size ${width}x$height $sourceTraceFilePath"
        return Thread {
            try {
                Log.i(FLICKER_TAG, "Running command $command")
                Runtime.getRuntime().exec(command)
            } catch (e: IOException) {
                Log.e(FLICKER_TAG, "Error executing $command", e)
            }
        }
    }

    private var recorderThread: Thread? = null

    override fun start() {
        if (recorderThread != null) {
            Log.i(FLICKER_TAG, "Screen recorder already running")
            return
        }
        Files.deleteIfExists(sourceTraceFilePath)
        Files.createDirectories(sourceTraceFilePath.parent)
        recorderThread = createRecorderThread()
        recorderThread?.start()
        var remainingTime = WAIT_TIMEOUT_MS
        do {
            SystemClock.sleep(WAIT_INTERVAL_MS)
            remainingTime -= WAIT_INTERVAL_MS
        } while (!Files.exists(sourceTraceFilePath) && remainingTime > 0)

        require(Files.exists(sourceTraceFilePath)) {
            "Screen recorder didn't start in $WAIT_TIMEOUT_MS ms" }
    }

    override fun stop() {
        if (recorderThread == null) {
            Log.i(FLICKER_TAG, "Screen recorder was not started")
            return
        }

        SystemUtil.runShellCommand("killall -s 2 screenrecord")
        try {
            recorderThread?.join()
        } catch (e: InterruptedException) {
            Log.e(FLICKER_TAG, "Failed to stop screen recording", e)
        }
        recorderThread = null
    }

    override val isEnabled: Boolean
        get() = recorderThread?.isAlive ?: false

    override fun setResult(flickerRunResultBuilder: FlickerRunResult.Builder, traceFile: Path) {
        flickerRunResultBuilder.screenRecording = traceFile
    }

    companion object {
        private const val WAIT_TIMEOUT_MS = 5000L
        private const val WAIT_INTERVAL_MS = 500L
    }
}
