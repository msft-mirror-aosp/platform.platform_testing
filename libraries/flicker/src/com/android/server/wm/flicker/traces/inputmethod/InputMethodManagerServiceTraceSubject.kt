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

import com.android.server.common.inputmethod.InputMethodManagerServiceEntry
import com.android.server.common.inputmethod.InputMethodManagerServiceTrace
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
 * Truth subject for [InputMethodManagerServiceTrace] objects, used to make assertions over
 * behaviors that occur throughout a whole trace
 *
 * To make assertions over a trace it is recommended to create a subject using
 * [InputMethodManagerServiceTraceSubject.assertThat](myTrace). Alternatively, it is also possible
 * to use Truth.assertAbout(InputMethodManagerServiceTraceSubject.FACTORY), however it will provide
 * less debug information because it uses Truth's default [FailureStrategy].
 *
 * Example: val trace = InputMethodManagerServiceTraceParser.parseFromTrace(myTraceFile) val subject
 * = InputMethodManagerServiceTraceSubject.assertThat(trace) Example2: val trace =
 * InputMethodManagerServiceTraceParser.parseFromTrace(myTraceFile) val subject =
 * InputMethodManagerServiceTraceSubject.assertThat(trace) { check("Custom check") {
 * myCustomAssertion(this) } }
 */
class InputMethodManagerServiceTraceSubject
private constructor(
    fm: FailureMetadata,
    val trace: InputMethodManagerServiceTrace,
    override val parent: InputMethodManagerServiceTraceSubject?,
    val facts: Collection<Fact>
) :
    FlickerTraceSubject<InputMethodManagerServiceEntrySubject>(fm, trace),
    IInputMethodManagerServiceSubject<InputMethodManagerServiceTraceSubject> {

    override val selfFacts by lazy {
        val allFacts = super.selfFacts.toMutableList()
        allFacts.addAll(facts)
        allFacts
    }

    override val subjects by lazy {
        trace.entries.map { InputMethodManagerServiceEntrySubject.assertThat(it, trace, this) }
    }

    /** {@inheritDoc} */
    override fun then(): InputMethodManagerServiceTraceSubject = apply { super.then() }

    /** {@inheritDoc} */
    override fun isEmpty(): InputMethodManagerServiceTraceSubject = apply {
        check("InputMethodManagerServiceTrace").that(trace).isEmpty()
    }

    /** {@inheritDoc} */
    override fun isNotEmpty(): InputMethodManagerServiceTraceSubject = apply {
        check("InputMethodManagerServiceTrace").that(trace).isNotEmpty()
    }

    /** Executes a custom [assertion] on the current subject */
    @JvmOverloads
    operator fun invoke(
        name: String,
        isOptional: Boolean = false,
        assertion: Assertion<InputMethodManagerServiceEntrySubject>
    ): InputMethodManagerServiceTraceSubject = apply { addAssertion(name, isOptional, assertion) }

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
    fun entry(timestamp: Long): InputMethodManagerServiceEntrySubject =
        subjects.first { it.entry.timestamp == timestamp }

    /**
     * @return List of [InputMethodManagerServiceEntrySubject]s matching [predicate] in the order
     *   they appear in the trace
     */
    fun imeClientEntriesThat(
        predicate: (InputMethodManagerServiceEntry) -> Boolean
    ): List<InputMethodManagerServiceEntrySubject> = subjects.filter { predicate(it.entry) }

    companion object {
        /** Boilerplate Subject.Factory for InputMethodManagerServiceTraceSubject */
        private fun getFactory(
            parent: InputMethodManagerServiceTraceSubject?,
            facts: Collection<Fact> = emptyList()
        ): Factory<Subject, InputMethodManagerServiceTrace> = Factory { fm, subject ->
            InputMethodManagerServiceTraceSubject(fm, subject, parent, facts)
        }

        /**
         * Creates a [InputMethodManagerServiceTraceSubject] to represent a
         * InputMethodManagerService trace, which can be used to make assertions.
         *
         * @param trace InputMethodManagerService trace
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            trace: InputMethodManagerServiceTrace,
            parent: InputMethodManagerServiceTraceSubject? = null,
            facts: Collection<Fact> = emptyList()
        ): InputMethodManagerServiceTraceSubject {
            val strategy = FlickerFailureStrategy()
            val subject =
                StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                    .about(getFactory(parent, facts))
                    .that(trace) as InputMethodManagerServiceTraceSubject
            strategy.init(subject)
            return subject
        }

        /** Static method for getting the subject factory (for use with assertAbout()) */
        @JvmStatic
        fun entries(
            parent: InputMethodManagerServiceTraceSubject?,
            facts: Collection<Fact> = emptyList()
        ): Factory<Subject, InputMethodManagerServiceTrace> {
            return getFactory(parent, facts)
        }
    }
}
