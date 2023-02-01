/*
 * Copyright (C) 2021 The Android Open Source Project
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

import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.Size
import com.android.server.wm.traces.common.withCache
import kotlin.js.JsName

const val BLANK_LAYER_STACK = -1

/** Wrapper for DisplayProto (frameworks/native/services/surfaceflinger/layerproto/display.proto) */
class Display
private constructor(
    @JsName("id") val id: ULong,
    @JsName("name") val name: String,
    @JsName("layerStackId") val layerStackId: Int,
    @JsName("size") val size: Size,
    @JsName("layerStackSpace") val layerStackSpace: Rect,
    @JsName("transform") val transform: Transform,
    @JsName("isVirtual") val isVirtual: Boolean
) {
    @JsName("isOff") val isOff = layerStackId == BLANK_LAYER_STACK

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Display) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (layerStackId != other.layerStackId) return false
        if (size != other.size) return false
        if (layerStackSpace != other.layerStackSpace) return false
        if (transform != other.transform) return false
        if (isVirtual != other.isVirtual) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.toInt()
        result = 31 * result + name.hashCode()
        result = 31 * result + layerStackId
        result = 31 * result + size.hashCode()
        result = 31 * result + layerStackSpace.hashCode()
        result = 31 * result + transform.hashCode()
        result = 31 * result + isVirtual.hashCode()
        return result
    }

    companion object {
        @JsName("EMPTY")
        val EMPTY: Display
            get() = withCache {
                Display(
                    id = 0.toULong(),
                    name = "EMPTY",
                    layerStackId = BLANK_LAYER_STACK,
                    size = Size.EMPTY,
                    layerStackSpace = Rect.EMPTY,
                    transform = Transform.EMPTY,
                    isVirtual = false
                )
            }

        @JsName("from")
        fun from(
            id: ULong,
            name: String,
            layerStackId: Int,
            size: Size,
            layerStackSpace: Rect,
            transform: Transform,
            isVirtual: Boolean
        ): Display = withCache {
            Display(id, name, layerStackId, size, layerStackSpace, transform, isVirtual)
        }
    }
}
