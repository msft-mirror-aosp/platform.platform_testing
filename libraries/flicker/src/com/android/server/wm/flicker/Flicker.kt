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
import androidx.annotation.VisibleForTesting
import androidx.test.uiautomator.UiDevice
import com.android.server.wm.flicker.assertions.FlickerAssertionError
import com.android.server.wm.flicker.dsl.AssertionTag
import com.android.server.wm.flicker.dsl.AssertionTarget
import com.android.server.wm.flicker.dsl.TestCommands
import com.android.server.wm.flicker.monitor.ITransitionMonitor
import com.android.server.wm.flicker.monitor.WindowAnimationFrameStatsMonitor
import com.android.server.wm.traces.parser.getCurrentState
import com.google.common.truth.Truth
import java.io.IOException
import java.nio.file.Files
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
     * Test name used to store the test results
     */
    private val testName: String,
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
    private val tags = AssertionTag.DEFAULT.map { it.tag }.toMutableSet()
    @VisibleForTesting
    var error: Throwable? = null
        private set

    /**
     * Iteration identifier during test run
     */
    private var iteration = 0

    /**
     * Executes the test.
     *
     * The commands are executed in the following order:
     * 1) [setup] ([TestCommands.testCommands])
     * 2) [setup] ([TestCommands.runCommands])
     * 3) Start monitors
     * 4) [transitions]
     * 5) Stop monitors
     * 6) [teardown] ([TestCommands.runCommands])
     * 7) [teardown] ([TestCommands.testCommands])
     *
     * If the tests were already executed, reuse the previous results
     *
     * @throws IllegalArgumentException If the transitions
     */
    fun execute() = apply {
        require(transitions.isNotEmpty()) { "A flicker test must include transitions to run" }
        if (results.isNotEmpty()) {
            Log.w(FLICKER_TAG, "Flicker test already executed. Reusing results.")
            return this
        }
        try {
            try {
                error = null
                setup.testCommands.forEach { it.invoke(this) }
                for (iteration in 0 until repetitions) {
                    this.iteration = iteration
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
                        Log.e(FLICKER_TAG, "Skipping iteration $iteration/${repetitions - 1} " +
                                "for test $testName due to jank. $frameStatsMonitor")
                        continue
                    }
                    saveResult(this.iteration)
                }
            } finally {
                teardown.testCommands.forEach { it.invoke(this) }
            }
        } catch (e: Throwable) {
            error = e
            throw RuntimeException(e)
        }
    }

    private fun cleanUp(failures: List<FlickerAssertionError>) {
        results.forEach {
            if (it.canDelete(failures)) {
                it.cleanUp()
            }
        }
    }

    @Deprecated("Prefer checkAssertions", replaceWith = ReplaceWith("checkAssertions"))
    fun makeAssertions() = checkAssertions(includeFlakyAssertions = false)

    /**
     * Run the assertions on the trace
     *
     * @param includeFlakyAssertions If true, checks the flaky assertion
     * @throws AssertionError If the assertions fail or the transition crashed
     */
    @JvmOverloads
    fun checkAssertions(includeFlakyAssertions: Boolean = false) {
        Truth.assertWithMessage(error?.message).that(error).isNull()
        Truth.assertWithMessage("Transition was not executed").that(results).isNotEmpty()
        val failures = results.flatMap { assertions.checkAssertions(it, includeFlakyAssertions) }
        this.cleanUp(failures)
        val failureMessage = failures.joinToString("\n") { it.message }
        Truth.assertWithMessage(failureMessage).that(failureMessage.isEmpty()).isTrue()
    }

    private fun getTaggedFilePath(tag: String, file: String) =
            "${this.testName}_${this.iteration}_${tag}_$file"

    /**
     * Captures a snapshot of the device state and associates it with a new tag.
     *
     * This tag can be used to make assertions about the state of the device when the
     * snapshot is collected.
     *
     * [tag] is used as part of the trace file name, thus, only valid letters and digits
     * can be used
     *
     * @throws IllegalArgumentException If [tag] contains invalid characters
     */
    fun createTag(tag: String) {
        if (tag in tags) {
            throw IllegalArgumentException("Tag $tag has already been used")
        }
        tags.add(tag)
        val assertionTag = AssertionTag(tag)

        val deviceState = getCurrentState(instrumentation.uiAutomation)
        try {
            val wmTraceFile = outputDir.resolve(getTaggedFilePath(tag, "wm_trace"))
            Files.write(wmTraceFile, deviceState.wmTraceData)

            val layersTraceFile = outputDir.resolve(getTaggedFilePath(tag, "layers_trace"))
            Files.write(layersTraceFile, deviceState.layersTraceData)

            val result = FlickerRunResult(
                    assertionTag,
                    iteration = this.iteration,
                    wmTraceFile = wmTraceFile,
                    layersTraceFile = layersTraceFile,
                    wmTrace = deviceState.wmTrace,
                    layersTrace = deviceState.layersTrace
            )
            results.add(result)
        } catch (e: IOException) {
            throw RuntimeException("Unable to create trace file: ${e.message}", e)
        }
    }

    /**
     * Runs a set of commands and, at the end, creates a tag containing the device state
     *
     * @param tag Identifier for the tag to be created
     * @param commands Commands to execute before creating the tag
     * @throws IllegalArgumentException If [tag] cannot be converted to a valid filename
     */
    fun withTag(tag: String, commands: Flicker.() -> Any) {
        commands()
        createTag(tag)
    }

    private fun saveResult(iteration: Int) {
        val resultBuilder = FlickerRunResult.Builder()
        traceMonitors.forEach { it.save(testName, iteration, resultBuilder) }

        AssertionTag.DEFAULT.forEach { location ->
            results.add(resultBuilder.build(location))
        }
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

    override fun toString(): String {
        return this.testName
    }
}