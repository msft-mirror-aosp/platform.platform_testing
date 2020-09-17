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

import com.android.server.wm.nano.RootWindowContainerProto
import com.android.server.wm.flicker.common.AssertionResult
import com.android.server.wm.flicker.common.Bounds
import com.android.server.wm.flicker.common.Rect
import com.android.server.wm.flicker.common.Region
import com.android.server.wm.flicker.common.traces.windowmanager.windows.Task
import com.android.server.wm.flicker.common.traces.windowmanager.windows.WindowContainer
import com.android.server.wm.flicker.common.traces.windowmanager.windows.ActivityRecord
import com.android.server.wm.flicker.common.traces.windowmanager.windows.WindowToken
import com.android.server.wm.flicker.common.traces.windowmanager.windows.WindowState
import com.android.server.wm.flicker.common.traces.windowmanager.windows.DisplayContent
import com.android.server.wm.flicker.common.traces.windowmanager.windows.RootDisplayArea
import com.android.server.wm.flicker.common.traces.windowmanager.windows.DisplayArea
import com.android.server.wm.nano.WindowContainerChildProto
import com.android.server.wm.nano.WindowContainerProto
import com.android.server.wm.nano.WindowManagerTraceProto
import com.android.server.wm.nano.WindowStateProto
import java.lang.Exception

/** Represents a single WindowManager trace entry.  */
class WindowManagerTraceEntry(rootWindow: WindowContainer, timestamp: Long) :
        com.android.server.wm.flicker.common.traces.windowmanager
        .WindowManagerTraceEntry(rootWindow, timestamp) {

    /**
     * Create a WindowManagerTraceEntry directly from the proto
     */
    constructor(proto: WindowManagerTraceProto) :
            this(rootWindow = transformWindowManagerTraceProto(proto),
                    timestamp = proto.elapsedRealtimeNanos)

    constructor(rootWindowContainer: RootWindowContainerProto, timestamp: Long) :
            this(rootWindow = transformWindowContainerProto(rootWindowContainer.windowContainer),
                    timestamp = timestamp)

    /**
     * Checks if the first window with title containing [windowTitle] has a visible area of at
     * least [testRegion], that is, if its area of the window frame covers each point in
     * the region.
     *
     * @param windowTitle Name of the layer to search
     * @param testRegion Expected visible area of the window
     */
    fun coversAtLeastRegion(windowTitle: String, testRegion: Region): AssertionResult {
        return coversAtLeastRegion(windowTitle, testRegion.toAndroidRegion())
    }

    /**
     * Checks if the first window with title containing [windowTitle] has a visible area of at
     * least [testRegion], that is, if its area of the window frame covers each point in
     * the region.
     *
     * @param windowTitle Name of the layer to search
     * @param testRegion Expected visible area of the window
     */
    fun coversAtLeastRegion(windowTitle: String, testRegion: android.graphics.Region):
            AssertionResult {
        return covers(windowTitle) { windowRegion ->
            val testRect = testRegion.bounds
            val intersection = windowRegion.toAndroidRegion()
            val covers = intersection.op(testRect, android.graphics.Region.Op.INTERSECT) &&
                    !intersection.op(testRect, android.graphics.Region.Op.XOR)

            val reason = if (covers) {
                "$windowTitle covers region $testRegion"
            } else {
                "Region to test: $testRegion\nUncovered region: $intersection"
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
        return coversAtMostRegion(windowTitle, testRegion.toAndroidRegion())
    }

    /**
     * Checks if the first window with title containing [windowTitle] has a visible area of at
     * most [testRegion], that is, if the region covers each point in the window frame.
     *
     * @param windowTitle Name of the layer to search
     * @param testRegion Expected visible area of the window
     */
    fun coversAtMostRegion(windowTitle: String, testRegion: android.graphics.Region):
            AssertionResult {
        return covers(windowTitle) { windowRegion ->
            val testRect = testRegion.bounds
            val intersection = windowRegion.toAndroidRegion()
            val covers = intersection.op(testRect, android.graphics.Region.Op.INTERSECT) &&
                    !intersection.op(windowRegion.toAndroidRegion(), android.graphics.Region.Op.XOR)

            val reason = if (covers) {
                "$windowTitle covers region $testRegion"
            } else {
                "Region to test: $testRegion\nOut-of-bounds region: $intersection"
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
                if (ourRegion.toAndroidRegion().op(otherRegion.toAndroidRegion(),
                                android.graphics.Region.Op.INTERSECT)) {
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

    companion object {
        /**
         * Transforms WindowManagerTraceProto into our internal representation of a Window hierarchy
         * @return the root WindowContainer of the hierarchy
         */
        private fun transformWindowManagerTraceProto(
            proto: WindowManagerTraceProto
        ): WindowContainer {
            return transformWindowContainerProto(
                    proto.windowManagerService.rootWindowContainer.windowContainer)
        }

        private fun transformWindowContainerChildProto(
            windowContainerChild: WindowContainerChildProto
        ): WindowContainer {
            return if (windowContainerChild.displayArea != null) {
                val windowContainer = transformWindowContainerProto(
                        windowContainerChild.displayArea.windowContainer)
                DisplayArea(windowContainer)
            } else if (windowContainerChild.displayContent != null &&
                    windowContainerChild.displayContent.windowContainer == null) {
                val windowContainer = transformWindowContainerProto(
                        windowContainerChild.displayContent.rootDisplayArea.windowContainer)
                val displayArea = DisplayArea(windowContainer)
                RootDisplayArea(displayArea)
            } else if (windowContainerChild.displayContent != null) {
                val windowContainer = transformWindowContainerProto(
                        windowContainerChild.displayContent.windowContainer)
                val displayArea = DisplayArea(windowContainer)
                val rootDisplayArea = RootDisplayArea(displayArea)
                val bound = Bounds(
                    windowContainerChild.displayContent.displayInfo.logicalWidth,
                    windowContainerChild.displayContent.displayInfo.logicalHeight
                )
                DisplayContent(rootDisplayArea, bound)
            } else if (windowContainerChild.task != null) {
                val windowContainer = transformWindowContainerProto(
                        windowContainerChild.task.windowContainer)
                Task(windowContainer)
            } else if (windowContainerChild.activity != null) {
                val windowContainer = transformWindowContainerProto(
                        windowContainerChild.activity.windowToken.windowContainer)
                val windowToken = WindowToken(windowContainer)
                ActivityRecord(windowToken)
            } else if (windowContainerChild.windowToken != null) {
                val windowContainer = transformWindowContainerProto(
                        windowContainerChild.windowToken.windowContainer)
                WindowToken(windowContainer)
            } else if (windowContainerChild.window != null) {
                // Base case
                transformWindowStateProto(windowContainerChild.window)
            } else if (windowContainerChild.windowContainer != null) {
                // We do not know the derived type use generic WindowContainer
                transformWindowContainerProto(windowContainerChild.windowContainer)
            } else {
                throw Exception("Unhandled WindowContainerChildProto case...")
            }
        }

        private fun transformWindowStateProto(proto: WindowStateProto): WindowState {
            val windowContainer = transformWindowContainerProto(proto.windowContainer)
            val childWindows: Array<WindowState> = proto.childWindows.map {
                transformWindowStateProto(it)
            }.toTypedArray()
            val frameProto = if (proto.windowFrames != null) {
                proto.windowFrames.frame
            } else {
                proto.frame
            }

            val frame = if (frameProto == null) {
                Rect(0, 0, 0, 0)
            } else {
                Rect(frameProto.left, frameProto.top, frameProto.right, frameProto.bottom)
            }

            return WindowState(windowContainer, childWindows, frame)
        }

        private fun transformWindowContainerProto(windowContainer: WindowContainerProto):
                WindowContainer {
            val children = windowContainer.children.map {
                child: WindowContainerChildProto -> transformWindowContainerChildProto(child)
            }.toTypedArray()
            val title = windowContainer.identifier.title
            val hashCode = windowContainer.identifier.hashCode
            val visible = windowContainer.visible

            return WindowContainer(
                    children,
                    title,
                    hashCode,
                    visible
            )
        }
    }

    private fun Region.toAndroidRegion(): android.graphics.Region {
        return android.graphics.Region(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

    private fun Rect.toAndroidRect(): android.graphics.Rect {
        return android.graphics.Rect(left, top, right, bottom)
    }
}
