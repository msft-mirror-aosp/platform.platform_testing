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

import com.android.server.wm.nano.WindowContainerChildProto
import com.android.server.wm.nano.WindowContainerProto
import com.android.server.wm.nano.WindowManagerTraceProto
import com.android.server.wm.nano.WindowStateProto

/** Represents a single WindowManager trace entry.  */
class WindowManagerTraceEntry(val proto: WindowManagerTraceProto) : ITraceEntry {
    override val timestamp by lazy {
        proto.elapsedRealtimeNanos
    }

    /** Returns window title of the top most visible app window.  */
    val topVisibleAppWindow by lazy {
        windows.filter { it.windowContainer.visible }
                .map { it.title }
                .firstOrNull() ?: ""
    }

    val windows by lazy {
        getWindows(proto.windowManagerService.rootWindowContainer.windowContainer)
    }

    val visibleWindows by lazy {
        windows.filter { it.isVisible() }
    }

    private fun getWindows(windowContainer: WindowContainerProto): List<WindowStateProto> {
        return windowContainer.children.flatMap { getWindows(it) }
    }

    private fun getWindows(windowContainer: WindowContainerChildProto): List<WindowStateProto> {
        return if (windowContainer.displayArea != null) {
            getWindows(windowContainer.displayArea.windowContainer)
        } else if (windowContainer.displayContent != null
                && windowContainer.displayContent.windowContainer == null) {
            getWindows(windowContainer.displayContent.rootDisplayArea.windowContainer)
        } else if (windowContainer.displayContent != null) {
            getWindows(windowContainer.displayContent.windowContainer)
        } else if (windowContainer.task != null) {
            getWindows(windowContainer.task.windowContainer)
        } else if (windowContainer.activity != null) {
            getWindows(windowContainer.activity.windowToken.windowContainer)
        } else if (windowContainer.windowToken != null) {
            getWindows(windowContainer.windowToken.windowContainer)
        } else if (windowContainer.window != null) {
            listOf(windowContainer.window)
        } else {
            getWindows(windowContainer.windowContainer)
        }
    }

    /** Checks if non app window with `windowTitle` is visible.  */
    private fun getNonAppWindowByIdentifier(
        windowState: WindowStateProto,
        windowTitle: String
    ): WindowStateProto? {
        return if (windowState.title.contains(windowTitle)) {
            windowState
        } else windowState.childWindows
                .firstOrNull { getNonAppWindowByIdentifier(it, windowTitle) != null }
    }

    /** Checks if non app window with `windowTitle` is visible.  */
    fun isNonAppWindowVisible(windowTitle: String): AssertionResult {
        val assertionName = "isAppWindowVisible"
        val foundWindow = windows
                .firstOrNull { getNonAppWindowByIdentifier(it, windowTitle) != null }
        return when {
            windows.isEmpty() -> return AssertionResult(
                    "No windows found",
                    assertionName,
                    timestamp,
                    success = false)
            foundWindow == null -> AssertionResult(
                    "$windowTitle cannot be found",
                    assertionName,
                    timestamp,
                    success = false)
            !foundWindow.isVisible() -> AssertionResult(
                    "$windowTitle is invisible",
                    assertionName,
                    timestamp,
                    success = false)
            else -> AssertionResult(
                    success = true,
                    reason = foundWindow.title + " is visible")
        }
    }

    /** Checks if app window with `windowTitle` is on top.  */
    fun isVisibleAppWindowOnTop(windowTitle: String): AssertionResult {
        val success = topVisibleAppWindow.contains(windowTitle)
        val reason = "wanted=$windowTitle found=$topVisibleAppWindow"
        return AssertionResult(reason, "isAppWindowOnTop", timestamp, success)
    }

    /** Checks if app window with `windowTitle` is visible.  */
    fun isAppWindowVisible(windowTitle: String): AssertionResult {
        val assertionName = "isAppWindowVisible"
        val foundWindow = windows.firstOrNull {
            it.title.contains(windowTitle) && it.windowContainer.visible
        }

        return when {
            windows.isEmpty() -> AssertionResult(
                    "No windows found",
                    assertionName,
                    timestamp,
                    success = false)
            foundWindow == null -> AssertionResult(
                    "Window $windowTitle cannot be found",
                    assertionName,
                    timestamp,
                    success = false)
            !foundWindow.isVisible -> AssertionResult(
                    "Window $windowTitle is invisible",
                    assertionName,
                    timestamp,
                    success = false)
            else -> AssertionResult(
                    "Window " + foundWindow.title + "is visible",
                    assertionName,
                    timestamp,
                    success = true)
        }
    }

    private fun WindowStateProto.isVisible(): Boolean = this.windowContainer.visible

    private val WindowStateProto.title: String
        get() = this.windowContainer.identifier.title
}