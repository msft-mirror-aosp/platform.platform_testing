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

package com.android.server.wm.flicker.helpers

import android.graphics.Insets
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.service.PlatformConsts

/**
 * A class containing utility methods related to rotation.
 *
 * @hide
 */
object RotationUtils {
    /** Rotates an Insets according to the given rotation. */
    fun rotateInsets(insets: Insets?, rotation: PlatformConsts.Rotation): Insets {
        if (insets == null || insets === Insets.NONE) {
            return Insets.NONE
        }
        val rotated =
            when (rotation) {
                PlatformConsts.Rotation.ROTATION_0 -> insets
                PlatformConsts.Rotation.ROTATION_90 ->
                    Insets.of(insets.top, insets.right, insets.bottom, insets.left)
                PlatformConsts.Rotation.ROTATION_180 ->
                    Insets.of(insets.right, insets.bottom, insets.left, insets.top)
                PlatformConsts.Rotation.ROTATION_270 ->
                    Insets.of(insets.bottom, insets.left, insets.top, insets.right)
            }
        return rotated
    }

    /**
     * Rotates bounds as if parentBounds and bounds are a group. The group is rotated from
     * oldRotation to newRotation. This assumes that parentBounds is at 0,0 and remains at 0,0 after
     * rotation. The bounds will be at the same physical position in parentBounds.
     */
    fun rotateBounds(
        inBounds: Rect,
        parentBounds: Rect,
        oldRotation: PlatformConsts.Rotation,
        newRotation: PlatformConsts.Rotation
    ): Rect = rotateBounds(inBounds, parentBounds, deltaRotation(oldRotation, newRotation))

    /**
     * Rotates inOutBounds together with the parent for a given rotation delta. This assumes that
     * the parent starts at 0,0 and remains at 0,0 after the rotation. The inOutBounds will remain
     * at the same physical position within the parent.
     */
    fun rotateBounds(
        inBounds: Rect,
        parentWidth: Int,
        parentHeight: Int,
        rotation: PlatformConsts.Rotation
    ): Rect {
        val origLeft = inBounds.left
        val origTop = inBounds.top
        return when (rotation) {
            PlatformConsts.Rotation.ROTATION_0 -> inBounds
            PlatformConsts.Rotation.ROTATION_90 ->
                Rect.from(
                    left = inBounds.top,
                    top = parentWidth - inBounds.right,
                    right = inBounds.bottom,
                    bottom = parentWidth - origLeft
                )
            PlatformConsts.Rotation.ROTATION_180 ->
                Rect.from(
                    left = parentWidth - inBounds.right,
                    right = parentWidth - origLeft,
                    top = parentHeight - inBounds.bottom,
                    bottom = parentHeight - origTop
                )
            PlatformConsts.Rotation.ROTATION_270 ->
                Rect.from(
                    left = parentHeight - inBounds.bottom,
                    bottom = inBounds.right,
                    right = parentHeight - inBounds.top,
                    top = origLeft
                )
        }
    }

    /**
     * Rotates bounds as if parentBounds and bounds are a group. The group is rotated by `delta`
     * 90-degree counter-clockwise increments. This assumes that parentBounds is at 0,0 and remains
     * at 0,0 after rotation. The bounds will be at the same physical position in parentBounds.
     */
    fun rotateBounds(inBounds: Rect, parentBounds: Rect, rotation: PlatformConsts.Rotation): Rect =
        rotateBounds(inBounds, parentBounds.right, parentBounds.bottom, rotation)

    /** @return the rotation needed to rotate from oldRotation to newRotation. */
    fun deltaRotation(
        oldRotation: PlatformConsts.Rotation,
        newRotation: PlatformConsts.Rotation
    ): PlatformConsts.Rotation {
        var delta = newRotation.value - oldRotation.value
        if (delta < 0) delta += 4
        return PlatformConsts.Rotation.getByValue(delta)
    }
}
