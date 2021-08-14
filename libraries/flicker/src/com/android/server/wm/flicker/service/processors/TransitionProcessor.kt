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

import com.android.server.wm.flicker.service.ITagProcessor
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.TagState
import com.android.server.wm.traces.common.tags.TagTrace
import com.android.server.wm.traces.common.tags.Transition
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

abstract class TransitionProcessor : ITagProcessor {
    private var tagStates = mutableListOf<TagState>()
    abstract val transition: Transition

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
        var wmStates = wmTrace.entries.iterator()
        var sfStates = layersTrace.entries.iterator()
        val finalTime = wmTrace.entries.last().timestamp

        /**
         * If WmState or sfState finish, then it is not possible to tag the end state, hence we
         * check both wmStates and sfStates have next stat
         */
        while (wmStates.hasNext() and sfStates.hasNext()) {
            val startTag = findStartTag(wmStates, sfStates)
            if (startTag != null) {
                tagStates.add(startTag)
                tagStates.add(findEndTag(wmStates, sfStates, startTag, finalTime))
            }
        }
        return TagTrace(tagStates.toTypedArray(), "")
    }

    protected abstract fun findStartTag(
        wmStates: Iterator<WindowManagerState>,
        layersEntries: Iterator<LayerTraceEntry>
    ): TagState?

    private fun findEndTag(
        wmStates: Iterator<WindowManagerState>,
        layersEntries: Iterator<LayerTraceEntry>,
        startTagState: TagState,
        finalTime: Long
    ): TagState {
        val endTag = if (wmStates.hasNext() and layersEntries.hasNext()) {
            doFindEndTag(wmStates, layersEntries, startTagState.tags.first().id)
        } else {
            null
        }
        return endTag ?: createFinalEndTag(startTagState, finalTime)
    }

    protected fun createTagState(
        timestamp: Long,
        id: Int,
        isStartTag: Boolean,
        layerId: Int = -1,
        windowToken: String = "",
        taskId: Int = -1,
        isFallback: Boolean = false
    ) = TagState(
            timestamp = timestamp,
            isFallback = isFallback,
            tags = arrayOf(
                Tag(
                    id = id,
                    transition = transition,
                    isStartTag = isStartTag,
                    taskId = taskId,
                    windowToken = windowToken,
                    layerId = layerId
                )
            )
        )

    private fun createFinalEndTag(startTagState: TagState, finalTime: Long): TagState {
        val startTag = startTagState.tags.first()
        return createTagState(
            timestamp = finalTime,
            id = startTag.id,
            isStartTag = false,
            windowToken = startTag.windowToken,
            taskId = startTag.taskId,
            layerId = startTag.layerId,
            isFallback = true
        )
    }

    abstract fun doFindEndTag(
        wmStates: Iterator<WindowManagerState>,
        layersEntries: Iterator<LayerTraceEntry>,
        startTagId: Int
    ): TagState?
}
