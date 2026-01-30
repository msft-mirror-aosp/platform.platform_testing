/*
 * Copyright (C) 2026 The Android Open Source Project
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

package android.tools.flicker.subject.surfaceflinger

import android.tools.CleanFlickerEnvironmentRuleWithDataStore
import android.tools.flicker.subject.layers.LayersTraceSubject
import android.tools.testutils.assertFail
import android.tools.testutils.assertThrows
import android.tools.testutils.getLayerTraceReaderFromAsset
import android.tools.traces.component.ComponentNameMatcher
import kotlin.time.Duration.Companion.seconds
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayersTraceSubjectDurationTest {
    /*
     * Trace created by opening the Calculator app, waiting between 3 to 4 seconds
     * and the swiping it away to go back to the home screen.
     * Thus the minimum duration for the test is 3 seconds and the maximum 4 seconds.
     */
    val testComponent = ComponentNameMatcher("", "Calculator")

    @Test
    fun testIsVisibleForAtLeastPass() {
        val reader =
            getLayerTraceReaderFromAsset("layers_trace_launch_calculator_duration.perfetto-trace")
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        LayersTraceSubject(trace, reader)
            .isInvisible(testComponent)
            .then()
            .isVisible(testComponent)
            .forAtLeast(3.seconds)
            .then()
            .isInvisible(testComponent)
            .forAllEntries()
    }

    @Test
    fun testIsVisibleForAtLeastFail() {
        val reader =
            getLayerTraceReaderFromAsset("layers_trace_launch_calculator_duration.perfetto-trace")
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        assertFail("Assertion [isVisible(Calculator)] did not meet min duration of 4s") {
            LayersTraceSubject(trace, reader)
                .isInvisible(testComponent)
                .then()
                .isVisible(testComponent)
                .forAtLeast(4.seconds)
                .then()
                .isInvisible(testComponent)
                .forAllEntries()
            error("Assertion should not have passed")
        }
    }

    @Test
    fun testIsVisibleForAtMostPass() {
        val reader =
            getLayerTraceReaderFromAsset("layers_trace_launch_calculator_duration.perfetto-trace")
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        LayersTraceSubject(trace, reader)
            .isInvisible(testComponent)
            .then()
            .isVisible(testComponent)
            .forAtMost(4.seconds)
            .then()
            .isInvisible(testComponent)
            .forAllEntries()
    }

    @Test
    fun testIsVisibleForAtMostFail() {
        val reader =
            getLayerTraceReaderFromAsset("layers_trace_launch_calculator_duration.perfetto-trace")
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        assertFail("Assertion [isVisible(Calculator)] exceeded max duration of 3s") {
            LayersTraceSubject(trace, reader)
                .isInvisible(testComponent)
                .then()
                .isVisible(testComponent)
                .forAtMost(3.seconds)
                .then()
                .isInvisible(testComponent)
                .forAllEntries()
            error("Assertion should not have passed")
        }
    }

    @Test
    fun testForAtLeastWithoutAssertionThrowsException() {
        val reader =
            getLayerTraceReaderFromAsset("layers_trace_launch_calculator_duration.perfetto-trace")
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        assertThrows<IllegalArgumentException> {
            LayersTraceSubject(trace, reader).forAtLeast(3.seconds).forAllEntries()
        }
    }

    @Test
    fun testForAtMostWithoutAssertionThrowsException() {
        val reader =
            getLayerTraceReaderFromAsset("layers_trace_launch_calculator_duration.perfetto-trace")
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        assertThrows<IllegalArgumentException> {
            LayersTraceSubject(trace, reader).forAtMost(3.seconds).forAllEntries()
        }
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRuleWithDataStore()
    }
}
