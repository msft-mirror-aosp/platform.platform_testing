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

package com.android.server.wm.traces.common.windowmanager

import com.android.server.wm.traces.common.AssertionResult
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.Region
import com.android.server.wm.traces.common.ITraceEntry
import com.android.server.wm.traces.common.windowmanager.windows.Activity
import com.android.server.wm.traces.common.windowmanager.windows.DisplayArea
import com.android.server.wm.traces.common.windowmanager.windows.DisplayContent
import com.android.server.wm.traces.common.windowmanager.windows.KeyguardControllerState
import com.android.server.wm.traces.common.windowmanager.windows.RootWindowContainer
import com.android.server.wm.traces.common.windowmanager.windows.ActivityTask
import com.android.server.wm.traces.common.windowmanager.windows.WindowManagerPolicy
import com.android.server.wm.traces.common.windowmanager.windows.WindowState

/**
 * Represents a single WindowManager trace entry.
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot
 * access internal Java/Android functionality
 *
 **/
open class WindowManagerState(
    val where: String,
    val policy: WindowManagerPolicy?,
    val focusedApp: String,
    val focusedDisplayId: Int,
    val focusedWindow: String,
    val inputMethodWindowAppToken: String,
    val isHomeRecentsComponent: Boolean,
    val isDisplayFrozen: Boolean,
    val pendingActivities: List<String>,
    val root: RootWindowContainer,
    val keyguardControllerState: KeyguardControllerState,
    override val timestamp: Long = 0
) : ITraceEntry {
    val kind = "entry"
    val stableId = "entry"

    var validityCheckFocusedWindow = true
        private set

    // Displays in z-order with the top most at the front of the list, starting with primary.
    val displays: Array<DisplayContent> by lazy { root.collectDescendants() }

    // Stacks in z-order with the top most at the front of the list, starting with primary display.
    val rootTasks: Array<ActivityTask>
        by lazy { displays.flatMap { it.rootTasks.toList() }.toTypedArray() }

    // Windows in z-order with the top most at the front of the list.
    val windowStates: Array<WindowState>
        by lazy { root.collectDescendants() }

    @Deprecated("Please use windowStates instead", replaceWith = ReplaceWith("windowStates"))
    val windows: Array<WindowState>
        get() = windowStates

    val appWindows: Array<WindowState>
        by lazy { windowStates.filter { it.isAppWindow }.toTypedArray() }
    val nonAppWindows: Array<WindowState>
        by lazy { windowStates.filterNot { it.isAppWindow }.toTypedArray() }
    val aboveAppWindows: Array<WindowState> by lazy {
        windowStates.takeWhile { !appWindows.contains(it) }.toTypedArray()
    }
    val belowAppWindows: Array<WindowState> by lazy {
        windowStates
            .dropWhile { !appWindows.contains(it) }.drop(appWindows.size).toTypedArray()
    }
    val visibleWindows: Array<WindowState>
        by lazy { windowStates.filter { it.isSurfaceShown }.toTypedArray() }
    val topVisibleAppWindow: String by lazy {
        appWindows.filter { it.isVisible }
            .map { it.title }
            .firstOrNull() ?: ""
    }

    val focusedDisplay: DisplayContent? by lazy { getDisplay(focusedDisplayId) }
    val focusedStackId: Int by lazy { focusedDisplay?.focusedRootTaskId ?: -1 }
    val focusedActivity: String by lazy { focusedDisplay?.resumedActivity ?: "" }
    val resumedActivitiesInDisplays: Array<String>
        by lazy { displays.map { it.resumedActivity }.filter { it.isNotEmpty() }.toTypedArray() }
    val resumedActivitiesInStacks: Array<String>
        by lazy { rootTasks.map { it.resumedActivity }.filter { it.isNotEmpty() }.toTypedArray() }
    val defaultPinnedStackBounds: Rect by lazy {
        displays.lastOrNull { it.defaultPinnedStackBounds.isNotEmpty }?.defaultPinnedStackBounds
            ?: Rect()
    }
    val pinnedStackMovementBounds: Rect by lazy {
        displays.lastOrNull { it.defaultPinnedStackBounds.isNotEmpty }?.pinnedStackMovementBounds
            ?: Rect()
    }
    val focusedStackActivityType: Int
        by lazy { getRootTask(focusedStackId)?.activityType ?: ACTIVITY_TYPE_UNDEFINED }
    val focusedStackWindowingMode: Int
        by lazy { getRootTask(focusedStackId)?.windowingMode ?: WINDOWING_MODE_UNDEFINED }
    val resumedActivity: String by lazy {
        focusedDisplay?.resumedActivity ?: "Default display not found"
    }
    val resumedActivitiesCount: Int by lazy { resumedActivitiesInStacks.size }
    val stackCount: Int by lazy { rootTasks.size }
    val displayCount: Int by lazy { displays.size }
    val homeTask: ActivityTask?
        by lazy { getStackByActivityType(ACTIVITY_TYPE_HOME)?.topTask }
    val recentsTask: ActivityTask?
        by lazy { getStackByActivityType(ACTIVITY_TYPE_RECENTS)?.topTask }
    val homeActivity: Activity? by lazy { homeTask?.activities?.lastOrNull() }
    val recentsActivity: Activity? by lazy { recentsTask?.activities?.lastOrNull() }
    val rootTasksCount: Int by lazy { rootTasks.size }
    val isRecentsActivityVisible: Boolean by lazy { recentsActivity?.isVisible ?: false }
    val dreamTask: ActivityTask? by lazy {
        getStackByActivityType(ACTIVITY_TYPE_DREAM)?.topTask
    }
    val defaultDisplayLastTransition: String by lazy {
        getDefaultDisplay()?.lastTransition
            ?: "Default display not found"
    }
    val defaultDisplayAppTransitionState: String by lazy {
        getDefaultDisplay()?.appTransitionState
            ?: "Default display not found"
    }
    val allNavigationBarStates: List<WindowState>
        by lazy { windowStates.filter { it.isValidNavBarType } }
    val frontWindow: String? by lazy { windowStates.map { it.title }.firstOrNull() }
    val stableBounds: Rect by lazy { getDefaultDisplay()?.stableBounds ?: Rect() }
    val inputMethodWindowState: WindowState?
        by lazy { getWindowStateForAppToken(inputMethodWindowAppToken) }

    /**
     * Returns the rect of all the frames in the entry
     * Converted to typedArray for better compatibility with JavaScript code
     */
    val rects: Array<Rect> by lazy { root.rects }

    /**
     * Enable/disable the mFocusedWindow check during the computeState.
     */
    fun setValidityCheckWithFocusedWindow(validityCheckFocusedWindow: Boolean) {
        this.validityCheckFocusedWindow = validityCheckFocusedWindow
    }

    fun getDefaultDisplay(): DisplayContent? =
        displays.firstOrNull { it.id == DEFAULT_DISPLAY }

    fun getDisplay(displayId: Int): DisplayContent? =
        displays.firstOrNull { it.id == displayId }

    fun getTaskDisplayArea(activityName: String): DisplayArea? {
        val result = displays.mapNotNull { it.getTaskDisplayArea(activityName) }

        if (result.size > 1) {
            throw IllegalArgumentException(
                "There must be exactly one activity among all TaskDisplayAreas.")
        }

        return result.firstOrNull()
    }

    fun getFrontRootTaskId(displayId: Int): Int =
        getDisplay(displayId)?.rootTasks?.first()?.rootTaskId ?: 0

    fun getFrontStackActivityType(displayId: Int): Int =
        getDisplay(displayId)?.rootTasks?.first()?.activityType ?: 0

    fun getFrontStackWindowingMode(displayId: Int): Int =
        getDisplay(displayId)?.rootTasks?.first()?.windowingMode ?: 0

    fun getTopActivityName(displayId: Int): String {
        return getDisplay(displayId)
            ?.rootTasks?.firstOrNull()
            ?.topTask
            ?.activities?.firstOrNull()
            ?.title
            ?: ""
    }

    fun getResumedActivitiesCountInPackage(packageName: String): Int {
        val componentPrefix = "$packageName/"
        var count = 0
        for (i in displays.indices.reversed()) {
            val mStacks = displays[i].rootTasks
            for (j in mStacks.indices.reversed()) {
                val resumedActivity = mStacks[j].resumedActivity
                if (resumedActivity.isNotEmpty() && resumedActivity.startsWith(componentPrefix)) {
                    count++
                }
            }
        }
        return count
    }

    fun getResumedActivity(displayId: Int): String {
        return getDisplay(displayId)?.resumedActivity ?: ""
    }

    fun containsStack(windowingMode: Int, activityType: Int): Boolean {
        return countStacks(windowingMode, activityType) > 0
    }

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

    fun getRootTask(taskId: Int): ActivityTask? =
        rootTasks.firstOrNull { it.rootTaskId == taskId }

    fun getRotation(displayId: Int): Int =
            getDisplay(displayId)?.rotation ?: error("Default display not found")

    fun getOrientation(displayId: Int): Int =
            getDisplay(displayId)?.lastOrientation ?: error("Default display not found")

    fun getStackByActivityType(activityType: Int): ActivityTask? =
        rootTasks.firstOrNull { it.activityType == activityType }

    fun getStandardStackByWindowingMode(windowingMode: Int): ActivityTask? =
        rootTasks.firstOrNull {
            it.activityType == ACTIVITY_TYPE_STANDARD &&
                it.windowingMode == windowingMode
        }

    fun getStandardTaskCountByWindowingMode(windowingMode: Int): Int {
        var count = 0
        for (stack in rootTasks) {
            if (stack.activityType != ACTIVITY_TYPE_STANDARD) {
                continue
            }
            if (stack.windowingMode == windowingMode) {
                count += if (stack.tasks.isEmpty()) 1 else stack.tasks.size
            }
        }
        return count
    }

    /** Get the stack on its display.  */
    fun getStackByActivity(activityName: String): ActivityTask? {
        return displays.map { display ->
            display.rootTasks.reversed().firstOrNull { stack ->
                stack.containsActivity(activityName)
            }
        }.firstOrNull()
    }

    /** Get the stack position on its display. */
    fun getStackIndexByActivityType(activityType: Int): Int {
        return displays
            .map { it.rootTasks.indexOfFirst { p -> p.activityType == activityType } }
            .firstOrNull { it > -1 }
            ?: -1
    }

    /** Get the stack position on its display. */
    fun getStackIndexByActivity(activityName: String): Int {
        for (display in displays) {
            for (i in display.rootTasks.indices.reversed()) {
                val stack = display.rootTasks[i]
                if (stack.containsActivity(activityName)) return i
            }
        }
        return -1
    }

    /** Get display id by activity on it. */
    fun getDisplayByActivity(activityComponent: String): Int {
        val task = getTaskByActivity(activityComponent) ?: return -1
        return getRootTask(task.rootTaskId)?.displayId
            ?: error("Task with name $activityComponent not found")
    }

    fun containsActivity(activityName: String): Boolean =
        rootTasks.any { it.containsActivity(activityName) }

    fun containsNoneOf(activityNames: Iterable<String>): Boolean {
        for (activityName in activityNames) {
            for (stack in rootTasks) {
                if (stack.containsActivity(activityName)) return false
            }
        }
        return true
    }

    fun containsActivityInWindowingMode(
        activityName: String,
        windowingMode: Int
    ): Boolean {
        for (stack in rootTasks) {
            val activity = stack.getActivity(activityName)
            if (activity != null && activity.windowingMode == windowingMode) {
                return true
            }
        }
        return false
    }

    fun isActivityVisible(activityName: String): Boolean =
        rootTasks.map { it.getActivity(activityName)?.isVisible ?: false }.firstOrNull()
            ?: false

    fun isActivityTranslucent(activityName: String): Boolean =
        rootTasks.map { it.getActivity(activityName)?.isTranslucent ?: false }.firstOrNull()
            ?: false

    fun isBehindOpaqueActivities(activityName: String): Boolean {
        for (stack in rootTasks) {
            val activity = stack.getActivity { a -> a.title == activityName || !a.isTranslucent }
            if (activity != null) {
                if (activity.title == activityName) {
                    return false
                }
                if (!activity.isTranslucent) {
                    return true
                }
            }
        }

        return false
    }

    fun containsStartedActivities(): Boolean = rootTasks.map {
        it.getActivity { a -> a.state != STATE_STOPPED && a.state != STATE_DESTROYED } != null
    }.firstOrNull() ?: false

    fun hasActivityState(activityName: String, activityState: String): Boolean =
        rootTasks.any { it.getActivity(activityName)?.state == activityState }

    fun getActivityProcId(activityName: String): Int =
        rootTasks.mapNotNull { it.getActivity(activityName)?.procId }
            .firstOrNull()
            ?: -1

    fun getStackIdByActivity(activityName: String): Int =
        getTaskByActivity(activityName)?.rootTaskId ?: INVALID_STACK_ID

    fun getTaskByActivity(activityName: String): ActivityTask? =
        getTaskByActivity(activityName, WINDOWING_MODE_UNDEFINED)

    fun getTaskByActivity(activityName: String, windowingMode: Int): ActivityTask? {
        for (stack in rootTasks) {
            if (windowingMode == WINDOWING_MODE_UNDEFINED || windowingMode == stack.windowingMode) {
                val activity = stack.getActivity(activityName)
                if (activity != null) return activity.task
            }
        }
        return null
    }

    /**
     * Get the number of activities in the task, with the option to count only activities with
     * specific name.
     * @param taskId Id of the task where we're looking for the number of activities.
     * @param activityName Optional name of the activity we're interested in.
     * @return Number of all activities in the task if activityName is `null`, otherwise will
     * report number of activities that have specified name.
     */
    fun getActivityCountInTask(taskId: Int, activityName: String?): Int {
        // If activityName is null, count all activities in the task.
        // Otherwise count activities that have specified name.
        for (stack in rootTasks) {
            val task = stack.getTask(taskId) ?: continue

            if (activityName == null) {
                return task.activities.size
            }
            var count = 0
            for (activity in task.activities) {
                if (activity.title == activityName) {
                    count++
                }
            }
            return count
        }
        return 0
    }

    fun getRootTasksCount(displayId: Int): Int {
        var count = 0
        for (rootTask in rootTasks) {
            if (rootTask.displayId == displayId) ++count
        }
        return count
    }

    fun pendingActivityContain(activityName: String): Boolean {
        return pendingActivities.contains(activityName)
    }

    fun getMatchingVisibleWindowState(windowName: String): List<WindowState> {
        return windowStates.filter { it.isSurfaceShown && it.title == windowName }
    }

    fun getWindowByPackageName(packageName: String, windowType: Int): WindowState? =
        getWindowsByPackageName(packageName, windowType).firstOrNull()

    fun getWindowsByPackageName(
        packageName: String,
        vararg restrictToTypes: Int
    ): List<WindowState> =
        windowStates.filter { ws ->
            ((ws.title == packageName ||
                ws.title.startsWith("$packageName/")) &&
                restrictToTypes.any { type -> type == ws.type })
        }

    fun getMatchingWindowType(type: Int): List<WindowState> =
        windowStates.filter { it.type == type }

    fun getMatchingWindowTokens(windowName: String): List<String> =
        windowStates.filter { it.title === windowName }.map { it.token }

    fun getNavBarWindow(displayId: Int): WindowState? {
        val navWindow = windowStates.filter { it.isValidNavBarType && it.displayId == displayId }

        // We may need some time to wait for nav bar showing.
        // It's Ok to get 0 nav bar here.
        if (navWindow.size > 1) {
            throw IllegalStateException("There should be at most one navigation bar on a display")
        }
        return navWindow.firstOrNull()
    }

    fun getWindowStateForAppToken(appToken: String): WindowState? =
        windowStates.firstOrNull { it.token == appToken }

    /**
     * Check if there exists a window record with matching windowName.
     */
    fun containsWindow(windowName: String): Boolean =
        windowStates.any { it.title == windowName }

    /**
     * Check if at least one window which matches the specified name has shown it's surface.
     */
    fun isWindowSurfaceShown(windowName: String): Boolean {
        for (window in windowStates) {
            if (window.title == windowName) {
                if (window.isSurfaceShown) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Check if at least one window which matches provided window name is visible.
     */
    fun isWindowVisible(windowName: String): Boolean {
        for (window in windowStates) {
            if (window.title == windowName) {
                if (window.isVisible) {
                    return true
                }
            }
        }
        return false
    }

    fun allWindowSurfacesShown(windowName: String): Boolean {
        var allShown = false
        for (window in windowStates) {
            if (window.title == windowName) {
                if (!window.isSurfaceShown) {
                    return false
                }
                allShown = true
            }
        }
        return allShown
    }

    /**
     * Checks whether the display contains the given activity.
     */
    fun hasActivityInDisplay(displayId: Int, activityName: String): Boolean {
        for (stack in getDisplay(displayId)!!.rootTasks) {
            if (stack.containsActivity(activityName)) {
                return true
            }
        }
        return false
    }

    fun findFirstWindowWithType(type: Int): WindowState? =
        windowStates.firstOrNull { it.type == type }

    fun getZOrder(w: WindowState): Int = windowStates.size - windowStates.indexOf(w)

    fun getStandardRootStackByWindowingMode(windowingMode: Int): ActivityTask? {
        for (task in rootTasks) {
            if (task.activityType != ACTIVITY_TYPE_STANDARD) {
                continue
            }
            if (task.windowingMode == windowingMode) {
                return task
            }
        }
        return null
    }

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
            if (focusedActivity.isEmpty()) {
                append("No focused activity found...")
            }
            if (resumedActivitiesInStacks.isEmpty()) {
                append("No resumed activities found...")
            }
            if (windowStates.isEmpty()) {
                append("No Windows found...")
            }
            if (focusedWindow.isEmpty()) {
                append("No Focused Window...")
            }
            if (focusedApp.isEmpty()) {
                append("No Focused App...")
            }
            if (keyguardControllerState.isKeyguardShowing) {
                append("Keyguard showing...")
            }
            if (!validityCheckFocusedWindow) {
                append("Validity check...")
            }
        }
    }

    fun isComplete(): Boolean = !isIncomplete()
    fun isIncomplete(): Boolean {
        return rootTasks.isEmpty() || focusedStackId == -1 || windowStates.isEmpty() ||
            focusedApp.isEmpty() || (validityCheckFocusedWindow && focusedWindow.isEmpty()) ||
            (focusedActivity.isEmpty() || resumedActivitiesInStacks.isEmpty()) &&
            !keyguardControllerState.isKeyguardShowing
    }

    private fun Array<WindowState>.isWindowVisible(
        assertionName: String,
        windowTitle: String,
        isVisible: Boolean = true
    ): AssertionResult {
        val foundWindow = this.filter { it.title.contains(windowTitle) }
        return when {
            this.isEmpty() -> AssertionResult(
                "No windows found",
                assertionName,
                timestamp,
                success = false)
            foundWindow.isEmpty() -> AssertionResult(
                "$windowTitle cannot be found",
                assertionName,
                timestamp,
                success = false)
            isVisible && foundWindow.none { it.isVisible } -> AssertionResult(
                "$windowTitle is invisible",
                assertionName,
                timestamp,
                success = false)
            !isVisible && foundWindow.any { it.isVisible } -> AssertionResult(
                "$windowTitle is visible",
                assertionName,
                timestamp,
                success = false)
            else -> {
                val reason = if (isVisible) {
                    "${foundWindow.first { it.isVisible }.title} is visible"
                } else {
                    "${foundWindow.first { !it.isVisible }.title} is invisible"
                }
                AssertionResult(
                    success = true,
                    reason = reason)
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
    fun isAboveAppWindow(windowTitle: String, isVisible: Boolean = true): AssertionResult {
        return aboveAppWindows.isWindowVisible(
            "isAboveAppWindow${if (isVisible) "Visible" else "Invisible"}",
            windowTitle,
            isVisible)
    }

    /**
     * Checks if the non-app window with title containing [windowTitle] exists below the app
     * windows and if its visibility is equal to [isVisible]
     *
     * @param windowTitle window title to search
     * @param isVisible if the found window should be visible or not
     */
    fun isBelowAppWindow(windowTitle: String, isVisible: Boolean = true): AssertionResult {
        return belowAppWindows.isWindowVisible(
            "isBelowAppWindow${if (isVisible) "Visible" else "Invisible"}",
            windowTitle,
            isVisible)
    }

    /**
     * Checks if non-app window with title containing the [windowTitle] exists above or below the
     * app windows and if its visibility is equal to [isVisible]
     *
     * @param windowTitle window title to search
     * @param isVisible if the found window should be visible or not
     */
    fun hasNonAppWindow(windowTitle: String, isVisible: Boolean = true): AssertionResult {
        return nonAppWindows.isWindowVisible(
            "hasNonAppWindow${if (isVisible) "Visible" else "Invisible"}", windowTitle, isVisible)
    }

    /**
     * Checks if app window with title containing the [windowTitle] is on top
     *
     * @param windowTitle window title to search
     */
    fun isVisibleAppWindowOnTop(windowTitle: String): AssertionResult {
        val success = topVisibleAppWindow.contains(windowTitle)
        val reason = "wanted=$windowTitle found=$topVisibleAppWindow"
        return AssertionResult(reason, "isAppWindowOnTop", timestamp, success)
    }

    /**
     * Checks if app window with title containing the [windowTitle] is visible
     *
     * @param windowTitle window title to search
     */
    fun isAppWindowVisible(windowTitle: String): AssertionResult {
        return appWindows.isWindowVisible("isAppWindowVisible",
            windowTitle,
            isVisible = true)
    }

    /**
     * Obtains the region of the first visible window with title containing [windowTitle].
     *
     * @param windowTitle Name of the layer to search
     * @param resultComputation Predicate to compute a result based on the found window's region
     */
    fun covers(
        windowTitle: String,
        resultComputation: (Region) -> AssertionResult
    ): AssertionResult {
        val assertionName = "covers"
        val visibilityCheck = windowStates.isWindowVisible(assertionName, windowTitle)
        if (!visibilityCheck.success) {
            return visibilityCheck
        }

        val foundWindow = windowStates.first { it.title.contains(windowTitle) }
        val foundRegion = foundWindow.frameRegion

        return resultComputation(foundRegion)
    }

    /**
     * Check if the window named [aboveWindowTitle] is above the one named [belowWindowTitle].
     */
    fun isAboveWindow(aboveWindowTitle: String, belowWindowTitle: String): AssertionResult {
        val assertionName = "isAboveWindow"

        // windows are ordered by z-order, from top to bottom
        val aboveZ = windowStates.indexOfFirst { aboveWindowTitle in it.title }
        val belowZ = windowStates.indexOfFirst { belowWindowTitle in it.title }

        val notFound = mutableSetOf<String>().apply {
            if (aboveZ == -1) {
                add(aboveWindowTitle)
            }
            if (belowZ == -1) {
                add(belowWindowTitle)
            }
        }

        if (notFound.isNotEmpty()) {
            return AssertionResult(
                reason = "Could not find ${notFound.joinToString(" and ")}!",
                assertionName = assertionName,
                timestamp = timestamp,
                success = false
            )
        }

        // ensure the z-order
        return AssertionResult(
            reason = "$aboveWindowTitle is above $belowWindowTitle",
            assertionName = assertionName,
            timestamp = timestamp,
            success = aboveZ < belowZ
        )
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
        internal const val INVALID_STACK_ID = -1
        internal const val ACTIVITY_TYPE_HOME = 2
        internal const val ACTIVITY_TYPE_RECENTS = 3
        internal const val ACTIVITY_TYPE_DREAM = 5
        internal const val WINDOWING_MODE_UNDEFINED = 0
        private const val DENSITY_DEFAULT = 160

        /**
         * @see WindowManager.LayoutParams
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
}