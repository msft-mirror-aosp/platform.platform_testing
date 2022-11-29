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

package com.android.server.wm.traces.common.transactions

import com.android.server.wm.traces.common.ITrace
import com.android.server.wm.traces.common.Utils
import kotlin.js.JsName

class TransactionsTrace(override val entries: Array<TransactionsTraceEntry>) :
    ITrace<TransactionsTraceEntry>, List<TransactionsTraceEntry> by entries.toList() {

    init {
        val alwaysIncreasing =
            entries
                .toList()
                .zipWithNext { prev, next ->
                    (prev.timestamp.elapsedNanos
                        ?: error("missing elapsedNanos")) <
                        (next.timestamp.elapsedNanos ?: error("missing elapsedNanos"))
                }
                .all { it }
        require(alwaysIncreasing) { "Transaction timestamp not always increasing..." }
    }

    @JsName("allTransactions")
    val allTransactions: List<Transaction> = entries.toList().flatMap { it.transactions.toList() }

    @JsName("sliceUsingElapsedTimestamp")
    fun slice(from: Long, to: Long): TransactionsTrace {
        return TransactionsTrace(
            Utils.sliceEntriesByTimestamp(this.entries, from, to) {
                it.timestamp.elapsedNanos ?: error("Missing elapsed timestamp")
            }
        )
    }
}
