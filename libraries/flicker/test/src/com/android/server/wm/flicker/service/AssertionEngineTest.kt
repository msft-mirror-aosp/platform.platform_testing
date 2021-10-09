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

import com.android.server.wm.flicker.readLayerTraceFromFile
import com.android.server.wm.flicker.readTagTraceFromFile
import com.android.server.wm.flicker.readWmTraceFromFile
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.Transition
import com.android.server.wm.traces.common.tags.TransitionTag
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [AssertionEngine] tests. To run this test:
 * `atest FlickerLibTest:AssertionEngineTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AssertionEngineTest {
    private val assertionEngine = AssertionEngine { }

    // TODO(b/197632497): Replace mocked Transition tag lists with tag trace files
    private val wmTagsList = listOf(
        TransitionTag(
            startTimestamp = 9213763541297,
            endTimestamp = 9215895891561,
            tag = Tag(
                id = 1,
                transition = Transition.APP_LAUNCH,
                isStartTag = true,
                taskId = 4,
                windowToken = "",
                layerId = 0
            )
        ),
        TransitionTag(
            startTimestamp = 9213763541297,
            endTimestamp = 9215895891561,
            tag = Tag(
                id = 2,
                transition = Transition.IME_APPEAR,
                isStartTag = true,
                taskId = 0,
                windowToken = "DummyAPP",
                layerId = 0
            )
        ),
        TransitionTag(
            startTimestamp = 9213763541297,
            endTimestamp = 9215895891561,
            tag = Tag(
                id = 3,
                transition = Transition.ROTATION,
                isStartTag = true,
                taskId = 0,
                windowToken = "",
                layerId = 0
            )
        )
    )

    private val layersTagsList = listOf(
        TransitionTag(
            startTimestamp = 71607477186189,
            endTimestamp = 71607812120180,
            tag = Tag(
                id = 1,
                transition = Transition.APP_LAUNCH,
                isStartTag = true,
                taskId = 0,
                windowToken = "",
                layerId = 2
            )
        ),
        TransitionTag(
            startTimestamp = 71607477186189,
            endTimestamp = 71607812120180,
            tag = Tag(
                id = 2,
                transition = Transition.ROTATION,
                isStartTag = true,
                taskId = 0,
                windowToken = "",
                layerId = 0
            )
        )
    )

    private val mergedTagsList = listOf(
        TransitionTag(
            startTimestamp = 9213763541297,
            endTimestamp = 9215895891561,
            tag = Tag(
                id = 1,
                transition = Transition.APP_LAUNCH,
                isStartTag = true,
                taskId = 0,
                windowToken = "",
                layerId = 2
            )
        ),
        TransitionTag(
            startTimestamp = 9215895891561,
            endTimestamp = 9216093628925,
            tag = Tag(
                id = 2,
                transition = Transition.APP_CLOSE,
                isStartTag = true,
                taskId = 4,
                windowToken = "",
                layerId = 0
            )
        ),
        TransitionTag(
            startTimestamp = 71607812120180,
            endTimestamp = 71608330503774,
            tag = Tag(
                id = 2,
                transition = Transition.APP_CLOSE,
                isStartTag = true,
                taskId = 0,
                windowToken = "",
                layerId = 3
            )
        )
    )

    @Test
    fun canExtractTransitionTags() {
        val tagTrace = readTagTraceFromFile("tag_trace_open_app_cold.pb")
        val transitionTags = assertionEngine.getTransitionTags(tagTrace)

        Truth.assertThat(transitionTags).isNotEmpty()
        Truth.assertThat(transitionTags.size).isEqualTo(1)
    }

    @Test
    fun canSplitLayersTrace_layerId() {
        val layersTrace = readLayerTraceFromFile("layers_trace_openchrome.pb")
        val blocks = assertionEngine
            .splitLayersTraceByTags(layersTrace, layersTagsList, Transition.APP_LAUNCH)

        Truth.assertThat(blocks).isNotEmpty()
        Truth.assertThat(blocks.size).isEqualTo(1)

        val entries = blocks.first().entries
        Truth.assertThat(entries.first().timestamp).isEqualTo(71607477186189)
        Truth.assertThat(entries.last().timestamp).isEqualTo(71607812120180)
    }

    @Test
    fun canSplitLayersTrace_emptyTag() {
        val layersTrace = readLayerTraceFromFile("layers_trace_openchrome.pb")
        val blocks = assertionEngine
                .splitLayersTraceByTags(layersTrace, layersTagsList, Transition.ROTATION)

        Truth.assertThat(blocks).isNotEmpty()
        Truth.assertThat(blocks.size).isEqualTo(1)

        val entries = blocks.first().entries
        Truth.assertThat(entries.first().timestamp).isEqualTo(71607477186189)
        Truth.assertThat(entries.last().timestamp).isEqualTo(71607812120180)
    }

    @Test
    fun canSplitLayersTrace_mergedTags() {
        val layersTrace = readLayerTraceFromFile("layers_trace_openchrome.pb")
        val blocks = assertionEngine
            .splitLayersTraceByTags(layersTrace, mergedTagsList, Transition.APP_CLOSE)

        Truth.assertThat(blocks).isNotEmpty()
        Truth.assertThat(blocks.size).isEqualTo(1)

        val entries = blocks.first().entries
        Truth.assertThat(entries.first().timestamp).isEqualTo(71607812120180)
        Truth.assertThat(entries.last().timestamp).isEqualTo(71608330503774)
    }

    @Test
    fun canSplitLayersTrace_noTags() {
        val layersTrace = readLayerTraceFromFile("layers_trace_openchrome.pb")
        val blocks = assertionEngine
            .splitLayersTraceByTags(layersTrace, wmTagsList, Transition.APP_LAUNCH)

        Truth.assertThat(blocks).isEmpty()
    }

    @Test
    fun canSplitWmTrace_taskId() {
        val wmTrace = readWmTraceFromFile("wm_trace_openchrome.pb")
        val blocks = assertionEngine
            .splitWmTraceByTags(wmTrace, wmTagsList, Transition.APP_LAUNCH)

        Truth.assertThat(blocks).isNotEmpty()
        Truth.assertThat(blocks.size).isEqualTo(1)

        val entries = blocks.first().entries
        Truth.assertThat(entries.first().timestamp).isEqualTo(9213763541297)
        Truth.assertThat(entries.last().timestamp).isEqualTo(9215895891561)
    }

    @Test
    fun canSplitWmTrace_windowToken() {
        val wmTrace = readWmTraceFromFile("wm_trace_openchrome.pb")
        val blocks = assertionEngine
                .splitWmTraceByTags(wmTrace, wmTagsList, Transition.IME_APPEAR)

        Truth.assertThat(blocks).isNotEmpty()
        Truth.assertThat(blocks.size).isEqualTo(1)

        val entries = blocks.first().entries
        Truth.assertThat(entries.first().timestamp).isEqualTo(9213763541297)
        Truth.assertThat(entries.last().timestamp).isEqualTo(9215895891561)
    }

    @Test
    fun canSplitWmTrace_emptyTag() {
        val wmTrace = readWmTraceFromFile("wm_trace_openchrome.pb")
        val blocks = assertionEngine
                .splitWmTraceByTags(wmTrace, wmTagsList, Transition.ROTATION)

        Truth.assertThat(blocks).isNotEmpty()
        Truth.assertThat(blocks.size).isEqualTo(1)

        val entries = blocks.first().entries
        Truth.assertThat(entries.first().timestamp).isEqualTo(9213763541297)
        Truth.assertThat(entries.last().timestamp).isEqualTo(9215895891561)
    }

    @Test
    fun canSplitWmTrace_mergedTags() {
        val wmTrace = readWmTraceFromFile("wm_trace_openchrome.pb")
        val blocks = assertionEngine
            .splitWmTraceByTags(wmTrace, mergedTagsList, Transition.APP_CLOSE)

        Truth.assertThat(blocks).isNotEmpty()
        Truth.assertThat(blocks.size).isEqualTo(1)

        val entries = blocks.first().entries
        Truth.assertThat(entries.first().timestamp).isEqualTo(9215895891561)
        Truth.assertThat(entries.last().timestamp).isEqualTo(9216093628925)
    }

    @Test
    fun canSplitWmTrace_noTags() {
        val wmTrace = readWmTraceFromFile("wm_trace_openchrome.pb")
        val blocks = assertionEngine
            .splitWmTraceByTags(wmTrace, layersTagsList, Transition.APP_LAUNCH)

        Truth.assertThat(blocks).isEmpty()
    }
}