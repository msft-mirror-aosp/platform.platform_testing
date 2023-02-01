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

package com.android.server.wm.flicker.io

import com.android.server.wm.flicker.TestTraces
import com.android.server.wm.flicker.readAssetAsFile
import com.android.server.wm.traces.common.Timestamp

/** Tests for [ResultReader] parsing [TraceType.TRANSITION] */
class ResultReaderTestParseTransitions : BaseResultReaderTestParseTrace() {
    override val assetFile = TestTraces.TransitionTrace.FILE
    override val traceName = "Transitions trace"
    override val startTimeTrace = TestTraces.TransitionTrace.START_TIME
    override val endTimeTrace = TestTraces.TransitionTrace.END_TIME
    override val validSliceTime = TestTraces.TransitionTrace.VALID_SLICE_TIME
    override val invalidSliceTime = TestTraces.TransitionTrace.INVALID_SLICE_TIME
    override val traceType = TraceType.TRANSITION
    override val invalidSizeMessage = "Transitions trace cannot be empty"
    override val expectedSlicedTraceSize = 1

    override fun doParse(reader: ResultReader) = reader.readTransitionsTrace()
    override fun getTime(traceTime: Timestamp) = traceTime.elapsedNanos
    override fun setupWriter(writer: ResultWriter): ResultWriter {
        return super.setupWriter(writer).also {
            val trace = readAssetAsFile("transactions_trace.winscope")
            it.addTraceResult(TraceType.TRANSACTION, trace)
        }
    }
}
