package com.android.server.wm.flicker.monitor

import android.util.EventLog
import com.android.server.wm.flicker.FlickerRunResult
import com.android.server.wm.flicker.traces.FocusEvent
import org.junit.Test

/**
 * Contains [EventLogMonitor] tests. To run this test: {@code
 * atest FlickerLibTest:EventLogMonitorTest}
 */
class EventLogMonitorTest {
    @Test
    fun canCaptureFocusEventLogs() {
        val monitor = EventLogMonitor()
        EventLog.writeEvent(INPUT_FOCUS_TAG /* input_focus */, "Focus leaving 183087b com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity (server)")
        EventLog.writeEvent(INPUT_FOCUS_TAG /* input_focus */, "Focus entering 4749f88 com.android.phone/com.android.phone.settings.fdn.FdnSetting (server)")
        monitor.start()
        EventLog.writeEvent(INPUT_FOCUS_TAG /* input_focus */, "Focus leaving 4749f88 com.android.phone/com.android.phone.settings.fdn.FdnSetting (server)")
        EventLog.writeEvent(INPUT_FOCUS_TAG /* input_focus */, "Focus entering 7c01447 com.android.phone/com.android.phone.settings.fdn.FdnSetting (server)")
        monitor.stop()
        EventLog.writeEvent(INPUT_FOCUS_TAG /* input_focus */, "Focus entering 2aa30cd com.android.phone/com.android.phone.settings.fdn.FdnSetting (server)")

        val result = FlickerRunResult.Builder(0)
        monitor.save("test", result)

        assert(result.eventLog.size == 2)
        assert(result.eventLog[0].window.equals("4749f88 com.android.phone/com.android.phone.settings.fdn.FdnSetting (server)"))
        assert(result.eventLog[0].focus == FocusEvent.Focus.LOST)
        assert(result.eventLog[1].window.equals("4749f88 com.android.phone/com.android.phone.settings.fdn.FdnSetting (server)"))
        assert(result.eventLog[1].focus == FocusEvent.Focus.GAINED)
        assert(result.eventLog[0].timestamp <= result.eventLog[1].timestamp)
    }

    private companion object {
        const val INPUT_FOCUS_TAG = 62001
    }
}