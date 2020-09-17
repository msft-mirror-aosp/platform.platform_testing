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

package com.android.server.wm.flicker.traces.windowmanager

import com.android.server.wm.flicker.assertions.TraceAssertion
import com.android.server.wm.flicker.common.AssertionResult
import com.android.server.wm.flicker.common.Region
import com.android.server.wm.flicker.traces.SubjectBase
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Truth

/** Truth subject for [WindowManagerTrace] objects.  */
class WmTraceSubject private constructor(
    fm: FailureMetadata,
    val trace: WindowManagerTrace
) : SubjectBase<WindowManagerTrace, WindowManagerTraceEntry>(fm, trace) {
    /**
     * Signal that the last assertion set is complete. The next assertion added will start a new
     * set of assertions.
     *
     * E.g.: checkA().then().checkB()
     *
     * Will produce two sets of assertions (checkA) and (checkB) and checkB will only be checked
     * after checkA passes.
     */
    fun then() = apply {
        newAssertion = true
        assertionsChecker.checkChangingAssertions()
    }

    /**
     * Signal that the last assertion set is not complete. The next assertion added will be
     * appended to the current set of assertions.
     *
     * E.g.: checkA().and().checkB()
     *
     * Will produce one sets of assertions (checkA, checkB) and the assertion will only pass is
     * both checkA and checkB pass.
     */
    fun and() = apply { newAssertion = false }

    /**
     * Ignores the first entries in the trace, until the first assertion passes. If it reaches the
     * end of the trace without passing any assertion, return a failure with the name/reason from
     * the first assertion
     *
     * @return
     */
    fun skipUntilFirstAssertion() = apply { assertionsChecker.skipUntilFirstAssertion() }

    fun failWithMessage(message: String) = apply { fail(message) }

    /**
     * Checks if the non-app window with title containing [partialWindowTitle] exists above the app
     * windows and is visible
     *
     * @param partialWindowTitle window title to search
     */
    fun showsAboveAppWindow(partialWindowTitle: String) = apply {
        addAssertion("showsAboveAppWindow($partialWindowTitle)") {
            p: WindowManagerTraceEntry -> p.isAboveAppWindow(partialWindowTitle)
        }
    }

    /**
     * Checks if the non-app window with title containing [partialWindowTitle] exists above the app
     * windows and is invisible
     *
     * @param partialWindowTitle window title to search
     */
    fun hidesAboveAppWindow(partialWindowTitle: String) = apply {
        addAssertion("hidesAboveAppWindow($partialWindowTitle)") {
            it.isAboveAppWindow(partialWindowTitle, isVisible = false)
        }
    }

    /**
     * Checks if the non-app window with title containing [partialWindowTitle] exists below the app
     * windows and is visible
     *
     * @param partialWindowTitle window title to search
     */
    fun showsBelowAppWindow(partialWindowTitle: String) = apply {
        addAssertion("showsBelowAppWindow($partialWindowTitle)") {
            it.isBelowAppWindow(partialWindowTitle)
        }
    }

    /**
     * Checks if the non-app window with title containing [partialWindowTitle] exists below the app
     * windows and is invisible
     *
     * @param partialWindowTitle window title to search
     */
    fun hidesBelowAppWindow(partialWindowTitle: String) = apply {
        addAssertion("hidesBelowAppWindow($partialWindowTitle)") {
            it.isBelowAppWindow(partialWindowTitle, isVisible = false)
        }
    }

    /**
     * Checks if non-app window with title containing the [partialWindowTitle] exists above or
     * below the app windows and is visible
     *
     * @param partialWindowTitle window title to search
     */
    fun showsNonAppWindow(partialWindowTitle: String) = apply {
        addAssertion("showsNonAppWindow($partialWindowTitle)") {
            it.hasNonAppWindow(partialWindowTitle)
        }
    }

    /**
     * Checks if non-app window with title containing the [partialWindowTitle] exists above or
     * below the app windows and is invisible
     *
     * @param partialWindowTitle window title to search
     */
    fun hidesNonAppWindow(partialWindowTitle: String) = apply {
        addAssertion("hidesNonAppWindow($partialWindowTitle)") {
            it.hasNonAppWindow(partialWindowTitle, isVisible = false)
        }
    }

    /**
     * Checks if an app window with title containing the [partialWindowTitles] is on top
     *
     * @param partialWindowTitles window title to search
     */
    fun showsAppWindowOnTop(vararg partialWindowTitles: String) = apply {
        val assertionName = "showsAppWindowOnTop(${partialWindowTitles.joinToString(",")})"
        addAssertion(assertionName) {
            var result = AssertionResult("No window titles to search", assertionName,
                    success = false)

            for (windowTitle in partialWindowTitles) {
                result = it.isAppWindowVisible(windowTitle)
                if (result.passed()) {
                    result = it.isVisibleAppWindowOnTop(windowTitle)
                    if (result.passed()) {
                        break
                    }
                }
            }
            result
        }
    }

    /**
     * Checks if app window with title containing the [partialWindowTitle] is not on top
     *
     * @param partialWindowTitle window title to search
     */
    fun appWindowNotOnTop(partialWindowTitle: String) = apply {
        addAssertion("hidesAppWindowOnTop($partialWindowTitle)") {
            var result = it.isAppWindowVisible(partialWindowTitle).negate()
            if (result.failed()) {
                result = it.isVisibleAppWindowOnTop(partialWindowTitle).negate()
            }
            result
        }
    }

    /**
     * Checks if app window with title containing the [partialWindowTitle] is visible
     *
     * @param partialWindowTitle window title to search
     */
    fun showsAppWindow(partialWindowTitle: String) = apply {
        addAssertion("showsAppWindow($partialWindowTitle)") {
            it.isAppWindowVisible(partialWindowTitle)
        }
    }

    /**
     * Checks if app window with title containing the [partialWindowTitle] is invisible
     *
     * @param partialWindowTitle window title to search
     */
    fun hidesAppWindow(partialWindowTitle: String) = apply {
        addAssertion("hidesAppWindow($partialWindowTitle)") {
            it.isAppWindowVisible(partialWindowTitle).negate()
        }
    }

    /**
     * Checks if no app windows containing the [partialWindowTitles] overlap with each other.
     *
     * @param partialWindowTitles partial titles of windows to check
     */
    fun noWindowsOverlap(vararg partialWindowTitles: String): WmTraceSubject = apply {
        val titles = partialWindowTitles.toSet()
        val repr = titles.joinToString(", ")
        require(titles.size > 1) { "Must give more than one window to check! (Given $repr)" }
        addAssertion("noWindowsOverlap($repr)") {
            it.noWindowsOverlap(titles)
        }
    }

    /**
     * Checks if the window named [aboveWindowTitle] is above the one named [belowWindowTitle] in
     * z-order.
     *
     * @param aboveWindowTitle partial name of the expected top window
     * @param belowWindowTitle partial name of the expected bottom window
     */
    fun isAboveWindow(aboveWindowTitle: String, belowWindowTitle: String): WmTraceSubject = apply {
        require(aboveWindowTitle != belowWindowTitle)
        addAssertion("$aboveWindowTitle is above $belowWindowTitle") {
            it.isAboveWindow(aboveWindowTitle, belowWindowTitle)
        }
    }

    fun coversAtLeastRegion(partialWindowTitle: String, region: Region) = apply {
        addAssertion("coversAtLeastRegion($partialWindowTitle, $region)") {
            it.coversAtLeastRegion(partialWindowTitle, region)
        }
    }

    fun coversAtLeastRegion(partialWindowTitle: String, region: android.graphics.Region) = apply {
        addAssertion("coversAtLeastRegion($partialWindowTitle, $region)") {
            it.coversAtLeastRegion(partialWindowTitle, region)
        }
    }

    fun coversAtMostRegion(partialWindowTitle: String, region: Region) = apply {
        addAssertion("coversAtMostRegion($partialWindowTitle, $region)") {
            it.coversAtMostRegion(partialWindowTitle, region)
        }
    }

    fun coversAtMostRegion(partialWindowTitle: String, region: android.graphics.Region) = apply {
        addAssertion("coversAtMostRegion($partialWindowTitle, $region)") {
            it.coversAtMostRegion(partialWindowTitle, region)
        }
    }

    operator fun invoke(name: String, assertion: TraceAssertion<WindowManagerTraceEntry>) =
            apply { addAssertion(name, assertion) }

    override val traceName: String
        get() = "WindowManager"

    companion object {
        /**
         * Boiler-plate Subject.Factory for WmTraceSubject
         */
        private val FACTORY = Factory { fm: FailureMetadata, subject: WindowManagerTrace ->
            WmTraceSubject(fm, subject)
        }

        /**
         * User-defined entry point
         */
        @JvmStatic
        fun assertThat(entry: WindowManagerTrace) =
                Truth.assertAbout(FACTORY).that(entry) as WmTraceSubject

        /**
         * Static method for getting the subject factory (for use with assertAbout())
         */
        @JvmStatic
        fun entries(): Factory<SubjectBase<WindowManagerTrace, WindowManagerTraceEntry>,
            WindowManagerTrace> = FACTORY
    }
}
