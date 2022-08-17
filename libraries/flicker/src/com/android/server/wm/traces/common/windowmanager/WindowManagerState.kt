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

package com.android.server.wm.traces.common.windowmanager

import com.android.server.wm.traces.common.IComponentMatcher
import com.android.server.wm.traces.common.ITraceEntry
import com.android.server.wm.traces.common.prettyTimestamp
import com.android.server.wm.traces.common.windowmanager.windows.Activity
import com.android.server.wm.traces.common.windowmanager.windows.DisplayContent
import com.android.server.wm.traces.common.windowmanager.windows.KeyguardControllerState
import com.android.server.wm.traces.common.windowmanager.windows.RootWindowContainer
import com.android.server.wm.traces.common.windowmanager.windows.Task
import com.android.server.wm.traces.common.windowmanager.windows.TaskFragment
import com.android.server.wm.traces.common.windowmanager.windows.WindowContainer
import com.android.server.wm.traces.common.windowmanager.windows.WindowManagerPolicy
import com.android.server.wm.traces.common.windowmanager.windows.WindowState

/**
 * Represents a single WindowManager trace entry.
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot
 * access internal Java/Android functionality
 *
 * The timestamp constructor must be a string due to lack of Kotlin/KotlinJS Long compatibility
 *
 **/
class WindowManagerState(
    val where: String,
    val policy: WindowManagerPolicy?,
    val focusedApp: String,
    val focusedDisplayId: Int,
    private val _focusedWindow: String,
    val inputMethodWindowAppToken: String,
    val isHomeRecentsComponent: Boolean,
    val isDisplayFrozen: Boolean,
    private val _pendingActivities: Array<String>,
    val root: RootWindowContainer,
    val keyguardControllerState: KeyguardControllerState,
    _timestamp: String = "0"
) : ITraceEntry {
    override val timestamp: Long = _timestamp.toLong()
    val isVisible: Boolean = true
    val stableId: String get() = this::class.simpleName ?: error("Unable to determine class")
    val name: String get() = prettyTimestamp(timestamp)
    val isTablet: Boolean get() = displays.any { it.isTablet }

    val windowContainers: Array<WindowContainer>
        get() = root.collectDescendants()

    val children: Array<WindowContainer>
        get() = root.children.reversedArray()

    /**
     * Displays in z-order with the top most at the front of the list, starting with primary.
     */
    val displays: Array<DisplayContent>
        get() = windowContainers.filterIsInstance<DisplayContent>().toTypedArray()

    /**
     * Root tasks in z-order with the top most at the front of the list, starting with primary display.
     */
    val rootTasks: Array<Task>
        get() = displays.flatMap { it.rootTasks.toList() }.toTypedArray()

    /**
     * TaskFragments in z-order with the top most at the front of the list.
      */
    val taskFragments: Array<TaskFragment>
        get() = windowContainers.filterIsInstance<TaskFragment>().toTypedArray()

    /**
     *Windows in z-order with the top most at the front of the list.
      */
    val windowStates: Array<WindowState>
        get() = windowContainers.filterIsInstance<WindowState>().toTypedArray()

    @Deprecated("Please use windowStates instead", replaceWith = ReplaceWith("windowStates"))
    val windows: Array<WindowState>
        get() = windowStates

    val appWindows: Array<WindowState>
        get() = windowStates.filter { it.isAppWindow }.toTypedArray()
    val nonAppWindows: Array<WindowState>
        get() = windowStates.filterNot { it.isAppWindow }.toTypedArray()
    val aboveAppWindows: Array<WindowState>
        get() = windowStates.takeWhile { !appWindows.contains(it) }.toTypedArray()
    val belowAppWindows: Array<WindowState>
        get() = windowStates
            .dropWhile { !appWindows.contains(it) }.drop(appWindows.size).toTypedArray()
    val visibleWindows: Array<WindowState>
        get() = windowStates
            .filter { it.isVisible }
            .filter { window ->
                val activities = getActivitiesForWindow(window)
                val activity = activities.firstOrNull { it.children.contains(window) }
                activity?.isVisible ?: true
            }
            .toTypedArray()
    val visibleAppWindows: Array<WindowState>
        get() = visibleWindows.filter { it.isAppWindow }.toTypedArray()
    val topVisibleAppWindow: WindowState?
        get() = visibleAppWindows.firstOrNull()
    val pinnedWindows: Array<WindowState>
        get() = visibleWindows
            .filter { it.windowingMode == WINDOWING_MODE_PINNED }
            .toTypedArray()
    val pendingActivities: Array<Activity> get() =
        _pendingActivities
            .mapNotNull { getActivity(it) }
            .toTypedArray()
    val focusedWindow: WindowState? get() =
        visibleWindows.firstOrNull { it.name == _focusedWindow }

    /**
     * Checks if the device state supports rotation, i.e., if the rotation sensor is
     * enabled (e.g., launcher) and if the rotation not fixed
     */
    val canRotate: Boolean
        get() = policy?.isFixedOrientation != true && policy?.isOrientationNoSensor != true
    val focusedDisplay: DisplayContent? get() = getDisplay(focusedDisplayId)
    val focusedStackId: Int get() = focusedDisplay?.focusedRootTaskId ?: -1
    val focusedActivity: Activity? get() {
        val focusedDisplay = focusedDisplay
        val focusedWindow = focusedWindow
        return when {
            focusedDisplay != null && focusedDisplay.resumedActivity.isNotEmpty() ->
                getActivity(focusedDisplay.resumedActivity)
            focusedWindow != null ->
                getActivitiesForWindow(focusedWindow, focusedDisplayId).firstOrNull()
            else -> null
        }
    }
    val resumedActivities: Array<Activity>
        get() = rootTasks.flatMap { it.resumedActivities.toList() }
            .mapNotNull { getActivity(it) }
            .toTypedArray()
    val resumedActivitiesCount: Int get() = resumedActivities.size
    val stackCount: Int get() = rootTasks.size
    val homeTask: Task? get() = getStackByActivityType(ACTIVITY_TYPE_HOME)?.topTask
    val recentsTask: Task? get() = getStackByActivityType(ACTIVITY_TYPE_RECENTS)?.topTask
    val homeActivity: Activity? get() = homeTask?.activities?.lastOrNull()
    val isHomeActivityVisible: Boolean get() {
        val activity = homeActivity
        return activity != null && activity.isVisible
    }
    val recentsActivity: Activity? get() = recentsTask?.activities?.lastOrNull()
    val isRecentsActivityVisible: Boolean get() {
        val activity = recentsActivity
        return activity != null && activity.isVisible
    }
    val frontWindow: WindowState? get() = windowStates.firstOrNull()
    val inputMethodWindowState: WindowState?
        get() = getWindowStateForAppToken(inputMethodWindowAppToken)

    fun getDefaultDisplay(): DisplayContent? =
        displays.firstOrNull { it.id == DEFAULT_DISPLAY }

    fun getDisplay(displayId: Int): DisplayContent? =
        displays.firstOrNull { it.id == displayId }

    fun countStacks(windowingMode: Int, activityType: Int): Int {
        var count = 0
        for (stack in rootTasks) {
            if (activityType != ACTIVITY_TYPE_UNDEFINED && activityType != stack.activityType) {
                continue
            }
            if (windowingMode != WINDOWING_MODE_UNDEFINED && windowingMode != stack.windowingMode) {
                continue
            }
            ++count
        }
        return count
    }

    fun getRootTask(taskId: Int): Task? =
        rootTasks.firstOrNull { it.rootTaskId == taskId }

    fun getRotation(displayId: Int): Int =
            getDisplay(displayId)?.rotation ?: error("Default display not found")

    fun getOrientation(displayId: Int): Int =
            getDisplay(displayId)?.lastOrientation ?: error("Default display not found")

    fun getStackByActivityType(activityType: Int): Task? =
        rootTasks.firstOrNull { it.activityType == activityType }

    fun getStandardStackByWindowingMode(windowingMode: Int): Task? =
        rootTasks.firstOrNull {
            it.activityType == ACTIVITY_TYPE_STANDARD &&
                it.windowingMode == windowingMode
        }

    private fun getActivitiesForWindow(
        windowState: WindowState,
        displayId: Int = DEFAULT_DISPLAY
    ): List<Activity> {
        return displays
            .firstOrNull { it.id == displayId }
            ?.rootTasks
            ?.mapNotNull { stack ->
                stack.getActivity { activity ->
                    activity.hasWindow(windowState)
                }
            } ?: emptyList()
    }

    /**
     * Get the all activities on display with id [displayId], containing a matching
     * [componentMatcher]
     *
     * @param componentMatcher Components to search
     * @param displayId display where to search the activity
     */
    fun getActivitiesForWindow(
        componentMatcher: IComponentMatcher,
        displayId: Int = DEFAULT_DISPLAY
    ): List<Activity> {
        return displays
            .firstOrNull { it.id == displayId }
            ?.rootTasks
            ?.mapNotNull { stack ->
            stack.getActivity { activity ->
                activity.hasWindow(componentMatcher)
            }
        } ?: emptyList()
    }

    /**
     * @return if any activity matches [componentMatcher]
     *
     * @param componentMatcher Components to search
     */
    fun containsActivity(componentMatcher: IComponentMatcher): Boolean =
        rootTasks.any { it.containsActivity(componentMatcher) }

    /**
     * @return the first [Activity] matching [componentMatcher], or null otherwise
     *
     * @param componentMatcher Components to search
     */
    fun getActivity(componentMatcher: IComponentMatcher): Activity? =
        rootTasks.firstNotNullOfOrNull { it.getActivity(componentMatcher) }

    private fun getActivity(activityName: String): Activity? =
        rootTasks.firstNotNullOfOrNull { task ->
            task.getActivity { activity ->
                activity.title.contains(activityName)
            }
        }

    /**
     * @return if any activity matching [componentMatcher] is visible
     *
     * @param componentMatcher Components to search
     */
    fun isActivityVisible(componentMatcher: IComponentMatcher): Boolean =
        getActivity(componentMatcher)?.isVisible ?: false

    /**
     * @return if any activity matching [componentMatcher] has state of [activityState]
     *
     * @param componentMatcher Components to search
     * @param activityState expected activity state
     */
    fun hasActivityState(componentMatcher: IComponentMatcher, activityState: String): Boolean =
        rootTasks.any { it.getActivity(componentMatcher)?.state == activityState }

    /**
     * @return if any pending activities match [componentMatcher]
     *
     * @param componentMatcher Components to search
     */
    fun pendingActivityContain(componentMatcher: IComponentMatcher): Boolean =
        componentMatcher.activityMatchesAnyOf(pendingActivities)

    /**
     * @return the visible [WindowState]s matching [componentMatcher]
     *
     * @param componentMatcher Components to search
     */
    fun getMatchingVisibleWindowState(componentMatcher: IComponentMatcher): Array<WindowState> {
        return windowStates
            .filter { it.isSurfaceShown && componentMatcher.windowMatchesAnyOf(it) }
                .toTypedArray()
    }

    /**
     * @return the [WindowState] for the nav bar in the display with id [displayId]
     */
    fun getNavBarWindow(displayId: Int): WindowState? {
        val navWindow = windowStates.filter { it.isValidNavBarType && it.displayId == displayId }

        // We may need some time to wait for nav bar showing.
        // It's Ok to get 0 nav bar here.
        if (navWindow.size > 1) {
            throw IllegalStateException("There should be at most one navigation bar on a display")
        }
        return navWindow.firstOrNull()
    }

    private fun getWindowStateForAppToken(appToken: String): WindowState? =
        windowStates.firstOrNull { it.token == appToken }

    /**
     * Checks if there exists a [WindowState] matching [componentMatcher]
     *
     * @param componentMatcher Components to search
     */
    fun containsWindow(componentMatcher: IComponentMatcher): Boolean =
        componentMatcher.windowMatchesAnyOf(windowStates)

    /**
     * Check if at least one [WindowState] matching [componentMatcher] is visible
     *
     * @param componentMatcher Components to search
     */
    fun isWindowSurfaceShown(componentMatcher: IComponentMatcher): Boolean =
        getMatchingVisibleWindowState(componentMatcher).isNotEmpty()

    /**
     * Checks if the state has any window in PIP mode
     */
    fun hasPipWindow(): Boolean = pinnedWindows.isNotEmpty()

    /**
     * Checks that a [WindowState] matching [componentMatcher] is in PIP mode
     *
     * @param componentMatcher Components to search
     */
    fun isInPipMode(componentMatcher: IComponentMatcher): Boolean =
        componentMatcher.windowMatchesAnyOf(pinnedWindows)

    fun getZOrder(w: WindowState): Int = windowStates.size - windowStates.indexOf(w)

    fun defaultMinimalTaskSize(displayId: Int): Int =
        dpToPx(DEFAULT_RESIZABLE_TASK_SIZE_DP.toFloat(), getDisplay(displayId)!!.dpi)

    fun defaultMinimalDisplaySizeForSplitScreen(displayId: Int): Int {
        return dpToPx(DEFAULT_MINIMAL_SPLIT_SCREEN_DISPLAY_SIZE_DP.toFloat(),
            getDisplay(displayId)!!.dpi)
    }

    fun getIsIncompleteReason(): String {
        return buildString {
            if (rootTasks.isEmpty()) {
                append("No stacks found...")
            }
            if (focusedStackId == -1) {
                append("No focused stack found...")
            }
            if (focusedActivity == null) {
                append("No focused activity found...")
            }
            if (resumedActivities.isEmpty()) {
                append("No resumed activities found...")
            }
            if (windowStates.isEmpty()) {
                append("No Windows found...")
            }
            if (focusedWindow == null) {
                append("No Focused Window...")
            }
            if (focusedApp.isEmpty()) {
                append("No Focused App...")
            }
            if (keyguardControllerState.isKeyguardShowing) {
                append("Keyguard showing...")
            }
        }
    }

    fun isComplete(): Boolean = !isIncomplete()
    fun isIncomplete(): Boolean {
        return rootTasks.isEmpty() || focusedStackId == -1 || windowStates.isEmpty() ||
            // overview screen has no focused window
            ((focusedApp.isEmpty() || focusedWindow == null) && homeActivity == null) ||
            (focusedActivity == null || resumedActivities.isEmpty()) &&
            !keyguardControllerState.isKeyguardShowing
    }

    fun asTrace(): WindowManagerTrace = WindowManagerTrace(arrayOf(this))

    override fun toString(): String {
        return "${prettyTimestamp(timestamp)} (timestamp=$timestamp)"
    }

    companion object {
        const val STATE_INITIALIZING = "INITIALIZING"
        const val STATE_RESUMED = "RESUMED"
        const val STATE_PAUSED = "PAUSED"
        const val STATE_STOPPED = "STOPPED"
        const val STATE_DESTROYED = "DESTROYED"
        const val APP_STATE_IDLE = "APP_STATE_IDLE"
        internal const val ACTIVITY_TYPE_UNDEFINED = 0
        internal const val ACTIVITY_TYPE_STANDARD = 1
        internal const val DEFAULT_DISPLAY = 0
        internal const val DEFAULT_MINIMAL_SPLIT_SCREEN_DISPLAY_SIZE_DP = 440
        internal const val ACTIVITY_TYPE_HOME = 2
        internal const val ACTIVITY_TYPE_RECENTS = 3
        internal const val WINDOWING_MODE_UNDEFINED = 0
        private const val DENSITY_DEFAULT = 160
        /**
         * @see android.app.WindowConfiguration.WINDOWING_MODE_PINNED
         */
        private const val WINDOWING_MODE_PINNED = 2

        /**
         * @see android.view.WindowManager.LayoutParams
         */
        internal const val TYPE_NAVIGATION_BAR_PANEL = 2024

        // Default minimal size of resizable task, used if none is set explicitly.
        // Must be kept in sync with 'default_minimal_size_resizable_task'
        // dimen from frameworks/base.
        internal const val DEFAULT_RESIZABLE_TASK_SIZE_DP = 220

        fun dpToPx(dp: Float, densityDpi: Int): Int {
            return (dp * densityDpi / DENSITY_DEFAULT + 0.5f).toInt()
        }
    }
    override fun equals(other: Any?): Boolean {
        return other is WindowManagerState && other.timestamp == this.timestamp
    }

    override fun hashCode(): Int {
        var result = where.hashCode()
        result = 31 * result + (policy?.hashCode() ?: 0)
        result = 31 * result + focusedApp.hashCode()
        result = 31 * result + focusedDisplayId
        result = 31 * result + focusedWindow.hashCode()
        result = 31 * result + inputMethodWindowAppToken.hashCode()
        result = 31 * result + isHomeRecentsComponent.hashCode()
        result = 31 * result + isDisplayFrozen.hashCode()
        result = 31 * result + pendingActivities.contentHashCode()
        result = 31 * result + root.hashCode()
        result = 31 * result + keyguardControllerState.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + isVisible.hashCode()
        return result
    }
}
