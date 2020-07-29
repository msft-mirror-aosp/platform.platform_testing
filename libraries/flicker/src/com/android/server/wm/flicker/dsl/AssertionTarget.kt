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

import com.android.server.wm.flicker.*
import com.android.server.wm.flicker.traces.layers.LayerTraceEntry
import com.android.server.wm.flicker.traces.layers.LayersTrace
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTrace
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceEntry
import com.android.server.wm.flicker.traces.windowmanager.WmTraceSubject

typealias WmAssertion = AssertionType<WmTraceSubject, WindowManagerTrace, WindowManagerTraceEntry>
typealias LayersAssertion = AssertionType<LayersTraceSubject, LayersTrace, LayerTraceEntry>

/**
 * Placeholder for components that can be asserted by the Flicker DSL
 *
 * Currently supports [WindowManagerTrace] and [LayersTrace]
 */
@FlickerDslMarker
class AssertionTarget {
    val wmAssertions = WmAssertion()
    val layerAssertions = LayersAssertion()

    /**
     * Assertions to check the WindowManager trace
     *
     * This command can be used multiple times, and the results are appended
     *
     * @param assertion Type of assertion determining which part of the trace will be checked (e.g., start, end, all)
     */
    fun windowManagerTrace(assertion: WmAssertion.() -> Unit) {
        wmAssertions.apply { assertion() }
    }

    /**
     * Assertions to check the Layers trace
     *
     * This command can be used multiple times, and the results are appended
     *
     * @param assertion Type of assertion determining which part of the trace will be checked (e.g., start, end, all)
     */
    fun layersTrace(assertion: LayersAssertion.() -> Unit) {
        layerAssertions.apply { assertion() }
    }
}