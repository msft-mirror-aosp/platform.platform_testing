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

package android.tools.common.flicker.traces.surfaceflinger

import android.tools.common.flicker.subject.layers.LayersTraceSubject
import android.tools.common.traces.component.ComponentNameMatcher
import android.tools.utils.TestComponents
import android.tools.utils.assertThatErrorContainsDebugInfo
import android.tools.utils.assertThrows
import android.tools.utils.getLayerTraceReaderFromAsset
import org.junit.Test

class LayerTraceEntrySubjectTest {
    @Test
    fun exceptionContainsDebugInfo() {
        val reader = getLayerTraceReaderFromAsset("layers_trace_emptyregion.pb", legacyTrace = true)
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        val error =
            assertThrows<AssertionError> {
                LayersTraceSubject(trace, reader).first().contains(TestComponents.IMAGINARY)
            }
        assertThatErrorContainsDebugInfo(error)
    }

    @Test
    fun canDetectInvisibleLayerOutOfScreen() {
        val reader = getLayerTraceReaderFromAsset("layers_trace_visible_outside_bounds.winscope")
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        val subject =
            LayersTraceSubject(trace, reader)
                .getEntryBySystemUpTime(1253267561044, byElapsedTimestamp = true)
        val region = subject.visibleRegion(ComponentNameMatcher.IME_SNAPSHOT)
        region.isEmpty()
        subject.isInvisible(ComponentNameMatcher.IME_SNAPSHOT)
    }

    @Test
    fun canDetectInvisibleLayerOutOfScreen_ConsecutiveLayers() {
        val reader = getLayerTraceReaderFromAsset("layers_trace_visible_outside_bounds.winscope")
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        val subject = LayersTraceSubject(trace, reader)
        subject.visibleLayersShownMoreThanOneConsecutiveEntry()
    }
}
