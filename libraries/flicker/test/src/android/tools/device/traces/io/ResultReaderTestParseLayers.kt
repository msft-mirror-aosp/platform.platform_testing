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

package android.tools.device.traces.io

import android.tools.TestTraces
import android.tools.common.Timestamp
import android.tools.common.io.TraceType

/** Tests for [ResultReader] parsing [TraceType.SF] */
class ResultReaderTestParseLayers : BaseResultReaderTestParseTrace() {
    override val assetFile = TestTraces.LayerTrace.FILE
    override val traceName = "Layers trace"
    override val startTimeTrace = TestTraces.LayerTrace.START_TIME
    override val endTimeTrace = TestTraces.LayerTrace.END_TIME
    override val validSliceTime = TestTraces.LayerTrace.SLICE_TIME
    override val invalidSliceTime = startTimeTrace
    override val traceType = TraceType.SF
    override val expectedSlicedTraceSize = 2

    override fun doParse(reader: ResultReader) = reader.readLayersTrace()
    override fun getTime(traceTime: Timestamp) = traceTime.systemUptimeNanos
}
