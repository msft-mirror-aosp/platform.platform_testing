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

package com.android.server.wm.flicker

import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTrace
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTrace.Companion.parseFrom
import com.android.server.wm.flicker.traces.windowmanager.WmTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WmTraceSubject.Companion.assertThat
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.lang.AssertionError

/**
 * Contains [WmTraceSubject] tests. To run this test: `atest
 * FlickerLibTest:WmTraceSubjectTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class WmTraceSubjectTest {
    @Test
    fun testVisibleAppWindowForRange() {
        val trace = readWmTraceFromFile("wm_trace_openchrome.pb")
        assertThat(trace)
                .showsAppWindowOnTop("NexusLauncherActivity")
                .and()
                .showsAboveAppWindow("ScreenDecorOverlay")
                .forRange(9213763541297L, 9215536878453L)
        assertThat(trace)
                .showsAppWindowOnTop("com.android.chrome")
                .and()
                .showsAppWindow("NexusLauncherActivity")
                .and()
                .showsAboveAppWindow("ScreenDecorOverlay")
                .then()
                .showsAppWindowOnTop("com.android.chrome")
                .and()
                .hidesAppWindow("NexusLauncherActivity")
                .and()
                .showsAboveAppWindow("ScreenDecorOverlay")
                .forRange(9215551505798L, 9216093628925L)
    }

    @Test
    fun testCanTransitionInAppWindow() {
        val trace = readWmTraceFromFile("wm_trace_openchrome.pb")
        assertThat(trace)
                .showsAppWindowOnTop("NexusLauncherActivity")
                .and()
                .showsAboveAppWindow("ScreenDecorOverlay")
                .then()
                .showsAppWindowOnTop("com.android.chrome")
                .and()
                .showsAboveAppWindow("ScreenDecorOverlay")
                .forAllEntries()
    }

    @Test
    fun testCanInspectBeginning() {
        val trace = readWmTraceFromFile("wm_trace_openchrome.pb")
        assertThat(trace)
                .showsAppWindowOnTop("NexusLauncherActivity")
                .and()
                .showsAboveAppWindow("ScreenDecorOverlay")
                .inTheBeginning()
    }

    @Test
    fun testCanInspectAppWindowOnTop() {
        val trace = readWmTraceFromFile("wm_trace_openchrome.pb")
        assertThat(trace)
                .showsAppWindowOnTop("NexusLauncherActivity", "InvalidWindow")
                .inTheBeginning()
        try {
            assertThat(trace)
                .showsAppWindowOnTop("AnotherInvalidWindow", "InvalidWindow")
                .inTheBeginning()
            Assert.fail("Could not detect the top app window")
        } catch (e: AssertionError) {
            Truth.assertWithMessage("Could not detect the top app window").that(e.message)
                    .contains("InvalidWindow cannot be found")
        }
    }

    @Test
    fun testCanInspectEnd() {
        val trace = readWmTraceFromFile("wm_trace_openchrome.pb")
        assertThat(trace)
                .showsAppWindowOnTop("com.android.chrome")
                .and()
                .showsAboveAppWindow("ScreenDecorOverlay")
                .atTheEnd()
    }

    @Test
    fun testCanTransitionNonAppWindow() {
        val trace = readWmTraceFromFile("wm_trace_ime.pb")
        assertThat(trace)
                .skipUntilFirstAssertion()
                .hidesNonAppWindow("InputMethod")
                .then()
                .showsNonAppWindow("InputMethod")
                .forAllEntries()
    }

    @Test(expected = AssertionError::class)
    fun testCanDetectOverlappingWindows() {
        val trace = readWmTraceFromFile("wm_trace_ime.pb")
        assertThat(trace)
                .noWindowsOverlap("InputMethod", "NavigationBar", "ImeActivity")
                .forAllEntries()
    }

    @Test
    fun testCanTransitionAboveAppWindow() {
        val trace = readWmTraceFromFile("wm_trace_ime.pb")
        assertThat(trace)
                .skipUntilFirstAssertion()
                .hidesAboveAppWindow("InputMethod")
                .then()
                .showsAboveAppWindow("InputMethod")
                .forAllEntries()
    }

    @Test
    fun testCanTransitionBelowAppWindow() {
        val trace = readWmTraceFromFile("wm_trace_open_app_cold.pb")
        assertThat(trace)
                .skipUntilFirstAssertion()
                .showsBelowAppWindow("Wallpaper")
                .then()
                .hidesBelowAppWindow("Wallpaper")
                .forAllEntries()
    }

    @Test
    fun testCanDetectVisibleWindowsMoreThanOneConsecutiveEntry() {
        val trace = readWmTraceFromFile("wm_trace_valid_visible_windows.pb")
        assertThat(trace).visibleWindowsShownMoreThanOneConsecutiveEntry().forAllEntries()
    }

    companion object {
        private fun readWmTraceFromFile(relativePath: String): WindowManagerTrace {
            return try {
                parseFrom(readTestFile(relativePath))
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
}
