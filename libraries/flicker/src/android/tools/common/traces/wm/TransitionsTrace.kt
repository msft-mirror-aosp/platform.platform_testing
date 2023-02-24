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

import android.tools.common.ITrace
import android.tools.common.Timestamp
import kotlin.js.JsName
import kotlin.text.StringBuilder

data class TransitionsTrace(override val entries: Array<Transition>) : ITrace<Transition> {
    constructor(entry: Transition) : this(arrayOf(entry))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransitionsTrace) return false

        if (!entries.contentEquals(other.entries)) return false

        return true
    }

    override fun hashCode(): Int {
        return entries.contentHashCode()
    }

    @JsName("prettyPrint")
    fun prettyPrint(): String {
        val sb = StringBuilder("TransitionTrace(")

        for (transition in entries) {
            sb.append("\n\t- ").append(transition)
        }
        if (entries.isEmpty()) {
            sb.append("EMPTY)")
        } else {
            sb.append("\n)")
        }
        return sb.toString()
    }

    override fun slice(startTimestamp: Timestamp, endTimestamp: Timestamp): TransitionsTrace {
        require(startTimestamp.hasElapsedTimestamp && endTimestamp.hasElapsedTimestamp)
        return sliceElapsed(startTimestamp.elapsedNanos, endTimestamp.elapsedNanos)
    }

    private fun sliceElapsed(from: Long, to: Long): TransitionsTrace {
        return TransitionsTrace(
            this.entries
                .dropWhile { it.sendTime.elapsedNanos < from }
                .dropLastWhile { it.start.elapsedNanos > to }
                .toTypedArray()
        )
    }
}
