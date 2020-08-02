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

import android.graphics.Matrix
import android.graphics.RectF
import android.surfaceflinger.nano.Layers

class Transform(transform: Layers.TransformProto?, position: Layers.PositionProto?) {
    private val matrix by lazy {
        val x = position?.x ?: 0f
        val y = position?.y ?: 0f

        when {
            transform == null || isSimpleTransform -> transform?.type.getDefaultTransform(x, y)
            else -> Matrix().also {
                val matrixValues = arrayOf(transform.dsdx, transform.dtdx, x,
                        transform.dsdy, transform.dtdy, y,
                        0.0f, 0.0f, 1.0f)
                it.setValues(matrixValues.toFloatArray())
            }
        }
    }

    /**
     * Returns true if the applying the transform on an an axis aligned rectangle
     * results in another axis aligned rectangle.
     */
    val isSimpleRotation = !(transform?.type?.isFlagSet(ROT_INVALID_VAL) ?: true)

    private val isSimpleTransform
            = transform?.type?.isFlagClear(ROT_INVALID_VAL or SCALE_VAL) ?: false

    private fun Int.isFlagClear(bits: Int): Boolean {
        return this and bits > 0
    }

    private fun Int.isFlagSet(bits: Int): Boolean {
        return this and bits == bits
    }

    private fun Int?.getDefaultTransform(x: Float, y: Float): Matrix {
        val matrixValues = when {
            // IDENTITY
            this == null -> arrayOf(1f, 0f, x, 0f, 1f, y, 0f, 0f, 1f)
            // // ROT_270 = ROT_90|FLIP_H|FLIP_V
            isFlagSet(ROT_90_VAL or FLIP_V_VAL or FLIP_H_VAL) -> arrayOf(0f, -1f, x, 1f, 0f, y, 0f, 0f, 1f)
            // ROT_180 = FLIP_H|FLIP_V
            isFlagSet(FLIP_V_VAL or FLIP_H_VAL) -> arrayOf(-1f, 0f, x, 0f, -1f, y, 0f, 0f, 1f)
            // ROT_90
            isFlagSet(ROT_90_VAL) -> arrayOf(0f, 1f, x, -1f, 0f, y, 0f, 0f, 1f)
            // IDENTITY
            isFlagClear(SCALE_VAL or ROTATE_VAL) -> arrayOf(1f, 0f, x, 0f, 1f, y, 0f, 0f, 1f)
            else -> throw IllegalStateException("Unknown transform type $this")
        }

        return Matrix().also { it.setValues(matrixValues.toFloatArray()) }
    }

    fun apply(rect: RectF?): RectF {
        val src = rect ?: RectF()
        val dst = RectF()
        matrix.mapRect(dst, src)
        return dst
    }

    companion object {
        /* transform type flags */
        private const val TRANSLATE_VAL = 0x0001
        private const val ROTATE_VAL = 0x0002
        private const val SCALE_VAL = 0x0004

        /* orientation flags */
        private const val FLIP_H_VAL = 0x0100 // (1 << 0 << 8)
        private const val FLIP_V_VAL = 0x0200 // (1 << 1 << 8)
        private const val ROT_90_VAL = 0x0400 // (1 << 2 << 8)
        private const val ROT_INVALID_VAL = 0x8000 // (0x80 << 8)
    }
}