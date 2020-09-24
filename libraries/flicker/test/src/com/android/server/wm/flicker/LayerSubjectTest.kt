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

import android.graphics.Rect
import com.android.server.wm.flicker.traces.layers.LayersTrace
import com.android.server.wm.flicker.traces.layers.LayersTrace.Companion.parseFrom
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject.Companion.assertThat
import org.junit.Test
import java.nio.file.Paths

/**
 * Contains [LayerSubject] tests. To run this test:
 * `atest FlickerLibTest:LayerSubjectTest`
 */
class LayerSubjectTest {
    @Test
    fun canTestAssertionsOnLayer() {
        val layersTraceEntries = readLayerTraceFromFile("layers_trace_emptyregion.pb")
        assertThat(layersTraceEntries).layer("SoundVizWallpaperV2", 26033).also {
            it.hasBufferSize(Rect(0, 0, 1440, 2960))
            it.hasScalingMode(0)
        }
        assertThat(layersTraceEntries).layer("DoesntExist", 1).doesNotExist()
    }

    companion object {
        private fun readLayerTraceFromFile(relativePath: String): LayersTrace {
            return try {
                parseFrom(readTestFile(relativePath), Paths.get(relativePath))
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
}
