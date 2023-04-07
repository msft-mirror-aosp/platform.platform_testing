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

import com.android.server.common.inputmethod.ImeClientEntry
import com.android.server.common.inputmethod.ImeClientTrace
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
 * Truth subject for [ImeClientEntry] objects, used to make assertions over behaviors that occur on
 * a single ImeClientEntry state.
 *
 * To make assertions over a specific state from a trace it is recommended to create a subject using
 * [ImeClientTraceSubject.assertThat](myTrace) and select the specific state using:
 * [ImeClientTraceSubject.first] [ImeClientTraceSubject.last] [ImeClientTraceSubject.entry]
 *
 * Alternatively, it is also possible to use [ImeClientEntrySubject.assertThat](myState) or
 * Truth.assertAbout([ImeClientEntrySubject.getFactory]), however they will provide less debug
 * information because it uses Truth's default [FailureStrategy].
 *
 * Example: val trace = ImeClientTraceParser.parseFromTrace(myTraceFile) val subject =
 * ImeClientTraceSubject.assertThat(trace).first() ... .invoke { myCustomAssertion(this) }
 */
class ImeClientEntrySubject
private constructor(
    fm: FailureMetadata,
    val entry: ImeClientEntry,
    val trace: ImeClientTrace?,
    override val parent: FlickerSubject?
) : FlickerSubject(fm, entry), IImeClientSubject<ImeClientEntrySubject> {
    override val timestamp: Long
        get() = entry.timestamp
    override val selfFacts = listOf(Fact.fact("ImeClientEntry", entry))

    /** Executes a custom [assertion] on the current subject */
    operator fun invoke(assertion: Assertion<ImeClientEntry>): ImeClientEntrySubject = apply {
        assertion(this.entry)
    }

    /** {@inheritDoc} */
    override fun isEmpty(): ImeClientEntrySubject = apply {
        check("ImeClientEntry").that(entry).isNull()
    }

    /** {@inheritDoc} */
    override fun isNotEmpty(): ImeClientEntrySubject = apply {
        check("ImeClientEntry").that(entry).isNotNull()
    }

    override fun toString(): String {
        return "ImeClientEntrySubject($entry)"
    }

    companion object {
        /** Boilerplate Subject.Factory for ImeClientTraceSubject */
        private fun getFactory(
            trace: ImeClientTrace?,
            parent: FlickerSubject?
        ): Factory<Subject, ImeClientEntry> = Factory { fm, subject ->
            ImeClientEntrySubject(fm, subject, trace, parent)
        }

        /**
         * Creates a [ImeClientEntrySubject] to represent a ImeClient state[entry], which can be
         * used to make assertions.
         *
         * @param entry ImeClient trace entry
         * @param parent Trace that contains this entry (optional)
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            entry: ImeClientEntry,
            trace: ImeClientTrace? = null,
            parent: FlickerSubject? = null
        ): ImeClientEntrySubject {
            val strategy = FlickerFailureStrategy()
            val subject =
                StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                    .about(getFactory(trace, parent))
                    .that(entry) as ImeClientEntrySubject
            strategy.init(subject)
            return subject
        }

        /** Static method for getting the subject factory (for use with assertAbout()) */
        @JvmStatic
        @JvmOverloads
        fun entries(
            trace: ImeClientTrace? = null,
            parent: FlickerSubject? = null
        ): Factory<Subject, ImeClientEntry> = getFactory(trace, parent)
    }
}
