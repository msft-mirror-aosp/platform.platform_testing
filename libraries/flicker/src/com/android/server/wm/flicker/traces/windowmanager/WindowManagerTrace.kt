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

package com.android.server.wm.flicker.traces.windowmanager

import com.android.server.wm.nano.WindowManagerServiceDumpProto
import com.android.server.wm.nano.WindowManagerTraceFileProto
import com.google.protobuf.nano.InvalidProtocolBufferNanoException
import java.nio.file.Path

/**
 * Contains a collection of parsed WindowManager trace entries and assertions to apply over a single
 * entry.
 *
 * Each entry is parsed into a list of [WindowManagerTraceEntry] objects.
 */
class WindowManagerTrace private constructor(
    entries: List<WindowManagerTraceEntry>,
    source: Path?,
    sourceChecksum: String
) : com.android.server.wm.flicker.common.traces.windowmanager
        .WindowManagerTrace<WindowManagerTraceEntry>(
    entries, source?.toString() ?: "",
    sourceChecksum
) {
    companion object {
        /**
         * Parses [WindowManagerTraceFileProto] from [data] and uses the proto to generates
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

        /**
         * Parses [WindowManagerServiceDumpProto] from [data] and uses the proto to generates
         * a list of trace entries.
         *
         * @param data binary proto data
         */
        @JvmStatic
        fun parseFromDump(data: ByteArray?): WindowManagerTrace {
            val fileProto = try {
                WindowManagerServiceDumpProto.parseFrom(data)
            } catch (e: InvalidProtocolBufferNanoException) {
                throw RuntimeException(e)
            }
            return WindowManagerTrace(
                listOf(WindowManagerTraceEntry(fileProto.rootWindowContainer, 0)),
                source = null,
                sourceChecksum = "")
        }
    }
}
