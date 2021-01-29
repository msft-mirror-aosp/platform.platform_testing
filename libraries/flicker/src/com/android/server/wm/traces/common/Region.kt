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

package com.android.server.wm.traces.common

class Region(val rect: Rect) {
    constructor(region: Region) : this(Rect(
        region.bounds.left,
        region.bounds.top,
        region.bounds.right,
        region.bounds.bottom))

    constructor(left: Int, top: Int, right: Int, bottom: Int) : this(Rect(left, top, right, bottom))

    constructor() : this(Rect())

    val bounds: Rect = rect
    val isEmpty: Boolean = rect.isEmpty
    override fun toString(): String {
        return bounds.toString()
    }
}