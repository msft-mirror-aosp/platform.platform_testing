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

package com.android.server.wm.flicker.traces.windowmanager

import android.graphics.Region
import android.graphics.nano.RectProto
import com.android.server.wm.flicker.assertions.AssertionResult
import com.android.server.wm.flicker.traces.ITraceEntry
import com.android.server.wm.nano.WindowContainerChildProto
import com.android.server.wm.nano.WindowContainerProto
import com.android.server.wm.nano.WindowManagerTraceProto
import com.android.server.wm.nano.WindowStateProto

/** Represents a single WindowManager trace entry.  */
class WindowManagerTraceEntry(val proto: WindowManagerTraceProto) : ITraceEntry {
    private var _appWindows = mutableSetOf<WindowStateProto>()

    override val timestamp by lazy {
        proto.elapsedRealtimeNanos
    }

    /**
     * Returns all windows in the hierarchy
     */
    val windows = getWindows(
            proto.windowManagerService.rootWindowContainer.windowContainer,
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
    val aboveAppWindows: List<WindowStateProto> by lazy {
        windows.takeWhile { !appWindows.contains(it) }
    }

    /**
     * Returns the non app windows in the hierarchy that appear after the first app window.
     */
    val belowAppWindows: List<WindowStateProto> by lazy {
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

    private fun getWindows(windowContainer: WindowContainerProto, isAppWindow: Boolean)
            = windowContainer.children.reversed().flatMap { getWindows(it, isAppWindow) }

    private fun getWindows(
            windowContainer: WindowContainerChildProto,
            isAppWindow: Boolean
    ): List<WindowStateProto> {
        return if (windowContainer.displayArea != null) {
            getWindows(windowContainer.displayArea.windowContainer, isAppWindow)
        } else if (windowContainer.displayContent != null
                && windowContainer.displayContent.windowContainer == null) {
            getWindows(windowContainer.displayContent.rootDisplayArea.windowContainer, isAppWindow)
        } else if (windowContainer.displayContent != null) {
            getWindows(windowContainer.displayContent.windowContainer, isAppWindow)
        } else if (windowContainer.task != null) {
            getWindows(windowContainer.task.windowContainer, isAppWindow)
        } else if (windowContainer.activity != null) {
            getWindows(windowContainer.activity.windowToken.windowContainer, true)
        } else if (windowContainer.windowToken != null) {
            getWindows(windowContainer.windowToken.windowContainer, isAppWindow)
        } else if (windowContainer.window != null) {
            if (isAppWindow) {
                _appWindows.add(windowContainer.window)
            }
            listOf(windowContainer.window)
        } else {
            getWindows(windowContainer.windowContainer, isAppWindow)
        }
    }

    /** Checks if non app window with `windowTitle` is visible.  */
    private fun getWindowByIdentifier(
        windowState: WindowStateProto,
        windowTitle: String
    ): WindowStateProto? {
        return if (windowState.title.contains(windowTitle)) {
            windowState
        } else windowState.childWindows
                .firstOrNull { getWindowByIdentifier(it, windowTitle) != null }
    }

    private fun Collection<WindowStateProto>.isWindowVisible(
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

    /**
     * Checks if the non-app window with title containing [windowTitle] exists above the app
     * windows and if its visibility is equal to [isVisible]
     *
     * @param windowTitle window title to search
     * @param isVisible if the found window should be visible or not
     */
    @JvmOverloads
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
    @JvmOverloads
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
    @JvmOverloads
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
    private fun covers(
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

    /**
     * Checks if the first window with title containing [windowTitle] has a visible area of at
     * least [testRegion], that is, if its area of the window frame covers each point in
     * the region.
     *
     * @param windowTitle Name of the layer to search
     * @param testRegion Expected visible area of the window
     */
    fun coversAtLeastRegion(windowTitle: String, testRegion: Region): AssertionResult {
        return covers(windowTitle) { windowRegion ->
            val testRect = testRegion.bounds
            val intersection = Region(windowRegion)
            val covers = intersection.op(testRect, Region.Op.INTERSECT)
                    && !intersection.op(testRect, Region.Op.XOR)

            val reason = if (covers) {
                "$windowTitle covers region $testRegion"
            } else {
                ("Region to test: $testRegion"
                        + "\nUncovered region: $intersection")
            }

            AssertionResult(reason, "coversAtLeastRegion", timestamp, success = covers)
        }
    }

    /**
     * Checks if the first window with title containing [windowTitle] has a visible area of at
     * most [testRegion], that is, if the region covers each point in the window frame.
     *
     * @param windowTitle Name of the layer to search
     * @param testRegion Expected visible area of the window
     */
    fun coversAtMostRegion(windowTitle: String, testRegion: Region): AssertionResult {
        return covers(windowTitle) { windowRegion ->
            val testRect = testRegion.bounds
            val intersection = Region(windowRegion)
            val covers = intersection.op(testRect, Region.Op.INTERSECT)
                    && !intersection.op(windowRegion, Region.Op.XOR)

            val reason = if (covers) {
                "$windowTitle covers region $testRegion"
            } else {
                ("Region to test: $testRegion"
                        + "\nOut-of-bounds region: $intersection")
            }

            AssertionResult(reason, "coversAtMostRegion", timestamp, success = covers)
        }
    }

    /** Checks if any of the given windows overlap with each other. */
    fun noWindowsOverlap(partialWindowTitles: Set<String>): AssertionResult {
        val foundWindows = partialWindowTitles.associateWith { title ->
            windows.find { getWindowByIdentifier(it, title) != null }
        }
        // keep entries only for windows that we actually found by removing nulls
        .filterValues { it != null }
        .mapValues { (_, v) -> v!!.frameRegion }

        val assertionName = "noWindowsOverlap"

        // ensure we found all required windows
        if (foundWindows.size < partialWindowTitles.size) {
            val notFound = partialWindowTitles - foundWindows.keys
            return AssertionResult(
                    reason = "Could not find windows containing: [${notFound.joinToString(", ")}]",
                    assertionName = assertionName,
                    timestamp = timestamp,
                    success = false
            )
        }

        val regions = foundWindows.entries.toList()
        for (i in regions.indices) {
            val (ourTitle, ourRegion) = regions[i]
            for (j in i + 1 until regions.size) {
                val (otherTitle, otherRegion) = regions[j]
                if (Region(ourRegion).op(otherRegion, Region.Op.INTERSECT)) {
                    return AssertionResult(
                            reason = "At least two windows overlap: $ourTitle, $otherTitle",
                            assertionName = assertionName,
                            timestamp = timestamp,
                            success = false
                    )
                }
            }
        }

        return AssertionResult("No windows overlap", assertionName, timestamp, success = true)
    }

    /** Check if the window named [aboveWindowTitle] is above the one named [belowWindowTitle]. */
    fun isAboveWindow(aboveWindowTitle: String, belowWindowTitle: String): AssertionResult {
        val assertionName = "isAboveWindow"

        // windows are ordered by z-order, from top to bottom
        val aboveZ = windows.indexOfFirst { aboveWindowTitle in it.title }
        val belowZ = windows.indexOfFirst { belowWindowTitle in it.title }

        val notFound = mutableSetOf<String>().apply {
            if (aboveZ == -1) {
                add(aboveWindowTitle)
            }
            if (belowZ == -1) {
                add(belowWindowTitle)
            }
        }

        if (notFound.isNotEmpty()) {
            return AssertionResult(
                reason = "Could not find ${notFound.joinToString(" and ")}!",
                assertionName = assertionName,
                timestamp = timestamp,
                success = false
            )
        }

        // ensure the z-order
        return AssertionResult(
            reason = "$aboveWindowTitle is above $belowWindowTitle",
            assertionName = assertionName,
            timestamp = timestamp,
            success = aboveZ < belowZ
        )
    }

    private fun WindowStateProto.isVisible(): Boolean = this.windowContainer.visible

    private val WindowStateProto.title: String
        get() = this.windowContainer.identifier.title

    private val WindowStateProto.frameRegion: Region
        get() = if (this.windowFrames != null) {
            this.windowFrames.frame.extract()
        } else {
            this.frame.extract()
        }

    private fun RectProto?.extract(): Region {
        return if (this == null) {
            Region()
        } else {
            Region(this.left, this.top, this.right, this.bottom)
        }
    }

}
