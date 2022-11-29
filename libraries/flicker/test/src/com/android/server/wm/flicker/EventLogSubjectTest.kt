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

package com.android.server.wm.flicker

import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import com.android.server.wm.traces.common.Timestamp
import com.google.common.truth.Truth
import org.junit.Test

/**
 * Contains [EventLogSubject] tests. To run this test: `atest FlickerLibTest:EventLogSubjectTest`
 */
class EventLogSubjectTest {
    @Test
    fun canDetectFocusChanges() {
        val runResult = FlickerRunResult("testName")
        runResult.transitionStartTime = Timestamp.MIN
        runResult.transitionEndTime = Timestamp.MAX
        runResult.eventLog =
            listOf(
                FocusEvent(Timestamp(unixNanos = 0), "WinB", FocusEvent.Focus.GAINED, "test"),
                FocusEvent(
                    Timestamp(unixNanos = 0),
                    "test WinA window",
                    FocusEvent.Focus.LOST,
                    "test"
                ),
                FocusEvent(Timestamp(unixNanos = 0), "WinB", FocusEvent.Focus.LOST, "test"),
                FocusEvent(Timestamp(unixNanos = 0), "test WinC", FocusEvent.Focus.GAINED, "test")
            )
        val result = runResult.eventLogSubject
        requireNotNull(result) { "Event log subject was not built" }
        result.focusChanges("WinA", "WinB", "WinC").forAllEntries()
        result.focusChanges("WinA", "WinB").forAllEntries()
        result.focusChanges("WinB", "WinC").forAllEntries()
        result.focusChanges("WinA").forAllEntries()
        result.focusChanges("WinB").forAllEntries()
        result.focusChanges("WinC").forAllEntries()
    }

    @Test
    fun canDetectFocusDoesNotChange() {
        val runResult = FlickerRunResult("testName")
        runResult.transitionStartTime = Timestamp.MIN
        runResult.transitionEndTime = Timestamp.MAX
        runResult.eventLog = emptyList()
        val result = runResult.eventLogSubject
        requireNotNull(result) { "Event log subject was not built" }
        result.focusDoesNotChange().forAllEntries()
    }

    @Test
    fun canExcludeSetupAndTeardownChanges() {
        val runResult = FlickerRunResult("testName")
        runResult.transitionStartTime = Timestamp(5, 5, 5)
        runResult.transitionEndTime = Timestamp(10, 10, 10)
        runResult.eventLog =
            listOf(
                FocusEvent(Timestamp(unixNanos = 0), "WinB", FocusEvent.Focus.GAINED, "test"),
                FocusEvent(
                    Timestamp(unixNanos = 5),
                    "test WinA window",
                    FocusEvent.Focus.LOST,
                    "test"
                ),
                FocusEvent(Timestamp(unixNanos = 6), "WinB", FocusEvent.Focus.LOST, "test"),
                FocusEvent(Timestamp(unixNanos = 10), "test WinC", FocusEvent.Focus.GAINED, "test"),
                FocusEvent(Timestamp(unixNanos = 12), "test WinD", FocusEvent.Focus.GAINED, "test")
            )
        val result = runResult.eventLogSubject
        requireNotNull(result) { "Event log subject was not built" }
        Truth.assertThat(result.trace).hasSize(3)
    }
}
