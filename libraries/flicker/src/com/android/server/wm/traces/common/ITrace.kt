/*
 * Copyright (C) 2020 The Android Open Source Project
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

import kotlin.js.JsName

interface ITrace<Entry : ITraceEntry> {
    @JsName("entries") val entries: Array<Entry>

    @JsName("slice") fun slice(startTimestamp: Timestamp, endTimestamp: Timestamp): ITrace<Entry>

    /**
     * @return an entry that matches exactly [timestamp]
     * @throws if there is no entry in the trace at [timestamp]
     */
    @JsName("getEntryExactlyAt")
    fun getEntryExactlyAt(timestamp: Timestamp): Entry {
        return entries.firstOrNull { it.timestamp == timestamp }
            ?: throw RuntimeException("Entry does not exist for timestamp $timestamp")
    }

    /**
     * @return the entry that is "active' at [timestamp]
     * ```
     *         (the entry at [timestamp] or the one before it if no entry exists at [timestamp])
     * @throws if
     * ```
     * the provided [timestamp] is before all entries in the trace
     */
    @JsName("getEntryAt")
    fun getEntryAt(timestamp: Timestamp): Entry {
        return entries.dropLastWhile { it.timestamp > timestamp }.lastOrNull()
            ?: error("No entry at or before timestamp $timestamp")
    }
}
