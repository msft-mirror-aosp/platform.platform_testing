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

package android.tools.traces.surfaceflinger

import android.tools.Cache
import android.tools.Timestamps
import android.tools.testutils.CleanFlickerEnvironmentRule
import android.tools.testutils.getLayerTraceReaderFromAsset
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.ClassRule
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [LayerTraceEntry] tests. To run this test: `atest FlickerLibTest:LayersTraceEntryTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayersTraceEntryTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun canParseAllLayers() {
        val reader = getLayerTraceReaderFromAsset("layers_trace_emptyregion.perfetto-trace")
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        Truth.assertThat(trace.entries).isNotEmpty()
        Truth.assertThat(trace.entries.first().timestamp.systemUptimeNanos).isEqualTo(922839428857)
        Truth.assertThat(trace.entries.last().timestamp.systemUptimeNanos).isEqualTo(941432656959)
        Truth.assertThat(trace.entries.last().flattenedLayers).hasSize(57)
    }

    @Test
    fun canParseVisibleLayers() {
        val reader = getLayerTraceReaderFromAsset("layers_trace_launch_split_screen.perfetto-trace")
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        val entry = trace.getEntryExactlyAt(Timestamps.from(systemUptimeNanos = 90493757372977))
        val visibleLayers = entry.visibleLayers
        Truth.assertThat(entry.flattenedLayers).hasSize(82)
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
    fun canParseLayerHierarchy() {
        val reader = getLayerTraceReaderFromAsset("layers_trace_emptyregion.perfetto-trace")
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        Truth.assertThat(trace.entries).isNotEmpty()
        Truth.assertThat(trace.entries.first().timestamp.systemUptimeNanos).isEqualTo(922839428857)
        Truth.assertThat(trace.entries.last().timestamp.systemUptimeNanos).isEqualTo(941432656959)
        Truth.assertThat(trace.entries.first().flattenedLayers).hasSize(57)
        val layers = trace.entries.first().children
        Truth.assertThat(layers.first().children).hasSize(3)
        Truth.assertThat(layers.drop(1).first().children).isEmpty()
    }

    // b/76099859
    @Test
    fun canDetectOrphanLayers() {
        try {
            val reader =
                getLayerTraceReaderFromAsset(
                    "layers_trace_orphanlayers.perfetto-trace",
                    ignoreOrphanLayers = false,
                )
            reader.readLayersTrace()?.entries?.first()?.flattenedLayers
            error("Failed to detect orphaned layers.")
        } catch (exception: RuntimeException) {
            Truth.assertThat(exception.message)
                .contains(
                    "Failed to parse layers trace. Found orphan layer with id = 49 with" +
                        " parentId = 1006"
                )
        }
    }

    @Test
    fun canParseTraceEmptyState() {
        val reader = getLayerTraceReaderFromAsset("layers_trace_empty_state.perfetto-trace")
        val trace = reader.readLayersTrace() ?: error("Unable to read layers trace")
        val emptyStates = trace.entries.filter { it.flattenedLayers.isEmpty() }

        Truth.assertWithMessage("Some states in the trace should be empty")
            .that(emptyStates)
            .isNotEmpty()

        Truth.assertWithMessage("Expected state 4d4h41m14s193ms to be empty")
            .that(emptyStates.first().timestamp.systemUptimeNanos)
            .isEqualTo(362474193519965)
    }

    @Test
    fun usesRealTimestampWhenAvailableAndFallsbackOnElapsedTimestamp() {
        var entry =
            LayerTraceEntry(
                elapsedTimestamp = 100,
                clockTimestamp = 600,
                hwcBlob = "",
                where = "",
                displays = emptyList(),
                vSyncId = 123,
                _rootLayers = emptyList(),
            )
        Truth.assertThat(entry.timestamp.elapsedNanos).isEqualTo(Timestamps.empty().elapsedNanos)
        Truth.assertThat(entry.timestamp.systemUptimeNanos).isEqualTo(100)
        Truth.assertThat(entry.timestamp.unixNanos).isEqualTo(600)

        entry =
            LayerTraceEntry(
                elapsedTimestamp = 100,
                clockTimestamp = null,
                hwcBlob = "",
                where = "",
                displays = emptyList(),
                vSyncId = 123,
                _rootLayers = emptyList(),
            )
        Truth.assertThat(entry.timestamp.elapsedNanos).isEqualTo(Timestamps.empty().elapsedNanos)
        Truth.assertThat(entry.timestamp.systemUptimeNanos).isEqualTo(100)
        Truth.assertThat(entry.timestamp.unixNanos).isEqualTo(Timestamps.empty().unixNanos)
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}
