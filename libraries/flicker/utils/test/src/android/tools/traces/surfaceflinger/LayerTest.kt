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

import android.graphics.Color
import android.graphics.RectF
import android.graphics.Region
import android.tools.Cache
import android.tools.datatypes.ActiveBuffer
import android.tools.datatypes.defaultColor
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.mockito.MockitoAnnotations

/** Contains [Layer] tests. To run this test: `atest FlickerLibTest:LayerTest` */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayerTest {
    @Before
    fun before() {
        Cache.clear()
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun hasVerboseFlagsProperty() {
        assertThat(makeLayerWithDefaults(flags = 0x0).verboseFlags).isEqualTo("")

        assertThat(makeLayerWithDefaults(flags = 0x1).verboseFlags).isEqualTo("HIDDEN (0x1)")

        assertThat(makeLayerWithDefaults(flags = 0x2).verboseFlags).isEqualTo("OPAQUE (0x2)")

        assertThat(makeLayerWithDefaults(flags = 0x40).verboseFlags)
            .isEqualTo("SKIP_SCREENSHOT (0x40)")

        assertThat(makeLayerWithDefaults(flags = 0x80).verboseFlags).isEqualTo("SECURE (0x80)")

        assertThat(makeLayerWithDefaults(flags = 0x100).verboseFlags)
            .isEqualTo("ENABLE_BACKPRESSURE (0x100)")

        assertThat(makeLayerWithDefaults(flags = 0x200).verboseFlags)
            .isEqualTo("DISPLAY_DECORATION (0x200)")

        assertThat(makeLayerWithDefaults(flags = 0x400).verboseFlags)
            .isEqualTo("IGNORE_DESTINATION_FRAME (0x400)")

        assertThat(makeLayerWithDefaults(flags = 0xc3).verboseFlags)
            .isEqualTo("HIDDEN|OPAQUE|SKIP_SCREENSHOT|SECURE (0xc3)")
    }

    @Test
    fun isRootLayer() {
        val layer = makeLayerWithDefaults()
        assertThat(layer.isRootLayer).isTrue()
    }

    @Test
    fun isNotRootLayer() {
        val layer = makeLayerWithDefaults()
        layer.parent = makeLayerWithDefaults()
        assertThat(layer.isRootLayer).isFalse()
    }

    @Test
    fun isTaskLayer() {
        val layer = makeLayerWithDefaults(name = "Task=1")
        assertThat(layer.isTask).isTrue()
    }

    @Test
    fun isNotTaskLayer() {
        val layer = makeLayerWithDefaults(name = "NotATask=1")
        assertThat(layer.isTask).isFalse()
    }

    private fun makeLayerWithDefaults(
        name: String = "",
        flags: Int = 0x0,
        visibleRegion: Region = Region(),
        bounds: RectF = RectF(),
        activeBuffer: ActiveBuffer = ActiveBuffer.EMPTY,
        screenBounds: RectF = RectF(),
        transform: Transform = Transform.EMPTY,
        color: Color = defaultColor(),
    ): Layer {
        return Layer.from(
            name = name,
            id = 0,
            parentId = 0,
            z = 0,
            visibleRegion = visibleRegion,
            activeBuffer = activeBuffer,
            flags = flags,
            bounds = bounds,
            color = color,
            shadowRadius = -1f,
            cornerRadii = CornerRadii.EMPTY,
            screenBounds = screenBounds,
            transform = transform,
            currFrame = -1,
            effectiveScalingMode = -1,
            bufferTransform = Transform.EMPTY,
            hwcCompositionType = HwcCompositionType.HWC_TYPE_UNSPECIFIED,
            backgroundBlurRadius = -1,
            crop = null,
            isRelativeOf = false,
            zOrderRelativeOfId = -1,
            stackId = -1,
        )
    }
}
