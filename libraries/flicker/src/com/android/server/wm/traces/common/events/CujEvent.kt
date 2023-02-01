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

package com.android.server.wm.traces.common.events

import com.android.server.wm.traces.common.Timestamp

class CujEvent(
    timestamp: Timestamp,
    val cuj: CujType,
    processId: Int,
    uid: String,
    threadId: Int,
    tag: String
) : Event(timestamp, processId, uid, threadId, tag) {

    val type: Type =
        when (tag) {
            JANK_CUJ_BEGIN_TAG -> Type.START
            JANK_CUJ_END_TAG -> Type.END
            JANK_CUJ_CANCEL_TAG -> Type.CANCEL
            else -> error("Unhandled tag type")
        }

    constructor(
        timestamp: Timestamp,
        processId: Int,
        uid: String,
        threadId: Int,
        tag: String,
        data: String
    ) : this(timestamp, getCujMarkerFromData(data), processId, uid, threadId, tag)

    override fun toString(): String {
        return "CujEvent(timestamp=$timestamp, cuj=$cuj, processId=$processId, uid=$uid, threadId=$threadId, tag=$tag)"
    }

    companion object {
        private fun getCujMarkerFromData(data: String): CujType {
            val eventId = data.toIntOrNull()
            requireNotNull(eventId) { "Data expected to be an integer" }
            return CujType.from(eventId)
        }

        enum class Type {
            START,
            END,
            CANCEL
        }

        const val JANK_CUJ_BEGIN_TAG = "jank_cuj_events_begin_request"
        const val JANK_CUJ_END_TAG = "jank_cuj_events_end_request"
        const val JANK_CUJ_CANCEL_TAG = "jank_cuj_events_cancel_request"
    }
}
