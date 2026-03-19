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

/**
 * Utility to convert timestamps between clocks (e.g. boottime to realtime) replicates the behavior
 * of Perfetto's clock_snapshot table and ClockConverter.
 *
 * Reference:
 * external/perfetto/src/trace_processor/perfetto_sql/intrinsics/functions/clock_functions.h
 */
class TimestampConverter(private val snapshots: List<ClockSnapshot>) {
    data class ClockSnapshot(
        val snapshotId: Long,
        val boottime: Long?,
        val realtime: Long?,
        val monotonic: Long?,
    )

    fun toRealTime(elapsedNanos: Long): Long? {
        if (snapshots.isEmpty()) return null

        // Find the latest snapshot that is <= elapsedNanos, or fallback to the first snapshot
        // This mirrors the `std::prev(upper_bound())` search used in Perfetto's ClockTracker
        val validSnapshots = snapshots.filter { it.boottime != null && it.realtime != null }
        if (validSnapshots.isEmpty()) return null

        val targetSnapshot =
            validSnapshots.filter { it.boottime!! <= elapsedNanos }.maxByOrNull { it.boottime!! }
                ?: validSnapshots.minByOrNull { it.boottime!! }
                ?: return null

        val offset = targetSnapshot.realtime!! - targetSnapshot.boottime!!
        return elapsedNanos + offset
    }

    fun toMonotonicTime(elapsedNanos: Long): Long? {
        if (snapshots.isEmpty()) return null

        val validSnapshots = snapshots.filter { it.boottime != null && it.monotonic != null }
        if (validSnapshots.isEmpty()) return null

        val targetSnapshot =
            validSnapshots.filter { it.boottime!! <= elapsedNanos }.maxByOrNull { it.boottime!! }
                ?: validSnapshots.minByOrNull { it.boottime!! }
                ?: return null

        val offset = targetSnapshot.monotonic!! - targetSnapshot.boottime!!
        return elapsedNanos + offset
    }
}
