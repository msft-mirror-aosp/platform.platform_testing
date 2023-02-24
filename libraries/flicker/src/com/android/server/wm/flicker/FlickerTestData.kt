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
import androidx.test.uiautomator.UiDevice
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import java.nio.file.Path

@DslMarker annotation class FlickerDslMarker

/**
 * Defines the runner for the flicker tests. This component is responsible for running the flicker
 * tests and executing assertions on the traces to check for inconsistent behaviors on
 * [WindowManagerTrace] and [LayersTrace]
 */
@FlickerDslMarker
open class FlickerTestData(
    /** Instrumentation to run the tests */
    override val instrumentation: Instrumentation,
    /** Test automation component used to interact with the device */
    override val device: UiDevice,
    /** Output directory for test results */
    override val outputDir: Path,
    /** Enabled tracing monitors */
    override val traceMonitors: List<ITransitionMonitor>,
    /** Commands to be executed before the transition */
    override val transitionSetup: List<IFlickerTestData.() -> Any>,
    /** Test commands */
    override val transitions: List<IFlickerTestData.() -> Any>,
    /** Commands to be executed after the transition */
    override val transitionTeardown: List<IFlickerTestData.() -> Any>,
    /** Helper object for WM Synchronization */
    override val wmHelper: WindowManagerStateHelper,
    /** Whether to run Flicker as a Service on the collected transition traces */
    val faasEnabled: Boolean = false,
    /**
     * Defines properties we allow on traces (e.g. is it valid for a transition to not have any
     * changed in the WM and Layers states)
     */
    val traceConfigs: TraceConfigs = DEFAULT_TRACE_CONFIG,
    /**
     * Whether the transition needs to be executed or if the trace monitors will just return the
     * traces without needing to run anything on device.
     */
    val usingExistingTraces: Boolean = false
) : AbstractFlickerTestData()
