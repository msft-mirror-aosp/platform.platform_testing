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

import android.tools.common.Timestamp
import android.tools.common.io.IReader
import android.tools.common.io.RunStatus
import android.tools.common.io.TraceType
import android.tools.common.traces.events.CujTrace
import android.tools.common.traces.events.EventLog
import android.tools.common.traces.surfaceflinger.LayersTrace
import android.tools.common.traces.surfaceflinger.TransactionsTrace
import android.tools.common.traces.wm.TransitionsTrace
import android.tools.common.traces.wm.WindowManagerTrace

/** Reads parsed traces from in memory objects */
class ParsedTracesReader(
    private val wmTrace: WindowManagerTrace? = null,
    private val layersTrace: LayersTrace? = null,
    private val transitionsTrace: TransitionsTrace? = null,
    private val transactionsTrace: TransactionsTrace? = null,
    private val eventLog: EventLog? = null
) : IReader {
    override val artifactPath = ""
    override val runStatus = RunStatus.UNDEFINED
    override val executionError = null

    override fun readLayersTrace(): LayersTrace? = layersTrace

    override fun readTransactionsTrace(): TransactionsTrace? = transactionsTrace

    override fun readTransitionsTrace(): TransitionsTrace? = transitionsTrace

    override fun readWmTrace(): WindowManagerTrace? = wmTrace

    override fun readEventLogTrace(): EventLog? = eventLog

    override fun readCujTrace(): CujTrace? = eventLog?.cujTrace

    override fun slice(startTimestamp: Timestamp, endTimestamp: Timestamp): ParsedTracesReader {
        return ParsedTracesReader(
            wmTrace?.slice(startTimestamp, endTimestamp),
            layersTrace?.slice(startTimestamp, endTimestamp),
            transitionsTrace?.slice(startTimestamp, endTimestamp),
            transactionsTrace?.slice(startTimestamp, endTimestamp),
            eventLog?.slice(startTimestamp, endTimestamp)
        )
    }

    override fun readLayersDump(tag: String): LayersTrace? {
        error("Trace type not available")
    }

    override fun readWmState(tag: String): WindowManagerTrace? {
        error("Trace type not available")
    }

    override fun readBytes(traceType: TraceType, tag: String): ByteArray? {
        error("Feature not available")
    }
}
