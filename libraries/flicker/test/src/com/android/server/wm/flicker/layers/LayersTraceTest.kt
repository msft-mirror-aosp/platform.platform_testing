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

package com.android.server.wm.flicker.layers

import com.android.server.wm.flicker.assertThatErrorContainsDebugInfo
import com.android.server.wm.flicker.assertThrows
import com.android.server.wm.flicker.readLayerTraceFromFile
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.utils.MockLayerTraceEntryBuilder
import com.android.server.wm.flicker.utils.MockLayersTraceBuilder
import com.android.server.wm.traces.common.Cache
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.layers.LayersTrace
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [LayersTrace] tests. To run this test:
 * `atest FlickerLibTest:LayersTraceTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayersTraceTest {
    private fun detectRootLayer(fileName: String) {
        val layersTrace = readLayerTraceFromFile(fileName)
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
        detectRootLayer("layers_trace_root.pb")
    }

    @Test
    fun testCanDetectRootLayerAOSP() {
        detectRootLayer("layers_trace_root_aosp.pb")
    }

    @Test
    fun canParseFromDumpWithDisplay() {
        val trace = readLayerTraceFromFile("layers_dump_with_display.pb")
        Truth.assertWithMessage("Dump is not empty")
            .that(trace)
            .isNotEmpty()
        Truth.assertWithMessage("Dump contains display is not empty")
            .that(trace.first().displays)
            .asList()
            .isNotEmpty()
    }

    @Test
    fun canTestLayerOccludedByAppLayerHasVisibleRegion() {
        val trace = readLayerTraceFromFile("layers_trace_occluded.pb")
        val entry = trace.getEntry(1700382131522L)
        val component = ComponentNameMatcher("",
            "com.android.server.wm.flicker.testapp.SimpleActivity#0")
        val layer = entry.getLayerWithBuffer(component)
        Truth.assertWithMessage("App should be visible")
                .that(layer?.visibleRegion?.isEmpty).isFalse()
        Truth.assertWithMessage("App should visible region")
                .that(layer?.visibleRegion?.toString())
                .contains("SkRegion((346,1583,1094,2839))")

        val splashScreenComponent = ComponentNameMatcher("",
            "Splash Screen com.android.server.wm.flicker.testapp#0")
        val splashScreenLayer = entry.getLayerWithBuffer(splashScreenComponent)
        Truth.assertWithMessage("Splash screen should be visible")
                .that(splashScreenLayer?.visibleRegion?.isEmpty).isFalse()
        Truth.assertWithMessage("Splash screen visible region")
                .that(splashScreenLayer?.visibleRegion?.toString())
                .contains("SkRegion((346,1583,1094,2839))")
    }

    @Test
    fun canTestLayerOccludedByAppLayerIsOccludedBySplashScreen() {
        val layerName = "com.android.server.wm.flicker.testapp.SimpleActivity#0"
        val component = ComponentNameMatcher("", layerName)
        val trace = readLayerTraceFromFile("layers_trace_occluded.pb")
        val entry = trace.getEntry(1700382131522L)
        val layer = entry.getLayerWithBuffer(component)
        val occludedBy = layer?.occludedBy ?: emptyArray()
        val partiallyOccludedBy = layer?.partiallyOccludedBy ?: emptyArray()
        Truth.assertWithMessage("Layer $layerName should not be occluded")
                .that(occludedBy).isEmpty()
        Truth.assertWithMessage("Layer $layerName should be partially occluded")
                .that(partiallyOccludedBy).isNotEmpty()
        Truth.assertWithMessage("Layer $layerName should be partially occluded")
                .that(partiallyOccludedBy.joinToString())
                .contains("Splash Screen com.android.server.wm.flicker.testapp#0 buffer:w:1440, " +
                        "h:3040, stride:1472, format:1 frame#1 visible:" +
                        "SkRegion((346,1583,1094,2839))")
    }

    @Test
    fun exceptionContainsDebugInfo() {
        val layersTraceEntries = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(layersTraceEntries)
                    .isEmpty()
        }
        assertThatErrorContainsDebugInfo(error, withBlameEntry = false)
    }

    @Test
    fun canSlice() {
        val trace = readLayerTraceFromFile("layers_trace_openchrome.pb")
        val splitlayersTrace = trace.slice(71607477186189, 71607812120180)

        Truth.assertThat(splitlayersTrace).isNotEmpty()

        Truth.assertThat(splitlayersTrace.entries.first().timestamp).isEqualTo(71607477186189)
        Truth.assertThat(splitlayersTrace.entries.last().timestamp).isEqualTo(71607812120180)
    }

    @Test
    fun canSlice_wrongTimestamps() {
        val trace = readLayerTraceFromFile("layers_trace_openchrome.pb")
        val splitLayersTrace = trace.slice(9213763541297, 9215895891561)

        Truth.assertThat(splitLayersTrace).isEmpty()
    }

    @Test
    fun canSlice_allBefore() {
        testSlice(0L, mockTraceForSliceTests.first().timestamp - 1, listOf())
    }

    @Test
    fun canSlice_allAfter() {
        val from = mockTraceForSliceTests.last().timestamp + 5
        val to = mockTraceForSliceTests.last().timestamp + 20
        val splitLayersTrace = mockTraceForSliceTests.slice(from, to)
        Truth.assertThat(splitLayersTrace).isEmpty()

        val splitLayersTraceWithInitialEntry = mockTraceForSliceTests
            .slice(from, to, addInitialEntry = true)
        Truth.assertThat(splitLayersTraceWithInitialEntry).hasSize(1)
        Truth.assertThat(splitLayersTraceWithInitialEntry.first().timestamp)
            .isEqualTo(mockTraceForSliceTests.last().timestamp)
    }

    @Test
    fun canSlice_inMiddle() {
        testSlice(15L, 25L, listOf(15L, 18L, 25L))
    }

    @Test
    fun canSlice_fromBeforeFirstEntryToMiddle() {
        testSlice(
            mockTraceForSliceTests.first().timestamp - 1, 27L,
            listOf(5L, 8L, 15L, 18L, 25L, 27L)
        )
    }

    @Test
    fun canSlice_fromMiddleToAfterLastEntry() {
        testSlice(18L, mockTraceForSliceTests.last().timestamp + 5,
            listOf(18L, 25L, 27L, 30L)
        )
    }

    @Test
    fun canSlice_fromBeforeToAfterLastEntry() {
        testSlice(
            mockTraceForSliceTests.first().timestamp - 1,
            mockTraceForSliceTests.last().timestamp + 1,
            mockTraceForSliceTests.map { it.timestamp }
        )
    }

    @Test
    fun canSlice_fromExactStartToAfterLastEntry() {
        testSlice(
            mockTraceForSliceTests.first().timestamp,
            mockTraceForSliceTests.last().timestamp + 1,
            mockTraceForSliceTests.map { it.timestamp }
        )
    }

    @Test
    fun canSlice_fromExactStartToExactEnd() {
        testSlice(
            mockTraceForSliceTests.first().timestamp,
            mockTraceForSliceTests.last().timestamp,
            mockTraceForSliceTests.map { it.timestamp }
        )
    }

    @Test
    fun canSlice_fromExactStartToMiddle() {
        testSlice(
            mockTraceForSliceTests.first().timestamp,
            18L,
            listOf(5L, 8L, 15L, 18L)
        )
    }

    @Test
    fun canSlice_fromMiddleToExactEnd() {
        testSlice(
            18L,
            mockTraceForSliceTests.last().timestamp,
            listOf(18L, 25L, 27L, 30L)
        )
    }

    @Test
    fun canSlice_fromBeforeToExactEnd() {
        testSlice(
            mockTraceForSliceTests.first().timestamp - 1,
            mockTraceForSliceTests.last().timestamp,
            mockTraceForSliceTests.map { it.timestamp }
        )
    }

    @Test
    fun canSlice_sameStartAndEnd() {
        testSlice(15L, 15L, listOf(15L))
    }

    private fun testSlice(from: Long, to: Long, expected: List<Long>) {
        require(from <= to) { "`from` not before `to`" }
        val fromBefore = from < mockTraceForSliceTests.first().timestamp
        val fromAfter = from < mockTraceForSliceTests.first().timestamp

        val toBefore = to < mockTraceForSliceTests.first().timestamp
        val toAfter = mockTraceForSliceTests.last().timestamp < to

        require(fromBefore || fromAfter ||
            mockTraceForSliceTests.map { it.timestamp }.contains(from)) {
            "`from` need to be in the trace or before or after all entries"
        }
        require(toBefore || toAfter ||
            mockTraceForSliceTests.map { it.timestamp }.contains(to)) {
            "`to` need to be in the trace or before or after all entries"
        }

        testSliceWithOutInitialEntry(from, to, expected)
        if (!fromAfter) {
            testSliceWithOutInitialEntry(from - 1, to, expected)
            testSliceWithOutInitialEntry(from - 1, to + 1, expected)
        }
        if (!toBefore) {
            testSliceWithOutInitialEntry(from, to + 1, expected)
        }

        testSliceWithInitialEntry(from, to, expected)
        if (!fromBefore) {
            if (from < to) {
                testSliceWithInitialEntry(from + 1, to, expected)
            }
            testSliceWithInitialEntry(from + 1, to + 1, expected)
        }
        if (!toBefore) {
            testSliceWithInitialEntry(from, to + 1, expected)
        }
    }

    private fun testSliceWithOutInitialEntry(from: Long, to: Long, expected: List<Long>) {
        val splitLayersTrace = mockTraceForSliceTests.slice(from, to)
        Truth.assertThat(splitLayersTrace.map { it.timestamp }).isEqualTo(expected)
    }

    private fun testSliceWithInitialEntry(from: Long, to: Long, expected: List<Long>) {
        val splitLayersTraceWithStartEntry = mockTraceForSliceTests
            .slice(from, to, addInitialEntry = true)
        Truth.assertThat(splitLayersTraceWithStartEntry.map { it.timestamp }).isEqualTo(expected)
    }

    companion object {
        val mockTraceForSliceTests = MockLayersTraceBuilder(entries = mutableListOf(
            MockLayerTraceEntryBuilder(timestamp = 5),
            MockLayerTraceEntryBuilder(timestamp = 8),
            MockLayerTraceEntryBuilder(timestamp = 15),
            MockLayerTraceEntryBuilder(timestamp = 18),
            MockLayerTraceEntryBuilder(timestamp = 25),
            MockLayerTraceEntryBuilder(timestamp = 27),
            MockLayerTraceEntryBuilder(timestamp = 30),
        )).build()
    }
}
