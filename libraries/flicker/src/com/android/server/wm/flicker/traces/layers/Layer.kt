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

package com.android.server.wm.flicker.traces.layers

import android.graphics.Rect
import android.surfaceflinger.nano.Layers
import android.surfaceflinger.nano.Layers.RectProto
import android.surfaceflinger.nano.Layers.RegionProto
import com.android.internal.R.attr.type
import com.android.server.wm.flicker.common.Buffer
import com.android.server.wm.flicker.common.Color
import com.android.server.wm.flicker.common.RectF
import com.android.server.wm.flicker.common.Region
import com.android.server.wm.flicker.common.traces.layers.ILayer

/** Represents a single layer with links to its parent and child layers.  */
class Layer(val proto: Layers.LayerProto) : ILayer<Layer> {
    val commonLayer: com.android.server.wm.flicker.common.traces.layers.Layer<Layer> =
        com.android.server.wm.flicker.common.traces.layers.Layer(
            proto.name ?: "",
            proto.id ?: -1,
            proto.parent ?: -1,
            proto.z ?: 0,
            proto.visibleRegion?.toRegion() ?: Region(0, 0, 0, 0),
            proto.activeBuffer?.toBuffer(),
            proto.flags ?: 0x0,
            proto.bounds?.toRectF() ?: RectF(),
            proto.color?.toColor(),
            proto.isOpaque ?: false,
            proto.shadowRadius ?: 0.0f,
            proto.type ?: "",
            proto.screenBounds?.toRectF(),
            Transform(proto.transform, proto.position),
            proto.sourceBounds?.toRectF()
        )

    override fun toString(): String {
        var value = "$name"
        if (proto.activeBuffer.width > 0 && proto.activeBuffer.height > 0) {
            value += " buffer:${proto.activeBuffer.width}x${proto.activeBuffer.height} " +
                    "frame#${proto.currFrame}"
        }

        if (isVisible) {
            value += " visible:$visibleRegion"
        }

        return value
    }

    /**
     * Extracts [Rect] from [RectProto].
     */
    private fun RectProto?.toRect(): Rect {
        return if (this == null) {
            Rect()
        } else {
            Rect(this.left, this.top, this.right, this.bottom)
        }
    }

    override val name: String
        get() = commonLayer.name
    override val id: Int
        get() = commonLayer.id
    override val parentId: Int
        get() = commonLayer.parentId
    override val visibleRegion: Region
        get() = commonLayer.visibleRegion
    override val z: Int
        get() = commonLayer.z
    override val sourceBounds: RectF?
        get() = commonLayer.sourceBounds
    override var parent: Layer
        get() = commonLayer.parent
        set(parent) {
            commonLayer.parent = parent
        }
    override val children: MutableCollection<Layer>
        get() = commonLayer.children
    override val occludedBy: MutableCollection<Layer>
        get() = commonLayer.occludedBy
    override val partiallyOccludedBy: MutableCollection<Layer>
        get() = commonLayer.partiallyOccludedBy
    override val coveredBy: MutableCollection<Layer>
        get() = commonLayer.coveredBy
    override val isVisible: Boolean
        get() = commonLayer.isVisible
    override val isHiddenByParent: Boolean
        get() = commonLayer.isHiddenByParent
    override val isOpaque: Boolean
        get() = commonLayer.isOpaque
    override val hiddenByParentReason: String
        get() = commonLayer.hiddenByParentReason
    override val isInvisible: Boolean
        get() = commonLayer.isInvisible
    override val visibilityReason: String
        get() = commonLayer.visibilityReason
    override val transform: com.android.server.wm.flicker.common.traces.layers.Transform
        get() = commonLayer.transform
    override val isHiddenByPolicy: Boolean
        get() = commonLayer.isHiddenByPolicy
    override val screenBounds: RectF
        get() = commonLayer.screenBounds

    override fun addChild(childLayer: Layer) {
        commonLayer.addChild(childLayer)
    }

    override fun contains(innerLayer: Layer): Boolean {
        return commonLayer.contains(innerLayer)
    }

    override fun overlaps(other: Layer): Boolean {
        return commonLayer.overlaps(other)
    }
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
private fun RegionProto.toRegion(): Region {
    val region = android.graphics.Region(0, 0, 0, 0)

    for (proto: RectProto in this.rect) {
        region.union(proto.toRect())
    }

    val bounds = region.bounds
    return Region(bounds.left, bounds.top, bounds.right, bounds.bottom)
}

private fun RectProto.toRect(): Rect {
    return if ((this.right - this.left) <= 0 || (this.bottom - this.top) <= 0) {
        Rect()
    } else {
        Rect(this.left, this.top, this.right, this.bottom)
    }
}
