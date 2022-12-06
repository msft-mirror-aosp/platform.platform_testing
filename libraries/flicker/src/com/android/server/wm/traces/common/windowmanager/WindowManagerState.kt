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
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.Timestamp.Companion.NULL_TIMESTAMP
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.windowmanager.windows.Activity
import com.android.server.wm.traces.common.windowmanager.windows.DisplayContent
import com.android.server.wm.traces.common.windowmanager.windows.KeyguardControllerState
import com.android.server.wm.traces.common.windowmanager.windows.RootWindowContainer
import com.android.server.wm.traces.common.windowmanager.windows.Task
import com.android.server.wm.traces.common.windowmanager.windows.TaskFragment
import com.android.server.wm.traces.common.windowmanager.windows.WindowContainer
import com.android.server.wm.traces.common.windowmanager.windows.WindowManagerPolicy
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
import kotlin.js.JsName

/**
 * Represents a single WindowManager trace entry.
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot access internal
 * Java/Android functionality
 *
 * The timestamp constructor must be a string due to lack of Kotlin/KotlinJS Long compatibility
 */
class WindowManagerState(
    @JsName("elapsedTimestamp") val elapsedTimestamp: Long,
    @JsName("clockTimestamp") val clockTimestamp: Long?,
    @JsName("where") val where: String,
    @JsName("policy") val policy: WindowManagerPolicy?,
    @JsName("focusedApp") val focusedApp: String,
    @JsName("focusedDisplayId") val focusedDisplayId: Int,
    @JsName("_focusedWindow") private val _focusedWindow: String,
    @JsName("inputMethodWindowAppToken") val inputMethodWindowAppToken: String,
    @JsName("isHomeRecentsComponent") val isHomeRecentsComponent: Boolean,
    @JsName("isDisplayFrozen") val isDisplayFrozen: Boolean,
    @JsName("_pendingActivities") private val _pendingActivities: Array<String>,
    @JsName("root") val root: RootWindowContainer,
    @JsName("keyguardControllerState") val keyguardControllerState: KeyguardControllerState
) : ITraceEntry {
    override val timestamp =
        Timestamp(elapsedNanos = elapsedTimestamp, unixNanos = clockTimestamp ?: NULL_TIMESTAMP)
    @JsName("isVisible") val isVisible: Boolean = true
    @JsName("stableId")
    val stableId: String
        get() = this::class.simpleName ?: error("Unable to determine class")
    @JsName("isTablet")
    val isTablet: Boolean
        get() = displays.any { it.isTablet }

    @JsName("windowContainers")
    val windowContainers: Array<WindowContainer>
        get() = root.collectDescendants()

    @JsName("children")
    val children: Array<WindowContainer>
        get() = root.children.reversedArray()

    /** Displays in z-order with the top most at the front of the list, starting with primary. */
    @JsName("displays")
    val displays: Array<DisplayContent>
        get() = windowContainers.filterIsInstance<DisplayContent>().toTypedArray()

    /**
     * Root tasks in z-order with the top most at the front of the list, starting with primary
     * display.
     */
    @JsName("rootTasks")
    val rootTasks: Array<Task>
        get() = displays.flatMap { it.rootTasks.toList() }.toTypedArray()

    /** TaskFragments in z-order with the top most at the front of the list. */
    @JsName("taskFragments")
    val taskFragments: Array<TaskFragment>
        get() = windowContainers.filterIsInstance<TaskFragment>().toTypedArray()

    /** Windows in z-order with the top most at the front of the list. */
    @JsName("windowStates")
    val windowStates: Array<WindowState>
        get() = windowContainers.filterIsInstance<WindowState>().toTypedArray()

    @Deprecated("Please use windowStates instead", replaceWith = ReplaceWith("windowStates"))
    @JsName("windows")
    val windows: Array<WindowState>
        get() = windowStates

    @JsName("appWindows")
    val appWindows: Array<WindowState>
        get() = windowStates.filter { it.isAppWindow }.toTypedArray()
    @JsName("nonAppWindows")
    val nonAppWindows: Array<WindowState>
        get() = windowStates.filterNot { it.isAppWindow }.toTypedArray()
    @JsName("aboveAppWindows")
    val aboveAppWindows: Array<WindowState>
        get() = windowStates.takeWhile { !appWindows.contains(it) }.toTypedArray()
    @JsName("belowAppWindows")
    val belowAppWindows: Array<WindowState>
        get() =
            windowStates.dropWhile { !appWindows.contains(it) }.drop(appWindows.size).toTypedArray()
    @JsName("visibleWindows")
    val visibleWindows: Array<WindowState>
        get() =
            windowStates
                .filter {
                    val activities = getActivitiesForWindowState(it)
                    val windowIsVisible = it.isVisible
                    val activityIsVisible = activities.any { activity -> activity.isVisible }

                    // for invisible checks it suffices if activity or window is invisible
                    windowIsVisible && (activityIsVisible || activities.isEmpty())
                }
                .toTypedArray()
    @JsName("visibleAppWindows")
    val visibleAppWindows: Array<WindowState>
        get() = visibleWindows.filter { it.isAppWindow }.toTypedArray()
    @JsName("topVisibleAppWindow")
    val topVisibleAppWindow: WindowState?
        get() = visibleAppWindows.firstOrNull()
    @JsName("pinnedWindows")
    val pinnedWindows: Array<WindowState>
        get() = visibleWindows.filter { it.windowingMode == WINDOWING_MODE_PINNED }.toTypedArray()
    @JsName("pendingActivities")
    val pendingActivities: Array<Activity>
        get() = _pendingActivities.mapNotNull { getActivityByName(it) }.toTypedArray()
    @JsName("focusedWindow")
    val focusedWindow: WindowState?
        get() = visibleWindows.firstOrNull { it.name == _focusedWindow }

    val isKeyguardShowing: Boolean
        get() = keyguardControllerState.isKeyguardShowing
    val isAodShowing: Boolean
        get() = keyguardControllerState.isAodShowing
    /**
     * Checks if the device state supports rotation, i.e., if the rotation sensor is enabled (e.g.,
     * launcher) and if the rotation not fixed
     */
    @JsName("canRotate")
    val canRotate: Boolean
        get() = policy?.isFixedOrientation != true && policy?.isOrientationNoSensor != true
    @JsName("focusedDisplay")
    val focusedDisplay: DisplayContent?
        get() = getDisplay(focusedDisplayId)
    @JsName("focusedStackId")
    val focusedStackId: Int
        get() = focusedDisplay?.focusedRootTaskId ?: -1
    @JsName("focusedActivity")
    val focusedActivity: Activity?
        get() {
            val focusedDisplay = focusedDisplay
            val focusedWindow = focusedWindow
            return when {
                focusedDisplay != null && focusedDisplay.resumedActivity.isNotEmpty() ->
                    getActivityByName(focusedDisplay.resumedActivity)
                focusedWindow != null ->
                    getActivitiesForWindowState(focusedWindow, focusedDisplayId).firstOrNull()
                else -> null
            }
        }
    @JsName("resumedActivities")
    val resumedActivities: Array<Activity>
        get() =
            rootTasks
                .flatMap { it.resumedActivities.toList() }
                .mapNotNull { getActivityByName(it) }
                .toTypedArray()
    @JsName("resumedActivitiesCount")
    val resumedActivitiesCount: Int
        get() = resumedActivities.size
    @JsName("stackCount")
    val stackCount: Int
        get() = rootTasks.size
    @JsName("homeTask")
    val homeTask: Task?
        get() = getStackByActivityType(ACTIVITY_TYPE_HOME)?.topTask
    @JsName("recentsTask")
    val recentsTask: Task?
        get() = getStackByActivityType(ACTIVITY_TYPE_RECENTS)?.topTask
    @JsName("homeActivity")
    val homeActivity: Activity?
        get() = homeTask?.activities?.lastOrNull()
    @JsName("isHomeActivityVisible")
    val isHomeActivityVisible: Boolean
        get() {
            val activity = homeActivity
            return activity != null && activity.isVisible
        }
    @JsName("recentsActivity")
    val recentsActivity: Activity?
        get() = recentsTask?.activities?.lastOrNull()
    @JsName("isRecentsActivityVisible")
    val isRecentsActivityVisible: Boolean
        get() {
            val activity = recentsActivity
            return activity != null && activity.isVisible
        }
    @JsName("frontWindow")
    val frontWindow: WindowState?
        get() = windowStates.firstOrNull()
    @JsName("inputMethodWindowState")
    val inputMethodWindowState: WindowState?
        get() = getWindowStateForAppToken(inputMethodWindowAppToken)

    @JsName("getDefaultDisplay")
    fun getDefaultDisplay(): DisplayContent? = displays.firstOrNull { it.id == DEFAULT_DISPLAY }

    @JsName("getDisplay")
    fun getDisplay(displayId: Int): DisplayContent? = displays.firstOrNull { it.id == displayId }

    @JsName("countStacks")
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

    @JsName("getRootTask")
    fun getRootTask(taskId: Int): Task? = rootTasks.firstOrNull { it.rootTaskId == taskId }

    @JsName("getRotation")
    fun getRotation(displayId: Int): PlatformConsts.Rotation =
        PlatformConsts.Rotation.getByValue(
            getDisplay(displayId)?.rotation ?: error("Default display not found")
        )

    @JsName("getOrientation")
    fun getOrientation(displayId: Int): Int =
        getDisplay(displayId)?.lastOrientation ?: error("Default display not found")

    @JsName("getStackByActivityType")
    fun getStackByActivityType(activityType: Int): Task? =
        rootTasks.firstOrNull { it.activityType == activityType }

    @JsName("getStandardStackByWindowingMode")
    fun getStandardStackByWindowingMode(windowingMode: Int): Task? =
        rootTasks.firstOrNull {
            it.activityType == ACTIVITY_TYPE_STANDARD && it.windowingMode == windowingMode
        }

    @JsName("getActivitiesForWindowState")
    fun getActivitiesForWindowState(
        windowState: WindowState,
        displayId: Int = DEFAULT_DISPLAY
    ): List<Activity> {
        return displays
            .firstOrNull { it.id == displayId }
            ?.rootTasks
            ?.mapNotNull { stack ->
                stack.getActivity { activity -> activity.hasWindowState(windowState) }
            }
            ?: emptyList()
    }

    /**
     * Get the all activities on display with id [displayId], containing a matching
     * [componentMatcher]
     *
     * @param componentMatcher Components to search
     * @param displayId display where to search the activity
     */
    @JsName("getActivitiesForWindow")
    fun getActivitiesForWindow(
        componentMatcher: IComponentMatcher,
        displayId: Int = DEFAULT_DISPLAY
    ): List<Activity> {
        return displays
            .firstOrNull { it.id == displayId }
            ?.rootTasks
            ?.mapNotNull { stack ->
                stack.getActivity { activity -> activity.hasWindow(componentMatcher) }
            }
            ?: emptyList()
    }

    /**
     * @return if any activity matches [componentMatcher]
     *
     * @param componentMatcher Components to search
     */
    @JsName("containsActivity")
    fun containsActivity(componentMatcher: IComponentMatcher): Boolean =
        rootTasks.any { it.containsActivity(componentMatcher) }

    /**
     * @return the first [Activity] matching [componentMatcher], or null otherwise
     *
     * @param componentMatcher Components to search
     */
    @JsName("getActivity")
    fun getActivity(componentMatcher: IComponentMatcher): Activity? =
        rootTasks.firstNotNullOfOrNull { it.getActivity(componentMatcher) }

    @JsName("getActivityByName")
    private fun getActivityByName(activityName: String): Activity? =
        rootTasks.firstNotNullOfOrNull { task ->
            task.getActivity { activity -> activity.title.contains(activityName) }
        }

    /**
     * @return if any activity matching [componentMatcher] is visible
     *
     * @param componentMatcher Components to search
     */
    @JsName("isActivityVisible")
    fun isActivityVisible(componentMatcher: IComponentMatcher): Boolean =
        getActivity(componentMatcher)?.isVisible ?: false

    /**
     * @return if any activity matching [componentMatcher] has state of [activityState]
     *
     * @param componentMatcher Components to search
     * @param activityState expected activity state
     */
    @JsName("hasActivityState")
    fun hasActivityState(componentMatcher: IComponentMatcher, activityState: String): Boolean =
        rootTasks.any { it.getActivity(componentMatcher)?.state == activityState }

    /**
     * @return if any pending activities match [componentMatcher]
     *
     * @param componentMatcher Components to search
     */
    @JsName("pendingActivityContain")
    fun pendingActivityContain(componentMatcher: IComponentMatcher): Boolean =
        componentMatcher.activityMatchesAnyOf(pendingActivities)

    /**
     * @return the visible [WindowState]s matching [componentMatcher]
     *
     * @param componentMatcher Components to search
     */
    @JsName("getMatchingVisibleWindowState")
    fun getMatchingVisibleWindowState(componentMatcher: IComponentMatcher): Array<WindowState> {
        return windowStates
            .filter { it.isSurfaceShown && componentMatcher.windowMatchesAnyOf(it) }
            .toTypedArray()
    }

    /** @return the [WindowState] for the nav bar in the display with id [displayId] */
    @JsName("getNavBarWindow")
    fun getNavBarWindow(displayId: Int): WindowState? {
        val navWindow = windowStates.filter { it.isValidNavBarType && it.displayId == displayId }

        // We may need some time to wait for nav bar showing.
        // It's Ok to get 0 nav bar here.
        if (navWindow.size > 1) {
            throw IllegalStateException("There should be at most one navigation bar on a display")
        }
        return navWindow.firstOrNull()
    }

    @JsName("getWindowStateForAppToken")
    private fun getWindowStateForAppToken(appToken: String): WindowState? =
        windowStates.firstOrNull { it.token == appToken }

    /**
     * Checks if there exists a [WindowState] matching [componentMatcher]
     *
     * @param componentMatcher Components to search
     */
    @JsName("containsWindow")
    fun containsWindow(componentMatcher: IComponentMatcher): Boolean =
        componentMatcher.windowMatchesAnyOf(windowStates)

    /**
     * Check if at least one [WindowState] matching [componentMatcher] is visible
     *
     * @param componentMatcher Components to search
     */
    @JsName("isWindowSurfaceShown")
    fun isWindowSurfaceShown(componentMatcher: IComponentMatcher): Boolean =
        getMatchingVisibleWindowState(componentMatcher).isNotEmpty()

    /** Checks if the state has any window in PIP mode */
    @JsName("hasPipWindow") fun hasPipWindow(): Boolean = pinnedWindows.isNotEmpty()

    /**
     * Checks that a [WindowState] matching [componentMatcher] is in PIP mode
     *
     * @param componentMatcher Components to search
     */
    @JsName("isInPipMode")
    fun isInPipMode(componentMatcher: IComponentMatcher): Boolean =
        componentMatcher.windowMatchesAnyOf(pinnedWindows)

    @JsName("getZOrder")
    fun getZOrder(w: WindowState): Int = windowStates.size - windowStates.indexOf(w)

    @JsName("defaultMinimalTaskSize")
    fun defaultMinimalTaskSize(displayId: Int): Int =
        dpToPx(DEFAULT_RESIZABLE_TASK_SIZE_DP.toFloat(), getDisplay(displayId)!!.dpi)

    @JsName("defaultMinimalDisplaySizeForSplitScreen")
    fun defaultMinimalDisplaySizeForSplitScreen(displayId: Int): Int {
        return dpToPx(
            DEFAULT_MINIMAL_SPLIT_SCREEN_DISPLAY_SIZE_DP.toFloat(),
            getDisplay(displayId)!!.dpi
        )
    }

    @JsName("getIsIncompleteReason")
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

    @JsName("isComplete") fun isComplete(): Boolean = !isIncomplete()
    @JsName("isIncomplete")
    fun isIncomplete(): Boolean {
        return rootTasks.isEmpty() ||
            focusedStackId == -1 ||
            windowStates.isEmpty() ||
            // overview screen has no focused window
            ((focusedApp.isEmpty() || focusedWindow == null) && homeActivity == null) ||
            (focusedActivity == null || resumedActivities.isEmpty()) &&
                !keyguardControllerState.isKeyguardShowing
    }

    @JsName("asTrace") fun asTrace(): WindowManagerTrace = WindowManagerTrace(arrayOf(this))

    override fun toString(): String {
        return "${timestamp}ns"
    }

    companion object {
        @JsName("STATE_INITIALIZING") const val STATE_INITIALIZING = "INITIALIZING"
        @JsName("STATE_RESUMED") const val STATE_RESUMED = "RESUMED"
        @JsName("STATE_PAUSED") const val STATE_PAUSED = "PAUSED"
        @JsName("STATE_STOPPED") const val STATE_STOPPED = "STOPPED"
        @JsName("STATE_DESTROYED") const val STATE_DESTROYED = "DESTROYED"
        @JsName("APP_STATE_IDLE") const val APP_STATE_IDLE = "APP_STATE_IDLE"
        @JsName("ACTIVITY_TYPE_UNDEFINED") internal const val ACTIVITY_TYPE_UNDEFINED = 0
        @JsName("ACTIVITY_TYPE_STANDARD") internal const val ACTIVITY_TYPE_STANDARD = 1
        @JsName("DEFAULT_DISPLAY") internal const val DEFAULT_DISPLAY = 0
        @JsName("DEFAULT_MINIMAL_SPLIT_SCREEN_DISPLAY_SIZE_DP")
        internal const val DEFAULT_MINIMAL_SPLIT_SCREEN_DISPLAY_SIZE_DP = 440
        @JsName("ACTIVITY_TYPE_HOME") internal const val ACTIVITY_TYPE_HOME = 2
        @JsName("ACTIVITY_TYPE_RECENTS") internal const val ACTIVITY_TYPE_RECENTS = 3
        @JsName("WINDOWING_MODE_UNDEFINED") internal const val WINDOWING_MODE_UNDEFINED = 0
        @JsName("DENSITY_DEFAULT") private const val DENSITY_DEFAULT = 160
        /** @see android.app.WindowConfiguration.WINDOWING_MODE_PINNED */
        @JsName("WINDOWING_MODE_PINNED") private const val WINDOWING_MODE_PINNED = 2

        /** @see android.view.WindowManager.LayoutParams */
        @JsName("TYPE_NAVIGATION_BAR_PANEL") internal const val TYPE_NAVIGATION_BAR_PANEL = 2024

        // Default minimal size of resizable task, used if none is set explicitly.
        // Must be kept in sync with 'default_minimal_size_resizable_task'
        // dimen from frameworks/base.
        @JsName("DEFAULT_RESIZABLE_TASK_SIZE_DP")
        internal const val DEFAULT_RESIZABLE_TASK_SIZE_DP = 220

        @JsName("dpToPx")
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
