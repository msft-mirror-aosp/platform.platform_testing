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

import com.android.server.wm.flicker.assertions.SubjectsParser
import com.android.server.wm.flicker.datastore.CachedResultReader
import com.android.server.wm.flicker.io.ResultReader
import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import com.android.server.wm.traces.common.Timestamp
import java.nio.file.Files
import org.junit.Test

/**
 * Contains [EventLogSubject] tests. To run this test: `atest FlickerLibTest:EventLogSubjectTest`
 */
class EventLogSubjectTest {
    @Test
    fun canDetectFocusChanges() {
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
        val writer =
            newTestCachedResultWriter()
                .addEventLogResult(
                    listOf(
                        FocusEvent(
                            Timestamp(unixNanos = 0),
                            "WinB",
                            FocusEvent.Focus.GAINED,
                            "test"
                        ),
                        FocusEvent(
                            Timestamp(unixNanos = 0),
                            "test WinA window",
                            FocusEvent.Focus.LOST,
                            "test"
                        ),
                        FocusEvent(Timestamp(unixNanos = 0), "WinB", FocusEvent.Focus.LOST, "test"),
                        FocusEvent(
                            Timestamp(unixNanos = 0),
                            "test WinC",
                            FocusEvent.Focus.GAINED,
                            "test"
                        )
                    )
                )
        writer.write()
        val subjectsParser = SubjectsParser(CachedResultReader(TEST_SCENARIO, DEFAULT_TRACE_CONFIG))

        val subject = subjectsParser.eventLogSubject ?: error("Event log subject not built")
        subject.focusChanges("WinA", "WinB", "WinC").forAllEntries()
        subject.focusChanges("WinA", "WinB").forAllEntries()
        subject.focusChanges("WinB", "WinC").forAllEntries()
        subject.focusChanges("WinA").forAllEntries()
        subject.focusChanges("WinB").forAllEntries()
        subject.focusChanges("WinC").forAllEntries()
    }

    @Test
    fun canDetectFocusDoesNotChange() {
        Files.deleteIfExists(outputFileName(RunStatus.RUN_EXECUTED))
        val writer = newTestResultWriter().addEventLogResult(emptyList())
        val result = writer.write()

        val subjectsParser = SubjectsParser(ResultReader(result, DEFAULT_TRACE_CONFIG))

        val subject = subjectsParser.eventLogSubject ?: error("Event log subject not built")
        subject.focusDoesNotChange().forAllEntries()
    }
}
