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

package com.android.server.wm.flicker.service

import com.android.server.wm.traces.common.errors.ErrorTrace
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.tags.TagTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

/**
 * Contains the logic for Flicker as a Service.
 */
class FlickerService {
    /**
     * The entry point for WM Flicker Service.
     *
     * Calls the Tagging Engine and the Assertion Engine.
     *
     * @param wmTrace Window Manager trace
     * @param layersTrace Surface Flinger trace
     * @return A list containing all failures
     */
    fun process(
        wmTrace: WindowManagerTrace,
        layersTrace: LayersTrace
    ): ErrorTrace {
        val taggingEngine = TaggingEngine()
        val tagTrace = taggingEngine.tag(wmTrace, layersTrace)
        injectTags(wmTrace, layersTrace, tagTrace)

        val assertionEngine = AssertionEngine()
        return assertionEngine.analyze(wmTrace, layersTrace)
    }

    /**
     * Connects the tags from [TagTrace] to [WindowManagerTrace] and [LayersTrace].
     */
    private fun injectTags(
        wmTrace: WindowManagerTrace,
        layersTrace: LayersTrace,
        tagTrace: TagTrace
    ) {
        tagTrace.entries.forEach { state ->
            state.tags.forEach {
                if (it.taskId != -1) {
                    val wmState = wmTrace.getEntry(timestamp = state.timestamp)
                    wmState.addTag(it.transition)
                }
                if (it.layerId != -1) {
                    val layersEntry = layersTrace.getEntry(timestamp = state.timestamp)
                    layersEntry.addTag(it.transition)
                }
            }
        }
    }
}
