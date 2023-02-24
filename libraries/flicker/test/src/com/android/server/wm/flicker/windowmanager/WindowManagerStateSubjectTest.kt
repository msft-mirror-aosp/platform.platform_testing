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

package com.android.server.wm.flicker.windowmanager

import com.android.server.wm.flicker.TestComponents
import com.android.server.wm.flicker.assertFailureFact
import com.android.server.wm.flicker.assertThatErrorContainsDebugInfo
import com.android.server.wm.flicker.assertThrows
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.readWmTraceFromDumpFile
import com.android.server.wm.flicker.readWmTraceFromFile
import com.android.server.wm.flicker.traces.FlickerSubjectException
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.Cache
import com.android.server.wm.traces.common.component.matchers.ComponentNameMatcher
import com.android.server.wm.traces.common.region.Region
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.windows.ConfigurationContainer
import com.android.server.wm.traces.common.windowmanager.windows.KeyguardControllerState
import com.android.server.wm.traces.common.windowmanager.windows.RootWindowContainer
import com.android.server.wm.traces.common.windowmanager.windows.WindowContainer
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [WindowManagerStateSubject] tests. To run this test: `atest
 * FlickerLibTest:WindowManagerStateSubjectTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class WindowManagerStateSubjectTest {
    private val trace
        get() = readWmTraceFromFile("wm_trace_openchrome.pb", legacyTrace = true)

    // Launcher is visible in fullscreen in the first frame of the trace
    private val traceFirstFrameTimestamp = 9213763541297

    // The first frame where the chrome splash screen is shown
    private val traceFirstChromeFlashScreenTimestamp = 9215551505798

    // The bounds of the display used to generate the trace [trace]
    private val displayBounds = Region.from(0, 0, 1440, 2960)

    // The region covered by the status bar in the trace
    private val statusBarRegion = Region.from(0, 0, 1440, 171)

    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun exceptionContainsDebugInfo() {
        val error =
            assertThrows<FlickerSubjectException> {
                WindowManagerTraceSubject(trace).first().visibleRegion(TestComponents.IMAGINARY)
            }
        assertThatErrorContainsDebugInfo(error)
        Truth.assertThat(error).hasMessageThat().contains(TestComponents.IMAGINARY.className)
        Truth.assertThat(error).hasMessageThat().contains(FlickerSubject.ASSERTION_TAG)
    }

    @Test
    fun canDetectAboveAppWindowVisibility_isVisible() {
        WindowManagerTraceSubject(trace)
            .getEntryByElapsedTimestamp(traceFirstFrameTimestamp)
            .containsAboveAppWindow(ComponentNameMatcher.NAV_BAR)
            .containsAboveAppWindow(TestComponents.SCREEN_DECOR_OVERLAY)
            .containsAboveAppWindow(ComponentNameMatcher.STATUS_BAR)
    }

    @Test
    fun canDetectAboveAppWindowVisibility_isInvisible() {
        val subject =
            WindowManagerTraceSubject(trace).getEntryByElapsedTimestamp(traceFirstFrameTimestamp)
        var failure =
            assertThrows<FlickerSubjectException> {
                subject
                    .containsAboveAppWindow(TestComponents.PIP_OVERLAY)
                    .isNonAppWindowVisible(TestComponents.PIP_OVERLAY)
            }
        assertFailureFact(failure, "Is Invisible").contains("pip-dismiss-overlay")

        failure =
            assertThrows<FlickerSubjectException> {
                subject
                    .containsAboveAppWindow(ComponentNameMatcher.NAV_BAR)
                    .isNonAppWindowInvisible(ComponentNameMatcher.NAV_BAR)
            }
        assertFailureFact(failure, "Is Visible").contains("NavigationBar")
    }

    @Test
    fun canDetectWindowCoversAtLeastRegion_exactSize() {
        val entry =
            WindowManagerTraceSubject(trace).getEntryByElapsedTimestamp(traceFirstFrameTimestamp)

        entry.visibleRegion(ComponentNameMatcher.STATUS_BAR).coversAtLeast(statusBarRegion)
        entry.visibleRegion(TestComponents.LAUNCHER).coversAtLeast(displayBounds)
    }

    @Test
    fun canDetectWindowCoversAtLeastRegion_smallerRegion() {
        val entry =
            WindowManagerTraceSubject(trace).getEntryByElapsedTimestamp(traceFirstFrameTimestamp)
        entry
            .visibleRegion(ComponentNameMatcher.STATUS_BAR)
            .coversAtLeast(Region.from(0, 0, 100, 100))
        entry.visibleRegion(TestComponents.LAUNCHER).coversAtLeast(Region.from(0, 0, 100, 100))
    }

    @Test
    fun canDetectWindowCoversAtLeastRegion_largerRegion() {
        val subject =
            WindowManagerTraceSubject(trace).getEntryByElapsedTimestamp(traceFirstFrameTimestamp)
        var failure =
            assertThrows<FlickerSubjectException> {
                subject
                    .visibleRegion(ComponentNameMatcher.STATUS_BAR)
                    .coversAtLeast(Region.from(0, 0, 1441, 171))
            }
        assertFailureFact(failure, "Uncovered region").contains("SkRegion((1440,0,1441,171))")

        failure =
            assertThrows<FlickerSubjectException> {
                subject
                    .visibleRegion(TestComponents.LAUNCHER)
                    .coversAtLeast(Region.from(0, 0, 1440, 2961))
            }
        assertFailureFact(failure, "Uncovered region").contains("SkRegion((0,2960,1440,2961))")
    }

    @Test
    fun canDetectWindowCoversExactlyRegion_exactSize() {
        val entry =
            WindowManagerTraceSubject(trace).getEntryByElapsedTimestamp(traceFirstFrameTimestamp)

        entry.visibleRegion(ComponentNameMatcher.STATUS_BAR).coversExactly(statusBarRegion)
        entry.visibleRegion(TestComponents.LAUNCHER).coversExactly(displayBounds)
    }

    @Test
    fun canDetectWindowCoversExactlyRegion_smallerRegion() {
        val subject =
            WindowManagerTraceSubject(trace).getEntryByElapsedTimestamp(traceFirstFrameTimestamp)
        var failure =
            assertThrows<FlickerSubjectException> {
                subject
                    .visibleRegion(ComponentNameMatcher.STATUS_BAR)
                    .coversAtMost(Region.from(0, 0, 100, 100))
            }
        assertFailureFact(failure, "Out-of-bounds region")
            .contains("SkRegion((100,0,1440,100)(0,100,1440,171))")

        failure =
            assertThrows<FlickerSubjectException> {
                subject
                    .visibleRegion(TestComponents.LAUNCHER)
                    .coversAtMost(Region.from(0, 0, 100, 100))
            }
        assertFailureFact(failure, "Out-of-bounds region")
            .contains("SkRegion((100,0,1440,100)(0,100,1440,2960))")
    }

    @Test
    fun canDetectWindowCoversExactlyRegion_largerRegion() {
        val subject =
            WindowManagerTraceSubject(trace).getEntryByElapsedTimestamp(traceFirstFrameTimestamp)
        var failure =
            assertThrows<FlickerSubjectException> {
                subject
                    .visibleRegion(ComponentNameMatcher.STATUS_BAR)
                    .coversAtLeast(Region.from(0, 0, 1441, 171))
            }
        assertFailureFact(failure, "Uncovered region").contains("SkRegion((1440,0,1441,171))")

        failure =
            assertThrows<FlickerSubjectException> {
                subject
                    .visibleRegion(TestComponents.LAUNCHER)
                    .coversAtLeast(Region.from(0, 0, 1440, 2961))
            }
        assertFailureFact(failure, "Uncovered region").contains("SkRegion((0,2960,1440,2961))")
    }

    @Test
    fun canDetectWindowCoversAtMostRegion_extactSize() {
        val entry =
            WindowManagerTraceSubject(trace).getEntryByElapsedTimestamp(traceFirstFrameTimestamp)
        entry.visibleRegion(ComponentNameMatcher.STATUS_BAR).coversAtMost(statusBarRegion)
        entry.visibleRegion(TestComponents.LAUNCHER).coversAtMost(displayBounds)
    }

    @Test
    fun canDetectWindowCoversAtMostRegion_smallerRegion() {
        val subject =
            WindowManagerTraceSubject(trace).getEntryByElapsedTimestamp(traceFirstFrameTimestamp)
        var failure =
            assertThrows<FlickerSubjectException> {
                subject
                    .visibleRegion(ComponentNameMatcher.STATUS_BAR)
                    .coversAtMost(Region.from(0, 0, 100, 100))
            }
        assertFailureFact(failure, "Out-of-bounds region")
            .contains("SkRegion((100,0,1440,100)(0,100,1440,171))")

        failure =
            assertThrows<FlickerSubjectException> {
                subject
                    .visibleRegion(TestComponents.LAUNCHER)
                    .coversAtMost(Region.from(0, 0, 100, 100))
            }
        assertFailureFact(failure, "Out-of-bounds region")
            .contains("SkRegion((100,0,1440,100)(0,100,1440,2960))")
    }

    @Test
    fun canDetectWindowCoversAtMostRegion_largerRegion() {
        val entry =
            WindowManagerTraceSubject(trace).getEntryByElapsedTimestamp(traceFirstFrameTimestamp)

        entry
            .visibleRegion(ComponentNameMatcher.STATUS_BAR)
            .coversAtMost(Region.from(0, 0, 1441, 171))
        entry.visibleRegion(TestComponents.LAUNCHER).coversAtMost(Region.from(0, 0, 1440, 2961))
    }

    @Test
    fun canDetectBelowAppWindowVisibility() {
        WindowManagerTraceSubject(trace)
            .getEntryByElapsedTimestamp(traceFirstFrameTimestamp)
            .containsNonAppWindow(TestComponents.WALLPAPER)
    }

    @Test
    fun canDetectAppWindowVisibility() {
        WindowManagerTraceSubject(trace)
            .getEntryByElapsedTimestamp(traceFirstFrameTimestamp)
            .containsAppWindow(TestComponents.LAUNCHER)

        WindowManagerTraceSubject(trace)
            .getEntryByElapsedTimestamp(traceFirstChromeFlashScreenTimestamp)
            .containsAppWindow(TestComponents.CHROME_SPLASH_SCREEN)
    }

    @Test
    fun canDetectAppWindowVisibilitySubject() {
        val trace =
            readWmTraceFromFile("wm_trace_launcher_visible_background.pb", legacyTrace = true)
        val firstEntry = WindowManagerTraceSubject(trace).first()
        val appWindowNames = firstEntry.wmState.appWindows.map { it.name }
        val expectedAppWindowName =
            "com.android.server.wm.flicker.testapp/" +
                "com.android.server.wm.flicker.testapp.SimpleActivity"
        firstEntry.check { "has1AppWindow" }.that(appWindowNames.size).isEqual(3)
        firstEntry
            .check { "App window names contain $expectedAppWindowName" }
            .that(appWindowNames)
            .contains(expectedAppWindowName)
    }

    @Test
    fun canDetectLauncherVisibility() {
        val trace =
            readWmTraceFromFile("wm_trace_launcher_visible_background.pb", legacyTrace = true)
        val subject = WindowManagerTraceSubject(trace)
        val firstTrace = subject.first()
        firstTrace.isAppWindowInvisible(TestComponents.LAUNCHER)

        // in the trace there are 2 launcher windows, a visible (usually the main launcher) and
        // an invisible one (the -1 window, for the swipe back on home screen action.
        // in flicker, the launcher is considered visible is any of them is visible
        subject.last().isAppWindowVisible(TestComponents.LAUNCHER)

        subject
            .isAppWindowNotOnTop(TestComponents.LAUNCHER)
            .isAppWindowInvisible(TestComponents.LAUNCHER)
            .then()
            .isAppWindowOnTop(TestComponents.LAUNCHER)
            .forAllEntries()
    }

    @Test
    fun canFailWithReasonForVisibilityChecks_windowNotFound() {
        val failure =
            assertThrows<FlickerSubjectException> {
                WindowManagerTraceSubject(trace)
                    .getEntryByElapsedTimestamp(traceFirstFrameTimestamp)
                    .containsNonAppWindow(TestComponents.IMAGINARY)
            }
        Truth.assertThat(failure).hasMessageThat().contains(TestComponents.IMAGINARY.packageName)
    }

    @Test
    fun canFailWithReasonForVisibilityChecks_windowNotVisible() {
        val failure =
            assertThrows<FlickerSubjectException> {
                WindowManagerTraceSubject(trace)
                    .getEntryByElapsedTimestamp(traceFirstFrameTimestamp)
                    .containsNonAppWindow(ComponentNameMatcher.IME)
                    .isNonAppWindowVisible(ComponentNameMatcher.IME)
            }
        assertFailureFact(failure, "Is Invisible").contains(ComponentNameMatcher.IME.packageName)
    }

    @Test
    fun canDetectAppZOrder() {
        WindowManagerTraceSubject(trace)
            .getEntryByElapsedTimestamp(traceFirstChromeFlashScreenTimestamp)
            .containsAppWindow(TestComponents.LAUNCHER)
            .isAppWindowVisible(TestComponents.LAUNCHER)
            .isAboveWindow(TestComponents.CHROME_SPLASH_SCREEN, TestComponents.LAUNCHER)
            .isAppWindowOnTop(TestComponents.LAUNCHER)
    }

    @Test
    fun canFailWithReasonForZOrderChecks_windowNotOnTop() {
        val failure =
            assertThrows<FlickerSubjectException> {
                WindowManagerTraceSubject(trace)
                    .getEntryByElapsedTimestamp(traceFirstChromeFlashScreenTimestamp)
                    .isAppWindowOnTop(TestComponents.CHROME_SPLASH_SCREEN)
            }
        assertFailureFact(failure, "Found").contains(TestComponents.LAUNCHER.packageName)
    }

    @Test
    fun canDetectActivityVisibility() {
        val trace = readWmTraceFromFile("wm_trace_split_screen.pb", legacyTrace = true)
        val lastEntry = WindowManagerTraceSubject(trace).last()
        lastEntry.isAppWindowVisible(TestComponents.SHELL_SPLIT_SCREEN_PRIMARY)
        lastEntry.isAppWindowVisible(TestComponents.SHELL_SPLIT_SCREEN_SECONDARY)
    }

    @Test
    fun canHandleNoSubjects() {
        val emptyRootContainer =
            RootWindowContainer(
                WindowContainer(
                    title = "root",
                    token = "",
                    orientation = 0,
                    layerId = 0,
                    _isVisible = true,
                    children = emptyArray(),
                    configurationContainer = ConfigurationContainer(null, null, null),
                    computedZ = 0
                )
            )
        val noWindowsState =
            WindowManagerState(
                elapsedTimestamp = 0,
                clockTimestamp = null,
                where = "",
                policy = null,
                focusedApp = "",
                focusedDisplayId = 0,
                _focusedWindow = "",
                inputMethodWindowAppToken = "",
                isHomeRecentsComponent = false,
                isDisplayFrozen = false,
                _pendingActivities = emptyArray(),
                root = emptyRootContainer,
                keyguardControllerState =
                    KeyguardControllerState.from(
                        isAodShowing = false,
                        isKeyguardShowing = false,
                        keyguardOccludedStates = mapOf()
                    )
            )

        val mockComponent = ComponentNameMatcher("", "Mock")

        val failure =
            assertThrows<FlickerSubjectException> {
                WindowManagerStateSubject(noWindowsState).isAppWindowOnTop(mockComponent)
            }
        Truth.assertThat(failure).hasMessageThat().contains("No visible app windows found")
    }

    @Test
    fun canDetectNoVisibleAppWindows() {
        val trace = readWmTraceFromFile("wm_trace_unlock.pb", legacyTrace = true)
        val firstEntry = WindowManagerTraceSubject(trace).first()
        firstEntry.hasNoVisibleAppWindow()
    }

    @Test
    fun canDetectHasVisibleAppWindows() {
        val trace = readWmTraceFromFile("wm_trace_unlock.pb", legacyTrace = true)
        val lastEntry = WindowManagerTraceSubject(trace).last()
        val failure = assertThrows<FlickerSubjectException> { lastEntry.hasNoVisibleAppWindow() }
        Truth.assertThat(failure).hasMessageThat().contains("Found visible windows")
    }

    @Test
    fun canDetectTaskFragment() {
        // Verify if parser can read a dump file with 2 TaskFragments showed side-by-side.
        val trace = readWmTraceFromDumpFile("wm_trace_taskfragment.winscope")
        // There's only one entry in dump file.
        val entry = WindowManagerTraceSubject(trace).first()
        // Verify there's exact 2 TaskFragments in window hierarchy.
        Truth.assertThat(entry.wmState.taskFragments.size).isEqualTo(2)
    }

    @Test
    fun canDetectIsHomeActivityVisibleTablet() {
        val trace = readWmTraceFromDumpFile("tablet/wm_dump_home_screen.winscope")
        // There's only one entry in dump file.
        val entry = WindowManagerTraceSubject(trace).first()
        // Verify that the device is in home screen
        Truth.assertThat(entry.wmState.isHomeActivityVisible).isTrue()
        // Verify that the subject is in home screen
        entry.isHomeActivityVisible()
    }

    @Test
    fun canDetectTaskBarIsVisible() {
        val trace = readWmTraceFromDumpFile("tablet/wm_dump_home_screen.winscope")
        // There's only one entry in dump file.
        val entry = WindowManagerTraceSubject(trace).first()
        // Verify that the taskbar is visible
        entry.isNonAppWindowVisible(ComponentNameMatcher.TASK_BAR)
    }

    @Test
    fun canDetectWindowVisibilityWhen2WindowsHaveSameName() {
        val trace =
            readWmTraceFromFile("wm_trace_2activities_same_name.winscope", legacyTrace = true)
        val componentMatcher =
            ComponentNameMatcher(
                "com.android.server.wm.flicker.testapp",
                "com.android.server.wm.flicker.testapp.NotificationActivity"
            )
        WindowManagerTraceSubject(trace)
            .isAppWindowInvisible(componentMatcher)
            .then()
            .isAppWindowVisible(ComponentNameMatcher.SNAPSHOT, isOptional = true)
            .then()
            .isAppWindowVisible(ComponentNameMatcher.SPLASH_SCREEN, isOptional = true)
            .then()
            .isAppWindowVisible(componentMatcher)
            .forElapsedTimeRange(394872035003110L, 394874232110818L)
    }

    @Test
    fun canDetectInvisibleWindowBecauseActivityIsInvisible() {
        val entry = WindowManagerTraceSubject(trace).getEntryByElapsedTimestamp(9215551505798L)
        entry.isAppWindowInvisible(TestComponents.CHROME_SPLASH_SCREEN)
    }
}
