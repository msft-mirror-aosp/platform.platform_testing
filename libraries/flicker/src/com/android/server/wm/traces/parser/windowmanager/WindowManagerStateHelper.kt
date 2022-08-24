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
import android.os.SystemClock
import android.util.Log
import android.view.Display
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.ComponentNameMatcher.Companion.IME
import com.android.server.wm.traces.common.ComponentNameMatcher.Companion.LAUNCHER
import com.android.server.wm.traces.common.ComponentNameMatcher.Companion.SNAPSHOT
import com.android.server.wm.traces.common.ComponentNameMatcher.Companion.SPLASH_SCREEN
import com.android.server.wm.traces.common.ComponentNameMatcher.Companion.SPLIT_DIVIDER
import com.android.server.wm.traces.common.Condition
import com.android.server.wm.traces.common.DeviceStateDump
import com.android.server.wm.traces.common.IComponentMatcher
import com.android.server.wm.traces.common.WaitCondition
import com.android.server.wm.traces.common.WindowManagerConditionsFactory
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.region.Region
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.common.windowmanager.windows.Activity
import com.android.server.wm.traces.common.windowmanager.windows.ConfigurationContainer
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
import com.android.server.wm.traces.parser.LOG_TAG
import com.android.server.wm.traces.parser.getCurrentStateDump

/**
 * Helper class to wait on [WindowManagerState] or [LayerTraceEntry] conditions
 */
open class WindowManagerStateHelper @JvmOverloads constructor(
    /**
     * Instrumentation to run the tests
     */
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
    private val clearCacheAfterParsing: Boolean = true,
    /**
     * Predicate to supply a new UI information
     */
    private val deviceDumpSupplier: () -> DeviceStateDump = {
        getCurrentStateDump(
            instrumentation.uiAutomation,
            clearCacheAfterParsing = clearCacheAfterParsing
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
    private var internalState: DeviceStateDump? = null

    /**
     * Queries the supplier for a new device state
     */
    val currentState: DeviceStateDump
        get() {
            if (internalState == null) {
                internalState = deviceDumpSupplier.invoke()
            } else {
                StateSyncBuilder().withValidState().waitFor()
            }
            return internalState ?: error("Unable to fetch an internal state")
        }

    protected open fun updateCurrState(value: DeviceStateDump) {
        internalState = value
    }

    /**
     * @return a [WindowState] from the current device state matching [componentMatcher],
     * or null otherwise
     *
     * @param componentMatcher Components to search
     */
    fun getWindow(componentMatcher: IComponentMatcher): WindowState? {
        return this.currentState.wmState.windowStates
            .firstOrNull { componentMatcher.windowMatchesAnyOf(it) }
    }

    /**
     * @return The frame [Region] a [WindowState] matching [componentMatcher]
     *
     * @param componentMatcher Components to search
     */
    fun getWindowRegion(componentMatcher: IComponentMatcher): Region =
        getWindow(componentMatcher)?.frameRegion ?: Region.EMPTY

    /**
     * Class to build conditions for waiting on specific [WindowManagerTrace] and [LayersTrace]
     * conditions
     */
    inner class StateSyncBuilder {
        private val conditionBuilder = createConditionBuilder()
        private var lastMessage = ""

        private fun createConditionBuilder(): WaitCondition.Builder<DeviceStateDump> =
            WaitCondition.Builder(deviceDumpSupplier, numRetries).onSuccess { updateCurrState(it) }
                .onFailure { updateCurrState(it) }.onLog { msg, isError ->
                    lastMessage = msg
                    if (isError) {
                        Log.e(LOG_TAG, msg)
                    } else {
                        Log.d(LOG_TAG, msg)
                    }
                }.onRetry { SystemClock.sleep(retryIntervalMs) }

        /**
         * Adds a new [condition] to the list
         *
         * @param condition to wait for
         */
        fun add(condition: Condition<DeviceStateDump>): StateSyncBuilder = apply {
            conditionBuilder.withCondition(condition)
        }

        /**
         * Adds a new [condition] to the list
         *
         * @param message describing the condition
         * @param condition to wait for
         */
        @JvmOverloads
        fun add(message: String = "", condition: (DeviceStateDump) -> Boolean): StateSyncBuilder =
            add(Condition(message, condition))

        /**
         * Waits until the list of conditions added to [conditionBuilder] are satisfied
         *
         * @return if the device state passed all conditions or not
         */
        fun waitFor(): Boolean {
            val passed = conditionBuilder.build().waitFor()
            // Ensure WindowManagerService wait until all animations have completed
            instrumentation.waitForIdleSync()
            instrumentation.uiAutomation.syncInputTransactions()
            return passed
        }

        /**
         * Waits until the list of conditions added to [conditionBuilder] are satisfied and
         * verifies the device state passes all conditions
         *
         * @throws IllegalArgumentException if the conditions were not met
         */
        fun waitForAndVerify() {
            val success = waitFor()
            require(success) { lastMessage }
        }

        /**
         * Waits for an app matching [componentMatcher] to be visible, in full screen, and for
         * nothing to be animating
         *
         * @param componentMatcher Components to search
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withFullScreenApp(
            componentMatcher: IComponentMatcher,
            displayId: Int = Display.DEFAULT_DISPLAY
        ) = withFullScreenAppCondition(componentMatcher)
            .withAppTransitionIdle(displayId)
            .add(WindowManagerConditionsFactory.isLayerVisible(componentMatcher))

        /**
         * Waits until the home activity is visible and nothing to be animating
         *
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withHomeActivityVisible(displayId: Int = Display.DEFAULT_DISPLAY) =
            withAppTransitionIdle(displayId)
                .withNavOrTaskBarVisible()
                .withStatusBarVisible()
                .add(WindowManagerConditionsFactory.isHomeActivityVisible())
                .add(WindowManagerConditionsFactory.isLayerVisible(LAUNCHER))

        /**
         * Waits until the split-screen divider is visible and nothing to be animating
         *
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withSplitDividerVisible(displayId: Int = Display.DEFAULT_DISPLAY) =
            withAppTransitionIdle(displayId)
                .add(WindowManagerConditionsFactory.isLayerVisible(SPLIT_DIVIDER))

        /**
         * Waits until the home activity is visible and nothing to be animating
         *
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withRecentsActivityVisible(displayId: Int = Display.DEFAULT_DISPLAY) =
            withAppTransitionIdle(displayId)
                .add(WindowManagerConditionsFactory.isRecentsActivityVisible())
                .add(WindowManagerConditionsFactory.isLayerVisible(LAUNCHER))

        /**
         * Wait for specific rotation for the display with id [displayId]
         *
         * @param rotation expected. Values are [Surface#Rotation]
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withRotation(rotation: Int, displayId: Int = Display.DEFAULT_DISPLAY) =
            withAppTransitionIdle(displayId)
                .add(WindowManagerConditionsFactory.hasRotation(rotation, displayId))

        /**
         * Waits until a [WindowState] matching [componentMatcher] has a state of [activityState]
         *
         * @param componentMatcher Components to search
         * @param activityState expected activity state
         */
        fun withActivityState(componentMatcher: IComponentMatcher, activityState: String) =
            add(Condition(
                    "state of ${componentMatcher.toActivityIdentifier()} to be $activityState") {
                it.wmState.hasActivityState(componentMatcher, activityState)
            })

        /**
         * Waits until the [ComponentNameMatcher.NAV_BAR] or [ComponentNameMatcher.TASK_BAR] are
         * visible (windows and layers)
         */
        fun withNavOrTaskBarVisible() = add(WindowManagerConditionsFactory.isNavOrTaskBarVisible())

        /**
         * Waits until the navigation and status bars are visible (windows and layers)
         */
        fun withStatusBarVisible() = add(WindowManagerConditionsFactory.isStatusBarVisible())

        /**
         * Wait until neither an [Activity] nor a [WindowState] matching [componentMatcher] exist
         * on the display with id [displayId] and for nothing to be animating
         *
         * @param componentMatcher Components to search
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withActivityRemoved(
            componentMatcher: IComponentMatcher,
            displayId: Int = Display.DEFAULT_DISPLAY
        ) = withAppTransitionIdle(displayId)
            .add(WindowManagerConditionsFactory.containsActivity(componentMatcher).negate())
            .add(WindowManagerConditionsFactory.containsWindow(componentMatcher).negate())

        /**
         * Wait until the splash screen and snapshot starting windows no longer exist, no layers
         * are animating, and [WindowManagerState] is idle on display [displayId]
         *
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withAppTransitionIdle(displayId: Int = Display.DEFAULT_DISPLAY) =
            withSplashScreenGone()
                .withSnapshotGone()
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(displayId))
                .add(WindowManagerConditionsFactory.hasLayersAnimating().negate())

        /**
         * Wait until least one [WindowState] matching [componentMatcher] is not visible on
         * display with idd [displayId] and nothing is animating
         *
         * @param componentMatcher Components to search
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withWindowSurfaceDisappeared(
            componentMatcher: IComponentMatcher,
            displayId: Int = Display.DEFAULT_DISPLAY
        ) = withAppTransitionIdle(displayId)
            .add(WindowManagerConditionsFactory.isWindowSurfaceShown(componentMatcher).negate())
            .add(WindowManagerConditionsFactory.isLayerVisible(componentMatcher).negate())
            .add(WindowManagerConditionsFactory.isAppTransitionIdle(displayId))

        /**
         * Wait until least one [WindowState] matching [componentMatcher] is visible on display
         * with idd [displayId] and nothing is animating
         *
         * @param componentMatcher Components to search
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withWindowSurfaceAppeared(
            componentMatcher: IComponentMatcher,
            displayId: Int = Display.DEFAULT_DISPLAY
        ) = withAppTransitionIdle(displayId)
            .add(WindowManagerConditionsFactory.isWindowSurfaceShown(componentMatcher))
            .add(WindowManagerConditionsFactory.isLayerVisible(componentMatcher))

        /**
         * Waits until the IME window and layer are visible
         *
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withImeShown(displayId: Int = Display.DEFAULT_DISPLAY) =
            withAppTransitionIdle(displayId)
                .add(WindowManagerConditionsFactory.isImeShown(displayId))

        /**
         * Waits until the [IME] layer is no longer visible.
         *
         * Cannot wait for the window as its visibility information is updated at a later state
         * and is not reliable in the trace
         *
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withImeGone(displayId: Int = Display.DEFAULT_DISPLAY) =
            withAppTransitionIdle(displayId)
                .add(WindowManagerConditionsFactory.isLayerVisible(IME).negate())

        /**
         * Waits until a window is in PIP mode. That is:
         *
         * - wait until a window is pinned ([WindowManagerState.pinnedWindows])
         * - no layers animating
         * - and [ComponentNameMatcher.PIP_CONTENT_OVERLAY] is no longer visible
         *
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withPipShown(displayId: Int = Display.DEFAULT_DISPLAY) =
            withAppTransitionIdle(displayId)
                .add(WindowManagerConditionsFactory.hasPipWindow())

        /**
         * Waits until a window is no longer in PIP mode. That is:
         *
         * - wait until there are no pinned ([WindowManagerState.pinnedWindows])
         * - no layers animating
         * - and [ComponentNameMatcher.PIP_CONTENT_OVERLAY] is no longer visible
         *
         * @param displayId of the target display
         */
        @JvmOverloads
        fun withPipGone(displayId: Int = Display.DEFAULT_DISPLAY) =
            withAppTransitionIdle(displayId)
                .add(WindowManagerConditionsFactory.hasPipWindow().negate())

        /**
         * Waits until the [SNAPSHOT] is gone
         */
        fun withSnapshotGone() =
            add(WindowManagerConditionsFactory.isLayerVisible(SNAPSHOT).negate())

        /**
         * Waits until the [SPLASH_SCREEN] is gone
         */
        fun withSplashScreenGone() =
            add(WindowManagerConditionsFactory.isLayerVisible(SPLASH_SCREEN).negate())

        /**
         * Waits until the is no top visible app window in the [WindowManagerState]
         */
        fun withoutTopVisibleAppWindows() = add("noAppWindowsOnTop") {
            it.wmState.topVisibleAppWindow == null
        }

        /**
         * Wait for the activities to appear in proper stacks and for valid state in AM and WM.
         *
         * @param waitForActivityState array of activity states to wait for.
         */
        internal fun withValidState(vararg waitForActivityState: WaitForValidActivityState) =
            waitForValidStateCondition(*waitForActivityState)

        private fun waitForValidStateCondition(vararg waitForCondition: WaitForValidActivityState) =
            apply {
                add(WindowManagerConditionsFactory.isWMStateComplete())
                if (waitForCondition.isNotEmpty()) {
                    add(Condition("!shouldWaitForActivities") {
                        !shouldWaitForActivities(it, *waitForCondition)
                    })
                }
            }

        fun withFullScreenAppCondition(componentMatcher: IComponentMatcher) =
            waitForValidStateCondition(
                WaitForValidActivityState
                    .Builder(componentMatcher)
                    .setWindowingMode(WindowConfiguration.WINDOWING_MODE_FULLSCREEN)
                    .setActivityType(WindowConfiguration.ACTIVITY_TYPE_STANDARD)
                    .build()
            )
    }

    companion object {
        /**
         * @return true if it should wait for some activities to become visible.
         */
        private fun shouldWaitForActivities(
            state: DeviceStateDump,
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
                    activityState.activityMatcher
                            ?: error("Activity name missing in $activityState")
                )
                val activityWindowVisible = matchingWindowStates.isNotEmpty()

                if (!activityWindowVisible) {
                    Log.i(LOG_TAG, "Activity window not visible: ${activityState.windowIdentifier}")
                    allActivityWindowsVisible = false
                } else if (!state.wmState.isActivityVisible(activityState.activityMatcher)
                ) {
                    Log.i(LOG_TAG, "Activity not visible: ${activityState.activityMatcher}")
                    allActivityWindowsVisible = false
                } else {
                    // Check if window is already the correct state requested by test.
                    var windowInCorrectState = false
                    for (ws in matchingWindowStates) {
                        if (activityState.stackId != ActivityTaskManager.INVALID_STACK_ID &&
                            ws.stackId != activityState.stackId
                        ) {
                            continue
                        }
                        if (!ws.isWindowingModeCompatible(activityState.windowingMode)) {
                            continue
                        }
                        if (activityState.activityType !=
                            WindowConfiguration.ACTIVITY_TYPE_UNDEFINED &&
                            ws.activityType != activityState.activityType
                        ) {
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

        private fun ConfigurationContainer.isWindowingModeCompatible(
            requestedWindowingMode: Int
        ): Boolean {
            return when (requestedWindowingMode) {
                WindowConfiguration.WINDOWING_MODE_UNDEFINED -> true
                else -> windowingMode == requestedWindowingMode
            }
        }
    }
}
