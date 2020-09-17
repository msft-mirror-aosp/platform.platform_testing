package com.android.server.wm.flicker.common

open class Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    /**
     * Returns true if the rectangle is empty (left >= right or top >= bottom)
     */
    val empty: Boolean = left >= right || top >= bottom
}