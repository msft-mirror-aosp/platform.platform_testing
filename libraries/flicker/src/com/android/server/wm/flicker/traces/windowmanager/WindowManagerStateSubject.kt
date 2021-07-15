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
import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.assertions.Assertion
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.android.server.wm.flicker.traces.RegionSubject
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.windows.Activity
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
import com.android.server.wm.traces.parser.toActivityName
import com.android.server.wm.traces.parser.toAndroidRegion
import com.android.server.wm.traces.parser.toWindowName
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.FailureStrategy
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

/**
 * Truth subject for [WindowManagerState] objects, used to make assertions over behaviors that
 * occur on a single WM state.
 *
 * To make assertions over a specific state from a trace it is recommended to create a subject
 * using [WindowManagerTraceSubject.assertThat](myTrace) and select the specific state using:
 *     [WindowManagerTraceSubject.first]
 *     [WindowManagerTraceSubject.last]
 *     [WindowManagerTraceSubject.entry]
 *
 * Alternatively, it is also possible to use [WindowManagerStateSubject.assertThat](myState) or
 * Truth.assertAbout([WindowManagerStateSubject.getFactory]), however they will provide less debug
 * information because it uses Truth's default [FailureStrategy].
 *
 * Example:
 *    val trace = WindowManagerTraceParser.parseFromTrace(myTraceFile)
 *    val subject = WindowManagerTraceSubject.assertThat(trace).first()
 *        .contains("ValidWindow")
 *        .notContains("ImaginaryWindow")
 *        .showsAboveAppWindow("NavigationBar")
 *        .invoke { myCustomAssertion(this) }
 */
class WindowManagerStateSubject private constructor(
    fm: FailureMetadata,
    val wmState: WindowManagerState,
    val trace: WindowManagerTraceSubject?,
    override val parent: FlickerSubject?
) : FlickerSubject(fm, wmState) {
    override val timestamp: Long get() = wmState.timestamp
    override val selfFacts = listOf(Fact.fact("Entry", wmState))

    val subjects by lazy {
        wmState.windowStates.map { WindowStateSubject.assertThat(it, this, timestamp) }
    }

    val appWindows: List<WindowStateSubject>
        get() = subjects.filter { wmState.appWindows.contains(it.windowState) }

    val nonAppWindows: List<WindowStateSubject>
        get() = subjects.filter { wmState.nonAppWindows.contains(it.windowState) }

    val aboveAppWindows: List<WindowStateSubject>
        get() = subjects.filter { wmState.aboveAppWindows.contains(it.windowState) }

    val belowAppWindows: List<WindowStateSubject>
        get() = subjects.filter { wmState.belowAppWindows.contains(it.windowState) }

    val visibleWindows: List<WindowStateSubject>
        get() = subjects.filter { wmState.visibleWindows.contains(it.windowState) }

    /**
     * Executes a custom [assertion] on the current subject
     */
    operator fun invoke(assertion: Assertion<WindowManagerState>): WindowManagerStateSubject =
        apply { assertion(this.wmState) }

    /** {@inheritDoc} */
    override fun clone(): FlickerSubject {
        return WindowManagerStateSubject(fm, wmState, trace, parent)
    }

    /**
     * Asserts the current WindowManager state doesn't contain [WindowState]s
     */
    fun isEmpty(): WindowManagerStateSubject = apply {
        check("State is empty").that(subjects).isEmpty()
    }

    /**
     * Asserts the current WindowManager state contains [WindowState]s
     */
    fun isNotEmpty(): WindowManagerStateSubject = apply {
        check("State is not empty").that(subjects).isNotEmpty()
    }

    /**
     * Obtains the region occupied by all windows with name containing any of [component]
     *
     * @param component Component to search
     */
    fun frameRegion(component: ComponentName?): RegionSubject {
        val windowName = component?.toWindowName() ?: ""
        val selectedWindows = subjects.filter { it.name.contains(windowName) }

        if (selectedWindows.isEmpty()) {
            fail("Could not find", windowName)
        }

        val visibleWindows = selectedWindows.filter { it.isVisible }
        val frameRegions = visibleWindows.mapNotNull { it.windowState?.frameRegion }.toTypedArray()
        return RegionSubject.assertThat(frameRegions, this)
    }

    /**
     * Asserts the state contains a [WindowState] with title matching [componentName] whose
     * visibility is [isVisible] above the app windows
     *
     * @param componentName Component to search
     * @param isVisible if the found window should be visible or not
     */
    @JvmOverloads
    fun isAboveAppWindow(
        componentName: ComponentName,
        isVisible: Boolean = true
    ): WindowManagerStateSubject = apply {
        hasWindowVisibility(aboveAppWindows, componentName, isVisible = isVisible)
    }

    /**
     * Asserts the state contains a [WindowState] with title matching [component] whose
     * visibility is [isVisible] below the app windows
     *
     * @param component Component to search
     * @param isVisible if the found window should be visible or not
     */
    @JvmOverloads
    fun isBelowAppWindow(
        component: ComponentName,
        isVisible: Boolean = true
    ): WindowManagerStateSubject = apply {
        hasWindowVisibility(belowAppWindows, component, isVisible = isVisible)
    }

    /**
     * Asserts the state contains [WindowState]s with titles matching [aboveWindow] and
     * [belowWindow], and that [aboveWindow] is above [belowWindow]
     *
     * This assertion can be used, for example, to assert that a PIP window is shown above
     * other apps.
     *
     * @param aboveWindow name of the window that should be above
     * @param belowWindow name of the window that should be below
     */
    fun isAboveWindow(aboveWindow: ComponentName, belowWindow: ComponentName) {
        contains(aboveWindow)
        contains(belowWindow)

        // windows are ordered by z-order, from top to bottom
        val aboveWindowTitle = aboveWindow.toWindowName()
        val belowWindowTitle = belowWindow.toWindowName()
        val aboveZ = wmState.windowStates.indexOfFirst { aboveWindowTitle in it.name }
        val belowZ = wmState.windowStates.indexOfFirst { belowWindowTitle in it.name }
        if (aboveZ >= belowZ) {
            fail("$aboveWindowTitle is above $belowWindowTitle")
        }
    }

    /**
     * Asserts the state contains a non-app [WindowState] with title matching [component] whose
     * visibility is [isVisible]
     *
     * @param component Component to search
     * @param isVisible if the found window should be visible or not
     */
    @JvmOverloads
    fun containsNonAppWindow(
        component: ComponentName,
        isVisible: Boolean = true
    ): WindowManagerStateSubject = apply {
        hasWindowVisibility(nonAppWindows, component, isVisible = isVisible)
    }

    /**
     * Asserts the title of the top visible app window in the state contains [component]
     *
     * @param component Component to search
     */
    fun isAppWindowOnTop(component: ComponentName): WindowManagerStateSubject = apply {
        val windowName = component.toWindowName()
        if (!wmState.topVisibleAppWindow.contains(windowName)) {
            fail(Fact.fact("Not on top", component.toWindowName()),
                    Fact.fact("Found", wmState.topVisibleAppWindow))
        }
    }

    /**
     * Asserts the bounds of the [WindowState]s title matching [component] don't overlap.
     *
     * @param component Component to search
     */
    fun noWindowsOverlap(vararg component: ComponentName): WindowManagerStateSubject = apply {
        component.forEach { contains(it) }
        val foundWindows = component.toSet()
            .associateWith { act ->
                wmState.windowStates.find { it.name.contains(act.toWindowName()) }
            }
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
     * Asserts the state contains an app [WindowState] with title matching [component] whose
     * visibility is [isVisible]
     *
     * @param component Component to search
     * @param isVisible if the found window should be visible or not
     * @param ignoreActivity If the activity check should be ignored or not
     */
    @JvmOverloads
    fun containsAppWindow(
        component: ComponentName,
        isVisible: Boolean = true,
        ignoreActivity: Boolean = false
    ): WindowManagerStateSubject = apply {
        // some component (such as Splash screen) don have a package name, and fail the search
        // for an activity, ignore them
        if (!ignoreActivity && component.packageName.isNotEmpty()) {
            hasActivityVisibility(component, isVisible = isVisible)
        }
        hasWindowVisibility(appWindows, component, isVisible = isVisible)
    }

    /**
     * Asserts the display with id [displayId] has rotation [rotation]
     *
     * @param rotation to assert
     * @param displayId of the target display
     */
    @JvmOverloads
    fun hasRotation(
        rotation: Int,
        displayId: Int = Display.DEFAULT_DISPLAY
    ): WindowManagerStateSubject = apply {
        check("Rotation should be $rotation")
            .that(rotation)
            .isEqualTo(wmState.getRotation(displayId))
    }

    /**
     * Asserts the state contains a [WindowState] nor an [Activity] with title matching
     * [component].
     *
     * @param component Component name to search
     * @param ignoreActivity If the activity check should be ignored or not
     */
    fun contains(
        component: ComponentName,
        ignoreActivity: Boolean = false
    ): WindowManagerStateSubject = apply {
        val windowName = component.toWindowName()
        val activityName = component.toActivityName()
        // system components (e.g., NavBar, StatusBar, PipOverlay) don't have a package name
        // nor an activity, ignore them
        if (!ignoreActivity && component.packageName.isNotEmpty()) {
            check("Activity=$activityName must exist.")
                    .that(wmState.containsActivity(activityName)).isTrue()
        }
        check("Window=$windowName must exist.")
            .that(wmState.containsWindow(windowName)).isTrue()
    }

    /**
     * Asserts the state doesn't contain a [WindowState] nor an [Activity] with title
     * matching [component].
     *
     * @param component Component name to search
     * @param ignoreActivity If the activity check should be ignored or not
     */
    fun notContains(
        component: ComponentName,
        ignoreActivity: Boolean = false
    ): WindowManagerStateSubject = apply {
        val windowName = component.toWindowName()
        val activityName = component.toActivityName()
        // system components (e.g., NavBar, StatusBar, PipOverlay) don't have a package name
        // nor an activity, ignore them
        if (!ignoreActivity && component.className.isNotEmpty()) {
            check("Activity=$activityName must NOT exist.")
                    .that(wmState.containsActivity(activityName)).isFalse()
        }
        check("Window=$windowName must NOT exits.")
            .that(wmState.containsWindow(windowName)).isFalse()
    }

    @JvmOverloads
    fun isRecentsActivityVisible(visible: Boolean = true): WindowManagerStateSubject = apply {
        if (wmState.isHomeRecentsComponent) {
            isHomeActivityVisible()
        } else {
            check("Recents activity is ${if (visible) "" else "not"} visible")
                .that(wmState.isRecentsActivityVisible)
                .isEqualTo(visible)
        }
    }

    /**
     * Asserts the state is valid, that is, if it has:
     *   - a resumed activity
     *   - a focused activity
     *   - a focused window
     *   - a front window
     *   - a focused app
     */
    @VisibleForTesting
    fun isValid(): WindowManagerStateSubject = apply {
        check("Must have stacks").that(wmState.stackCount).isGreaterThan(0)
        // TODO: Update when keyguard will be shown on multiple displays
        if (!wmState.keyguardControllerState.isKeyguardShowing) {
            check("There should be at least one resumed activity in the system.")
                .that(wmState.resumedActivitiesCount).isGreaterThan(0)
        }
        check("Must have focus activity.")
            .that(wmState.focusedActivity).isNotEmpty()
        wmState.rootTasks.forEach { aStack ->
            val stackId = aStack.rootTaskId
            aStack.tasks.forEach { aTask ->
                check("Stack can only contain its own tasks")
                    .that(stackId).isEqualTo(aTask.rootTaskId)
            }
        }
        check("Must have front window.")
            .that(wmState.frontWindow).isNotEmpty()
        check("Must have focused window.")
            .that(wmState.focusedWindow).isNotEmpty()
        check("Must have app.")
            .that(wmState.focusedApp).isNotEmpty()
    }

    /**
     * Asserts the state contains a visible window with [WindowState.title] matching [component].
     *
     * Also, if [component] has a package name (i.e., is not a system component), also checks that
     * it contains a visible [Activity] with [Activity.title] matching [component].
     *
     * @param component Component name to search
     * @param ignoreActivity If the activity check should be ignored or not
     */
    fun isVisible(
        component: ComponentName,
        ignoreActivity: Boolean = false
    ): WindowManagerStateSubject = apply {
        // system components (e.g., NavBar, StatusBar, PipOverlay) don't have a package name
        // nor an activity, ignore them
        if (!ignoreActivity && component.packageName.isNotEmpty()) {
            hasActivityVisibility(component, isVisible = true)
        }
        hasWindowVisibility(subjects, component, isVisible = true)
    }

    /**
     * Asserts the state contains an invisible window with [WindowState.title] matching [component].
     *
     * Also, if [component] has a package name (i.e., is not a system component), also checks that
     * it contains an invisible [Activity] with [Activity.title] matching [component].
     *
     * @param component Component name to search
     * @param ignoreActivity If the activity check should be ignored or not
     */
    fun isInvisible(
        component: ComponentName,
        ignoreActivity: Boolean = false
    ): WindowManagerStateSubject = apply {
        // system components (e.g., NavBar, StatusBar, PipOverlay) don't have a package name
        // nor an activity, ignore them
        if (!ignoreActivity && component.packageName.isNotEmpty()) {
            hasActivityVisibility(component, isVisible = false)
        }
        hasWindowVisibility(subjects, component, isVisible = false)
    }

    private fun hasActivityVisibility(component: ComponentName, isVisible: Boolean) {
        // Check existence of activity
        val activityName = component.toActivityName()
        check("Activity=$activityName must exist.")
                .that(wmState.containsActivity(activityName)).isTrue()

        // Check visibility of activity and window.
        check("Activity=$activityName must${if (isVisible) "" else " NOT"} be visible.")
                .that(isVisible).isEqualTo(wmState.isActivityVisible(activityName))
    }

    private fun hasWindowVisibility(
        subjectList: List<WindowStateSubject>,
        component: ComponentName,
        isVisible: Boolean
    ) {
        // Check existence of window.
        contains(component)

        val windowName = component.toWindowName()
        val foundWindows = subjectList.filter { it.name.contains(windowName) }
        val windowsWithVisibility = foundWindows.filter { it.isVisible == isVisible }

        if (windowsWithVisibility.isEmpty()) {
            if (isVisible) {
                foundWindows.first().fail("Is Invisible", windowName)
            } else {
                foundWindows.first().fail("Is Visible", windowName)
            }
        }
    }

    /**
     * Asserts the state home activity visibility is equal to [isVisible]
     *
     * @param isVisible expected home activity visibility
     */
    @JvmOverloads
    fun isHomeActivityVisible(isVisible: Boolean = true): WindowManagerStateSubject = apply {
        val homeIsVisible = wmState.homeActivity?.isVisible ?: false
        if (isVisible) {
            check("Home activity doesn't exist").that(wmState.homeActivity).isNotNull()
            check("Home activity is not visible").that(homeIsVisible).isTrue()
        } else {
            check("Home activity is visible").that(homeIsVisible).isFalse()
        }
    }

    /**
     * Asserts that [component] exists and is pinned (in PIP mode)
     *
     * @param component Component name to search
     */
    fun isPinned(component: ComponentName): WindowManagerStateSubject = apply {
        contains(component)
        val windowName = component.toWindowName()
        val pinnedWindows = wmState.pinnedWindows.map { it.title }
        check("Window not in PIP mode").that(pinnedWindows).contains(windowName)
    }

    /**
     * Asserts that [component] exists and is not pinned (not in PIP mode)
     *
     * @param component Component name to search
     */
    fun isNotPinned(component: ComponentName): WindowManagerStateSubject = apply {
        contains(component)
        val windowName = component.toWindowName()
        val pinnedWindows = wmState.pinnedWindows.map { it.title }
        check("Window not in PIP mode").that(pinnedWindows).doesNotContain(windowName)
    }

    /**
     * Obtains the first subject with [WindowState.title] containing [name].
     *
     * Always returns a subject, event when the layer doesn't exist. To verify if layer
     * actually exists in the hierarchy use [WindowStateSubject.exists] or
     * [WindowStateSubject.doesNotExist]
     */
    fun windowState(name: String): WindowStateSubject {
        return subjects.firstOrNull {
            it.windowState?.name?.contains(name) == true
        } ?: WindowStateSubject.assertThat(name, this, timestamp)
    }

    /**
     * Obtains the first subject matching  [predicate].
     *
     * Always returns a subject, event when the layer doesn't exist. To verify if layer
     * actually exists in the hierarchy use [WindowStateSubject.exists] or
     * [WindowStateSubject.doesNotExist]
     *
     * @param predicate to search for a subject
     * @param name Name of the subject to use when not found (optional)
     */
    fun windowState(name: String = "", predicate: (WindowState) -> Boolean): WindowStateSubject {
        return subjects.firstOrNull {
            it.windowState?.run { predicate(this) } ?: false
        } ?: WindowStateSubject.assertThat(name, this, timestamp)
    }

    override fun toString(): String {
        return "WindowManagerStateSubject($wmState)"
    }

    companion object {
        /**
         * Boiler-plate Subject.Factory for WindowManagerStateSubject
         *
         * @param parent containing the entry
         */
        private fun getFactory(
            trace: WindowManagerTraceSubject?,
            parent: FlickerSubject?
        ): Factory<Subject, WindowManagerState> =
            Factory { fm, subject -> WindowManagerStateSubject(fm, subject, trace, parent) }

        /**
         * User-defined entry point
         *
         * @param entry to assert
         * @param parent containing the entry
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            entry: WindowManagerState,
            trace: WindowManagerTraceSubject? = null,
            parent: FlickerSubject? = null
        ): WindowManagerStateSubject {
            val strategy = FlickerFailureStrategy()
            val subject = StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                .about(getFactory(trace, parent))
                .that(entry) as WindowManagerStateSubject
            strategy.init(subject)
            return subject
        }
    }
}