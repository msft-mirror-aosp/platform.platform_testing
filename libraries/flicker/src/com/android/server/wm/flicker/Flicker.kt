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

package com.android.server.wm.flicker

import android.app.Instrumentation
import android.support.test.launcherhelper.ILauncherStrategy
import android.util.Log
import androidx.test.uiautomator.UiDevice
import com.android.server.wm.flicker.dsl.AssertionTarget
import com.android.server.wm.flicker.dsl.TestCommands
import com.android.server.wm.flicker.helpers.FLICKER_TAG
import com.android.server.wm.flicker.monitor.ITransitionMonitor
import com.android.server.wm.flicker.monitor.WindowAnimationFrameStatsMonitor
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WmTraceSubject
import java.nio.file.Path

@DslMarker
annotation class FlickerDslMarker

/**
 * Defines the runner for the flicker tests. This component is responsible for running the flicker
 * tests and executing assertions on the traces to check for inconsistent behaviors on
 * [WindowManagerTrace] and [LayersTrace]
 */
@FlickerDslMarker
data class Flicker(
    /**
     * Instrumentation to run the tests
     */
    val instrumentation: Instrumentation,
    /**
     * Test automation component used to interact with the device
     */
    val device: UiDevice,
    /**
     * Strategy used to interact with the launcher
     */
    val launcherStrategy: ILauncherStrategy,
    /**
     * Output directory for test results
     */
    val outputDir: Path,
    /**
     * Test tag used to store the test results
     */
    private val testTag: String,
    /**
     * Number of times the test should be executed
     */
    private var repetitions: Int,
    /**
     * Monitor for janky frames, when filtering out janky runs
     */
    private val frameStatsMonitor: WindowAnimationFrameStatsMonitor?,
    /**
     * Enabled tracing monitors
     */
    private val traceMonitors: List<ITransitionMonitor>,
    /**
     * Commands to be executed before the test
     */
    private val setup: TestCommands,
    /**
     * Commands to be executed after the test
     */
    private val teardown: TestCommands,
    /**
     * Test commands
     */
    private val transitions: List<Flicker.() -> Any>,
    /**
     * Custom set of assertions
     */
    private val assertions: AssertionTarget
) {
    private val results = mutableListOf<FlickerRunResult>()

    /**
     * Executes the test. The commands are executed in the following order:
     * 1) [setup] ([TestCommands.testCommands])
     * 2) [setup] ([TestCommands.runCommands])
     * 3) Start monitors
     * 4) [transitions]
     * 5) Stop monitors
     * 6) [teardown] ([TestCommands.runCommands])
     * 7) [teardown] ([TestCommands.testCommands])
     */
    fun execute() = apply {
        try {
            try {
                setup.testCommands.forEach { it.invoke(this) }
                for (iteration in 0 until repetitions) {
                    try {
                        setup.runCommands.forEach { it.invoke(this) }
                        traceMonitors.forEach { it.start() }
                        frameStatsMonitor?.run { start() }
                        transitions.forEach { it.invoke(this) }
                    } finally {
                        traceMonitors.forEach { it.tryStop() }
                        frameStatsMonitor?.run { tryStop() }
                        teardown.runCommands.forEach { it.invoke(this) }
                    }
                    if (frameStatsMonitor?.jankyFramesDetected() == true) {
                        Log.e(FLICKER_TAG, "Skipping iteration ${iteration}/${repetitions - 1} " +
                                "for test $testTag due to jank. $frameStatsMonitor")
                        continue
                    }
                    saveResult(iteration)
                }
            } finally {
                teardown.testCommands.forEach { it.invoke(this) }
            }
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }

    /**
     * Run the assertions on the trace
     */
    fun makeAssertions() {
        val failures = StringBuilder()
        results.forEach { iteration ->
            val wmTrace = iteration.wmTrace
            if (wmTrace != null) {
                assertions.wmAssertions
                        .filter { it.enabled }
                        .forEach { assertion ->
                    try {
                        assertion.assertion(WmTraceSubject.assertThat(wmTrace))
                    } catch(e: AssertionError) {
                        failures.append("\nTest failed: ${assertion.name}")
                                .append("\nIteration: ${iteration.iteration}")
                                .append("\nTrace: ${iteration.wmTraceFile}")
                                .append("\n")
                                .append(e.message)
                                .append("\n")
                    }
                }
            }

            val layersTrace = iteration.layersTrace
            if (layersTrace != null) {
                assertions.layerAssertions
                        .filter { it.enabled }
                        .forEach { assertion ->
                    try {
                        assertion.assertion(LayersTraceSubject.assertThat(layersTrace))
                    } catch(e: AssertionError) {
                        failures.append("\nTest failed: ${assertion.name}")
                                .append("\nIteration: ${iteration.iteration}")
                                .append("\nTrace: ${iteration.layersTraceFile}")
                                .append("\n")
                                .append(e.message)
                                .append("\n")
                    }
                }
            }
        }

        assert(failures.isEmpty()) { failures.toString() }
    }

    private fun saveResult(iteration: Int) {
        val resultBuilder = FlickerRunResult.Builder(iteration)
        traceMonitors.forEach { it.save(testTag, iteration, resultBuilder) }
        results.add(resultBuilder.build())
    }

    private fun ITransitionMonitor.tryStop() {
        this.run {
            try {
                stop()
            } catch (e: Exception) {
                Log.e(FLICKER_TAG, "Unable to stop $this")
            }
        }
    }
}
