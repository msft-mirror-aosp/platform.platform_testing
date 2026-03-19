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

package android.tools.traces.parsers.perfetto

import com.google.common.truth.Truth.assertThat
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TimestampConverterTest {
    @Test
    fun convertsToRealTimeUsingLatestSnapshot() {
        val snapshots =
            listOf(
                TimestampConverter.ClockSnapshot(
                    snapshotId = 1,
                    boottime = 1000L,
                    realtime = 1500L,
                    monotonic = 1200L,
                ),
                TimestampConverter.ClockSnapshot(
                    snapshotId = 2,
                    boottime = 2000L,
                    realtime = 2600L, // offset is 600
                    monotonic = 2300L,
                ),
            )
        val converter = TimestampConverter(snapshots)

        // Exact match on snapshot 1 (offset 500)
        assertThat(converter.toRealTime(1000L)).isEqualTo(1500L)
        // Between snapshot 1 and 2, should use snapshot 1 (offset 500)
        assertThat(converter.toRealTime(1500L)).isEqualTo(2000L)
        // Exact match on snapshot 2 (offset 600)
        assertThat(converter.toRealTime(2000L)).isEqualTo(2600L)
        // After snapshot 2, should use snapshot 2 (offset 600)
        assertThat(converter.toRealTime(2500L)).isEqualTo(3100L)
        // Before snapshot 1, should fallback to snapshot 1 (offset 500)
        assertThat(converter.toRealTime(500L)).isEqualTo(1000L)
    }

    @Test
    fun convertsToMonotonicTimeUsingLatestSnapshot() {
        val snapshots =
            listOf(
                TimestampConverter.ClockSnapshot(
                    snapshotId = 1,
                    boottime = 1000L,
                    realtime = 1500L,
                    monotonic = 1200L, // offset 200
                ),
                TimestampConverter.ClockSnapshot(
                    snapshotId = 2,
                    boottime = 2000L,
                    realtime = 2600L,
                    monotonic = 2300L, // offset 300
                ),
            )
        val converter = TimestampConverter(snapshots)

        assertThat(converter.toMonotonicTime(1000L)).isEqualTo(1200L)
        assertThat(converter.toMonotonicTime(1500L)).isEqualTo(1700L)
        assertThat(converter.toMonotonicTime(2000L)).isEqualTo(2300L)
        assertThat(converter.toMonotonicTime(2500L)).isEqualTo(2800L)
        assertThat(converter.toMonotonicTime(500L)).isEqualTo(700L)
    }

    @Test
    fun ignoresSnapshotsWithNullClocks() {
        val snapshots =
            listOf(
                TimestampConverter.ClockSnapshot(
                    snapshotId = 1,
                    boottime = 1000L,
                    realtime = null,
                    monotonic = 1200L, // offset 200
                ),
                TimestampConverter.ClockSnapshot(
                    snapshotId = 2,
                    boottime = 2000L,
                    realtime = 2600L, // offset 600
                    monotonic = 2300L, // offset 300
                ),
            )
        val converter = TimestampConverter(snapshots)

        // For realtime, snapshot 1 lacks realtime, so uses snapshot 2 fallback
        assertThat(converter.toRealTime(1000L)).isEqualTo(1600L)
        // For monotonic, snapshot 1 is valid
        assertThat(converter.toMonotonicTime(1000L)).isEqualTo(1200L)
    }

    @Test
    fun returnsNullWhenNoSnapshots() {
        val converter = TimestampConverter(emptyList())
        assertThat(converter.toRealTime(1000L)).isNull()
        assertThat(converter.toMonotonicTime(1000L)).isNull()
    }
}
