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

package com.android.server.wm.traces.common

import kotlin.js.JsName

/**
 * Wrapper for PositionProto (frameworks/native/services/surfaceflinger/layerproto/layers.proto)
 *
 * This class is used by flicker and Winscope
 */
class PointF private constructor(val x: Float = 0f, val y: Float = 0f) {
    @JsName("prettyPrint")
    fun prettyPrint(): String = "(${FloatFormatter.format(x)}, ${FloatFormatter.format(y)})"

    override fun toString(): String = prettyPrint()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointF) return false

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    companion object {
        @JsName("EMPTY")
        val EMPTY: PointF
            get() = withCache { PointF() }

        @JsName("from") fun from(x: Float, y: Float): PointF = withCache { PointF(x, y) }
    }
}
