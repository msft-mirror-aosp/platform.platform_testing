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

package android.tools.device.flicker

import android.tools.common.CrossPlatform
import android.tools.common.FLICKER_TAG
import android.tools.common.Scenario
import android.tools.common.ScenarioBuilder
import android.tools.common.flicker.ITracesCollector
import android.tools.common.io.IReader
import android.tools.device.traces.DEFAULT_TRACE_CONFIG
import android.tools.device.traces.io.IResultData
import android.tools.device.traces.io.ResultReaderWithLru
import android.tools.device.traces.io.ResultWriter
import android.tools.device.traces.monitors.events.EventLogMonitor
import android.tools.device.traces.monitors.surfaceflinger.LayersTraceMonitor
import android.tools.device.traces.monitors.surfaceflinger.TransactionsTraceMonitor
import android.tools.device.traces.monitors.wm.TransitionsTraceMonitor
import android.tools.device.traces.monitors.wm.WindowManagerTraceMonitor
import java.io.File

class FlickerServiceTracesCollector(
    val outputDir: File,
    val scenario: Scenario =
        ScenarioBuilder().forClass(FlickerServiceTracesCollector::class.java.simpleName).build()
) : ITracesCollector {

    private var result: IResultData? = null

    private val traceMonitors =
        listOf(
            WindowManagerTraceMonitor(),
            LayersTraceMonitor(),
            TransitionsTraceMonitor(),
            TransactionsTraceMonitor(),
            EventLogMonitor()
        )

    override fun start() {
        reportErrorsBlock("Failed to start traces") {
            reset()
            traceMonitors.forEach { it.start() }
        }
    }

    override fun stop() {
        reportErrorsBlock("Failed to stop traces") {
            CrossPlatform.log.v(LOG_TAG, "Creating output directory for trace files")
            outputDir.mkdirs()

            CrossPlatform.log.v(LOG_TAG, "Stopping trace monitors")
            val writer = ResultWriter().forScenario(scenario).withOutputDir(outputDir)
            traceMonitors.forEach { it.stop(writer) }
            result = writer.write()
        }
    }

    override fun getResultReader(): IReader {
        return reportErrorsBlock("Failed to get collected traces") {
            val result = result
            requireNotNull(result) { "Result not set" }
            ResultReaderWithLru(result, DEFAULT_TRACE_CONFIG)
        }
    }

    private fun reset() {
        result = null
        cleanupTraceFiles()
    }

    /**
     * Remove the WM trace and layers trace files collected from previous test runs if the directory
     * exists.
     */
    private fun cleanupTraceFiles() {
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
    }

    private fun <T : Any> reportErrorsBlock(msg: String, block: () -> T): T {
        try {
            return block()
        } catch (e: Throwable) {
            CrossPlatform.log.e(LOG_TAG, msg, e)
            throw e
        }
    }

    companion object {
        private const val LOG_TAG = "$FLICKER_TAG-Collector"
    }
}
