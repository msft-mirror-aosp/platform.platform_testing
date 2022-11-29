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

package com.android.server.wm.traces.common

class Utils {
    companion object {
        inline fun <reified Entry : ITraceEntry> sliceEntriesByTimestamp(
            entries: Array<Entry>,
            from: Long,
            to: Long,
            addInitialEntry: Boolean = false,
            timestampGetter: ((entry: Entry) -> Long),
        ): Array<Entry> {
            require(from <= to) { "`from` must be smaller or equal to `to` but was $from and $to" }

            return when {
                entries.isEmpty() -> {
                    emptyArray()
                }
                to < timestampGetter.invoke(entries.first()) -> {
                    // Slice before all entries
                    emptyArray()
                }
                timestampGetter.invoke(entries.last()) < from -> {
                    // Slice after all entries
                    if (addInitialEntry) {
                        // Keep the last entry as the start entry of the sliced trace
                        arrayOf(entries.last())
                    } else {
                        emptyArray()
                    }
                }
                else -> {
                    // first entry <= to
                    // last entry >= from
                    // -----|--------|------
                    //      [   to     to
                    //  from    from ]

                    var first = entries.indexOfFirst { timestampGetter.invoke(it) >= from }
                    require(first >= 0) { "No match found for first index" }
                    val last =
                        entries.lastIndex -
                            entries.reversed().indexOfFirst { timestampGetter.invoke(it) <= to }
                    require(last >= 0) { "No match found for last index" }

                    if (
                        addInitialEntry &&
                            first > 0 &&
                            timestampGetter.invoke(entries[first]) > from
                    ) {
                        // Include previous state since from timestamp is in between the previous
                        // one and first, and the previous state is the state we were still at a
                        // timestamp from.
                        first--
                    }

                    entries.slice(first..last).toTypedArray()
                }
            }
        }
    }
}
