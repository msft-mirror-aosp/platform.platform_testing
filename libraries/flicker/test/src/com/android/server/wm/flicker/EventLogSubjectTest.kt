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
        EventLogSubject.assertThat(result).focusChanges("WinA", "WinB", "WinC")
        EventLogSubject.assertThat(result).focusChanges("WinA", "WinB")
        EventLogSubject.assertThat(result).focusChanges("WinB", "WinC")
        EventLogSubject.assertThat(result).focusChanges("WinA")
        EventLogSubject.assertThat(result).focusChanges("WinB")
        EventLogSubject.assertThat(result).focusChanges("WinC")
    }

    @Test
    fun canDetectFocusDoesNotChange() {
        val builder = FlickerRunResult.Builder(0)
        builder.eventLog = listOf()
        val result = builder.build()
        EventLogSubject.assertThat(result).focusDoesNotChange()
    }
}