package com.android.server.wm.flicker.common

class RectF {
    var left = 0f
    var top = 0f
    var right = 0f
    var bottom = 0f

    /**
     * Returns true if the rectangle is empty (left >= right or top >= bottom)
     */
    val empty: Boolean
        get() = left >= right || top >= bottom

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
