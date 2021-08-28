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

package com.android.server.wm.flicker.service.processors

import android.util.Log
import com.android.server.wm.traces.common.RectF
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.Transition
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.parser.windowmanager.WindowManagerConditionsFactory
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper

/**
 * Processor to detect rotations.
 *
 * First check the WM state for a rotation change, then wait the SF rotation
 * to occur and both nav and status bars to appear
 */
class RotationProcessor : TransitionProcessor() {
    override fun getInitialState(tags: MutableMap<Long, MutableList<Tag>>) = InitialState(tags)

    /**
     * Base state for the FSM, check if there are more WM and SF states to process,
     * if so, process, otherwise closes open tags and returns null
     */
    abstract class BaseState(tags: MutableMap<Long, MutableList<Tag>>) : FSMState(tags) {
        abstract fun doProcessState(
            previous: WindowManagerStateHelper.Dump?,
            current: WindowManagerStateHelper.Dump,
            next: WindowManagerStateHelper.Dump
        ): FSMState

        override fun process(
            previous: WindowManagerStateHelper.Dump?,
            current: WindowManagerStateHelper.Dump,
            next: WindowManagerStateHelper.Dump?
        ): FSMState? {
            return when (next) {
                null -> {
                    // last state
                    Log.v(LOG_TAG, "(${current.layerState.timestamp}) Trace has reached the end")
                    if (hasOpenTag()) {
                        Log.v(LOG_TAG, "(${current.layerState.timestamp}) Has an open tag, " +
                            "closing it on the last SF state")
                        addEndTransitionTag(current, Transition.ROTATION)
                    }
                    null
                }
                else -> doProcessState(previous, current, next)
            }
        }
    }

    /**
     * Initial FSM state, obtains the current display size and start searching
     * for display size changes
     */
    open class InitialState(
        tags: MutableMap<Long, MutableList<Tag>>
    ) : BaseState(tags) {
        override fun doProcessState(
            previous: WindowManagerStateHelper.Dump?,
            current: WindowManagerStateHelper.Dump,
            next: WindowManagerStateHelper.Dump
        ): FSMState {
            val currDisplayRect = current.wmState.displaySize()
            Log.v(LOG_TAG, "(${current.wmState.timestamp}) Initial state. " +
                "Display size $currDisplayRect")
            return WaitDisplayRectChange(tags, currDisplayRect)
        }
    }

    /**
     * FSM state when the display size has not changed since [InitialState]
     */
    class WaitDisplayRectChange(
        tags: MutableMap<Long, MutableList<Tag>>,
        private val currDisplayRect: RectF
    ) : BaseState(tags) {
        override fun doProcessState(
            previous: WindowManagerStateHelper.Dump?,
            current: WindowManagerStateHelper.Dump,
            next: WindowManagerStateHelper.Dump
        ): FSMState {
            val newWmDisplayRect = current.wmState.displaySize()
            val newLayersDisplayRect = current.layerState.screenBounds()

            return when {
                // WM display changed first (Regular rotation)
                // SF display changed first (Seamless rotation)
                newWmDisplayRect != currDisplayRect || newLayersDisplayRect != currDisplayRect -> {
                    requireNotNull(previous) { "Should have a previous state" }
                    val rect = if (newWmDisplayRect != currDisplayRect) {
                        newWmDisplayRect
                    } else {
                        newLayersDisplayRect
                    }
                    processDisplaySizeChange(previous, rect)
                }
                else -> {
                    Log.v(LOG_TAG, "(${current.wmState.timestamp}) No display size change")
                    this
                }
            }
        }

        private fun processDisplaySizeChange(
            previous: WindowManagerStateHelper.Dump,
            newDisplayRect: RectF
        ): FSMState {
            Log.v(LOG_TAG, "(${previous.wmState.timestamp}) Display size changed " +
                "to $newDisplayRect")
            // tag on the last complete state at the start
            Log.v(LOG_TAG, "(${previous.wmState.timestamp}) Tagging transition start")
            addStartTransitionTag(previous, Transition.ROTATION)
            return WaitRotationFinished(tags)
        }
    }

    /**
     * FSM state for when the animation occurs in the SF trace
     */
    class WaitRotationFinished(tags: MutableMap<Long, MutableList<Tag>>) : BaseState(tags) {
        private val rotationLayerExists = WindowManagerConditionsFactory
            .isLayerVisible(WindowManagerStateHelper.ROTATION_COMPONENT)
        private val backSurfaceLayerExists = WindowManagerConditionsFactory
            .isLayerVisible(WindowManagerStateHelper.BLACK_SURFACE_COMPONENT)
        private val areLayersAnimating = WindowManagerConditionsFactory.hasLayersAnimating()
        private val wmStateIdle = WindowManagerConditionsFactory.isAppTransitionIdle()
        private val wmStateComplete = WindowManagerConditionsFactory.isWMStateComplete()

        override fun doProcessState(
            previous: WindowManagerStateHelper.Dump?,
            current: WindowManagerStateHelper.Dump,
            next: WindowManagerStateHelper.Dump
        ): FSMState {
            val anyLayerAnimating = areLayersAnimating.isSatisfied(current)
            val rotationLayerExists = rotationLayerExists.isSatisfied(current)
            val blackSurfaceLayerExists = backSurfaceLayerExists.isSatisfied(current)
            val wmStateIdle = wmStateIdle.isSatisfied(current)
            val wmStateComplete = wmStateComplete.isSatisfied(current)

            val newWmDisplayRect = current.wmState.displaySize()
            val newLayersDisplayRect = current.layerState.screenBounds()
            val displaySizeDifferent = newWmDisplayRect != newLayersDisplayRect

            val inRotation = anyLayerAnimating || rotationLayerExists || blackSurfaceLayerExists ||
                displaySizeDifferent || !wmStateIdle || !wmStateComplete
            Log.v(LOG_TAG, "(${current.layerState.timestamp}) " +
                "In rotation? $inRotation (" +
                "anyLayerAnimating=$anyLayerAnimating, " +
                "blackSurfaceLayerExists=$blackSurfaceLayerExists, " +
                "rotationLayerExists=$rotationLayerExists, " +
                "wmStateIdle=$wmStateIdle, " +
                "wmStateComplete=$wmStateComplete, " +
                "displaySizeDifferent=$displaySizeDifferent)")
            return if (inRotation) {
                this
            } else {
                // tag on the last complete state at the start
                Log.v(LOG_TAG, "(${current.layerState.timestamp}) Tagging transition end")
                addEndTransitionTag(current, Transition.ROTATION)
                // return to start to wait for a second rotation
                val lastDisplayRect = current.wmState.displaySize()
                WaitDisplayRectChange(tags, lastDisplayRect)
            }
        }
    }

    companion object {
        @JvmStatic
        private fun LayerTraceEntry.screenBounds() = this.children
            .sortedBy { it.id }
            .firstOrNull { it.isRootLayer }
            ?.screenBounds ?: RectF.EMPTY

        @JvmStatic
        private fun WindowManagerState.displaySize() = getDefaultDisplay()
            ?.displayRect?.toRectF() ?: RectF.EMPTY
    }
}