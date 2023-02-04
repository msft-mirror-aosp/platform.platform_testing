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
package com.android.server.wm.flicker.traces.layers

import android.graphics.Point
import com.android.server.wm.flicker.assertions.Assertion
import com.android.server.wm.flicker.assertions.Fact
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.traces.region.RegionSubject
import com.android.server.wm.traces.common.Size
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.layers.Layer

/**
 * Subject for [Layer] objects, used to make assertions over behaviors that occur on a single layer
 * of a SurfaceFlinger state.
 *
 * To make assertions over a layer from a state it is recommended to create a subject using
 * [LayerTraceEntrySubject.layer](layerName)
 *
 * Alternatively, it is also possible to use [LayerSubject](myLayer).
 *
 * Example:
 * ```
 *    val trace = LayersTraceParser().parse(myTraceFile)
 *    val subject = LayersTraceSubject(trace).first()
 *        .layer("ValidLayer")
 *        .exists()
 *        .hasBufferSize(BUFFER_SIZE)
 *        .invoke { myCustomAssertion(this) }
 * ```
 */
class LayerSubject(
    public override val parent: FlickerSubject,
    override val timestamp: Timestamp,
    val layer: Layer?,
    private val layerName: String? = null
) : FlickerSubject() {
    val isEmpty: Boolean
        get() = layer == null
    val isNotEmpty: Boolean
        get() = !isEmpty
    val isVisible: Boolean
        get() = layer?.isVisible == true
    val isInvisible: Boolean
        get() = layer?.isVisible == false
    val name: String
        get() = layer?.name ?: ""

    /** Visible region calculated by the Composition Engine */
    val visibleRegion: RegionSubject
        get() = RegionSubject(layer?.visibleRegion, this, timestamp)

    val visibilityReason: Array<String>
        get() = layer?.visibilityReason ?: emptyArray()

    /**
     * Visible region calculated by the Composition Engine (when available) or calculated based on
     * the layer bounds and transform
     */
    val screenBounds: RegionSubject
        get() = RegionSubject(layer?.screenBounds, this, timestamp)

    override val selfFacts =
        if (layer != null) {
            listOf(Fact("Frame", layer.currFrame), Fact("Layer", layer.name))
        } else {
            listOf(Fact("Layer name", layerName))
        }

    /** If the [layer] exists, executes a custom [assertion] on the current subject */
    operator fun invoke(assertion: Assertion<Layer>): LayerSubject = apply {
        layer ?: return exists()
        assertion(this.layer)
    }

    /** Asserts that current subject doesn't exist in the layer hierarchy */
    fun doesNotExist(): LayerSubject = apply {
        check { "Layer does not exist ${layer?.name}" }.that(layer).isEqual(null)
    }

    /** Asserts that current subject exists in the layer hierarchy */
    fun exists(): LayerSubject = apply { check { "Layer exists" }.that(layer).isNotEqual(null) }

    @Deprecated("Prefer hasBufferSize(bounds)")
    fun hasBufferSize(expected: Point): LayerSubject = apply {
        val bounds = Size.from(expected.x, expected.y)
        hasBufferSize(bounds)
    }

    /**
     * Asserts that current subject has an [Layer.activeBuffer] with width equals to [Point.x] and
     * height equals to [Point.y]
     *
     * @param expected expected buffer size
     */
    fun hasBufferSize(expected: Size): LayerSubject = apply {
        layer ?: return exists()
        val bufferSize = Size.from(layer.activeBuffer.width, layer.activeBuffer.height)
        check { "Buffer size" }.that(bufferSize).isEqual(expected)
    }

    /**
     * Asserts that current subject has an [Layer.screenBounds] with width equals to [Point.x] and
     * height equals to [Point.y]
     *
     * @param size expected layer bounds size
     */
    fun hasLayerSize(size: Point): LayerSubject = apply {
        layer ?: return exists()
        val layerSize = Point(layer.screenBounds.width.toInt(), layer.screenBounds.height.toInt())
        check { "Number of layers" }.that(layerSize).isEqual(size)
    }

    /**
     * Asserts that current subject has an [Layer.effectiveScalingMode] equals to
     * [expectedScalingMode]
     */
    fun hasScalingMode(expectedScalingMode: Int): LayerSubject = apply {
        layer ?: return exists()
        val actualScalingMode = layer.effectiveScalingMode
        check(actualScalingMode == expectedScalingMode) {
            "Scaling mode. Actual: $actualScalingMode, expected: $expectedScalingMode"
        }
    }

    /**
     * Asserts that current subject has an [Layer.bufferTransform] orientation equals to
     * [expectedOrientation]
     */
    fun hasBufferOrientation(expectedOrientation: Int): LayerSubject = apply {
        layer ?: return exists()
        // see Transform::getOrientation
        val bufferTransformType = layer.bufferTransform.type ?: 0
        val actualOrientation = (bufferTransformType shr 8) and 0xFF
        check(actualOrientation == expectedOrientation) {
            "Buffer orientation. Actual: $actualOrientation, expected: $expectedOrientation"
        }
    }

    override fun toString(): String {
        return "Layer:${layer?.name} frame#${layer?.currFrame}"
    }
}
