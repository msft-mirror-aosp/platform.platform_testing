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

import com.android.server.wm.flicker.traces.FlickerSubjectException
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject.Companion.assertThat
import com.google.common.truth.Truth
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper.Companion.IME_COMPONENT
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper.Companion.NAV_BAR_COMPONENT
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper.Companion.SNAPSHOT_COMPONENT
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [WindowManagerTraceSubject] tests. To run this test: `atest
 * FlickerLibTest:WindowManagerTraceSubjectTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class WindowManagerTraceSubjectTest {
    private val chromeTrace by lazy { readWmTraceFromFile("wm_trace_openchrome.pb") }
    private val imeTrace by lazy { readWmTraceFromFile("wm_trace_ime.pb") }

    @Test
    fun testVisibleAppWindowForRange() {
        assertThat(chromeTrace)
            .isAppWindowOnTop(LAUNCHER_COMPONENT)
            .isAboveAppWindowVisible(SCREEN_DECOR_COMPONENT)
            .forRange(9213763541297L, 9215536878453L)

        assertThat(chromeTrace)
            .isAppWindowOnTop(CHROME_SPLASH_SCREEN_COMPONENT)
            .isAppWindowVisible(LAUNCHER_COMPONENT)
            .isAboveAppWindowVisible(SCREEN_DECOR_COMPONENT)
            .then()
            .isAppWindowOnTop(CHROME_SPLASH_SCREEN_COMPONENT)
            .isAppWindowInvisible(LAUNCHER_COMPONENT)
            .isAboveAppWindowVisible(SCREEN_DECOR_COMPONENT)
            .then()
            .isAppWindowOnTop(CHROME_COMPONENT)
            .isAppWindowInvisible(LAUNCHER_COMPONENT)
            .isAboveAppWindowVisible(SCREEN_DECOR_COMPONENT)
            .forRange(9215551505798L, 9216093628925L)
    }

    @Test
    fun testCanTransitionInAppWindow() {
        assertThat(chromeTrace)
            .isAppWindowOnTop(LAUNCHER_COMPONENT)
            .isAboveAppWindowVisible(SCREEN_DECOR_COMPONENT)
            .then()
            .isAppWindowOnTop(CHROME_SPLASH_SCREEN_COMPONENT)
            .isAboveAppWindowVisible(SCREEN_DECOR_COMPONENT)
            .then()
            .isAppWindowOnTop(CHROME_COMPONENT)
            .isAboveAppWindowVisible(SCREEN_DECOR_COMPONENT)
            .forAllEntries()
    }

    @Test
    fun testCanDetectTransitionWithOptionalValue() {
        val trace = readWmTraceFromFile("wm_trace_open_from_overview.pb")
        val subject = assertThat(trace)
        subject.isAppWindowOnTop(LAUNCHER_COMPONENT)
                .then()
                .isAppWindowOnTop(SNAPSHOT_COMPONENT)
                .then()
                .isAppWindowOnTop(CHROME_COMPONENT)
    }

    @Test
    fun testCanTransitionInAppWindow_withOptional() {
        assertThat(chromeTrace)
                .isAppWindowOnTop(LAUNCHER_COMPONENT)
                .isAboveAppWindowVisible(SCREEN_DECOR_COMPONENT)
                .then()
                .isAppWindowOnTop(CHROME_SPLASH_SCREEN_COMPONENT)
                .isAboveAppWindowVisible(SCREEN_DECOR_COMPONENT)
                .then()
                .isAppWindowOnTop(CHROME_COMPONENT)
                .isAboveAppWindowVisible(SCREEN_DECOR_COMPONENT)
                .forAllEntries()
    }

    @Test
    fun testCanInspectBeginning() {
        assertThat(chromeTrace)
            .first()
            .isAppWindowOnTop(LAUNCHER_COMPONENT)
            .isAboveAppWindow(SCREEN_DECOR_COMPONENT)
    }

    @Test
    fun testCanInspectAppWindowOnTop() {
        assertThat(chromeTrace)
            .first()
            .isAppWindowOnTop(LAUNCHER_COMPONENT)

        val failure = assertThrows(FlickerSubjectException::class.java) {
            assertThat(chromeTrace)
                .first()
                .isAppWindowOnTop(IMAGINARY_COMPONENT)
                .fail("Could not detect the top app window")
        }
        assertFailure(failure).hasMessageThat().contains("ImaginaryWindow")
    }

    @Test
    fun testCanInspectEnd() {
        assertThat(chromeTrace)
            .last()
            .isAppWindowOnTop(CHROME_COMPONENT)
            .isAboveAppWindow(SCREEN_DECOR_COMPONENT)
    }

    @Test
    fun testCanTransitionNonAppWindow() {
        assertThat(imeTrace)
            .skipUntilFirstAssertion()
            .isNonAppWindowInvisible(IME_COMPONENT)
            .then()
            .isNonAppWindowVisible(IME_COMPONENT)
            .forAllEntries()
    }

    @Test(expected = AssertionError::class)
    fun testCanDetectOverlappingWindows() {
        assertThat(imeTrace)
            .noWindowsOverlap(IME_COMPONENT, NAV_BAR_COMPONENT,
                    IME_ACTIVITY_COMPONENT)
            .forAllEntries()
    }

    @Test
    fun testCanTransitionAboveAppWindow() {
        assertThat(imeTrace)
            .skipUntilFirstAssertion()
            .isAboveAppWindowInvisible(IME_COMPONENT)
            .then()
            .isAboveAppWindowVisible(IME_COMPONENT)
            .forAllEntries()
    }

    @Test
    fun testCanTransitionBelowAppWindow() {
        val trace = readWmTraceFromFile("wm_trace_open_app_cold.pb")
        assertThat(trace)
            .skipUntilFirstAssertion()
            .isBelowAppWindowVisible(WALLPAPER_COMPONENT)
            .then()
            .isBelowAppWindowInvisible(WALLPAPER_COMPONENT)
            .forAllEntries()
    }

    @Test
    fun testCanDetectVisibleWindowsMoreThanOneConsecutiveEntry() {
        val trace = readWmTraceFromFile("wm_trace_valid_visible_windows.pb")
        assertThat(trace).visibleWindowsShownMoreThanOneConsecutiveEntry().forAllEntries()
    }

    @Test
    fun testCanAssertWindowStateSequence() {
        val windowStates = assertThat(chromeTrace).windowStates(
            "com.android.chrome/org.chromium.chrome.browser.firstrun.FirstRunActivity")
        val visibilityChange = windowStates.zipWithNext { current, next ->
            current.windowState?.isVisible != next.windowState?.isVisible
        }

        Truth.assertWithMessage("Visibility should have changed only 1x in the trace")
            .that(visibilityChange.count { it })
            .isEqualTo(1)
    }

    @Test
    fun exceptionContainsDebugInfo() {
        val error = assertThrows(AssertionError::class.java) {
            assertThat(chromeTrace).isEmpty()
        }
        Truth.assertThat(error).hasMessageThat().contains("Trace start")
        Truth.assertThat(error).hasMessageThat().contains("Trace start")
        Truth.assertThat(error).hasMessageThat().contains("Trace file")
    }
}
