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

import android.surfaceflinger.nano.Layerstrace
import java.nio.file.Path

/**
 * Contains a collection of parsed Layers trace entries and assertions to apply over a single entry.
 *
 *
 * Each entry is parsed into a list of [LayerTraceEntry] objects.
 */
class LayersTrace private constructor(
    val entries: List<LayerTraceEntry>,
    val source: Path?,
    val sourceChecksum: String
) {
    fun getEntry(timestamp: Long): LayerTraceEntry {
        return entries.first { it.timestamp == timestamp }
    }

    fun hasSource(): Boolean = source != null

    companion object {
        /**
         * Parses `LayersTraceFileProto` from `data` and uses the proto to generates a list
         * of trace entries, storing the flattened layers into its hierarchical structure.
         *
         * @param data binary proto data
         * @param source Path to source of data for additional debug information
         * @param sourceChecksum Checksum of the source file
         * @param orphanLayerCallback a callback to handle any unexpected orphan layers
         */
        /**
         * Parses `LayersTraceFileProto` from `data` and uses the proto to generates a list
         * of trace entries, storing the flattened layers into its hierarchical structure.
         *
         * @param data binary proto data
         * @param source Path to source of data for additional debug information
         */
        /**
         * Parses `LayersTraceFileProto` from `data` and uses the proto to generates a list
         * of trace entries, storing the flattened layers into its hierarchical structure.
         *
         * @param data binary proto data
         */
        /**
         * Parses `LayersTraceFileProto` from `data` and uses the proto to generates a list
         * of trace entries, storing the flattened layers into its hierarchical structure.
         *
         * @param data binary proto data
         * @param source Path to source of data for additional debug information
         */
        @JvmOverloads
        @JvmStatic
        fun parseFrom(
            data: ByteArray,
            source: Path? = null,
            sourceChecksum: String = "",
            orphanLayerCallback: ((Layer) -> Boolean)? = null
        ): LayersTrace {
            val entries: MutableList<LayerTraceEntry> = ArrayList()
            val fileProto: Layerstrace.LayersTraceFileProto
            try {
                fileProto = Layerstrace.LayersTraceFileProto.parseFrom(data)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            for (traceProto: Layerstrace.LayersTraceProto in fileProto.entry) {
                val entry: LayerTraceEntry = LayerTraceEntry.fromFlattenedLayers(
                        traceProto.elapsedRealtimeNanos, traceProto.layers.layers,
                        orphanLayerCallback)
                entries.add(entry)
            }
            return LayersTrace(entries, source, sourceChecksum)
        }
    }
}