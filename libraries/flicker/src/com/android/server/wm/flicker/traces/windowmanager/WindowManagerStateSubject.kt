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

import android.content.ComponentName
import android.view.Display
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.android.server.wm.traces.common.Region
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
import com.android.server.wm.traces.parser.toActivityName
import com.android.server.wm.traces.parser.toAndroidRegion
import com.android.server.wm.traces.parser.toWindowName
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

class WindowManagerStateSubject private constructor(
    fm: FailureMetadata,
    val wmState: WindowManagerState,
    trace: WindowManagerTraceSubject?
) : FlickerSubject(fm, wmState) {
    override val defaultFacts = "${trace?.defaultFacts ?: ""}\nEntry: $wmState"

    fun isEmpty(): WindowManagerStateSubject = apply {
        check("State is empty")
            .that(wmState.windowStates)
            .isEmpty()
    }

    fun isNotEmpty(): WindowManagerStateSubject = apply {
        check("State is not empty")
            .that(wmState.windowStates)
            .isNotEmpty()
    }

    /**
     * Checks if the first window with title containing [windowTitle] has a visible area of at
     * least [testRegion], that is, if its area of the window frame covers each point in
     * the region.
     *
     * @param windowTitle Name of the layer to search
     * @param testRegion Expected visible area of the window
     */
    fun coversAtLeastRegion(
        windowTitle: String,
        testRegion: Region
    ): WindowManagerStateSubject = apply {
        return coversAtLeastRegion(windowTitle, testRegion.toAndroidRegion())
    }

    /**
     * Checks if the first window with title containing [windowTitle] has a visible area of at
     * least [testRegion], that is, if its area of the window frame covers each point in
     * the region.
     *
     * @param windowTitle Name of the layer to search
     * @param testRegion Expected visible area of the window
     */
    fun coversAtLeastRegion(
        windowTitle: String,
        testRegion: android.graphics.Region
    ): WindowManagerStateSubject = apply {
        covers(windowTitle) { windowRegion ->
            val testRect = testRegion.bounds
            val intersection = windowRegion.toAndroidRegion()
            val covers = intersection.op(testRect, android.graphics.Region.Op.INTERSECT) &&
                !intersection.op(testRect, android.graphics.Region.Op.XOR)

            if (!covers) {
                fail(Fact.fact("Region to test", testRegion),
                    Fact.fact("Uncovered region", intersection))
            }
        }
    }

    /**
     * Checks if the first window with title containing [windowTitle] has a visible area of at
     * most [testRegion], that is, if the region covers each point in the window frame.
     *
     * @param windowTitle Name of the layer to search
     * @param testRegion Expected visible area of the window
     */
    fun coversAtMostRegion(
        windowTitle: String,
        testRegion: Region
    ): WindowManagerStateSubject = apply {
        coversAtMostRegion(windowTitle, testRegion.toAndroidRegion())
    }

    /**
     * Checks if the first window with title containing [windowTitle] has a visible area of at
     * most [testRegion], that is, if the region covers each point in the window frame.
     *
     * @param windowTitle Name of the layer to search
     * @param testRegion Expected visible area of the window
     */
    fun coversAtMostRegion(
        windowTitle: String,
        testRegion: android.graphics.Region
    ): WindowManagerStateSubject = apply {
        covers(windowTitle) { windowRegion ->
            val testRect = testRegion.bounds
            val intersection = windowRegion.toAndroidRegion()
            val covers = intersection.op(testRect, android.graphics.Region.Op.INTERSECT) &&
                !intersection.op(windowRegion.toAndroidRegion(), android.graphics.Region.Op.XOR)

            if (!covers) {
                fail(Fact.fact("Region to test", testRegion),
                    Fact.fact("Out-of-bounds region", intersection))
            }
        }
    }

    /**
     * Obtains the region of the first visible window with title containing [windowTitle].
     *
     * @param windowTitle Name of the layer to search
     * @param resultComputation Predicate to compute a result based on the found window's region
     */
    private fun covers(
        windowTitle: String,
        resultComputation: (Region) -> Unit
    ): WindowManagerStateSubject = apply {
        wmState.windowStates.checkVisibility(windowTitle, isVisible = true)

        val foundWindow = wmState.windowStates.first { it.name.contains(windowTitle) }
        val foundRegion = foundWindow.frameRegion
        resultComputation(foundRegion)
    }

    /**
     * Checks any window containing the title [windowTitle] exists in the hierarchy.
     *
     * @param partialWindowTitles window title to search to search
     */
    fun hasWindow(vararg partialWindowTitles: String): WindowManagerStateSubject = apply {
        var found = false

        for (windowTitle in partialWindowTitles) {
            found = wmState.windowStates.any { it.name.contains(windowTitle) }

            if (found) {
                break
            }
        }
        if (!found) {
            fail("Could not find", partialWindowTitles.toList().joinToString(", "))
        }
    }

    /**
     * Checks if a window containing the title [windowTitle] exists in the hierarchy.
     *
     * @param windowTitle Title of the window to search
     */
    fun hasNotWindow(windowTitle: String): WindowManagerStateSubject = apply {
        val found = wmState.windowStates.none { it.name.contains(windowTitle) }
        if (!found) {
            fail("Could find", windowTitle)
        }
    }

    /**
     * Checks a window containing the title [windowTitle] is visible
     *
     * @param windowTitle Title of the window to search
     */
    fun isVisible(windowTitle: String): WindowManagerStateSubject = apply {
        wmState.windowStates.checkVisibility(windowTitle, isVisible = true)
    }

    /**
     * Checks a window containing the title [windowTitle] is invisible
     *
     * @param windowTitle Title of the window to search
     */
    fun isInvisible(windowTitle: String): WindowManagerStateSubject = apply {
        wmState.windowStates.checkVisibility(windowTitle, isVisible = false)
    }

    private fun Array<WindowState>.checkVisibility(windowTitle: String, isVisible: Boolean) {
        if (isVisible) {
            hasWindow(windowTitle)
            val invisibleWindows = this.filterNot { it.isVisible }
            if (invisibleWindows.any { it.name.contains(windowTitle) }) {
                fail("Is Invisible", windowTitle)
            }
        } else {
            try {
                hasNotWindow(windowTitle)
            } catch (e: AssertionError) {
                val visibleWindows = this.filter { it.isVisible }
                if (visibleWindows.any { it.name.contains(windowTitle) }) {
                    fail("Is Visible", windowTitle)
                }
            }
        }
    }

    /**
     * Checks if the non-app window with title containing [windowTitle] exists above the app
     * windows and if its visibility is equal to [isVisible]
     *
     * @param windowTitle window title to search
     * @param isVisible if the found window should be visible or not
     */
    @JvmOverloads
    fun isAboveAppWindow(
        windowTitle: String,
        isVisible: Boolean = true
    ): WindowManagerStateSubject = apply {
        wmState.aboveAppWindows.checkVisibility(windowTitle, isVisible)
    }

    /**
     * Checks if the non-app window with title containing [windowTitle] exists below the app
     * windows and if its visibility is equal to [isVisible]
     *
     * @param windowTitle window title to search
     * @param isVisible if the found window should be visible or not
     */
    @JvmOverloads
    fun isBelowAppWindow(
        windowTitle: String,
        isVisible: Boolean = true
    ): WindowManagerStateSubject = apply {
        wmState.belowAppWindows.checkVisibility(windowTitle, isVisible)
    }

    /**
     * Check if the window named [aboveWindowTitle] is above the one named [belowWindowTitle].
     *
     * @param aboveWindowTitle name of the window that should be above
     * @param belowWindowTitle name of the window that should be below
     */
    fun isAboveWindow(aboveWindowTitle: String, belowWindowTitle: String) {
        // windows are ordered by z-order, from top to bottom
        val aboveZ = wmState.windowStates.indexOfFirst { aboveWindowTitle in it.name }
        val belowZ = wmState.windowStates.indexOfFirst { belowWindowTitle in it.name }

        hasWindow(aboveWindowTitle)
        hasWindow(belowWindowTitle)
        if (aboveZ >= belowZ) {
            fail("$aboveWindowTitle is above $belowWindowTitle")
        }
    }

    /**
     * Checks if non-app window with title containing the [windowTitle] exists above or below the
     * app windows and if its visibility is equal to [isVisible]
     *
     * @param windowTitle window title to search
     * @param isVisible if the found window should be visible or not
     */
    @JvmOverloads
    fun hasNonAppWindow(
        windowTitle: String,
        isVisible: Boolean = true
    ): WindowManagerStateSubject = apply {
        wmState.nonAppWindows.checkVisibility(windowTitle, isVisible)
    }

    /**
     * Checks if an app window with title containing the [partialWindowTitles] is on top
     *
     * @param partialWindowTitles window title to search
     */
    fun showsAppWindowOnTop(vararg partialWindowTitles: String): WindowManagerStateSubject = apply {
        hasWindow(*partialWindowTitles)
        val windowOnTop = partialWindowTitles.any { wmState.topVisibleAppWindow.contains(it) }

        if (!windowOnTop) {
            fail(Fact.fact("Not on top", partialWindowTitles),
                Fact.fact("Found", wmState.topVisibleAppWindow))
        }
    }

    /**
     * Checks if any of the given windows overlap with each other.
     *
     * @param partialWindowTitles Title of the windows that should not overlap
     */
    fun noWindowsOverlap(partialWindowTitles: Set<String>): WindowManagerStateSubject = apply {
        partialWindowTitles.forEach { hasWindow(it) }
        val foundWindows = partialWindowTitles
            .associateWith { title -> wmState.windowStates.find { it.name.contains(title) } }
            // keep entries only for windows that we actually found by removing nulls
            .filterValues { it != null }
            .mapValues { (_, v) -> v!!.frameRegion }

        val regions = foundWindows.entries.toList()
        for (i in regions.indices) {
            val (ourTitle, ourRegion) = regions[i]
            for (j in i + 1 until regions.size) {
                val (otherTitle, otherRegion) = regions[j]
                if (ourRegion.toAndroidRegion().op(otherRegion.toAndroidRegion(),
                        android.graphics.Region.Op.INTERSECT)) {
                    fail(Fact.fact("Overlap", ourTitle), Fact.fact("Overlap", otherTitle))
                }
            }
        }
    }

    /**
     * Checks if app window with title containing the [windowTitle] is visible
     *
     * @param windowTitle window title to search
     * @param isVisible if the found window should be visible or not
     */
    @JvmOverloads
    fun hasAppWindow(
        windowTitle: String,
        isVisible: Boolean = true
    ): WindowManagerStateSubject = apply {
        wmState.appWindows.checkVisibility(windowTitle, isVisible)
    }

    /**
     * Asserts if the display with id [displayId] has rotation [rotation]
     *
     * @param rotation to assert
     * @param displayId of the target display
     */
    @JvmOverloads
    fun isRotation(
        rotation: Int,
        displayId: Int = Display.DEFAULT_DISPLAY
    ): WindowManagerStateSubject = apply {
        Truth.assertWithMessage("Rotation should be $rotation")
            .that(rotation)
            .isEqualTo(wmState.getRotation(displayId))
    }

    /**
     * Asserts if the display with id [displayId] has rotation [rotation]
     *
     * @param rotation to assert
     * @param displayId of the target display
     */
    @JvmOverloads
    fun isNotRotation(
        rotation: Int,
        displayId: Int = Display.DEFAULT_DISPLAY
    ): WindowManagerStateSubject = apply {
        Truth.assertWithMessage("Rotation should not be $rotation")
            .that(rotation)
            .isNotEqualTo(wmState.getRotation(displayId))
    }

    fun contains(activity: ComponentName): WindowManagerStateSubject = apply {
        val windowName = activity.toWindowName()
        val activityName = activity.toActivityName()
        Truth.assertWithMessage("Activity=$activityName must exist.")
            .that(wmState.containsActivity(activityName)).isTrue()
        Truth.assertWithMessage("Window=$windowName must exits.")
            .that(wmState.containsWindow(windowName)).isTrue()
    }

    fun notContains(activity: ComponentName): WindowManagerStateSubject = apply {
        val windowName = activity.toWindowName()
        val activityName = activity.toActivityName()
        Truth.assertWithMessage("Activity=$activityName must NOT exist.")
            .that(wmState.containsActivity(activityName)).isFalse()
        Truth.assertWithMessage("Window=$windowName must NOT exits.")
            .that(wmState.containsWindow(windowName)).isFalse()
    }

    @JvmOverloads
    fun isRecentsActivityVisible(visible: Boolean = true): WindowManagerStateSubject = apply {
        if (wmState.isHomeRecentsComponent) {
            isHomeActivityVisible()
        } else {
            Truth.assertWithMessage("Recents activity is ${if (visible) "" else "not"} visible")
                .that(wmState.isRecentsActivityVisible)
                .isEqualTo(visible)
        }
    }

    fun isValid(): WindowManagerStateSubject = apply {
        Truth.assertWithMessage("Must have stacks").that(wmState.stackCount).isGreaterThan(0)
        // TODO: Update when keyguard will be shown on multiple displays
        if (!wmState.keyguardControllerState.isKeyguardShowing) {
            Truth.assertWithMessage("There should be at least one resumed activity in the system.")
                .that(wmState.resumedActivitiesCount).isGreaterThan(0)
        }
        Truth.assertWithMessage("Must have focus activity.")
            .that(wmState.focusedActivity).isNotEmpty()
        wmState.rootTasks.forEach { aStack ->
            val stackId = aStack.rootTaskId
            aStack.tasks.forEach { aTask ->
                Truth.assertWithMessage("Stack can only contain its own tasks")
                    .that(stackId).isEqualTo(aTask.rootTaskId)
            }
        }
        Truth.assertWithMessage("Must have front window.")
            .that(wmState.frontWindow).isNotEmpty()
        Truth.assertWithMessage("Must have focused window.")
            .that(wmState.focusedWindow).isNotEmpty()
        Truth.assertWithMessage("Must have app.")
            .that(wmState.focusedApp).isNotEmpty()
    }

    fun hasFocusedActivity(activity: ComponentName): WindowManagerStateSubject = apply {
        val activityComponentName = activity.toActivityName()
        Truth.assertWithMessage("Focused activity invalid")
            .that(activityComponentName)
            .isEqualTo(wmState.focusedActivity)
        Truth.assertWithMessage("Focused app invalid")
            .that(activityComponentName)
            .isEqualTo(wmState.focusedApp)
    }

    fun hasNotFocusedActivity(activity: ComponentName): WindowManagerStateSubject = apply {
        val activityComponentName = activity.toActivityName()
        Truth.assertWithMessage("Has focused activity")
            .that(wmState.focusedActivity)
            .isNotEqualTo(activityComponentName)
        Truth.assertWithMessage("Has focused app")
            .that(wmState.focusedApp)
            .isNotEqualTo(activityComponentName)
    }

    @JvmOverloads
    fun hasFocusedApp(
        activity: ComponentName,
        displayId: Int = Display.DEFAULT_DISPLAY
    ): WindowManagerStateSubject = apply {
        val activityComponentName = activity.toActivityName()
        Truth.assertWithMessage("Focused app invalid")
            .that(activityComponentName)
            .isEqualTo(wmState.getDisplay(displayId)?.focusedApp)
    }

    fun hasResumedActivity(activity: ComponentName): WindowManagerStateSubject = apply {
        val activityComponentName = activity.toActivityName()
        Truth.assertWithMessage("Invalid resumed activity")
            .that(activityComponentName)
            .isEqualTo(wmState.focusedActivity)
    }

    fun hasNotResumedActivity(activity: ComponentName): WindowManagerStateSubject = apply {
        val activityComponentName = activity.toActivityName()
        Truth.assertWithMessage("Has resumed activity")
            .that(wmState.focusedActivity)
            .isNotEqualTo(activityComponentName)
    }

    fun isWindowFocused(windowName: String): WindowManagerStateSubject = apply {
        Truth.assertWithMessage("Invalid focused window")
            .that(windowName)
            .isEqualTo(wmState.focusedWindow)
    }

    fun isWindowNotFocused(windowName: String): WindowManagerStateSubject = apply {
        Truth.assertWithMessage("Has focused window")
            .that(wmState.focusedWindow)
            .isNotEqualTo(windowName)
    }

    fun isVisible(activity: ComponentName): WindowManagerStateSubject =
        hasActivityAndWindowVisibility(activity, visible = true)

    fun isInvisible(activity: ComponentName): WindowManagerStateSubject =
        hasActivityAndWindowVisibility(activity, visible = false)

    private fun hasActivityAndWindowVisibility(
        activity: ComponentName,
        visible: Boolean
    ): WindowManagerStateSubject = apply {
        // Check existence of activity and window.
        val windowName = activity.toWindowName()
        val activityName = activity.toActivityName()
        Truth.assertWithMessage("Activity=$activityName must exist.")
            .that(wmState.containsActivity(activityName)).isTrue()
        Truth.assertWithMessage("Window=$windowName must exist.")
            .that(wmState.containsWindow(windowName)).isTrue()

        // Check visibility of activity and window.
        Truth.assertWithMessage("Activity=$activityName must ${if (visible) "" else " NOT"}" +
            " be visible.")
            .that(visible).isEqualTo(wmState.isActivityVisible(activityName))
        Truth.assertWithMessage("Window=$windowName must ${if (visible) "" else " NOT"}" +
            " have shown surface.")
            .that(visible).isEqualTo(wmState.isWindowSurfaceShown(windowName))
    }

    @JvmOverloads
    fun isHomeActivityVisible(visible: Boolean = true): WindowManagerStateSubject = apply {
        val homeActivity = wmState.homeActivityName
        require(homeActivity != null)
        hasActivityAndWindowVisibility(homeActivity, visible)
    }

    @JvmOverloads
    fun isImeWindowShown(
        displayId: Int = Display.DEFAULT_DISPLAY
    ): WindowManagerStateSubject = apply {
        val imeWinState = wmState.inputMethodWindowState
        Truth.assertWithMessage("IME window must exist")
            .that(imeWinState).isNotNull()
        Truth.assertWithMessage("IME window must be shown")
            .that(imeWinState?.isSurfaceShown ?: false).isTrue()
        Truth.assertWithMessage("IME window must be on the given display")
            .that(displayId).isEqualTo(imeWinState?.displayId ?: -1)
    }

    @JvmOverloads
    fun isImeWindowNotShown(
        displayId: Int = Display.DEFAULT_DISPLAY
    ): WindowManagerStateSubject = apply {
        val imeWinState = wmState.inputMethodWindowState
        Truth.assertWithMessage("IME window must not be shown")
            .that(imeWinState?.isSurfaceShown ?: false).isFalse()
        if (imeWinState?.isSurfaceShown == true) {
            Truth.assertWithMessage("IME window must not be on the given display")
                .that(displayId).isNotEqualTo(imeWinState.displayId)
        }
    }

    private val WindowManagerState.homeActivityName: ComponentName?
        get() {
            val activity = homeActivity ?: return null
            return ComponentName.unflattenFromString(activity.name)
        }

    override fun toString(): String {
        return "WindowManagerStateSubject($wmState)"
    }

    companion object {
        /**
         * Boiler-plate Subject.Factory for WindowManagerStateSubject
         *
         * @param trace containing the entry
         */
        private fun getFactory(
            trace: WindowManagerTraceSubject? = null
        ): Factory<Subject, WindowManagerState> =
            Factory { fm, subject -> WindowManagerStateSubject(fm, subject, trace) }

        /**
         * User-defined entry point
         *
         * @param entry to assert
         * @param trace containing the entry
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            entry: WindowManagerState,
            trace: WindowManagerTraceSubject? = null
        ): WindowManagerStateSubject {
            val strategy = FlickerFailureStrategy()
            val subject = StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                .about(getFactory(trace))
                .that(entry) as WindowManagerStateSubject
            strategy.init(subject)
            return subject
        }
    }
}