/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.tools.flicker.subject.protolog

import android.tools.Timestamps
import android.tools.flicker.subject.FlickerSubject
import android.tools.flicker.subject.events.FocusEventSubject
import android.tools.flicker.subject.exceptions.ExceptionMessageBuilder
import android.tools.io.Reader
import android.tools.traces.events.FocusEvent
import android.tools.traces.protolog.ProtoLogTrace

/** Truth subject for [ProtoLogTrace] objects. */
class ProtoLogSubject(val protolog: ProtoLogTrace, override val reader: Reader) :
    FlickerSubject(), FocusEventSubject {

    override val timestamp = protolog.entries.firstOrNull()?.timestamp ?: Timestamps.empty()

    override val focusChanges by lazy {
        val regex = Regex("^Focus (entering|leaving) (.+) reason=(.*)$")
        protolog.entries
            .filter { it.tag == "INPUT_FOCUS" && it.message.matches(regex) }
            .map {
                regex.find(it.message)?.let { res ->
                    val (_, action, window, reason) = res.groupValues
                    val type =
                        when (action) {
                            "entering" -> FocusEvent.Type.GAINED
                            "leaving" -> FocusEvent.Type.LOST
                            else -> error("Unexpected focus action: $action")
                        }
                    FocusEvent(it.timestamp, window, type, reason, 0, "", 0)
                } ?: error("Unexpected focus message: ${it.message}")
            }
    }

    override val exceptionMessageBuilder
        get() = ExceptionMessageBuilder().forSubject(this)
}
