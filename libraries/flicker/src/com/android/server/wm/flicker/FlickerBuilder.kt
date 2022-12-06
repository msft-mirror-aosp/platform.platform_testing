/*
 * Copyright (C) 2022 The Android Open Source Project
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
import androidx.annotation.VisibleForTesting
import androidx.test.uiautomator.UiDevice
import com.android.server.wm.flicker.helpers.isShellTransitionsEnabled
import com.android.server.wm.flicker.io.TraceType
import com.android.server.wm.flicker.monitor.EventLogMonitor
import com.android.server.wm.flicker.monitor.LayersTraceMonitor
import com.android.server.wm.flicker.monitor.NoTraceMonitor
import com.android.server.wm.flicker.monitor.ScreenRecorder
import com.android.server.wm.flicker.monitor.TransactionsTraceMonitor
import com.android.server.wm.flicker.monitor.TransitionsTraceMonitor
import com.android.server.wm.flicker.monitor.WindowManagerTraceMonitor
import com.android.server.wm.traces.common.layers.BaseLayerTraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import java.io.File
import java.nio.file.Path

/** Build Flicker tests using Flicker DSL */
@FlickerDslMarker
class FlickerBuilder
private constructor(
    internal val instrumentation: Instrumentation,
    private val outputDir: Path,
    private val wmHelper: WindowManagerStateHelper,
    private val setupCommands: MutableList<IFlickerTestData.() -> Any>,
    private val transitionCommands: MutableList<IFlickerTestData.() -> Any>,
    private val teardownCommands: MutableList<IFlickerTestData.() -> Any>,
    val device: UiDevice,
    private val traceMonitors: MutableList<ITransitionMonitor>,
    private var faasEnabled: Boolean = false,
    private var traceConfigs: TraceConfigs = DEFAULT_TRACE_CONFIG
) {
    private var usingExistingTraces = false

    /** Default flicker builder constructor */
    @JvmOverloads
    constructor(
        /** Instrumentation to run the tests */
        instrumentation: Instrumentation,
        /** Output directory for the test results */
        outputDir: Path = getDefaultFlickerOutputDir(),
        /** Helper object for WM Synchronization */
        wmHelper: WindowManagerStateHelper =
            WindowManagerStateHelper(instrumentation, clearCacheAfterParsing = false),
        traceMonitors: MutableList<ITransitionMonitor> =
            mutableListOf<ITransitionMonitor>().also {
                it.add(WindowManagerTraceMonitor(outputDir))
                it.add(LayersTraceMonitor(outputDir))
                if (isShellTransitionsEnabled) {
                    // Transition tracing only works if shell transitions are enabled.
                    it.add(TransitionsTraceMonitor(outputDir))
                }
                it.add(TransactionsTraceMonitor(outputDir))
                it.add(ScreenRecorder(instrumentation.targetContext, outputDir))
                it.add(EventLogMonitor())
            }
    ) : this(
        instrumentation,
        outputDir,
        wmHelper,
        setupCommands = mutableListOf(),
        transitionCommands = mutableListOf(),
        teardownCommands = mutableListOf(),
        device = UiDevice.getInstance(instrumentation),
        traceMonitors = traceMonitors
    )

    /**
     * Configure a [WindowManagerTraceMonitor] to obtain [WindowManagerTrace]
     *
     * By default, the tracing is always active. To disable tracing return null
     *
     * If this tracing is disabled, the assertions for [WindowManagerTrace] and [WindowManagerState]
     * will not be executed
     */
    fun withWindowManagerTracing(
        traceMonitor: (Path) -> WindowManagerTraceMonitor?
    ): FlickerBuilder = apply {
        traceMonitors.removeIf { it is WindowManagerTraceMonitor }
        addMonitor(traceMonitor(outputDir))
    }

    /** Disable [LayersTraceMonitor]. */
    fun withoutLayerTracing(): FlickerBuilder = apply { withLayerTracing { null } }

    /**
     * Configure a [LayersTraceMonitor] to obtain [LayersTrace].
     *
     * By default the tracing is always active. To disable tracing return null
     *
     * If this tracing is disabled, the assertions for [LayersTrace] and [BaseLayerTraceEntry] will
     * not be executed
     */
    fun withLayerTracing(traceMonitor: (Path) -> LayersTraceMonitor?): FlickerBuilder = apply {
        traceMonitors.removeIf { it is LayersTraceMonitor }
        addMonitor(traceMonitor(outputDir))
    }

    /** Disable [TransitionsTraceMonitor]. */
    fun withoutTransitionTracing(): FlickerBuilder = apply { withTransitionTracing { null } }

    /**
     * Configure a [TransitionsTraceMonitor] to obtain [TransitionsTrace].
     *
     * By default, shell transition tracing is disabled.
     */
    fun withTransitionTracing(traceMonitor: (Path) -> TransitionsTraceMonitor?): FlickerBuilder =
        apply {
            traceMonitors.removeIf { it is TransitionsTraceMonitor }
            addMonitor(traceMonitor(outputDir))
        }

    /** Disable [TransactionsTraceMonitor]. */
    fun withoutTransactionsTracing(): FlickerBuilder = apply { withTransactionsTracing { null } }

    /**
     * Configure a [TransactionsTraceMonitor] to obtain [TransactionsTrace].
     *
     * By default, shell transition tracing is disabled.
     */
    fun withTransactionsTracing(traceMonitor: (Path) -> TransactionsTraceMonitor?): FlickerBuilder =
        apply {
            traceMonitors.removeIf { it is TransactionsTraceMonitor }
            addMonitor(traceMonitor(outputDir))
        }

    /**
     * Configure a [ScreenRecorder].
     *
     * By default, the tracing is always active. To disable tracing return null
     */
    fun withScreenRecorder(screenRecorder: (Path) -> ScreenRecorder?): FlickerBuilder = apply {
        traceMonitors.removeIf { it is ScreenRecorder }
        addMonitor(screenRecorder(outputDir))
    }

    fun withoutScreenRecorder(): FlickerBuilder = apply {
        traceMonitors.removeIf { it is ScreenRecorder }
    }

    fun withFlickerAsAService(predicate: () -> Boolean): FlickerBuilder = apply {
        faasEnabled = predicate()
    }

    /** Defines the setup commands executed before the [transitions] to test */
    fun setup(commands: IFlickerTestData.() -> Unit): FlickerBuilder = apply {
        setupCommands.add(commands)
    }

    /** Defines the teardown commands executed after the [transitions] to test */
    fun teardown(commands: IFlickerTestData.() -> Unit): FlickerBuilder = apply {
        teardownCommands.add(commands)
    }

    /** Defines the commands that trigger the behavior to test */
    fun transitions(command: IFlickerTestData.() -> Unit): FlickerBuilder = apply {
        require(!usingExistingTraces) {
            "Can't update transition after calling usingExistingTraces"
        }
        transitionCommands.add(command)
    }

    data class TraceFiles(
        val wmTrace: File,
        val layersTrace: File,
        val transactions: File,
        val transitions: File
    )

    /** Use pre-executed results instead of running transitions to get the traces */
    fun usingExistingTraces(_traceFiles: () -> TraceFiles): FlickerBuilder = apply {
        val traceFiles = _traceFiles()
        // Remove all trace monitor and use only monitor that read from existing trace file
        this.traceMonitors.clear()
        addMonitor(NoTraceMonitor { it.addTraceResult(TraceType.WM, traceFiles.wmTrace) })
        addMonitor(NoTraceMonitor { it.addTraceResult(TraceType.SF, traceFiles.layersTrace) })
        addMonitor(
            NoTraceMonitor { it.addTraceResult(TraceType.TRANSACTION, traceFiles.transactions) }
        )
        addMonitor(
            NoTraceMonitor { it.addTraceResult(TraceType.TRANSITION, traceFiles.transitions) }
        )

        // Remove all transitions execution
        this.transitionCommands.clear()
        this.usingExistingTraces = true
        this.traceConfigs.applyToAll { it.usingExistingTraces = true }
    }

    @VisibleForTesting
    fun allowNoopTransition(): FlickerBuilder = apply {
        allowNoWmChange()
        allowNoLayersChange()
        allowNoTransitions()
    }

    private fun allowNoWmChange(): FlickerBuilder = apply {
        this.traceConfigs.wmTrace.allowNoChange = true
    }

    private fun allowNoLayersChange(): FlickerBuilder = apply {
        this.traceConfigs.layersTrace.allowNoChange = true
    }

    private fun allowNoTransitions(): FlickerBuilder = apply {
        this.traceConfigs.transitionsTrace.allowNoChange = true
    }

    /** Creates a new Flicker runner based on the current builder configuration */
    fun build(): IFlickerTestData {
        return FlickerTestData(
            instrumentation,
            device,
            outputDir,
            traceMonitors,
            setupCommands,
            transitionCommands,
            teardownCommands,
            wmHelper,
            faasEnabled,
            traceConfigs,
            usingExistingTraces
        )
    }

    /** Returns a copy of the current builder with the changes of [block] applied */
    fun copy(block: FlickerBuilder.() -> Unit) =
        FlickerBuilder(
                instrumentation,
                outputDir.toAbsolutePath(),
                wmHelper,
                setupCommands.toMutableList(),
                transitionCommands.toMutableList(),
                teardownCommands.toMutableList(),
                device,
                traceMonitors.toMutableList(),
            )
            .apply(block)

    private fun addMonitor(newMonitor: ITransitionMonitor?) {
        require(!usingExistingTraces) { "Can't add monitors after calling usingExistingTraces" }

        if (newMonitor != null) {
            traceMonitors.add(newMonitor)
        }
    }
}
