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

import com.android.server.wm.flicker.TestComponents
import com.android.server.wm.flicker.assertFailure
import com.android.server.wm.flicker.assertThatErrorContainsDebugInfo
import com.android.server.wm.flicker.assertThrows
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.readLayerTraceFromFile
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.traces.common.Cache
import com.android.server.wm.traces.common.ComponentMatcher
import com.android.server.wm.traces.common.region.Region
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [LayerTraceEntrySubject] tests. To run this test: `atest
 * FlickerLibTest:LayersTraceTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayerTraceEntrySubjectTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun exceptionContainsDebugInfo() {
        val layersTraceEntries = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(layersTraceEntries)
                .first()
                .visibleRegion(TestComponents.IMAGINARY)
        }
        assertThatErrorContainsDebugInfo(error)
        Truth.assertThat(error)
            .hasMessageThat()
            .contains(TestComponents.IMAGINARY.classNames.first())
        Truth.assertThat(error).hasMessageThat().contains(FlickerSubject.ASSERTION_TAG)
    }

    @Test
    fun testCanInspectBeginning() {
        val layersTraceEntries = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        LayerTraceEntrySubject.assertThat(layersTraceEntries.entries.first())
            .isVisible(ComponentMatcher.NAV_BAR)
            .notContains(TestComponents.DOCKER_STACK_DIVIDER)
            .isVisible(TestComponents.LAUNCHER)
    }

    @Test
    fun testCanInspectEnd() {
        val layersTraceEntries = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        LayerTraceEntrySubject.assertThat(layersTraceEntries.entries.last())
            .isVisible(ComponentMatcher.NAV_BAR)
            .isVisible(TestComponents.DOCKER_STACK_DIVIDER)
    }

    // b/75276931
    @Test
    fun canDetectUncoveredRegion() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedRegion = Region.from(0, 0, 1440, 2960)
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(trace).entry(935346112030)
                .visibleRegion()
                .coversAtLeast(expectedRegion)
        }
        assertFailure(error)
            .factValue("Region to test")
            .contains("SkRegion((0,0,1440,2960))")

        assertFailure(error)
            .factValue("Uncovered region")
            .contains("SkRegion((0,1440,1440,2960))")
    }

    // Visible region tests
    @Test
    fun canTestLayerVisibleRegion_layerDoesNotExist() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedVisibleRegion = Region.from(0, 0, 1, 1)
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(trace).entry(937229257165)
                .visibleRegion(TestComponents.IMAGINARY)
                .coversExactly(expectedVisibleRegion)
        }
        assertFailure(error)
            .factValue("Could not find layers")
                .contains(TestComponents.IMAGINARY.toWindowName())
    }

    @Test
    fun canTestLayerVisibleRegion_layerDoesNotHaveExpectedVisibleRegion() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedVisibleRegion = Region.from(0, 0, 1, 1)
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(trace).entry(937126074082)
                .visibleRegion(TestComponents.DOCKER_STACK_DIVIDER)
                .coversExactly(expectedVisibleRegion)
        }
        assertFailure(error)
            .factValue("Covered region")
            .contains("SkRegion()")
    }

    @Test
    fun canTestLayerVisibleRegion_layerIsHiddenByParent() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedVisibleRegion = Region.from(0, 0, 1, 1)
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(trace).entry(935346112030)
                .visibleRegion(TestComponents.SIMPLE_APP)
                .coversExactly(expectedVisibleRegion)
        }
        assertFailure(error)
            .factValue("Covered region")
            .contains("SkRegion()")
    }

    @Test
    fun canTestLayerVisibleRegion_incorrectRegionSize() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedVisibleRegion = Region.from(0, 0, 1440, 99)
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(trace).entry(937126074082)
                .visibleRegion(ComponentMatcher.STATUS_BAR)
                .coversExactly(expectedVisibleRegion)
        }
        assertFailure(error)
            .factValue("Region to test")
            .contains("SkRegion((0,0,1440,99))")
    }

    @Test
    fun canTestLayerVisibleRegion() {
        val trace = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        val expectedVisibleRegion = Region.from(0, 0, 1080, 145)
        LayersTraceSubject.assertThat(trace).entry(90480846872160)
            .visibleRegion(ComponentMatcher.STATUS_BAR)
            .coversExactly(expectedVisibleRegion)
    }

    @Test
    fun canTestLayerVisibleRegion_layerIsNotVisible() {
        val trace = readLayerTraceFromFile("layers_trace_invalid_layer_visibility.pb")
        val error = assertThrows(AssertionError::class.java) {
            LayersTraceSubject.assertThat(trace).entry(252794268378458)
                .isVisible(TestComponents.SIMPLE_APP)
        }
        assertFailure(error)
            .factValue("Is Invisible", 0)
            .contains("Bounds is 0x0")
    }

    @Test
    fun testCanParseWithoutHWC_visibleRegion() {
        val layersTrace = readLayerTraceFromFile("layers_trace_no_hwc_composition.pb")
        val entry = LayersTraceSubject.assertThat(layersTrace)
            .entry(238517209878020)

        entry.visibleRegion(useCompositionEngineRegionOnly = false)
            .coversExactly(Region.from(0, 0, 1440, 2960))

        entry.visibleRegion(ComponentMatcher.IME,
            useCompositionEngineRegionOnly = false)
            .coversExactly(Region.from(0, 171, 1440, 2960))
    }
}
