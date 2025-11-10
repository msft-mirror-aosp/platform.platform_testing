/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.test.desktop

import android.Manifest
import android.graphics.Matrix
import android.graphics.PointF
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_90
import android.window.WindowInfosListenerForTest
import android.window.WindowInfosListenerForTest.DisplayInfo
import android.window.WindowInfosListenerForTest.WindowInfo
import androidx.annotation.RequiresPermission
import com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A stateful helper that captures the display transformation state at the moment this helper class
 * was created. Provides utilities to convert between logical and physical PX coordinates.
 *
 * This conversion is necessary because there are 3 different coordinates spaces:
 * 1. Logical DP: Used to represent global coordinates, and used in apps
 * 2. Logical PX: Metric is calculated from applying display density to DP coordinate, used in
 *    frameworks WM and Display. When scaling is disabled this would be equal to the display
 *    physical PX
 * 3. Physical PX: The actual physical PX unit in the hardware display panel. In the native input
 *    stack, when scaling is disabled, this corresponds to mouse devices' coordinate space. When the
 *    coordinates are exposed to other components, the input stack applies physical to logical px
 *    transform. Also refer to go/input-rotation
 *
 * This class performs the opposite to Input. This inverts the transformation done by Input from
 * PhysicalPX -> LogicalPX, so that user of this helper class can convert LogicalPX -> PhysicalPX to
 * possibly send coordinates correctly back to Input.
 *
 * TODO(b/445797989): Move inverse display transform process to library in lower layer, e.g.
 *   VirtualInputDeviceController
 */
class LogicalPhysicalDisplayTransformHelper
@RequiresPermission(Manifest.permission.ACCESS_SURFACE_FLINGER)
constructor(displayManager: DisplayManager) {

    private val displayIdToTransformMap: Map<Int, Matrix>
    private val displayIdToInverseTransformMap: Map<Int, Matrix>

    init {
        displayIdToTransformMap = fetchDisplayTransforms(displayManager)
        displayIdToInverseTransformMap =
            displayIdToTransformMap.mapValues { (_, matrix) ->
                val inverse = Matrix()
                matrix.invert(inverse)
                inverse
            }
    }

    /**
     * Returns the display scale, which represents the logical size of one physical pixel. In
     * another words, 1 physical display PX = [DisplayScale] logical display PX
     *
     * @param displayId the ID of the target display.
     */
    fun getScale(displayId: Int): DisplayScale {
        val displayTransform =
            displayIdToTransformMap[displayId]
                ?: error("Failed to fetch displayTransform for display#$displayId")
        val matrixVal = FloatArray(9)
        displayTransform.getValues(matrixVal)
        return DisplayScale(matrixVal[Matrix.MSCALE_X], matrixVal[Matrix.MSCALE_Y])
    }

    /**
     * Transforms a point from the logical coordinate space to the physical coordinate space.
     *
     * @param point the point in logical PX coordinates
     * @param displayId the ID of the display on which the point should be scaled on
     * @return the point in physical PX coordinates.
     */
    fun logicalToPhysical(point: PointF, displayId: Int): PointPhysicalPxF {
        val inverseTransform =
            displayIdToInverseTransformMap[displayId]
                ?: error("Failed to fetch inverse displayTransform for display#$displayId")

        val pointArray = floatArrayOf(point.x, point.y)
        inverseTransform.mapPoints(pointArray)
        return PointPhysicalPxF(pointArray[0], pointArray[1])
    }

    /**
     * Transforms a delta from the logical coordinate space to the physical coordinate space.
     *
     * @param delta the delta in logical PX coordinates
     * @param displayId the ID of the display on which the delta should be scaled on
     * @return the point in physical PX coordinates
     */
    fun logicalToPhysical(delta: DeltaLogicalPxF, displayId: Int): DeltaPhysicalPxF {
        val inverseTransform =
            displayIdToInverseTransformMap[displayId]
                ?: error("Failed to fetch inverse displayTransform for display#$displayId")

        val deltaArray = floatArrayOf(delta.dx, delta.dy)
        inverseTransform.mapVectors(deltaArray)
        return DeltaPhysicalPxF(deltaArray[0], deltaArray[1])
    }

    /** Fetches the current display transforms. This is a blocking call. */
    @Throws(InterruptedException::class)
    private fun fetchDisplayTransforms(displayManager: DisplayManager): Map<Int, Matrix> {
        val consumer =
            object : BiConsumer<List<WindowInfo>, List<DisplayInfo>> {
                private val latch = CountDownLatch(1)
                private var isComplete = false
                lateinit var resultMap: Map<Int, Matrix>

                override fun accept(windows: List<WindowInfo>, displays: List<DisplayInfo>) {
                    if (isComplete || displays.isEmpty()) {
                        return
                    }
                    isComplete = true
                    resultMap = buildMap {
                        displays.forEach { display ->
                            displayManager.getDisplay(display.displayId)?.let { dmDisplay ->
                                val finalTransform = Matrix(display.transform)
                                val rotationDegrees =
                                    when (dmDisplay.rotation) {
                                        ROTATION_90 -> 90f
                                        ROTATION_180 -> 180f
                                        ROTATION_270 -> 270f
                                        else -> 0f
                                    }
                                if (rotationDegrees != 0f) {
                                    finalTransform.postRotate(rotationDegrees)
                                }
                                put(display.displayId, finalTransform)
                            } ?: run { Log.w(TAG, "Skipping invalid display#${display.displayId}") }
                        }
                    }
                    latch.countDown()
                }

                fun awaitAndGet(): Map<Int, Matrix> {
                    latch.await(TIMEOUT.inWholeSeconds, TimeUnit.SECONDS)
                    return resultMap
                }
            }

        val listener = WindowInfosListenerForTest()
        try {
            runWithShellPermissionIdentity(
                { listener.addWindowInfosListener(consumer) },
                Manifest.permission.ACCESS_SURFACE_FLINGER,
            )
            return consumer.awaitAndGet()
        } finally {
            listener.removeWindowInfosListener(consumer)
        }
    }

    data class PointPhysicalPxF(val x: Float, val y: Float)

    data class DeltaPhysicalPxF(val dx: Float, val dy: Float)

    data class DeltaLogicalPxF(val dx: Float, val dy: Float) {
        fun toPhysicalPx(displayId: Int, displayTransform: LogicalPhysicalDisplayTransformHelper) =
            displayTransform.logicalToPhysical(this, displayId)
    }

    data class DisplayScale(val scaleX: Float, val scaleY: Float)

    companion object {
        operator fun PointPhysicalPxF.minus(other: PointPhysicalPxF): DeltaPhysicalPxF {
            val dx = this.x - other.x
            val dy = this.y - other.y
            return DeltaPhysicalPxF(dx, dy)
        }

        fun LogicalDisplayPointPx.getPointPhysicalPx(
            displayTransform: LogicalPhysicalDisplayTransformHelper
        ) = displayTransform.logicalToPhysical(this.getPointF(), this.displayId)

        private val TIMEOUT: Duration = 10.seconds
        private const val TAG = "LogicalPhysicalDisplayTransformHelper"
    }
}
