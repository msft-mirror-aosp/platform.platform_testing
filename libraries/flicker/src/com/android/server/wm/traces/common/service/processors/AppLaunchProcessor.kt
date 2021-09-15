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

package com.android.server.wm.traces.common.service.processors

import com.android.server.wm.traces.common.DeviceStateDump
import com.android.server.wm.traces.common.WindowManagerConditionsFactory.isAppLaunchEnded
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.Transition
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.windows.Activity
import com.android.server.wm.traces.common.windowmanager.windows.WindowState

class AppLaunchProcessor(logger: (String) -> Unit) : TransitionProcessor(logger) {
    override fun getInitialState(tags: MutableMap<Long, MutableList<Tag>>) =
            InitialState(tags)

    /**
     * Base state for the FSM, check if there are more WM and SF states to process
     */
    abstract inner class BaseState(tags: MutableMap<Long, MutableList<Tag>>) : FSMState(tags) {
        protected abstract fun doProcessState(
            previous: DeviceStateDump<WindowManagerState, LayerTraceEntry>?,
            current: DeviceStateDump<WindowManagerState, LayerTraceEntry>,
            next: DeviceStateDump<WindowManagerState, LayerTraceEntry>
        ): FSMState

        override fun process(
            previous: DeviceStateDump<WindowManagerState, LayerTraceEntry>?,
            current: DeviceStateDump<WindowManagerState, LayerTraceEntry>,
            next: DeviceStateDump<WindowManagerState, LayerTraceEntry>?
        ): FSMState? {
            return when (next) {
                null -> {
                    // last state
                    logger.invoke("(${current.layerState.timestamp}) Trace has reached the end")
                    if (hasOpenTag()) {
                        logger.invoke("(${current.layerState.timestamp}) Has an open tag, " +
                                "closing it on the last SF state")
                        addEndTransitionTag(current, Transition.IME_APPEAR)
                    }
                    null
                }
                else -> doProcessState(previous, current, next)
            }
        }
    }

    /**
     * Initial FSM state that passes the current app launch activity if any to the next state.
     */
    inner class InitialState(
        tags: MutableMap<Long, MutableList<Tag>>
    ) : BaseState(tags) {
        override fun doProcessState(
            previous: DeviceStateDump<WindowManagerState, LayerTraceEntry>?,
            current: DeviceStateDump<WindowManagerState, LayerTraceEntry>,
            next: DeviceStateDump<WindowManagerState, LayerTraceEntry>
        ): FSMState {
            val prevTaskActivities = filterVisibleAppStartActivities(current.wmState)
            val prevAppLaunchActivity = appLaunchActivityWithSurface(prevTaskActivities)

            return WaitNewAppLaunchActivity(tags, prevAppLaunchActivity)
        }
    }

    /**
     * Finds the app launch when a new [WindowManagerState] contains an activity that is resuming or
     * initializing, with a SplashScreen Window that has type
     * [PlatformConsts.TYPE_APPLICATION_STARTING] and window's surface is showing. This condition
     * should not be true in the previous timestamp.
     */
    inner class WaitNewAppLaunchActivity(
        tags: MutableMap<Long, MutableList<Tag>>,
        private val prevAppLaunchActivity: Activity?
    ) : BaseState(tags) {
        override fun doProcessState(
            previous: DeviceStateDump<WindowManagerState, LayerTraceEntry>?,
            current: DeviceStateDump<WindowManagerState, LayerTraceEntry>,
            next: DeviceStateDump<WindowManagerState, LayerTraceEntry>
        ): FSMState {
            if (previous == null) return this

            /**
             * Activities that have started and surfaced that were not already doing so in the
             * previous timestamp.
             */
            val currTaskActivities = filterVisibleAppStartActivities(current.wmState)
            val currAppLaunchActivity = appLaunchActivityWithSurface(currTaskActivities)

            val startingActivityName = if (currAppLaunchActivity != null &&
                currAppLaunchActivity != prevAppLaunchActivity) {
                currAppLaunchActivity.name
            } else ""

            return if (startingActivityName.isNotEmpty()) {
                val taskId = current.wmState.rootTasks.first {
                    it.containsActivity(startingActivityName)
                }.taskId
                logger.invoke("(${current.wmState.timestamp}) " +
                    "Task $taskId appears to have launched")
                addStartTransitionTag(current, Transition.IME_APPEAR, taskId = taskId)
                WaitAppLaunchEnded(tags, taskId)
            } else {
                logger.invoke("(${current.wmState.timestamp}) No Start of App Launch Detected")
                WaitNewAppLaunchActivity(tags, currAppLaunchActivity)
            }
        }
    }

    fun filterVisibleAppStartActivities(
        wmState: WindowManagerState
    ): List<Activity> {
        return wmState.rootTasks.flatMap { task ->
            task.activities.filter {
                (it.state == "RESUMED" || it.state == "INITIALIZING") && it.isVisible
            }
        }
    }

    fun appLaunchActivityWithSurface(
        activities: List<Activity>
    ): Activity? {
        return activities.firstOrNull { activity ->
            activity.children.filterIsInstance<WindowState>().any { window ->
                window.attributes.type == PlatformConsts.TYPE_APPLICATION_STARTING &&
                    window.isSurfaceShown
            }
        }
    }

    /**
     * Wait for SplashScreen window under the app task to no longer be visible as the splash screen
     * has finished its job.
     */
    inner class WaitAppLaunchEnded(
        tags: MutableMap<Long, MutableList<Tag>>,
        private val taskId: Int
    ) : BaseState(tags) {
        override fun doProcessState(
            previous: DeviceStateDump<WindowManagerState, LayerTraceEntry>?,
            current: DeviceStateDump<WindowManagerState, LayerTraceEntry>,
            next: DeviceStateDump<WindowManagerState, LayerTraceEntry>
        ): FSMState {
            val timestamp = current.wmState.timestamp

            return if (isAppLaunchEnded(taskId).isSatisfied(current)) {
                logger.invoke("($timestamp) App has finished launching with task $taskId")
                addEndTransitionTag(current, Transition.APP_LAUNCH, taskId = taskId)
                InitialState(tags)
            } else {
                logger.invoke("($timestamp) No end of app launch detected")
                this
            }
        }
    }
}