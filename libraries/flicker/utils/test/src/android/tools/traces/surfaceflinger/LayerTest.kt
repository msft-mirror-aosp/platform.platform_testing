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
    fun useVisibleRegionIfCompositionStateIsAvailableForVisibility() {
        assertThat(
                makeLayerWithDefaults(
                        excludeCompositionState = false,
                        visibleRegion = Region(),
                        activeBuffer = ActiveBuffer.from(100, 100, 1, 0),
                    )
                    .isVisible
            )
            .isFalse()
        assertThat(
                makeLayerWithDefaults(
                        excludeCompositionState = false,
                        visibleRegion = Region(0, 0, 100, 100),
                        activeBuffer = ActiveBuffer.from(100, 100, 1, 0),
                    )
                    .isVisible
            )
            .isTrue()
    }

    @Test
    fun fallbackOnLayerBoundsIfCompositionStateIsNotAvailableForVisibility() {
        assertThat(
                makeLayerWithDefaults(
                        excludeCompositionState = true,
                        bounds = RectF(),
                        activeBuffer = ActiveBuffer.from(100, 100, 1, 0),
                    )
                    .isVisible
            )
            .isFalse()
        assertThat(
                makeLayerWithDefaults(
                        excludeCompositionState = true,
                        bounds = RectF(0f, 0f, 100f, 100f),
                        activeBuffer = ActiveBuffer.from(100, 100, 1, 0),
                    )
                    .isVisible
            )
            .isTrue()
        assertThat(
                makeLayerWithDefaults(
                        excludeCompositionState = true,
                        visibleRegion = Region(0, 0, 100, 100),
                        bounds = RectF(),
                        activeBuffer = ActiveBuffer.from(100, 100, 1, 0),
                    )
                    .isVisible
            )
            .isFalse()
    }

    @Test
    fun isHiddenByPolicy() {
        val layer = makeLayerWithDefaults(flags = Flag.HIDDEN.value)
        assertThat(layer.isHiddenByPolicy).isTrue()
    }

    @Test
    fun isHiddenByParent() {
        val parent = makeLayerWithDefaults(flags = Flag.HIDDEN.value)
        val child = makeLayerWithDefaults()
        child.parent = parent
        assertThat(child.isHiddenByParent).isTrue()
    }

    @Test
    fun isNotHiddenByParent() {
        val parent = makeLayerWithDefaults(flags = 0)
        val child = makeLayerWithDefaults()
        child.parent = parent
        assertThat(child.isHiddenByParent).isFalse()
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

    @Test
    fun contains() {
        val layer1 =
            makeLayerWithDefaults(
                screenBounds = RectF(0f, 0f, 100f, 100f),
                transform = Transform.EMPTY,
            )
        val layer2 =
            makeLayerWithDefaults(
                screenBounds = RectF(10f, 10f, 90f, 90f),
                transform = Transform.EMPTY,
            )
        assertThat(layer1.contains(layer2)).isTrue()
    }

    @Test
    fun doesNotContain() {
        val layer1 =
            makeLayerWithDefaults(
                screenBounds = RectF(0f, 0f, 100f, 100f),
                transform = Transform.EMPTY,
            )
        val layer2 =
            makeLayerWithDefaults(
                screenBounds = RectF(110f, 110f, 190f, 190f),
                transform = Transform.EMPTY,
            )
        assertThat(layer1.contains(layer2)).isFalse()
    }

    @Test
    fun overlaps() {
        val layer1 =
            makeLayerWithDefaults(
                screenBounds = RectF(0f, 0f, 100f, 100f),
                transform = Transform.EMPTY,
            )
        val layer2 =
            makeLayerWithDefaults(
                screenBounds = RectF(50f, 50f, 150f, 150f),
                transform = Transform.EMPTY,
            )
        assertThat(layer1.overlaps(layer2)).isTrue()
    }

    @Test
    fun doesNotOverlap() {
        val layer1 =
            makeLayerWithDefaults(
                screenBounds = RectF(0f, 0f, 100f, 100f),
                transform = Transform.EMPTY,
            )
        val layer2 =
            makeLayerWithDefaults(
                screenBounds = RectF(110f, 110f, 190f, 190f),
                transform = Transform.EMPTY,
            )
        assertThat(layer1.overlaps(layer2)).isFalse()
    }

    @Test
    fun visibilityReasonIsHidden() {
        val layer = makeLayerWithDefaults(flags = Flag.HIDDEN.value)
        assertThat(layer.visibilityReason).contains("Flag is hidden")
    }

    @Test
    fun visibilityReasonIsEmptyBuffer() {
        val layer = makeLayerWithDefaults(activeBuffer = ActiveBuffer.EMPTY)
        assertThat(layer.visibilityReason).contains("Buffer is empty")
    }

    @Test
    fun visibilityReasonIsAlpha() {
        val layer = makeLayerWithDefaults(color = Color.valueOf(0f, 0f, 0f, 0f))
        assertThat(layer.visibilityReason).contains("Alpha is 0")
    }

    /*@Test
    fun mockProperties() {
        val properties = mock<ILayerProperties>()
        whenever(properties.screenBounds).thenReturn(RectF(0f, 0f, 100f, 100f))
        whenever(properties.transform).thenReturn(Transform.EMPTY)
        val layer = Layer("test", 1, 0, 0, 0, properties)
        assertThat(layer.screenBounds).isEqualTo(RectF(0f, 0f, 100f, 100f))
    }*/

    private fun makeLayerWithDefaults(
        name: String = "",
        flags: Int = 0x0,
        excludeCompositionState: Boolean = false,
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
            bounds = bounds,
            z = 0,
            visibleRegion = visibleRegion,
            activeBuffer = activeBuffer,
            flags = flags,
            color = color,
            isOpaque = false,
            shadowRadius = -1f,
            cornerRadius = -1f,
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
            excludesCompositionState = excludeCompositionState,
        )
    }
}
