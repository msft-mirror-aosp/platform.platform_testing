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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.math.nextDown
import kotlin.math.nextUp
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.golden.FloatTolerances.isWithinRelativeTolerance
import platform.test.motion.golden.FloatTolerances.isWithinTolerance

@RunWith(AndroidJUnit4::class)
class FloatTolerancesTest {

    @Test
    fun isWithinTolerance_basicEquality() {
        assertThat(isWithinTolerance(1.0f, 1.0f)).isTrue()
        assertThat(isWithinTolerance(1.0f, 1.1f)).isFalse()
    }

    @Test
    fun isWithinTolerance_ulpSafetyNet() {
        val value = 1_000_000f

        // Adding a tiny amount that is just 1 step (ULP) away
        assertThat(isWithinTolerance(value, value.nextUp(), 0f)).isTrue()
        assertThat(isWithinTolerance(value, value.nextDown(), 0f)).isTrue()
    }

    @Test
    fun isWithinRelativeTolerance_scaling() {
        assertThat(isWithinRelativeTolerance(1000f, 1010f, 0.01f)).isTrue()
        assertThat(isWithinRelativeTolerance(1000f, 1011f, 0.01f)).isFalse()
        assertThat(isWithinRelativeTolerance(0.001f, 0.002f, 0.5f)).isTrue()
        assertThat(isWithinRelativeTolerance(0.001f, 0.002f, 0.5f.nextDown())).isFalse()
    }

    @Test
    fun edgeCases_signedZero_isEqual() {
        // IEEE 754: 0.0 == -0.0
        assertThat(isWithinTolerance(0.0f, -0.0f)).isTrue()
        assertThat(isWithinRelativeTolerance(0.0f, -0.0f, toleranceFactor = 0f)).isTrue()
    }

    @Test
    fun edgeCases_infinities() {
        assertThat(isWithinTolerance(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)).isTrue()
        assertThat(isWithinTolerance(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)).isTrue()
        assertThat(isWithinTolerance(Float.POSITIVE_INFINITY, Float.MAX_VALUE)).isFalse()

        assertThat(
                isWithinRelativeTolerance(
                    Float.POSITIVE_INFINITY,
                    Float.POSITIVE_INFINITY,
                    toleranceFactor = 0f,
                )
            )
            .isTrue()
        assertThat(
                isWithinRelativeTolerance(
                    Float.NEGATIVE_INFINITY,
                    Float.NEGATIVE_INFINITY,
                    toleranceFactor = 0f,
                )
            )
            .isTrue()
        assertThat(
                isWithinRelativeTolerance(
                    Float.POSITIVE_INFINITY,
                    Float.MAX_VALUE,
                    toleranceFactor = 0f,
                )
            )
            .isFalse()
    }

    @Test
    fun edgeCases_nan() {
        // NaN is not equal to itself
        assertThat(isWithinTolerance(Float.NaN, Float.NaN)).isTrue()
        assertThat(isWithinRelativeTolerance(Float.NaN, Float.NaN, toleranceFactor = 0f)).isTrue()
    }

    @Test
    fun edgeCases_subnormalNumbers() {
        val tiny = Float.MIN_VALUE // Smallest positive non-zero value
        assertThat(isWithinTolerance(0f, tiny)).isTrue()
        assertThat(isWithinRelativeTolerance(0f, tiny, toleranceFactor = 0f)).isTrue()
    }

    @Test
    fun validation_negativeToleranceThrows() {
        Assert.assertThrows(IllegalArgumentException::class.java) { isWithinTolerance(1f, 1f, -1f) }
        Assert.assertThrows(IllegalArgumentException::class.java) {
            isWithinRelativeTolerance(1f, 1f, -1f)
        }
    }
}
