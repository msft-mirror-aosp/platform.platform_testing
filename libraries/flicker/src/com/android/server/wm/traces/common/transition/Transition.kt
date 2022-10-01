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
    val startTransaction: Transaction,
    @JsName("finishTransaction")
    val finishTransaction: Transaction,
    @JsName("changes")
    val changes: List<TransitionChange>,
    @JsName("aborted")
    val aborted: Boolean
) : ITraceEntry {
    override val timestamp = start

    @JsName("isIncomplete")
    val isIncomplete: Boolean get() = collectingStart == -1L || start == -1L || end == -1L ||
            aborted

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
                if (state.state == State.COLLECTING) {
                    this.collectingStart = state.timestamp
                }
                if (state.state == State.PLAYING) {
                    this.start = state.timestamp
                    this.changes = state.changes
                    this.type = state.type
                    this.startTransactionId = state.startTransactionId
                    this.finishTransactionId = state.finishTransactionId
                }
                if (state.state == State.ABORT) {
                    this.end = state.timestamp
                    this.aborted = true
                }
                if (state.state == State.FINISHED) {
                    this.end = state.timestamp
                }
            }

            @JsName("linkTransactions")
            fun linkTransactions(transactionsTrace: TransactionsTrace) {
                startTransaction = transactionsTrace.allTransactions.firstOrNull {
                    transaction -> transaction.id == startTransactionId
                }
                finishTransaction = transactionsTrace.allTransactions.firstOrNull {
                    transaction -> transaction.id == finishTransactionId
                }

                val startTransaction = startTransaction
                requireNotNull(startTransaction) {
                    "Failed to find a matching start transition on linking."
                }
                val finishTransaction = finishTransaction
                requireNotNull(finishTransaction) {
                    "Failed to find a matching finish transition on linking."
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
                requireNotNull(startTransaction) {
                    "Can't build transition without matched start transaction"
                }
                val finishTransaction = finishTransaction
                requireNotNull(finishTransaction) {
                    "Can't build transition without matched finish transaction"
                }
                return Transition(
                    type,
                    start,
                    end,
                    collectingStart,
                    startTransaction,
                    finishTransaction,
                    changes,
                    aborted
                )
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
