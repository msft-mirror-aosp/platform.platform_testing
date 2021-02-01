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

package com.android.server.wm.flicker

import android.graphics.Region
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [LayerTraceEntrySubject] tests. To run this test: `atest
 * FlickerLibTest:LayersTraceTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayerTraceEntrySubjectTest {
    @Test
    fun exceptionContainsDebugInfo() {
        val layersTraceEntries = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(layersTraceEntries)
                .first()
                .exists("ImaginaryLayer")
        }
        Truth.assertThat(error).hasMessageThat().contains("Trace:")
        Truth.assertThat(error).hasMessageThat().contains("Path: ")
        Truth.assertThat(error).hasMessageThat().contains("Entry:")
    }

    @Test
    fun testCanInspectBeginning() {
        val layersTraceEntries = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        LayerTraceEntrySubject.assertThat(layersTraceEntries.entries.first())
            .isVisible("NavigationBar0#0")
            .notExists("DockedStackDivider#0")
            .isVisible("NexusLauncherActivity#0")
    }

    @Test
    fun testCanInspectEnd() {
        val layersTraceEntries = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        LayerTraceEntrySubject.assertThat(layersTraceEntries.entries.last())
            .isVisible("NavigationBar0#0")
            .isVisible("DockedStackDivider#0")
    }

    // b/75276931
    @Test
    fun canDetectUncoveredRegion() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedRegion = Region(0, 0, 1440, 2960)
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(trace).entry(935346112030)
                .coversAtLeastRegion(expectedRegion)
        }
        assertFailure(error)
            .factValue("Region to test")
            .contains("SkRegion((0,0,1440,2960))")

        assertFailure(error)
            .factValue("Uncovered region")
            .contains("SkRegion((0,171,1440,2960))")
    }

    // Visible region tests
    @Test
    fun canTestLayerVisibleRegion_layerDoesNotExist() {
        val imaginaryLayer = "ImaginaryLayer"
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedVisibleRegion = Region(0, 0, 1, 1)
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(trace).entry(937229257165)
                .hasVisibleRegion(imaginaryLayer, expectedVisibleRegion)
        }
        assertFailure(error)
            .factValue("Could not find")
            .isEqualTo(imaginaryLayer)
    }

    @Test
    fun canTestLayerVisibleRegion_layerDoesNotHaveExpectedVisibleRegion() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedVisibleRegion = Region(0, 0, 1, 1)
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(trace).entry(937126074082)
                .hasVisibleRegion("DockedStackDivider#0", expectedVisibleRegion)
        }
        assertFailure(error)
            .factValue("Is Invisible")
            .contains("activeBuffer=null type != ColorLayer flags=1 (FLAG_HIDDEN set) " +
                "visible region is empty")
    }

    @Test
    fun canTestLayerVisibleRegion_layerIsHiddenByParent() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedVisibleRegion = Region(0, 0, 1, 1)
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(trace).entry(935346112030)
                .hasVisibleRegion("SimpleActivity#0", expectedVisibleRegion)
        }
        assertFailure(error)
            .factValue("Hidden by parent")
            .contains("b5cf8d3 com.android.server.wm.flicker.testapp")
    }

    @Test
    fun canTestLayerVisibleRegion_incorrectRegionSize() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedVisibleRegion = Region(0, 0, 1440, 99)
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(trace).entry(937126074082)
                .hasVisibleRegion("StatusBar", expectedVisibleRegion)
        }
        assertFailure(error)
            .factValue("expected")
            .contains("SkRegion((0,0,1440,99))")
    }

    @Test
    fun canTestLayerVisibleRegion() {
        val trace = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        val expectedVisibleRegion = Region(0, 0, 1080, 145)
        LayersTraceSubject.assertThat(trace).entry(90480846872160)
            .hasVisibleRegion("StatusBar", expectedVisibleRegion)
    }

    @Test
    fun canTestLayerVisibleRegion_layerIsNotVisible() {
        val trace = readLayerTraceFromFile("layers_trace_invalid_layer_visibility.pb")
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(trace).entry(252794268378458)
                .isVisible("com.android.server.wm.flicker.testapp")
        }
        assertFailure(error)
            .factValue("Is Invisible")
            .contains("type != ColorLayer visible region is empty")
    }
}