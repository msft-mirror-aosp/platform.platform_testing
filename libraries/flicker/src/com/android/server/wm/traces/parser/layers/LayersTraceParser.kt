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

import android.graphics.Rect
import android.surfaceflinger.nano.Layers
import android.surfaceflinger.nano.Layers.RectProto
import android.surfaceflinger.nano.Layers.RegionProto
import android.surfaceflinger.nano.Layerstrace
import android.util.Log
import com.android.server.wm.traces.common.Buffer
import com.android.server.wm.traces.common.Color
import com.android.server.wm.traces.common.RectF
import com.android.server.wm.traces.common.Region
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.parser.LOG_TAG
import com.google.protobuf.nano.InvalidProtocolBufferNanoException
import java.nio.file.Path
import kotlin.system.measureTimeMillis

/**
 * Parser for [LayersTrace] objects containing traces or state dumps
 **/
class LayersTraceParser {
    companion object {
        /**
         * Parses [LayersTrace] from [data] and uses the proto to generates a list
         * of trace entries, storing the flattened layers into its hierarchical structure.
         *
         * @param data binary proto data
         * @param source Path to source of data for additional debug information
         * @param sourceChecksum Checksum of the source file
         * @param orphanLayerCallback a callback to handle any unexpected orphan layers
         */
        @JvmOverloads
        @JvmStatic
        fun parseFromTrace(
            data: ByteArray,
            source: Path? = null,
            sourceChecksum: String = "",
            orphanLayerCallback: ((Layer) -> Boolean)? = null
        ): LayersTrace {
            val fileProto: Layerstrace.LayersTraceFileProto
            try {
                measureTimeMillis {
                    fileProto = Layerstrace.LayersTraceFileProto.parseFrom(data)
                }.also {
                    Log.v(LOG_TAG, "Parsing proto (Layers Trace): ${it}ms")
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            return parseFromTrace(fileProto, source, sourceChecksum, orphanLayerCallback)
        }

        /**
         * Parses [LayersTrace] from [proto] and uses the proto to generates a list
         * of trace entries, storing the flattened layers into its hierarchical structure.
         *
         * @param proto Parsed proto data
         * @param source Path to source of data for additional debug information
         * @param sourceChecksum Checksum of the source file
         * @param orphanLayerCallback a callback to handle any unexpected orphan layers
         */
        @JvmOverloads
        @JvmStatic
        fun parseFromTrace(
            proto: Layerstrace.LayersTraceFileProto,
            source: Path? = null,
            sourceChecksum: String = "",
            orphanLayerCallback: ((Layer) -> Boolean)? = null
        ): LayersTrace {
            val entries: MutableList<LayerTraceEntry> = ArrayList()
            var traceParseTime = 0L
            for (traceProto: Layerstrace.LayersTraceProto in proto.entry) {
                val entryParseTime = measureTimeMillis {
                    val entry = newEntry(
                        traceProto.elapsedRealtimeNanos, traceProto.layers.layers,
                        orphanLayerCallback)
                    entries.add(entry)
                }
                traceParseTime += entryParseTime
            }
            Log.v(LOG_TAG, "Parsing duration (Layers Trace): ${traceParseTime}ms " +
                "(avg ${traceParseTime / entries.size}ms per entry)")
            return LayersTrace(entries, source?.toString() ?: "", sourceChecksum)
        }

        /**
         * Parses [LayersTrace] from [proto] and uses the proto to generates
         * a list of trace entries.
         *
         * @param proto Parsed proto data
         */
        @JvmStatic
        fun parseFromDump(proto: Layers.LayersProto): LayersTrace {
            val entry = newEntry(timestamp = 0, protos = proto.layers)
            return LayersTrace(entry)
        }

        /**
         * Parses [LayersTrace] from [data] and uses the proto to generates
         * a list of trace entries.
         *
         * @param data binary proto data
         */
        @JvmStatic
        fun parseFromDump(data: ByteArray?): LayersTrace {
            val traceProto = try {
                Layers.LayersProto.parseFrom(data)
            } catch (e: InvalidProtocolBufferNanoException) {
                throw RuntimeException(e)
            }
            return parseFromDump(traceProto)
        }

        @JvmStatic
        private fun newEntry(
            timestamp: Long,
            protos: Array<Layers.LayerProto>,
            orphanLayerCallback: ((Layer) -> Boolean)? = null
        ): LayerTraceEntry {
            val layers = protos.map { newLayer(it) }
            val trace = LayerTraceEntry.fromFlattenedLayers(
                timestamp, layers, orphanLayerCallback)
            return LayerTraceEntry(trace.timestamp, trace.rootLayers)
        }

        @JvmStatic
        private fun newLayer(proto: Layers.LayerProto): Layer {
            return Layer(
                    proto.name ?: "",
                    proto.id,
                    proto.parent,
                    proto.z,
                    proto.visibleRegion.toRegion(),
                    proto.activeBuffer?.toBuffer(),
                    proto.flags,
                    proto.bounds?.toRectF() ?: RectF(),
                    proto.color?.toColor(),
                    proto.isOpaque,
                    proto.shadowRadius,
                    proto.cornerRadius,
                    proto.type ?: "",
                    proto.screenBounds?.toRectF(),
                    Transform(proto.transform, proto.position),
                    proto.sourceBounds?.toRectF(),
                    proto.currFrame,
                    proto.effectiveScalingMode,
                    Transform(proto.bufferTransform, position = null)
            )
        }

        @JvmStatic
        private fun Layers.FloatRectProto?.toRectF(): RectF? {
            return this?.let {
                val rect = RectF()
                rect.left = left
                rect.top = top
                rect.right = right
                rect.bottom = bottom

                rect
            }
        }

        @JvmStatic
        private fun Layers.ColorProto.toColor(): Color {
            return Color(r, g, b, a)
        }

        @JvmStatic
        private fun Layers.ActiveBufferProto.toBuffer(): Buffer {
            return Buffer(height, width)
        }

        /**
         * Extracts [Rect] from [RegionProto] by returning a rect that encompasses all
         * the rectangles making up the region.
         */
        @JvmStatic
        private fun RegionProto?.toRegion(): Region {
            return if (this == null) {
                Region(0, 0, 0, 0)
            } else {
                val region = android.graphics.Region(0, 0, 0, 0)

                for (proto: RectProto in this.rect) {
                    region.union(proto.toRect())
                }

                val bounds = region.bounds
                Region(bounds.left, bounds.top, bounds.right, bounds.bottom)
            }
        }

        @JvmStatic
        private fun RectProto.toRect(): Rect {
            return if ((this.right - this.left) <= 0 || (this.bottom - this.top) <= 0) {
                Rect()
            } else {
                Rect(this.left, this.top, this.right, this.bottom)
            }
        }
    }
}