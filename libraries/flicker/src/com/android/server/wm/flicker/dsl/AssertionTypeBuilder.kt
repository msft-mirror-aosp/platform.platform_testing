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
import com.android.server.wm.flicker.assertions.FlickerSubject
import kotlin.reflect.KClass

/**
 * Placeholder for the types of assertions are supported by the Flicker DSL
 *
 * Currently supports the initial state [start], the final state [end], all states [all] and
 * used-defined [tag]s
 */
@FlickerDslMarker
abstract class AssertionTypeBuilder<out Trace : FlickerSubject, out Entry : FlickerSubject>
@JvmOverloads constructor(
    /**
     * List of trace assertions
     */
    protected val assertions: MutableList<AssertionData> = mutableListOf()
) {
    protected abstract val traceSubjectClass: KClass<out FlickerSubject>
    protected abstract val entrySubjectClass: KClass<out FlickerSubject>
    abstract fun copy(): AssertionTypeBuilder<Trace, Entry>

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
    @JvmOverloads
    fun start(
        name: String = "",
        bugId: Int = 0,
        enabled: Boolean = bugId == 0,
        assertion: Entry.() -> Any
    ) {
        assertions.add(
            AssertionData(AssertionTag.START, name, enabled, bugId,
                entrySubjectClass, assertion as FlickerSubject.() -> Unit)
        )
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
    @JvmOverloads
    fun end(
        name: String = "",
        bugId: Int = 0,
        enabled: Boolean = bugId == 0,
        assertion: Entry.() -> Any
    ) {
        assertions.add(
            AssertionData(AssertionTag.END, name, enabled, bugId,
                entrySubjectClass, assertion as FlickerSubject.() -> Unit)
        )
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
    @JvmOverloads
    fun all(
        name: String = "",
        bugId: Int = 0,
        enabled: Boolean = bugId == 0,
        assertion: Trace.() -> Any
    ) {
        assertions.add(
            AssertionData(AssertionTag.ALL, name, enabled, bugId,
                traceSubjectClass, assertion as FlickerSubject.() -> Unit)
        )
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
    @JvmOverloads
    fun tag(
        tag: String,
        name: String = "",
        bugId: Int = 0,
        enabled: Boolean = bugId == 0,
        assertion: Entry.() -> Any
    ) {
        assertions.add(
            AssertionData(tag, name, enabled, bugId,
                entrySubjectClass, assertion as FlickerSubject.() -> Unit)
        )
    }

    /**
     * Builds the list of assertions
     */
    fun build(): List<AssertionData> = assertions.toList()
}