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

package android.platform.uiautomator_helpers

import android.graphics.Point
import android.platform.uiautomator_helpers.DeviceHelpers.context
import android.platform.uiautomator_helpers.DeviceHelpers.uiDevice
import android.util.TypedValue
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** A fling utility that should be used instead of [UiObject2.fling] for more reliable flings. */
object BetterFling {
    private const val DEFAULT_FLING_MARGIN_DP = 30
    private val DEFAULT_FLING_DURATION = Duration.of(100, ChronoUnit.MILLIS)
    private val DEFAULT_WAIT_TIMEOUT = Duration.of(5, ChronoUnit.SECONDS)

    /** Fling [obj] in the given [direction]. */
    @JvmStatic
    @JvmOverloads
    fun fling(
        obj: UiObject2,
        direction: Direction,
        duration: Duration = DEFAULT_FLING_DURATION,
        marginDp: Int = DEFAULT_FLING_MARGIN_DP,
    ) {
        val margin = marginDp.dpToPx().roundToInt()
        val bounds = obj.visibleBounds
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val (start, stop) =
            when (direction) {
                Direction.LEFT -> {
                    Point(bounds.right - margin, centerY) to Point(bounds.left + margin, centerY)
                }
                Direction.RIGHT -> {
                    Point(bounds.left + margin, centerY) to Point(bounds.right - margin, centerY)
                }
                Direction.UP -> {
                    Point(centerX, bounds.bottom - margin) to Point(centerX, bounds.top + margin)
                }
                Direction.DOWN -> {
                    Point(centerX, bounds.top + margin) to Point(centerX, bounds.bottom - margin)
                }
            }

        uiDevice.performActionAndWait(
            { BetterSwipe.from(start).to(stop, duration).release() },
            Until.scrollFinished(Direction.reverse(direction)),
            DEFAULT_WAIT_TIMEOUT.toMillis()
        )
    }

    private fun Number.dpToPx(): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            toFloat(),
            context.resources.displayMetrics,
        )
    }
}
