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

package android.tools.device.traces.parsers.wm

import android.tools.common.CrossPlatform
import android.tools.common.Timestamp
import android.tools.common.parsers.AbstractTraceParser
import android.tools.common.traces.wm.Transition
import android.tools.common.traces.wm.Transition.Companion.Type
import android.tools.common.traces.wm.TransitionChange
import android.tools.common.traces.wm.TransitionsTrace
import android.tools.common.traces.wm.WindowingMode
import com.android.server.wm.shell.nano.TransitionTraceProto

/** Parser for [TransitionsTrace] objects */
class TransitionsTraceParser :
    AbstractTraceParser<
        TransitionTraceProto,
        com.android.server.wm.shell.nano.Transition,
        Transition,
        TransitionsTrace
    >() {
    override val traceName: String = "Transition trace"

    override fun createTrace(entries: List<Transition>): TransitionsTrace {
        return TransitionsTrace(entries.toTypedArray())
    }

    override fun doDecodeByteArray(bytes: ByteArray): TransitionTraceProto =
        TransitionTraceProto.parseFrom(bytes)

    override fun shouldParseEntry(entry: com.android.server.wm.shell.nano.Transition): Boolean {
        return true
    }

    override fun getEntries(
        input: TransitionTraceProto
    ): List<com.android.server.wm.shell.nano.Transition> = input.sentTransitions.toList()

    override fun getTimestamp(entry: com.android.server.wm.shell.nano.Transition): Timestamp {
        return CrossPlatform.timestamp.from(elapsedNanos = entry.createTimeNs)
    }

    override fun onBeforeParse(input: TransitionTraceProto) {}

    override fun doParseEntry(entry: com.android.server.wm.shell.nano.Transition): Transition {
        val windowingMode = WindowingMode.WINDOWING_MODE_UNDEFINED // TODO: Get the windowing mode
        val changes =
            entry.targets.map {
                TransitionChange(Type.fromInt(it.mode), it.layerId, it.windowId, windowingMode)
            }

        return Transition(
            start = CrossPlatform.timestamp.from(elapsedNanos = entry.createTimeNs),
            sendTime = CrossPlatform.timestamp.from(elapsedNanos = entry.sendTimeNs),
            startTransactionId = entry.startTransactionId,
            finishTransactionId = entry.finishTransactionId,
            changes = changes,
            played = true,
            aborted = false,
        )
    }
}
