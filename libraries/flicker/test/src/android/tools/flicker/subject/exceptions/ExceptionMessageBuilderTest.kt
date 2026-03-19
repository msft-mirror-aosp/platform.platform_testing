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

package android.tools.flicker.subject.exceptions

import android.tools.DAY_AS_NANOSECONDS
import android.tools.HOUR_AS_NANOSECONDS
import android.tools.MINUTE_AS_NANOSECONDS
import android.tools.Timestamps
import android.tools.testutils.ParsedTracesReader
import android.tools.traces.parsers.perfetto.TimestampConverter
import com.google.common.truth.Truth.assertThat
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ExceptionMessageBuilderTest {

    @Test
    fun generatesRealtimeTimestampFromConverter() {
        val snapshots =
            listOf(
                TimestampConverter.ClockSnapshot(
                    snapshotId = 1,
                    boottime = 100_000_000L,
                    realtime =
                        ((2026L - 1970L) * 365L + 14L /* 14 leap days */) * DAY_AS_NANOSECONDS +
                            3 * HOUR_AS_NANOSECONDS +
                            45 * MINUTE_AS_NANOSECONDS,
                    monotonic = null,
                )
            )
        val converter = TimestampConverter(snapshots)

        val reader = ParsedTracesReader(artifacts = emptyArray(), timestampConverter = converter)

        val builder = ExceptionMessageBuilder()
        builder.setReader(reader)
        // Set an elapsed timestamp so the hasElapsedTimestamp check passes
        builder.setTimestamp(Timestamps.from(elapsedNanos = 100_000_000L))

        val message = builder.build()

        assertThat(message).contains("Where?")
        assertThat(message).contains("2026-01-01T03:45:00.000000000 (1767239100000000000ns)")
    }

    @Test
    fun skipsRealtimeTimestampIfNoConverter() {
        val reader = ParsedTracesReader(artifacts = emptyArray(), timestampConverter = null)

        val builder = ExceptionMessageBuilder()
        builder.setReader(reader)
        builder.setTimestamp(Timestamps.from(elapsedNanos = 100_000_000L))

        val message = builder.build()

        assertThat(message).contains("Where?")
        // Should not contain realtime because the converter was null
        assertThat(message).doesNotContain("Realtime:")
    }
}
