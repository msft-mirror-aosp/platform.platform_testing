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

import com.android.server.wm.flicker.readLayerTraceFromFile
import com.android.server.wm.flicker.readWmTraceFromFile
import com.android.server.wm.traces.common.service.processors.PipExitProcessor
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PipExitProcessorTest {
    private val processor = PipExitProcessor { }

    private val tagPipExitByDismissButton by lazy {
        val wmTrace = readWmTraceFromFile(
    "tagprocessors/pip/pipexit/dismissbutton/WindowManagerTrace.winscope"
        )
        val layersTrace = readLayerTraceFromFile(
    "tagprocessors/pip/pipexit/dismissbutton/SurfaceFlingerTrace.winscope"
        )
        processor.generateTags(wmTrace, layersTrace)
    }

    private val tagPipExitBySwipe by lazy {
        val wmTrace = readWmTraceFromFile(
    "tagprocessors/pip/pipexit/swipe/WindowManagerTrace.winscope"
        )
        val layersTrace = readLayerTraceFromFile(
    "tagprocessors/pip/pipexit/swipe/SurfaceFlingerTrace.winscope"
        )
        processor.generateTags(wmTrace, layersTrace)
    }

    private val tagPipClose by lazy {
        val wmTrace = readWmTraceFromFile(
    "tagprocessors/pip/pipclose/expand/WindowManagerTrace.winscope"
        )
        val layersTrace = readLayerTraceFromFile(
    "tagprocessors/pip/pipclose/expand/SurfaceFlingerTrace.winscope"
        )
        processor.generateTags(wmTrace, layersTrace)
    }

    @Test
    fun generatesPipExitTagsByDismissButton() {
        val tagTrace = tagPipExitByDismissButton
        Truth.assertWithMessage("Should have 2 pip exit tags")
            .that(tagTrace)
            .hasSize(2)
        val startTagTimestamp = 2852929744046 // 0d0h47m32s929ms
        val endTagTimestamp = 2853783914340 // 0d0h47m33s783ms
        Truth.assertThat(tagTrace.first().timestamp).isEqualTo(startTagTimestamp)
        Truth.assertThat(tagTrace.last().timestamp).isEqualTo(endTagTimestamp)
    }

    @Test
    fun generatesPipExitTagsBySwipe() {
        val tagTrace = tagPipExitBySwipe
        Truth.assertWithMessage("Should have 2 pip exit tags")
            .that(tagTrace)
            .hasSize(2)
        val startTagTimestamp = 2057767033167 // 0d0h34m17s767ms
        val endTagTimestamp = 2058963092661 // 0d0h34m18s963ms
        Truth.assertThat(tagTrace.first().timestamp).isEqualTo(startTagTimestamp)
        Truth.assertThat(tagTrace.last().timestamp).isEqualTo(endTagTimestamp)
    }

    @Test
    fun doesNotTagPipExitOnPipClose() {
        val tagTrace = tagPipClose
        Truth.assertWithMessage("Should have 0 pip exit tags")
            .that(tagTrace)
            .hasSize(0)
    }
}