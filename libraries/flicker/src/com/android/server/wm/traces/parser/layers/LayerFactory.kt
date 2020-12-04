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

package com.android.server.wm.traces.parser.layers

import android.graphics.Rect
import android.surfaceflinger.nano.Layers
import android.surfaceflinger.nano.Layers.RectProto
import android.surfaceflinger.nano.Layers.RegionProto
import com.android.server.wm.traces.common.Buffer
import com.android.server.wm.traces.common.Color
import com.android.server.wm.traces.common.RectF
import com.android.server.wm.traces.common.Region
import com.android.server.wm.traces.common.layers.Layer

/**
 * Represents a single layer with links to its parent and child layers.
 *
 * This is object is exclusively accessed by Java/Android code and can access internal
 * Java/Android functionality
 *
 **/
class LayerFactory {
    companion object {
        @JvmStatic
        fun fromProto(proto: Layers.LayerProto): Layer {
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
                    proto.type ?: "",
                    proto.screenBounds?.toRectF(),
                    Transform(proto.transform, proto.position),
                    proto.sourceBounds?.toRectF(),
                    proto.currFrame,
                    proto.effectiveScalingMode,
                    Transform(proto.bufferTransform, position = null)
            )
        }

        private fun Layers.FloatRectProto.toRectF(): RectF {
            val rect = RectF()
            rect.left = left
            rect.top = top
            rect.right = right
            rect.bottom = bottom

            return rect
        }

        private fun Layers.ColorProto.toColor(): Color {
            return Color(r, g, b, a)
        }

        private fun Layers.ActiveBufferProto.toBuffer(): Buffer {
            return Buffer(height, width)
        }

        /**
         * Extracts [Rect] from [RegionProto] by returning a rect that encompasses all
         * the rectangles making up the region.
         */
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

        private fun RectProto.toRect(): Rect {
            return if ((this.right - this.left) <= 0 || (this.bottom - this.top) <= 0) {
                Rect()
            } else {
                Rect(this.left, this.top, this.right, this.bottom)
            }
        }
    }
}