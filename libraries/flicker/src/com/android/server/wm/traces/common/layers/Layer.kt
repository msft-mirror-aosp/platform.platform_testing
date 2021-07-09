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

package com.android.server.wm.traces.common.layers

import com.android.server.wm.traces.common.Buffer
import com.android.server.wm.traces.common.Color
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.Region
import com.android.server.wm.traces.common.RectF

/**
 * Represents a single layer with links to its parent and child layers.
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot
 * access internal Java/Android functionality
 *
 **/
data class Layer(
    val name: String,
    val id: Int,
    val parentId: Int,
    val z: Int,
    val visibleRegion: Region?,
    val activeBuffer: Buffer,
    val flags: Int,
    val bounds: RectF,
    val color: Color,
    private val _isOpaque: Boolean,
    val shadowRadius: Float,
    val cornerRadius: Float,
    val type: String,
    private val _screenBounds: RectF?,
    val transform: Transform,
    val sourceBounds: RectF,
    val currFrame: Long,
    val effectiveScalingMode: Int,
    val bufferTransform: Transform,
    val hwcCompositionType: Int,
    val hwcCrop: RectF,
    val hwcFrame: Rect,
    val backgroundBlurRadius: Int,
    val crop: Rect?,
    val isRelativeOf: Boolean,
    val zOrderRelativeOfId: Int
) {
    val stableId: String = "$type $id $name"
    var parent: Layer? = null
    var zOrderRelativeOf: Layer? = null
    var zOrderRelativeParentOf: Int = 0

    /**
     * Checks if the [Layer] is a root layer in the hierarchy
     *
     * @return
     */
    val isRootLayer: Boolean get() = parent == null

    private val _children = mutableListOf<Layer>()
    private val _occludedBy = mutableListOf<Layer>()
    private val _partiallyOccludedBy = mutableListOf<Layer>()
    private val _coveredBy = mutableListOf<Layer>()
    val children: Array<Layer>
        get() = _children.toTypedArray()
    val occludedBy: Array<Layer>
        get() = _occludedBy.toTypedArray()
    val partiallyOccludedBy: Array<Layer>
        get() = _partiallyOccludedBy.toTypedArray()
    val coveredBy: Array<Layer>
        get() = _coveredBy.toTypedArray()
    var isMissing: Boolean = false
        internal set

    /**
     * Checks if the layer's active buffer is empty
     *
     * An active buffer is empty if it is not in the proto or if its height or width are 0
     *
     * @return
     */
    val isActiveBufferEmpty: Boolean get() = activeBuffer.isEmpty

    /**
     * Checks if the layer is hidden, that is, if its flags contain 0x1 (FLAG_HIDDEN)
     *
     * @return
     */
    val isHiddenByPolicy: Boolean
        get() {
            return (flags and /* FLAG_HIDDEN */0x1) != 0x0 ||
                // offscreen layer root has a unique layer id
                id == 0x7FFFFFFD
        }

    /**
     * Checks if the layer is visible.
     *
     * A layer is visible if:
     * - it has an active buffer or has effects
     * - is not hidden
     * - is not transparent
     * - not occluded by other layers
     *
     * @return
     */
    val isVisible: Boolean
        get() {
            return when {
                isHiddenByParent -> false
                isHiddenByPolicy -> false
                isActiveBufferEmpty && !hasEffects -> false
                !fillsColor -> false
                occludedBy.isNotEmpty() -> false
                visibleRegion?.isEmpty ?: false -> false
                else -> !bounds.isEmpty
            }
        }

    val isOpaque: Boolean = if (color.a != 1.0f) false else _isOpaque

    /**
     * Checks if the [Layer] has a color
     *
     * @return
     */
    val fillsColor: Boolean get() = color.isNotEmpty

    /**
     * Checks if the [Layer] draws a shadow
     *
     * @return
     */
    val drawsShadows: Boolean get() = shadowRadius > 0

    /**
     * Checks if the [Layer] has blur
     *
     * @return
     */
    val hasBlur: Boolean get() = backgroundBlurRadius > 0

    /**
     * Checks if the [Layer] has rounded corners
     *
     * @return
     */
    val hasRoundedCorners: Boolean get() = cornerRadius > 0

    /**
     * Checks if the [Layer] draws has effects, which include:
     * - is a color layer
     * - is an effects layers which [fillsColor] or [drawsShadows]
     *
     * @return
     */
    val hasEffects: Boolean
        get() {
            // Support previous color layer
            if (isColorLayer) {
                return true
            }

            // Support newer effect layer
            return isEffectLayer && (fillsColor || drawsShadows)
        }

    /**
     * Checks if the [Layer] type is BufferStateLayer or BufferQueueLayer
     *
     * @return
     */
    val isBufferLayer: Boolean
        get() = type == "BufferStateLayer" || type == "BufferQueueLayer"

    /**
     * Checks if the [Layer] type is ColorLayer
     *
     * @return
     */
    val isColorLayer: Boolean get() = type == "ColorLayer"

    /**
     * Checks if the [Layer] type is ContainerLayer
     *
     * @return
     */
    val isContainerLayer: Boolean get() = type == "ContainerLayer"

    /**
     * Checks if the [Layer] type is EffectLayer
     *
     * @return
     */
    val isEffectLayer: Boolean get() = type == "EffectLayer"

    /**
     * Checks if the [Layer] is hidden by its parent
     *
     * @return
     */
    val isHiddenByParent: Boolean
        get() = !isRootLayer &&
            (parent?.isHiddenByPolicy == true || parent?.isHiddenByParent == true)

    /**
     * Gets a description of why the layer is (in)visible
     *
     * @return
     */
    val visibilityReason: String
        get() {
            return when {
                isVisible -> ""
                isContainerLayer -> "ContainerLayer"
                isHiddenByPolicy -> "Flag is hidden"
                isHiddenByParent -> "Hidden by parent ${parent?.name}"
                isBufferLayer && isActiveBufferEmpty -> "Buffer is empty"
                color.isEmpty -> "Alpha is 0"
                crop?.isEmpty ?: false -> "Crop is 0x0"
                bounds.isEmpty -> "Bounds is 0x0"
                !transform.isValid -> "Transform is invalid"
                isRelativeOf && zOrderRelativeOf == null -> "RelativeOf layer has been removed"
                isEffectLayer && !fillsColor && !drawsShadows && !hasBlur ->
                    "Effect layer does not have color fill, shadow or blur"
                _occludedBy.isNotEmpty() -> {
                    val occludedByIds = _occludedBy.joinToString(", ") { it.id.toString() }
                    "Layer is occluded by: $occludedByIds"
                }
                visibleRegion?.isEmpty ?: false ->
                    "Visible region calculated by Composition Engine is empty"
                else -> "Unknown"
            }
        }

    val screenBounds: RectF = when {
        visibleRegion?.isNotEmpty == true -> visibleRegion.toRectF()
        _screenBounds != null -> _screenBounds
        else -> transform.apply(bounds)
    }

    val absoluteZ: String
        get() {
            val zOrderRelativeOf = zOrderRelativeOf
            return buildString {
                when {
                    zOrderRelativeOf != null -> append(zOrderRelativeOf.absoluteZ).append(",")
                    parent != null -> append(parent?.absoluteZ).append(",")
                }
                append(z)
            }
        }

    fun contains(innerLayer: Layer): Boolean {
        return if (!this.transform.isSimpleRotation || !innerLayer.transform.isSimpleRotation) {
            false
        } else {
            this.screenBounds.contains(innerLayer.screenBounds)
        }
    }

    fun addChild(childLayer: Layer) {
        _children.add(childLayer)
    }

    fun addOccludedBy(layers: Array<Layer>) {
        _occludedBy.addAll(layers)
    }

    fun addPartiallyOccludedBy(layers: Array<Layer>) {
        _partiallyOccludedBy.addAll(layers)
    }

    fun addCoveredBy(layers: Array<Layer>) {
        _coveredBy.addAll(layers)
    }

    fun overlaps(other: Layer): Boolean =
        !this.screenBounds.intersection(other.screenBounds).isEmpty

    override fun toString(): String {
        return buildString {
            append(name)

            if (activeBuffer.isNotEmpty) {
                append(" buffer:$activeBuffer")
                append(" frame#$currFrame")
            }

            if (isVisible) {
                append(" visible:$visibleRegion")
            }
        }
    }
}