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

import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.Transition
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import kotlin.math.max
import kotlin.math.min

/**
 * Base state for a FSM
 */
abstract class FSMState(protected val tags: MutableMap<Long, MutableList<Tag>>) {
    private var lastTagId = -1
    abstract fun process(
        previous: WindowManagerStateHelper.Dump?,
        current: WindowManagerStateHelper.Dump,
        next: WindowManagerStateHelper.Dump?
    ): FSMState?

    protected fun addStartTransitionTag(
        state: WindowManagerStateHelper.Dump,
        transition: Transition,
        layerId: Int = -1,
        windowToken: String = "",
        taskId: Int = -1
    ) {
        val timestamp = min(state.wmState.timestamp, state.layerState.timestamp)
        val tagId = lastTagId++
        val startTag = Tag(id = tagId, transition, isStartTag = true, layerId = layerId,
            windowToken = windowToken, taskId = taskId)
        tags.putIfAbsent(timestamp, mutableListOf())
        tags.getValue(timestamp).add(startTag)
    }

    protected fun addEndTransitionTag(
        state: WindowManagerStateHelper.Dump,
        transition: Transition,
        layerId: Int = -1,
        windowToken: String = "",
        taskId: Int = -1
    ) {
        val timestamp = max(state.wmState.timestamp, state.layerState.timestamp)
        val endTag = Tag(id = lastTagId, transition, isStartTag = false, layerId = layerId,
            windowToken = windowToken, taskId = taskId)
        tags.putIfAbsent(timestamp, mutableListOf())
        tags.getValue(timestamp).add(endTag)
    }

    protected fun hasOpenTag() = tags.values.flatten().size % 2 != 0
}
