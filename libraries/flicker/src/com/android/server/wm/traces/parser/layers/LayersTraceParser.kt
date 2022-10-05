/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.traces.parser.layers

import android.surfaceflinger.Layers
import android.surfaceflinger.Layerstrace
import android.util.Log
import com.android.server.wm.traces.common.Cache
import com.android.server.wm.traces.common.layers.BaseLayerTraceEntry
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.LayerTraceEntryBuilder
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.parser.LOG_TAG
import com.google.protobuf.InvalidProtocolBufferException
import kotlin.math.max
import kotlin.system.measureTimeMillis

/** Parser for [LayersTrace] objects containing traces or state dumps */
class LayersTraceParser {
    companion object {
        /**
         * Parses [LayersTrace] from [data] and uses the proto to generates a list of trace entries,
         * storing the flattened layers into its hierarchical structure.
         *
         * @param data binary proto data
         * @param orphanLayerCallback a callback to handle any unexpected orphan layers
         * @param clearCacheAfterParsing If the caching used while parsing the proto should be
         * ```
         *                               cleared or remain in memory
         * ```
         */
        @JvmOverloads
        @JvmStatic
        fun parseFromTrace(
            data: ByteArray,
            ignoreLayersStackMatchNoDisplay: Boolean = true,
            ignoreLayersInVirtualDisplay: Boolean = true,
            clearCacheAfterParsing: Boolean = true,
            orphanLayerCallback: ((Layer) -> Boolean)? = null
        ): LayersTrace {
            var fileProto: Layerstrace.LayersTraceFileProto?
            try {
                measureTimeMillis { fileProto = Layerstrace.LayersTraceFileProto.parseFrom(data) }
                    .also { Log.v(LOG_TAG, "Parsing proto (Layers Trace): ${it}ms") }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            return fileProto?.let {
                parseFromTrace(
                    it,
                    ignoreLayersStackMatchNoDisplay,
                    ignoreLayersInVirtualDisplay,
                    clearCacheAfterParsing,
                    orphanLayerCallback
                )
            }
                ?: error("Unable to read trace file")
        }

        /**
         * Parses [LayersTrace] from [proto] and uses the proto to generates a list of trace
         * entries, storing the flattened layers into its hierarchical structure.
         *
         * @param proto Parsed proto data
         * @param orphanLayerCallback a callback to handle any unexpected orphan layers
         * @param clearCacheAfterParsing If the caching used while parsing the proto should be
         * ```
         *                               cleared or remain in memory
         * ```
         */
        @JvmOverloads
        @JvmStatic
        fun parseFromTrace(
            proto: Layerstrace.LayersTraceFileProto,
            ignoreLayersStackMatchNoDisplay: Boolean = true,
            ignoreLayersInVirtualDisplay: Boolean = true,
            clearCacheAfterParsing: Boolean = true,
            orphanLayerCallback: ((Layer) -> Boolean)? = null
        ): LayersTrace {
            try {
                val entries: MutableList<BaseLayerTraceEntry> = ArrayList()
                var traceParseTime = 0L
                for (traceProto: Layerstrace.LayersTraceProto in proto.entryList) {
                    val entryParseTime = measureTimeMillis {
                        val layers =
                            traceProto.layers.layersList
                                .map { LayerTraceEntryLazy.newLayer(it) }
                                .toTypedArray()
                        val displays =
                            traceProto.displaysList
                                .map { LayerTraceEntryLazy.newDisplay(it) }
                                .toTypedArray()
                        val builder =
                            LayerTraceEntryBuilder(
                                    traceProto.elapsedRealtimeNanos,
                                    layers,
                                    displays,
                                    traceProto.vsyncId,
                                    traceProto.hwcBlob,
                                    traceProto.where
                                )
                                .setOrphanLayerCallback(orphanLayerCallback)
                                .ignoreLayersStackMatchNoDisplay(ignoreLayersStackMatchNoDisplay)
                                .ignoreVirtualDisplay(ignoreLayersInVirtualDisplay)
                        val entry = builder.build()
                        entries.add(entry)
                    }
                    traceParseTime += entryParseTime
                }
                Log.v(
                    LOG_TAG,
                    "Parsing duration (Layers Trace): ${traceParseTime}ms " +
                        "(avg ${traceParseTime / max(entries.size, 1)}ms per entry)"
                )
                return LayersTrace(entries.toTypedArray())
            } finally {
                if (clearCacheAfterParsing) {
                    Cache.clear()
                }
            }
        }

        /**
         * Parses [LayersTrace] from [proto] and uses the proto to generates a list of trace
         * entries.
         *
         * @param proto Parsed proto data
         * @param clearCacheAfterParsing If the caching used while parsing the proto should be
         * ```
         *                               cleared or remain in memory
         * ```
         */
        @JvmStatic
        @JvmOverloads
        @Deprecated(
            "This functions parsers old SF dumps. Now SF dumps create a " +
                "single entry trace, for new dump use [parseFromTrace]"
        )
        fun parseFromLegacyDump(
            proto: Layers.LayersProto,
            clearCacheAfterParsing: Boolean = true
        ): LayersTrace {
            try {
                val entry =
                    LayerTraceEntryLazy(
                        timestamp = 0,
                        displayProtos = emptyArray(),
                        layerProtos = proto.layersList.toTypedArray(),
                        ignoreLayersStackMatchNoDisplay = false,
                        ignoreLayersInVirtualDisplay = false,
                        vSyncId = -1L,
                    )
                return LayersTrace(entry)
            } finally {
                if (clearCacheAfterParsing) {
                    Cache.clear()
                }
            }
        }

        /**
         * Parses [LayersTrace] from [data] and uses the proto to generates a list of trace entries.
         *
         * @param data binary proto data
         * @param clearCacheAfterParsing If the caching used while parsing the proto should be
         * ```
         *                               cleared or remain in memory
         * ```
         */
        @JvmStatic
        @JvmOverloads
        @Deprecated(
            "This functions parsers old SF dumps. Now SF dumps create a " +
                "single entry trace, for new dump use [parseFromTrace]"
        )
        fun parseFromLegacyDump(
            data: ByteArray?,
            clearCacheAfterParsing: Boolean = true
        ): LayersTrace {
            val traceProto =
                try {
                    Layers.LayersProto.parseFrom(data)
                } catch (e: InvalidProtocolBufferException) {
                    throw RuntimeException(e)
                }
            return parseFromLegacyDump(traceProto, clearCacheAfterParsing)
        }
    }
}
