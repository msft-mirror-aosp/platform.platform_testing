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

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.flicker.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.io.TraceType
import java.nio.file.Files
import java.nio.file.Path

/** Captures screen contents and saves it as a mp4 video file. */
open class ScreenRecorder
@JvmOverloads
constructor(
    private val context: Context,
    outputDir: Path = getDefaultFlickerOutputDir(),
    private val width: Int = 720,
    private val height: Int = 1280
) : TraceMonitor(outputDir, getDefaultFlickerOutputDir().resolve("transition.mp4")) {
    override val traceType: TraceType
        get() = TraceType.SCREEN_RECORDING

    private var recordingThread: Thread? = null
    private var recordingRunnable: ScreenRecordingRunnable? = null

    private fun newRecordingThread(): Thread {
        val runnable = ScreenRecordingRunnable(sourceFile, context, width, height)
        recordingRunnable = runnable
        return Thread(runnable)
    }

    /** Indicates if any frame has been recorded. */
    @VisibleForTesting
    val isFrameRecorded: Boolean
        get() =
            when {
                !isEnabled -> false
                else -> recordingRunnable?.isFrameRecorded ?: false
            }

    override fun startTracing() {
        if (recordingThread != null) {
            Log.i(FLICKER_TAG, "Screen recorder already running")
            return
        }
        Files.deleteIfExists(sourceFile)
        require(!Files.exists(sourceFile)) { "Could not delete old trace file" }
        Files.createDirectories(sourceFile.parent)

        val recordingThread = newRecordingThread()
        this.recordingThread = recordingThread
        Log.d(FLICKER_TAG, "Starting screen recording thread")
        recordingThread.start()

        var remainingTime = WAIT_TIMEOUT_MS
        do {
            SystemClock.sleep(WAIT_INTERVAL_MS)
            remainingTime -= WAIT_INTERVAL_MS
        } while (recordingRunnable?.isFrameRecorded != true)

        require(Files.exists(sourceFile)) { "Screen recorder didn't start" }
    }

    override fun stopTracing() {
        if (recordingThread == null) {
            Log.i(FLICKER_TAG, "Screen recorder was not started")
            return
        }

        Log.d(FLICKER_TAG, "Stopping screen recording. Storing result in $sourceFile")
        try {
            recordingRunnable?.stop()
            recordingThread?.join()
        } catch (e: Exception) {
            throw IllegalStateException("Unable to stop screen recording", e)
        } finally {
            recordingRunnable = null
            recordingThread = null
        }
    }

    override val isEnabled: Boolean
        get() = recordingThread != null

    override fun toString(): String {
        return "ScreenRecorder($sourceFile)"
    }

    companion object {
        private const val WAIT_TIMEOUT_MS = 5000L
        private const val WAIT_INTERVAL_MS = 500L
    }
}
