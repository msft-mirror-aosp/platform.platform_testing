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

data class RectF(
    var left: Float = 0f,
    var top: Float = 0f,
    var right: Float = 0f,
    var bottom: Float = 0f
) {
    val height: Float get() = bottom - top
    val width: Float get() = right - left

    /**
     * Returns true if the rectangle is empty (left >= right or top >= bottom)
     */
    val isEmpty: Boolean
        get() = width == 0f || height == 0f

    /**
     * Returns true iff the specified rectangle r is inside or equal to this
     * rectangle. An empty rectangle never contains another rectangle.
     *
     * @param r The rectangle being tested for containment.
     * @return true iff the specified rectangle r is inside or equal to this
     *              rectangle
     */
    operator fun contains(r: RectF): Boolean {
        // check for empty first
        return this.left < this.right && this.top < this.bottom && // now check for containment
                left <= r.left && top <= r.top && right >= r.right && bottom >= r.bottom
    }

    /**
     * If the rectangle specified by left,top,right,bottom intersects this
     * rectangle, return true and set this rectangle to that intersection,
     * otherwise return false and do not change this rectangle. No check is
     * performed to see if either rectangle is empty. Note: To just test for
     * intersection, use intersects()
     *
     * @param left The left side of the rectangle being intersected with this
     * rectangle
     * @param top The top of the rectangle being intersected with this rectangle
     * @param right The right side of the rectangle being intersected with this
     * rectangle.
     * @param bottom The bottom of the rectangle being intersected with this
     * rectangle.
     * @return true if the specified rectangle and this rectangle intersect
     * (and this rectangle is then set to that intersection) else
     * return false and do not change this rectangle.
     */
    fun intersect(left: Float, top: Float, right: Float, bottom: Float): Boolean {
        if (this.left < right && left < this.right && this.top < bottom && top < this.bottom) {
            if (this.left < left) {
                this.left = left
            }
            if (this.top < top) {
                this.top = top
            }
            if (this.right > right) {
                this.right = right
            }
            if (this.bottom > bottom) {
                this.bottom = bottom
            }
            return true
        }
        return false
    }

    /**
     * If the specified rectangle intersects this rectangle, return true and set
     * this rectangle to that intersection, otherwise return false and do not
     * change this rectangle. No check is performed to see if either rectangle
     * is empty. To just test for intersection, use intersects()
     *
     * @param r The rectangle being intersected with this rectangle.
     * @return true if the specified rectangle and this rectangle intersect
     * (and this rectangle is then set to that intersection) else
     * return false and do not change this rectangle.
     */
    fun intersect(r: RectF): Boolean {
        return intersect(r.left, r.top, r.right, r.bottom)
    }
}
