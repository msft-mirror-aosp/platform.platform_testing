/*
 * Copyright (C) 2026 The Android Open Source Project
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

package platform.test.motion.golden

import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max

/**
 * Custom floating-point comparison logic that scales based on magnitude.
 *
 * This utility ensures that comparisons don't fail due to infinitesimal rounding errors inherent to
 * IEEE 754 math, even with a tolerance of zero.
 */
object FloatTolerances {
    private const val MaxUlps = 10

    /**
     * Checks if [a] and [b] are within an absolute [tolerance].
     *
     * Allows for tiny differences due to precision loss of the floating-point operations, even with
     * a [tolerance] of 0.
     *
     * Note that isWithinTolerance(Float.NaN, Float.NaN); For the purpose of comparing goldens NaN
     * is equal to itself.
     */
    fun isWithinTolerance(a: Float, b: Float, tolerance: Float = 0f): Boolean {
        require(tolerance.isFinite() && tolerance >= 0) { "Invalid tolerance: $tolerance" }

        if (a == b) return true
        if (a.isNaN() && b.isNaN()) return true

        val diff = abs(a - b)

        if (diff <= tolerance) return true

        return isWithinFloatMathTolerance(diff, largerValue = max(a.absoluteValue, b.absoluteValue))
    }

    /**
     * Checks if the difference between [a] and [b] is within a [toleranceFactor] relative to the
     * magnitude of the larger value.
     *
     * Allows for tiny differences due to precision loss of the floating-point operations, even with
     * a [toleranceFactor] of 0.
     */
    fun isWithinRelativeTolerance(a: Float, b: Float, toleranceFactor: Float): Boolean {
        require(toleranceFactor.isFinite() && toleranceFactor >= 0) {
            "Invalid tolerance: $toleranceFactor"
        }

        if (a == b) return true
        if (a.isNaN() && b.isNaN()) return true

        val diff = abs(a - b)

        val largerValue = max(a.absoluteValue, b.absoluteValue)

        return diff <= toleranceFactor * largerValue ||
            isWithinFloatMathTolerance(diff, largerValue)
    }

    private fun isWithinFloatMathTolerance(diff: Float, largerValue: Float): Boolean {
        return diff.isFinite() && largerValue.isFinite() && diff <= Math.ulp(largerValue) * MaxUlps
    }
}
