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
import com.android.server.wm.traces.common.Region
import com.android.server.wm.traces.common.WindowRect

/**
 * Represents a window in the window manager hierarchy
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot
 * access internal Java/Android functionality
 *
 */
open class WindowState(
    val type: Int,
    val displayId: Int,
    val stackId: Int,
    val layer: Int,
    val isSurfaceShown: Boolean,
    val windowType: Int,
    private val _frame: Rect?,
    private val _containingFrame: Rect?,
    private val _parentFrame: Rect?,
    private val _contentFrame: Rect?,
    private val _contentInsets: Rect?,
    private val _surfaceInsets: Rect?,
    private val _givenContentInsets: Rect?,
    private val _crop: Rect?,
    windowContainer: WindowContainer,
    val isAppWindow: Boolean,
    override val rects: Array<Rect> = arrayOf(WindowRect(_frame ?: Rect(), windowContainer,
        getWindowTitle(windowContainer.title)))
) : WindowContainer(windowContainer, getWindowTitle(windowContainer.title)) {
    override val kind: String = "Window"

    val frame: Rect get() = _frame ?: Rect()
    val containingFrame: Rect get() = _containingFrame ?: Rect()
    val parentFrame: Rect get() = _parentFrame ?: Rect()
    val contentFrame: Rect get() = _contentFrame ?: Rect()
    val contentInsets: Rect get() = _contentInsets ?: Rect()
    val surfaceInsets: Rect get() = _surfaceInsets ?: Rect()
    val givenContentInsets: Rect get() = _givenContentInsets ?: Rect()
    val crop: Rect get() = _crop ?: Rect()

    val isStartingWindow: Boolean = windowType == WINDOW_TYPE_STARTING
    val isExitingWindow: Boolean = windowType == WINDOW_TYPE_EXITING
    val isDebuggerWindow: Boolean = windowType == WINDOW_TYPE_DEBUGGER
    val isValidNavBarType: Boolean = this.type == TYPE_NAVIGATION_BAR

    val frameRegion: Region = Region(frame)

    private fun getWindowTypeSuffix(windowType: Int): String {
        when (windowType) {
            WINDOW_TYPE_STARTING -> return " STARTING"
            WINDOW_TYPE_EXITING -> return " EXITING"
            WINDOW_TYPE_DEBUGGER -> return " DEBUGGER"
            else -> {
            }
        }
        return ""
    }

    override fun toString(): String {
        return "$kind: {$token $title${getWindowTypeSuffix(windowType)}} " +
            "type=$type cf=$containingFrame pf=$parentFrame"
    }

    override fun equals(other: Any?): Boolean {
        return other is WindowState &&
            other.kind == kind &&
            other.type == type &&
            other.token == token &&
            other.title == title &&
            other.containingFrame == containingFrame &&
            other.parentFrame == parentFrame
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + displayId
        result = 31 * result + stackId
        result = 31 * result + layer
        result = 31 * result + isSurfaceShown.hashCode()
        result = 31 * result + windowType
        result = 31 * result + frame.hashCode()
        result = 31 * result + containingFrame.hashCode()
        result = 31 * result + parentFrame.hashCode()
        result = 31 * result + contentFrame.hashCode()
        result = 31 * result + contentInsets.hashCode()
        result = 31 * result + surfaceInsets.hashCode()
        result = 31 * result + givenContentInsets.hashCode()
        result = 31 * result + crop.hashCode()
        result = 31 * result + isAppWindow.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + isStartingWindow.hashCode()
        result = 31 * result + isExitingWindow.hashCode()
        result = 31 * result + isDebuggerWindow.hashCode()
        result = 31 * result + isValidNavBarType.hashCode()
        result = 31 * result + frameRegion.hashCode()
        return result
    }

    companion object {
        internal const val WINDOW_TYPE_STARTING = 1
        internal const val WINDOW_TYPE_EXITING = 2
        private const val WINDOW_TYPE_DEBUGGER = 3

        internal const val STARTING_WINDOW_PREFIX = "Starting "
        internal const val DEBUGGER_WINDOW_PREFIX = "Waiting For Debugger: "

        /**
         * @see WindowManager.LayoutParams
         */
        private const val TYPE_NAVIGATION_BAR = 2019

        private fun getWindowTitle(title: String): String {
            return when {
                // Existing code depends on the prefix being removed
                title.startsWith(STARTING_WINDOW_PREFIX) ->
                    title.substring(STARTING_WINDOW_PREFIX.length)
                title.startsWith(DEBUGGER_WINDOW_PREFIX) ->
                    title.substring(DEBUGGER_WINDOW_PREFIX.length)
                else -> title
            }
        }
    }
}
