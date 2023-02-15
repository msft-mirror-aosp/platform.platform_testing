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
import com.android.server.wm.flicker.assertions.Fact
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.region.RegionSubject
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.component.matchers.ComponentNameMatcher
import com.android.server.wm.traces.common.component.matchers.IComponentMatcher
import com.android.server.wm.traces.common.region.Region
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.windows.WindowState

/**
 * Subject for [WindowManagerState] objects, used to make assertions over behaviors that occur on a
 * single WM state.
 *
 * To make assertions over a specific state from a trace it is recommended to create a subject using
 * [WindowManagerTraceSubject](myTrace) and select the specific state using:
 * ```
 *     [WindowManagerTraceSubject.first]
 *     [WindowManagerTraceSubject.last]
 *     [WindowManagerTraceSubject.entry]
 * ```
 * Alternatively, it is also possible to use [WindowManagerStateSubject](myState).
 *
 * Example:
 * ```
 *    val trace = WindowManagerTraceParser().parse(myTraceFile)
 *    val subject = WindowManagerTraceSubject(trace).first()
 *        .contains("ValidWindow")
 *        .notContains("ImaginaryWindow")
 *        .showsAboveAppWindow("NavigationBar")
 *        .invoke { myCustomAssertion(this) }
 * ```
 */
class WindowManagerStateSubject(
    val wmState: WindowManagerState,
    val trace: WindowManagerTraceSubject? = null,
    override val parent: FlickerSubject? = null
) : FlickerSubject(), IWindowManagerSubject<WindowManagerStateSubject, RegionSubject> {
    override val timestamp: Timestamp
        get() = wmState.timestamp
    override val selfFacts = listOf(Fact("WM State", wmState))

    val subjects by lazy { wmState.windowStates.map { WindowStateSubject(this, timestamp, it) } }

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

    /** Executes a custom [assertion] on the current subject */
    operator fun invoke(assertion: Assertion<WindowManagerState>): WindowManagerStateSubject =
        apply {
            assertion(this.wmState)
        }

    /** {@inheritDoc} */
    override fun isEmpty(): WindowManagerStateSubject = apply {
        check { "WM state is empty" }.that(subjects.isEmpty()).isEqual(true)
    }

    /** {@inheritDoc} */
    override fun isNotEmpty(): WindowManagerStateSubject = apply {
        check { "WM state is not empty" }.that(subjects.isEmpty()).isEqual(false)
    }

    /** {@inheritDoc} */
    override fun visibleRegion(componentMatcher: IComponentMatcher?): RegionSubject {
        val selectedWindows =
            if (componentMatcher == null) {
                // No filters so use all subjects
                subjects
            } else {
                subjects.filter {
                    it.windowState != null && componentMatcher.windowMatchesAnyOf(it.windowState)
                }
            }

        if (selectedWindows.isEmpty()) {
            val str = componentMatcher?.toWindowIdentifier() ?: "<any>"
            fail(Fact(ASSERTION_TAG, "visibleRegion($str)"), Fact("Could not find windows", str))
        }

        val visibleWindows = selectedWindows.filter { it.isVisible }
        val visibleRegions =
            visibleWindows.mapNotNull { it.windowState?.frameRegion }.toTypedArray()
        return RegionSubject(visibleRegions, this, timestamp)
    }

    /** {@inheritDoc} */
    override fun containsAboveAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply { contains(aboveAppWindows, componentMatcher) }

    /** {@inheritDoc} */
    override fun containsBelowAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply { contains(belowAppWindows, componentMatcher) }

    /** {@inheritDoc} */
    override fun isAboveWindow(
        aboveWindowComponentMatcher: IComponentMatcher,
        belowWindowComponentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        contains(aboveWindowComponentMatcher)
        contains(belowWindowComponentMatcher)

        val aboveWindow =
            wmState.windowStates.first { aboveWindowComponentMatcher.windowMatchesAnyOf(it) }
        val belowWindow =
            wmState.windowStates.first { belowWindowComponentMatcher.windowMatchesAnyOf(it) }
        if (aboveWindow == belowWindow) {
            fail(
                Fact(
                    ASSERTION_TAG,
                    "Above and below windows should be different. " +
                        "Instead they were ${aboveWindow.title} " +
                        "(matched with ${aboveWindowComponentMatcher.toWindowIdentifier()} and " +
                        "${belowWindowComponentMatcher.toWindowIdentifier()})"
                )
            )
        }

        // windows are ordered by z-order, from top to bottom
        val aboveZ =
            wmState.windowStates.indexOfFirst { aboveWindowComponentMatcher.windowMatchesAnyOf(it) }
        val belowZ =
            wmState.windowStates.indexOfFirst { belowWindowComponentMatcher.windowMatchesAnyOf(it) }
        if (aboveZ >= belowZ) {
            val matchedAboveWindow =
                subjects.first {
                    it.windowState != null &&
                        aboveWindowComponentMatcher.windowMatchesAnyOf(it.windowState)
                }
            val aboveWindowTitleStr = aboveWindowComponentMatcher.toWindowIdentifier()
            val belowWindowTitleStr = belowWindowComponentMatcher.toWindowIdentifier()
            matchedAboveWindow.fail(
                Fact(
                    ASSERTION_TAG,
                    "isAboveWindow(above=$aboveWindowTitleStr, below=$belowWindowTitleStr"
                ),
                Fact("Above", aboveWindowTitleStr),
                Fact("Below", belowWindowTitleStr)
            )
        }
    }

    /** {@inheritDoc} */
    override fun containsNonAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply { contains(nonAppWindows, componentMatcher) }

    /** {@inheritDoc} */
    override fun isAppWindowOnTop(componentMatcher: IComponentMatcher): WindowManagerStateSubject =
        apply {
            if (wmState.visibleAppWindows.isEmpty()) {
                fail(
                    Fact(
                        ASSERTION_TAG,
                        "isAppWindowOnTop(${componentMatcher.toWindowIdentifier()})"
                    ),
                    Fact("Not found", "No visible app windows found")
                )
            }
            val topVisibleAppWindow = wmState.topVisibleAppWindow
            if (
                topVisibleAppWindow == null ||
                    !componentMatcher.windowMatchesAnyOf(topVisibleAppWindow)
            ) {
                isNotEmpty()
                val topWindow = subjects.first { it.windowState == topVisibleAppWindow }
                topWindow.fail(
                    Fact(
                        ASSERTION_TAG,
                        "isAppWindowOnTop(${componentMatcher.toWindowIdentifier()})"
                    ),
                    Fact("Not on top", componentMatcher.toWindowIdentifier()),
                    Fact("Found", wmState.topVisibleAppWindow)
                )
            }
        }

    /** {@inheritDoc} */
    override fun isAppWindowNotOnTop(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        val topVisibleAppWindow = wmState.topVisibleAppWindow
        if (
            topVisibleAppWindow != null && componentMatcher.windowMatchesAnyOf(topVisibleAppWindow)
        ) {
            val topWindow = subjects.first { it.windowState == topVisibleAppWindow }
            topWindow.fail(
                Fact(
                    ASSERTION_TAG,
                    "isAppWindowNotOnTop(${componentMatcher.toWindowIdentifier()})"
                ),
                Fact("On top", componentMatcher.toWindowIdentifier())
            )
        }
    }

    /** {@inheritDoc} */
    override fun doNotOverlap(
        vararg componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        check {
                val repr = componentMatcher.joinToString(", ") { it.toWindowIdentifier() }
                "Must give more than one window to check! (Given $repr)"
            }
            .that(componentMatcher.size)
            .isEqual(1)

        componentMatcher.forEach { contains(it) }
        val foundWindows =
            componentMatcher
                .toSet()
                .associateWith { act ->
                    wmState.windowStates.firstOrNull { act.windowMatchesAnyOf(it) }
                }
                // keep entries only for windows that we actually found by removing nulls
                .filterValues { it != null }
        val foundWindowsRegions =
            foundWindows.mapValues { (_, v) -> v?.frameRegion ?: Region.EMPTY }

        val regions = foundWindowsRegions.entries.toList()
        for (i in regions.indices) {
            val (ourTitle, ourRegion) = regions[i]
            for (j in i + 1 until regions.size) {
                val (otherTitle, otherRegion) = regions[j]
                if (ourRegion.op(otherRegion, Region.Op.INTERSECT)) {
                    val window = foundWindows[ourTitle] ?: error("Window $ourTitle not found")
                    val windowSubject = subjects.first { it.windowState == window }
                    windowSubject.fail(
                        Fact(
                            ASSERTION_TAG,
                            "noWindowsOverlap" +
                                componentMatcher.joinToString { it.toWindowIdentifier() }
                        ),
                        Fact("Overlap", ourTitle),
                        Fact("Overlap", otherTitle)
                    )
                }
            }
        }
    }

    /** {@inheritDoc} */
    override fun containsAppWindow(componentMatcher: IComponentMatcher): WindowManagerStateSubject =
        apply {
            // Check existence of activity
            val activity = wmState.getActivitiesForWindow(componentMatcher).firstOrNull()
            assert(activity != null) {
                "Activity exists for window ${componentMatcher.toWindowIdentifier()}"
            }
            // Check existence of window.
            contains(componentMatcher)
        }

    /** {@inheritDoc} */
    override fun hasRotation(
        rotation: PlatformConsts.Rotation,
        displayId: Int
    ): WindowManagerStateSubject = apply {
        assert(rotation == wmState.getRotation(displayId)) { "Rotation is $rotation" }
    }

    /** {@inheritDoc} */
    override fun contains(componentMatcher: IComponentMatcher): WindowManagerStateSubject = apply {
        contains(subjects, componentMatcher)
    }

    /** {@inheritDoc} */
    override fun notContainsAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        // system components (e.g., NavBar, StatusBar, PipOverlay) don't have a package name
        // nor an activity, ignore them
        check { "Activity '${componentMatcher.toActivityIdentifier()}' does not exist" }
            .that(wmState.containsActivity(componentMatcher))
            .isEqual(false)
        notContains(componentMatcher)
    }

    /** {@inheritDoc} */
    override fun notContains(componentMatcher: IComponentMatcher): WindowManagerStateSubject =
        apply {
            check { "Window '${componentMatcher.toWindowIdentifier()}' does not exist" }
                .that(wmState.containsWindow(componentMatcher))
                .isEqual(false)
        }

    /** {@inheritDoc} */
    override fun isRecentsActivityVisible(): WindowManagerStateSubject = apply {
        if (wmState.isHomeRecentsComponent) {
            isHomeActivityVisible()
        } else {
            check { "Recents activity is visible" }
                .that(wmState.isRecentsActivityVisible)
                .isEqual(true)
        }
    }

    /** {@inheritDoc} */
    override fun isRecentsActivityInvisible(): WindowManagerStateSubject = apply {
        if (wmState.isHomeRecentsComponent) {
            isHomeActivityInvisible()
        } else {
            check { "Recents activity is not visible" }
                .that(wmState.isRecentsActivityVisible)
                .isEqual(false)
        }
    }

    /** {@inheritDoc} */
    @VisibleForTesting
    override fun isValid(): WindowManagerStateSubject = apply {
        check { "Stacks count" }.that(wmState.stackCount).isGreater(0)
        // TODO: Update when keyguard will be shown on multiple displays
        if (!wmState.keyguardControllerState.isKeyguardShowing) {
            check { "Resumed activity" }.that(wmState.resumedActivitiesCount).isGreater(0)
        }
        check { "No focused activity" }.that(wmState.focusedActivity).isNotEqual(null)
        wmState.rootTasks.forEach { aStack ->
            val stackId = aStack.rootTaskId
            aStack.tasks.forEach { aTask ->
                check { "Root task Id for stack $aTask" }.that(stackId).isEqual(aTask.rootTaskId)
            }
        }
        check { "Front window" }.that(wmState.frontWindow).isNotEqual(null)
        check { "Focused window" }.that(wmState.focusedWindow).isNotEqual(null)
        check { "Focused app" }.that(wmState.focusedApp.isNotEmpty()).isEqual(true)
    }

    /** {@inheritDoc} */
    override fun isNonAppWindowVisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply { checkWindowIsVisible(nonAppWindows, componentMatcher) }

    /** {@inheritDoc} */
    override fun isAppWindowVisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        containsAppWindow(componentMatcher)
        checkWindowIsVisible(appWindows, componentMatcher)
    }

    /** {@inheritDoc} */
    override fun hasNoVisibleAppWindow(): WindowManagerStateSubject = apply {
        if (visibleAppWindows.isNotEmpty()) {
            val visibleAppWindows = visibleAppWindows.joinToString { it.name }
            fail(
                Fact(ASSERTION_TAG, "hasNoVisibleAppWindow()"),
                Fact("Found visible windows", visibleAppWindows)
            )
        }
    }

    /** {@inheritDoc} */
    override fun isKeyguardShowing(): WindowManagerStateSubject = apply {
        if (!wmState.isKeyguardShowing && !wmState.isAodShowing) {
            fail(
                Fact(ASSERTION_TAG, "isKeyguardShowing()"),
                Fact("Keyguard showing", wmState.isKeyguardShowing)
            )
        }
    }

    /** {@inheritDoc} */
    override fun isAppWindowInvisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply { checkWindowIsInvisible(appWindows, componentMatcher) }

    /** {@inheritDoc} */
    override fun isNonAppWindowInvisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply { checkWindowIsInvisible(nonAppWindows, componentMatcher) }

    private fun checkWindowIsVisible(
        subjectList: List<WindowStateSubject>,
        componentMatcher: IComponentMatcher
    ) {
        // Check existence of window.
        contains(subjectList, componentMatcher)

        val foundWindows =
            subjectList.filter {
                it.windowState != null && componentMatcher.windowMatchesAnyOf(it.windowState)
            }

        val visibleWindows =
            wmState.visibleWindows.filter { visibleWindow ->
                foundWindows.any { it.windowState == visibleWindow }
            }

        if (visibleWindows.isEmpty()) {
            val windowId = componentMatcher.toWindowIdentifier()
            val facts =
                listOf(Fact(ASSERTION_TAG, "isVisible($windowId)"), Fact("Is Invisible", windowId))
            foundWindows.first().fail(facts)
        }
    }

    private fun checkWindowIsInvisible(
        subjectList: List<WindowStateSubject>,
        componentMatcher: IComponentMatcher
    ) {
        // Check existence of window.
        contains(subjectList, componentMatcher)

        val foundWindows =
            subjectList.filter {
                it.windowState != null && componentMatcher.windowMatchesAnyOf(it.windowState)
            }

        val visibleWindows =
            wmState.visibleWindows.filter { visibleWindow ->
                foundWindows.any { it.windowState == visibleWindow }
            }

        if (visibleWindows.isNotEmpty()) {
            val windowId = componentMatcher.toWindowIdentifier()
            val facts =
                listOf(Fact(ASSERTION_TAG, "isInvisible($windowId)"), Fact("Is Visible", windowId))
            foundWindows.first { it.windowState == visibleWindows.first() }.fail(facts)
        }
    }

    private fun contains(
        subjectList: List<WindowStateSubject>,
        componentMatcher: IComponentMatcher
    ) {
        val windowStates = subjectList.mapNotNull { it.windowState }
        check { "Window '${componentMatcher.toWindowIdentifier()}' exists" }
            .that(componentMatcher.windowMatchesAnyOf(windowStates))
            .isEqual(true)
    }

    /** {@inheritDoc} */
    override fun isHomeActivityVisible(): WindowManagerStateSubject = apply {
        val homeIsVisible = wmState.homeActivity?.isVisible ?: false
        check { "Home activity exists" }.that(wmState.homeActivity).isNotEqual(null)
        check { "Home activity is visible" }.that(homeIsVisible).isEqual(true)
    }

    /** {@inheritDoc} */
    override fun isHomeActivityInvisible(): WindowManagerStateSubject = apply {
        val homeIsVisible = wmState.homeActivity?.isVisible ?: false
        check { "Home activity is not visible" }.that(homeIsVisible).isEqual(false)
    }

    /** {@inheritDoc} */
    override fun isFocusedApp(app: String): WindowManagerStateSubject = apply {
        check { "Window is focused app $app" }.that(wmState.focusedApp).isEqual(app)
    }

    /** {@inheritDoc} */
    override fun isNotFocusedApp(app: String): WindowManagerStateSubject = apply {
        check { "Window is not focused app $app" }.that(wmState.focusedApp).isNotEqual(app)
    }

    /** {@inheritDoc} */
    override fun isPinned(componentMatcher: IComponentMatcher): WindowManagerStateSubject = apply {
        contains(componentMatcher)
        check { "Window is pinned ${componentMatcher.toWindowIdentifier()}" }
            .that(wmState.isInPipMode(componentMatcher))
            .isEqual(true)
    }

    /** {@inheritDoc} */
    override fun isNotPinned(componentMatcher: IComponentMatcher): WindowManagerStateSubject =
        apply {
            contains(componentMatcher)
            check { "Window is pinned ${componentMatcher.toWindowIdentifier()}" }
                .that(wmState.isInPipMode(componentMatcher))
                .isEqual(false)
        }

    /** {@inheritDoc} */
    override fun isAppSnapshotStartingWindowVisibleFor(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject = apply {
        val activity = wmState.getActivitiesForWindow(componentMatcher).firstOrNull()
        requireNotNull(activity) { "Activity for $componentMatcher not found" }

        // Check existence and visibility of SnapshotStartingWindow
        val snapshotStartingWindow =
            activity.getWindows(ComponentNameMatcher.SNAPSHOT).firstOrNull()

        check { "SnapshotStartingWindow does not exist for activity ${activity.name}" }
            .that(snapshotStartingWindow)
            .isNotEqual(null)
        check { "Activity is visible" }.that(activity.isVisible).isEqual(true)
        check { "SnapshotStartingWindow is visible" }
            .that(snapshotStartingWindow?.isVisible ?: false)
            .isEqual(true)
    }

    /** {@inheritDoc} */
    override fun isAboveAppWindowVisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject =
        containsAboveAppWindow(componentMatcher).isNonAppWindowVisible(componentMatcher)

    /** {@inheritDoc} */
    override fun isAboveAppWindowInvisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject =
        containsAboveAppWindow(componentMatcher).isNonAppWindowInvisible(componentMatcher)

    /** {@inheritDoc} */
    override fun isBelowAppWindowVisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject =
        containsBelowAppWindow(componentMatcher).isNonAppWindowVisible(componentMatcher)

    /** {@inheritDoc} */
    override fun isBelowAppWindowInvisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerStateSubject =
        containsBelowAppWindow(componentMatcher).isNonAppWindowInvisible(componentMatcher)

    /**
     * Obtains the first subject with [WindowState.title] containing [name].
     *
     * Always returns a subject, event when the layer doesn't exist. To verify if layer actually
     * exists in the hierarchy use [WindowStateSubject.exists] or [WindowStateSubject.doesNotExist]
     */
    fun windowState(name: String): WindowStateSubject =
        subjects.firstOrNull { it.windowState?.name?.contains(name) == true }
            ?: WindowStateSubject(this, timestamp, null, name)

    /**
     * Obtains the first subject matching [predicate].
     *
     * Always returns a subject, event when the layer doesn't exist. To verify if layer actually
     * exists in the hierarchy use [WindowStateSubject.exists] or [WindowStateSubject.doesNotExist]
     *
     * @param predicate to search for a subject
     * @param name Name of the subject to use when not found (optional)
     */
    fun windowState(name: String = "", predicate: (WindowState) -> Boolean): WindowStateSubject =
        subjects.firstOrNull { it.windowState?.run { predicate(this) } ?: false }
            ?: WindowStateSubject(this, timestamp, null, name)

    override fun toString(): String {
        return "WindowManagerStateSubject($wmState)"
    }
}
