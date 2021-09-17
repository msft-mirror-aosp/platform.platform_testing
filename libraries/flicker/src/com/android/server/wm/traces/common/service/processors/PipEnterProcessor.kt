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

import com.android.server.wm.traces.common.ConditionList
import com.android.server.wm.traces.common.DeviceStateDump
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.Transform
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.Transition
import com.android.server.wm.traces.common.WindowManagerConditionsFactory
import com.android.server.wm.traces.common.WindowManagerConditionsFactory.isAppTransitionIdle
import com.android.server.wm.traces.common.WindowManagerConditionsFactory.hasPipWindow
import com.android.server.wm.traces.common.WindowManagerConditionsFactory.isPipWindowLayerSizeMatch
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.windowmanager.WindowManagerState

/**
 * Processor to detect PIP mode enter.
 * Waits for a window to enter pip mode, then tags transition start at the last moment before
 * corresponding layer started scaling.
 * Tags transition end when window and layer stop animating and their sizes match.
 */
class PipEnterProcessor(logger: (String) -> Unit) : TransitionProcessor(logger) {
    private val allScalingLayers = mutableMapOf<Int, Long>()

    override fun getInitialState(tags: MutableMap<Long, MutableList<Tag>>) =
        WaitPipEnterStart(tags)

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
            return if (next != null) {
                doProcessState(previous, current, next)
            } else {
                // last state
                logger.invoke("(${current.layerState.timestamp}) Trace has reached the end")
                if (hasOpenTag()) {
                    logger.invoke("(${current.layerState.timestamp}) Has an open tag, " +
                        "closing it on the last layer state")
                    addEndTransitionTag(current, Transition.PIP_ENTER)
                }
                null
            }
        }
    }

    /**
     * FSM state that waits for a pinned window to appear. Until this occurs, it watches all
     * scaling layers, recording when they started to scale in the companion object. When the
     * pinned window has appeared, it adds a start tag at the timestamp at which the layer with
     * the same id as the pinned window layerId started to scale.
     */
    inner class WaitPipEnterStart(
        tags: MutableMap<Long, MutableList<Tag>>
    ) : BaseState(tags) {

        private fun getScalingLayers(
            current: DeviceStateDump<WindowManagerState, LayerTraceEntry>
        ): List<Layer> = current.layerState.flattenedLayers.filter {
            WindowManagerConditionsFactory.isLayerTransformFlagSet(
                it.id,
                Transform.SCALE_VAL
            ).isSatisfied(current)
        }

        override fun doProcessState(
            previous: DeviceStateDump<WindowManagerState, LayerTraceEntry>?,
            current: DeviceStateDump<WindowManagerState, LayerTraceEntry>,
            next: DeviceStateDump<WindowManagerState, LayerTraceEntry>
        ): FSMState {
            if (previous == null) return this

            // get current scaling layers and add to allScalingLayers map for whole trace
            val scalingLayers = getScalingLayers(current)
            val scalingLayerIds = scalingLayers.map { it.id }
            scalingLayerIds.forEach {
                allScalingLayers.getOrPut(it) { previous.layerState.timestamp }
            }

            // remove all layers that are no longer scaling from allScalingLayers map
            val notScalingLayerIds = allScalingLayers.keys.filter { !scalingLayerIds.contains(it) }
            notScalingLayerIds.forEach {
                allScalingLayers.remove(it)
            }

            return if (hasPipWindow().isSatisfied(current) &&
                hasPipWindow().negate().isSatisfied(previous)) {
                processPipEnterStart(current)
            } else {
                logger.invoke("(${current.layerState.timestamp}) PIP enter not started.")
                this
            }
        }

        private fun processPipEnterStart(
            current: DeviceStateDump<WindowManagerState, LayerTraceEntry>
        ): FSMState {
            val pipWindow = current.wmState.pinnedWindows.first()
            val startTimestamp = allScalingLayers.get(pipWindow.layerId)

            if (startTimestamp != null) {
                addStartTransitionTag(
                        current,
                        Transition.PIP_ENTER,
                        layerId = pipWindow.layerId,
                        timestamp = startTimestamp
                )
            } else {
                addStartTransitionTag(
                        current,
                        Transition.PIP_ENTER,
                        layerId = pipWindow.layerId
                )
            }
            // reset all scaling layers
            allScalingLayers.clear()

            logger.invoke("($startTimestamp) PIP enter started.")
            return WaitPipEnterFinished(tags, pipWindow.layerId)
        }
    }

    /**
     * FSM state to check when the PIP enter has finished. This is when the pinned window
     * has the same size as the associated layer.
     */
    inner class WaitPipEnterFinished(
        tags: MutableMap<Long, MutableList<Tag>>,
        private val layerId: Int
    ) : BaseState(tags) {
        override fun doProcessState(
            previous: DeviceStateDump<WindowManagerState, LayerTraceEntry>?,
            current: DeviceStateDump<WindowManagerState, LayerTraceEntry>,
            next: DeviceStateDump<WindowManagerState, LayerTraceEntry>
        ): FSMState {
            return if (isPipEnterFinished.isSatisfied(current)) {
                // tag on the last complete state at the start
                logger.invoke("(${current.wmState.timestamp}) PIP enter finished.")
                addEndTransitionTag(current, Transition.PIP_ENTER, layerId = layerId)

                // return to start to wait for a second PIP enter
                WaitPipEnterStart(tags)
            } else {
                logger.invoke("(${current.wmState.timestamp}) PIP enter not finished.")
                this
            }
        }

        private val isPipEnterFinished = ConditionList(listOf(
            isAppTransitionIdle(PlatformConsts.DEFAULT_DISPLAY),
            hasPipWindow(),
            isPipWindowLayerSizeMatch(layerId)
        ))
    }
}