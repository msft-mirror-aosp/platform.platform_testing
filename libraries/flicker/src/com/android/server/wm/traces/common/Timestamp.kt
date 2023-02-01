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

/**
 * Time class with all available timestamp types
 *
 * @param elapsedNanos Nanoseconds since boot, including time spent in sleep.
 * @param systemUptimeNanos Nanoseconds since boot, not counting time spent in deep sleep
 * @param unixNanos Nanoseconds since Unix epoch
 */
data class Timestamp(
    val elapsedNanos: Long = 0,
    val systemUptimeNanos: Long = 0,
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
