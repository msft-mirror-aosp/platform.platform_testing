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

package com.android.server.wm.traces.common.layers

import com.android.server.wm.traces.common.ActiveBuffer
import com.android.server.wm.traces.common.Color
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.RectF
import com.android.server.wm.traces.common.layers.Transform.Companion.isFlagSet
import com.android.server.wm.traces.common.region.Region

/**
 * Common properties of a layer that are not related to their position in the hierarchy
 *
 * These properties are frequently stable throughout the trace and can be more efficiently cached
 * than the full layers
 */
interface ILayerProperties {
    val visibleRegion: Region?
    val activeBuffer: ActiveBuffer
    val flags: Int
    val bounds: RectF
    val color: Color
    val isOpaque: Boolean
    val shadowRadius: Float
    val cornerRadius: Float
    val type: String
    val screenBounds: RectF
    val transform: Transform
    val sourceBounds: RectF
    val effectiveScalingMode: Int
    val bufferTransform: Transform
    val hwcCompositionType: Int
    val hwcCrop: RectF
    val hwcFrame: Rect
    val backgroundBlurRadius: Int
    val crop: Rect?
    val isRelativeOf: Boolean
    val zOrderRelativeOfId: Int
    val stackId: Int
    val requestedTransform: Transform
    val requestedColor: Color
    val cornerRadiusCrop: RectF
    val inputTransform: Transform
    val inputRegion: Region?

    val isScaling: Boolean get() = isTransformFlagSet(Transform.SCALE_VAL)
    val isTranslating: Boolean get() = isTransformFlagSet(Transform.TRANSLATE_VAL)
    val isRotating: Boolean get() = isTransformFlagSet(Transform.ROTATE_VAL)

    /**
     * Checks if the layer's active buffer is empty
     *
     * An active buffer is empty if it is not in the proto or if its height or width are 0
     *
     * @return
     */
    val isActiveBufferEmpty: Boolean get() = activeBuffer.isEmpty

    /**
     * Layer state flags as defined in LayerState.h
     */
    enum class Flag(val value: Int) {
        HIDDEN(0x01),
        OPAQUE(0x02),
        SKIP_SCREENSHOT(0x40),
        SECURE(0x80),
        ENABLE_BACKPRESSURE(0x100),
        DISPLAY_DECORATION(0x200),
        IGNORE_DESTINATION_FRAME(0x400)
    }

    /**
     * Converts flags to human readable tokens.
     *
     * @return
     */
    val verboseFlags: String
        get() {
            val tokens = Flag.values().filter { (it.value and flags) != 0 }.map { it.name }

            return if (tokens.isEmpty()) {
                ""
            } else {
                "${tokens.joinToString("|")} (0x${flags.toString(16)})"
            }
        }

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

    private fun isTransformFlagSet(transform: Int): Boolean =
        this.transform.type?.isFlagSet(transform) ?: false
}
