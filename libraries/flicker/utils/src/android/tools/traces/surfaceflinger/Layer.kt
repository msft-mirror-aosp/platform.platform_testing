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
import android.tools.traces.component.ComponentName
import com.android.internal.annotations.VisibleForTesting

/**
 * Represents a single layer with links to its parent and child layers.
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot access internal
 * Java/Android functionality
 */
class Layer
@VisibleForTesting
public constructor(
    val name: String,
    val id: Int,
    val parentId: Int,
    val z: Int,
    val currFrame: Long,
    val isVisible: Boolean,
    val visibilityReason: Collection<String>,
    val occludedBy: Collection<Int>,
    properties: LayerProperties,
) : LayerProperties by properties {
    val stableId: String = "$id $name"
    var parent: Layer? = null
    var zOrderRelativeOf: Layer? = null
    var zOrderRelativeParentOf: Int = 0
    val packageName = ComponentName.fromLayerName(name).packageName

    /**
     * Checks if the [Layer] is an animation leash
     *
     * @return
     */
    val isAnimationLeash: Boolean
        get() = name.contains("animation-leash") || name.contains("transition-leash")

    /**
     * Checks if the [Layer] is a root layer in the hierarchy
     *
     * @return
     */
    val isRootLayer: Boolean
        get() = parent == null

    private val _children = mutableListOf<Layer>()
    val children: Collection<Layer>
        get() = _children

    val isTask: Boolean
        get() = name.startsWith("Task=")

    fun addChild(childLayer: Layer) {
        _children.add(childLayer)
    }

    override fun toString(): String {
        return buildString {
            append(name)

            if (!activeBuffer.isEmpty) {
                append(" buffer:$activeBuffer")
                append(" frame#$currFrame")
            }

            if (isVisible) {
                append(" visible:$visibleRegion")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Layer) return false

        if (name != other.name) return false
        if (id != other.id) return false
        if (parentId != other.parentId) return false
        if (z != other.z) return false
        if (currFrame != other.currFrame) return false
        if (stableId != other.stableId) return false
        if (zOrderRelativeOf != other.zOrderRelativeOf) return false
        if (zOrderRelativeParentOf != other.zOrderRelativeParentOf) return false
        if (visibleRegion != other.visibleRegion) return false
        if (activeBuffer != other.activeBuffer) return false
        if (flags != other.flags) return false
        if (bounds != other.bounds) return false
        if (color != other.color) return false
        if (shadowRadius != other.shadowRadius) return false
        if (cornerRadii != other.cornerRadii) return false
        if (effectiveCornerRadii != other.effectiveCornerRadii) return false
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
        if (isVisible != other.isVisible) return false
        if (visibilityReason != other.visibilityReason) return false
        if (occludedBy != other.occludedBy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = visibleRegion?.hashCode() ?: 0
        result = 31 * result + activeBuffer.hashCode()
        result = 31 * result + flags
        result = 31 * result + bounds.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + shadowRadius.hashCode()
        result = 31 * result + cornerRadii.hashCode()
        result = 31 * result + effectiveCornerRadii.hashCode()
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
        result = 31 * result + name.hashCode()
        result = 31 * result + id
        result = 31 * result + parentId
        result = 31 * result + z
        result = 31 * result + currFrame.hashCode()
        result = 31 * result + stableId.hashCode()
        result = 31 * result + (zOrderRelativeOf?.hashCode() ?: 0)
        result = 31 * result + zOrderRelativeParentOf
        result = 31 * result + _children.hashCode()
        result = 31 * result + isVisible.hashCode()
        result = 31 * result + visibilityReason.hashCode()
        result = 31 * result + occludedBy.hashCode()
        return result
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun from(
            name: String,
            id: Int,
            parentId: Int,
            z: Int = 0,
            visibleRegion: Region = Region(),
            activeBuffer: ActiveBuffer = ActiveBuffer.EMPTY,
            flags: Int = 0,
            bounds: RectF = RectF(),
            color: Color = emptyColor(),
            shadowRadius: Float = 0.0f,
            cornerRadii: CornerRadii = CornerRadii.EMPTY,
            effectiveCornerRadii: CornerRadii = CornerRadii.EMPTY,
            screenBounds: RectF = RectF(),
            transform: Transform = Transform.EMPTY,
            currFrame: Long = 0,
            effectiveScalingMode: Int = 0,
            bufferTransform: Transform = Transform.EMPTY,
            hwcCompositionType: HwcCompositionType = HwcCompositionType.HWC_TYPE_UNSPECIFIED,
            backgroundBlurRadius: Int = 0,
            crop: RectF? = RectF(),
            isRelativeOf: Boolean = false,
            zOrderRelativeOfId: Int = 0,
            stackId: Int = 0,
            isVisible: Boolean = false,
            visibilityReason: Collection<String> = emptyList<String>(),
            occludedBy: Collection<Int> = emptyList<Int>(),
        ): Layer {
            val properties =
                LayerPropertiesImpl.from(
                    visibleRegion,
                    activeBuffer,
                    flags,
                    bounds,
                    color,
                    shadowRadius,
                    cornerRadii,
                    effectiveCornerRadii,
                    screenBounds,
                    transform,
                    effectiveScalingMode,
                    bufferTransform,
                    hwcCompositionType,
                    backgroundBlurRadius,
                    crop ?: RectF(),
                    isRelativeOf,
                    zOrderRelativeOfId,
                    stackId,
                )
            return Layer(
                name,
                id,
                parentId,
                z,
                currFrame,
                isVisible,
                visibilityReason,
                occludedBy,
                properties,
            )
        }
    }
}
