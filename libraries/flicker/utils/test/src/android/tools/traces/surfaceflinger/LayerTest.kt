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

/** Contains [Layer] tests. To run this test: `atest FlickerLibTest:LayerTest` */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LayerTest {
    @Before
    fun before() {
        Cache.clear()
    }

    @Test
    fun detectsIfTask() {
        assertThat(makeLayerWithDefaults().isTask).isFalse()
        assertThat(makeLayerWithDefaults(name = "Task=123").isTask).isTrue()
    }

    @Test
    fun detectsIfRootLayer() {
        val layer = makeLayerWithDefaults()
        assertThat(layer.isRootLayer).isTrue()
        layer.parent = makeLayerWithDefaults()
        assertThat(layer.isRootLayer).isFalse()
    }

    private fun makeLayerWithDefaults(name: String = ""): Layer {
        return Layer.from(
            name,
            0,
            0,
            0,
            Region(),
            ActiveBuffer.EMPTY,
            0x0,
            RectF(),
            defaultColor(),
            -1f,
            -1f,
            RectF(),
            Transform.EMPTY,
            -1,
            -1,
            Transform.EMPTY,
            HwcCompositionType.HWC_TYPE_UNSPECIFIED,
            -1,
            null,
            false,
            -1,
            -1,
            false,
        )
    }
}
