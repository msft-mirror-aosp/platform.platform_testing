/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.traces.surfaceflinger

import android.graphics.Color
import android.graphics.RectF
import android.graphics.Region
import android.tools.datatypes.ActiveBuffer
import android.tools.datatypes.emptyColor
import android.tools.withCache

/** {@inheritDoc} */
class LayerPropertiesImpl
private constructor(
    override val visibleRegion: Region,
    override val activeBuffer: ActiveBuffer,
    override val flags: Int,
    override val bounds: RectF,
    override val color: Color,
    override val shadowRadius: Float,
    override val cornerRadii: CornerRadii,
    override val screenBounds: RectF,
    override val transform: Transform,
    override val effectiveScalingMode: Int,
    override val bufferTransform: Transform,
    override val hwcCompositionType: HwcCompositionType,
    override val backgroundBlurRadius: Int,
    override val crop: RectF = RectF(),
    override val isRelativeOf: Boolean,
    override val zOrderRelativeOfId: Int,
    override val stackId: Int,
) : LayerProperties {
    override fun hashCode(): Int {
        var result = visibleRegion.hashCode()
        result = 31 * result + activeBuffer.hashCode()
        result = 31 * result + flags
        result = 31 * result + bounds.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + shadowRadius.hashCode()
        result = 31 * result + cornerRadii.hashCode()
        result = 31 * result + screenBounds.hashCode()
        result = 31 * result + transform.hashCode()
        result = 31 * result + effectiveScalingMode
        result = 31 * result + bufferTransform.hashCode()
        result = 31 * result + hwcCompositionType.hashCode()
        result = 31 * result + backgroundBlurRadius
        result = 31 * result + crop.hashCode()
        result = 31 * result + isRelativeOf.hashCode()
        result = 31 * result + zOrderRelativeOfId
        result = 31 * result + stackId
        result = 31 * result + screenBounds.hashCode()
        return result
    }

    override fun toString(): String {
        return "LayerProperties(visibleRegion=$visibleRegion, activeBuffer=$activeBuffer, " +
            "flags=$flags, bounds=$bounds, color=$color, " +
            "shadowRadius=$shadowRadius, cornerRadii=$cornerRadii, " +
            "screenBounds=$screenBounds, transform=$transform, " +
            "effectiveScalingMode=$effectiveScalingMode, bufferTransform=$bufferTransform, " +
            "hwcCompositionType=$hwcCompositionType, " +
            "backgroundBlurRadius=$backgroundBlurRadius, crop=$crop, isRelativeOf=$isRelativeOf, " +
            "zOrderRelativeOfId=$zOrderRelativeOfId, stackId=$stackId, " +
            "screenBounds=$screenBounds)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerPropertiesImpl) return false

        if (visibleRegion != other.visibleRegion) return false
        if (activeBuffer != other.activeBuffer) return false
        if (flags != other.flags) return false
        if (bounds != other.bounds) return false
        if (color != other.color) return false
        if (shadowRadius != other.shadowRadius) return false
        if (cornerRadii != other.cornerRadii) return false
        if (screenBounds != other.screenBounds) return false
        if (transform != other.transform) return false
        if (effectiveScalingMode != other.effectiveScalingMode) return false
        if (bufferTransform != other.bufferTransform) return false
        if (hwcCompositionType != other.hwcCompositionType) return false
        if (backgroundBlurRadius != other.backgroundBlurRadius) return false
        if (crop != other.crop) return false
        if (isRelativeOf != other.isRelativeOf) return false
        if (zOrderRelativeOfId != other.zOrderRelativeOfId) return false
        if (stackId != other.stackId) return false
        if (screenBounds != other.screenBounds) return false

        return true
    }

    companion object {
        val EMPTY: LayerPropertiesImpl
            get() = withCache {
                LayerPropertiesImpl(
                    visibleRegion = Region(),
                    activeBuffer = ActiveBuffer.EMPTY,
                    flags = 0,
                    bounds = RectF(),
                    color = emptyColor(),
                    shadowRadius = 0f,
                    cornerRadii = CornerRadii.EMPTY,
                    screenBounds = RectF(),
                    transform = Transform.EMPTY,
                    effectiveScalingMode = 0,
                    bufferTransform = Transform.EMPTY,
                    hwcCompositionType = HwcCompositionType.HWC_TYPE_UNSPECIFIED,
                    backgroundBlurRadius = 0,
                    isRelativeOf = false,
                    zOrderRelativeOfId = 0,
                    stackId = 0,
                )
            }

        @JvmStatic
        @JvmOverloads
        fun from(
            visibleRegion: Region = Region(),
            activeBuffer: ActiveBuffer = ActiveBuffer.EMPTY,
            flags: Int = 0,
            bounds: RectF = RectF(),
            color: Color = emptyColor(),
            shadowRadius: Float = 0f,
            cornerRadii: CornerRadii = CornerRadii.EMPTY,
            screenBounds: RectF = RectF(),
            transform: Transform = Transform.EMPTY,
            effectiveScalingMode: Int = 0,
            bufferTransform: Transform = Transform.EMPTY,
            hwcCompositionType: HwcCompositionType = HwcCompositionType.HWC_TYPE_UNSPECIFIED,
            backgroundBlurRadius: Int = 0,
            crop: RectF = RectF(),
            isRelativeOf: Boolean = false,
            zOrderRelativeOfId: Int = 0,
            stackId: Int = 0,
        ): LayerProperties {
            return withCache {
                LayerPropertiesImpl(
                    visibleRegion,
                    activeBuffer,
                    flags,
                    bounds,
                    color,
                    shadowRadius,
                    cornerRadii,
                    screenBounds,
                    transform,
                    effectiveScalingMode,
                    bufferTransform,
                    hwcCompositionType,
                    backgroundBlurRadius,
                    crop,
                    isRelativeOf,
                    zOrderRelativeOfId,
                    stackId,
                )
            }
        }
    }
}
