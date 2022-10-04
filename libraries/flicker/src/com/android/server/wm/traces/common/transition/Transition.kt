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

package com.android.server.wm.traces.common.transition

import com.android.server.wm.traces.common.ITraceEntry
import com.android.server.wm.traces.common.transactions.Transaction
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transition.TransitionState.Companion.State
import kotlin.js.JsName

class Transition(
    @JsName("type")
    val type: Type,
    @JsName("start")
    val start: Long,
    @JsName("end")
    val end: Long,
    @JsName("collectingStart")
    val collectingStart: Long,
    @JsName("startTransaction")
    val startTransaction: Transaction?,
    @JsName("finishTransaction")
    val finishTransaction: Transaction?,
    @JsName("changes")
    val changes: List<TransitionChange>,
    @JsName("played")
    val played: Boolean,
    @JsName("aborted")
    val aborted: Boolean
) : ITraceEntry {
    override val timestamp = start

    @JsName("isIncomplete")
    val isIncomplete: Boolean get() = !played || aborted

    override fun toString(): String =
        "Transition#${hashCode()}" +
                "($type, aborted=$aborted, start=$start, end=$end," +
                "startTransaction=$startTransaction, finishTransaction=$finishTransaction, " +
                "changes=[${changes.joinToString()}])"

    companion object {
        enum class Type(val value: Int) {
            UNDEFINED(-1),
            NONE(0),
            OPEN(1),
            CLOSE(2),
            TO_FRONT(3),
            TO_BACK(4),
            RELAUNCH(5),
            CHANGE(6),
            KEYGUARD_GOING_AWAY(7),
            KEYGUARD_OCCLUDE(8),
            KEYGUARD_UNOCCLUDE(9),
            PIP(10),
            WAKE(11),
            // START OF CUSTOM TYPES
            FIRST_CUSTOM(12); // TODO: Add custom types we know about

            companion object {
                fun fromInt(value: Int) = values().first { it.value == value }
            }
        }

        class Builder(val id: Int) {
            @JsName("type")
            var type: Type = Type.UNDEFINED
            @JsName("start")
            var start: Long = -1
            @JsName("end")
            var end: Long = -1
            @JsName("collectingStart")
            var collectingStart: Long = -1
            @JsName("changes")
            var changes: List<TransitionChange> = emptyList()
            @JsName("played")
            var played = false
            @JsName("aborted")
            var aborted = false
            @JsName("startTransactionId")
            var startTransactionId = -1L
            @JsName("finishTransactionId")
            var finishTransactionId = -1L
            @JsName("startTransaction")
            var startTransaction: Transaction? = null
            @JsName("finishTransaction")
            var finishTransaction: Transaction? = null

            // Assumes each state is reported once
            @JsName("addState")
            fun addState(state: TransitionState) {
                when (state.state) {
                    State.PENDING -> {
                        // Nothing to do
                    }
                    State.COLLECTING -> {
                        this.collectingStart = state.timestamp
                    }
                    State.STARTED -> {
                        // Nothing to do
                    }
                    State.PLAYING -> {
                        this.start = state.timestamp
                        this.changes = state.changes
                        this.type = state.type
                        this.startTransactionId = state.startTransactionId
                        this.finishTransactionId = state.finishTransactionId
                        this.played = true
                    }
                    State.ABORT -> {
                        this.end = state.timestamp
                        this.aborted = true
                    }
                    State.FINISHED -> {
                        this.end = state.timestamp
                    }
                }
            }

            @JsName("linkTransactions")
            fun linkTransactions(transactionsTrace: TransactionsTrace) {
                if (this.startTransactionId == -1L || this.finishTransactionId == -1L) {
                    // Transition not played
                    require(this.aborted) {
                        "$this has no start or finish transaction but is not aborted"
                    }
                    return
                }

                startTransaction = transactionsTrace.allTransactions.firstOrNull {
                    transaction -> transaction.id == startTransactionId
                }
                finishTransaction = transactionsTrace.allTransactions.firstOrNull {
                    transaction -> transaction.id == finishTransactionId
                }

                val startTransaction = startTransaction
                requireNotNull(startTransaction) {
                    "Failed to find a matching start transaction for $this on linking. " +
                        "Transaction with id $startTransactionId not found in transactions trace."
                }
                val finishTransaction = finishTransaction
                requireNotNull(finishTransaction) {
                    "Failed to find a matching finish transaction for $this on linking. " +
                        "Transaction with id $finishTransactionId not found in transactions trace."
                }

                require(startTransaction.appliedVSyncId != -1L ) {
                    "Matched start transaction had a vSyncId of -1..."
                }
                require(finishTransaction.appliedVSyncId != -1L ) {
                    "Matched start transaction had a vSyncId of -1..."
                }
            }

            @JsName("build")
            fun build(): Transition {
                val startTransaction = startTransaction
                require(startTransaction != null || !played) {
                    "Can't build played transition without matched start transaction"
                }
                val finishTransaction = finishTransaction
                require(finishTransaction != null || aborted) {
                    "Can't build non-aborted transition without matched finish transaction"
                }
                return Transition(
                    type,
                    start,
                    end,
                    collectingStart,
                    startTransaction,
                    finishTransaction,
                    changes,
                    played,
                    aborted
                )
            }

            override fun toString(): String {
                return buildString {
                    appendLine("Transition.Builder(")
                    appendLine("  id=$id")
                    appendLine("  type=$type")
                    appendLine("  start=$start")
                    appendLine("  end=$end")
                    appendLine("  collectingStart=$collectingStart")
                    appendLine("  changes=$changes")
                    appendLine("  aborted=$aborted")
                    appendLine("  startTransactionId=$startTransactionId")
                    appendLine("  finishTransactionId=$finishTransactionId")
                    appendLine("  startTransaction=$startTransaction")
                    appendLine("  finishTransaction=$finishTransaction")
                    append(")")
                }
            }
        }

        @JsName("emptyTransition")
        fun emptyTransition(stateId: Int = 0): Transition {
            val transitionBuilder = Builder(stateId)
            transitionBuilder.startTransaction = Transaction.emptyTransaction()
            transitionBuilder.finishTransaction = Transaction.emptyTransaction()
            return transitionBuilder.build()
        }
    }
}
