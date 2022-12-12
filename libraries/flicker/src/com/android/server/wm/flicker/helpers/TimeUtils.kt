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

import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.Timestamp.Companion.NULL_TIMESTAMP
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val MILLISECOND_AS_NANOSECONDS: Long = 1000000
private const val SECOND_AS_NANOSECONDS: Long = 1000000000
private const val MINUTE_AS_NANOSECONDS: Long = 60000000000
private const val HOUR_AS_NANOSECONDS: Long = 3600000000000
private const val DAY_AS_NANOSECONDS: Long = 86400000000000

fun Timestamp.format(): String {
    if (this == Timestamp.EMPTY) {
        return "<NO TIMESTAMP>"
    }

    val unixNanos = this.unixNanos
    if (unixNanos != NULL_TIMESTAMP) {
        return "${formatRealTimestamp(unixNanos)} (${unixNanos}ns)"
    }
    val systemUptimeNanos = this.systemUptimeNanos
    if (systemUptimeNanos != NULL_TIMESTAMP) {
        return "${formatElapsedTimestamp(systemUptimeNanos)} (${systemUptimeNanos}ns)"
    }
    val elapsedNanos = this.elapsedNanos
    if (elapsedNanos != NULL_TIMESTAMP) {
        return "${formatElapsedTimestamp(elapsedNanos)} (${elapsedNanos}ns)"
    }

    throw Throwable("Timestamp had no valid timestamps sets")
}

fun formatElapsedTimestamp(timestampNs: Long): String {
    var remainingNs = timestampNs
    val prettyTimestamp = StringBuilder()

    val timeUnitToNanoSeconds =
        mapOf(
            "d" to DAY_AS_NANOSECONDS,
            "h" to HOUR_AS_NANOSECONDS,
            "m" to MINUTE_AS_NANOSECONDS,
            "s" to SECOND_AS_NANOSECONDS,
            "ms" to MILLISECOND_AS_NANOSECONDS,
            "ns" to 1,
        )

    for ((timeUnit, ns) in timeUnitToNanoSeconds) {
        val convertedTime = remainingNs / ns
        remainingNs %= ns
        if (prettyTimestamp.isEmpty() && convertedTime == 0L) {
            // Trailing 0 unit
            continue
        }
        prettyTimestamp.append("$convertedTime$timeUnit")
    }

    if (prettyTimestamp.isEmpty()) {
        return "0ns"
    }

    return prettyTimestamp.toString()
}

fun formatRealTimestamp(timestampNs: Long): String {
    val timestampMs = timestampNs / MILLISECOND_AS_NANOSECONDS
    val remainderNs = timestampNs % MILLISECOND_AS_NANOSECONDS
    val date = Date(timestampMs)

    val timeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH)
    timeFormatter.timeZone = TimeZone.getTimeZone("UTC")

    return "${timeFormatter.format(date)}${remainderNs.toString().padStart(6, '0')}"
}

class TimeFormatter {
    companion object {
        fun format(timestampNs: Long): String {
            // We assume that any timestamp greater than about 30 years since epoch (i.e. year 2000)
            // should be interpreted as a real timestamp and anything below is an elapsed timestamp.
            return if (timestampNs > DAY_AS_NANOSECONDS * 365 * 30) {
                formatRealTimestamp(timestampNs)
            } else {
                formatElapsedTimestamp(timestampNs)
            }
        }
    }
}
