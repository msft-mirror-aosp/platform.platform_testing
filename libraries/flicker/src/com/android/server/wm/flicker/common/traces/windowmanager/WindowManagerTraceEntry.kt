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

package com.android.server.wm.flicker.common.traces.windowmanager

import com.android.server.wm.flicker.common.AssertionResult
import com.android.server.wm.flicker.common.Region
import com.android.server.wm.flicker.common.traces.ITraceEntry
import com.android.server.wm.flicker.common.traces.windowmanager.windows.ActivityRecord
import com.android.server.wm.flicker.common.traces.windowmanager.windows.WindowContainer
import com.android.server.wm.flicker.common.traces.windowmanager.windows.WindowState

/** Represents a single WindowManager trace entry.  */
open class WindowManagerTraceEntry(val rootWindow: WindowContainer, override val timestamp: Long)
    : ITraceEntry {

    private var _appWindows = mutableSetOf<WindowState>()

    /**
     * Returns the rect of all the frames in the entry
     * Converted to typedArray for better compatibility with JavaScript code
     */
    val rects = rootWindow.rects.toTypedArray()

    /**
     * Returns all windows in the hierarchy
     */
    val windows = getWindows(
            rootWindow,
            isAppWindow = false
    ).toSet()

    /**
     * Return the app windows in the hierarchy.
     * A window is considered an app window if any of its ancestors is an activity record
     */
    val appWindows = _appWindows.toSet()

    /**
     * Returns the non app windows in the hierarchy.
     * A window is considered a non app window if none of its ancestors is an activity record
     */
    val nonAppWindows by lazy {
        windows - appWindows
    }

    /**
     * Returns the non app windows in the hierarchy that appear before the first app window.
     */
    val aboveAppWindows: List<WindowState> by lazy {
        windows.takeWhile { !appWindows.contains(it) }
    }

    /**
     * Returns the non app windows in the hierarchy that appear after the first app window.
     */
    val belowAppWindows: List<WindowState> by lazy {
        windows.dropWhile { !appWindows.contains(it) }
                .drop(appWindows.size)
    }

    /**
     * Returns window title of the top most visible app window.
     */
    val topVisibleAppWindow by lazy {
        appWindows.filter { it.isVisible() }
                .map { it.title }
                .firstOrNull() ?: ""
    }

    /**
     * Returns all visible windows in the hierarchy
     */
    val visibleWindows by lazy {
        windows.filter { it.isVisible() }
    }

    /**
     * Checks if the non-app window with title containing [windowTitle] exists above the app
     * windows and if its visibility is equal to [isVisible]
     *
     * @param windowTitle window title to search
     * @param isVisible if the found window should be visible or not
     */
    fun isAboveAppWindow(windowTitle: String, isVisible: Boolean = true): AssertionResult {
        return aboveAppWindows.isWindowVisible(
                "isAboveAppWindow${if (isVisible) "Visible" else "Invisible"}",
                windowTitle,
                isVisible)
    }

    /**
     * Checks if the non-app window with title containing [windowTitle] exists below the app windows
     * and if its visibility is equal to [isVisible]
     *
     * @param windowTitle window title to search
     * @param isVisible if the found window should be visible or not
     */
    fun isBelowAppWindow(windowTitle: String, isVisible: Boolean = true): AssertionResult {
        return belowAppWindows.isWindowVisible(
                "isBelowAppWindow${if (isVisible) "Visible" else "Invisible"}",
                windowTitle,
                isVisible)
    }

    /**
     * Checks if non-app window with title containing the [windowTitle] exists above or below the
     * app windows and if its visibility is equal to [isVisible]
     *
     * @param windowTitle window title to search
     * @param isVisible if the found window should be visible or not
     */
    fun hasNonAppWindow(windowTitle: String, isVisible: Boolean = true): AssertionResult {
        return nonAppWindows.isWindowVisible(
                "isAppWindowVisible", windowTitle, isVisible)
    }

    /**
     * Checks if app window with title containing the [windowTitle] is on top
     *
     * @param windowTitle window title to search
     */
    fun isVisibleAppWindowOnTop(windowTitle: String): AssertionResult {
        val success = topVisibleAppWindow.contains(windowTitle)
        val reason = "wanted=$windowTitle found=$topVisibleAppWindow"
        return AssertionResult(reason, "isAppWindowOnTop", timestamp, success)
    }

    /**
     * Checks if app window with title containing the [windowTitle] is visible
     *
     * @param windowTitle window title to search
     */
    fun isAppWindowVisible(windowTitle: String): AssertionResult {
        return appWindows.isWindowVisible("isAppWindowVisible",
                windowTitle,
                isVisible = true)
    }

    /**
     * Obtains the region of the first visible window with title containing [windowTitle].
     *
     * @param windowTitle Name of the layer to search
     * @param resultComputation Predicate to compute a result based on the found window's region
     */
    protected fun covers(
        windowTitle: String,
        resultComputation: (Region) -> AssertionResult
    ): AssertionResult {
        val assertionName = "covers"
        val visibilityCheck = windows.isWindowVisible(assertionName, windowTitle)
        if (!visibilityCheck.success) {
            return visibilityCheck
        }

        val foundWindow = windows.first { getWindowByIdentifier(it, windowTitle) != null }
        val foundRegion = foundWindow.frameRegion

        return resultComputation(foundRegion)
    }

    protected fun getWindows(
        windowContainer: WindowContainer,
        isAppWindow: Boolean
    ): List<WindowState> {
        return windowContainer.childrenWindows.flatMap {
            when (it) {
                is ActivityRecord -> {
                    getWindows(it, true)
                }
                is WindowState -> {
                    if (isAppWindow) {
                        _appWindows.add(it)
                    }
                    listOf(it)
                }
                else -> {
                    getWindows(it, isAppWindow)
                }
            }
        }
    }

    /** Checks if non app window with `windowTitle` is visible.  */
    protected fun getWindowByIdentifier(
        windowContainer: WindowContainer,
        windowTitle: String
    ): WindowContainer? {
        return if (windowContainer.title.contains(windowTitle)) {
            windowContainer
        } else windowContainer.childrenWindows
                .firstOrNull { getWindowByIdentifier(it, windowTitle) != null }
    }

    protected fun Collection<WindowState>.isWindowVisible(
        assertionName: String,
        windowTitle: String,
        isVisible: Boolean = true
    ): AssertionResult {
        val foundWindow = this.filter { getWindowByIdentifier(it, windowTitle) != null }
        return when {
            this.isEmpty() -> return AssertionResult(
                    "No windows found",
                    assertionName,
                    timestamp,
                    success = false)
            foundWindow.isEmpty() -> AssertionResult(
                    "$windowTitle cannot be found",
                    assertionName,
                    timestamp,
                    success = false)
            isVisible && foundWindow.none { it.isVisible() } -> AssertionResult(
                    "$windowTitle is invisible",
                    assertionName,
                    timestamp,
                    success = false)
            !isVisible && foundWindow.any { it.isVisible() } -> AssertionResult(
                    "$windowTitle is visible",
                    assertionName,
                    timestamp,
                    success = false)
            else -> {
                val reason = if (isVisible) {
                    "${foundWindow.first { it.isVisible() }.title} is visible"
                } else {
                    "${foundWindow.first { !it.isVisible() }.title} is invisible"
                }
                AssertionResult(
                        success = true,
                        reason = reason)
            }
        }
    }
}