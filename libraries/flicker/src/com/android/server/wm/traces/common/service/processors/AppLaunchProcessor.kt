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
import com.android.server.wm.traces.common.WindowManagerConditionsFactory.hasLayersAnimating
import com.android.server.wm.traces.common.WindowManagerConditionsFactory.isAppTransitionIdle
import com.android.server.wm.traces.common.WindowManagerConditionsFactory.isWMStateComplete
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.service.PlatformConsts.TYPE_APPLICATION_STARTING
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.Transition
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.windows.WindowState

class AppLaunchProcessor(logger: (String) -> Unit) : TransitionProcessor(logger) {
    private val transition = Transition.APP_LAUNCH

    override fun getInitialState(tags: MutableMap<Long, MutableList<Tag>>) =
        WaitUntilSnapshotLayersStartAnimating(tags)

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
                        addEndTransitionTag(current, transition)
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
    inner class WaitUntilSnapshotLayersStartAnimating(
        tags: MutableMap<Long, MutableList<Tag>>
    ) : BaseState(tags) {
        override fun doProcessState(
            previous: DeviceStateDump<WindowManagerState, LayerTraceEntry>?,
            current: DeviceStateDump<WindowManagerState, LayerTraceEntry>,
            next: DeviceStateDump<WindowManagerState, LayerTraceEntry>
        ): FSMState {
            if (previous == null) return this

            val startingWindows = current.wmState.rootTasks.flatMap { task ->
                task.activities.flatMap { activity ->
                    activity.children.filterIsInstance<WindowState>().filter { window ->
                        window.attributes.type == TYPE_APPLICATION_STARTING
                    }
                }
            }

            val snapshotLayersAreAnimating = startingWindows.toList().filter { window ->
                val prevLayer = previous.layerState.getLayerById(window.layerId)
                val currLayer = current.layerState.getLayerById(window.layerId)
                if (prevLayer != null && currLayer != null) {
                    !prevLayer.isScaling && currLayer.isScaling
                } else {
                    false
                }
            }
            // Only want to tag when one app is being launched.
            // Other scenarios like app pairs enter are ignored.
            return if (snapshotLayersAreAnimating.size == 1) {
                val layerId = snapshotLayersAreAnimating.first().layerId
                addStartTransitionTag(previous, transition,
                    layerId = layerId,
                    timestamp = previous.layerState.timestamp
                )
                WaitUntilAppSnapshotLayerIsIdentity(tags, layerId)
            } else {
                this
            }
        }
    }

    inner class WaitUntilAppSnapshotLayerIsIdentity(
        tags: MutableMap<Long, MutableList<Tag>>,
        private val layerId: Int
    ) : BaseState(tags) {
        private val areLayersAnimating = hasLayersAnimating()
        private val wmStateIdle = isAppTransitionIdle(/* default display */ 0)
        private val wmStateComplete = isWMStateComplete()

        override fun doProcessState(
            previous: DeviceStateDump<WindowManagerState, LayerTraceEntry>?,
            current: DeviceStateDump<WindowManagerState, LayerTraceEntry>,
            next: DeviceStateDump<WindowManagerState, LayerTraceEntry>
        ): FSMState {
            val snapshotLayerGone = current.layerState.getLayerById(layerId) == null
            val isStableState = wmStateIdle.isSatisfied(current) ||
                wmStateComplete.isSatisfied(current) ||
                areLayersAnimating.negate().isSatisfied(current)

            return if (snapshotLayerGone && isStableState) {
                addEndTransitionTag(current, transition,
                    layerId = layerId,
                    timestamp = current.layerState.timestamp
                )
                WaitUntilSnapshotLayersStartAnimating(tags)
            } else {
                this
            }
        }
    }
}
