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
package com.android.server.wm.flicker.traces.layers

import android.graphics.Point
import android.graphics.Rect
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth

/** Truth subject for a single [Layer] entry  */
class LayerSubject private constructor(
    fm: FailureMetadata,
    val layer: Layer?
) : Subject(fm, layer) {

    fun doesNotExist() {
        check("doesNotExist()").that(layer).isNull()
    }

    fun exists() {
        if (layer == null) {
            failWithoutActual(simpleFact("doesn't exist"))
        }
    }

    fun hasBufferSize(size: Point) {
        layer ?: return exists()
        val bufferSize = Point(layer.proto.activeBuffer.width, layer.proto.activeBuffer.height)
        Truth.assertThat(bufferSize).isEqualTo(size)
    }

    fun hasLayerSize(size: Point) {
        layer ?: return exists()
        val screenBoundsProto = layer.proto.screenBounds
        val layerBounds = Rect(screenBoundsProto.left.toInt(), screenBoundsProto.top.toInt(),
                screenBoundsProto.right.toInt(), screenBoundsProto.bottom.toInt())
        val layerSize = Point(layerBounds.width(), layerBounds.height())
        Truth.assertThat(layerSize).isEqualTo(size)
    }

    fun hasScalingMode(expectedScalingMode: Int) {
        layer ?: return exists()
        val actualScalingMode = layer.proto.effectiveScalingMode
        Truth.assertThat(actualScalingMode).isEqualTo(expectedScalingMode)
    }

    fun hasBufferOrientation(expectedOrientation: Int) {
        layer ?: return exists()
        // see Transform::getOrientation
        val actualOrientation = (layer.proto.bufferTransform.type shr 8) and 0xFF
        check("hasBufferTransformOrientation()")
                .that(actualOrientation).isEqualTo(expectedOrientation)
    }

    companion object {
        /**
         * Boiler-plate Subject.Factory for LayerSubject
         */
        val FACTORY = Factory { fm: FailureMetadata, subject: Layer? ->
            LayerSubject(fm, subject)
        }

        /**
         * User-defined entry point
         */
        @JvmStatic
        fun assertThat(entry: Layer?) = Truth.assertAbout(FACTORY).that(entry) as LayerSubject
    }
}