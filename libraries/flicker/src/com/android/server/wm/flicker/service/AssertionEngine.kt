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

import com.android.server.wm.flicker.service.assertors.AssertorConfigModel
import com.android.server.wm.flicker.service.assertors.TransitionAssertor
import com.android.server.wm.flicker.service.assertors.readConfigurationFile
import com.android.server.wm.traces.common.errors.ErrorState
import com.android.server.wm.traces.common.errors.ErrorTrace
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.TagTrace
import com.android.server.wm.traces.common.tags.Transition
import com.android.server.wm.traces.common.tags.TransitionTag
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

/**
 * Invokes the configured assertors and summarizes the results.
 */
class AssertionEngine(private val logger: (String) -> Unit) {
    private val configuration: Array<AssertorConfigModel> =
        readConfigurationFile(FlickerService.configFileName)

    fun analyze(
        wmTrace: WindowManagerTrace,
        layersTrace: LayersTrace,
        tagTrace: TagTrace
    ): ErrorTrace {
        val allStates = mutableListOf<ErrorState>()
        val transitionTags = getTransitionTags(tagTrace)

        configuration.forEach { assertorConfiguration ->
            val assertor = TransitionAssertor(assertorConfiguration, logger)
            val transition = assertorConfiguration.transition
            allStates.addAll(
                splitWmTraceByTags(wmTrace, transitionTags, transition)
                    .flatMap { block ->
                        assertor.analyzeWmTrace(block).entries.asList()
                    }
            )

            allStates.addAll(
                splitLayersTraceByTags(layersTrace, transitionTags, transition)
                    .flatMap { block ->
                        assertor.analyzeLayersTrace(block).entries.asList()
                    }
            )
        }

        /* Ensure all error states with same timestamp are merged */
        val errorStates = allStates.distinct()
                .groupBy({ it.timestamp }, { it.errors.asList() })
                .mapValues { (key, value) ->
                    ErrorState(value.flatten().toTypedArray(), key.toString()) }
                .values.toTypedArray()

        return ErrorTrace(errorStates, source = "")
    }

    /**
     * Extracts all [TransitionTag]s from a [TagTrace].
     *
     * @param tagTrace Tag Trace
     * @return a list with [TransitionTag]
     */
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
    fun splitWmTraceByTags(
        wmTrace: WindowManagerTrace,
        transitionTags: List<TransitionTag>,
        transition: Transition
    ): List<WindowManagerTrace> {
        val wmTags = transitionTags
            .filter { transitionTag ->
                transitionTag.tag.taskId > 0 ||
                transitionTag.tag.windowToken.isNotEmpty() ||
                transitionTag.isEmpty()
            }.filter { transitionTag -> transitionTag.tag.transition == transition }

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
    fun splitLayersTraceByTags(
        layersTrace: LayersTrace,
        transitionTags: List<TransitionTag>,
        transition: Transition
    ): List<LayersTrace> {
        val layersTags = transitionTags
            .filter { transitionTag -> transitionTag.tag.layerId > 0 || transitionTag.isEmpty() }
            .filter { transitionTag -> transitionTag.tag.transition == transition }

        return layersTags.map { tag ->
            layersTrace.filter(tag.startTimestamp, tag.endTimestamp)
        }
    }
}
