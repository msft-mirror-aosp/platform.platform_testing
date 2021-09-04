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
import android.app.Instrumentation
import android.app.WindowConfiguration
import android.content.ComponentName
import android.graphics.Rect
import android.graphics.Region
import android.util.Log
import android.view.Display
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.windows.ConfigurationContainer
import com.android.server.wm.traces.common.windowmanager.windows.WindowContainer
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
import com.android.server.wm.traces.parser.Condition
import com.android.server.wm.traces.parser.LOG_TAG
import com.android.server.wm.traces.parser.WaitCondition
import com.android.server.wm.traces.parser.getCurrentStateDump
import com.android.server.wm.traces.parser.toActivityName
import com.android.server.wm.traces.parser.toAndroidRegion
import com.android.server.wm.traces.parser.toWindowName

open class WindowManagerStateHelper @JvmOverloads constructor(
    /**
     * Instrumentation to run the tests
     */
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
    /**
     * Predicate to supply a new UI information
     */
    private val deviceDumpSupplier: () -> Dump = {
        val currState = getCurrentStateDump(
            instrumentation.uiAutomation)
        Dump(
            currState.wmTrace?.entries?.first() ?: error("Unable to parse WM trace"),
            currState.layersTrace?.entries?.first() ?: error("Unable to parse Layers trace")
        )
    },
    /**
     * Number of attempts to satisfy a wait condition
     */
    private val numRetries: Int = WaitCondition.DEFAULT_RETRY_LIMIT,
    /**
     * Interval between wait for state dumps during wait conditions
     */
    private val retryIntervalMs: Long = WaitCondition.DEFAULT_RETRY_INTERVAL_MS
) {
    private var internalState: Dump? = null

    /**
     * Queries the supplier for a new device state
     *
     * @param ignoreInvalidStates If false, retries up to [numRetries] times (with a sleep
     * interval of [retryIntervalMs] ms to obtain a complete WM state, otherwise returns the
     * first state
     */
    val currentState: Dump
        get() {
            if (internalState == null) {
                internalState = deviceDumpSupplier.invoke()
            } else {
                waitForValidState()
            }
            return internalState ?: error("Unable to fetch an internal state")
    }

    protected open fun updateCurrState(value: Dump) {
        internalState = value
    }

    private fun createConditionBuilder(): WaitCondition.Builder<Dump> =
        WaitCondition.Builder(deviceDumpSupplier, numRetries, retryIntervalMs)
            .onSuccess { updateCurrState(it) }
            .onFailure { updateCurrState(it) }

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
        val builder = createConditionBuilder()
            .withCondition(WindowManagerConditionsFactory.isWMStateComplete())

        if (waitForActivitiesVisible.isNotEmpty()) {
            builder.withCondition("!shouldWaitForActivities") {
                !shouldWaitForActivities(it, *waitForActivitiesVisible)
            }
        }
        val success = builder.build().waitFor()
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

    fun waitForHomeActivityVisible(): Boolean =
        createConditionBuilder()
            .withCondition(WindowManagerConditionsFactory.isHomeActivityVisible())
            .withCondition(WindowManagerConditionsFactory.isAppTransitionIdle())
            .withCondition(WindowManagerConditionsFactory.isNavBarVisible())
            .withCondition(WindowManagerConditionsFactory.isStatusBarVisible())
            .build()
            .waitFor()

    fun waitForRecentsActivityVisible(): Boolean =
        createConditionBuilder()
            .withCondition("isRecentsActivityVisible") {
                it.wmState.isRecentsActivityVisible
            }
            .build()
            .waitFor()

    /**
     * Wait for specific rotation for the default display. Values are Surface#Rotation
     */
    @JvmOverloads
    fun waitForRotation(rotation: Int, displayId: Int = Display.DEFAULT_DISPLAY): Boolean {
        val hasRotationCondition = WindowManagerConditionsFactory.hasRotation(rotation, displayId)
        return createConditionBuilder()
            .withCondition("waitForRotation[$rotation]") {
                if (!it.wmState.canRotate) {
                    Log.v(LOG_TAG, "Rotation is not allowed in the state")
                    true
                } else {
                    hasRotationCondition.isSatisfied(it)
                }
            }
            .withCondition(WindowManagerConditionsFactory.isAppTransitionIdle())
            .build()
            .waitFor()
    }

    fun waitForActivityState(activity: ComponentName, activityState: String): Boolean {
        val activityName = activity.toActivityName()
        return createConditionBuilder()
            .withCondition("state of $activityName to be $activityState") {
                it.wmState.hasActivityState(activityName, activityState)
            }
            .build()
            .waitFor()
    }

    /**
     * Waits until the navigation and status bars are visible (windows and layers)
     */
    fun waitForNavBarStatusBarVisible(): Boolean =
        createConditionBuilder()
            .withCondition(WindowManagerConditionsFactory.isNavBarVisible())
            .withCondition(WindowManagerConditionsFactory.isStatusBarVisible())
            .build()
            .waitFor()

    fun waitForVisibleWindow(component: ComponentName): Boolean =
        createConditionBuilder()
            .withCondition(WindowManagerConditionsFactory.containsActivity(component))
            .withCondition(WindowManagerConditionsFactory.containsWindow(component))
            .withCondition(WindowManagerConditionsFactory.isActivityVisible(component))
            .withCondition(WindowManagerConditionsFactory.isWindowSurfaceShown(component))
            .withCondition(WindowManagerConditionsFactory.isAppTransitionIdle())
            .build()
            .waitFor()

    fun waitForActivityRemoved(component: ComponentName): Boolean =
        createConditionBuilder()
            .withCondition(WindowManagerConditionsFactory.containsActivity(component).negate())
            .withCondition(WindowManagerConditionsFactory.containsWindow(component).negate())
            .withCondition(WindowManagerConditionsFactory.isAppTransitionIdle())
            .build()
            .waitFor()

    @JvmOverloads
    fun waitForAppTransitionIdle(displayId: Int = Display.DEFAULT_DISPLAY): Boolean =
        createConditionBuilder()
            .withCondition(WindowManagerConditionsFactory.isAppTransitionIdle(displayId))
            .build()
            .waitFor()

    fun waitForWindowSurfaceDisappeared(componentName: ComponentName): Boolean {
        val condition = WindowManagerConditionsFactory.isWindowSurfaceShown(componentName).negate()
        return createConditionBuilder()
            .withCondition(condition)
            .withCondition(WindowManagerConditionsFactory.isAppTransitionIdle())
            .build()
            .waitFor()
    }

    fun waitForSurfaceAppeared(surfaceName: String): Boolean =
        createConditionBuilder()
            .withCondition(WindowManagerConditionsFactory.isWindowSurfaceShown(surfaceName))
            .withCondition(WindowManagerConditionsFactory.isAppTransitionIdle())
            .build()
            .waitFor()

    fun waitFor(vararg conditions: Condition<Dump>): Boolean {
        val builder = createConditionBuilder()
        conditions.forEach { builder.withCondition(it) }
        return builder.build().waitFor()
    }

    @JvmOverloads
    fun waitFor(
        message: String = "",
        waitCondition: (Dump) -> Boolean
    ): Boolean = createConditionBuilder()
            .withCondition(message, waitCondition)
            .build()
            .waitFor()

    /**
     * @return true if should wait for some activities to become visible.
     */
    private fun shouldWaitForActivities(
        state: Dump,
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
        for (activityState in waitForActivitiesVisible) {
            val matchingWindowStates = state.wmState.getMatchingVisibleWindowState(
                activityState.windowName ?: "")
            val activityWindowVisible = matchingWindowStates.isNotEmpty()

            if (!activityWindowVisible) {
                Log.i(LOG_TAG, "Activity window not visible: ${activityState.windowName}")
                allActivityWindowsVisible = false
            } else if (activityState.activityName != null &&
                !state.wmState.isActivityVisible(activityState.activityName.toActivityName())) {
                Log.i(LOG_TAG, "Activity not visible: ${activityState.activityName}")
                allActivityWindowsVisible = false
            } else {
                // Check if window is already the correct state requested by test.
                var windowInCorrectState = false
                for (ws in matchingWindowStates) {
                    if (activityState.stackId != ActivityTaskManager.INVALID_STACK_ID &&
                        ws.stackId != activityState.stackId) {
                        continue
                    }
                    if (!ws.isWindowingModeCompatible(activityState.windowingMode)) {
                        continue
                    }
                    if (activityState.activityType != WindowConfiguration.ACTIVITY_TYPE_UNDEFINED &&
                        ws.activityType != activityState.activityType) {
                        continue
                    }
                    windowInCorrectState = true
                    break
                }
                if (!windowInCorrectState) {
                    Log.i(LOG_TAG, "Window in incorrect stack: $activityState")
                    tasksInCorrectStacks = false
                }
            }
        }
        return !allActivityWindowsVisible || !tasksInCorrectStacks
    }

    /**
     * Waits until the IME window and layer are visible
     */
    @JvmOverloads
    fun waitImeShown(displayId: Int = Display.DEFAULT_DISPLAY): Boolean =
        createConditionBuilder()
            .withCondition(WindowManagerConditionsFactory.isImeShown(displayId))
            .withCondition(WindowManagerConditionsFactory.isAppTransitionIdle())
            .build()
            .waitFor()

    /**
     * Waits until the IME layer is no longer visible. Cannot wait for the window as
     * its visibility information is updated at a later state and is not reliable in
     * the trace
     */
    fun waitImeGone(): Boolean =
        createConditionBuilder()
            .withCondition(WindowManagerConditionsFactory.isLayerVisible(IME_COMPONENT).negate())
            .withCondition(WindowManagerConditionsFactory.isAppTransitionIdle())
            .build()
            .waitFor()

    /**
     * Obtains a [WindowContainer] from the current device state, or null if the WindowContainer
     * doesn't exist
     */
    fun getWindow(activity: ComponentName): WindowState? {
        val windowName = activity.toWindowName()
        return this.currentState.wmState.windowStates
            .firstOrNull { it.title == windowName }
    }

    /**
     * Obtains the region of a window in the state, or an empty [Rect] is there are none
     */
    fun getWindowRegion(activity: ComponentName): Region {
        val window = getWindow(activity)
        return window?.frameRegion?.toAndroidRegion() ?: Region()
    }

    companion object {
        @VisibleForTesting
        @JvmStatic
        val NAV_BAR_COMPONENT = ComponentName("", "NavigationBar0")
        @VisibleForTesting
        @JvmStatic
        val STATUS_BAR_COMPONENT = ComponentName("", "StatusBar")
        @VisibleForTesting
        @JvmStatic
        val ROTATION_COMPONENT = ComponentName("", "RotationLayer")
        @VisibleForTesting
        @JvmStatic
        val BLACK_SURFACE_COMPONENT = ComponentName("", "BackColorSurface")
        @VisibleForTesting
        @JvmStatic
        val IME_COMPONENT = ComponentName("", "InputMethod")
        @VisibleForTesting
        @JvmStatic
        val SPLASH_SCREEN_COMPONENT = ComponentName("", "Splash Screen")
        @VisibleForTesting
        @JvmStatic
        val SNAPSHOT_COMPONENT = ComponentName("", "SnapshotStartingWindow")
    }

    data class Dump(
        /**
         * Window manager state
         */
        @JvmField val wmState: WindowManagerState,
        /**
         * Layers state
         */
        @JvmField val layerState: LayerTraceEntry
    )
}
