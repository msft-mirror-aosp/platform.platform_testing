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

package android.tools.common.traces.surfaceflinger

import android.tools.InitRule
import android.tools.assertThatErrorContainsDebugInfo
import android.tools.assertThrows
import android.tools.common.Cache
import android.tools.common.CrossPlatform
import android.tools.common.datatypes.component.ComponentNameMatcher
import android.tools.common.flicker.subject.layers.LayersTraceSubject
import android.tools.readLayerTraceFromFile
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Contains [LayersTrace] tests. To run this test: `atest FlickerLibTest:LayersTraceTest` */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayersTraceTest {
    private fun detectRootLayer(fileName: String, legacyTrace: Boolean = false) {
        val layersTrace = readLayerTraceFromFile(fileName, legacyTrace = legacyTrace)
        for (entry in layersTrace.entries) {
            val rootLayers = entry.children
            Truth.assertWithMessage("Does not have any root layer")
                .that(rootLayers.size)
                .isGreaterThan(0)
            val firstParentId = rootLayers.first().parentId
            Truth.assertWithMessage("Has multiple root layers")
                .that(rootLayers.all { it.parentId == firstParentId })
                .isTrue()
        }
    }

    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun testCanDetectRootLayer() {
        detectRootLayer("layers_trace_root.pb", legacyTrace = true)
    }

    @Test
    fun testCanDetectRootLayerAOSP() {
        detectRootLayer("layers_trace_root_aosp.pb", legacyTrace = true)
    }

    @Test
    fun canTestLayerOccludedByAppLayerHasVisibleRegion() {
        val trace = readLayerTraceFromFile("layers_trace_occluded.pb", legacyTrace = true)
        val entry =
            trace.getEntryExactlyAt(
                CrossPlatform.timestamp.from(systemUptimeNanos = 1700382131522L)
            )
        val component =
            ComponentNameMatcher("", "com.android.server.wm.flicker.testapp.SimpleActivity#0")
        val layer = entry.getLayerWithBuffer(component)
        Truth.assertWithMessage("App should be visible")
            .that(layer?.visibleRegion?.isEmpty)
            .isFalse()
        Truth.assertWithMessage("App should visible region")
            .that(layer?.visibleRegion?.toString())
            .contains("SkRegion((346,1583,1094,2839))")

        val splashScreenComponent =
            ComponentNameMatcher("", "Splash Screen com.android.server.wm.flicker.testapp#0")
        val splashScreenLayer = entry.getLayerWithBuffer(splashScreenComponent)
        Truth.assertWithMessage("Splash screen should be visible")
            .that(splashScreenLayer?.visibleRegion?.isEmpty)
            .isFalse()
        Truth.assertWithMessage("Splash screen visible region")
            .that(splashScreenLayer?.visibleRegion?.toString())
            .contains("SkRegion((346,1583,1094,2839))")
    }

    @Test
    fun canTestLayerOccludedByAppLayerIsOccludedBySplashScreen() {
        val layerName = "com.android.server.wm.flicker.testapp.SimpleActivity#0"
        val component = ComponentNameMatcher("", layerName)
        val trace = readLayerTraceFromFile("layers_trace_occluded.pb", legacyTrace = true)
        val entry =
            trace.getEntryExactlyAt(
                CrossPlatform.timestamp.from(systemUptimeNanos = 1700382131522L)
            )
        val layer = entry.getLayerWithBuffer(component)
        val occludedBy = layer?.occludedBy ?: emptyArray()
        val partiallyOccludedBy = layer?.partiallyOccludedBy ?: emptyArray()
        Truth.assertWithMessage("Layer $layerName should be occluded").that(occludedBy).isNotEmpty()
        Truth.assertWithMessage("Layer $layerName should not be partially occluded")
            .that(partiallyOccludedBy)
            .isEmpty()
        Truth.assertWithMessage("Layer $layerName should be occluded")
            .that(occludedBy.joinToString())
            .contains(
                "Splash Screen com.android.server.wm.flicker.testapp#0 buffer:w:1440, " +
                    "h:3040, stride:1472, format:1 frame#1 visible:" +
                    "SkRegion((346,1583,1094,2839))"
            )
    }

    @Test
    fun exceptionContainsDebugInfo() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_emptyregion.pb", legacyTrace = true)
        val error =
            assertThrows<AssertionError> { LayersTraceSubject(layersTraceEntries).isEmpty() }
        assertThatErrorContainsDebugInfo(error, withBlameEntry = false)
    }

    companion object {
        @ClassRule @JvmField val initRule = InitRule()
    }
}
