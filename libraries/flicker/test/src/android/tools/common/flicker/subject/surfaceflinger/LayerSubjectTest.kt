/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.common.flicker.subject.surfaceflinger

import android.tools.InitRule
import android.tools.common.Cache
import android.tools.common.datatypes.Size
import android.tools.common.flicker.subject.layers.LayerSubject
import android.tools.common.flicker.subject.layers.LayersTraceSubject
import android.tools.readLayerTraceFromFile
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Contains [LayerSubject] tests. To run this test: `atest FlickerLibTest:LayerSubjectTest` */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayerSubjectTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun exceptionContainsDebugInfoImaginary() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_emptyregion.pb", legacyTrace = true)
        val foundLayer = LayersTraceSubject(layersTraceEntries).first().layer("ImaginaryLayer", 0)
        Truth.assertWithMessage("ImaginaryLayer is not found").that(foundLayer).isNull()
    }

    @Test
    fun canTestAssertionsOnLayer() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_emptyregion.pb", legacyTrace = true)
        LayersTraceSubject(layersTraceEntries)
            .layer("SoundVizWallpaperV2", 26033)
            .hasBufferSize(Size.from(1440, 2960))
            .hasScalingMode(0)
    }

    companion object {
        @ClassRule @JvmField val initRule = InitRule()
    }
}
