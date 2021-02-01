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

package com.android.server.wm.flicker.dsl

import com.android.server.wm.flicker.FlickerDslMarker
import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject

/**
 * Placeholder for components that can be asserted by the Flicker DSL
 *
 * Currently supports [WindowManagerTraceSubject], [LayersTraceSubject] and list of [FocusEvent]s
 */
@FlickerDslMarker
class AssertionTargetBuilder private constructor(
    private val wmAssertionsBuilder: WmAssertionBuilder = WmAssertionBuilder(),
    private val layerAssertionsBuilder: LayersAssertionBuilder = LayersAssertionBuilder(),
    private val eventLogAssertionsBuilder: EventLogAssertionBuilder = EventLogAssertionBuilder()
) {
    constructor() : this(WmAssertionBuilder(), LayersAssertionBuilder(),
        EventLogAssertionBuilder())

    /**
     * Copy constructor
     */
    constructor(otherTarget: AssertionTargetBuilder) : this(
        otherTarget.wmAssertionsBuilder.copy() as WmAssertionBuilder,
        otherTarget.layerAssertionsBuilder.copy() as LayersAssertionBuilder,
        otherTarget.eventLogAssertionsBuilder.copy() as EventLogAssertionBuilder
    )

    fun build(): List<AssertionData> {
        return listOf(
            wmAssertionsBuilder.build(),
            layerAssertionsBuilder.build(),
            eventLogAssertionsBuilder.build()
        ).flatten()
    }

    /**
     * Assertions to check the WindowManager trace
     *
     * This command can be used multiple times, and the results are appended
     *
     * @param assertion Type of assertion determining which part of the trace will be
     * checked (e.g., start, end, all)
     */
    fun windowManagerTrace(assertion: WmAssertionBuilder.() -> Unit) {
        wmAssertionsBuilder.apply { assertion() }
    }

    /**
     * Assertions to check the Layers trace
     *
     * This command can be used multiple times, and the results are appended
     *
     * @param assertion Type of assertion determining which part of the trace will be
     * checked (e.g., start, end, all)
     */
    fun layersTrace(assertion: LayersAssertionBuilder.() -> Unit) {
        layerAssertionsBuilder.apply { assertion() }
    }

    /**
     * Assertions to check the event log
     *
     * This command can be used multiple times, and the results are appended
     *
     * @param assertion Type of assertion determining which part of the trace will be checked
     */
    fun eventLog(assertion: EventLogAssertionBuilder.() -> Unit) {
        eventLogAssertionsBuilder.apply { assertion() }
    }
}