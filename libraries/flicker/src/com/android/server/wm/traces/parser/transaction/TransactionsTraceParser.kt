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

package com.android.server.wm.traces.parser.transaction

import android.surfaceflinger.proto.Transactions.TransactionState
import android.surfaceflinger.proto.Transactions.TransactionTraceFile
import android.util.Log
import com.android.server.wm.traces.common.transactions.Transaction
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transactions.TransactionsTraceEntry
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.parser.LOG_TAG
import kotlin.math.max
import kotlin.system.measureTimeMillis

/**
 * Parser for [TransitionsTrace] objects
 **/
class TransactionsTraceParser {
    companion object {
        /**
         * Parses [TransitionsTrace] from [data] and uses the proto to generates a list
         * of trace entries.
         *
         * @param data binary proto data
         */
        @JvmStatic
        fun parseFromTrace(data: ByteArray): TransactionsTrace {
            var fileProto: TransactionTraceFile?
            try {
                measureTimeMillis {
                    fileProto = TransactionTraceFile.parseFrom(data)
                }.also {
                    Log.v(LOG_TAG, "Parsing proto (Transactions Trace): ${it}ms")
                }
            } catch (e: Throwable) {
                throw RuntimeException(e)
            }
            return fileProto?.let {
                parseFromTrace(it)
            } ?: error("Unable to read trace file")
        }

        /**
         * Uses the proto to generates a list of trace entries.
         *
         * @param proto Parsed proto data
         */
        @JvmStatic
        fun parseFromTrace(proto: TransactionTraceFile): TransactionsTrace {
            val transactionsTraceEntries = mutableListOf<TransactionsTraceEntry>()
            var traceParseTime = 0L
            for (entry in proto.entryList) {
                val entryParseTime = measureTimeMillis {
                    val transactions = parseTransactionsProto(entry.transactionsList)
                    val transactionsTraceEntry = TransactionsTraceEntry(
                        entry.elapsedRealtimeNanos,
                        entry.vsyncId,
                        transactions
                    )
                    transactions.forEach { it.appliedInEntry = transactionsTraceEntry }
                    transactionsTraceEntries.add(transactionsTraceEntry)
                }
                traceParseTime += entryParseTime
            }

            Log.v(
                LOG_TAG,
                "Parsing duration (Transactions Trace): ${traceParseTime}ms " +
                    "(avg ${traceParseTime / max(transactionsTraceEntries.size, 1)}ms per entry)"
            )
            return TransactionsTrace(transactionsTraceEntries.toTypedArray())
        }

        private fun parseTransactionsProto(
            transactionStates: List<TransactionState>
        ): Array<Transaction> {
            val transactions = mutableListOf<Transaction>()
            for (state in transactionStates) {
                val transaction = Transaction(
                    state.pid,
                    state.uid,
                    state.vsyncId,
                    state.postTime,
                    state.transactionId
                )
                transactions.add(transaction)
            }

            return transactions.toTypedArray()
        }
    }
}
