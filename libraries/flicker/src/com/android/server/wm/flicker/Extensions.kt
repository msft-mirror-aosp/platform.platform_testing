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

package com.android.server.wm.flicker

import android.graphics.Rect
import android.graphics.Region
import android.surfaceflinger.nano.Layers.RectProto
import android.surfaceflinger.nano.Layers.RegionProto

internal const val FLICKER_TAG = "FLICKER"

/** Extracts [Rect] from [RectProto].  */
fun RectProto?.extract(): Rect {
    return if (this == null) {
        Rect()
    } else {
        Rect(this.left, this.top, this.right, this.bottom)
    }
}

/**
 * Extracts [Rect] from [RegionProto] by returning a rect that encompasses all
 * the rectangles making up the region.
 */
fun RegionProto.extract(): Region {
    val region = Region()
    for (proto: RectProto in this.rect) {
        region.union(proto.extract())
    }
    return region
}