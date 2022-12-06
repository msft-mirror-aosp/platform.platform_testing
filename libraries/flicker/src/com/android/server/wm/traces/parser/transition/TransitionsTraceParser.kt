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
import com.android.server.wm.traces.parser.AbstractTraceParser
import com.android.server.wm.traces.parser.LOG_TAG

/** Parser for [TransitionsTrace] objects */
class TransitionsTraceParser(private val transactions: TransactionsTrace) :
    AbstractTraceParser<
        TransitionTraceProto,
        com.android.server.wm.shell.nano.Transition,
        TransitionState,
        TransitionsTrace
    >() {
    override val traceName: String = "Transition trace"

    override fun createTrace(entries: List<TransitionState>): TransitionsTrace {
        val transitions = transitionStatesToTransitions(entries, transactions)
        return TransitionsTrace(transitions.toTypedArray())
    }

    override fun doDecodeByteArray(bytes: ByteArray): TransitionTraceProto =
        TransitionTraceProto.parseFrom(bytes)

    override fun shouldParseEntry(entry: com.android.server.wm.shell.nano.Transition): Boolean {
        return (entry.id != -1).also {
            // Invalid transition state
            Log.w(LOG_TAG, "Got transition state with invalid id :: $entry")
        }
    }

    override fun getEntries(
        input: TransitionTraceProto
    ): List<com.android.server.wm.shell.nano.Transition> = input.transition.toList()

    override fun getTimestamp(entry: com.android.server.wm.shell.nano.Transition): Long =
        entry.timestamp

    override fun onBeforeParse(input: TransitionTraceProto) {}

    override fun doParseEntry(entry: com.android.server.wm.shell.nano.Transition): TransitionState {
        val changes = entry.change.map { parseTransitionChangeFromProto(it) }
        return TransitionState(
            entry.id,
            Type.fromInt(entry.transitionType),
            entry.timestamp,
            State.fromInt(entry.state),
            entry.flags,
            changes,
            entry.startTransactionId,
            entry.finishTransactionId
        )
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
