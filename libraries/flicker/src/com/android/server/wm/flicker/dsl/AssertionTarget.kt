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

import com.android.server.wm.flicker.FlickerDslMarker
import com.android.server.wm.flicker.assertions.FlickerAssertionError
import com.android.server.wm.flicker.FlickerRunResult
import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.common.traces.ITraceEntry
import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import com.android.server.wm.flicker.traces.layers.LayerTraceEntry
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTrace
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceEntry
import com.android.server.wm.flicker.traces.windowmanager.WmTraceSubject

typealias WmAssertion = AssertionType<WmTraceSubject, WindowManagerTraceEntry>
typealias LayersAssertion = AssertionType<LayersTraceSubject, LayerTraceEntry>
typealias EventLogAssertion = AssertionType<EventLogSubject, FocusEvent>
/**
 * Placeholder for components that can be asserted by the Flicker DSL
 *
 * Currently supports [WindowManagerTrace], [LayersTrace] and list of [FocusEvent]s
 */
@FlickerDslMarker
class AssertionTarget private constructor(
    private val wmAssertions: WmAssertion,
    private val layerAssertions: LayersAssertion,
    private val eventLogAssertions: EventLogAssertion
) {
    constructor() : this(WmAssertion(), LayersAssertion(), EventLogAssertion())

    /**
     * Copy constructor
     */
    constructor(otherTarget: AssertionTarget) : this(
        WmAssertion(otherTarget.wmAssertions.assertions.toMutableList()),
        LayersAssertion(otherTarget.layerAssertions.assertions.toMutableList()),
        EventLogAssertion(otherTarget.eventLogAssertions.assertions.toMutableList())
    )

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

    /**
     * Assertions to check the event log
     *
     * This command can be used multiple times, and the results are appended
     *
     * @param assertion Type of assertion determining which part of the trace will be checked
     */
    fun eventLog(assertion: EventLogAssertion.() -> Unit) {
        eventLogAssertions.apply { assertion() }
    }

    private fun <Assertions : AssertionType<Subject, Entry>,
        Entry : ITraceEntry, Subject> checkAssertions(
            assertions: Assertions,
            run: FlickerRunResult,
            factory: () -> Subject,
            failureFactory: (Throwable, AssertionData<out Subject>) -> FlickerAssertionError,
            includeFlakyAssertions: Boolean
        ): List<FlickerAssertionError> {
        return assertions
            .filter { (it.enabled || includeFlakyAssertions) && it.tag == run.assertionTag }
            .map { it to runCatching { it.assertion(factory()) } }
            .filter { it.second.isFailure }
            .map {
                val failure = it.second.exceptionOrNull()
                    ?: error("No exception associated to ${it.first}: ${it.second}")
                failureFactory(failure, it.first)
            }
    }

    /**
     * Run the assertions on a flicker test run
     *
     * @param run Results of a flicker test run
     * @param includeFlakyAssertions Include flaky assertions
     * @return List of failures
     */
    fun checkAssertions(
        run: FlickerRunResult,
        includeFlakyAssertions: Boolean
    ): List<FlickerAssertionError> {
        val failures = mutableListOf<FlickerAssertionError>()
        run.wmTrace?.let {
            failures.addAll(this.checkAssertions(
                wmAssertions,
                run,
                { WmTraceSubject.assertThat(it) },
                { error, assertion ->
                    FlickerAssertionError(error, assertion, run, run.wmTraceFile)
                },
                includeFlakyAssertions
            ))
        }

        run.layersTrace?.let {
            failures.addAll(this.checkAssertions(
                layerAssertions,
                run,
                { LayersTraceSubject.assertThat(it) },
                { error, assertion ->
                    FlickerAssertionError(error, assertion, run, run.layersTraceFile)
                },
                includeFlakyAssertions
            ))
        }

        failures.addAll(this.checkAssertions(eventLogAssertions, run,
            { EventLogSubject.assertThat(run) },
            { error, assertion -> FlickerAssertionError(error, assertion, run, trace = null) },
            includeFlakyAssertions
        ))

        return failures
    }
}