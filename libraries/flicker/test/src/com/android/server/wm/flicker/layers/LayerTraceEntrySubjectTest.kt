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
import com.android.server.wm.flicker.utils.MockLayerBuilder
import com.android.server.wm.flicker.utils.MockLayerTraceEntryBuilder
import com.android.server.wm.traces.common.Cache
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.OrComponentMatcher
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.region.Region
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [LayerTraceEntrySubject] tests. To run this test: `atest
 * FlickerLibTest:LayerTraceEntrySubjectTest`
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
        val error =
            assertThrows(AssertionError::class.java) {
                LayersTraceSubject.assertThat(layersTraceEntries)
                    .first()
                    .visibleRegion(TestComponents.IMAGINARY)
            }
        assertThatErrorContainsDebugInfo(error)
        Truth.assertThat(error).hasMessageThat().contains(TestComponents.IMAGINARY.className)
        Truth.assertThat(error).hasMessageThat().contains(FlickerSubject.ASSERTION_TAG)
    }

    @Test
    fun testCanInspectBeginning() {
        val layersTraceEntries = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        LayerTraceEntrySubject.assertThat(layersTraceEntries.entries.first())
            .isVisible(ComponentNameMatcher.NAV_BAR)
            .notContains(TestComponents.DOCKER_STACK_DIVIDER)
            .isVisible(TestComponents.LAUNCHER)
    }

    @Test
    fun testCanInspectEnd() {
        val layersTraceEntries = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        LayerTraceEntrySubject.assertThat(layersTraceEntries.entries.last())
            .isVisible(ComponentNameMatcher.NAV_BAR)
            .isVisible(TestComponents.DOCKER_STACK_DIVIDER)
    }

    // b/75276931
    @Test
    fun canDetectUncoveredRegion() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedRegion = Region.from(0, 0, 1440, 2960)
        val error =
            assertThrows(AssertionError::class.java) {
                LayersTraceSubject.assertThat(trace)
                    .entry(935346112030)
                    .visibleRegion()
                    .coversAtLeast(expectedRegion)
            }
        assertFailure(error).factValue("Region to test").contains("SkRegion((0,0,1440,2960))")

        assertFailure(error).factValue("Uncovered region").contains("SkRegion((0,1440,1440,2960))")
    }

    // Visible region tests
    @Test
    fun canTestLayerVisibleRegion_layerDoesNotExist() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedVisibleRegion = Region.from(0, 0, 1, 1)
        val error =
            assertThrows(AssertionError::class.java) {
                LayersTraceSubject.assertThat(trace)
                    .entry(937229257165)
                    .visibleRegion(TestComponents.IMAGINARY)
                    .coversExactly(expectedVisibleRegion)
            }
        assertFailure(error)
            .factValue("Could not find layers")
            .contains(TestComponents.IMAGINARY.toWindowIdentifier())
    }

    @Test
    fun canTestLayerVisibleRegion_layerDoesNotHaveExpectedVisibleRegion() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedVisibleRegion = Region.from(0, 0, 1, 1)
        val error =
            assertThrows(AssertionError::class.java) {
                LayersTraceSubject.assertThat(trace)
                    .entry(937126074082)
                    .visibleRegion(TestComponents.DOCKER_STACK_DIVIDER)
                    .coversExactly(expectedVisibleRegion)
            }
        assertFailure(error).factValue("Covered region").contains("SkRegion()")
    }

    @Test
    fun canTestLayerVisibleRegion_layerIsHiddenByParent() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedVisibleRegion = Region.from(0, 0, 1, 1)
        val error =
            assertThrows(AssertionError::class.java) {
                LayersTraceSubject.assertThat(trace)
                    .entry(935346112030)
                    .visibleRegion(TestComponents.SIMPLE_APP)
                    .coversExactly(expectedVisibleRegion)
            }
        assertFailure(error).factValue("Covered region").contains("SkRegion()")
    }

    @Test
    fun canTestLayerVisibleRegion_incorrectRegionSize() {
        val trace = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        val expectedVisibleRegion = Region.from(0, 0, 1440, 99)
        val error =
            assertThrows(AssertionError::class.java) {
                LayersTraceSubject.assertThat(trace)
                    .entry(937126074082)
                    .visibleRegion(ComponentNameMatcher.STATUS_BAR)
                    .coversExactly(expectedVisibleRegion)
            }
        assertFailure(error).factValue("Region to test").contains("SkRegion((0,0,1440,99))")
    }

    @Test
    fun canTestLayerVisibleRegion() {
        val trace = readLayerTraceFromFile("layers_trace_launch_split_screen.pb")
        val expectedVisibleRegion = Region.from(0, 0, 1080, 145)
        LayersTraceSubject.assertThat(trace)
            .entry(90480846872160)
            .visibleRegion(ComponentNameMatcher.STATUS_BAR)
            .coversExactly(expectedVisibleRegion)
    }

    @Test
    fun canTestLayerVisibleRegion_layerIsNotVisible() {
        val trace = readLayerTraceFromFile("layers_trace_invalid_layer_visibility.pb")
        val error =
            assertThrows(AssertionError::class.java) {
                LayersTraceSubject.assertThat(trace)
                    .entry(252794268378458)
                    .isVisible(TestComponents.SIMPLE_APP)
            }
        assertFailure(error).factValue("Invisibility reason", 1).contains("Bounds is 0x0")
    }

    @Test
    fun orComponentMatcher_visibility_oneVisibleOtherInvisible() {
        val app1Name = "com.simple.test.app1"
        val app2Name = "com.simple.test.app2"

        val layerTraceEntry =
            MockLayerTraceEntryBuilder()
                .addDisplay(
                    rootLayers =
                        listOf(
                            MockLayerBuilder(app1Name)
                                .setContainerLayer()
                                .addChild(MockLayerBuilder(app1Name).setVisible()),
                            MockLayerBuilder(app2Name)
                                .setContainerLayer()
                                .addChild(MockLayerBuilder(app2Name).setInvisible()),
                        )
                )
                .build()

        val subject = LayerTraceEntrySubject.assertThat(layerTraceEntry)
        val component =
            OrComponentMatcher(
                arrayOf(ComponentNameMatcher(app1Name), ComponentNameMatcher(app2Name))
            )

        subject.isVisible(ComponentNameMatcher(app1Name))
        subject.isInvisible(ComponentNameMatcher(app2Name))

        subject.isInvisible(component)
        subject.isVisible(component)
    }

    @Test
    fun orComponentMatcher_visibility_oneVisibleOtherMissing() {
        val app1Name = "com.simple.test.app1"
        val app2Name = "com.simple.test.app2"

        val layerTraceEntry =
            MockLayerTraceEntryBuilder()
                .addDisplay(
                    rootLayers =
                        listOf(
                            MockLayerBuilder(app1Name)
                                .setContainerLayer()
                                .addChild(MockLayerBuilder(app1Name).setVisible())
                        )
                )
                .build()

        val subject = LayerTraceEntrySubject.assertThat(layerTraceEntry)
        val component =
            OrComponentMatcher(
                arrayOf(ComponentNameMatcher(app1Name), ComponentNameMatcher(app2Name))
            )

        subject.isVisible(ComponentNameMatcher(app1Name))
        subject.notContains(ComponentNameMatcher(app2Name))

        subject.isInvisible(component)
        subject.isVisible(component)
    }

    @Test
    fun canUseOrComponentMatcher_visibility_allVisible() {
        val app1Name = "com.simple.test.app1"
        val app2Name = "com.simple.test.app2"

        val layerTraceEntry =
            MockLayerTraceEntryBuilder()
                .addDisplay(
                    rootLayers =
                        listOf(
                            MockLayerBuilder(app1Name)
                                .setContainerLayer()
                                .setAbsoluteBounds(Rect.from(0, 0, 200, 200))
                                .addChild(MockLayerBuilder("$app1Name child").setVisible()),
                            MockLayerBuilder(app2Name)
                                .setContainerLayer()
                                .setAbsoluteBounds(Rect.from(200, 200, 400, 400))
                                .addChild(MockLayerBuilder("$app2Name child").setVisible()),
                        )
                )
                .build()

        val subject = LayerTraceEntrySubject.assertThat(layerTraceEntry)
        val component =
            OrComponentMatcher(
                arrayOf(ComponentNameMatcher(app1Name), ComponentNameMatcher(app2Name))
            )

        subject.isVisible(ComponentNameMatcher(app1Name))
        subject.isVisible(ComponentNameMatcher(app2Name))

        assertThrows(AssertionError::class.java) { subject.isInvisible(component) }
        subject.isVisible(component)
    }

    @Test
    fun canUseOrComponentMatcher_contains_withOneExists() {
        val app1Name = "com.simple.test.app1"
        val app2Name = "com.simple.test.app2"

        val layerTraceEntry =
            MockLayerTraceEntryBuilder()
                .addDisplay(
                    rootLayers =
                        listOf(
                            MockLayerBuilder(app1Name)
                                .setContainerLayer()
                                .addChild(MockLayerBuilder(app1Name))
                        )
                )
                .build()

        val subject = LayerTraceEntrySubject.assertThat(layerTraceEntry)
        val component =
            OrComponentMatcher(
                arrayOf(ComponentNameMatcher(app1Name), ComponentNameMatcher(app2Name))
            )

        subject.contains(ComponentNameMatcher(app1Name))
        subject.notContains(ComponentNameMatcher(app2Name))

        subject.notContains(component)
        subject.contains(component)
    }

    @Test
    fun canUseOrComponentMatcher_contains_withNoneExists() {
        val app1Name = "com.simple.test.app1"
        val app2Name = "com.simple.test.app2"

        val layerTraceEntry = MockLayerTraceEntryBuilder().addDisplay(rootLayers = listOf()).build()

        val subject = LayerTraceEntrySubject.assertThat(layerTraceEntry)
        val component =
            OrComponentMatcher(
                arrayOf(ComponentNameMatcher(app1Name), ComponentNameMatcher(app2Name))
            )

        subject.notContains(ComponentNameMatcher(app1Name))
        subject.notContains(ComponentNameMatcher(app2Name))

        subject.notContains(component)
        assertThrows(AssertionError::class.java) { subject.contains(component) }
    }

    @Test
    fun canUseOrComponentMatcher_contains_withBothExists() {
        val app1Name = "com.simple.test.app1"
        val app2Name = "com.simple.test.app2"

        val layerTraceEntry =
            MockLayerTraceEntryBuilder()
                .addDisplay(
                    rootLayers =
                        listOf(
                            MockLayerBuilder(app1Name)
                                .setContainerLayer()
                                .addChild(MockLayerBuilder(app1Name)),
                            MockLayerBuilder(app2Name)
                                .setContainerLayer()
                                .addChild(MockLayerBuilder(app2Name)),
                        )
                )
                .build()

        val subject = LayerTraceEntrySubject.assertThat(layerTraceEntry)
        val component =
            OrComponentMatcher(
                arrayOf(ComponentNameMatcher(app1Name), ComponentNameMatcher(app2Name))
            )

        subject.contains(ComponentNameMatcher(app1Name))
        subject.contains(ComponentNameMatcher(app2Name))

        assertThrows(AssertionError::class.java) { subject.notContains(component) }
        subject.contains(component)
    }
}
