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

import android.graphics.Region
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

/** Truth subject for [WindowManagerTrace] objects.  */
class WmTraceSubject private constructor(
    fm: FailureMetadata,
    subject: WindowManagerTrace
) : Subject<WmTraceSubject, WindowManagerTrace>(fm, subject) {

    private val assertionsChecker = AssertionsChecker<WindowManagerTraceEntry>()
    private var newAssertion = true

    private fun addAssertion(name: String, assertion: TraceAssertion<WindowManagerTraceEntry>) {
        if (newAssertion) {
            assertionsChecker.add(assertion, name)
        } else {
            assertionsChecker.append(assertion, name)
        }
    }

    /**
     * Run the assertions for all trace entries
     */
    fun forAllEntries() {
        test()
    }

    /**
     * Run the assertions for all trace entries within the specified time range
     */
    fun forRange(startTime: Long, endTime: Long) {
        assertionsChecker.filterByRange(startTime, endTime)
        test()
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
    fun then(): WmTraceSubject {
        newAssertion = true
        assertionsChecker.checkChangingAssertions()
        return this
    }

    /**
     * Signal that the last assertion set is not complete. The next assertion added will be
     * appended to the current set of assertions.
     *
     * E.g.: checkA().and().checkB()
     *
     * Will produce one sets of assertions (checkA, checkB) and the assertion will only pass is both
     * checkA and checkB pass.
     */
    fun and(): WmTraceSubject {
        newAssertion = false
        return this
    }

    /**
     * Ignores the first entries in the trace, until the first assertion passes. If it reaches the
     * end of the trace without passing any assertion, return a failure with the name/reason from
     * the first assertion
     *
     * @return
     */
    fun skipUntilFirstAssertion(): WmTraceSubject {
        assertionsChecker.skipUntilFirstAssertion()
        return this
    }

    /**
     * Run the assertions only in the first trace entry
     */
    fun inTheBeginning() {
        if (actual().entries.isEmpty()) {
            fail("No entries found.")
        }
        assertionsChecker.checkFirstEntry()
        test()
    }

    /**
     * Run the assertions only in the last  trace entry
     */
    fun atTheEnd() {
        if (actual().entries.isEmpty()) {
            fail("No entries found.")
        }
        assertionsChecker.checkLastEntry()
        test()
    }

    private fun test() {
        val failures = assertionsChecker.test(actual().entries)
        if (failures.isNotEmpty()) {
            val failureTracePath = actual()!!.source
            val failureLogs = failures.joinToString("\n") { it.toString() }
            var tracePath = ""
            if (failureTracePath.isPresent) {
                tracePath = """

                    WindowManager Trace can be found in: ${failureTracePath.get().toAbsolutePath()}
                    Checksum: ${actual()!!.sourceChecksum}

                    """.trimIndent()
            }
            fail(tracePath + failureLogs)
        }
    }


    /**
     * Checks if the non-app window with title containing [partialWindowTitle] exists above the app
     * windows and is visible
     *
     * @param partialWindowTitle window title to search
     */
    fun showsAboveAppWindow(partialWindowTitle: String): WmTraceSubject {
        addAssertion("showsAboveAppWindow($partialWindowTitle)") {
            p: WindowManagerTraceEntry -> p.isAboveAppWindow(partialWindowTitle)
        }
        return this
    }

    /**
     * Checks if the non-app window with title containing [partialWindowTitle] exists above the app
     * windows and is invisible
     *
     * @param partialWindowTitle window title to search
     */
    fun hidesAboveAppWindow(partialWindowTitle: String): WmTraceSubject {
        addAssertion("hidesAboveAppWindow($partialWindowTitle)") {
            it.isAboveAppWindow(partialWindowTitle, isVisible = false)
        }
        return this
    }

    /**
     * Checks if the non-app window with title containing [partialWindowTitle] exists below the app
     * windows and is visible
     *
     * @param partialWindowTitle window title to search
     */
    fun showsBelowAppWindow(partialWindowTitle: String): WmTraceSubject {
        addAssertion("showsBelowAppWindow($partialWindowTitle)") {
            it.isBelowAppWindow(partialWindowTitle)
        }
        return this
    }

    /**
     * Checks if the non-app window with title containing [partialWindowTitle] exists below the app
     * windows and is invisible
     *
     * @param partialWindowTitle window title to search
     */
    fun hidesBelowAppWindow(partialWindowTitle: String): WmTraceSubject {
        addAssertion("hidesBelowAppWindow($partialWindowTitle)") {
            it.isBelowAppWindow(partialWindowTitle, isVisible = false)
        }
        return this
    }

    /**
     * Checks if non-app window with title containing the [partialWindowTitle] exists above or
     * below the app windows and is visible
     *
     * @param partialWindowTitle window title to search
     */
    fun showsNonAppWindow(partialWindowTitle: String): WmTraceSubject {
        addAssertion("showsNonAppWindow($partialWindowTitle)") {
            it.hasNonAppWindow(partialWindowTitle)
        }
        return this
    }

    /**
     * Checks if non-app window with title containing the [partialWindowTitle] exists above or
     * below the app windows and is invisible
     *
     * @param partialWindowTitle window title to search
     */
    fun hidesNonAppWindow(partialWindowTitle: String): WmTraceSubject {
        addAssertion("hidesNonAppWindow($partialWindowTitle)") {
            it.hasNonAppWindow(partialWindowTitle, isVisible = false)
        }
        return this
    }

    /**
     * Checks if app window with title containing the [partialWindowTitle] is on top
     *
     * @param partialWindowTitle window title to search
     */
    fun showsAppWindowOnTop(partialWindowTitle: String): WmTraceSubject {
        addAssertion("showsAppWindowOnTop($partialWindowTitle)") {
            var result = it.isAppWindowVisible(partialWindowTitle)
            if (result.passed()) {
                result = it.isVisibleAppWindowOnTop(partialWindowTitle)
            }
            result
        }
        return this
    }

    /**
     * Checks if app window with title containing the [partialWindowTitle] is not on top
     *
     * @param partialWindowTitle window title to search
     */
    fun appWindowNotOnTop(partialWindowTitle: String): WmTraceSubject {
        addAssertion("hidesAppWindowOnTop($partialWindowTitle)") { entry: WindowManagerTraceEntry ->
            var result = entry.isAppWindowVisible(partialWindowTitle).negate()
            if (result.failed()) {
                result = entry.isVisibleAppWindowOnTop(partialWindowTitle).negate()
            }
            result
        }
        return this
    }

    /**
     * Checks if app window with title containing the [partialWindowTitle] is visible
     *
     * @param partialWindowTitle window title to search
     */
    fun showsAppWindow(partialWindowTitle: String): WmTraceSubject {
        addAssertion("showsAppWindow($partialWindowTitle)") {
            it.isAppWindowVisible(partialWindowTitle)
        }
        return this
    }

    /**
     * Checks if app window with title containing the [partialWindowTitle] is invisible
     *
     * @param partialWindowTitle window title to search
     */
    fun hidesAppWindow(partialWindowTitle: String): WmTraceSubject {
        addAssertion("hidesAppWindow($partialWindowTitle)") {
            it.isAppWindowVisible(partialWindowTitle).negate()
        }
        return this
    }

    fun coversAtLeastRegion(partialWindowTitle: String, region: Region) = apply {
        addAssertion("coversAtLeastRegion($partialWindowTitle, $region)") {
            it.coversAtLeastRegion(partialWindowTitle, region)
        }
    }

    fun coversAtMostRegion(partialWindowTitle: String, region: Region) = apply {
        addAssertion("coversAtMostRegion($partialWindowTitle, $region)") {
            it.coversAtMostRegion(partialWindowTitle, region)
        }
    }

    companion object {
        // Boiler-plate Subject.Factory for WmTraceSubject
        private val FACTORY = Factory { fm: FailureMetadata, subject: WindowManagerTrace ->
            WmTraceSubject(fm, subject)
        }

        // User-defined entry point
        @JvmStatic
        fun assertThat(entry: WindowManagerTrace): WmTraceSubject {
            return Truth.assertAbout(FACTORY).that(entry)
        }

        // User-defined entry point
        @JvmStatic
        fun assertThat(result: TransitionResult): WmTraceSubject {
            val entries = WindowManagerTrace.parseFrom(
                    result.windowManagerTrace,
                    result.windowManagerTracePath,
                    result.windowManagerTraceChecksum)
            return Truth.assertWithMessage(result.toString()).about(FACTORY).that(entries)
        }

        // Static method for getting the subject factory (for use with assertAbout())
        @JvmStatic
        fun entries(): Factory<WmTraceSubject, WindowManagerTrace> {
            return FACTORY
        }
    }
}
