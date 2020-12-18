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

package com.android.server.wm.traces.parser.windowmanager

import android.app.ActivityTaskManager
import android.app.WindowConfiguration
import android.content.ComponentName
import android.os.SystemClock
import android.util.Log
import android.view.Display
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.traces.parser.FLAG_STATE_DUMP_FLAG_WM
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.parser.getCurrentState
import com.android.server.wm.traces.common.windowmanager.windows.ConfigurationContainer
import com.android.server.wm.traces.parser.Condition
import com.android.server.wm.traces.parser.LOG_TAG
import com.android.server.wm.traces.parser.toActivityName
import com.android.server.wm.traces.parser.toWindowName

class WindowManagerStateHelper @JvmOverloads constructor(
    /**
     * Predicate to supply a new UI information
     */
    private val stateSupplier: () -> WindowManagerState = {
        val stateDump = getCurrentState(InstrumentationRegistry.getInstrumentation().uiAutomation,
            FLAG_STATE_DUMP_FLAG_WM)
        stateDump.wmTrace?.entries?.first()
            ?: error("Unable to parse Window Manager trace")
    },
    private val numRetries: Int = 5,
    private val retryIntervalMs: Long = 500L
) {
    var state: WindowManagerState = computeState(ignoreInvalidStates = true)
        private set

    private fun computeState(ignoreInvalidStates: Boolean = false): WindowManagerState {
        val newState = stateSupplier.invoke()
        for (retryNr in 0..numRetries) {
            if (!ignoreInvalidStates && newState.isIncomplete()) {
                Log.w(LOG_TAG, "***Incomplete AM state: ${newState.getIsIncompleteReason()}" +
                    " Waiting ${retryIntervalMs}ms and retrying ($retryNr/$numRetries)...")
                SystemClock.sleep(retryIntervalMs)
            } else {
                break
            }
        }
        return newState
    }

    private fun ConfigurationContainer.isWindowingModeCompatible(
        requestedWindowingMode: Int
    ): Boolean {
        return when (requestedWindowingMode) {
            WindowConfiguration.WINDOWING_MODE_UNDEFINED -> true
            WindowConfiguration.WINDOWING_MODE_SPLIT_SCREEN_SECONDARY ->
                (windowingMode == WindowConfiguration.WINDOWING_MODE_FULLSCREEN ||
                    windowingMode == WindowConfiguration.WINDOWING_MODE_SPLIT_SCREEN_SECONDARY)
            else -> windowingMode == requestedWindowingMode
        }
    }

    /**
     * Wait for the activities to appear in proper stacks and for valid state in AM and WM.
     * @param waitForActivitiesVisible array of activity states to wait for.
     */
    fun waitForValidState(vararg waitForActivitiesVisible: WaitForValidActivityState): Boolean {
        val success = Condition.waitFor<WindowManagerState>("valid stacks and activities states",
            retryLimit = numRetries, retryIntervalMs = retryIntervalMs) {
            // TODO: Get state of AM and WM at the same time to avoid mismatches caused by
            // requesting dump in some intermediate state.
            state = computeState()
            !(shouldWaitForValidityCheck() ||
                shouldWaitForValidStacks() ||
                shouldWaitForActivities(*waitForActivitiesVisible) ||
                shouldWaitForWindows())
        }
        if (!success) {
            Log.e(LOG_TAG, "***Waiting for states failed: " +
                waitForActivitiesVisible.contentToString())
        }
        return success
    }

    fun waitForFullScreenApp(componentName: ComponentName): Boolean =
            waitForValidState(
                    WaitForValidActivityState
                            .Builder(componentName)
                            .setWindowingMode(WindowConfiguration.WINDOWING_MODE_FULLSCREEN)
                            .setActivityType(WindowConfiguration.ACTIVITY_TYPE_STANDARD)
                            .build())

    fun waitForHomeActivityVisible(): Boolean {
        // Sometimes this function is called before we know what Home Activity is
        if (state.homeActivityName == null) {
            Log.i(LOG_TAG, "Computing state to determine Home Activity")
            state = computeState()
        }
        val homeActivity = state.homeActivityName
        requireNotNull(homeActivity) { "homeActivity should not be null" }
        return waitForValidState(WaitForValidActivityState(homeActivity))
    }

    fun waitForRecentsActivityVisible(): Boolean =
        if (state.isHomeRecentsComponent) {
            waitForHomeActivityVisible()
        } else {
            waitFor("recents activity to be visible",
                WindowManagerState::isRecentsActivityVisible)
        }

    fun waitForAodShowing(): Boolean =
        waitFor("AOD showing") { it.keyguardControllerState.isAodShowing }

    fun waitForKeyguardGone(): Boolean =
        waitFor("Keyguard gone") { !it.keyguardControllerState.isKeyguardShowing }

    /**
     * Wait for specific rotation for the default display. Values are Surface#Rotation
     */
    @JvmOverloads
    fun waitForRotation(rotation: Int, displayId: Int = Display.DEFAULT_DISPLAY): Boolean =
        waitFor("Rotation: $rotation") { it.getRotation(displayId) == rotation }

    /**
     * Wait for specific orientation for the default display.
     * Values are ActivityInfo.ScreenOrientation
     */
    @JvmOverloads
    fun waitForLastOrientation(
        orientation: Int,
        displayId: Int = Display.DEFAULT_DISPLAY
    ): Boolean =
        waitFor("LastOrientation: $orientation") { it.getOrientation(displayId) == orientation }

    fun waitForActivityState(activity: ComponentName, activityState: String): Boolean {
        val activityName = activity.toActivityName()
        return waitFor("state of $activityName to be $activityState") {
            it.hasActivityState(activityName, activityState)
        }
    }

    fun waitForVisibleWindow(activity: ComponentName): Boolean {
        val activityName = activity.toActivityName()
        val windowName = activity.toWindowName()
        return waitFor("$activityName to exist") {
            (it.containsActivity(activityName) &&
                it.containsWindow(windowName) &&
                it.isActivityVisible(activityName) &&
                it.isWindowSurfaceShown(windowName)
                )
        }
    }

    fun waitForActivityRemoved(activity: ComponentName): Boolean {
        val activityName = activity.toActivityName()
        val windowName = activity.toWindowName()
        return waitFor("$activityName to be removed") {
            (!it.containsActivity(activityName) && !it.containsWindow(windowName))
        }
    }

    fun waitForPendingActivityContain(activity: ComponentName): Boolean {
        val activityName: String = activity.toActivityName()
        return waitFor("${activity.toActivityName()} in pending list") {
            it.pendingActivityContain(activityName)
        }
    }

    @JvmOverloads
    fun waitForAppTransitionIdle(displayId: Int = Display.DEFAULT_DISPLAY): Boolean =
        waitFor("app transition idle on Display $displayId") {
            WindowManagerState.APP_STATE_IDLE == it.getDisplay(displayId)?.appTransitionState
        }

    fun waitForWindowSurfaceDisappeared(windowName: String): Boolean =
        waitFor("$windowName's surface is disappeared") {
            !it.isWindowSurfaceShown(windowName)
        }

    fun waitWindowingModeTopFocus(
        windowingMode: Int,
        topFocus: Boolean,
        message: String
    ): Boolean = waitFor(message) {
        val stack = it.getStandardStackByWindowingMode(windowingMode)
        (stack != null && topFocus == (it.focusedStackId == stack.rootTaskId))
    }

    /**
     * @return `true` if the wait is successful; `false` if timeout occurs.
     */
    @JvmOverloads
    fun waitFor(
        message: String = "",
        waitCondition: (WindowManagerState) -> Boolean
    ): Boolean = Condition.waitFor<WindowManagerState>(message, retryLimit = numRetries,
        retryIntervalMs = retryIntervalMs) {
        state = computeState()
        waitCondition.invoke(this.state)
    }

    /**
     * @return true if should wait for valid stacks state.
     */
    private fun shouldWaitForValidStacks(): Boolean {
        val stackCount: Int = state.stackCount
        if (stackCount == 0) {
            Log.i(LOG_TAG, "***stackCount=$stackCount")
            return true
        }
        val resumedActivitiesCount: Int = state.resumedActivitiesCount
        if (!state.keyguardControllerState.isKeyguardShowing && resumedActivitiesCount < 1) {
            Log.i(LOG_TAG, "***resumedActivitiesCount=$resumedActivitiesCount")
            return true
        }
        if (state.focusedActivity.isEmpty()) {
            Log.i(LOG_TAG, "***focusedActivity=null")
            return true
        }
        return false
    }

    /**
     * @return true if should wait for some activities to become visible.
     */
    private fun shouldWaitForActivities(
        vararg waitForActivitiesVisible: WaitForValidActivityState
    ): Boolean {
        if (waitForActivitiesVisible.isEmpty()) {
            return false
        }
        // If the caller is interested in waiting for some particular activity windows to be
        // visible before compute the state. Check for the visibility of those activity windows
        // and for placing them in correct stacks (if requested).
        var allActivityWindowsVisible = true
        var tasksInCorrectStacks = true
        for (state in waitForActivitiesVisible) {
            val matchingWindowStates = this.state.getMatchingVisibleWindowState(
                state.windowName ?: "")
            val activityWindowVisible = matchingWindowStates.isNotEmpty()

            if (!activityWindowVisible) {
                Log.i(LOG_TAG, "Activity window not visible: ${state.windowName}")
                allActivityWindowsVisible = false
            } else if (state.activityName != null &&
                !this.state.isActivityVisible(state.activityName.toActivityName())) {
                Log.i(LOG_TAG, "Activity not visible: ${state.activityName.toActivityName()}")
                allActivityWindowsVisible = false
            } else {
                // Check if window is already the correct state requested by test.
                var windowInCorrectState = false
                for (ws in matchingWindowStates) {
                    if (state.stackId != ActivityTaskManager.INVALID_STACK_ID &&
                        ws.stackId != state.stackId) {
                        continue
                    }
                    if (!ws.isWindowingModeCompatible(state.windowingMode)) {
                        continue
                    }
                    if (state.activityType != WindowConfiguration.ACTIVITY_TYPE_UNDEFINED &&
                        ws.activityType != state.activityType) {
                        continue
                    }
                    windowInCorrectState = true
                    break
                }
                if (!windowInCorrectState) {
                    Log.i(LOG_TAG, "Window in incorrect stack: $state")
                    tasksInCorrectStacks = false
                }
            }
        }
        return !allActivityWindowsVisible || !tasksInCorrectStacks
    }

    /**
     * @return true if should wait for the valid windows state.
     */
    private fun shouldWaitForWindows(): Boolean {
        return when {
            state.frontWindow == null -> {
                Log.i(LOG_TAG, "***frontWindow=null")
                true
            }
            state.focusedWindow.isEmpty() -> {
                Log.i(LOG_TAG, "***focusedWindow=null")
                true
            }
            state.focusedApp.isEmpty() -> {
                Log.i(LOG_TAG, "***focusedApp=null")
                true
            }
            else -> false
        }
    }

    private fun shouldWaitForValidityCheck(): Boolean {
        return !state.isComplete()
    }

    @JvmOverloads
    fun waitImeWindowShown(displayId: Int = Display.DEFAULT_DISPLAY): Boolean =
        waitFor("IME window") {
            (it.inputMethodWindowState?.isSurfaceShown == true &&
                it.inputMethodWindowState?.displayId == displayId)
        }

    @JvmOverloads
    fun waitImeWindowGone(displayId: Int = Display.DEFAULT_DISPLAY): Boolean =
            waitFor("IME window gone") {
                it.inputMethodWindowState?.isSurfaceShown == false
            }
}