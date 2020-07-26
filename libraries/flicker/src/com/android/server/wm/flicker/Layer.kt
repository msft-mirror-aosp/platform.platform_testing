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

import android.graphics.Rect
import android.graphics.Region
import android.surfaceflinger.nano.Layers
import android.surfaceflinger.nano.Layers.RectProto
import android.surfaceflinger.nano.Layers.RegionProto

/** Represents a single layer with links to its parent and child layers.  */
class Layer(var proto: Layers.LayerProto?) {

    val children = mutableListOf<Layer>()
    lateinit var parent: Layer

    fun addChild(childLayer: Layer) {
        children.add(childLayer)
    }

    fun addParent(parentLayer: Layer) {
        parent = parentLayer
    }

    /**
     * Gets the layer ID
     *
     * @return layer ID or -1 if the layer has no proto
     */
    val id get() = proto?.id ?: -1

    /**
     * Gets the parent layer ID
     *
     * @return parent layer ID or -1 if the layer has no proto or the proto has no parent
     */
    val parentId get() = proto?.parent ?: -1

    /**
     * Gets the layer name
     *
     * @return layer name or an empty string if the layer has no proto
     */
    val name get() = proto?.name ?: ""

    /**
     * Gets the visible region as a [Rect]
     *
     * @return visible region or an empty [Rect] if the layer has no proto
     */
    val visibleRegion get() = proto?.visibleRegion?.extract() ?: Region()

    /**
     * Checks if the layer's active buffer is empty
     *
     * An active buffer is empty if it is not in the proto or if its height or width are 0
     *
     * @return
     */
    val isActiveBufferEmpty: Boolean
        get() {
            val activeBuffer = proto?.activeBuffer
            return (activeBuffer == null)
                    || (activeBuffer.height == 0)
                    || (activeBuffer.width == 0)
        }

    /**
     * Checks if the layer is hidden, that is, if its flags contain 0x1 (FLAG_HIDDEN)
     *
     * @return
     */
    val isHidden: Boolean
        get() {
            val flags = proto?.flags ?: 0x0
            return (flags and  /* FLAG_HIDDEN */0x1) != 0x0
        }

    /**
     * Checks if the layer is visible.
     *
     * If a layer has [children] its visibility is determined by that of its children of type
     * [isBufferStateLayer], otherwise a layer is visible if:
     * - it has an active buffer or has effects
     * - is not hidden
     * - is not transparent
     * - has a visible region
     *
     * @return
     */
    val isVisible: Boolean
        get() {
            return if (children.isEmpty()) {
                ((!isActiveBufferEmpty || hasEffects())
                        && !isHidden
                        && fillsColor
                        && !visibleRegion.isEmpty)
            } else {
                children.any { it.isBufferStateLayer && it.isVisible }
            }
        }

    /**
     * Checks if the [Layer] name contains [layerName]

     * @return
     */
    fun nameContains(layerName: String): Boolean {
        return this.proto?.name?.contains(layerName) ?: false
    }

    /**
     * Checks if the [Layer] has a color
     *
     * @return
     */
    val fillsColor: Boolean
        get() {
            val proto = proto ?: return false
            return (proto.color != null
                    && proto.color.a > 0
                    && proto.color.r >= 0
                    && proto.color.g >= 0
                    && proto.color.b >= 0)
        }

    /**
     * Checks if the [Layer] draws a shadow
     *
     * @return
     */
    val drawsShadows get() = (proto?.shadowRadius ?: 0.0f) > 0

    /**
     * Checks if the [Layer] draws has effects, which include:
     * - is a color layer
     * - is an effects layers which [fillsColor] or [drawsShadows]
     *
     * @return
     */
    fun hasEffects(): Boolean {
        // Support previous color layer
        if (isColorLayer) {
            return true
        }

        // Support newer effect layer
        return isEffectLayer && (fillsColor || drawsShadows)
    }

    /**
     * Gets the [Layer] type
     *
     * @return
     */
    val type get() = proto?.type ?: ""

    /**
     * Checks if the [Layer] type is BufferStateLayer
     *
     * @return
     */
    val isBufferStateLayer get() = type == "BufferStateLayer"

    /**
     * Checks if the [Layer] type is ColorLayer
     *
     * @return
     */
    val isColorLayer get() = type == "ColorLayer"

    /**
     * Checks if the [Layer] type is EffectLayer
     *
     * @return
     */
    val isEffectLayer get() = type == "EffectLayer"

    /**
     * Checks if the [Layer] is a root layer in the hierarchy
     *
     * @return
     */
    val isRootLayer get() = ::parent.isInitialized && parent.proto == null

    /**
     * Checks if the [Layer] is not visible
     *
     * @return
     */
    val isInvisible get() = !isVisible

    /**
     * Checks if the [Layer] is hidden by its parent
     *
     * @return
     */
    val isHiddenByParent: Boolean
        get() = !isRootLayer && (parent.isHidden || parent.isHiddenByParent)

    /**
     * Gets a description of why the layer is hidden by its parent
     *
     * @return
     */
    val hiddenByParentReason: String
        get() {
            var reason = "Layer $name"
            reason += if (isHiddenByParent) {
                " is hidden by parent: " + parent.name
            } else {
                " is not hidden by parent: " + parent.name
            }
            return reason
        }

    /**
     * Gets a description of why the layer is (in)visible
     *
     * @return
     */
    val visibilityReason: String
        get() {
            val proto = proto ?: return "Not proto layer attached"
            var reason = "Layer $name"
            if (isVisible) {
                reason += " is visible:"
            } else {
                reason += " is invisible:"
                when {
                    proto.activeBuffer == null -> {
                        reason += " activeBuffer=null"
                    }
                    proto.activeBuffer.height == 0 -> {
                        reason += " activeBuffer.height=0"
                    }
                    proto.activeBuffer.width == 0 -> {
                        reason += " activeBuffer.width=0"
                    }
                }
                if (!isColorLayer) {
                    reason += " type != ColorLayer"
                }
                if (isHidden) {
                    reason += " flags=" + proto.flags + " (FLAG_HIDDEN set)"
                }
                if (proto.color == null || proto.color.a == 0f) {
                    reason += " color.a=0"
                }
                if (visibleRegion.isEmpty) {
                    reason += " visible region is empty"
                }
            }
            return reason
        }

    override fun toString(): String {
        var value = "$name $type $visibleRegion"

        if (isVisible) {
            value += "(visible)"
        }

        return value
    }

    /**
     * Extracts [Rect] from [RectProto].
     */
    private fun RectProto?.extract(): Rect {
        return if (this == null) {
            Rect()
        } else {
            Rect(this.left, this.top, this.right, this.bottom)
        }
    }

    /**
     * Extracts [Rect] from [RegionProto] by returning a rect that encompasses all
     * the rectangles making up the region.
     */
    private fun RegionProto.extract(): Region {
        val region = Region()
        for (proto: RectProto in this.rect) {
            region.union(proto.extract())
        }
        return region
    }
}