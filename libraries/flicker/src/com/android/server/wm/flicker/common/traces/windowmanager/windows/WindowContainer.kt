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

package com.android.server.wm.flicker.common.traces.windowmanager.windows

import com.android.server.wm.flicker.common.Rect

open class WindowContainer(
    val childrenArray: Array<WindowContainer>,
    val title: String,
    val windowHashCode: Int,
    val visible: Boolean
) {

    constructor(windowContainer: WindowContainer) : this(
        windowContainer.childrenArray,
        windowContainer.title,
        windowContainer.windowHashCode, windowContainer.visible
    )

    fun isVisible(): Boolean = visible

    val childrenWindows: Array<WindowContainer> = childrenArray.reversed().toTypedArray()

    open val rects: List<Rect> = childrenWindows.flatMap { it.rects }

    private fun removeRedundancyInName(name: String): String {
        if (!name.contains('/')) {
            return name
        }

        val split = name.split('/')
        val pkg = split[0]
        var clazz = split.slice(1..split.lastIndex).joinToString("/")

        if (clazz.startsWith("$pkg.")) {
            clazz = clazz.slice(pkg.length + 1..clazz.lastIndex)

            return "$pkg/$clazz"
        }

        return name
    }

    private fun shortenName(name: String): String {
        val classParts = name.split(".")

        if (classParts.size <= 3) {
            return name
        }

        val className = classParts.last()

        return "${classParts[0]}.${classParts[1]}.(...).$className"
    }
}