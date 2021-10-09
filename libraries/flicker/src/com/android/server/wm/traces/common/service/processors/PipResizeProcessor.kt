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
import com.android.server.wm.traces.common.WindowManagerConditionsFactory.isLayerTransformFlagSet
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.layers.Transform
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.Transition
import com.android.server.wm.traces.common.windowmanager.WindowManagerState

class PipResizeProcessor(logger: (String) -> Unit) : TransitionProcessor(logger) {
    private val transition = Transition.PIP_RESIZE
    private val scalingWindows =
        HashMap<String, DeviceStateDump<WindowManagerState, LayerTraceEntry>>()

    override fun getInitialState(tags: MutableMap<Long, MutableList<Tag>>) =
        WaitUntilAppStopsAnimatingYetStillPinned(tags)

    /**
     * Base state for the FSM, check if there are more WM and SF states to process,
     * if so, process, otherwise closes open tags and returns null
     */
    abstract inner class BaseState(tags: MutableMap<Long, MutableList<Tag>>) : FSMState(tags) {
        abstract fun doProcessState(
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

    inner class WaitUntilAppStopsAnimatingYetStillPinned(
        tags: MutableMap<Long, MutableList<Tag>>
    ) : BaseState(tags) {
        override fun doProcessState(
            previous: DeviceStateDump<WindowManagerState, LayerTraceEntry>?,
            current: DeviceStateDump<WindowManagerState, LayerTraceEntry>,
            next: DeviceStateDump<WindowManagerState, LayerTraceEntry>
        ): FSMState {
            val currPinnedWindow = current.wmState.pinnedWindows.firstOrNull() ?: return this
            previous?.wmState?.pinnedWindows?.firstOrNull() ?: return this

            val isScaling = isLayerTransformFlagSet(currPinnedWindow.layerId, Transform.SCALE_VAL)
            val startedScaling = isScaling.negate().isSatisfied(previous) &&
                isScaling.isSatisfied(current)
            if (startedScaling) {
                // remember when pinned window/layer starting scaling
                scalingWindows[currPinnedWindow.token] = previous
            }

            // Bounds have changed and layer no longer scaling
            val currBounds = current.layerState.getLayerById(currPinnedWindow.layerId)?.bounds
            val prevBounds = previous.layerState.getLayerById(currPinnedWindow.layerId)?.bounds
            val finishedResizing = isScaling.isSatisfied(previous) &&
                isScaling.negate().isSatisfied(current) &&
                (currBounds?.height != prevBounds?.height) &&
                (currBounds?.width != prevBounds?.width)

            if (finishedResizing) {
                val lastScaledDump = scalingWindows[currPinnedWindow.token]
                if (lastScaledDump != null) {
                    addStartTransitionTag(lastScaledDump, transition,
                        layerId = currPinnedWindow.layerId,
                        windowToken = currPinnedWindow.token,
                        timestamp = lastScaledDump.layerState.timestamp
                    )
                    addEndTransitionTag(lastScaledDump, transition,
                        layerId = currPinnedWindow.layerId,
                        windowToken = currPinnedWindow.token,
                        timestamp = current.layerState.timestamp
                    )
                    scalingWindows.remove(currPinnedWindow.token)
                }
            }
            return this
        }
    }
}
