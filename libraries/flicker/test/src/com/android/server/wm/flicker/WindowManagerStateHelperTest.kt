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

import android.content.ComponentName
import android.view.Display
import android.view.Surface
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [WindowManagerStateHelper] tests. To run this test: `atest
 * FlickerLibTest:WindowManagerTraceHelperTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class WindowManagerStateHelperTest {
    private fun String.toComponentName() =
        ComponentName.unflattenFromString(this) ?: error("Unable to extract component name")

    private val chromeComponentName = ("com.android.chrome/org.chromium.chrome.browser" +
        ".firstrun.FirstRunActivity").toComponentName()
    private val simpleAppComponentName = "com.android.server.wm.flicker.testapp/.SimpleActivity"
        .toComponentName()

    private fun WindowManagerTrace.asSupplier(
        startingTimestamp: Long = 0
    ): () -> WindowManagerState {
        val iterator = this.dropWhile { it.timestamp < startingTimestamp }.iterator()
        return {
            if (iterator.hasNext()) {
                iterator.next()
            } else {
                error("Reached the end of the trace")
            }
        }
    }

    @Test
    fun canWaitForIme() {
        val trace = readWmTraceFromFile("wm_trace_ime.pb")
        val supplier = trace.asSupplier()
        val helper = WindowManagerStateHelper(supplier, numRetries = trace.entries.size,
            retryIntervalMs = 1)
        try {
            WindowManagerStateSubject.assertThat(helper.state)
                .isImeWindowShown(Display.DEFAULT_DISPLAY)
            error("IME state should not be available")
        } catch (e: AssertionError) {
            helper.waitImeWindowShown(Display.DEFAULT_DISPLAY)
            WindowManagerStateSubject.assertThat(helper.state)
                .isImeWindowShown(Display.DEFAULT_DISPLAY)
        }
    }

    @Test
    fun canFailImeNotShown() {
        val supplier = readWmTraceFromFile("wm_trace_ime.pb").asSupplier()
        val helper = WindowManagerStateHelper(supplier, retryIntervalMs = 1)
        try {
            WindowManagerStateSubject.assertThat(helper.state)
                .isImeWindowShown()
            error("IME state should not be available")
        } catch (e: AssertionError) {
            helper.waitImeWindowShown()
            WindowManagerStateSubject.assertThat(helper.state)
                .isImeWindowNotShown()
        }
    }

    @Test
    fun canWaitForWindow() {
        val trace = readWmTraceFromFile("wm_trace_open_app_cold.pb")
        val supplier = trace.asSupplier()
        val helper = WindowManagerStateHelper(supplier, numRetries = trace.entries.size,
            retryIntervalMs = 1)
        try {
            WindowManagerStateSubject.assertThat(helper.state).contains(simpleAppComponentName)
            error("Chrome window should not exist in the start of the trace")
        } catch (e: AssertionError) {
            helper.waitForVisibleWindow(simpleAppComponentName)
            WindowManagerStateSubject.assertThat(helper.state)
                .isVisible(simpleAppComponentName)
        }
    }

    @Test
    fun canFailWindowNotShown() {
        val trace = readWmTraceFromFile("wm_trace_open_app_cold.pb")
        val supplier = trace.asSupplier()
        val helper = WindowManagerStateHelper(supplier, numRetries = 3, retryIntervalMs = 1)
        try {
            WindowManagerStateSubject.assertThat(helper.state).contains(simpleAppComponentName)
            error("SimpleActivity window should not exist in the start of the trace")
        } catch (e: AssertionError) {
            helper.waitForVisibleWindow(simpleAppComponentName)
            WindowManagerStateSubject.assertThat(helper.state)
                .notContains(simpleAppComponentName)
        }
    }

    @Test
    fun canDetectHomeActivityVisibility() {
        val trace = readWmTraceFromFile("wm_trace_open_and_close_chrome.pb")
        val supplier = trace.asSupplier()
        val helper = WindowManagerStateHelper(supplier, numRetries = trace.entries.size,
            retryIntervalMs = 1)
        WindowManagerStateSubject.assertThat(helper.state).isHomeActivityVisible()
        helper.waitForVisibleWindow(chromeComponentName)
        WindowManagerStateSubject.assertThat(helper.state).isHomeActivityVisible(false)
        helper.waitForHomeActivityVisible()
        WindowManagerStateSubject.assertThat(helper.state).isHomeActivityVisible()
    }

    @Test
    fun canWaitActivityRemoved() {
        val trace = readWmTraceFromFile("wm_trace_open_and_close_chrome.pb")
        val supplier = trace.asSupplier()
        val helper = WindowManagerStateHelper(supplier, numRetries = trace.entries.size,
            retryIntervalMs = 1)
        WindowManagerStateSubject.assertThat(helper.state)
            .isHomeActivityVisible()
            .notContains(chromeComponentName)
        helper.waitForVisibleWindow(chromeComponentName)
        WindowManagerStateSubject.assertThat(helper.state).isVisible(chromeComponentName)
        helper.waitForActivityRemoved(chromeComponentName)
        WindowManagerStateSubject.assertThat(helper.state)
            .notContains(chromeComponentName)
            .isHomeActivityVisible()
    }

    @Test
    fun canWaitAppStateIdle() {
        val trace = readWmTraceFromFile("wm_trace_open_and_close_chrome.pb")
        val supplier = trace.asSupplier(startingTimestamp = 69443911868523)
        val helper = WindowManagerStateHelper(supplier, numRetries = trace.entries.size,
            retryIntervalMs = 1)
        try {
            WindowManagerStateSubject.assertThat(helper.state).isValid()
            error("Initial state in the trace should not be valid")
        } catch (e: AssertionError) {
            Truth.assertWithMessage("App transition never became idle")
                .that(helper.waitForAppTransitionIdle())
                .isTrue()
            WindowManagerStateSubject.assertThat(helper.state).isValid()
        }
    }

    @Test
    fun canWaitForRotation() {
        val trace = readWmTraceFromFile("wm_trace_rotation.pb")
        val supplier = trace.asSupplier()
        val helper = WindowManagerStateHelper(supplier, numRetries = trace.entries.size,
            retryIntervalMs = 1)
        WindowManagerStateSubject.assertThat(helper.state).isRotation(Surface.ROTATION_0)
        helper.waitForRotation(Surface.ROTATION_270)
        WindowManagerStateSubject.assertThat(helper.state).isRotation(Surface.ROTATION_270)
        helper.waitForRotation(Surface.ROTATION_0)
        WindowManagerStateSubject.assertThat(helper.state).isRotation(Surface.ROTATION_0)
    }

    @Test
    fun canFailRotationNotReached() {
        val trace = readWmTraceFromFile("wm_trace_rotation.pb")
        val supplier = trace.asSupplier()
        val helper = WindowManagerStateHelper(supplier, numRetries = trace.entries.size,
            retryIntervalMs = 1)
        WindowManagerStateSubject.assertThat(helper.state).isRotation(Surface.ROTATION_0)
        try {
            helper.waitForRotation(Surface.ROTATION_90)
            error("Should not have reached orientation ${Surface.ROTATION_90}")
        } catch (e: IllegalStateException) {
            WindowManagerStateSubject.assertThat(helper.state)
                .isNotRotation(Surface.ROTATION_90)
                .isRotation(Surface.ROTATION_0)
        }
    }

    @Test
    fun canWaitForRecents() {
        val trace = readWmTraceFromFile("wm_trace_open_recents.pb")
        val supplier = trace.asSupplier()
        val helper = WindowManagerStateHelper(supplier, numRetries = trace.entries.size,
            retryIntervalMs = 1)
        WindowManagerStateSubject.assertThat(helper.state).isRecentsActivityVisible(visible = false)
        helper.waitForRecentsActivityVisible()
        WindowManagerStateSubject.assertThat(helper.state)
            .isRecentsActivityVisible()
    }
}