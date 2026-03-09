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

package platform.test.motion.compose

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import platform.test.motion.golden.FloatTolerances

/**
 * [Dp] comparison logic that scales based on magnitude
 *
 * This utility ensures that comparisons don't fail due to infinitesimal rounding errors inherent to
 * IEEE 754 math, even with a tolerance of zero.
 *
 * @see FloatTolerances
 */
object DpTolerances {
    /**
     * Checks if [a] and [b] are within an absolute [tolerance].
     *
     * Allows for tiny differences due to precision loss of the floating-point operations, even with
     * a [tolerance] of 0.dp.
     */
    fun isWithinTolerance(a: Dp, b: Dp, tolerance: Dp = 0.dp): Boolean =
        FloatTolerances.isWithinTolerance(a.value, b.value, tolerance.value)
}
