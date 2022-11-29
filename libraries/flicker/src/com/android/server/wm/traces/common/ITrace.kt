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

    @JsName("getEntryByElapsedTimestamp")
    fun getEntryByElapsedTimestamp(timestamp: Long): Entry {
        return entries.firstOrNull { it.timestamp.elapsedNanos == timestamp }
            ?: throw RuntimeException("Entry does not exist for timestamp $timestamp")
    }

    @JsName("getEntryBySystemUptime")
    fun getEntryBySystemUptime(timestamp: Long): Entry {
        return entries.firstOrNull { it.timestamp.systemUptimeNanos == timestamp }
            ?: throw RuntimeException("Entry does not exist for timestamp $timestamp")
    }

    @JsName("getEntryByUnixTimestamp")
    fun getEntryByUnixTimestamp(timestamp: Long): Entry {
        return entries.firstOrNull { it.timestamp.unixNanos == timestamp }
            ?: throw RuntimeException("Entry does not exist for timestamp $timestamp")
    }
}
