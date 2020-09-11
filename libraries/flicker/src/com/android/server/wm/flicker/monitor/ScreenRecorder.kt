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
import com.android.server.wm.flicker.FlickerRunResult
import com.android.server.wm.flicker.FLICKER_TAG
import java.io.IOException
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
        return Thread(
                Runnable {
                    try {
                        Runtime.getRuntime().exec(command)
                    } catch (e: IOException) {
                        Log.e(FLICKER_TAG, "Error executing $command", e)
                    }
                })
    }

    private var recorderThread = createRecorderThread()

    override fun start() {
        outputPath.toFile().mkdirs()
        recorderThread.start()
    }

    override fun stop() {
        SystemUtil.runShellCommand("killall -s 2 screenrecord")
        try {
            recorderThread.join()
        } catch (e: InterruptedException) {
            // ignore
        }
        recorderThread = createRecorderThread()
    }

    override val isEnabled: Boolean
        get() = recorderThread.isAlive

    override fun setResult(flickerRunResultBuilder: FlickerRunResult.Builder, traceFile: Path) {
        flickerRunResultBuilder.screenRecording = traceFile
    }
}
