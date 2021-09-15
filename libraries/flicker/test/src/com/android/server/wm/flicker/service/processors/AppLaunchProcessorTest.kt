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
    private val wmTraceColdAppLaunch =
            readWmTraceFromFile(
                    "tagprocessors/applaunch/cold/WindowManagerTrace.winscope"
            )
    private val sfTraceColdAppLaunch =
            readLayerTraceFromFile(
                    "tagprocessors/applaunch/cold/SurfaceFlingerTrace.winscope"
            )
    private val wmTraceWarmAppLaunch =
            readWmTraceFromFile(
                    "tagprocessors/applaunch/warm/WindowManagerTrace.winscope"
            )
    private val sfTraceWarmAppLaunch =
            readLayerTraceFromFile(
                    "tagprocessors/applaunch/warm/SurfaceFlingerTrace.winscope"
            )
    private val wmTraceAppLaunchByIntent =
            readWmTraceFromFile(
                    "tagprocessors/applaunch/intent/WindowManagerTrace.winscope"
            )
    private val sfTraceAppLaunchByIntent =
            readLayerTraceFromFile(
                    "tagprocessors/applaunch/intent/SurfaceFlingerTrace.winscope"
            )
    private val wmTraceAppLaunchWithRotation =
            readWmTraceFromFile(
                    "tagprocessors/applaunch/withrot/WindowManagerTrace.winscope"
            )
    private val sfTraceAppLaunchWithRotation =
            readLayerTraceFromFile(
                    "tagprocessors/applaunch/withrot/SurfaceFlingerTrace.winscope"
            )

    /**
     * Scenarios expecting no tags
     */
    private val wmTraceComposeNewMessage =
            readWmTraceFromFile(
                    "tagprocessors/ime/appear/bygesture/WindowManagerTrace.winscope"
            )
    private val sfTraceComposeNewMessage =
            readLayerTraceFromFile(
                    "tagprocessors/ime/appear/bygesture/SurfaceFlingerTrace.winscope"
            )
    private val wmTraceRotation =
            readWmTraceFromFile(
                    "tagprocessors/rotation/verticaltohorizontal/WindowManagerTrace.winscope"
            )
    private val sfTraceRotation =
            readLayerTraceFromFile(
                    "tagprocessors/rotation/verticaltohorizontal/SurfaceFlingerTrace.winscope"
            )

    @Test
    fun tagsColdAppLaunch() {
        val tagStates = processor.generateTags(wmTraceColdAppLaunch, sfTraceColdAppLaunch).entries
        Truth.assertThat(tagStates.size).isEqualTo(2)

        val startTagTimestamp = 268309543090536 // Represents 3d2h31m49s543ms
        val endTagTimestamp = 268310230837688 // Represents 3d2h31m50s230ms
        Truth.assertThat(tagStates.first().timestamp).isEqualTo(startTagTimestamp)
        Truth.assertThat(tagStates.last().timestamp).isEqualTo(endTagTimestamp)
    }

    @Test
    fun tagsWarmAppLaunch() {
        val tagStates = processor
            .generateTags(wmTraceWarmAppLaunch, sfTraceWarmAppLaunch).entries
        Truth.assertThat(tagStates.size).isEqualTo(2)

        val startTagTimestamp = 237300088466617 // Represents 2d17h55m0s88ms
        val endTagTimestamp = 237300592571094 // Represents 2d17h55m0s592ms
        Truth.assertThat(tagStates.first().timestamp).isEqualTo(startTagTimestamp)
        Truth.assertThat(tagStates.last().timestamp).isEqualTo(endTagTimestamp)
    }

    @Test
    fun tagsAppLaunchByIntent() {
        val tagStates = processor
            .generateTags(wmTraceAppLaunchByIntent, sfTraceAppLaunchByIntent).entries
        Truth.assertThat(tagStates.size).isEqualTo(2)

        val startTagTimestamp = 80872063113909 // Represents 0d22h27m52s63ms
        val endTagTimestamp = 80872910948056 // Represents 0d22h27m52s910ms
        Truth.assertThat(tagStates.first().timestamp).isEqualTo(startTagTimestamp)
        Truth.assertThat(tagStates.last().timestamp).isEqualTo(endTagTimestamp)
    }

    @Test
    fun tagsAppLaunchWithRotation() {
        val tagStates = processor
            .generateTags(wmTraceAppLaunchWithRotation, sfTraceAppLaunchWithRotation).entries
        Truth.assertThat(tagStates.size).isEqualTo(2)

        val startTagTimestamp = 17864904019309 // Represents 0d4h57m44s904ms
        val endTagTimestamp = 17865482335252 // Represents 0d4h57m45s482ms
        Truth.assertThat(tagStates.first().timestamp).isEqualTo(startTagTimestamp)
        Truth.assertThat(tagStates.last().timestamp).isEqualTo(endTagTimestamp)
    }

    @Test
    fun doesNotTagComposeNewMessage() {
        val tagStates = processor
            .generateTags(wmTraceComposeNewMessage, sfTraceComposeNewMessage).entries
        Truth.assertThat(tagStates).isEmpty()
    }

    @Test
    fun doesNotTagRotation() {
        val tagStates = processor.generateTags(wmTraceRotation, sfTraceRotation).entries
        Truth.assertThat(tagStates).isEmpty()
    }
}