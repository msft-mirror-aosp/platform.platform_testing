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

package com.android.server.wm.traces.parser.transition

import android.util.Log
import com.android.server.wm.shell.nano.ChangeInfo
import com.android.server.wm.shell.nano.TransitionTraceProto
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transition.Transition
import com.android.server.wm.traces.common.transition.Transition.Companion.Type
import com.android.server.wm.traces.common.transition.TransitionChange
import com.android.server.wm.traces.common.transition.TransitionState
import com.android.server.wm.traces.common.transition.TransitionState.Companion.State
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.parser.LOG_TAG
import kotlin.math.max
import kotlin.system.measureTimeMillis

/** Parser for [TransitionsTrace] objects */
class TransitionsTraceParser {
    companion object {
        /**
         * Parses [TransitionsTrace] from [data] and uses the proto to generates a list of trace
         * entries.
         *
         * @param data binary proto data
         */
        @JvmStatic
        fun parseFromTrace(data: ByteArray, transactions: TransactionsTrace): TransitionsTrace {
            var fileProto: TransitionTraceProto?
            try {
                measureTimeMillis { fileProto = TransitionTraceProto.parseFrom(data) }
                    .also { Log.v(LOG_TAG, "Parsing proto (Transition Trace): ${it}ms") }
            } catch (e: Throwable) {
                throw RuntimeException(e)
            }
            return fileProto?.let { parseFromTrace(it, transactions) }
                ?: error("Unable to read trace file")
        }

        /**
         * Uses the proto to generates a list of trace entries.
         *
         * @param proto Parsed proto data
         */
        @JvmStatic
        fun parseFromTrace(
            proto: TransitionTraceProto,
            transactions: TransactionsTrace
        ): TransitionsTrace {
            val transitionStates = mutableListOf<TransitionState>()
            var traceParseTime = 0L
            for (transitionProto in proto.transition) {
                if (transitionProto.id == -1) {
                    // Invalid transition state
                    Log.w(LOG_TAG, "Got transition state with invalid id :: $transitionProto")
                    continue
                }
                val entryParseTime = measureTimeMillis {
                    val changes = transitionProto.change.map { parseTransitionChangeFromProto(it) }
                    val state =
                        TransitionState(
                            transitionProto.id,
                            Type.fromInt(transitionProto.transitionType),
                            transitionProto.timestamp,
                            State.fromInt(transitionProto.state),
                            transitionProto.flags,
                            changes,
                            transitionProto.startTransactionId,
                            transitionProto.finishTransactionId
                        )
                    transitionStates.add(state)
                }
                traceParseTime += entryParseTime
            }

            val transitions = transitionStatesToTransitions(transitionStates, transactions)

            Log.v(
                LOG_TAG,
                "Parsing duration (Transition Trace): ${traceParseTime}ms " +
                    "(avg ${traceParseTime / max(transitions.size, 1)}ms per entry)"
            )
            return TransitionsTrace(transitions.toTypedArray())
        }

        private fun transitionStatesToTransitions(
            transitionStates: List<TransitionState>,
            transactions: TransactionsTrace
        ): List<Transition> {
            val transitionBuilders: MutableMap<Int, Transition.Companion.Builder> = mutableMapOf()

            for (state in transitionStates) {
                if (!transitionBuilders.containsKey(state.id)) {
                    transitionBuilders[state.id] = Transition.Companion.Builder(state.id)
                }
                val builder = transitionBuilders[state.id]!!
                builder.addState(state)
            }

            transitionBuilders.values.forEach { it.linkTransactions(transactions) }
            val transitions = transitionBuilders.values.map { it.build() }
            return transitions.sortedBy { it.start }
        }

        private fun parseTransitionChangeFromProto(proto: ChangeInfo): TransitionChange {
            val windowName = proto.windowIdentifier.title
            val windowId = proto.windowIdentifier.hashCode.toString(16)

            return TransitionChange(windowName, Type.fromInt(proto.transitMode))
        }
    }
}
