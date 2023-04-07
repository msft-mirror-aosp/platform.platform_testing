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
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.FailureStrategy
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

/**
 * Truth subject for [InputMethodServiceEntry] objects, used to make assertions over behaviors that
 * occur on a single InputMethodServiceEntry state.
 *
 * To make assertions over a specific state from a trace it is recommended to create a subject using
 * [InputMethodServiceTraceSubject.assertThat](myTrace) and select the specific state using:
 * [InputMethodServiceTraceSubject.first] [InputMethodServiceTraceSubject.last]
 * [InputMethodServiceTraceSubject.entry]
 *
 * Alternatively, it is also possible to use [InputMethodServiceEntrySubject.assertThat](myState) or
 * Truth.assertAbout([InputMethodServiceEntrySubject.getFactory]), however they will provide less
 * debug information because it uses Truth's default [FailureStrategy].
 *
 * Example: val trace = InputMethodServiceTraceParser.parseFromTrace(myTraceFile) val subject =
 * InputMethodServiceTraceSubject.assertThat(trace).first() ... .invoke { myCustomAssertion(this) }
 */
class InputMethodServiceEntrySubject
private constructor(
    fm: FailureMetadata,
    val entry: InputMethodServiceEntry,
    val trace: InputMethodServiceTrace?,
    override val parent: FlickerSubject?
) : FlickerSubject(fm, entry), IInputMethodServiceSubject<InputMethodServiceEntrySubject> {
    override val timestamp: Long
        get() = entry.timestamp
    override val selfFacts = listOf(Fact.fact("InputMethodServiceEntry", entry))

    /** Executes a custom [assertion] on the current subject */
    operator fun invoke(
        assertion: Assertion<InputMethodServiceEntry>
    ): InputMethodServiceEntrySubject = apply { assertion(this.entry) }

    /** {@inheritDoc} */
    override fun isEmpty(): InputMethodServiceEntrySubject = apply {
        check("InputMethodServiceEntry").that(entry).isNull()
    }

    /** {@inheritDoc} */
    override fun isNotEmpty(): InputMethodServiceEntrySubject = apply {
        check("InputMethodServiceEntry").that(entry).isNotNull()
    }

    override fun toString(): String {
        return "InputMethodServiceEntrySubject($entry)"
    }

    companion object {
        /** Boilerplate Subject.Factory for InputMethodServiceTraceSubject */
        private fun getFactory(
            trace: InputMethodServiceTrace?,
            parent: FlickerSubject?
        ): Factory<Subject, InputMethodServiceEntry> = Factory { fm, subject ->
            InputMethodServiceEntrySubject(fm, subject, trace, parent)
        }

        /**
         * Creates a [InputMethodServiceEntrySubject] to represent a InputMethodService
         * state[entry], which can be used to make assertions.
         *
         * @param entry InputMethodService trace entry
         * @param parent Trace that contains this entry (optional)
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            entry: InputMethodServiceEntry,
            trace: InputMethodServiceTrace? = null,
            parent: FlickerSubject? = null
        ): InputMethodServiceEntrySubject {
            val strategy = FlickerFailureStrategy()
            val subject =
                StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                    .about(getFactory(trace, parent))
                    .that(entry) as InputMethodServiceEntrySubject
            strategy.init(subject)
            return subject
        }

        /** Static method for getting the subject factory (for use with assertAbout()) */
        @JvmStatic
        @JvmOverloads
        fun entries(
            trace: InputMethodServiceTrace? = null,
            parent: FlickerSubject? = null
        ): Factory<Subject, InputMethodServiceEntry> = getFactory(trace, parent)
    }
}
