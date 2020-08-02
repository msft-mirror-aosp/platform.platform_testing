package com.android.server.wm.flicker

import com.android.server.wm.flicker.traces.EventLogSubject
import com.android.server.wm.flicker.traces.FocusEvent
import org.junit.Test

class EventLogSubjectTest {
    @Test
    fun canDetectFocusChanges() {
        val builder = FlickerRunResult.Builder(0)
        builder.eventLog =
                listOf( FocusEvent(0, "WinB", FocusEvent.Focus.GAINED),
                        FocusEvent(0, "test WinA window", FocusEvent.Focus.LOST),
                        FocusEvent(0, "WinB", FocusEvent.Focus.LOST),
                        FocusEvent(0, "test WinC", FocusEvent.Focus.GAINED))
        val result = builder.build()
        EventLogSubject.assertThat(result).focusChanges(arrayOf("WinA", "WinB", "WinC")).forAllEntries()
        EventLogSubject.assertThat(result).focusChanges(arrayOf("WinA", "WinB")).forAllEntries()
        EventLogSubject.assertThat(result).focusChanges(arrayOf("WinB", "WinC")).forAllEntries()
        EventLogSubject.assertThat(result).focusChanges(arrayOf("WinA")).forAllEntries()
        EventLogSubject.assertThat(result).focusChanges(arrayOf("WinB")).forAllEntries()
        EventLogSubject.assertThat(result).focusChanges(arrayOf("WinC")).forAllEntries()
    }

    @Test
    fun canDetectFocusDoesNotChange() {
        val builder = FlickerRunResult.Builder(0)
        builder.eventLog = listOf()
        val result = builder.build()
        EventLogSubject.assertThat(result).focusDoesNotChange().forAllEntries()
    }
}