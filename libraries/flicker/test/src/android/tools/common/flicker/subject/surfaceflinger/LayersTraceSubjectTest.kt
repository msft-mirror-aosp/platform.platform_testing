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
import android.tools.TestComponents
import android.tools.assertFailureFact
import android.tools.assertThrows
import android.tools.common.Cache
import android.tools.common.datatypes.Region
import android.tools.common.datatypes.component.ComponentNameMatcher
import android.tools.common.flicker.subject.FlickerSubjectException
import android.tools.common.flicker.subject.layers.LayersTraceSubject
import android.tools.common.traces.surfaceflinger.LayersTrace
import android.tools.readLayerTraceFromFile
import androidx.test.filters.FlakyTest
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [LayersTraceSubject] tests. To run this test: `atest
 * FlickerLibTest:LayersTraceSubjectTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayersTraceSubjectTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun exceptionContainsDebugInfo() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_launch_split_screen.pb", legacyTrace = true)
        val error =
            assertThrows<AssertionError> { LayersTraceSubject(layersTraceEntries).isEmpty() }
        Truth.assertThat(error).hasMessageThat().contains("Trace start")
        Truth.assertThat(error).hasMessageThat().contains("Trace end")
    }

    @Test
    fun testCanDetectEmptyRegionFromLayerTrace() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_emptyregion.pb", legacyTrace = true)
        val failure =
            assertThrows<FlickerSubjectException> {
                LayersTraceSubject(layersTraceEntries)
                    .visibleRegion()
                    .coversAtLeast(DISPLAY_REGION)
                    .forAllEntries()
                error("Assertion should not have passed")
            }
        assertFailureFact(failure, "Region to test").contains(DISPLAY_REGION.toString())
        assertFailureFact(failure, "Uncovered region").contains("SkRegion((0,1440,1440,2880))")
    }

    @Test
    fun testCanInspectBeginning() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_launch_split_screen.pb", legacyTrace = true)
        LayersTraceSubject(layersTraceEntries)
            .first()
            .isVisible(ComponentNameMatcher.NAV_BAR)
            .notContains(TestComponents.DOCKER_STACK_DIVIDER)
            .isVisible(TestComponents.LAUNCHER)
    }

    @Test
    fun testCanInspectEnd() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_launch_split_screen.pb", legacyTrace = true)
        LayersTraceSubject(layersTraceEntries)
            .last()
            .isVisible(ComponentNameMatcher.NAV_BAR)
            .isVisible(TestComponents.DOCKER_STACK_DIVIDER)
    }

    @Test
    fun testAssertionsOnRange() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_launch_split_screen.pb", legacyTrace = true)

        LayersTraceSubject(layersTraceEntries)
            .isVisible(ComponentNameMatcher.NAV_BAR)
            .isInvisible(TestComponents.DOCKER_STACK_DIVIDER)
            .forSystemUpTimeRange(90480846872160L, 90480994138424L)

        LayersTraceSubject(layersTraceEntries)
            .isVisible(ComponentNameMatcher.NAV_BAR)
            .isVisible(TestComponents.DOCKER_STACK_DIVIDER)
            .forSystemUpTimeRange(90491795074136L, 90493757372977L)
    }

    @Test
    fun testCanDetectChangingAssertions() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_launch_split_screen.pb", legacyTrace = true)
        LayersTraceSubject(layersTraceEntries)
            .isVisible(ComponentNameMatcher.NAV_BAR)
            .notContains(TestComponents.DOCKER_STACK_DIVIDER)
            .then()
            .isVisible(ComponentNameMatcher.NAV_BAR)
            .isInvisible(TestComponents.DOCKER_STACK_DIVIDER)
            .then()
            .isVisible(ComponentNameMatcher.NAV_BAR)
            .isVisible(TestComponents.DOCKER_STACK_DIVIDER)
            .forAllEntries()
    }

    @FlakyTest
    @Test
    fun testCanDetectIncorrectVisibilityFromLayerTrace() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_invalid_layer_visibility.pb", legacyTrace = true)
        val error =
            assertThrows<FlickerSubjectException> {
                LayersTraceSubject(layersTraceEntries)
                    .isVisible(TestComponents.SIMPLE_APP)
                    .then()
                    .isInvisible(TestComponents.SIMPLE_APP)
                    .forAllEntries()
            }

        Truth.assertThat(error)
            .hasMessageThat()
            .contains("layers_trace_invalid_layer_visibility.pb")
        Truth.assertThat(error).hasMessageThat().contains("2d22h13m14s303ms")
        Truth.assertThat(error).hasMessageThat().contains("!isVisible")
        Truth.assertThat(error)
            .hasMessageThat()
            .contains(
                "com.android.server.wm.flicker.testapp/" +
                    "com.android.server.wm.flicker.testapp.SimpleActivity#0 is visible"
            )
    }

    @Test
    fun testCanDetectInvalidVisibleLayerForMoreThanOneConsecutiveEntry() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_invalid_visible_layers.pb", legacyTrace = true)
        val error =
            assertThrows<FlickerSubjectException> {
                LayersTraceSubject(layersTraceEntries)
                    .visibleLayersShownMoreThanOneConsecutiveEntry()
                    .forAllEntries()
                error("Assertion should not have passed")
            }

        Truth.assertThat(error).hasMessageThat().contains("2d18h35m56s397ms")
        Truth.assertThat(error).hasMessageThat().contains("StatusBar#0")
        Truth.assertThat(error).hasMessageThat().contains("is not visible for 2 entries")
    }

    private fun testCanDetectVisibleLayersMoreThanOneConsecutiveEntry(trace: LayersTrace) {
        LayersTraceSubject(trace).visibleLayersShownMoreThanOneConsecutiveEntry().forAllEntries()
    }

    @Test
    fun testCanDetectVisibleLayersMoreThanOneConsecutiveEntry() {
        testCanDetectVisibleLayersMoreThanOneConsecutiveEntry(
            readLayerTraceFromFile("layers_trace_snapshot_visible.pb", legacyTrace = true)
        )
    }

    @Test
    fun testCanIgnoreLayerEqualNameInVisibleLayersMoreThanOneConsecutiveEntry() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_invalid_visible_layers.pb", legacyTrace = true)
        LayersTraceSubject(layersTraceEntries)
            .visibleLayersShownMoreThanOneConsecutiveEntry(listOf(ComponentNameMatcher.STATUS_BAR))
            .forAllEntries()
    }

    @Test
    fun testCanIgnoreLayerShorterNameInVisibleLayersMoreThanOneConsecutiveEntry() {
        val layersTraceEntries =
            readLayerTraceFromFile("one_visible_layer_launcher_trace.pb", legacyTrace = true)
        val launcherComponent =
            ComponentNameMatcher(
                "com.google.android.apps.nexuslauncher",
                "com.google.android.apps.nexuslauncher.NexusLauncherActivity#1"
            )
        LayersTraceSubject(layersTraceEntries)
            .visibleLayersShownMoreThanOneConsecutiveEntry(listOf(launcherComponent))
            .forAllEntries()
    }

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

    @Test
    fun testCanDetectRootLayer() {
        detectRootLayer("layers_trace_root.pb", legacyTrace = true)
    }

    @Test
    fun testCanDetectRootLayerAOSP() {
        detectRootLayer("layers_trace_root_aosp.pb", legacyTrace = true)
    }

    @Test
    fun canTestLayerOccludedBySplashScreenLayerIsNotVisible() {
        val trace = readLayerTraceFromFile("layers_trace_occluded.pb", legacyTrace = true)
        val entry =
            LayersTraceSubject(trace)
                .getEntryBySystemUpTime(1700382131522L, byElapsedTimestamp = true)
        entry.isInvisible(TestComponents.SIMPLE_APP)
        entry.isVisible(ComponentNameMatcher.SPLASH_SCREEN)
    }

    @Test
    fun testCanDetectLayerExpanding() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_openchrome.pb", legacyTrace = true)
        val animation =
            LayersTraceSubject(layersTraceEntries).layers("animation-leash of app_transition#0")
        // Obtain the area of each layer and checks if the next area is
        // greater or equal to the previous one
        val areas =
            animation.map {
                val region = it.layer?.visibleRegion ?: Region()
                val area = region.width * region.height
                area
            }
        val expanding = areas.zipWithNext { currentArea, nextArea -> nextArea >= currentArea }

        Truth.assertWithMessage("Animation leash should be expanding")
            .that(expanding.all { it })
            .isTrue()
    }

    @Test
    fun checkVisibleRegionAppMinusPipLayer() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_pip_wmshell.pb", legacyTrace = true)
        val subject = LayersTraceSubject(layersTraceEntries).last()

        try {
            subject.visibleRegion(TestComponents.FIXED_APP).coversExactly(DISPLAY_REGION_ROTATED)
            error(
                "Layer is partially covered by a Pip layer and should not cover the device screen"
            )
        } catch (e: AssertionError) {
            val pipRegion = subject.visibleRegion(TestComponents.PIP_APP).region
            val expectedWithoutPip = DISPLAY_REGION_ROTATED.minus(pipRegion)
            subject.visibleRegion(TestComponents.FIXED_APP).coversExactly(expectedWithoutPip)
        }
    }

    @Test
    fun checkVisibleRegionAppPlusPipLayer() {
        val layersTraceEntries =
            readLayerTraceFromFile("layers_trace_pip_wmshell.pb", legacyTrace = true)
        val subject = LayersTraceSubject(layersTraceEntries).last()
        val pipRegion = subject.visibleRegion(TestComponents.PIP_APP).region
        subject
            .visibleRegion(TestComponents.FIXED_APP)
            .plus(pipRegion)
            .coversExactly(DISPLAY_REGION_ROTATED)
    }

    @Test
    fun checkCanDetectSplashScreen() {
        val trace = readLayerTraceFromFile("layers_trace_splashscreen.pb", legacyTrace = true)
        val newLayer =
            ComponentNameMatcher(
                "com.android.server.wm.flicker.testapp",
                "com.android.server.wm.flicker.testapp.SimpleActivity"
            )
        LayersTraceSubject(trace)
            .isVisible(TestComponents.LAUNCHER)
            .then()
            .isSplashScreenVisibleFor(TestComponents.SIMPLE_APP, isOptional = false)
            .then()
            .isVisible(TestComponents.SIMPLE_APP)
            .forAllEntries()

        val failure =
            assertThrows<FlickerSubjectException> {
                LayersTraceSubject(trace)
                    .isVisible(TestComponents.LAUNCHER)
                    .then()
                    .isVisible(TestComponents.SIMPLE_APP)
                    .forAllEntries()
            }
        Truth.assertThat(failure).hasMessageThat().contains("Is Invisible")
    }

    @Test
    fun checkCanDetectMissingSplashScreen() {
        val trace = readLayerTraceFromFile("layers_trace_splashscreen.pb", legacyTrace = true)
        val newLayer =
            ComponentNameMatcher(
                "com.android.server.wm.flicker.testapp",
                "com.android.server.wm.flicker.testapp.SimpleActivity"
            )

        // No splashscreen because no matching activity record
        var failure =
            assertThrows<FlickerSubjectException> {
                LayersTraceSubject(trace)
                    .first()
                    .isSplashScreenVisibleFor(TestComponents.SIMPLE_APP)
            }
        Truth.assertThat(failure).hasMessageThat().contains("Could not find Activity Record layer")

        // No splashscreen for target activity record
        failure =
            assertThrows<FlickerSubjectException> {
                LayersTraceSubject(trace).first().isSplashScreenVisibleFor(TestComponents.LAUNCHER)
            }
        Truth.assertThat(failure).hasMessageThat().contains("No splash screen visible")
    }

    companion object {
        private val DISPLAY_REGION = Region.from(0, 0, 1440, 2880)
        private val DISPLAY_REGION_ROTATED = Region.from(0, 0, 2160, 1080)
        private const val SHELL_APP_PACKAGE = "com.android.wm.shell.flicker.testapp"
        private val FIXED_APP =
            ComponentNameMatcher(SHELL_APP_PACKAGE, "$SHELL_APP_PACKAGE.FixedActivity")
        private val PIP_APP =
            ComponentNameMatcher(SHELL_APP_PACKAGE, "$SHELL_APP_PACKAGE.PipActivity")

        @ClassRule @JvmField val initRule = InitRule()
    }
}
