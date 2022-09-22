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

package com.android.server.wm.flicker

import android.app.Instrumentation
import android.support.test.launcherhelper.ILauncherStrategy
import android.util.Log
import androidx.test.uiautomator.UiDevice
import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.monitor.ITransitionMonitor
import com.android.server.wm.flicker.service.FlickerServiceResultsCollector
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import java.nio.file.Path

@DslMarker
annotation class FlickerDslMarker

/**
 * Defines the runner for the flicker tests. This component is responsible for running the flicker
 * tests and executing assertions on the traces to check for inconsistent behaviors on
 * [WindowManagerTrace] and [LayersTrace]
 */
@FlickerDslMarker
class Flicker(
    /**
     * Instrumentation to run the tests
     */
    @JvmField val instrumentation: Instrumentation,
    /**
     * Test automation component used to interact with the device
     */
    @JvmField val device: UiDevice,
    /**
     * Strategy used to interact with the launcher
     */
    @JvmField val launcherStrategy: ILauncherStrategy,
    /**
     * Output directory for test results
     */
    @JvmField val outputDir: Path,
    /**
     * Test name used to store the test results
     */
    @JvmField val testName: String,
    /**
     * Enabled tracing monitors
     */
    @JvmField val traceMonitors: List<ITransitionMonitor>,
    /**
     * Commands to be executed before the transition
     */
    @JvmField val transitionSetup: List<Flicker.() -> Any>,
    /**
     * Test commands
     */
    @JvmField val transitions: List<Flicker.() -> Any>,
    /**
     * Commands to be executed after the transition
     */
    @JvmField val transitionTeardown: List<Flicker.() -> Any>,
    /**
     * Runner to execute the test transitions
     */
    @JvmField val runner: TransitionRunner,
    /**
     * Helper object for WM Synchronization
     */
    val wmHelper: WindowManagerStateHelper,
    /**
     * Whether or not to run Flicker as a Service on the collected transition traces
     */
    @JvmField val faasEnabled: Boolean = false,
    /**
     * Defines properties we allow on traces (e.g. is it valid for a transition to not have any
     * changed in the WM and Layers states)
     */
    @JvmField val traceConfigs: TraceConfigs = DEFAULT_TRACE_CONFIG
) {
    internal val faasTracesCollector = LegacyFlickerTraceCollector()
    internal val faas = FlickerServiceResultsCollector(
        outputDir,
        tracesCollector = faasTracesCollector
    )

    var result: FlickerResult? = null

    private var assertionsCheckedCallback: ((Boolean) -> Unit)? = null

    /**
     * Executes the test transition.
     *
     * @throws IllegalStateException If cannot execute the transition
     */
    fun execute(): Flicker = apply {
        this.result = runner.execute(this, useCacheIfAvailable = true)
    }

    /**
     * Run an assertion on the trace
     *
     * @param assertion Assertion to run
     * @throws AssertionError If the assertions fail or the transition crashed
     */
    fun checkAssertion(assertion: AssertionData) {
        var ranChecks = false
        try {
            if (result == null) {
                execute()
            }

            val result = result

            requireNotNull(result)

            if (!result.ranSuccessfully) {
                // No successful transition runs so can't check assertions against anything
                // Any execution errors that lead to having no successful runs will be reported
                // appropriately by the FlickerBlockJUnit4ClassRunner.
                if (result.transitionExecutionError == null) {
                    // If there are no execution errors we want to throw an error here since we won't
                    // fail later in the FlickerBlockJUnit4ClassRunner.
                    throw Exception("No transition runs were executed! Can't check assertion.")
                }

                return
            }

            val failure = result.checkAssertion(assertion)
            ranChecks = true
            if (failure != null) {
                throw failure
            }
        } finally {
            assertionsCheckedCallback?.invoke(ranChecks)
        }
    }

    /**
     * Saves the traces files assertions were run on, clears the cached runner results.
     */
    fun clear() {
        Log.v(FLICKER_TAG, "Cleaning up spec $testName")
        runner.cleanUp()
        result?.clearFromMemory()
        faasTracesCollector.stop()
        faasTracesCollector.clear()
        Log.v(FLICKER_TAG, "Cleaned up spec $testName")
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
        runner.createTag(this, tag)
    }

    fun createTag(tag: String) {
        withTag(tag) {}
    }

    override fun toString(): String {
        return this.testName
    }

    fun setAssertionsCheckedCallback(assertionsCheckedCallback: (Boolean) -> Unit) {
        this.assertionsCheckedCallback = assertionsCheckedCallback
    }
}
