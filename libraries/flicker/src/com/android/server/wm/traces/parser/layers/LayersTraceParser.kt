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

import android.surfaceflinger.Layerstrace
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.layers.LayerTraceEntryBuilder
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.parser.AbstractTraceParser

/** Parser for [LayersTrace] objects containing traces or state dumps */
class LayersTraceParser(
    private val ignoreLayersStackMatchNoDisplay: Boolean = true,
    private val ignoreLayersInVirtualDisplay: Boolean = true,
    private val orphanLayerCallback: ((Layer) -> Boolean)? = null
) :
    AbstractTraceParser<
        Layerstrace.LayersTraceFileProto, Layerstrace.LayersTraceProto, LayerTraceEntry, LayersTrace
    >() {
    private var realToElapsedTimeOffsetNanos = 0L

    override val traceName: String = "Layers Trace"

    override fun doDecodeByteArray(bytes: ByteArray): Layerstrace.LayersTraceFileProto =
        Layerstrace.LayersTraceFileProto.parseFrom(bytes)

    override fun createTrace(entries: List<LayerTraceEntry>): LayersTrace =
        LayersTrace(entries.toTypedArray())

    override fun getEntries(
        input: Layerstrace.LayersTraceFileProto
    ): List<Layerstrace.LayersTraceProto> = input.entryList

    override fun getTimestamp(entry: Layerstrace.LayersTraceProto): Long =
        entry.elapsedRealtimeNanos

    override fun onBeforeParse(input: Layerstrace.LayersTraceFileProto) {
        realToElapsedTimeOffsetNanos = input.realToElapsedTimeOffsetNanos
    }

    override fun doParseEntry(entry: Layerstrace.LayersTraceProto): LayerTraceEntry {
        val layers = entry.layers.layersList.map { LayerTraceEntryLazy.newLayer(it) }.toTypedArray()
        val displays = entry.displaysList.map { LayerTraceEntryLazy.newDisplay(it) }.toTypedArray()
        val builder =
            LayerTraceEntryBuilder(
                    entry.elapsedRealtimeNanos.toString(),
                    layers,
                    displays,
                    entry.vsyncId,
                    entry.hwcBlob,
                    entry.where,
                    realToElapsedTimeOffsetNanos.toString()
                )
                .setOrphanLayerCallback(orphanLayerCallback)
                .ignoreLayersStackMatchNoDisplay(ignoreLayersStackMatchNoDisplay)
                .ignoreVirtualDisplay(ignoreLayersInVirtualDisplay)
        return builder.build()
    }
}
