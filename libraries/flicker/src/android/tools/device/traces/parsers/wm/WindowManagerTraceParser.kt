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
import android.tools.common.traces.wm.WindowManagerState
import android.tools.common.traces.wm.WindowManagerTrace
import com.android.server.wm.nano.WindowManagerTraceFileProto
import com.android.server.wm.nano.WindowManagerTraceProto

/** Parser for [WindowManagerTrace] objects containing traces */
open class WindowManagerTraceParser(private val legacyTrace: Boolean = false) :
    AbstractTraceParser<
        WindowManagerTraceFileProto, WindowManagerTraceProto, WindowManagerState, WindowManagerTrace
    >() {
    private var realToElapsedTimeOffsetNanos = 0L

    override val traceName: String = "WM Trace"

    override fun doDecodeByteArray(bytes: ByteArray): WindowManagerTraceFileProto =
        WindowManagerTraceFileProto.parseFrom(bytes)

    override fun createTrace(entries: List<WindowManagerState>): WindowManagerTrace =
        WindowManagerTrace(entries.toTypedArray())

    override fun getEntries(input: WindowManagerTraceFileProto): List<WindowManagerTraceProto> =
        input.entry.toList()

    override fun getTimestamp(entry: WindowManagerTraceProto): Timestamp {
        require(legacyTrace || realToElapsedTimeOffsetNanos != 0L)
        return CrossPlatform.timestamp.from(
            elapsedNanos = entry.elapsedRealtimeNanos,
            unixNanos = entry.elapsedRealtimeNanos + realToElapsedTimeOffsetNanos
        )
    }

    override fun onBeforeParse(input: WindowManagerTraceFileProto) {
        realToElapsedTimeOffsetNanos = input.realToElapsedTimeOffsetNanos
    }

    override fun doParseEntry(entry: WindowManagerTraceProto): WindowManagerState {
        return WindowManagerStateBuilder()
            .atPlace(entry.where)
            .forTimestamp(entry.elapsedRealtimeNanos)
            .withRealTimeOffset(realToElapsedTimeOffsetNanos)
            .forProto(entry.windowManagerService)
            .build()
    }
}
