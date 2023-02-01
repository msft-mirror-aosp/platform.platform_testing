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

package com.android.server.wm.flicker.traces.eventlog

import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.helpers.format
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.events.EventLog
import com.android.server.wm.traces.common.events.FocusEvent
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

/** Truth subject for [FocusEvent] objects. */
class EventLogSubject
private constructor(failureMetadata: FailureMetadata, val eventLog: EventLog) :
    FlickerSubject(failureMetadata, eventLog) {

    override val timestamp: Timestamp
        get() = Timestamp.EMPTY
    override val parent: FlickerSubject?
        get() = null
    override val selfFacts by lazy {
        val firstTimestamp = eventLog.entries.firstOrNull()?.timestamp ?: Timestamp.EMPTY
        val lastTimestamp = eventLog.entries.lastOrNull()?.timestamp ?: Timestamp.EMPTY
        listOf(
            Fact.fact("Trace start", firstTimestamp.format()),
            Fact.fact("Trace end", lastTimestamp.format())
        )
    }

    private val subjects by lazy {
        eventLog.focusEvents.map { FocusEventSubject.assertThat(it, this) }
    }

    private val _focusChanges by lazy {
        val focusList = mutableListOf<String>()
        eventLog.focusEvents.firstOrNull { !it.hasFocus() }?.let { focusList.add(it.window) }
        focusList + eventLog.focusEvents.filter { it.hasFocus() }.map { it.window }
    }

    fun focusChanges(vararg windows: String) = apply {
        if (windows.isNotEmpty()) {
            val focusChanges =
                _focusChanges.dropWhile { !it.contains(windows.first()) }.take(windows.size)
            val success =
                windows.size <= focusChanges.size &&
                    focusChanges.zip(windows).all { (focus, search) -> focus.contains(search) }

            if (!success) {
                fail(
                    Fact.fact("Expected", windows.joinToString(",")),
                    Fact.fact("Found", focusChanges.joinToString(","))
                )
            }
        }
    }

    fun focusDoesNotChange() = apply { check("Focus changes").that(_focusChanges).isEmpty() }

    companion object {
        /** Boiler-plate Subject.Factory for EventLogSubject */
        private val FACTORY = Factory { fm: FailureMetadata, subject: EventLog ->
            EventLogSubject(fm, subject)
        }

        /** User-defined entry point */
        fun assertThat(logs: EventLog) = Truth.assertAbout(FACTORY).that(logs) as EventLogSubject

        /** Static method for getting the subject factory (for use with assertAbout()) */
        fun entries(): Factory<EventLogSubject, EventLog> {
            return FACTORY
        }
    }
}
