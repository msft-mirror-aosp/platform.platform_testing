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

package com.android.server.wm.flicker.traces.eventlog

import com.android.server.wm.flicker.FlickerRunResult
import com.android.server.wm.flicker.assertions.AssertionsChecker
import com.android.server.wm.traces.common.AssertionResult
import com.android.server.wm.traces.common.IRangedSubject
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

/** Truth subject for [FocusEvent] objects.  */
class EventLogSubject private constructor(
    failureMetadata: FailureMetadata,
    private val subject: List<FocusEvent>
) : Subject(failureMetadata, subject),
    IRangedSubject<FocusEvent> {
    private val assertionsChecker = AssertionsChecker<FocusEvent>()
    private val _focusChanges by lazy {
        val focusList = mutableListOf<String>()
        subject.firstOrNull { !it.hasFocus() }?.let { focusList.add(it.window) }
        focusList + subject.filter { it.hasFocus() }.map { it.window }
    }

    fun focusChanges(windows: Array<out String>) = apply {
        assertionsChecker.addList { Companion.focusChanges(windows, _focusChanges) }
    }

    fun focusDoesNotChange() = apply {
        assertionsChecker.addList { AssertionResult("focusDoesNotChange", it.isEmpty()) }
    }

    companion object {
        /**
         * Boiler-plate Subject.Factory for EventLogSubject
         */
        private val FACTORY = Factory { fm: FailureMetadata, subject: List<FocusEvent> ->
            EventLogSubject(fm, subject)
        }

        /**
         * User-defined entry point
         */
        fun assertThat(entry: List<FocusEvent>) =
                Truth.assertAbout(FACTORY).that(entry) as EventLogSubject

        fun assertThat(result: FlickerRunResult) = assertThat(result.eventLog)

        /**
         * Static method for getting the subject factory (for use with assertAbout())
         */
        fun entries(): Factory<EventLogSubject, List<FocusEvent>> {
            return FACTORY
        }

        private fun focusChanges(
            windows: Array<out String>,
            _focusChanges: List<String>
        ): AssertionResult {
            if (windows.isEmpty()) {
                return AssertionResult(assertionName = "focusChanges", success = true)
            }
            val focusChanges = _focusChanges.dropWhile { !it.contains(windows[0]) }
                    .take(windows.size)
            val success = windows.size <= focusChanges.size &&
                    focusChanges.zip(windows).all { (focus, search) -> focus.contains(search) }

            return AssertionResult(
                    reason = if (success) "" else "Expected ${windows.joinToString(",")}\n",
                    assertionName = "focusChanges",
                    timestamp = 0,
                    success = success
            )
        }
    }

    override fun forAllEntries() {
        assertionsChecker.checkChangingAssertions()
        test()
    }

    /**
     * Run the assertions.
     */
    private fun test() {
        val failures = assertionsChecker.test(subject)
        if (failures.isNotEmpty()) {
            failWithActual(failures.joinToString("\n") { it.toString() }, subject)
        }
    }
}
