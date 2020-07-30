/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.flicker.traces

import com.android.server.wm.flicker.FlickerRunResult
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import java.util.*

/** Truth subject for [FocusEvent] objects.  */
class EventLogSubject private constructor(
    failureMetadata: FailureMetadata,
    subject: List<FocusEvent>
) : Subject<EventLogSubject, List<FocusEvent>>(failureMetadata, subject) {

    private val _focusChangeList by lazy {
        val focusList = mutableListOf<String>()
        actual().firstOrNull{ !it.hasFocus() }?.let { focusList.add(it.window) }
        focusList + actual().filter { it.hasFocus() }.map { it.window }
    }

    fun focusChanges(vararg windows: String) {
        if (windows.isEmpty()) {
            return
        }
        val focusChanges = _focusChangeList.dropWhile { !it.contains(windows[0])}.take(windows.size)
        val success = windows.size <= focusChanges.size &&
                focusChanges.zip(windows).all { (focus, search) -> focus.contains(search) }

        if (!success) {
            val failureLogs = "Expected focus to change: ${windows.joinToString(",")}\n"
            val focusEventLogs = "\nActual:\n" + actual().joinToString("\n")
            fail(failureLogs + focusEventLogs + _focusChangeList)
        }
    }

    fun focusDoesNotChange() {
        val success = _focusChangeList.isEmpty()
        if (!success) {
            val failureLogs = "Expected no focus changes\n"
            val focusEventLogs = "\nActual:\n" + actual().joinToString("\n")
            fail(failureLogs + focusEventLogs)
        }
    }

    companion object {
        // Boiler-plate Subject.Factory for LayersTraceSubject
        private val FACTORY = Factory { fm: FailureMetadata, subject: List<FocusEvent> ->
            EventLogSubject(fm, subject)
        }

        // User-defined entry point
        fun assertThat(entry: List<FocusEvent>): EventLogSubject {
            return Truth.assertAbout(FACTORY).that(entry)
        }

        fun assertThat(result: FlickerRunResult): EventLogSubject {
            return Truth.assertWithMessage(result.toString()).about(FACTORY).that(result.eventLog)
        }

        // Static method for getting the subject factory (for use with assertAbout())
        fun entries(): Factory<EventLogSubject, List<FocusEvent>> {
            return FACTORY
        }
    }
}