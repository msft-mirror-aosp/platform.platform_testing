/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.flicker

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Region
import android.view.WindowManager
import androidx.test.filters.FlakyTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.traces.layers.Layer
import com.android.server.wm.flicker.traces.layers.LayersTrace
import com.android.server.wm.flicker.traces.layers.LayersTrace.Companion.parseFrom
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [LayersTrace] tests. To run this test: `atest
 * FlickerLibTest:LayersTraceTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayersTraceTest {
    @Test
    fun canParseAllLayers() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        Truth.assertThat(trace.entries).isNotEmpty()
        Truth.assertThat(trace.entries[0].timestamp).isEqualTo(2307984557311L)
        Truth.assertThat(trace.entries[trace.entries.size - 1].timestamp)
                .isEqualTo(2308521813510L)
        val flattenedLayers = trace.entries[0].flattenedLayers
        val msg = "Layers:\n" + flattenedLayers.joinToString("\n\t") { it.name }
        Truth.assertWithMessage(msg).that(flattenedLayers).hasSize(47)
    }

    @FlakyTest
    @Test
    fun canParseVisibleLayers() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        Truth.assertThat(trace.entries).isNotEmpty()
        Truth.assertThat(trace.entries[0].timestamp).isEqualTo(2307984557311L)
        Truth.assertThat(trace.entries[trace.entries.size - 1].timestamp)
                .isEqualTo(2308521813510L)
        val flattenedLayers: List<Layer> = trace.entries[0].flattenedLayers
        val visibleLayers = flattenedLayers.filter { it.isVisible && !it.isHiddenByParent }
        val msg = "Visible Layers:\n" + visibleLayers.joinToString("\n") { "\t" + it.name }
        Truth.assertWithMessage(msg).that(visibleLayers).hasSize(9)
    }

    @Test
    fun canParseLayerHierarchy() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        Truth.assertThat(trace.entries).isNotEmpty()
        Truth.assertThat(trace.entries[0].timestamp).isEqualTo(2307984557311L)
        Truth.assertThat(trace.entries[trace.entries.size - 1].timestamp)
                .isEqualTo(2308521813510L)
        val layers = trace.entries[0].rootLayers
        Truth.assertThat(layers).hasSize(2)
        Truth.assertThat(layers[0].children).hasSize(layers[0].children.size)
        Truth.assertThat(layers[1].children).hasSize(layers[1].children.size)
    }

    // b/76099859
    @Test
    fun canDetectOrphanLayers() {
        try {
            readLayerTraceFromFile("layers_trace_orphanlayers.pb")
            Assert.fail("Failed to detect orphaned layers.")
        } catch (exception: RuntimeException) {
            Truth.assertThat(exception.message)
                    .contains("Failed to parse layers trace. Found orphan layers "
                            + "with parent layer id:1006 : 49")
        }
    }

    // b/75276931
    @Test
    fun canDetectUncoveredRegion() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val entry = trace.getEntry(2308008331271L)
        val result = entry.coversRegion(Region(0, 0, 1440, 2960))
        Truth.assertThat(result.failed()).isTrue()
        Truth.assertThat(result.reason).contains("Region to test: SkRegion((0,0,1440,2960))")
        Truth.assertThat(result.reason).contains("first empty point: 0, 99")
        Truth.assertThat(result.reason).contains("visible regions:")
        Truth.assertWithMessage("Reason contains list of visible regions")
                .that(result.reason)
                .contains("StatusBar#0 - SkRegion((0,0,1440,98))")
    }

    // Visible region tests
    @Test
    fun canTestLayerVisibleRegion_layerDoesNotExist() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val entry = trace.getEntry(2308008331271L)
        val expectedVisibleRegion = Region(0, 0, 1, 1)
        val result = entry.hasVisibleRegion("ImaginaryLayer", expectedVisibleRegion)
        Truth.assertThat(result.failed()).isTrue()
        Truth.assertThat(result.reason).contains("Could not find ImaginaryLayer")
    }

    @Test
    fun canTestLayerVisibleRegion_layerDoesNotHaveExpectedVisibleRegion() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val entry = trace.getEntry(2307993020072L)
        val expectedVisibleRegion = Region(0, 0, 1, 1)
        val result = entry.hasVisibleRegion("NexusLauncherActivity#2", expectedVisibleRegion)
        Truth.assertThat(result.failed()).isTrue()
        Truth.assertThat(result.reason)
                .contains(
                        "Layer com.google.android.apps.nexuslauncher/com.google.android.apps"
                                + ".nexuslauncher.NexusLauncherActivity#2 is invisible: activeBuffer=null"
                                + " type != ColorLayer flags=1 (FLAG_HIDDEN set) visible region is empty")
    }

    @Test
    fun canTestLayerVisibleRegion_layerIsHiddenByParent() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val entry = trace.getEntry(2308455948035L)
        val expectedVisibleRegion = Region(0, 0, 1, 1)
        val result = entry.hasVisibleRegion(
                "SurfaceView - com.android.chrome/com.google.android.apps.chrome.Main",
                expectedVisibleRegion)
        Truth.assertThat(result.failed()).isTrue()
        Truth.assertThat(result.reason)
                .contains(
                        "Layer SurfaceView - com.android.chrome/com.google.android.apps.chrome.Main#0 is "
                                + "hidden by parent: com.android.chrome/com.google.android.apps.chrome"
                                + ".Main#0")
    }

    @Test
    fun canTestLayerVisibleRegion_incorrectRegionSize() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val entry = trace.getEntry(2308008331271L)
        val expectedVisibleRegion = Region(0, 0, 1440, 99)
        val result = entry.hasVisibleRegion("StatusBar", expectedVisibleRegion)
        Truth.assertThat(result.failed()).isTrue()
        Truth.assertThat(result.reason)
                .contains("StatusBar#0 has visible "
                        + "region:SkRegion((0,0,1440,98)) expected:SkRegion((0,0,1440,99))")
    }

    @Test
    fun canTestLayerVisibleRegion() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val entry = trace.getEntry(2308008331271L)
        val expectedVisibleRegion = Region(0, 0, 1440, 98)
        val result = entry.hasVisibleRegion("StatusBar", expectedVisibleRegion)
        Truth.assertThat(result.passed()).isTrue()
    }

    @Test
    fun canTestLayerVisibleRegion_layerIsNotVisible() {
        val trace = readLayerTraceFromFile("layers_trace_invalid_layer_visibility.pb")
        val entry = trace.getEntry(252794268378458L)
        val result = entry.isVisible("com.android.server.wm.flicker.testapp")
        Truth.assertThat(result.failed()).isTrue()
        Truth.assertThat(result.reason)
                .contains(
                        "Layer com.android.server.wm.flicker.testapp/com.android.server.wm.flicker"
                                + ".testapp.SimpleActivity#0 is invisible: type != ColorLayer visible "
                                + "region is empty")
    }

    companion object {
        private fun readLayerTraceFromFile(relativePath: String): LayersTrace {
            return try {
                parseFrom(readTestFile(relativePath)) { false }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        private val displayBounds: Region
            get() {
                val display = Point()
                val wm = InstrumentationRegistry.getInstrumentation()
                        .context
                        .getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.defaultDisplay.getRealSize(display)
                return Region(Rect(0, 0, display.x, display.y))
            }
    }
}
