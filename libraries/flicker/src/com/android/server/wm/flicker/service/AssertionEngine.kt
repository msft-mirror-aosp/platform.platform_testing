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
import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.service.FlickerService.Companion.getFassFilePath
import com.android.server.wm.flicker.service.detectors.AppLaunchDetector
import com.android.server.wm.traces.common.errors.ErrorState
import com.android.server.wm.traces.common.errors.ErrorTrace
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.TagTrace
import com.android.server.wm.traces.common.tags.Transition
import com.android.server.wm.traces.common.tags.TransitionTag
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.errors.toProto
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Invokes the configured detectors and summarizes the results.
 */
class AssertionEngine(private val outputDir: Path, private val testTag: String) {
    private val flickerDetectors = mapOf<IFlickerDetector, Transition>(
        // TODO: Add new detectors to invoke
        AppLaunchDetector() to Transition.APP_LAUNCH
    )

    fun analyze(
        wmTrace: WindowManagerTrace,
        layersTrace: LayersTrace,
        tagTrace: TagTrace
    ): ErrorTrace {
        val allStates = mutableListOf<ErrorState>()
        val transitionTags = getTransitionTags(tagTrace)

        flickerDetectors.forEach { (detector, transition) ->
            allStates.addAll(
                splitWmTraceByTags(wmTrace, transitionTags, transition)
                    .flatMap { block ->
                        detector.analyzeWmTrace(block).entries.asList()
                    }
            )

            allStates.addAll(
                splitLayersTraceByTags(layersTrace, transitionTags, transition)
                    .flatMap { block ->
                        detector.analyzeLayersTrace(block).entries.asList()
                    }
            )
        }

        /* Ensure all error states with same timestamp are merged */
        val errorStates = allStates.distinct()
                .groupBy({ it.timestamp }, { it.errors.asList() })
                .mapValues { (key, value) -> ErrorState(value.flatten().toTypedArray(), key) }
                .values.toTypedArray()

        val errorTrace = ErrorTrace(errorStates, source = "")
        writeFile(errorTrace)
        return errorTrace
    }

    /**
     * Extracts all [TransitionTag]s from a [TagTrace].
     *
     * @param tagTrace Tag Trace
     * @return a list with [TransitionTag]
     */
    @VisibleForTesting
    fun getTransitionTags(tagTrace: TagTrace): List<TransitionTag> {
        return tagTrace.entries.flatMap { state ->
            state.tags.filter { tag -> tag.isStartTag }
                .map {
                    TransitionTag(
                        tag = it,
                        startTimestamp = state.timestamp,
                        endTimestamp = getEndTagTimestamp(tagTrace, it)
                    )
                }
        }
    }

    private fun getEndTagTimestamp(tagTrace: TagTrace, tag: Tag): Long {
        val finalTag = tag.copy(isStartTag = false)
        return tagTrace.entries.firstOrNull { state -> state.tags.contains(finalTag) }?.timestamp
            ?: throw RuntimeException("All open tags should be closed!")
    }

    /**
     * Splits a wmTrace by a [Transition].
     *
     * @param wmTrace Window Manager trace
     * @param transitionTags a list with all [TransitionTag]s
     * @param transition the [Transition] to filter the list by
     * @return a list with [WindowManagerTrace] blocks
     */
    @VisibleForTesting
    fun splitWmTraceByTags(
        wmTrace: WindowManagerTrace,
        transitionTags: List<TransitionTag>,
        transition: Transition
    ): List<WindowManagerTrace> {
        val wmTags = transitionTags
            .filter { transitionTag ->
                transitionTag.tag.layerId == -1 && transitionTag.tag.transition == transition
            }

        return wmTags.map { tag -> wmTrace.filter(tag.startTimestamp, tag.endTimestamp) }
    }

    /**
     * Splits a layersTrace by a [Transition].
     *
     * @param layersTrace Surface Flinger trace
     * @param transitionTags a list with all [TransitionTag]s
     * @param transition the [Transition] to filter the list by
     * @return a list with [LayersTrace] blocks
     */
    @VisibleForTesting
    fun splitLayersTraceByTags(
        layersTrace: LayersTrace,
        transitionTags: List<TransitionTag>,
        transition: Transition
    ): List<LayersTrace> {
        val layersTags = transitionTags
            .filter { transitionTag ->
                transitionTag.tag.layerId > 0 && transitionTag.tag.transition == transition
            }

        return layersTags.map { tag ->
            layersTrace.filter(tag.startTimestamp, tag.endTimestamp)
        }
    }

    /**
     * Stores the error trace in a .winscope file.
     */
    private fun writeFile(errorTrace: ErrorTrace) {
        val errorTraceBytes = errorTrace.toProto().toByteArray()
        val errorTraceFile = getFassFilePath(outputDir, testTag, "error_trace")

        try {
            Log.i("FLICKER_ERROR_TRACE", errorTraceFile.toString())
            Files.createDirectories(errorTraceFile.parent)
            Files.write(errorTraceFile, errorTraceBytes)
        } catch (e: IOException) {
            throw RuntimeException("Unable to create error trace file: ${e.message}", e)
        }
    }
}
