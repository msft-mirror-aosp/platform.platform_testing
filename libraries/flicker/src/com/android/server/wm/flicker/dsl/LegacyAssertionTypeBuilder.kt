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
 * Placeholder for the assertions on the Flicker DSL supporting legacy assertion format.
 *
 * Currently supports the initial state [start], the final state [end], all states [all] and
 * used-defined [tag]s
 *
 * For new tests use the new presubmit blocks [AssertionBlockBuilder.presubmit],
 * [AssertionBlockBuilder.postsubmit] and [AssertionBlockBuilder.flaky].
 */
@FlickerDslMarker
@Deprecated("Move the assertion into one of the specific blocks (presubmit, postsubmit, flaky)")
class LegacyAssertionTypeBuilder<out Trace : FlickerSubject, out Entry : FlickerSubject>(
    traceSubjectClass: KClass<out FlickerSubject>,
    entrySubjectClass: KClass<out FlickerSubject>,
    /**
     * List of trace assertions
     */
    assertions: MutableList<AssertionData> = mutableListOf()
) : AssertionTypeBuilder<Trace, Entry>(AssertionBlock.PRESUBMIT, traceSubjectClass,
    entrySubjectClass, assertions) {
    @AssertionBlock
    private fun Boolean.toBlock(): Int {
        return if (this) {
            AssertionBlock.PRESUBMIT
        } else {
            AssertionBlock.FLAKY
        }
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
     * @param enabled If the assertion is enabled or not
     * @param bugId If the assertion is disabled because of a bug, which bug is it.
     * @param assertion Assertion command
     */
    fun start(
        name: String,
        bugId: Int = 0,
        enabled: Boolean = bugId == 0,
        assertion: Entry.() -> Any
    ) {
        addEntry(AssertionTag.START, name, bugId, enabled.toBlock(), assertion)
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
     * @param enabled If the assertion is enabled or not
     * @param bugId If the assertion is disabled because of a bug, which bug is it.
     * @param assertion Assertion command
     */
    fun end(
        name: String,
        bugId: Int = 0,
        enabled: Boolean = bugId == 0,
        assertion: Entry.() -> Any
    ) {
        addEntry(AssertionTag.END, name, bugId, enabled.toBlock(), assertion)
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
     * @param enabled If the assertion is enabled or not
     * @param bugId If the assertion is disabled because of a bug, which bug is it.
     * @param assertion Assertion command
     */
    fun all(
        name: String,
        bugId: Int = 0,
        enabled: Boolean = bugId == 0,
        assertion: Trace.() -> Any
    ) {
        addTrace(name, bugId, enabled.toBlock(), assertion)
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
     * @param enabled If the assertion is enabled or not
     * @param bugId If the assertion is disabled because of a bug, which bug is it.
     * @param assertion Assertion command
     */
    fun tag(
        tag: String,
        name: String = tag,
        bugId: Int = 0,
        enabled: Boolean = bugId == 0,
        assertion: Entry.() -> Any
    ) {
        addEntry(tag, name, bugId, enabled.toBlock(), assertion)
    }

    /**
     * {@inheritDoc}
     */
    override fun start(name: String, bugId: Int, assertion: Entry.() -> Any) {
        addEntry(AssertionTag.START, name, bugId, assertion = assertion,
            block = (bugId == 0).toBlock())
    }

    /**
     * {@inheritDoc}
     */
    override fun end(name: String, bugId: Int, assertion: Entry.() -> Any) {
        addEntry(AssertionTag.END, name, bugId, assertion = assertion,
            block = (bugId == 0).toBlock())
    }

    /**
     * {@inheritDoc}
     */
    override fun all(name: String, bugId: Int, assertion: Trace.() -> Any) {
        addTrace(name, bugId, assertion = assertion,
            block = (bugId == 0).toBlock())
    }

    /**
     * {@inheritDoc}
     */
    override fun tag(tag: String, name: String, bugId: Int, assertion: Entry.() -> Any) {
        addEntry(tag, name, bugId, assertion = assertion,
            block = (bugId == 0).toBlock())
    }

    override fun copy(): AssertionTypeBuilder<Trace, Entry> {
        return LegacyAssertionTypeBuilder(traceSubjectClass, entrySubjectClass, assertions)
    }

    companion object {
        fun newWMAssertions():
            LegacyAssertionTypeBuilder<WindowManagerTraceSubject, WindowManagerStateSubject> {
            return LegacyAssertionTypeBuilder(WindowManagerTraceSubject::class,
                WindowManagerStateSubject::class)
        }

        fun newLayerAssertions():
            LegacyAssertionTypeBuilder<LayersTraceSubject, LayerTraceEntrySubject> {
            return LegacyAssertionTypeBuilder(LayersTraceSubject::class,
                LayerTraceEntrySubject::class)
        }

        fun newEventLogAssertions():
            LegacyAssertionTypeBuilder<EventLogSubject, FocusEventSubject> {
            return LegacyAssertionTypeBuilder(EventLogSubject::class,
                FocusEventSubject::class)
        }
    }
}

typealias WmAssertionBuilderLegacy = LegacyAssertionTypeBuilder<WindowManagerTraceSubject,
    WindowManagerStateSubject>
typealias LayersAssertionBuilderLegacy = LegacyAssertionTypeBuilder<LayersTraceSubject,
    LayerTraceEntrySubject>
typealias EventLogAssertionBuilderLegacy = LegacyAssertionTypeBuilder<EventLogSubject,
    FocusEventSubject>