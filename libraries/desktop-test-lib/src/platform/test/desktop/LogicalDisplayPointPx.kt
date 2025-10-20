/*
 * Copyright 2025 The Android Open Source Project
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
package platform.test.desktop

import android.graphics.Point
import android.graphics.PointF
import kotlin.math.roundToInt

/** Represents a logical-display-space coordinate (PX) on a particular display */
data class LogicalDisplayPointPx(val displayId: Int, val x: Float, val y: Float) {
    constructor(displayId: Int, x: Int, y: Int) : this(displayId, x.toFloat(), y.toFloat())

    constructor(displayId: Int, p: Point) : this(displayId, p.x.toFloat(), p.y.toFloat())

    fun getPoint() = Point(this.x.roundToInt(), this.y.roundToInt())

    fun getPointF() = PointF(this.x, this.y)
}
