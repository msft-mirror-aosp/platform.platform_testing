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

import com.android.server.common.inputmethod.InputMethodServiceEntry
import com.android.server.common.inputmethod.InputMethodServiceTrace
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
 * Truth subject for [InputMethodServiceTrace] objects, used to make assertions over behaviors that
 * occur throughout a whole trace
 *
 * To make assertions over a trace it is recommended to create a subject using
 * [InputMethodServiceTraceSubject.assertThat](myTrace). Alternatively, it is also possible to use
 * Truth.assertAbout(InputMethodServiceTraceSubject.FACTORY), however it will provide less debug
 * information because it uses Truth's default [FailureStrategy].
 *
 * Example: val trace = InputMethodServiceTraceParser.parseFromTrace(myTraceFile) val subject =
 * InputMethodServiceTraceSubject.assertThat(trace) Example2: val trace =
 * InputMethodServiceTraceParser.parseFromTrace(myTraceFile) val subject =
 * InputMethodServiceTraceSubject.assertThat(trace) { check("Custom check") {
 * myCustomAssertion(this) } }
 */
class InputMethodServiceTraceSubject
private constructor(
    fm: FailureMetadata,
    val trace: InputMethodServiceTrace,
    override val parent: InputMethodServiceTraceSubject?,
    val facts: Collection<Fact>
) :
    FlickerTraceSubject<InputMethodServiceEntrySubject>(fm, trace),
    IInputMethodServiceSubject<InputMethodServiceTraceSubject> {

    override val selfFacts by lazy {
        val allFacts = super.selfFacts.toMutableList()
        allFacts.addAll(facts)
        allFacts
    }

    override val subjects by lazy {
        trace.entries.map { InputMethodServiceEntrySubject.assertThat(it, trace, this) }
    }

    /** {@inheritDoc} */
    override fun then(): InputMethodServiceTraceSubject = apply { super.then() }

    /** {@inheritDoc} */
    override fun isEmpty(): InputMethodServiceTraceSubject = apply {
        check("InputMethodServiceTrace").that(trace).isEmpty()
    }

    /** {@inheritDoc} */
    override fun isNotEmpty(): InputMethodServiceTraceSubject = apply {
        check("InputMethodServiceTrace").that(trace).isNotEmpty()
    }

    /** Executes a custom [assertion] on the current subject */
    @JvmOverloads
    operator fun invoke(
        name: String,
        isOptional: Boolean = false,
        assertion: Assertion<InputMethodServiceEntrySubject>
    ): InputMethodServiceTraceSubject = apply { addAssertion(name, isOptional, assertion) }

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
    fun entry(timestamp: Long): InputMethodServiceEntrySubject =
        subjects.first { it.entry.timestamp == timestamp }

    /**
     * @return List of [InputMethodServiceEntrySubject]s matching [predicate] in the order they
     *   appear in the trace
     */
    fun imeClientEntriesThat(
        predicate: (InputMethodServiceEntry) -> Boolean
    ): List<InputMethodServiceEntrySubject> = subjects.filter { predicate(it.entry) }

    companion object {
        /** Boilerplate Subject.Factory for InputMethodServiceTraceSubject */
        private fun getFactory(
            parent: InputMethodServiceTraceSubject?,
            facts: Collection<Fact> = emptyList()
        ): Factory<Subject, InputMethodServiceTrace> = Factory { fm, subject ->
            InputMethodServiceTraceSubject(fm, subject, parent, facts)
        }

        /**
         * Creates a [InputMethodServiceTraceSubject] to represent a InputMethodService trace, which
         * can be used to make assertions.
         *
         * @param trace InputMethodService trace
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            trace: InputMethodServiceTrace,
            parent: InputMethodServiceTraceSubject? = null,
            facts: Collection<Fact> = emptyList()
        ): InputMethodServiceTraceSubject {
            val strategy = FlickerFailureStrategy()
            val subject =
                StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                    .about(getFactory(parent, facts))
                    .that(trace) as InputMethodServiceTraceSubject
            strategy.init(subject)
            return subject
        }

        /** Static method for getting the subject factory (for use with assertAbout()) */
        @JvmStatic
        fun entries(
            parent: InputMethodServiceTraceSubject?,
            facts: Collection<Fact> = emptyList()
        ): Factory<Subject, InputMethodServiceTrace> {
            return getFactory(parent, facts)
        }
    }
}
