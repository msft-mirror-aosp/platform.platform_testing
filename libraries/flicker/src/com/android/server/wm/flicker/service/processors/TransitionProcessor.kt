/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker.service.processors

import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.flicker.service.ITagProcessor
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.TagState
import com.android.server.wm.traces.common.tags.TagTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper

abstract class TransitionProcessor : ITagProcessor {
    abstract fun getInitialState(tags: MutableMap<Long, MutableList<Tag>>): FSMState

    /**
     * Add the start and end tags corresponding to the transition from
     * the WindowManager and SurfaceFlinger traces
     * @param wmTrace - WindowManager trace
     * @param layersTrace - SurfaceFlinger trace
     * @return [TagTrace] - containing all the newly generated tags in states with
     * timestamps
     */
    override fun generateTags(
        wmTrace: WindowManagerTrace,
        layersTrace: LayersTrace
    ): TagTrace {
        val tags = mutableMapOf<Long, MutableList<Tag>>()
        var currPosition: FSMState? = getInitialState(tags)

        val dumpList = createDumpList(wmTrace, layersTrace)
        val dumpIterator = dumpList.iterator()

        // keep always a reference to previous, current and next states
        var previous: WindowManagerStateHelper.Dump?
        var current: WindowManagerStateHelper.Dump? = null
        var next: WindowManagerStateHelper.Dump? = dumpIterator.next()
        while (currPosition != null) {
            previous = current
            current = next
            next = if (dumpIterator.hasNext()) dumpIterator.next() else null
            requireNotNull(current) { "Current state shouldn't be null" }
            val newPosition = currPosition.process(previous, current, next)
            currPosition = newPosition
        }

        return buildTagTrace(tags)
    }

    private fun buildTagTrace(tags: MutableMap<Long, MutableList<Tag>>): TagTrace {
        val tagStates = tags.map { entry ->
            val timestamp = entry.key
            val stateTags = entry.value
            TagState(timestamp, stateTags.toTypedArray())
        }
        return TagTrace(tagStates.toTypedArray(), source = "")
    }

    companion object {
        @JvmStatic
        protected val LOG_TAG = "$FLICKER_TAG-PROC"

        internal fun createDumpList(
            wmTrace: WindowManagerTrace,
            layersTrace: LayersTrace
        ): List<WindowManagerStateHelper.Dump> {
            val wmTimestamps = wmTrace.map { it.timestamp }.toTypedArray()
            val layersTimestamps = layersTrace.map { it.timestamp }.toTypedArray()
            val fullTimestamps = sortedSetOf(*wmTimestamps, *layersTimestamps)

            return fullTimestamps.map { baseTimestamp ->
                val wmState = wmTrace
                    .lastOrNull { it.timestamp <= baseTimestamp }
                    ?: wmTrace.first()
                val layerState = layersTrace
                    .lastOrNull { it.timestamp <= baseTimestamp }
                    ?: layersTrace.first()
                WindowManagerStateHelper.Dump(wmState, layerState)
            }.distinctBy { Pair(it.wmState.timestamp, it.layerState.timestamp) }
        }
    }
}
