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

import com.android.server.wm.traces.common.ActiveBuffer
import com.android.server.wm.traces.common.Cache
import com.android.server.wm.traces.common.Color
import com.android.server.wm.traces.common.Rect
import com.android.server.wm.traces.common.RectF
import com.android.server.wm.traces.common.layers.HwcCompositionType
import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.layers.Transform
import com.android.server.wm.traces.common.region.Region
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Contains [Layer] tests. To run this test: `atest FlickerLibTest:LayerTest` */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayerTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun hasVerboseFlagsProperty() {
        assertThat(makeLayerWithFlags(0x0).verboseFlags).isEqualTo("")

        assertThat(makeLayerWithFlags(0x1).verboseFlags).isEqualTo("HIDDEN (0x1)")

        assertThat(makeLayerWithFlags(0x2).verboseFlags).isEqualTo("OPAQUE (0x2)")

        assertThat(makeLayerWithFlags(0x40).verboseFlags).isEqualTo("SKIP_SCREENSHOT (0x40)")

        assertThat(makeLayerWithFlags(0x80).verboseFlags).isEqualTo("SECURE (0x80)")

        assertThat(makeLayerWithFlags(0x100).verboseFlags).isEqualTo("ENABLE_BACKPRESSURE (0x100)")

        assertThat(makeLayerWithFlags(0x200).verboseFlags).isEqualTo("DISPLAY_DECORATION (0x200)")

        assertThat(makeLayerWithFlags(0x400).verboseFlags)
            .isEqualTo("IGNORE_DESTINATION_FRAME (0x400)")

        assertThat(makeLayerWithFlags(0xc3).verboseFlags)
            .isEqualTo("HIDDEN|OPAQUE|SKIP_SCREENSHOT|SECURE (0xc3)")
    }

    private fun makeLayerWithFlags(flags: Int): Layer {
        return Layer.from(
            "",
            0,
            0,
            0,
            Region.EMPTY,
            ActiveBuffer.EMPTY,
            flags,
            RectF.EMPTY,
            Color.EMPTY,
            false,
            -1f,
            -1f,
            "",
            RectF.EMPTY,
            Transform.EMPTY,
            RectF.EMPTY,
            -1,
            -1,
            Transform.EMPTY,
            HwcCompositionType.INVALID,
            RectF.EMPTY,
            Rect.EMPTY,
            -1,
            null,
            false,
            -1,
            -1,
            Transform.EMPTY,
            Color.EMPTY,
            RectF.EMPTY,
            Transform.EMPTY,
            null
        )
    }
}
