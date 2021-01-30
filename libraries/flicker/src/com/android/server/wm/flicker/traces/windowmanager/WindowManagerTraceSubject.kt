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

package com.android.server.wm.flicker.traces.windowmanager

import com.android.server.wm.flicker.assertions.Assertion
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.traces.common.Region
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.google.common.truth.FailureMetadata
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Truth

/** Truth subject for [WindowManagerTrace] objects.  */
class WindowManagerTraceSubject private constructor(
    fm: FailureMetadata,
    val trace: WindowManagerTrace
) : FlickerTraceSubject<WindowManagerStateSubject>(fm, trace) {
    override val defaultFacts: String = buildString {
        if (trace.hasSource()) {
            append("Path: ${trace.source}")
            append("\n")
        }
        append("Trace: $trace")
    }

    override val subjects by lazy {
        trace.entries.map { WindowManagerStateSubject.assertThat(it, this) }
    }

    /**
     * Signal that the last assertion set is complete. The next assertion added will start a new
     * set of assertions.
     *
     * E.g.: checkA().then().checkB()
     *
     * Will produce two sets of assertions (checkA) and (checkB) and checkB will only be checked
     * after checkA passes.
     */
    fun then(): WindowManagerTraceSubject =
        apply { startAssertionBlock() }

    /**
     * Ignores the first entries in the trace, until the first assertion passes. If it reaches the
     * end of the trace without passing any assertion, return a failure with the name/reason from
     * the first assertion
     *
     * @return
     */
    fun skipUntilFirstAssertion(): WindowManagerTraceSubject =
        apply { assertionsChecker.skipUntilFirstAssertion() }

    fun isEmpty(): WindowManagerTraceSubject = apply {
        check("Trace is empty").that(trace).isEmpty()
    }

    fun isNotEmpty(): WindowManagerTraceSubject = apply {
        check("Trace is not empty").that(trace).isNotEmpty()
    }

    /**
     * Checks if the non-app window with title containing [partialWindowTitle] exists above the app
     * windows and is visible
     *
     * @param partialWindowTitle window title to search
     */
    fun showsAboveAppWindow(partialWindowTitle: String): WindowManagerTraceSubject = apply {
        addAssertion("showsAboveAppWindow($partialWindowTitle)") {
            it.isAboveAppWindow(partialWindowTitle)
        }
    }

    /**
     * Checks if the non-app window with title containing [partialWindowTitle] exists above the app
     * windows and is invisible
     *
     * @param partialWindowTitle window title to search
     */
    fun hidesAboveAppWindow(partialWindowTitle: String): WindowManagerTraceSubject = apply {
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
    fun showsBelowAppWindow(partialWindowTitle: String): WindowManagerTraceSubject = apply {
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
    fun hidesBelowAppWindow(partialWindowTitle: String): WindowManagerTraceSubject = apply {
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
    fun showsNonAppWindow(partialWindowTitle: String): WindowManagerTraceSubject = apply {
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
    fun hidesNonAppWindow(partialWindowTitle: String): WindowManagerTraceSubject = apply {
        addAssertion("hidesNonAppWindow($partialWindowTitle)") {
            it.hasNonAppWindow(partialWindowTitle, isVisible = false)
        }
    }

    /**
     * Checks if an app window with title containing the [partialWindowTitles] is on top
     *
     * @param partialWindowTitles window title to search
     */
    fun showsAppWindowOnTop(vararg partialWindowTitles: String): WindowManagerTraceSubject = apply {
        val assertionName = "showsAppWindowOnTop(${partialWindowTitles.joinToString(",")})"
        addAssertion(assertionName) {
            Truth.assertWithMessage("No window titles to search")
                .that(partialWindowTitles)
                .isNotEmpty()
            it.showsAppWindowOnTop(*partialWindowTitles)
        }
    }

    /**
     * Checks if app window with title containing the [partialWindowTitle] is not on top
     *
     * @param partialWindowTitle window title to search
     */
    fun appWindowNotOnTop(partialWindowTitle: String): WindowManagerTraceSubject = apply {
        addAssertion("hidesAppWindowOnTop($partialWindowTitle)") {
            it.hasAppWindow(partialWindowTitle, isVisible = false)
        }
    }

    /**
     * Checks if app window with title containing the [partialWindowTitle] is visible
     *
     * @param partialWindowTitle window title to search
     */
    fun showsAppWindow(partialWindowTitle: String): WindowManagerTraceSubject = apply {
        addAssertion("showsAppWindow($partialWindowTitle)") {
            it.hasAppWindow(partialWindowTitle, isVisible = true)
        }
    }

    /**
     * Checks if app window with title containing the [partialWindowTitle] is invisible
     *
     * @param partialWindowTitle window title to search
     */
    fun hidesAppWindow(partialWindowTitle: String): WindowManagerTraceSubject = apply {
        addAssertion("hidesAppWindow($partialWindowTitle)") {
            it.hasAppWindow(partialWindowTitle, isVisible = false)
        }
    }

    /**
     * Checks if no app windows containing the [partialWindowTitles] overlap with each other.
     *
     * @param partialWindowTitles partial titles of windows to check
     */
    fun noWindowsOverlap(vararg partialWindowTitles: String): WindowManagerTraceSubject = apply {
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
    fun isAboveWindow(
        aboveWindowTitle: String,
        belowWindowTitle: String
    ): WindowManagerTraceSubject = apply {
        require(aboveWindowTitle != belowWindowTitle)
        addAssertion("$aboveWindowTitle is above $belowWindowTitle") {
            it.isAboveWindow(aboveWindowTitle, belowWindowTitle)
        }
    }

    fun coversAtLeastRegion(
        partialWindowTitle: String,
        region: Region
    ): WindowManagerTraceSubject = apply {
        addAssertion("coversAtLeastRegion($partialWindowTitle, $region)") {
            it.coversAtLeastRegion(partialWindowTitle, region)
        }
    }

    fun coversAtLeastRegion(partialWindowTitle: String, region: android.graphics.Region) = apply {
        addAssertion("coversAtLeastRegion($partialWindowTitle, $region)") {
            it.coversAtLeastRegion(partialWindowTitle, region)
        }
    }

    fun coversAtMostRegion(
        partialWindowTitle: String,
        region: Region
    ): WindowManagerTraceSubject = apply {
        addAssertion("coversAtMostRegion($partialWindowTitle, $region)") {
            it.coversAtMostRegion(partialWindowTitle, region)
        }
    }

    fun coversAtMostRegion(partialWindowTitle: String, region: android.graphics.Region) = apply {
        addAssertion("coversAtMostRegion($partialWindowTitle, $region)") {
            it.coversAtMostRegion(partialWindowTitle, region)
        }
    }

    /**
     * Checks that all visible layers are shown for more than one consecutive entry
     */
    fun visibleWindowsShownMoreThanOneConsecutiveEntry(
        ignoreWindows: List<String> = emptyList()
    ): WindowManagerTraceSubject = apply {
        visibleEntriesShownMoreThanOneConsecutiveTime { subject ->
            subject.wmState.windowStates
                .filter { it.isVisible }
                .filter {
                    ignoreWindows.none { windowName -> windowName in it.title }
                }
                .map { it.name }
                .toSet()
        }
    }

    operator fun invoke(
        name: String,
        assertion: Assertion<WindowManagerStateSubject>
    ): WindowManagerTraceSubject = apply { addAssertion(name, assertion) }

    /**
     * Run the assertions for all trace entries within the specified time range
     */
    fun forRange(startTime: Long, endTime: Long) {
        val subjectsInRange = subjects.filter { it.wmState.timestamp in startTime..endTime }
        assertionsChecker.test(subjectsInRange)
    }

    /**
     * User-defined entry point for the trace entry with [timestamp]
     *
     * @param timestamp of the entry
     */
    fun entry(timestamp: Long): WindowManagerStateSubject =
        subjects.first { it.wmState.timestamp == timestamp }

    companion object {
        /**
         * Boiler-plate Subject.Factory for WmTraceSubject
         */
        private val FACTORY: Factory<Subject, WindowManagerTrace> =
            Factory { fm, subject -> WindowManagerTraceSubject(fm, subject) }

        /**
         * User-defined entry point
         */
        @JvmStatic
        fun assertThat(entry: WindowManagerTrace): WindowManagerTraceSubject {
            val strategy = FlickerFailureStrategy()
            val subject = StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                .about(FACTORY)
                .that(entry) as WindowManagerTraceSubject
            strategy.init(subject)
            return subject
        }

        /**
         * Static method for getting the subject factory (for use with assertAbout())
         */
        @JvmStatic
        fun entries(): Factory<Subject, WindowManagerTrace> = FACTORY
    }
}
