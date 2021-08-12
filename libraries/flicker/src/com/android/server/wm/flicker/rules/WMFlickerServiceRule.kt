/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker.rules

import com.android.server.wm.flicker.monitor.LayersTraceMonitor
import com.android.server.wm.flicker.monitor.TraceMonitor
import com.android.server.wm.flicker.monitor.TransitionMonitor.Companion.WINSCOPE_EXT
import com.android.server.wm.flicker.monitor.WindowManagerTraceMonitor
import com.android.server.wm.flicker.service.FlickerService
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.layers.LayersTraceParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Collect the WM and SF traces, parse them and call the WM Flicker Service after the test
 */
open class WMFlickerServiceRule : TestWatcher() {
    private val traceMonitors = mutableListOf<TraceMonitor>()
    protected var wmTrace: WindowManagerTrace? = null
    protected var layersTrace: LayersTrace? = null

    override fun starting(description: Description?) {
        setupMonitors()
        cleanupTraceFiles()
        traceMonitors.forEach {
            it.start()
        }
    }

    override fun finished(description: Description?) {
        traceMonitors.forEach {
            it.stop()
            it.save(testTag)
        }

        wmTrace = getWindowManagerTrace(getFilePath("wm_trace"))
        layersTrace = getLayersTrace(getFilePath("layers_trace"))

        val flickerService = FlickerService()
        if (wmTrace != null && layersTrace != null) {
            flickerService.process(wmTrace!!, layersTrace!!)
        }
    }

    private fun setupMonitors() {
        traceMonitors.add(WindowManagerTraceMonitor(outputDir))
        traceMonitors.add(LayersTraceMonitor(outputDir))
    }

    /**
     * Remove the WM trace and layers trace files collected from previous test runs.
     */
    private fun cleanupTraceFiles() {
        Files.deleteIfExists(getFilePath("wm_trace"))
        Files.deleteIfExists(getFilePath("layers_trace"))
    }

    private fun getFilePath(file: String) = outputDir.resolve("${testTag}_$file$WINSCOPE_EXT")

    /**
     * Parse the window manager trace file.
     *
     * @param traceFilePath
     * @return parsed window manager trace.
     */
    private fun getWindowManagerTrace(traceFilePath: Path): WindowManagerTrace {
        val wmTraceByteArray: ByteArray = Files.readAllBytes(traceFilePath)
        return WindowManagerTraceParser.parseFromTrace(wmTraceByteArray)
    }

    /**
     * Parse the layers trace file.
     *
     * @param traceFilePath
     * @return parsed layers trace.
     */
    private fun getLayersTrace(traceFilePath: Path): LayersTrace {
        val layersTraceByteArray: ByteArray = Files.readAllBytes(traceFilePath)
        return LayersTraceParser.parseFromTrace(layersTraceByteArray)
    }

    companion object {
        @JvmStatic
        private val outputDir = Paths.get("/sdcard/fass")
        private val testTag = "fass"
    }
}