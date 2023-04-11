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

package com.android.server.wm.flicker.traces.inputmethod

import com.android.server.common.inputmethod.InputMethodManagerServiceEntry
import com.android.server.common.inputmethod.InputMethodManagerServiceTrace
import com.android.server.wm.flicker.assertions.Assertion
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.FailureStrategy
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

/**
 * Truth subject for [InputMethodManagerServiceEntry] objects, used to make assertions over
 * behaviors that occur on a single InputMethodManagerServiceEntry state.
 *
 * To make assertions over a specific state from a trace it is recommended to create a subject using
 * [InputMethodManagerServiceTraceSubject.assertThat](myTrace) and select the specific state using:
 * [InputMethodManagerServiceTraceSubject.first] [InputMethodManagerServiceTraceSubject.last]
 * [InputMethodManagerServiceTraceSubject.entry]
 *
 * Alternatively, it is also possible to use
 * [InputMethodManagerServiceEntrySubject.assertThat](myState) or
 * Truth.assertAbout([InputMethodManagerServiceEntrySubject.getFactory]), however they will provide
 * less debug information because it uses Truth's default [FailureStrategy].
 *
 * Example: val trace = InputMethodManagerServiceTraceParser.parseFromTrace(myTraceFile) val subject
 * = InputMethodManagerServiceTraceSubject.assertThat(trace).first() ... .invoke {
 * myCustomAssertion(this) }
 */
class InputMethodManagerServiceEntrySubject
private constructor(
    fm: FailureMetadata,
    val entry: InputMethodManagerServiceEntry,
    val trace: InputMethodManagerServiceTrace?,
    override val parent: FlickerSubject?
) :
    FlickerSubject(fm, entry),
    IInputMethodManagerServiceSubject<InputMethodManagerServiceEntrySubject> {
    override val timestamp: Long
        get() = entry.timestamp
    override val selfFacts = listOf(Fact.fact("InputMethodManagerServiceEntry", entry))

    /** Executes a custom [assertion] on the current subject */
    operator fun invoke(
        assertion: Assertion<InputMethodManagerServiceEntry>
    ): InputMethodManagerServiceEntrySubject = apply { assertion(this.entry) }

    /** {@inheritDoc} */
    override fun isEmpty(): InputMethodManagerServiceEntrySubject = apply {
        check("InputMethodManagerServiceEntry").that(entry).isNull()
    }

    /** {@inheritDoc} */
    override fun isNotEmpty(): InputMethodManagerServiceEntrySubject = apply {
        check("InputMethodManagerServiceEntry").that(entry).isNotNull()
    }

    override fun toString(): String {
        return "InputMethodManagerServiceEntrySubject($entry)"
    }

    companion object {
        /** Boilerplate Subject.Factory for InputMethodManagerServiceTraceSubject */
        private fun getFactory(
            trace: InputMethodManagerServiceTrace?,
            parent: FlickerSubject?
        ): Factory<Subject, InputMethodManagerServiceEntry> = Factory { fm, subject ->
            InputMethodManagerServiceEntrySubject(fm, subject, trace, parent)
        }

        /**
         * Creates a [InputMethodManagerServiceEntrySubject] to represent a
         * InputMethodManagerService state[entry], which can be used to make assertions.
         *
         * @param entry InputMethodManagerService trace entry
         * @param parent Trace that contains this entry (optional)
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            entry: InputMethodManagerServiceEntry,
            trace: InputMethodManagerServiceTrace? = null,
            parent: FlickerSubject? = null
        ): InputMethodManagerServiceEntrySubject {
            val strategy = FlickerFailureStrategy()
            val subject =
                StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                    .about(getFactory(trace, parent))
                    .that(entry) as InputMethodManagerServiceEntrySubject
            strategy.init(subject)
            return subject
        }

        /** Static method for getting the subject factory (for use with assertAbout()) */
        @JvmStatic
        @JvmOverloads
        fun entries(
            trace: InputMethodManagerServiceTrace? = null,
            parent: FlickerSubject? = null
        ): Factory<Subject, InputMethodManagerServiceEntry> = getFactory(trace, parent)
    }
}
