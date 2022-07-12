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

import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.assertions.Assertion
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.android.server.wm.flicker.traces.region.RegionSubject
import com.android.server.wm.traces.common.IComponentMatcher
import com.android.server.wm.traces.common.region.Region
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
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
) : FlickerSubject(fm, wmState),
    IWindowManagerSubject<WindowManagerStateSubject, RegionSubject> {
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

    val visibleAppWindows: List<WindowStateSubject>
        get() = subjects.filter { wmState.visibleAppWindows.contains(it.windowState) }

    /**
     * Executes a custom [assertion] on the current subject
     */
    operator fun invoke(assertion: Assertion<WindowManagerState>): WindowManagerStateSubject =
        apply { assertion(this.wmState) }

    /** {@inheritDoc} */
    override fun isEmpty(): WindowManagerStateSubject = apply {
        check("State is empty").that(subjects).isEmpty()
    }

    /** {@inheritDoc} */
    override fun isNotEmpty(): WindowManagerStateSubject = apply {
        check("State is not empty").that(subjects).isNotEmpty()
    }

    /** {@inheritDoc} */
    override fun visibleRegion(componentMatcher: IComponentMatcher?): RegionSubject {
        val selectedWindows = if (componentMatcher == null) {
            // No filters so use all subjects
            subjects
        } else {
            subjects.filter {
                it.windowState != null && componentMatcher.windowMatchesAnyOf(it.windowState)
            }
        }

        if (selectedWindows.isEmpty()) {
            val str = componentMatcher?.toWindowName() ?: "<any>"
            fail(Fact.fact(ASSERTION_TAG, "visibleRegion($str)"),
                    Fact.fact("Could not find windows", str))
        }

        val visibleWindows = selectedWindows.filter { it.isVisible }
        val visibleRegions = visibleWindows
                .mapNotNull { it.windowState?.frameRegion }.toTypedArray()
        return RegionSubject.assertThat(visibleRegions, this, timestamp)
    }

    /** {@inheritDoc} */
    override fun containsAboveAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        contains(aboveAppWindows, componentMatcher)
    }

    /** {@inheritDoc} */
    override fun containsBelowAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        contains(belowAppWindows, componentMatcher)
    }

    /** {@inheritDoc} */
    override fun isAboveWindow(
        aboveWindowComponent: IComponentMatcher,
        belowWindowComponent: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        val aboveWindowTitle = aboveWindowComponent.toWindowName()
        val belowWindowTitle = belowWindowComponent.toWindowName()
        require(aboveWindowTitle != belowWindowTitle)

        contains(aboveWindowComponent)
        contains(belowWindowComponent)

        // windows are ordered by z-order, from top to bottom
        val aboveZ = wmState.windowStates
            .indexOfFirst { aboveWindowComponent.windowMatchesAnyOf(it) }
        val belowZ = wmState.windowStates
            .indexOfFirst { belowWindowComponent.windowMatchesAnyOf(it) }
        if (aboveZ >= belowZ) {
            val aboveWindow = subjects.first {
                it.windowState != null &&
                    aboveWindowComponent.windowMatchesAnyOf(it.windowState)
            }
            val aboveWindowTitleStr = aboveWindowComponent.toWindowName()
            val belowWindowTitleStr = belowWindowComponent.toWindowName()
            aboveWindow.fail(
                Fact.fact(ASSERTION_TAG, "isAboveWindow(" +
                    "above=$aboveWindowTitleStr, " +
                    "below=$belowWindowTitleStr"),
                Fact.fact("Above", aboveWindowTitleStr),
                Fact.fact("Below", belowWindowTitleStr))
        }
    }

    /** {@inheritDoc} */
    override fun containsNonAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        contains(nonAppWindows, componentMatcher)
    }

    /** {@inheritDoc} */
    override fun isAppWindowOnTop(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        if (wmState.visibleAppWindows.isEmpty()) {
            fail(
                Fact.fact(ASSERTION_TAG, "isAppWindowOnTop(${componentMatcher.toWindowName()})"),
                Fact.fact("Not found", "No visible app windows found")
            )
        }
        val topVisibleAppWindow = wmState.topVisibleAppWindow
        if (topVisibleAppWindow == null ||
            !componentMatcher.windowMatchesAnyOf(topVisibleAppWindow)
        ) {
            isNotEmpty()
            val topWindow = subjects.first { it.windowState == topVisibleAppWindow }
            topWindow.fail(
                Fact.fact(ASSERTION_TAG, "isAppWindowOnTop(${componentMatcher.toWindowName()})"),
                Fact.fact("Not on top", componentMatcher.toWindowName()),
                Fact.fact("Found", wmState.topVisibleAppWindow)
            )
        }
    }

    /** {@inheritDoc} */
    override fun isAppWindowNotOnTop(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        val topVisibleAppWindow = wmState.topVisibleAppWindow
        if (topVisibleAppWindow != null &&
            componentMatcher.windowMatchesAnyOf(topVisibleAppWindow)
        ) {
            val topWindow = subjects.first { it.windowState == topVisibleAppWindow }
            topWindow.fail(
                Fact.fact(ASSERTION_TAG, "isAppWindowNotOnTop(${componentMatcher.toWindowName()})"),
                Fact.fact("On top", componentMatcher.toWindowName())
            )
        }
    }

    /** {@inheritDoc} */
    override fun doNotOverlap(
        vararg componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        val repr = componentMatcher.joinToString(", ") { it.toWindowName() }
        verify("Must give more than one window to check! (Given $repr)")
            .that(componentMatcher)
            .hasLength(1)

        componentMatcher.forEach { contains(it) }
        val foundWindows = componentMatcher.toSet()
            .associateWith { act ->
                wmState.windowStates.firstOrNull { act.windowMatchesAnyOf(it) }
            }
            // keep entries only for windows that we actually found by removing nulls
            .filterValues { it != null }
        val foundWindowsRegions = foundWindows
            .mapValues { (_, v) -> v?.frameRegion ?: Region.EMPTY }

        val regions = foundWindowsRegions.entries.toList()
        for (i in regions.indices) {
            val (ourTitle, ourRegion) = regions[i]
            for (j in i + 1 until regions.size) {
                val (otherTitle, otherRegion) = regions[j]
                if (ourRegion.op(otherRegion,
                        Region.Op.INTERSECT)) {
                    val window = foundWindows[ourTitle] ?: error("Window $ourTitle not found")
                    val windowSubject = subjects.first { it.windowState == window }
                    windowSubject.fail(Fact.fact(ASSERTION_TAG,
                            "noWindowsOverlap" +
                                componentMatcher.joinToString { it.toWindowName() }),
                        Fact.fact("Overlap", ourTitle),
                        Fact.fact("Overlap", otherTitle))
                }
            }
        }
    }

    /** {@inheritDoc} */
    override fun containsAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        // Check existence of activity
        val activity = wmState.getActivitiesForWindow(componentMatcher).firstOrNull()
        check("Activity for window ${componentMatcher.toWindowName()} must exist.")
            .that(activity).isNotNull()
        // Check existence of window.
        contains(componentMatcher)
    }

    /** {@inheritDoc} */
    override fun hasRotation(
        rotation: Int,
        displayId: Int
    ): WindowManagerStateSubject = apply {
        check("Rotation should be $rotation")
            .that(rotation)
            .isEqualTo(wmState.getRotation(displayId))
    }

    /** {@inheritDoc} */
    override fun contains(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        contains(subjects, componentMatcher)
    }

    /** {@inheritDoc} */
    override fun notContainsAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        // system components (e.g., NavBar, StatusBar, PipOverlay) don't have a package name
        // nor an activity, ignore them
        check("Activity=${componentMatcher.toActivityName()} must NOT exist.")
            .that(wmState.containsActivity(componentMatcher))
            .isFalse()
        notContains(componentMatcher)
    }

    /** {@inheritDoc} */
    override fun notContains(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        check("Window=${componentMatcher.toWindowName()} must NOT exits.")
            .that(wmState.containsWindow(componentMatcher))
            .isFalse()
    }

    /** {@inheritDoc} */
    override fun isRecentsActivityVisible(): WindowManagerStateSubject = apply {
        if (wmState.isHomeRecentsComponent) {
            isHomeActivityVisible()
        } else {
            check("Recents activity visibility")
                .that(wmState.isRecentsActivityVisible)
                .isTrue()
        }
    }

    /** {@inheritDoc} */
    override fun isRecentsActivityInvisible(): WindowManagerStateSubject = apply {
        if (wmState.isHomeRecentsComponent) {
            isHomeActivityInvisible()
        } else {
            check("Recents activity visibility")
                .that(wmState.isRecentsActivityVisible)
                .isFalse()
        }
    }

    /** {@inheritDoc} */
    @VisibleForTesting
    override fun isValid(): WindowManagerStateSubject = apply {
        check("Must have stacks").that(wmState.stackCount).isGreaterThan(0)
        // TODO: Update when keyguard will be shown on multiple displays
        if (!wmState.keyguardControllerState.isKeyguardShowing) {
            check("There should be at least one resumed activity in the system.")
                .that(wmState.resumedActivitiesCount).isGreaterThan(0)
        }
        check("Must have focus activity.")
            .that(wmState.focusedActivity)
            .isNotNull()
        wmState.rootTasks.forEach { aStack ->
            val stackId = aStack.rootTaskId
            aStack.tasks.forEach { aTask ->
                check("Stack can only contain its own tasks")
                    .that(stackId).isEqualTo(aTask.rootTaskId)
            }
        }
        check("Must have front window.")
            .that(wmState.frontWindow)
            .isNotNull()
        check("Must have focused window.")
            .that(wmState.focusedWindow)
            .isNotNull()
        check("Must have app.")
            .that(wmState.focusedApp)
            .isNotEmpty()
    }

    /** {@inheritDoc} */
    override fun isNonAppWindowVisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        checkWindowVisibility("isVisible", nonAppWindows, componentMatcher, isVisible = true)
    }

    /** {@inheritDoc} */
    override fun isAppWindowVisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        containsAppWindow(componentMatcher)

        // Check existence of activity
        val activity = wmState.getActivitiesForWindow(componentMatcher).firstOrNull()
        // Check visibility of activity and window.
        check("Activity=${activity?.name} must be visible.")
            .that(activity?.isVisible ?: false)
            .isTrue()
        checkWindowVisibility("isVisible", appWindows, componentMatcher, isVisible = true)
    }

    /** {@inheritDoc} */
    override fun hasNoVisibleAppWindow(): WindowManagerStateSubject = apply {
        if (visibleAppWindows.isNotEmpty()) {
            val visibleAppWindows = visibleAppWindows.joinToString { it.name }
            fail(
                Fact.fact(ASSERTION_TAG, "hasNoVisibleAppWindow()"),
                Fact.fact("Found visible windows", visibleAppWindows)
            )
        }
    }

    /** {@inheritDoc} */
    override fun isAppWindowInvisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        // system components (e.g., NavBar, StatusBar, PipOverlay) don't have a package name
        // nor an activity, ignore them
        // activity is visible, check window
        if (wmState.isActivityVisible(componentMatcher)) {
            checkWindowVisibility("isInvisible", appWindows, componentMatcher, isVisible = false)
        }
    }

    /** {@inheritDoc} */
    override fun isNonAppWindowInvisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        checkWindowVisibility("isInvisible", nonAppWindows, componentMatcher, isVisible = false)
    }

    private fun checkWindowVisibility(
        assertionName: String,
        subjectList: List<WindowStateSubject>,
        componentMatcher: IComponentMatcher,
        isVisible: Boolean
    ) {
        // Check existence of window.
        contains(subjectList, componentMatcher)

        val foundWindows = subjectList.filter {
            it.windowState != null && componentMatcher.windowMatchesAnyOf(it.windowState)
        }
        val windowsWithVisibility = foundWindows.filter { it.isVisible == isVisible }

        if (windowsWithVisibility.isEmpty()) {
            val errorTag = if (isVisible) "Is Invisible" else "Is Visible"
            val facts = listOf<Fact>(
                Fact.fact(ASSERTION_TAG, "$assertionName(${componentMatcher.toWindowName()})"),
                Fact.fact(errorTag, componentMatcher.toWindowName())
            )
            foundWindows.first().fail(facts)
        }
    }

    private fun contains(
        subjectList: List<WindowStateSubject>,
        componentMatcher: IComponentMatcher
    ) {
        val windowStates = subjectList.mapNotNull { it.windowState }
        check("Window=${componentMatcher.toWindowName()} must exist.")
            .that(componentMatcher.windowMatchesAnyOf(windowStates))
            .isTrue()
    }

    /** {@inheritDoc} */
    override fun isHomeActivityVisible(): WindowManagerStateSubject = apply {
        val homeIsVisible = wmState.homeActivity?.isVisible ?: false
        check("Home activity doesn't exist").that(wmState.homeActivity).isNotNull()
        check("Home activity is not visible").that(homeIsVisible).isTrue()
    }

    /** {@inheritDoc} */
    override fun isHomeActivityInvisible(): WindowManagerStateSubject = apply {
        val homeIsVisible = wmState.homeActivity?.isVisible ?: false
        check("Home activity is visible").that(homeIsVisible).isFalse()
    }

    /** {@inheritDoc} */
    override fun isPinned(componentMatcher: IComponentMatcher): WindowManagerStateSubject = apply {
        contains(componentMatcher)
        check("Window ${componentMatcher.toWindowName()} in PIP mode")
            .that(wmState.isInPipMode(componentMatcher))
            .isTrue()
    }

    /** {@inheritDoc} */
    override fun isNotPinned(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        contains(componentMatcher)
        check("Window ${componentMatcher.toWindowName()} not in PIP mode")
            .that(wmState.isInPipMode(componentMatcher))
            .isFalse()
    }

    /** {@inheritDoc} */
    override fun isAppSnapshotStartingWindowVisibleFor(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        val activity = wmState.getActivitiesForWindow(componentMatcher).firstOrNull()

        // Check existence and visibility of SnapshotStartingWindow
        val snapshotStartingWindow = activity?.children
            ?.firstOrNull { it.name.startsWith("SnapshotStartingWindow for taskId=") }
        check("SnapshotStartingWindow for Activity=${activity?.name} must be visible.")
            .that(snapshotStartingWindow?.isVisible ?: false).isTrue()
    }

    /** {@inheritDoc} */
    override fun isAboveAppWindowVisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject =
        containsAboveAppWindow(componentMatcher)
            .isNonAppWindowVisible(componentMatcher)

    /** {@inheritDoc} */
    override fun isAboveAppWindowInvisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject =
        containsAboveAppWindow(componentMatcher)
            .isNonAppWindowInvisible(componentMatcher)

    /** {@inheritDoc} */
    override fun isBelowAppWindowVisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject =
        containsBelowAppWindow(componentMatcher)
            .isNonAppWindowVisible(componentMatcher)

    /** {@inheritDoc} */
    override fun isBelowAppWindowInvisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject =
        containsBelowAppWindow(componentMatcher)
            .isNonAppWindowInvisible(componentMatcher)

    /**
     * Obtains the first subject with [WindowState.title] containing [name].
     *
     * Always returns a subject, event when the layer doesn't exist. To verify if layer
     * actually exists in the hierarchy use [WindowStateSubject.exists] or
     * [WindowStateSubject.doesNotExist]
     */
    fun windowState(name: String): WindowStateSubject =
        subjects.firstOrNull {
            it.windowState?.name?.contains(name) == true
        } ?: WindowStateSubject.assertThat(name, this, timestamp)

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
    fun windowState(name: String = "", predicate: (WindowState) -> Boolean): WindowStateSubject =
        subjects.firstOrNull {
            it.windowState?.run { predicate(this) } ?: false
        } ?: WindowStateSubject.assertThat(name, this, timestamp)

    override fun toString(): String {
        return "WindowManagerStateSubject($wmState)"
    }

    companion object {
        /**
         * Boilerplate Subject.Factory for WindowManagerStateSubject
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
