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

package com.android.server.wm.traces.common.windowmanager.windows

import com.android.server.wm.traces.common.Rect

/**
 * Represents WindowContainer classes such as DisplayContent.WindowContainers and
 * DisplayContent.NonAppWindowContainers. This can be expanded into a specific class
 * if we need track and assert some state in the future.
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot
 * access internal Java/Android functionality
 *
 */
open class WindowContainer private constructor(
    val title: String,
    val token: String,
    val orientation: Int,
    val isVisible: Boolean,
    configurationContainer: ConfigurationContainer,
    protected var childContainers: Array<WindowContainerChild> = emptyArray()
) : ConfigurationContainer(configurationContainer) {
    protected constructor(
        windowContainer: WindowContainer,
        titleOverride: String? = null,
        isVisibleOverride: Boolean? = null
    ) : this(
        titleOverride ?: windowContainer.title,
        windowContainer.token,
        windowContainer.orientation,
        isVisibleOverride ?: windowContainer.isVisible,
        windowContainer,
        windowContainer.childContainers
    )

    constructor(
        name: String,
        token: String,
        orientation: Int,
        isVisible: Boolean,
        configurationContainer: ConfigurationContainer
    ) : this(name, token, orientation, isVisible, configurationContainer,
        childContainers = emptyArray())

    fun addChildrenWindows(children: List<WindowContainerChild>) {
        childContainers = children.toMutableList().also {
            it.addAll(childContainers)
        }.toTypedArray()
    }

    open val name: String = title
    open val kind: String = "WindowContainer"
    open val stableId: String by lazy { kind + token }

    val childrenWindows: Array<WindowContainer>
        by lazy { childContainers.mapNotNull { it.getContainer() }.toTypedArray() }

    open val rects: Array<Rect>
        by lazy { childrenWindows.flatMap { it.rects.toList() }.toTypedArray() }
    open val isFullscreen: Boolean = false
    open val bounds: Rect = Rect()
    protected open val _subWindows = mutableListOf<WindowState>()

    val windows: Array<WindowState>
        get() = _subWindows.toTypedArray()

    fun traverseTopDown(): List<WindowContainer> {
        val traverseList = mutableListOf(this)

        this.childrenWindows.reversed()
            .forEach { childLayer ->
                traverseList.addAll(childLayer.traverseTopDown())
            }

        return traverseList
    }

    /**
     * For a given WindowContainer, traverse down the hierarchy and collect all children of type
     * [T] if the child passes the test [predicate].
     *
     * @param predicate Filter function
     */
    inline fun <reified T : WindowContainer> collectDescendants(
        predicate: (T) -> Boolean = { true }
    ): Array<T> {
        val traverseList = traverseTopDown()

        return traverseList.filterIsInstance<T>()
            .filter { predicate(it) }
            .toTypedArray()
    }

    override fun toString(): String {
        if (this.title.isEmpty() || listOf("WindowContainer", "Task")
                .any { it.contains(this.title) }) {
            return ""
        }

        return "$${removeRedundancyInName(this.title)}@${this.token}"
    }

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

    override val isEmpty: Boolean
        get() = super.isEmpty &&
            title.isEmpty() &&
            token.isEmpty()
}
