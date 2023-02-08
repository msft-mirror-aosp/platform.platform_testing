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

import android.surfaceflinger.Common
import android.surfaceflinger.Display
import android.surfaceflinger.Layers
import android.surfaceflinger.Layerstrace
import com.android.server.wm.traces.common.ActiveBuffer
import com.android.server.wm.traces.common.Color
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.RectF
import com.android.server.wm.traces.common.Size
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.layers.HwcCompositionType
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.layers.LayerTraceEntryBuilder
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.region.Region
import com.android.server.wm.traces.parser.AbstractTraceParser

/** Parser for [LayersTrace] objects containing traces or state dumps */
class LayersTraceParser(
    private val ignoreLayersStackMatchNoDisplay: Boolean = true,
    private val ignoreLayersInVirtualDisplay: Boolean = true,
    private val legacyTrace: Boolean = false,
    private val orphanLayerCallback: ((Layer) -> Boolean)? = null,
) :
    AbstractTraceParser<
        Layerstrace.LayersTraceFileProto, Layerstrace.LayersTraceProto, LayerTraceEntry, LayersTrace
    >() {
    private var realToElapsedTimeOffsetNanos = Timestamp.NULL_TIMESTAMP

    override val traceName: String = "Layers Trace"

    override fun doDecodeByteArray(bytes: ByteArray): Layerstrace.LayersTraceFileProto =
        Layerstrace.LayersTraceFileProto.parseFrom(bytes)

    override fun createTrace(entries: List<LayerTraceEntry>): LayersTrace =
        LayersTrace(entries.toTypedArray())

    override fun getEntries(
        input: Layerstrace.LayersTraceFileProto
    ): List<Layerstrace.LayersTraceProto> = input.entryList

    override fun getTimestamp(entry: Layerstrace.LayersTraceProto): Timestamp {
        require(legacyTrace || realToElapsedTimeOffsetNanos != Timestamp.NULL_TIMESTAMP)
        return Timestamp(
            systemUptimeNanos = entry.elapsedRealtimeNanos,
            unixNanos = entry.elapsedRealtimeNanos + realToElapsedTimeOffsetNanos
        )
    }

    override fun onBeforeParse(input: Layerstrace.LayersTraceFileProto) {
        realToElapsedTimeOffsetNanos = input.realToElapsedTimeOffsetNanos
    }

    override fun doParseEntry(entry: Layerstrace.LayersTraceProto): LayerTraceEntry {
        val layers = entry.layers.layersList.map { newLayer(it) }.toTypedArray()
        val displays = entry.displaysList.map { newDisplay(it) }.toTypedArray()
        val builder =
            LayerTraceEntryBuilder()
                .setElapsedTimestamp(entry.elapsedRealtimeNanos.toString())
                .setLayers(layers)
                .setDisplays(displays)
                .setVSyncId(entry.vsyncId.toString())
                .setHwcBlob(entry.hwcBlob)
                .setWhere(entry.where)
                .setRealToElapsedTimeOffsetNs(realToElapsedTimeOffsetNanos.toString())
                .setOrphanLayerCallback(orphanLayerCallback)
                .ignoreLayersStackMatchNoDisplay(ignoreLayersStackMatchNoDisplay)
                .ignoreVirtualDisplay(ignoreLayersInVirtualDisplay)
        return builder.build()
    }

    companion object {
        private fun newLayer(
            proto: Layers.LayerProto,
            excludeCompositionState: Boolean = false
        ): Layer {
            // Differentiate between the cases when there's no HWC data on
            // the trace, and when the visible region is actually empty
            val activeBuffer = proto.activeBuffer.toBuffer()
            val visibleRegion = proto.visibleRegion.toRegion() ?: Region.EMPTY
            val crop = proto.crop?.toCropRect()
            return Layer.from(
                proto.name ?: "",
                proto.id,
                proto.parent,
                proto.z,
                visibleRegion,
                activeBuffer,
                proto.flags,
                proto.bounds?.toRectF() ?: RectF.EMPTY,
                proto.color.toColor(),
                proto.isOpaque,
                proto.shadowRadius,
                proto.cornerRadius,
                proto.type ?: "",
                proto.screenBounds?.toRectF() ?: RectF.EMPTY,
                Transform(proto.transform, proto.position),
                proto.sourceBounds?.toRectF() ?: RectF.EMPTY,
                proto.currFrame,
                proto.effectiveScalingMode,
                Transform(proto.bufferTransform, position = null),
                toHwcCompositionType(proto.hwcCompositionType),
                proto.hwcCrop.toRectF() ?: RectF.EMPTY,
                proto.hwcFrame.toRect(),
                proto.backgroundBlurRadius,
                crop,
                proto.isRelativeOf,
                proto.zOrderRelativeOf,
                proto.layerStack,
                Transform(proto.transform, position = proto.requestedPosition),
                proto.requestedColor.toColor(),
                proto.cornerRadiusCrop?.toRectF() ?: RectF.EMPTY,
                Transform(proto.inputWindowInfo?.transform, position = null),
                proto.inputWindowInfo?.touchableRegion?.toRegion(),
                excludeCompositionState
            )
        }

        private fun newDisplay(
            proto: Display.DisplayProto
        ): com.android.server.wm.traces.common.layers.Display {
            return com.android.server.wm.traces.common.layers.Display.from(
                proto.id.toULong(),
                proto.name,
                proto.layerStack,
                proto.size.toSize(),
                proto.layerStackSpaceRect.toRect(),
                Transform(proto.transform, position = null),
                proto.isVirtual
            )
        }

        private fun Layers.FloatRectProto?.toRectF(): RectF? {
            return this?.let { RectF.from(left = left, top = top, right = right, bottom = bottom) }
        }

        private fun Common.SizeProto?.toSize(): Size {
            return this?.let { Size.from(this.w, this.h) } ?: Size.EMPTY
        }

        private fun Common.ColorProto?.toColor(): Color {
            return this?.let { Color.from(r, g, b, a) } ?: Color.EMPTY
        }

        private fun Layers.ActiveBufferProto?.toBuffer(): ActiveBuffer {
            return this?.let { ActiveBuffer.from(width, height, stride, format) }
                ?: ActiveBuffer.EMPTY
        }

        private fun toHwcCompositionType(value: Layers.HwcCompositionType): HwcCompositionType {
            return when (value) {
                Layers.HwcCompositionType.INVALID -> HwcCompositionType.INVALID
                Layers.HwcCompositionType.CLIENT -> HwcCompositionType.CLIENT
                Layers.HwcCompositionType.DEVICE -> HwcCompositionType.DEVICE
                Layers.HwcCompositionType.SOLID_COLOR -> HwcCompositionType.SOLID_COLOR
                Layers.HwcCompositionType.CURSOR -> HwcCompositionType.CURSOR
                Layers.HwcCompositionType.SIDEBAND -> HwcCompositionType.SIDEBAND
                else -> HwcCompositionType.UNRECOGNIZED
            }
        }

        private fun Common.RectProto?.toCropRect(): Rect? {
            return when {
                this == null -> Rect.EMPTY
                // crop (0,0) (-1,-1) means no crop
                right == -1 && left == 0 && bottom == -1 && top == 0 -> null
                (right - left) <= 0 || (bottom - top) <= 0 -> Rect.EMPTY
                else -> Rect.from(left, top, right, bottom)
            }
        }

        /**
         * Extracts [Rect] from [Common.RegionProto] by returning a rect that encompasses all the
         * rectangles making up the region.
         */
        private fun Common.RegionProto?.toRegion(): Region? {
            return this?.let {
                val rectArray = this.rectList.map { it.toRect() }.toTypedArray()
                return Region(rectArray)
            }
        }

        fun Common.RectProto?.toRect(): Rect =
            Rect.from(this?.left ?: 0, this?.top ?: 0, this?.right ?: 0, this?.bottom ?: 0)
    }
}
