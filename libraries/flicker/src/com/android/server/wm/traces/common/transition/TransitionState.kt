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

package com.android.server.wm.traces.common.transition

import kotlin.js.JsName

data class TransitionState(
    @JsName("id") val id: Int,
    @JsName("type") val type: Transition.Companion.Type,
    @JsName("timestamp") val timestamp: Long,
    @JsName("state") val state: State,
    @JsName("flags") val flags: Int,
    @JsName("changes") val changes: List<TransitionChange>,
    @JsName("startTransactionId") val startTransactionId: Long,
    @JsName("finishTransactionId") val finishTransactionId: Long
) {
    companion object {
        enum class State(val value: Int) {
            PENDING(-1),
            COLLECTING(0),
            STARTED(1),
            PLAYING(2),
            ABORT(3),
            FINISHED(4);

            companion object {
                @JsName("fromInt") fun fromInt(value: Int) = values().first { it.value == value }
            }
        }
    }
}
