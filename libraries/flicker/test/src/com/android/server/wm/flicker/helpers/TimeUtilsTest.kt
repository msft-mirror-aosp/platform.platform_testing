/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.wm.flicker.helpers

import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Contains [TimeUtils] tests. To run this test: `atest FlickerLibTest:TimeUtilsTest` */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TimeUtilsTest {
    private val MILLISECOND = 1000000L
    private val SECOND = 1000 * MILLISECOND
    private val MINUTE = 60 * SECOND
    private val HOUR = 60 * MINUTE
    private val DAY = 24 * HOUR

    @Test
    fun canFormatElapsedTime() {
        Truth.assertThat(formatElapsedTimestamp(0)).isEqualTo("0ns")
        Truth.assertThat(formatElapsedTimestamp(1000)).isEqualTo("1000ns")
        Truth.assertThat(formatElapsedTimestamp(MILLISECOND - 1)).isEqualTo("999999ns")
        Truth.assertThat(formatElapsedTimestamp(MILLISECOND)).isEqualTo("1ms0ns")
        Truth.assertThat(formatElapsedTimestamp(10 * MILLISECOND)).isEqualTo("10ms0ns")

        Truth.assertThat(formatElapsedTimestamp(SECOND - 1)).isEqualTo("999ms999999ns")
        Truth.assertThat(formatElapsedTimestamp(SECOND)).isEqualTo("1s0ms0ns")
        Truth.assertThat(formatElapsedTimestamp(SECOND + MILLISECOND)).isEqualTo("1s1ms0ns")

        Truth.assertThat(formatElapsedTimestamp(MINUTE - 1)).isEqualTo("59s999ms999999ns")
        Truth.assertThat(formatElapsedTimestamp(MINUTE)).isEqualTo("1m0s0ms0ns")
        Truth.assertThat(formatElapsedTimestamp(MINUTE + SECOND + MILLISECOND))
            .isEqualTo("1m1s1ms0ns")
        Truth.assertThat(formatElapsedTimestamp(MINUTE + SECOND + MILLISECOND + 1))
            .isEqualTo("1m1s1ms1ns")

        Truth.assertThat(formatElapsedTimestamp(HOUR - 1)).isEqualTo("59m59s999ms999999ns")
        Truth.assertThat(formatElapsedTimestamp(HOUR)).isEqualTo("1h0m0s0ms0ns")
        Truth.assertThat(formatElapsedTimestamp(HOUR + MINUTE + SECOND + MILLISECOND))
            .isEqualTo("1h1m1s1ms0ns")

        Truth.assertThat(formatElapsedTimestamp(DAY - 1)).isEqualTo("23h59m59s999ms999999ns")
        Truth.assertThat(formatElapsedTimestamp(DAY)).isEqualTo("1d0h0m0s0ms0ns")
        Truth.assertThat(formatElapsedTimestamp(DAY + HOUR + MINUTE + SECOND + MILLISECOND))
            .isEqualTo("1d1h1m1s1ms0ns")
    }

    @Test
    fun canFormatRealTime() {
        val NOV_10_2022 = 1668038400000 * MILLISECOND

        Truth.assertThat(formatRealTimestamp(0)).isEqualTo("1970-01-01T00:00:00.000000000")
        Truth.assertThat(
                formatRealTimestamp(
                    NOV_10_2022 + 22 * HOUR + 4 * MINUTE + 54 * SECOND + 186 * MILLISECOND + 123212
                )
            )
            .isEqualTo("2022-11-10T22:04:54.186123212")
        Truth.assertThat(
                formatRealTimestamp(
                    NOV_10_2022 + 22 * HOUR + 4 * MINUTE + 54 * SECOND + 186 * MILLISECOND + 2
                )
            )
            .isEqualTo("2022-11-10T22:04:54.186000002")
        Truth.assertThat(formatRealTimestamp(NOV_10_2022))
            .isEqualTo("2022-11-10T00:00:00.000000000")
        Truth.assertThat(formatRealTimestamp(NOV_10_2022 + 1))
            .isEqualTo("2022-11-10T00:00:00.000000001")
    }

    @Test
    fun formatToRightType() {
        Truth.assertThat(TimeFormatter.format(1668117894186123212L))
            .isEqualTo("2022-11-10T22:04:54.186123212")
        Truth.assertThat(TimeFormatter.format(10 * DAY + 12 * HOUR)).isEqualTo("10d12h0m0s0ms0ns")
    }
}
