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

import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.traces.common.events.EventLog
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import java.nio.file.Path

/** Reads parsed traces from in memory objects */
class ParsedTracesReader(
    private val wmTrace: WindowManagerTrace? = null,
    private val layersTrace: LayersTrace? = null,
    private val transitionsTrace: TransitionsTrace? = null,
    private val transactionsTrace: TransactionsTrace? = null,
    private val eventLog: EventLog? = null
) : IReader {
    override val artifactPath: Path? = null
    override val runStatus: RunStatus = RunStatus.UNDEFINED
    override val executionError: Throwable? = null

    override fun readLayersTrace(): LayersTrace? = layersTrace

    override fun readTransactionsTrace(): TransactionsTrace? = transactionsTrace

    override fun readTransitionsTrace(): TransitionsTrace? = transitionsTrace

    override fun readWmTrace(): WindowManagerTrace? = wmTrace

    override fun readEventLogTrace(): EventLog? = eventLog

    override fun readLayersDump(tag: String): LayersTrace? {
        error("Trace type not available")
    }

    override fun readWmState(tag: String): WindowManagerTrace? {
        error("Trace type not available")
    }
}
