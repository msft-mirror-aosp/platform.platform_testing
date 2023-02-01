/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.wm.flicker.utils

import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.Size
import com.android.server.wm.traces.common.layers.Display
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.layers.Transform

class MockLayerTraceEntryBuilder() {
    private val displays = mutableListOf<Display>()
    private val layers = mutableListOf<Layer>()
    private val bounds = Rect.from(0, 0, 1080, 1920)
    var timestamp = -1L
        private set

    constructor(timestamp: Long) : this() {
        setTimestamp(timestamp)
    }

    init {
        if (timestamp <= 0L) {
            timestamp = ++lastTimestamp
        }
    }

    fun addDisplay(rootLayers: List<MockLayerBuilder>): MockLayerTraceEntryBuilder = apply {
        val displayLayer =
            MockLayerBuilder("Display").setAbsoluteBounds(bounds).addChildren(rootLayers).build()
        val displayId = 1UL
        val stackId = 1
        this.displays.add(
            Display.from(
                id = displayId,
                name = "Display",
                layerStackId = stackId,
                size = Size.from(bounds.width, bounds.height),
                layerStackSpace = bounds,
                transform = Transform.EMPTY,
                isVirtual = false
            )
        )
        this.layers.add(displayLayer)
    }

    fun setTimestamp(timestamp: Long): MockLayerTraceEntryBuilder = apply {
        require(timestamp > 0) { "Timestamp must be a positive value." }
        this.timestamp = timestamp
        lastTimestamp = timestamp
    }

    fun build(): LayerTraceEntry {
        return LayerTraceEntry(
            elapsedTimestamp = timestamp,
            clockTimestamp = null,
            hwcBlob = "",
            where = "",
            displays = displays.toTypedArray(),
            vSyncId = 100,
            _rootLayers = layers.toTypedArray()
        )
    }

    companion object {
        private var lastTimestamp = 1L
    }
}
