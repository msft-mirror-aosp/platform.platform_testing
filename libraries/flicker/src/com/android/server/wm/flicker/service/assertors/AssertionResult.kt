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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.flicker.traces.FlickerSubjectException
import com.android.server.wm.flicker.traces.layers.LayerSubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowStateSubject
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.service.AssertionInvocationGroup
import com.android.server.wm.traces.common.service.Scenario
import com.android.server.wm.traces.common.windowmanager.windows.WindowState

typealias AssertionEvaluator =
    (wmSubject: WindowManagerTraceSubject, layerSubject: LayersTraceSubject) -> Unit

/**
 * Base class for a FASS assertion
 */
data class AssertionResult(
    val assertionName: String,
    val scenario: Scenario,
    val invocationGroup: AssertionInvocationGroup,
    val assertionError: FlickerSubjectException?
) {
    val failed: Boolean get() = (assertionError !== null)

    /**
     * Returns the layer responsible for the failure, if any
     *
     * @param tag a list with all [TransitionTag]s
     * @param wmSubject Window Manager trace subject
     * @param layerSubject Surface Flinger trace subject
     */
    fun getFailureLayer(
        wmSubject: WindowManagerTraceSubject,
        layerSubject: LayersTraceSubject
    ): Layer? {
        val failureSubject = assertionError?.subject
        return if (failureSubject is LayerSubject) {
            failureSubject.layer
        } else {
            null
        }
    }

    /**
     * Returns the window responsible for the last failure, if any
     *
     * @param tag a list with all [TransitionTag]s
     * @param wmSubject Window Manager trace subject
     * @param layerSubject Surface Flinger trace subject
     */
    fun getFailureWindow(
        wmSubject: WindowManagerTraceSubject,
        layerSubject: LayersTraceSubject
    ): WindowState? {
        val failureSubject = assertionError?.subject
        return if (failureSubject is WindowStateSubject) {
            failureSubject.windowState
        } else {
            null
        }
    }
}
