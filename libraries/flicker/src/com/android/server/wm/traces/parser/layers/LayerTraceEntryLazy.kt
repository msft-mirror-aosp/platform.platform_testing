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

package com.android.server.wm.traces.parser.layers

import android.graphics.Rect
import android.surfaceflinger.Common
import android.surfaceflinger.Display
import android.surfaceflinger.Layers
import com.android.server.wm.traces.common.ActiveBuffer
import com.android.server.wm.traces.common.Color
import com.android.server.wm.traces.common.RectF
import com.android.server.wm.traces.common.Size
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.Timestamp.Companion.NULL_TIMESTAMP
import com.android.server.wm.traces.common.layers.BaseLayerTraceEntry
import com.android.server.wm.traces.common.layers.HwcCompositionType
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.LayerTraceEntryBuilder
import com.android.server.wm.traces.common.region.Region

/** Lazy loading of a trace entry used by legacy Winscope */
@Deprecated("To be removed when legacy Winscope is discontinued. Use [LayerTraceEntry] instead")
class LayerTraceEntryLazy(
    override val elapsedTimestamp: Long,
    override val clockTimestamp: Long?,
    override val hwcBlob: String = "",
    override val where: String = "",
    override val vSyncId: Long = -1L,
    private val ignoreLayersStackMatchNoDisplay: Boolean = true,
    private val ignoreLayersInVirtualDisplay: Boolean = true,
    private var displayProtos: Array<Display.DisplayProto> = emptyArray(),
    private var layerProtos: Array<Layers.LayerProto> = emptyArray(),
    private val orphanLayerCallback: ((Layer) -> Boolean)? = null
) : BaseLayerTraceEntry() {
    override val timestamp =
        Timestamp(elapsedNanos = elapsedTimestamp, unixNanos = clockTimestamp ?: NULL_TIMESTAMP)

    private val parsedEntry by lazy {
        val layers = layerProtos.map { newLayer(it) }.toTypedArray()
        val displays = displayProtos.map { newDisplay(it) }.toTypedArray()
        val builder =
            LayerTraceEntryBuilder()
                .setElapsedTimestamp(timestamp.toString())
                .setLayers(layers)
                .setDisplays(displays)
                .setVSyncId(vSyncId.toString())
                .setHwcBlob(hwcBlob)
                .setWhere(where)
                .setOrphanLayerCallback(orphanLayerCallback)
                .ignoreLayersStackMatchNoDisplay(ignoreLayersStackMatchNoDisplay)
                .ignoreVirtualDisplay(ignoreLayersInVirtualDisplay)
        displayProtos = emptyArray()
        layerProtos = emptyArray()
        builder.build()
    }

    override val displays: Array<com.android.server.wm.traces.common.layers.Display>
        get() = parsedEntry.displays

    override val flattenedLayers: Array<Layer>
        get() = parsedEntry.flattenedLayers

    companion object {

        @JvmStatic
        fun newLayer(proto: Layers.LayerProto, excludeCompositionState: Boolean = false): Layer {
            // Differentiate between the cases when there's no HWC data on
            // the trace, and when the visible region is actually empty
            val activeBuffer = proto.activeBuffer.toBuffer()
            val visibleRegion = proto.visibleRegion.toRegion() ?: Region.EMPTY
            val crop = getCrop(proto.crop)
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

        fun newDisplay(
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

        @JvmStatic
        fun Layers.FloatRectProto?.toRectF(): RectF? {
            return this?.let { RectF.from(left = left, top = top, right = right, bottom = bottom) }
        }

        @JvmStatic
        fun Common.SizeProto?.toSize(): Size {
            return this?.let { Size.from(this.w, this.h) } ?: Size.EMPTY
        }

        @JvmStatic
        fun Common.ColorProto?.toColor(): Color {
            if (this == null) {
                return Color.EMPTY
            }
            return Color.from(r, g, b, a)
        }

        @JvmStatic
        fun Layers.ActiveBufferProto?.toBuffer(): ActiveBuffer {
            if (this == null) {
                return ActiveBuffer.EMPTY
            }
            return ActiveBuffer.from(width, height, stride, format)
        }

        @JvmStatic
        fun toHwcCompositionType(value: Layers.HwcCompositionType): HwcCompositionType {
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

        @JvmStatic
        fun getCrop(crop: Common.RectProto?): com.android.server.wm.traces.common.Rect? {
            return when {
                crop == null -> com.android.server.wm.traces.common.Rect.EMPTY
                // crop (0,0) (-1,-1) means no crop
                crop.right == -1 && crop.left == 0 && crop.bottom == -1 && crop.top == 0 -> null
                (crop.right - crop.left) <= 0 || (crop.bottom - crop.top) <= 0 ->
                    com.android.server.wm.traces.common.Rect.EMPTY
                else ->
                    com.android.server.wm.traces.common.Rect.from(
                        crop.left,
                        crop.top,
                        crop.right,
                        crop.bottom
                    )
            }
        }

        /**
         * Extracts [Rect] from [Common.RegionProto] by returning a rect that encompasses all the
         * rectangles making up the region.
         */
        @JvmStatic
        fun Common.RegionProto?.toRegion(): Region? {
            return if (this == null) {
                null
            } else {
                val rects = this.rectList.map { it.toRect() }.toTypedArray()
                return Region(rects)
            }
        }

        @JvmStatic
        fun Common.RectProto?.toRect(): com.android.server.wm.traces.common.Rect =
            com.android.server.wm.traces.common.Rect.from(
                this?.left ?: 0,
                this?.top ?: 0,
                this?.right ?: 0,
                this?.bottom ?: 0
            )
    }
}
