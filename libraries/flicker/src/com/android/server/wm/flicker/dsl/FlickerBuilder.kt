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

package com.android.server.wm.flicker.dsl

import android.app.Instrumentation
import android.support.test.launcherhelper.ILauncherStrategy
import android.support.test.launcherhelper.LauncherStrategyFactory
import androidx.test.uiautomator.UiDevice
import com.android.server.wm.flicker.Flicker
import com.android.server.wm.flicker.FlickerDslMarker
import com.android.server.wm.flicker.helpers.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.monitor.ITransitionMonitor
import com.android.server.wm.flicker.traces.layers.LayersTrace
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTrace
import com.android.server.wm.flicker.monitor.LayersTraceMonitor
import com.android.server.wm.flicker.monitor.ScreenRecorder
import com.android.server.wm.flicker.monitor.WindowAnimationFrameStatsMonitor
import com.android.server.wm.flicker.monitor.WindowManagerTraceMonitor
import java.nio.file.Path

/**
 * Build Flicker tests using Flicker DSL
 */
@FlickerDslMarker
data class FlickerBuilder(
        /**
     * Instrumentation to run the tests
     */
    private val instrumentation: Instrumentation,
        /**
     * Strategy used to interact with the launcher
     */
    private val launcherStrategy: ILauncherStrategy,
        /**
     * Include or discard janky runs
     */
    private val includeJankyRuns: Boolean = true,
        /**
     * Output directory for the test results
     */
    private val outputDir: Path = getDefaultFlickerOutputDir(instrumentation)
) {
    private var testTag: String = ""
    private var iterations: Int = 1
    private val device = UiDevice.getInstance(instrumentation)
    private val setupCommands = TestCommands()
    private val teardownCommands = TestCommands()
    private val transitionCommands = mutableListOf<Flicker.() -> Any>()
    private val assertions = AssertionTarget()

    private val traceMonitors = mutableListOf<ITransitionMonitor>()
            .also {
                it.add(WindowManagerTraceMonitor(outputDir))
                it.add(LayersTraceMonitor(outputDir))
                it.add(ScreenRecorder(outputDir))
            }
    private val frameStatsMonitor: WindowAnimationFrameStatsMonitor? = if (includeJankyRuns) {
        null
    } else {
        WindowAnimationFrameStatsMonitor(instrumentation)
    }

    /**
     * Test tag used to store the test results
     *
     * If reused throughout the test, only the last value is stored
     */
    fun withTag(testTag: () -> String) {
        val tag = testTag()
        require(!tag.contains(" ")) {
            "The test tag can not contain spaces since it is a part of the file name"
        }
        this.testTag = tag
    }

    /**
     * Configure a [WindowManagerTraceMonitor] to obtain [WindowManagerTrace]
     *
     * By default the tracing is always active. To disable tracing return null
     *
     * If this tracing is disabled, the assertions for [AssertionTarget.layerAssertions] will
     * not be executed
     */
    fun withWindowManagerTracing(traceMonitor: (Path) -> WindowManagerTraceMonitor?) {
        traceMonitors.removeIf { it is WindowManagerTraceMonitor }
        val newMonitor = traceMonitor(outputDir)

        if (newMonitor != null) {
            traceMonitors.add(newMonitor)
        }
    }

    /**
     * Configure a [LayersTraceMonitor] to obtain [LayersTrace].
     *
     * By default the tracing is always active. To disable tracing return null
     *
     * If this tracing is disabled, the assertions for [AssertionTarget.layerAssertions] will
     * not be executed
     */
    fun withLayerTracing(traceMonitor: (Path) -> LayersTraceMonitor?) {
        traceMonitors.removeIf { it is LayersTraceMonitor }
        val newMonitor = traceMonitor(outputDir)

        if (newMonitor != null) {
            traceMonitors.add(newMonitor)
        }
    }

    /**
     * Configure a [ScreenRecorder].
     *
     * By default the tracing is always active. To disable tracing return null
     */
    fun withScreenRecorder(screenRecorder: (Path) -> ScreenRecorder?) {
        traceMonitors.removeIf { it is ScreenRecorder }
        val newMonitor = screenRecorder(outputDir)

        if (newMonitor != null) {
            traceMonitors.add(newMonitor)
        }
    }

    /**
     * Defines how many times the test run should be repeated
     */
    fun repeat(predicate: () -> Int) {
        val repeat = predicate()
        require(repeat >= 1) { "Number of repetitions should be greater or equal to 1" }
        iterations = repeat
    }

    /**
     * Defines the test ([TestCommands.testCommands]) and run ([TestCommands.runCommands])
     * commands executed before the [transitions] to test
     */
    fun setup(commands: TestCommands.() -> Unit) {
        setupCommands.apply { commands() }
    }

    /**
     * Defines the test ([TestCommands.testCommands]) and run ([TestCommands.runCommands])
     * commands executed after the [transitions] to test
     */
    fun teardown(commands: TestCommands.() -> Unit) {
        teardownCommands.apply { commands() }
    }

    /**
     * Defines the commands that trigger the behavior to test
     */
    fun transitions(command: Flicker.() -> Any) {
        transitionCommands.add(command)
    }

    /**
     * Defines the assertions to check the recorded traces
     */
    fun assertions(assertion: AssertionTarget.() -> Unit) {
        assertions.apply { assertion() }
    }

    /**
     * Creates a new Flicker runner based on the current builder configuration
     */
    fun build() = Flicker(
            instrumentation,
            device,
            launcherStrategy,
            outputDir,
            testTag,
            iterations,
            frameStatsMonitor,
            traceMonitors,
            setupCommands,
            teardownCommands,
            transitionCommands,
            assertions
    )
}

/**
 * Entry point for the Flicker DSL.
 *
 * Configures a builder, build a test running, executes the test and assertions
 *
 * @param instrumentation to run the test (used to interact with the device
 * @param configuration Flicker DSL configuration
 */
@JvmOverloads
fun flicker(
    instrumentation: Instrumentation,
    launcherStrategy: ILauncherStrategy
        = LauncherStrategyFactory.getInstance(instrumentation).launcherStrategy,
    configuration: FlickerBuilder.() -> Unit
) = FlickerBuilder(instrumentation, launcherStrategy)
        .apply(configuration)
        .build()
        .execute()
        .makeAssertions()
