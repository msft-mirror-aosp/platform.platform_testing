package com.android.server.wm.flicker.common

class Region(val rect: Rect) {
    constructor(region: Region) : this(Rect(
        region.bounds.left,
        region.bounds.top,
        region.bounds.right,
        region.bounds.bottom))

    constructor(left: Int, top: Int, right: Int, bottom: Int) : this(Rect(left, top, right, bottom))

    val bounds: Rect = rect
    val empty: Boolean = rect.empty
    override fun toString(): String {
        return bounds.toString()
    }
}