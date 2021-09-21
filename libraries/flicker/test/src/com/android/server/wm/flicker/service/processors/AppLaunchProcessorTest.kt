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
import com.android.server.wm.traces.common.service.processors.AppLaunchProcessor
import com.google.common.truth.Truth
import org.junit.Test

/**
 * Contains [AppLaunchProcessor] tests. To run this test: `atest
 * FlickerLibTest:AppLaunchProcessorTest`
 */
class AppLaunchProcessorTest {
    private val processor = AppLaunchProcessor { }

    /**
     * Scenarios expecting tags
     */
    private val tagsColdAppLaunch by lazy {
        val wmTrace = readWmTraceFromFile(
            "tagprocessors/applaunch/cold/WindowManagerTrace.winscope"
        )
        val layersTrace = readLayerTraceFromFile(
            "tagprocessors/applaunch/cold/SurfaceFlingerTrace.winscope"
        )
        processor.generateTags(wmTrace, layersTrace)
    }

    private val tagsWarmAppLaunch by lazy {
        val wmTrace = readWmTraceFromFile(
            "tagprocessors/applaunch/warm/WindowManagerTrace.winscope"
        )
        val layersTrace = readLayerTraceFromFile(
            "tagprocessors/applaunch/warm/SurfaceFlingerTrace.winscope"
        )
        processor.generateTags(wmTrace, layersTrace)
    }

    private val tagsAppLaunchByIntent by lazy {
        val wmTrace = readWmTraceFromFile(
            "tagprocessors/applaunch/intent/WindowManagerTrace.winscope"
        )
        val layersTrace = readLayerTraceFromFile(
            "tagprocessors/applaunch/intent/SurfaceFlingerTrace.winscope"
        )
        processor.generateTags(wmTrace, layersTrace)
    }

    private val tagsAppLaunchWithRotation by lazy {
        val wmTrace = readWmTraceFromFile(
            "tagprocessors/applaunch/withrot/WindowManagerTrace.winscope"
        )
        val layersTrace = readLayerTraceFromFile(
            "tagprocessors/applaunch/withrot/SurfaceFlingerTrace.winscope"
        )
        processor.generateTags(wmTrace, layersTrace)
    }

    /**
     * Scenarios expecting no tags
     */
    private val tagsComposeNewMessage by lazy {
        val wmTrace = readWmTraceFromFile(
            "tagprocessors/ime/appear/bygesture/WindowManagerTrace.winscope"
        )
        val layersTrace = readLayerTraceFromFile(
            "tagprocessors/ime/appear/bygesture/SurfaceFlingerTrace.winscope"
        )
        processor.generateTags(wmTrace, layersTrace)
    }

    private val tagsRotation by lazy {
        val wmTrace = readWmTraceFromFile(
            "tagprocessors/rotation/verticaltohorizontal/WindowManagerTrace.winscope"
        )
        val layersTrace = readLayerTraceFromFile(
            "tagprocessors/rotation/verticaltohorizontal/SurfaceFlingerTrace.winscope"
        )
        processor.generateTags(wmTrace, layersTrace)
    }

    @Test
    fun tagsColdAppLaunch() {
        val tagTrace = tagsColdAppLaunch
        Truth.assertWithMessage("Should have 2 app launch tags")
            .that(tagTrace)
            .hasSize(2)
        val startTagTimestamp = 192568912054261 // Represents 2d5h29m28s912ms
        val endTagTimestamp = 192569897936182 // Represents 2d5h29m29s897ms
        Truth.assertThat(tagTrace.first().timestamp).isEqualTo(startTagTimestamp)
        Truth.assertThat(tagTrace.last().timestamp).isEqualTo(endTagTimestamp)
    }

    @Test
    fun tagsWarmAppLaunch() {
        val tagTrace = tagsWarmAppLaunch
        Truth.assertWithMessage("Should have 2 app launch tags")
            .that(tagTrace)
            .hasSize(2)
        val startTagTimestamp = 192782254751712 // Represents 2d5h33m2s275ms
        val endTagTimestamp = 192782828336352 // Represents 2d5h33m2s828ms
        Truth.assertThat(tagTrace.first().timestamp).isEqualTo(startTagTimestamp)
        Truth.assertThat(tagTrace.last().timestamp).isEqualTo(endTagTimestamp)
    }

    @Test
    fun tagsAppLaunchByIntent() {
        val tagTrace = tagsAppLaunchByIntent
        Truth.assertWithMessage("Should have 2 app launch tags")
            .that(tagTrace)
            .hasSize(2)
        val startTagTimestamp = 192613118153196 // Represents 2d5h30m13s118ms
        val endTagTimestamp = 192614112528399 // Represents 2d5h30m14s112ms
        Truth.assertThat(tagTrace.first().timestamp).isEqualTo(startTagTimestamp)
        Truth.assertThat(tagTrace.last().timestamp).isEqualTo(endTagTimestamp)
    }

    @Test
    fun tagsAppLaunchWithRotation() {
        val tagTrace = tagsAppLaunchWithRotation
        Truth.assertWithMessage("Should have 2 app launch tags")
            .that(tagTrace)
            .hasSize(2)
        val startTagTimestamp = 192631295932194 // Represents 2d5h30m31s295ms
        val endTagTimestamp = 192632120383579 // Represents 2d5h30m32s120ms
        Truth.assertThat(tagTrace.first().timestamp).isEqualTo(startTagTimestamp)
        Truth.assertThat(tagTrace.last().timestamp).isEqualTo(endTagTimestamp)
    }

    @Test
    fun doesNotTagComposeNewMessage() {
        val tagTrace = tagsComposeNewMessage
        Truth.assertWithMessage("Should have 0 app launch tags")
            .that(tagTrace)
            .isEmpty()
    }

    @Test
    fun doesNotTagRotation() {
        val tagTrace = tagsRotation
        Truth.assertWithMessage("Should have 0 app launch tags")
            .that(tagTrace)
            .isEmpty()
    }
}