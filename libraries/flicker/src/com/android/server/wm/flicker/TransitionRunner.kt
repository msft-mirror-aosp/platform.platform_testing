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
import android.util.Log
import com.android.server.wm.flicker.monitor.ITransitionMonitor
import com.android.server.wm.flicker.monitor.LayersTraceMonitor
import com.android.server.wm.flicker.monitor.ScreenRecorder
import com.android.server.wm.flicker.monitor.WindowAnimationFrameStatsMonitor
import com.android.server.wm.flicker.monitor.WindowManagerTraceMonitor
import java.nio.file.Path

/**
 * Builds and runs UI transitions capturing test artifacts.
 *
 *
 * User can compose a transition from simpler steps, specifying setup and teardown steps. During
 * a transition, Layers trace, WindowManager trace, screen recordings and window animation frame
 * stats can be captured.
 *
 * <pre>
 * Transition builder options:
 * [TransitionBuilder.run] run transition under test. Monitors will be started
 * before the transition and stopped after the transition is completed.
 * [TransitionBuilder.repeat] repeat transitions under test multiple times recording
 * result for each run.
 * [TransitionBuilder.withTag] specify a string identifier used to prefix logs and
 * artifacts generated.
 * [TransitionBuilder.runBeforeAll] run setup transitions once before all other
 * transition are run to set up an initial state on device.
 * [TransitionBuilder.runBefore] run setup transitions before each test transition
 * run.
 * [TransitionBuilder.runAfter] run teardown transitions after each test
 * transition.
 * [TransitionBuilder.runAfter] run teardown transitions once after all
 * other transition  are run.
 * [TransitionBuilder.includeJankyRuns] disables [WindowAnimationFrameStatsMonitor]
 * to monitor janky frames. If janky frames are detected, then the test run is skipped. This
 * monitor is enabled by default.
 * [TransitionBuilder.skipLayersTrace] disables [LayersTraceMonitor] used to
 * capture Layers trace during a transition. This monitor is enabled by default.
 * [TransitionBuilder.skipWindowManagerTrace] disables [WindowManagerTraceMonitor]
 * used to capture WindowManager trace during a transition. This monitor is enabled by
 * default.
 * [TransitionBuilder.recordAllRuns] records the screen contents and saves it to a file.
 * All the runs including setup and teardown transitions are included in the recording. This
 * monitor is used for debugging purposes.
 * [TransitionBuilder.recordEachRun] records the screen contents during test transitions
 * and saves it to a file for each run. This monitor is used for debugging purposes.
 *
 * Example transition to capture WindowManager and Layers trace when opening a test app:
 * `TransitionRunner.newBuilder()
 * .withTag("OpenTestAppFast")
 * .runBeforeAll(UiAutomationLib::wakeUp)
 * .runBeforeAll(UiAutomationLib::UnlockDevice)
 * .runBeforeAll(UiAutomationLib::openTestApp)
 * .runBefore(UiAutomationLib::closeTestApp)
 * .run(UiAutomationLib::openTestApp)
 * .runAfterAll(UiAutomationLib::closeTestApp)
 * .repeat(5)
 * .build()
 * .run();
` *
</pre> *
 */
class TransitionRunner private constructor(
    private val screenRecorder: ScreenRecorder,
    private val wmTraceMonitor: WindowManagerTraceMonitor,
    private val layersTraceMonitor: LayersTraceMonitor,
    private val frameStatsMonitor: WindowAnimationFrameStatsMonitor,
    private val allRunsMonitors: List<ITransitionMonitor>,
    private val perRunMonitors: List<ITransitionMonitor>,
    private val beforeAlls: List<() -> Any>,
    private val befores: List<() -> Any>,
    private val transitions: List<() -> Any>,
    private val afters: List<() -> Any>,
    private val afterAlls: List<() -> Any>,
    private val iterations: Int,
    val testTag: String
) {
    private val _results = mutableListOf<TransitionResult>()

    /**
     * Returns a list of transition results.
     *
     * @return list of transition results.
     */
    val results: List<TransitionResult>
        get() {
            check(_results.isNotEmpty()) { "Results do not exist!" }
            return _results
        }

    /**
     * Runs the composed transition and calls monitors at the appropriate stages. If jank monitor is
     * enabled, transitions with jank are skipped.
     *
     * @return itself
     */
    fun run(): TransitionRunner {
        allRunsMonitors.forEach { it.start() }
        beforeAlls.forEach { it.invoke() }
        for (iteration in 0 until iterations) {
            befores.forEach { it.invoke() }
            perRunMonitors.forEach { it.start() }
            transitions.forEach { it.invoke() }
            perRunMonitors.forEach { it.stop() }
            afters.forEach { it.invoke() }
            if (runJankFree() && frameStatsMonitor.jankyFramesDetected()) {
                Log.e(FLICKER_TAG, "Skipping iteration ${iteration}/${iterations - 1} " +
                        "for test $testTag due to jank. $frameStatsMonitor")
                continue
            }
            _results.add(saveResult(iteration))
        }
        afterAlls.forEach { it.invoke() }
        allRunsMonitors.forEach {
                    it.stop()
                    it.save(testTag)
                }
        return this
    }

    /**
     * Deletes all transition results that are not marked for saving.
     *
     * @return list of transition results.
     */
    fun deleteResults() {
        if (_results.isEmpty()) {
            return
        }
        _results.filter { it.canDelete() }.forEach { it.delete() }
        _results.clear()
    }

    /**
     * Saves monitor results to file.
     *
     * @return object containing paths to test artifacts
     */
    private fun saveResult(iteration: Int): TransitionResult {
        var windowTrace: Path? = null
        var windowTraceChecksum = ""
        var layerTrace: Path? = null
        var layerTraceChecksum = ""
        var screenCaptureVideo: Path? = null
        var screenCaptureVideoChecksum = ""

        if (perRunMonitors.contains(wmTraceMonitor)) {
            windowTrace = wmTraceMonitor.save(testTag, iteration)
            windowTraceChecksum = wmTraceMonitor.checksum
        }
        if (perRunMonitors.contains(layersTraceMonitor)) {
            layerTrace = layersTraceMonitor.save(testTag, iteration)
            layerTraceChecksum = layersTraceMonitor.checksum
        }
        if (perRunMonitors.contains(screenRecorder)) {
            screenCaptureVideo = screenRecorder.save(testTag, iteration)
            screenCaptureVideoChecksum = screenRecorder.checksum
        }
        return TransitionResult(
                layerTrace,
                layerTraceChecksum,
                windowTrace,
                windowTraceChecksum,
                screenCaptureVideo,
                screenCaptureVideoChecksum)
    }

    private fun runJankFree(): Boolean {
        return perRunMonitors.contains(frameStatsMonitor)
    }

    /** Builds a [TransitionRunner] instance.  */
    data class TransitionBuilder @JvmOverloads constructor(
        private val instrumentation: Instrumentation,
        private val outputDir: Path = instrumentation
            .targetContext
            .getExternalFilesDir(null)?.toPath()
            ?: throw IllegalArgumentException("Output dir cannot be null")
    ) {
        private var runJankFree = true
        private var captureWindowManagerTrace = true
        private var captureLayersTrace = true
        private var recordEachRun = false
        private var iterations = 1
        private var testTag = ""
        private var recordAllRuns = false

        private val allRunsMonitors= mutableListOf<ITransitionMonitor>()
        private val perRunMonitors= mutableListOf<ITransitionMonitor>()
        private val beforeAlls= mutableListOf<() -> Any>()
        private val befores= mutableListOf<() -> Any>()
        private val transitions= mutableListOf<() -> Any>()
        private val afters= mutableListOf<() -> Any>()
        private val afterAlls= mutableListOf<() -> Any>()

        fun build(): TransitionRunner {
            val wmTraceMonitor = WindowManagerTraceMonitor(outputDir)
            val layersTraceMonitor = LayersTraceMonitor(outputDir)
            val frameStatsMonitor = WindowAnimationFrameStatsMonitor(instrumentation)
            val screenRecorder = ScreenRecorder(outputDir)
            if (captureWindowManagerTrace) {
                perRunMonitors.add(wmTraceMonitor)
            }
            if (captureLayersTrace) {
                perRunMonitors.add(layersTraceMonitor)
            }
            if (runJankFree) {
                perRunMonitors.add(frameStatsMonitor)
            }
            if (recordAllRuns) {
                allRunsMonitors.add(screenRecorder)
            }
            if (recordEachRun) {
                perRunMonitors.add(screenRecorder)
            }
            return TransitionRunner(
                    screenRecorder,
                    wmTraceMonitor,
                    layersTraceMonitor,
                    frameStatsMonitor,
                    allRunsMonitors,
                    perRunMonitors,
                    beforeAlls,
                    befores,
                    transitions,
                    afters,
                    afterAlls,
                    iterations,
                    testTag
            )
        }

        /**
         * Execute [runnable] before activating the winscope tracing.
         *
         * Repeats this execution for each iteration.
         */
        fun runBeforeAll(runnable: Runnable) = apply{ runBeforeAll { runnable.run() } }

        /**
         * Execute [command] before activating the winscope tracing.
         *
         * Repeats this execution for each iteration.
         */
        fun runBeforeAll(command: () -> Any) = apply { beforeAlls.add(command) }

        /**
         * Execute [runnable] before activating the winscope tracing.
         *
         * Repeats this execution once, irrespectively of the number of iterations
         */
        fun runBefore(runnable: Runnable) = apply { runBefore { runnable.run() } }

        /**
         * Execute [command] before activating the winscope tracing.
         *
         * Repeats this execution once, irrespectively of the number of iterations
         */
        fun runBefore(command: () -> Any) = apply { befores.add(command) }

        /**
         * Execute [runnable] while tracing is active.
         */
        fun run(runnable: Runnable) = apply { run { runnable.run() } }

        /**
         * Execute [command] while tracing is active.
         */
        fun run(command: () -> Any) = apply { transitions.add(command) }

        /**
         * Execute [runnable] after deactivating the winscope tracing.
         *
         * Repeats this execution once, irrespectively of the number of iterations
         */
        fun runAfter(runnable: Runnable) = apply {runAfter { runnable.run() } }

        /**
         * Execute [command] after deactivating the winscope tracing.
         *
         * Repeats this execution once, irrespectively of the number of iterations
         */
        fun runAfter(command: () -> Any) = apply { afters.add(command) }

        /**
         * Execute [runnable] after deactivating the winscope tracing.
         *
         * Repeats this execution for each iteration.
         */
        fun runAfterAll(runnable: Runnable) = apply { runAfterAll { runnable } }

        /**
         * Execute [command] after deactivating the winscope tracing.
         *
         * Repeats this execution for each iteration.
         */
        fun runAfterAll(command: () -> Any) = apply { afterAlls.add(command) }

        /**
         * Run the test [iterations] times
         */
        fun repeat(iterations: Int) = apply { this.iterations = iterations }

        fun skipWindowManagerTrace() = apply { captureWindowManagerTrace = false }

        fun skipLayersTrace() = apply { captureLayersTrace = false }

        fun includeJankyRuns() = apply { runJankFree = false }

        /**
         * Record a video of each run
         */
        fun recordEachRun() = apply {
            require(!recordAllRuns) { "Invalid option with recordAllRuns" }
            recordEachRun = true
        }

        /**
         * Record a single video for all runs
         */
        fun recordAllRuns() = apply {
            require(!recordEachRun) { "Invalid option with recordEachRun" }
            recordAllRuns = true
        }

        fun withTag(testTag: String) = apply {
            require(!testTag.contains(" ")) {
                "The test tag can not contain spaces since it is a part of the file name"
            }
            this.testTag = testTag
        }
    }
}
