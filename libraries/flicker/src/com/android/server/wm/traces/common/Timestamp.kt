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

package com.android.server.wm.traces.common

class Timestamp(
    // TODO: We should probably update all traces that make use of this timestamp to just use
    //       elapsedNanos or unixNanos
    /* Nanoseconds since boot, including time spent in sleep. */
    val elapsedNanos: Long = 0,
    /* Nanoseconds since boot, not counting time spent in deep sleep */
    val systemUptimeNanos: Long = 0,
    /* Nanoseconds since Unix epoch */
    val unixNanos: Long = 0
) : Comparable<Timestamp> {

    override fun compareTo(other: Timestamp): Int {
        // Comparing UNIX timestamp is more accurate
        if (this.unixNanos != NULL_TIMESTAMP && other.unixNanos != NULL_TIMESTAMP) {
            return when {
                this.unixNanos > other.unixNanos -> 1
                this.unixNanos < other.unixNanos -> -1
                else -> 0
            }
        }

        // Assumes timestamps are collected from the same device
        if (this.elapsedNanos != NULL_TIMESTAMP && other.elapsedNanos != NULL_TIMESTAMP) {
            return when {
                this.elapsedNanos > other.elapsedNanos -> 1
                this.elapsedNanos < other.elapsedNanos -> -1
                else -> 0
            }
        }

        if (this.systemUptimeNanos != NULL_TIMESTAMP && other.elapsedNanos != NULL_TIMESTAMP) {
            return when {
                this.systemUptimeNanos > other.elapsedNanos -> 1
                this.systemUptimeNanos < other.elapsedNanos -> -1
                else -> 0
            }
        }

        throw Throwable("Timestamps are not comparable no common timestamp types to compare.")
    }

    override fun equals(other: Any?): Boolean {
        return other is Timestamp &&
            this.elapsedNanos == other.elapsedNanos &&
            this.systemUptimeNanos == other.systemUptimeNanos &&
            this.unixNanos == other.unixNanos
    }

    override fun hashCode(): Int {
        var result = elapsedNanos.hashCode()
        result = 31 * result + systemUptimeNanos.hashCode()
        result = 31 * result + unixNanos.hashCode()
        return result
    }

    companion object {
        fun from(elapsedNanos: Long, elapsedOffsetNanos: Long): Timestamp {
            return Timestamp(
                elapsedNanos = elapsedNanos,
                unixNanos = elapsedNanos + elapsedOffsetNanos
            )
        }

        const val NULL_TIMESTAMP = 0L
        val EMPTY = Timestamp(NULL_TIMESTAMP, NULL_TIMESTAMP, NULL_TIMESTAMP)
        val MIN = EMPTY
        val MAX = Timestamp(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)
    }
}
