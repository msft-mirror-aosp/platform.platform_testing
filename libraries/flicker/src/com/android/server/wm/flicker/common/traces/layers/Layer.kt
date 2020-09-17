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

package com.android.server.wm.flicker.common.traces.layers

import com.android.server.wm.flicker.common.Buffer
import com.android.server.wm.flicker.common.Color
import com.android.server.wm.flicker.common.Region
import com.android.server.wm.flicker.common.RectF

/** Represents a single layer with links to its parent and child layers.  */
open class Layer<LayerT : ILayer<LayerT>>(
    override val name: String,
    override val id: Int,
    override val parentId: Int,
    override val z: Int,
    override val visibleRegion: Region,
    val activeBuffer: Buffer?,
    val flags: Int,
    val bounds: RectF,
    val color: Color?,
    val _isOpaque: Boolean,
    val shadowRadius: Float,
    val type: String,
    val _screenBounds: RectF?,
    override val transform: Transform,
    override val sourceBounds: RectF?
) : ILayer<LayerT> {
    override lateinit var parent: LayerT

    /**
     * Checks if the [Layer] is a root layer in the hierarchy
     *
     * @return
     */
    val isRootLayer: Boolean
        get() {
            return !::parent.isInitialized
        }

    override val children = mutableListOf<LayerT>()
    override val occludedBy = mutableListOf<LayerT>()
    override val partiallyOccludedBy = mutableListOf<LayerT>()
    override val coveredBy = mutableListOf<LayerT>()

    override fun addChild(childLayer: LayerT) {
        children.add(childLayer)
    }

    /**
     * Checks if the layer's active buffer is empty
     *
     * An active buffer is empty if it is not in the proto or if its height or width are 0
     *
     * @return
     */
    val isActiveBufferEmpty: Boolean
        get() {
            return (activeBuffer == null) ||
                    (activeBuffer.height == 0) ||
                    (activeBuffer.width == 0)
        }

    /**
     * Checks if the layer is hidden, that is, if its flags contain 0x1 (FLAG_HIDDEN)
     *
     * @return
     */
    override val isHiddenByPolicy: Boolean
        get() {
            return (flags and /* FLAG_HIDDEN */0x1) != 0x0
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
    override val isVisible: Boolean
        get() {
            return when {
                isHiddenByPolicy -> false
                isActiveBufferEmpty && !hasEffects -> false
                !fillsColor -> false
                occludedBy.isNotEmpty() -> false
                else -> !bounds.empty
            }
        }

    override val isOpaque: Boolean
        get() {
            return if (color?.a != 1.0f) {
                false
            } else {
                _isOpaque
            }
        }

    /**
     * Checks if the [Layer] has a color
     *
     * @return
     */
    val fillsColor: Boolean
        get() {
            return (color != null &&
                    color.a > 0 &&
                    color.r >= 0 &&
                    color.g >= 0 &&
                    color.b >= 0)
        }

    /**
     * Checks if the [Layer] draws a shadow
     *
     * @return
     */
    val drawsShadows get() = (shadowRadius ?: 0.0f) > 0

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
        get() {
            return type == "BufferStateLayer" || type == "BufferQueueLayer"
        }

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
     * Checks if the [Layer] is not visible
     *
     * @return
     */
    override val isInvisible get() = !isVisible

    /**
     * Checks if the [Layer] is hidden by its parent
     *
     * @return
     */
    override val isHiddenByParent: Boolean
        get() = !isRootLayer && (parent.isHiddenByPolicy || parent.isHiddenByParent)

    /**
     * Gets a description of why the layer is hidden by its parent
     *
     * @return
     */
    override val hiddenByParentReason: String
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
    override val visibilityReason: String
        get() {
            var reason = "Layer $name"
            if (isVisible) {
                reason += " is visible:"
            } else {
                reason += " is invisible:"
                when {
                    activeBuffer == null -> {
                        reason += " activeBuffer=null"
                    }
                    activeBuffer.height == 0 -> {
                        reason += " activeBuffer.height=0"
                    }
                    activeBuffer.width == 0 -> {
                        reason += " activeBuffer.width=0"
                    }
                }
                if (!isColorLayer) {
                    reason += " type != ColorLayer"
                }
                if (isHiddenByPolicy) {
                    reason += " flags=" + flags + " (FLAG_HIDDEN set)"
                }
                if (color == null || color.a == 0f) {
                    reason += " color.a=0"
                }
                if (visibleRegion.empty) {
                    reason += " visible region is empty"
                }
            }
            return reason
        }

    override val screenBounds: RectF
        get() {
            return when {
                _screenBounds != null -> _screenBounds
                else -> transform.apply(bounds)
            }
        }

    override fun contains(innerLayer: LayerT): Boolean {
        return if (!this.transform.isSimpleRotation || !innerLayer.transform.isSimpleRotation) {
            false
        } else {
            this.screenBounds.contains(innerLayer.screenBounds)
        }
    }

    override fun overlaps(other: LayerT): Boolean = this.screenBounds.intersect(other.screenBounds)

    override fun toString(): String {
        var value = "$name $type $visibleRegion"

        if (isVisible) {
            value += "(visible)"
        }

        return value
    }
}