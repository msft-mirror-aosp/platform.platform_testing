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

import android.util.Log
import com.android.server.wm.flicker.service.processors.AppLaunchProcessor
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.tags.TagState
import com.android.server.wm.traces.common.tags.TagTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.tags.toProto
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Invokes all concrete tag producers and writes to a .winscope file
 */
class TaggingEngine(private val outputDir: Path, private val testTag: String) {
    private val transitions = listOf<ITagProcessor>(
        // TODO: Keep adding new transition processors to invoke
        AppLaunchProcessor()
    )

    /**
     * Generate tags denoting start and end points for all [transitions] within traces
     * @param wmTrace - WindowManager trace
     * @param layersTrace - SurfaceFlinger trace
     */
    fun tag(wmTrace: WindowManagerTrace, layersTrace: LayersTrace): TagTrace {
        val allStates = transitions.flatMap {
            it.generateTags(wmTrace, layersTrace).entries.asList()
        }

        /**
         * Ensure all tag states with the same timestamp are merged
         */
        val tagStates = allStates.distinct()
            .groupBy({ it.timestamp }, { it.tags.asList() })
            .mapValues { (key, value) -> TagState(key, value.flatten().toTypedArray()) }
            .values.toTypedArray()

        val tagTrace = TagTrace(tagStates, source = "")
        writeFile(tagTrace)
        return tagTrace
    }

    /**
     * Stores the tag trace in a .winscope file.
     */
    private fun writeFile(tagTrace: TagTrace) {
        val tagTraceBytes = tagTrace.toProto().toByteArray()
        val tagTraceFile = FlickerService.getFassFilePath(outputDir, testTag, "tag_trace")

        try {
            Log.i("FLICKER_TAG_TRACE", tagTraceFile.toString())
            Files.createDirectories(tagTraceFile.parent)
            Files.write(tagTraceFile, tagTraceBytes)
        } catch (e: IOException) {
            throw RuntimeException("Unable to create error trace file: ${e.message}", e)
        }
    }
}
