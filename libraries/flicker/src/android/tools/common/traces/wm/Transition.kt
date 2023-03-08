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

import android.tools.common.ITraceEntry
import android.tools.common.Timestamp
import android.tools.common.traces.surfaceflinger.Transaction
import kotlin.js.JsName

class Transition(
    @JsName("start") val start: Timestamp,
    @JsName("sendTime") val sendTime: Timestamp,
    @JsName("startTransactionId") val startTransactionId: Long,
    @JsName("finishTransactionId") val finishTransactionId: Long,
    @JsName("changes") val changes: List<TransitionChange>,
    @JsName("played") val played: Boolean,
    @JsName("aborted") val aborted: Boolean
) : ITraceEntry {
    override val timestamp = start

    @JsName("startTransaction") val startTransaction: Transaction? = null // TODO: Get
    @JsName("finishTransaction") val finishTransaction: Transaction? = null // TODO: Get

    @JsName("isIncomplete")
    val isIncomplete: Boolean
        get() = !played || aborted

    override fun toString(): String =
        "Transition#${hashCode()}" +
            "(\naborted=$aborted,\nstart=$start,\nsendTime=$sendTime,\n" +
            "startTransaction=$startTransaction,\nfinishTransaction=$finishTransaction,\n" +
            "changes=[\n${changes.joinToString(",\n").prependIndent()}\n])"

    companion object {
        enum class Type(val value: Int) {
            UNDEFINED(-1),
            NONE(0),
            OPEN(1),
            CLOSE(2),
            TO_FRONT(3),
            TO_BACK(4),
            RELAUNCH(5),
            CHANGE(6),
            KEYGUARD_GOING_AWAY(7),
            KEYGUARD_OCCLUDE(8),
            KEYGUARD_UNOCCLUDE(9),
            PIP(10),
            WAKE(11),
            // START OF CUSTOM TYPES
            FIRST_CUSTOM(12); // TODO: Add custom types we know about

            companion object {
                @JsName("fromInt") fun fromInt(value: Int) = values().first { it.value == value }
            }
        }
    }
}
