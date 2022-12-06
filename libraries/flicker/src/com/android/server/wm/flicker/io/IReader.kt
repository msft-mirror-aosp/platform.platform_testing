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
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import java.io.IOException
import java.nio.file.Path

interface IReader {
    val artifactPath: Path?
    val executionError: Throwable?
    val runStatus: RunStatus
    val isFailure: Boolean
        get() = runStatus.isFailure

    /**
     * @return a [WindowManagerTrace] from the dump associated to [tag]
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class) fun readWmState(tag: String): WindowManagerTrace?

    /**
     * @return a [WindowManagerTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class) fun readWmTrace(): WindowManagerTrace?

    /**
     * @return a [LayersTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class) fun readLayersTrace(): LayersTrace?

    /**
     * @return a [LayersTrace] from the dump associated to [tag]
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class) fun readLayersDump(tag: String): LayersTrace?

    /**
     * @return a [TransactionsTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class) fun readTransactionsTrace(): TransactionsTrace?

    /**
     * @return a [TransitionsTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class) fun readTransitionsTrace(): TransitionsTrace?

    /**
     * @return a List<[FocusEvent]> for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    fun readEventLogTrace(): List<FocusEvent>?
}
