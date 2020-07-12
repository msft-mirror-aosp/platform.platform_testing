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

package com.android.server.wm.flicker

import com.android.server.wm.nano.WindowManagerTraceFileProto
import com.google.protobuf.nano.InvalidProtocolBufferNanoException
import java.nio.file.Path
import java.util.Optional

/**
 * Contains a collection of parsed WindowManager trace entries and assertions to apply over a single
 * entry.
 *
 * Each entry is parsed into a list of [WindowManagerTraceEntry] objects.
 */
class WindowManagerTrace private constructor(
    val entries: List<WindowManagerTraceEntry>,
    private val _source: Path?,
    val sourceChecksum: String
) {
    val source get() = Optional.ofNullable(_source)

    fun getEntry(timestamp: Long): WindowManagerTraceEntry {
        return entries.firstOrNull { it.timestamp == timestamp }
                ?: throw RuntimeException("Entry does not exist for timestamp $timestamp")
    }

    companion object {
        /**
         * Parses `WindowManagerTraceFileProto` from `data` and uses the proto to generates
         * a list of trace entries.
         *
         * @param data binary proto data
         * @param source Path to source of data for additional debug information
         */
        @JvmOverloads
        @JvmStatic
        fun parseFrom(
            data: ByteArray?,
            source: Path? = null,
            checksum: String = ""
        ): WindowManagerTrace {
            val entries = mutableListOf<WindowManagerTraceEntry>()
            val fileProto = try {
                WindowManagerTraceFileProto.parseFrom(data)
            } catch (e: InvalidProtocolBufferNanoException) {
                throw RuntimeException(e)
            }
            for (entryProto in fileProto.entry) {
                entries.add(WindowManagerTraceEntry(entryProto))
            }
            return WindowManagerTrace(entries, source, checksum)
        }
    }
}
