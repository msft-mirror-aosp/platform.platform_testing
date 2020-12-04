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

package com.android.server.wm.traces.common

open class Rect(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val height: Int get() = bottom - top
    val width: Int get() = right - left
    fun centerX(): Int = left + right / 2
    fun centerY(): Int = top + bottom / 2
    /**
     * Returns true if the rectangle is empty (left >= right or top >= bottom)
     */
    val isEmpty: Boolean = width == 0 || height == 0

    val isNotEmpty = !isEmpty

    override fun toString(): String {
        return if (isEmpty) {
            "[empty]"
        } else {
            "[$left, $top, $right, $bottom]"
        }
    }
}