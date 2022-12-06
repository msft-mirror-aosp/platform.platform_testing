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

package com.android.server.wm.traces.parser

import com.android.server.wm.traces.common.Cache
import com.android.server.wm.traces.common.Utils

/** Base trace parser class */
abstract class AbstractTraceParser<
    InputTypeTrace, InputTypeEntry, OutputTypeEntry, OutputTypeTrace> :
    AbstractParser<InputTypeTrace, OutputTypeTrace>() {
    protected abstract fun onBeforeParse(input: InputTypeTrace)
    protected abstract fun getEntries(input: InputTypeTrace): List<InputTypeEntry>
    protected abstract fun getTimestamp(entry: InputTypeEntry): Long
    protected abstract fun doParseEntry(entry: InputTypeEntry): OutputTypeEntry
    protected abstract fun createTrace(entries: List<OutputTypeEntry>): OutputTypeTrace

    open fun shouldParseEntry(entry: InputTypeEntry) = true

    override fun parse(bytes: ByteArray, clearCache: Boolean): OutputTypeTrace {
        return parse(
            bytes,
            from = Long.MIN_VALUE,
            to = Long.MAX_VALUE,
            addInitialEntry = true,
            clearCache = clearCache
        )
    }

    override fun parse(input: InputTypeTrace, clearCache: Boolean): OutputTypeTrace {
        return parse(
            input,
            from = Long.MIN_VALUE,
            to = Long.MAX_VALUE,
            addInitialEntry = true,
            clearCache = clearCache
        )
    }

    override fun doParse(input: InputTypeTrace): OutputTypeTrace {
        return doParse(input, from = Long.MIN_VALUE, to = Long.MAX_VALUE, addInitialEntry = true)
    }

    /**
     * Uses [InputTypeTrace] to generates a trace
     *
     * @param input Parsed proto data
     * @param from Initial timestamp to be parsed
     * @param to Final timestamp to be parsed
     * @param addInitialEntry If the last entry smaller than [from] should be included as well
     */
    private fun doParse(
        input: InputTypeTrace,
        from: Long,
        to: Long,
        addInitialEntry: Boolean
    ): OutputTypeTrace {
        val parsedEntries = mutableListOf<OutputTypeEntry>()
        val rawEntries = getEntries(input)
        val allInputTimestamps = rawEntries.map { getTimestamp(it) }
        val selectedInputTimestamps =
            Utils.getTimestampsInRange(allInputTimestamps, from, to, addInitialEntry)
        onBeforeParse(input)
        for (rawEntry in rawEntries) {
            val currTimestamp = getTimestamp(rawEntry)
            if (!selectedInputTimestamps.contains(currTimestamp) || !shouldParseEntry(rawEntry)) {
                continue
            }
            val parsedEntry =
                logTime("Creating entry for time $currTimestamp") { doParseEntry(rawEntry) }
            parsedEntries.add(parsedEntry)
        }
        return createTrace(parsedEntries)
    }

    /**
     * Uses [InputTypeTrace] to generates a trace
     *
     * @param input Parsed proto data
     * @param from Initial timestamp to be parsed
     * @param to Final timestamp to be parsed
     * @param addInitialEntry If the last entry smaller than [from] should be included as well
     * @param clearCache If the caching used while parsing the object should be cleared
     */
    fun parse(
        input: InputTypeTrace,
        from: Long,
        to: Long,
        addInitialEntry: Boolean = true,
        clearCache: Boolean = true
    ): OutputTypeTrace {
        return try {
            logTime("Parsing objects") { doParse(input, from, to, addInitialEntry) }
        } finally {
            if (clearCache) {
                Cache.clear()
            }
        }
    }

    /**
     * Uses a [ByteArray] to generates a trace
     *
     * @param bytes Parsed proto data
     * @param from Initial timestamp to be parsed
     * @param to Final timestamp to be parsed
     * @param addInitialEntry If the last entry smaller than [from] should be included as well
     * @param clearCache If the caching used while parsing the object should be cleared
     */
    fun parse(
        bytes: ByteArray,
        from: Long,
        to: Long,
        addInitialEntry: Boolean = true,
        clearCache: Boolean = true
    ): OutputTypeTrace {
        val input = decodeByteArray(bytes)
        return parse(input, from, to, addInitialEntry, clearCache)
    }
}
