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

data class Color(val r: Float, val g: Float, val b: Float, val a: Float) {
    val isEmpty: Boolean
        get() = a == 0f || r < 0 || g < 0 || b < 0

    val isNotEmpty: Boolean
        get() = !isEmpty

    companion object {
        val EMPTY = Color(r = -1f, g = -1f, b = -1f, a = 0f)
    }
}