/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.common.traces.wm

import android.tools.common.traces.wm.Transition.Companion.Type
import kotlin.js.JsName

class TransitionChange(
    @JsName("transitMode") val transitMode: Type,
    @JsName("layerId") val layerId: Int,
    @JsName("windowId") val windowId: Int,
    @JsName("windowingMode") val windowingMode: WindowingMode
) {

    override fun toString(): String {
        return "TransitionChange(" +
            "transitMode=$transitMode, " +
            "layerId=$layerId, " +
            "windowId=$windowId, " +
            "windowingMode=$windowingMode" +
            ")"
    }
}
