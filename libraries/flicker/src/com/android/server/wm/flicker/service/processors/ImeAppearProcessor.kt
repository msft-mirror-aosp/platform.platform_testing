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
import com.android.server.wm.traces.parser.windowmanager.WindowManagerConditionsFactory.isLayerVisible
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper.Companion.IME_COMPONENT

class ImeAppearProcessor : TransitionProcessor() {
    override fun getInitialState(tags: MutableMap<Long, MutableList<Tag>>) =
            WaitInputMethodVisible(tags)

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
                    addEndTransitionTag(current, Transition.IME_APPEAR)
                }
                null
            }
        }
    }

    /**
     * FSM state that waits until the InputMethod is visible in both WM and SF.
     */
    class WaitInputMethodVisible(
        tags: MutableMap<Long, MutableList<Tag>>
    ) : BaseState(tags) {
        private val newImeVisible = isImeShown()
        private val prevImeInvisible = newImeVisible.negate()

    override fun doProcessState(
        previous: WindowManagerStateHelper.Dump?,
        current: WindowManagerStateHelper.Dump,
        next: WindowManagerStateHelper.Dump
    ): FSMState {
            if (previous == null) return this

            return if (newImeVisible.isSatisfied(current) &&
                    prevImeInvisible.isSatisfied(previous)) {
                processInputMethodVisible(current)
            } else {
                this
            }
        }

        private fun processInputMethodVisible(
            current: WindowManagerStateHelper.Dump
        ): FSMState {
            Log.v(LOG_TAG, "(${current.layerState.timestamp}) IME appear started.")
            // add factory method as well
            val inputMethodLayer = current.layerState.visibleLayers.first {
                it.name.contains(IME_COMPONENT.toLayerName())
            }
            addStartTransitionTag(current, Transition.IME_APPEAR, layerId = inputMethodLayer.id)
            return WaitImeAppearFinished(tags, inputMethodLayer.id)
        }
    }

    /**
    * FSM state to check when the Ime Appear has finished by opaque color alpha of input method
     * and it has finished transforming and scaling.
    */
    class WaitImeAppearFinished(
        tags: MutableMap<Long, MutableList<Tag>>,
        private val layerId: Int
    ) : BaseState(tags) {
        override fun doProcessState(
            previous: WindowManagerStateHelper.Dump?,
            current: WindowManagerStateHelper.Dump,
            next: WindowManagerStateHelper.Dump
        ): FSMState {
            val isImeAppearFinished = isImeAppearFinished.isSatisfied(current)

            return if (isImeAppearFinished) {
                // tag on the last complete state at the start
                Log.v(LOG_TAG, "(${current.layerState.timestamp}) Ime appear end detected.")
                addEndTransitionTag(current, Transition.IME_APPEAR, layerId = layerId)
                // return to start to wait for a second IME appear
                WaitInputMethodVisible(tags)
            } else {
                Log.v(LOG_TAG, "(${current.layerState.timestamp}) Ime appear hasn't finished.")
                this
            }
        }

        private val isImeAppearFinished = ConditionList(listOf(
                    isLayerVisible(IME_COMPONENT),
                    isLayerColorAlphaOne(IME_COMPONENT),
                    isLayerTransformFlagSet(IME_COMPONENT, Transform.TRANSLATE_VAL),
                    isLayerTransformFlagSet(IME_COMPONENT, Transform.SCALE_VAL).negate()
            ))
    }
}