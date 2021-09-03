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
import com.android.server.wm.traces.common.layers.Transform
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.Transition
import com.android.server.wm.traces.parser.ConditionList
import com.android.server.wm.traces.parser.toLayerName
import com.android.server.wm.traces.parser.windowmanager.WindowManagerConditionsFactory.isImeShown
import com.android.server.wm.traces.parser.windowmanager.WindowManagerConditionsFactory.isLayerColorAlphaOne
import com.android.server.wm.traces.parser.windowmanager.WindowManagerConditionsFactory.isLayerTransformFlagSet
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper.Companion.IME_COMPONENT

class ImeDisappearProcessor : TransitionProcessor() {
    override fun getInitialState(tags: MutableMap<Long, MutableList<Tag>>) =
        WaitImeDisappearStart(tags)

    /**
     * Base state for the FSM, check if there are more WM and SF states to process
     */
    abstract class BaseState(tags: MutableMap<Long, MutableList<Tag>>) : FSMState(tags) {
        protected abstract fun doProcessState(
            previous: WindowManagerStateHelper.Dump?,
            current: WindowManagerStateHelper.Dump,
            next: WindowManagerStateHelper.Dump
        ): FSMState

        override fun process(
            previous: WindowManagerStateHelper.Dump?,
            current: WindowManagerStateHelper.Dump,
            next: WindowManagerStateHelper.Dump?
        ): FSMState? {
            return if (next != null) {
                doProcessState(previous, current, next)
            } else {
                // last state
                Log.v(LOG_TAG,
                        "(${current.layerState.timestamp}) Trace has reached the end")
                if (hasOpenTag()) {
                    Log.v(LOG_TAG, "(${current.layerState.timestamp}) Has an open tag, " +
                            "closing it on the last SF state")
                    addEndTransitionTag(current, Transition.IME_DISAPPEAR)
                }
                null
            }
        }
    }

    /**
     * FSM state that waits until the IME begins to disappear
     * Different conditions required for IME closing by gesture (layer color alpha < 1), compared
     * to IME closing via app close (layer translate SCALE_VAL bit set)
     */
    class WaitImeDisappearStart(
        tags: MutableMap<Long, MutableList<Tag>>
    ) : BaseState(tags) {
        private val isImeShown = isImeShown()
        private val isImeAppeared =
            ConditionList(listOf(
                isImeShown,
                isLayerColorAlphaOne(IME_COMPONENT),
                isLayerTransformFlagSet(IME_COMPONENT, Transform.TRANSLATE_VAL),
                isLayerTransformFlagSet(IME_COMPONENT, Transform.SCALE_VAL).negate()
            ))
        private val isImeDisappearByGesture =
            ConditionList(listOf(
                isImeShown,
                isLayerColorAlphaOne(IME_COMPONENT).negate()
            ))
        private val isImeDisappearByAppClose =
            ConditionList(listOf(
                isImeShown,
                isLayerTransformFlagSet(IME_COMPONENT, Transform.SCALE_VAL)
            ))

        override fun doProcessState(
            previous: WindowManagerStateHelper.Dump?,
            current: WindowManagerStateHelper.Dump,
            next: WindowManagerStateHelper.Dump
        ): FSMState {
            if (previous == null) return this

            return if (isImeAppeared.isSatisfied(previous) &&
                (isImeDisappearByGesture.isSatisfied(current) ||
                isImeDisappearByAppClose.isSatisfied(current))) {
                processImeDisappearing(current)
            } else {
                Log.v(LOG_TAG, "(${current.layerState.timestamp}) IME disappear not started.")
                this
            }
        }

        private fun processImeDisappearing(
            current: WindowManagerStateHelper.Dump
        ): FSMState {
            Log.v(LOG_TAG, "(${current.layerState.timestamp}) IME disappear started.")
            val inputMethodLayer = current.layerState.visibleLayers.first {
                it.name.contains(IME_COMPONENT.toLayerName())
            }
            addStartTransitionTag(current, Transition.IME_DISAPPEAR, layerId = inputMethodLayer.id)
            return WaitImeDisappearFinished(tags, inputMethodLayer.id)
        }
    }

    /**
     * FSM state to check when the IME disappear has finished i.e. when the input method layer is
     * no longer visible.
     */
    class WaitImeDisappearFinished(
        tags: MutableMap<Long, MutableList<Tag>>,
        private val layerId: Int
    ) : BaseState(tags) {
        override fun doProcessState(
            previous: WindowManagerStateHelper.Dump?,
            current: WindowManagerStateHelper.Dump,
            next: WindowManagerStateHelper.Dump
        ): FSMState {
            return if (isImeShown().negate().isSatisfied(current)) {
                // tag on the last complete state at the start
                Log.v(LOG_TAG, "(${current.layerState.timestamp}) IME disappear end detected.")
                addEndTransitionTag(current, Transition.IME_DISAPPEAR, layerId = layerId)
                // return to start to wait for a second IME disappear
                WaitImeDisappearStart(tags)
            } else {
                Log.v(LOG_TAG, "(${current.layerState.timestamp}) IME disappear not finished.")
                this
            }
        }
    }
}