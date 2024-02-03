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

package android.tools.common.datatypes

import android.tools.common.FloatFormatter
import android.tools.common.withCache
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Representation of a matrix 3x3 used for layer transforms
 *
 * ```
 *          |dsdx dsdy  tx|
 * matrix = |dtdx dtdy ty|
 *          |0    0     1 |
 * ```
 */
@JsExport
class Matrix33
private constructor(
    @JsName("dsdx") val dsdx: Float,
    @JsName("dtdx") val dtdx: Float,
    @JsName("tx") val tx: Float = 0F,
    @JsName("dsdy") val dsdy: Float,
    @JsName("dtdy") val dtdy: Float,
    @JsName("ty") val ty: Float = 0F
) : DataType() {
    override val isEmpty =
        dsdx == 0f && dtdx == 0f && tx == 0f && dsdy == 0f && dtdy == 0f && ty == 0f

    override fun doPrintValue() = buildString {
        append("dsdx:${FloatFormatter.format(dsdx)}   ")
        append("dtdx:${FloatFormatter.format(dtdx)}   ")
        append("dsdy:${FloatFormatter.format(dsdy)}   ")
        append("dtdy:${FloatFormatter.format(dtdy)}   ")
        append("tx:${FloatFormatter.format(tx)}   ")
        append("ty:${FloatFormatter.format(ty)}")
    }

    companion object {
        val EMPTY: Matrix33
            get() = withCache { from(dsdx = 0f, dtdx = 0f, tx = 0f, dsdy = 0f, dtdy = 0f, ty = 0f) }

        @JsName("identity")
        fun identity(x: Float, y: Float): Matrix33 = withCache {
            from(dsdx = 1f, dtdx = 0f, x, dsdy = 0f, dtdy = 1f, y)
        }

        @JsName("rot270")
        fun rot270(x: Float, y: Float): Matrix33 = withCache {
            from(dsdx = 0f, dtdx = -1f, x, dsdy = 1f, dtdy = 0f, y)
        }

        @JsName("rot180")
        fun rot180(x: Float, y: Float): Matrix33 = withCache {
            from(dsdx = -1f, dtdx = 0f, x, dsdy = 0f, dtdy = -1f, y)
        }

        @JsName("rot90")
        fun rot90(x: Float, y: Float): Matrix33 = withCache {
            from(dsdx = 0f, dtdx = 1f, x, dsdy = -1f, dtdy = 0f, y)
        }

        @JsName("from")
        fun from(
            dsdx: Float,
            dtdx: Float,
            tx: Float,
            dsdy: Float,
            dtdy: Float,
            ty: Float
        ): Matrix33 = withCache { Matrix33(dsdx, dtdx, tx, dsdy, dtdy, ty) }
    }
}
