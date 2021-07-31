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

import android.graphics.Region
import com.android.server.wm.flicker.traces.FlickerSubjectException
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject.Companion.assertThat
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.lang.AssertionError

/**
 * Contains [WindowManagerStateSubject] tests. To run this test: `atest
 * FlickerLibTest:WindowManagerStateSubjectTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class WindowManagerStateSubjectTest {
    private val trace: WindowManagerTrace by lazy { readWmTraceFromFile("wm_trace_openchrome.pb") }

    @Test
    fun exceptionContainsDebugInfo() {
        val error = assertThrows(AssertionError::class.java) {
            assertThat(trace).first().contains(IMAGINARY_COMPONENT)
        }
        Truth.assertThat(error).hasMessageThat().contains(IMAGINARY_COMPONENT.className)
        Truth.assertThat(error).hasMessageThat().contains("Trace start")
        Truth.assertThat(error).hasMessageThat().contains("Trace start")
        Truth.assertThat(error).hasMessageThat().contains("Trace file")
        Truth.assertThat(error).hasMessageThat().contains("Entry")
    }

    @Test
    fun canDetectAboveAppWindowVisibility_isVisible() {
        assertThat(trace)
            .entry(9213763541297)
            .isAboveAppWindow(WindowManagerStateHelper.NAV_BAR_COMPONENT)
            .isAboveAppWindow(SCREEN_DECOR_COMPONENT)
            .isAboveAppWindow(WindowManagerStateHelper.STATUS_BAR_COMPONENT)
    }

    @Test
    fun canDetectAboveAppWindowVisibility_isInvisible() {
        val subject = assertThat(trace).entry(9213763541297)
        var failure = assertThrows(AssertionError::class.java) {
            subject.isAboveAppWindow(PIP_DISMISS_COMPONENT)
        }
        assertFailure(failure).factValue("Is Invisible").contains("pip-dismiss-overlay")

        failure = assertThrows(AssertionError::class.java) {
            subject.isAboveAppWindow(WindowManagerStateHelper.NAV_BAR_COMPONENT, isVisible = false)
        }
        assertFailure(failure).factValue("Is Visible").contains("NavigationBar")
    }

    @Test
    fun canDetectWindowCoversAtLeastRegion_exactSize() {
        val entry = assertThat(trace)
            .entry(9213763541297)

        entry.frameRegion(WindowManagerStateHelper.STATUS_BAR_COMPONENT)
                .coversAtLeast(Region(0, 0, 1440, 171))
        entry.frameRegion(LAUNCHER_COMPONENT)
            .coversAtLeast(Region(0, 0, 1440, 2960))
    }

    @Test
    fun canDetectWindowCoversAtLeastRegion_smallerRegion() {
        val entry = assertThat(trace)
            .entry(9213763541297)
        entry.frameRegion(WindowManagerStateHelper.STATUS_BAR_COMPONENT)
                .coversAtLeast(Region(0, 0, 100, 100))
        entry.frameRegion(LAUNCHER_COMPONENT)
            .coversAtLeast(Region(0, 0, 100, 100))
    }

    @Test
    fun canDetectWindowCoversAtLeastRegion_largerRegion() {
        val subject = assertThat(trace).entry(9213763541297)
        var failure = assertThrows(FlickerSubjectException::class.java) {
            subject.frameRegion(WindowManagerStateHelper.STATUS_BAR_COMPONENT)
                    .coversAtLeast(Region(0, 0, 1441, 171))
        }
        assertFailure(failure).factValue("Uncovered region").contains("SkRegion((1440,0,1441,171))")

        failure = assertThrows(FlickerSubjectException::class.java) {
            subject.frameRegion(LAUNCHER_COMPONENT)
                .coversAtLeast(Region(0, 0, 1440, 2961))
        }
        assertFailure(failure).factValue("Uncovered region")
            .contains("SkRegion((0,2960,1440,2961))")
    }

    @Test
    fun canDetectWindowCoversAtMostRegion_extactSize() {
        val entry = assertThat(trace)
            .entry(9213763541297)
        entry.frameRegion(WindowManagerStateHelper.STATUS_BAR_COMPONENT)
                .coversAtMost(Region(0, 0, 1440, 171))
        entry.frameRegion(LAUNCHER_COMPONENT)
            .coversAtMost(Region(0, 0, 1440, 2960))
    }

    @Test
    fun canDetectWindowCoversAtMostRegion_smallerRegion() {
        val subject = assertThat(trace).entry(9213763541297)
        var failure = assertThrows(FlickerSubjectException::class.java) {
            subject.frameRegion(WindowManagerStateHelper.STATUS_BAR_COMPONENT)
                    .coversAtMost(Region(0, 0, 100, 100))
        }
        assertFailure(failure).factValue("Out-of-bounds region")
            .contains("SkRegion((100,0,1440,100)(0,100,1440,171))")

        failure = assertThrows(FlickerSubjectException::class.java) {
            subject.frameRegion(LAUNCHER_COMPONENT)
                .coversAtMost(Region(0, 0, 100, 100))
        }
        assertFailure(failure).factValue("Out-of-bounds region")
            .contains("SkRegion((100,0,1440,100)(0,100,1440,2960))")
    }

    @Test
    fun canDetectWindowCoversAtMostRegion_largerRegion() {
        val entry = assertThat(trace)
            .entry(9213763541297)

        entry.frameRegion(WindowManagerStateHelper.STATUS_BAR_COMPONENT)
                .coversAtMost(Region(0, 0, 1441, 171))
        entry.frameRegion(LAUNCHER_COMPONENT)
            .coversAtMost(Region(0, 0, 1440, 2961))
    }

    @Test
    fun canDetectBelowAppWindowVisibility() {
        assertThat(trace)
            .entry(9213763541297)
            .containsNonAppWindow(WALLPAPER_COMPONENT)
    }

    @Test
    fun canDetectAppWindowVisibility() {
        assertThat(trace)
            .entry(9213763541297)
            .containsAppWindow(LAUNCHER_COMPONENT)

        assertThat(trace)
            .entry(9215551505798)
            .containsAppWindow(CHROME_SPLASH_SCREEN_COMPONENT)
    }

    @Test
    fun canDetectAppWindowVisibilitySubject() {
        val trace = readWmTraceFromFile("wm_trace_launcher_visible_background.pb")
        val firstEntry = assertThat(trace).first()
        val appWindowNames = firstEntry.wmState.appWindows.map { it.name }
        firstEntry.verify("has1AppWindow").that(appWindowNames).hasSize(3)
        firstEntry.verify("has1AppWindow").that(appWindowNames)
                .contains("com.android.server.wm.flicker.testapp/" +
                        "com.android.server.wm.flicker.testapp.SimpleActivity")
    }

    @Test
    fun canDetectLauncherVisibility() {
        val trace = readWmTraceFromFile("wm_trace_launcher_visible_background.pb")
        val subject = assertThat(trace)
        val firstTrace = subject.first()
        firstTrace.isInvisible(LAUNCHER_COMPONENT)

        val lastTrace = subject.last()
        lastTrace.isVisible(LAUNCHER_COMPONENT)

        subject.isAppWindowInvisible(LAUNCHER_COMPONENT)
                .then()
                .isAppWindowVisible(LAUNCHER_COMPONENT)
                .forAllEntries()
    }

    @Test
    fun canFailWithReasonForVisibilityChecks_windowNotFound() {
        val failure = assertThrows(FlickerSubjectException::class.java) {
            assertThat(trace)
                .entry(9213763541297)
                .containsNonAppWindow(IMAGINARY_COMPONENT)
        }
        assertFailure(failure).hasMessageThat()
            .contains(IMAGINARY_COMPONENT.packageName)
    }

    @Test
    fun canFailWithReasonForVisibilityChecks_windowNotVisible() {
        val failure = assertThrows(FlickerSubjectException::class.java) {
            assertThat(trace)
                .entry(9213763541297)
                .containsNonAppWindow(WindowManagerStateHelper.IME_COMPONENT)
        }
        assertFailure(failure).factValue("Is Invisible")
            .contains(WindowManagerStateHelper.IME_COMPONENT.packageName)
    }

    @Test
    fun canDetectAppZOrder() {
        assertThat(trace)
            .entry(9215551505798)
            .containsAppWindow(LAUNCHER_COMPONENT, isVisible = true)
            .isAppWindowOnTop(CHROME_SPLASH_SCREEN_COMPONENT)
    }

    @Test
    fun canFailWithReasonForZOrderChecks_windowNotOnTop() {
        val failure = assertThrows(FlickerSubjectException::class.java) {
            assertThat(trace)
                .entry(9215551505798)
                .isAppWindowOnTop(LAUNCHER_COMPONENT)
        }
        assertFailure(failure)
            .factValue("Found")
            .contains(CHROME_SPLASH_SCREEN_COMPONENT.packageName)
    }

    @Test
    fun canDetectActivityVisibility() {
        val trace = readWmTraceFromFile("wm_trace_split_screen.pb")
        val lastEntry = assertThat(trace).last()
        lastEntry.isVisible(SHELL_SPLIT_SCREEN_PRIMARY_COMPONENT)
        lastEntry.isVisible(SHELL_SPLIT_SCREEN_SECONDARY_COMPONENT)
    }
}