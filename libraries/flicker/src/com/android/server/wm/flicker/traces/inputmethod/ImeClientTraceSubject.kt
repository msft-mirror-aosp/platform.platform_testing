/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.wm.flicker.traces.inputmethod

import com.android.server.common.inputmethod.ImeClientEntry
import com.android.server.common.inputmethod.ImeClientTrace
import com.android.server.wm.flicker.assertions.Assertion
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.FailureStrategy
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

/**
 * Truth subject for [ImeClientTrace] objects, used to make assertions over behaviors that occur
 * throughout a whole trace
 *
 * To make assertions over a trace it is recommended to create a subject using
 * [ImeClientTraceSubject.assertThat](myTrace). Alternatively, it is also possible to use
 * Truth.assertAbout(ImeClientTraceSubject.FACTORY), however it will provide less debug information
 * because it uses Truth's default [FailureStrategy].
 *
 * Example: val trace = ImeClientTraceParser.parseFromTrace(myTraceFile) val subject =
 * ImeClientTraceSubject.assertThat(trace) Example2: val trace =
 * ImeClientTraceParser.parseFromTrace(myTraceFile) val subject =
 * ImeClientTraceSubject.assertThat(trace) { check("Custom check") { myCustomAssertion(this) } }
 */
class ImeClientTraceSubject
private constructor(
    fm: FailureMetadata,
    val trace: ImeClientTrace,
    override val parent: ImeClientTraceSubject?,
    val facts: Collection<Fact>
) :
    FlickerTraceSubject<ImeClientEntrySubject>(fm, trace),
    IImeClientSubject<ImeClientTraceSubject> {

    override val selfFacts by lazy {
        val allFacts = super.selfFacts.toMutableList()
        allFacts.addAll(facts)
        allFacts
    }

    override val subjects by lazy {
        trace.entries.map { ImeClientEntrySubject.assertThat(it, trace, this) }
    }

    /** {@inheritDoc} */
    override fun then(): ImeClientTraceSubject = apply { super.then() }

    /** {@inheritDoc} */
    override fun isEmpty(): ImeClientTraceSubject = apply {
        check("ImeClientTrace").that(trace).isEmpty()
    }

    /** {@inheritDoc} */
    override fun isNotEmpty(): ImeClientTraceSubject = apply {
        check("ImeClientTrace").that(trace).isNotEmpty()
    }

    /** Executes a custom [assertion] on the current subject */
    @JvmOverloads
    operator fun invoke(
        name: String,
        isOptional: Boolean = false,
        assertion: Assertion<ImeClientEntrySubject>
    ): ImeClientTraceSubject = apply { addAssertion(name, isOptional, assertion) }

    /** Run the assertions for all trace entries within the specified time range */
    fun forRange(startTime: Long, endTime: Long) {
        val subjectsInRange = subjects.filter { it.entry.timestamp in startTime..endTime }
        assertionsChecker.test(subjectsInRange)
    }

    /**
     * User-defined entry point for the trace entry with [timestamp]
     *
     * @param timestamp of the entry
     */
    fun entry(timestamp: Long): ImeClientEntrySubject =
        subjects.first { it.entry.timestamp == timestamp }

    /**
     * @return List of [ImeClientEntrySubject]s matching [predicate] in the order they appear in the
     *   trace
     */
    fun imeClientEntriesThat(predicate: (ImeClientEntry) -> Boolean): List<ImeClientEntrySubject> =
        subjects.filter { predicate(it.entry) }

    companion object {
        /** Boilerplate Subject.Factory for ImeClientTraceSubject */
        private fun getFactory(
            parent: ImeClientTraceSubject?,
            facts: Collection<Fact> = emptyList()
        ): Factory<Subject, ImeClientTrace> = Factory { fm, subject ->
            ImeClientTraceSubject(fm, subject, parent, facts)
        }

        /**
         * Creates a [ImeClientTraceSubject] to represent a ImeClient trace, which can be used to
         * make assertions.
         *
         * @param trace ImeClient trace
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            trace: ImeClientTrace,
            parent: ImeClientTraceSubject? = null,
            facts: Collection<Fact> = emptyList()
        ): ImeClientTraceSubject {
            val strategy = FlickerFailureStrategy()
            val subject =
                StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                    .about(getFactory(parent, facts))
                    .that(trace) as ImeClientTraceSubject
            strategy.init(subject)
            return subject
        }

        /** Static method for getting the subject factory (for use with assertAbout()) */
        @JvmStatic
        fun entries(
            parent: ImeClientTraceSubject?,
            facts: Collection<Fact> = emptyList()
        ): Factory<Subject, ImeClientTrace> {
            return getFactory(parent, facts)
        }
    }
}
