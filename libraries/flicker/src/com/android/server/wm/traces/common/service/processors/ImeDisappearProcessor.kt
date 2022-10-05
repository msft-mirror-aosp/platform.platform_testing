/*
 * Copyright (C) 2022 The Android Open Source Project
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

import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.ConditionList
import com.android.server.wm.traces.common.DeviceStateDump
import com.android.server.wm.traces.common.WindowManagerConditionsFactory.isImeShown
import com.android.server.wm.traces.common.WindowManagerConditionsFactory.isLayerColorAlphaOne
import com.android.server.wm.traces.common.WindowManagerConditionsFactory.isLayerTransformFlagSet
import com.android.server.wm.traces.common.layers.Transform
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.service.Scenario
import com.android.server.wm.traces.common.tags.Tag

/**
 * This processor creates tags when the keyboard starts and finishes disappearing.
 * @param logger logs by invoking any event messages
 */
class ImeDisappearProcessor(logger: (String) -> Unit) : TransitionProcessor(logger) {
    override val scenario = Scenario.IME_DISAPPEAR
    override fun getInitialState(tags: MutableMap<Long, MutableList<Tag>>) =
        WaitImeDisappearStart(tags)

    /**
     * FSM state that waits until the IME begins to disappear Different conditions required for IME
     * closing by gesture (layer color alpha < 1), compared to IME closing via app close (layer
     * translate SCALE_VAL bit set)
     */
    inner class WaitImeDisappearStart(tags: MutableMap<Long, MutableList<Tag>>) : BaseState(tags) {
        private val isImeShown = isImeShown(PlatformConsts.DEFAULT_DISPLAY)
        private val isImeAppeared =
            ConditionList(
                listOf(
                    isImeShown,
                    isLayerColorAlphaOne(ComponentNameMatcher.IME),
                    isLayerTransformFlagSet(ComponentNameMatcher.IME, Transform.TRANSLATE_VAL),
                    isLayerTransformFlagSet(ComponentNameMatcher.IME, Transform.SCALE_VAL).negate()
                )
            )
        private val isImeDisappearByGesture =
            ConditionList(
                listOf(isImeShown, isLayerColorAlphaOne(ComponentNameMatcher.IME).negate())
            )
        private val isImeDisappearByAppClose =
            ConditionList(
                listOf(
                    isImeShown,
                    isLayerTransformFlagSet(ComponentNameMatcher.IME, Transform.SCALE_VAL)
                )
            )

        override fun doProcessState(
            previous: DeviceStateDump?,
            current: DeviceStateDump,
            next: DeviceStateDump
        ): FSMState {
            if (previous == null) return this

            return if (
                isImeAppeared.isSatisfied(previous) &&
                    (isImeDisappearByGesture.isSatisfied(current) ||
                        isImeDisappearByAppClose.isSatisfied(current))
            ) {
                processImeDisappearing(current)
            } else {
                logger.invoke("(${current.layerState.timestamp}) IME disappear not started.")
                this
            }
        }

        private fun processImeDisappearing(current: DeviceStateDump): FSMState {
            logger.invoke("(${current.layerState.timestamp}) IME disappear started.")
            val inputMethodLayer =
                current.layerState.visibleLayers.first {
                    ComponentNameMatcher.IME.layerMatchesAnyOf(it)
                }
            addStartTransitionTag(current, scenario, layerId = inputMethodLayer.id)
            return WaitImeDisappearFinished(tags, inputMethodLayer.id)
        }
    }

    /**
     * FSM state to check when the IME disappear has finished i.e. when the input method layer is no
     * longer visible.
     */
    inner class WaitImeDisappearFinished(
        tags: MutableMap<Long, MutableList<Tag>>,
        private val layerId: Int
    ) : BaseState(tags) {
        private val imeNotShown = isImeShown(PlatformConsts.DEFAULT_DISPLAY).negate()
        override fun doProcessState(
            previous: DeviceStateDump?,
            current: DeviceStateDump,
            next: DeviceStateDump
        ): FSMState {
            return if (imeNotShown.isSatisfied(current)) {
                // tag on the last complete state at the start
                logger.invoke("(${current.layerState.timestamp}) IME disappear end detected.")
                addEndTransitionTag(current, scenario, layerId = layerId)
                // return to start to wait for a second IME disappear
                WaitImeDisappearStart(tags)
            } else {
                logger.invoke("(${current.layerState.timestamp}) IME disappear not finished.")
                this
            }
        }
    }
}
