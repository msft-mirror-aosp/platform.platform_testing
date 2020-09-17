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
import androidx.test.platform.app.InstrumentationRegistry
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
        Truth.assertThat(trace.entries.first().timestamp).isEqualTo(922839428857)
        Truth.assertThat(trace.entries.last().timestamp).isEqualTo(941432656959)
        val flattenedLayers = trace.entries.first().flattenedLayers
        val msg = "Layers:\n" + flattenedLayers.joinToString("\n\t") { it.name }
        Truth.assertWithMessage(msg).that(flattenedLayers).hasSize(57)
    }

    @Test
    fun canParseVisibleLayersLauncher() {
        val trace = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        val visibleLayers = trace.getEntry(90480846872160).visibleLayers
        val msg = "Visible Layers:\n" + visibleLayers.joinToString("\n") { "\t" + it.name }
        Truth.assertWithMessage(msg).that(visibleLayers).hasSize(6)
        Truth.assertThat(msg).contains("ScreenDecorOverlay#0")
        Truth.assertThat(msg).contains("ScreenDecorOverlayBottom#0")
        Truth.assertThat(msg).contains("NavigationBar0#0")
        Truth.assertThat(msg).contains("ImageWallpaper#0")
        Truth.assertThat(msg).contains("StatusBar#0")
        Truth.assertThat(msg).contains("NexusLauncherActivity#0")
    }

    @Test
    fun canParseVisibleLayersSplitScreen() {
        val trace = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        val visibleLayers = trace.getEntry(90493757372977).visibleLayers
        val msg = "Visible Layers:\n" + visibleLayers.joinToString("\n") { "\t" + it.name }
        Truth.assertWithMessage(msg).that(visibleLayers).hasSize(7)
        Truth.assertThat(msg).contains("ScreenDecorOverlayBottom#0")
        Truth.assertThat(msg).contains("ScreenDecorOverlay#0")
        Truth.assertThat(msg).contains("NavigationBar0#0")
        Truth.assertThat(msg).contains("StatusBar#0")
        Truth.assertThat(msg).contains("DockedStackDivider#0")
        Truth.assertThat(msg).contains("ConversationListActivity#0")
        Truth.assertThat(msg).contains("GoogleDialtactsActivity#0")
    }

    @Test
    fun canParseVisibleLayersInTransition() {
        val trace = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        val visibleLayers = trace.getEntry(90488463619533).visibleLayers
        val msg = "Visible Layers:\n" + visibleLayers.joinToString("\n") { "\t" + it.name }
        Truth.assertWithMessage(msg).that(visibleLayers).hasSize(10)
        Truth.assertThat(msg).contains("ScreenDecorOverlayBottom#0")
        Truth.assertThat(msg).contains("ScreenDecorOverlay#0")
        Truth.assertThat(msg).contains("NavigationBar0#0")
        Truth.assertThat(msg).contains("StatusBar#0")
        Truth.assertThat(msg).contains("DockedStackDivider#0")
        Truth.assertThat(msg).contains("SnapshotStartingWindow for taskId=21 - " +
            "task-snapshot-surface#0")
        Truth.assertThat(msg).contains("SnapshotStartingWindow for taskId=21")
        Truth.assertThat(msg).contains("NexusLauncherActivity#0")
        Truth.assertThat(msg).contains("ImageWallpaper#0")
        Truth.assertThat(msg).contains("ConversationListActivity#0")
    }

    @Test
    fun canParseLayerHierarchy() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        Truth.assertThat(trace.entries).isNotEmpty()
        Truth.assertThat(trace.entries.first().timestamp).isEqualTo(922839428857)
        Truth.assertThat(trace.entries.last().timestamp).isEqualTo(941432656959)
        val layers = trace.entries.first().rootLayers
        Truth.assertThat(layers).hasSize(3)
        Truth.assertThat(layers[0].children).hasSize(3)
        Truth.assertThat(layers[1].children).isEmpty()
    }

    // b/76099859
    @Test
    fun canDetectOrphanLayers() {
        try {
            readLayerTraceFromFile("layers_trace_orphanlayers.pb", ignoreOrphanLayers = false)
            Assert.fail("Failed to detect orphaned layers.")
        } catch (exception: RuntimeException) {
            Truth.assertThat(exception.message)
                    .contains("Failed to parse layers trace. Found orphan layer with id = 49 with" +
                            " parentId = 1006")
        }
    }

    // b/75276931
    @Test
    fun canDetectUncoveredRegion() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val entry = trace.getEntry(935346112030)
        val result = entry.coversAtLeastRegion(Region(0, 0, 1440, 2960))
        Truth.assertWithMessage(result.reason).that(result.failed()).isTrue()
        Truth.assertThat(result.reason).contains("Region to test: SkRegion((0,0,1440,2960))")
        Truth.assertThat(result.reason).contains("Uncovered region: SkRegion((0,171,1440,2960))")
    }

    // Visible region tests
    @Test
    fun canTestLayerVisibleRegion_layerDoesNotExist() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val entry = trace.getEntry(937229257165)
        val expectedVisibleRegion = Region(0, 0, 1, 1)
        val result = entry.hasVisibleRegion("ImaginaryLayer", expectedVisibleRegion)
        Truth.assertWithMessage(result.reason).that(result.failed()).isTrue()
        Truth.assertThat(result.reason).contains("Could not find ImaginaryLayer")
    }

    @Test
    fun canTestLayerVisibleRegion_layerDoesNotHaveExpectedVisibleRegion() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val entry = trace.getEntry(937126074082)
        val expectedVisibleRegion = Region(0, 0, 1, 1)
        val result = entry.hasVisibleRegion("DockedStackDivider#0", expectedVisibleRegion)
        Truth.assertWithMessage(result.reason).that(result.failed()).isTrue()
        Truth.assertThat(result.reason)
                .contains("Layer DockedStackDivider#0 is invisible: " +
                        "activeBuffer=null type != ColorLayer flags=1 (FLAG_HIDDEN set) " +
                        "visible region is empty")
    }

    @Test
    fun canTestLayerVisibleRegion_layerIsHiddenByParent() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val entry = trace.getEntry(935346112030)
        val expectedVisibleRegion = Region(0, 0, 1, 1)
        val result = entry.hasVisibleRegion("SimpleActivity#0", expectedVisibleRegion)
        Truth.assertWithMessage(result.reason).that(result.failed()).isTrue()
        Truth.assertThat(result.reason)
                .contains("SimpleActivity#0 is hidden by parent: " +
                        "b5cf8d3 com.android.server.wm.flicker.testapp")
    }

    @Test
    fun canTestLayerVisibleRegion_incorrectRegionSize() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val entry = trace.getEntry(937126074082)
        val expectedVisibleRegion = Region(0, 0, 1440, 99)
        val result = entry.hasVisibleRegion("StatusBar", expectedVisibleRegion)
        Truth.assertWithMessage(result.reason).that(result.failed()).isTrue()
        Truth.assertThat(result.reason)
                .contains("StatusBar#0 has visible " +
                    "region:SkRegion((0,0,1440,171)) expected:SkRegion((0,0,1440,99))")
    }

    @Test
    fun canTestLayerVisibleRegion() {
        val trace = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        val entry = trace.getEntry(90480846872160)
        val expectedVisibleRegion = Region(0, 0, 1080, 145)
        val result = entry.hasVisibleRegion("StatusBar", expectedVisibleRegion)
        Truth.assertWithMessage(result.reason).that(result.passed()).isTrue()
    }

    @Test
    fun canTestLayerVisibleRegion_layerIsNotVisible() {
        val trace = readLayerTraceFromFile("layers_trace_invalid_layer_visibility.pb")
        val entry = trace.getEntry(252794268378458L)
        val result = entry.isVisible("com.android.server.wm.flicker.testapp")
        Truth.assertWithMessage(result.reason).that(result.failed()).isTrue()
        Truth.assertThat(result.reason)
                .contains(
                    "Layer com.android.server.wm.flicker.testapp/com.android.server.wm.flicker" +
                    ".testapp.SimpleActivity#0 is invisible: type != ColorLayer visible " +
                    "region is empty")
    }

    companion object {
        private fun readLayerTraceFromFile(
            relativePath: String,
            ignoreOrphanLayers: Boolean = true
        ): LayersTrace {
            return try {
                parseFrom(readTestFile(relativePath)) { ignoreOrphanLayers }
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
