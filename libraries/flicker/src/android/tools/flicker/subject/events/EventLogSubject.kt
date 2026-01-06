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

package android.tools.flicker.subject.events

import android.tools.Timestamps
import android.tools.flicker.subject.FlickerSubject
import android.tools.flicker.subject.exceptions.ExceptionMessageBuilder
import android.tools.io.Reader
import android.tools.traces.events.EventLog
import android.tools.traces.events.FocusEvent

/** Truth subject for [FocusEvent] objects. */
class EventLogSubject(val eventLog: EventLog, override val reader: Reader) :
    FlickerSubject(), FocusEventSubject {

    init {
        if (android.tracing.Flags.nativeProtoLogging()) {
            error(
                "EventLogSubject should no longer be used with native protolog support." +
                    "Instead use the ProtoLogSubject, since all events supported by " +
                    "eventlog are supported by protolog."
            )
        }
    }

    override val timestamp = eventLog.entries.firstOrNull()?.timestamp ?: Timestamps.empty()

    override val focusChanges by lazy {
        val focusList = mutableListOf<FocusEvent>()
        eventLog.focusEvents.firstOrNull { !it.hasFocus() }?.let { focusList.add(it) }
        focusList + eventLog.focusEvents.filter { it.hasFocus() }
    }

    override val exceptionMessageBuilder
        get() = ExceptionMessageBuilder().forSubject(this)
}
