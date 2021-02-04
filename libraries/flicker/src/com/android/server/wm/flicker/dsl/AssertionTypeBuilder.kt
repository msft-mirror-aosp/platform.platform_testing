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
import com.android.server.wm.flicker.assertions.AssertionBlock
import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.android.server.wm.flicker.traces.eventlog.FocusEventSubject
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import kotlin.reflect.KClass

/**
 * Placeholder for the types of assertions are supported by the Flicker DSL
 *
 * Currently supports the initial state [start], the final state [end], all states [all] and
 * used-defined [tag]s
 */
@FlickerDslMarker
open class AssertionTypeBuilder<out Trace : FlickerSubject, out Entry : FlickerSubject>
@JvmOverloads constructor(
    /**
     * Moment where the assertion should run
     */
    @AssertionBlock protected val block: Int,
    /**
     * Type of flicker subject for trace assertions
     */
    protected val traceSubjectClass: KClass<out FlickerSubject>,
    /**
     * Type of flicker subject for entry assertions
     */
    protected val entrySubjectClass: KClass<out FlickerSubject>,
    /**
     * List of trace assertions
     */
    protected val assertions: MutableList<AssertionData> = mutableListOf()
) {
    open fun copy(): AssertionTypeBuilder<Trace, Entry> {
        return AssertionTypeBuilder(block, traceSubjectClass, entrySubjectClass, assertions)
    }

    /**
     * Assertions to run only in the first trace entry
     *
     * Used for assertions that check the first state in the test. For example:
     *    initialState.shows(A).and().shows(B)
     *
     * This command can be used multiple times, and the results are appended
     *
     * @param name Name of the assertion to appear on errors
     * @param bugId Associated with the assertion
     * @param assertion Assertion command
     */
    @JvmOverloads
    open fun start(name: String, bugId: Int = 0, assertion: Entry.() -> Any) {
        addEntry(AssertionTag.START, name, bugId, assertion = assertion)
    }

    /**
     * Assertions to run only in the last trace entry
     *
     * Used for assertions that check the last state in the test. For example:
     *    endState.shows(A).and().shows(B)
     *
     * This command can be used multiple times, and the results are appended
     *
     * @param name Name of the assertion to appear on errors
     * @param bugId Associated with the assertion
     * @param assertion Assertion command
     */
    @JvmOverloads
    open fun end(name: String, bugId: Int = 0, assertion: Entry.() -> Any) {
        addEntry(AssertionTag.END, name, bugId, assertion = assertion)
    }

    /**
     * Assertions to run in all trace entries
     *
     * Used for assertions that check all states in the test. For example:
     *    trace.shows(A).then().hides(A).and().shows(B)
     *
     * This command can be used multiple times, and the results are appended
     *
     * @param name Name of the assertion to appear on errors
     * @param bugId Associated with the assertion
     * @param assertion Assertion command
     */
    @JvmOverloads
    open fun all(name: String, bugId: Int = 0, assertion: Trace.() -> Any) {
        addTrace(name, bugId, assertion = assertion)
    }

    /**
     * Assertions to run at specific moments in the trace (identified by a [tag])
     *
     * Used for assertions that check the state in the test when [tag] is created. For example:
     *    taggedMoment.shows(A).and().shows(B)
     *
     * This command can be used multiple times, and the results are appended.
     *
     * To create a new tag during testing use [com.android.server.wm.flicker.Flicker.createTag]
     *
     * @param tag Tag used during tracing
     * @param name Name of the assertion to appear on errors
     * @param bugId Associated with the assertion
     * @param assertion Assertion command
     */
    @JvmOverloads
    open fun tag(tag: String, name: String = tag, bugId: Int = 0, assertion: Entry.() -> Any) {
        addEntry(tag, name, bugId, assertion = assertion)
    }

    protected fun addEntry(
        tag: String,
        name: String,
        bugId: Int,
        @AssertionBlock block: Int = this.block,
        assertion: Entry.() -> Any
    ) {
        assertions.add(
            AssertionData(tag, name, bugId, entrySubjectClass, block,
                assertion as FlickerSubject.() -> Unit)
        )
    }

    protected fun addTrace(
        name: String,
        bugId: Int,
        @AssertionBlock block: Int = this.block,
        assertion: Trace.() -> Any
    ) {
        assertions.add(
            AssertionData(AssertionTag.ALL, name, bugId, traceSubjectClass, block,
                assertion as FlickerSubject.() -> Unit)
        )
    }

    /**
     * Builds the list of assertions
     */
    fun build(): List<AssertionData> = assertions.toList()

    companion object {
        fun newWMAssertions(@AssertionBlock block: Int):
            AssertionTypeBuilder<WindowManagerTraceSubject, WindowManagerStateSubject> {
            return AssertionTypeBuilder(block, WindowManagerTraceSubject::class,
                WindowManagerStateSubject::class)
        }

        fun newLayerAssertions(@AssertionBlock block: Int):
            AssertionTypeBuilder<LayersTraceSubject, LayerTraceEntrySubject> {
            return AssertionTypeBuilder(block, LayersTraceSubject::class,
                LayerTraceEntrySubject::class)
        }

        fun newEventLogAssertions(@AssertionBlock block: Int):
            AssertionTypeBuilder<EventLogSubject, FocusEventSubject> {
            return AssertionTypeBuilder(block, EventLogSubject::class,
                FocusEventSubject::class)
        }
    }
}

typealias WmAssertionBuilder = AssertionTypeBuilder<WindowManagerTraceSubject,
    WindowManagerStateSubject>
typealias LayersAssertionBuilder = AssertionTypeBuilder<LayersTraceSubject, LayerTraceEntrySubject>
typealias EventLogAssertionBuilder = AssertionTypeBuilder<EventLogSubject, FocusEventSubject>