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

import kotlin.math.max

/**
 * Time class with all available timestamp types
 *
 * @param elapsedNanos Nanoseconds since boot, including time spent in sleep.
 * @param systemUptimeNanos Nanoseconds since boot, not counting time spent in deep sleep
 * @param unixNanos Nanoseconds since Unix epoch
 */
data class Timestamp(
    val elapsedNanos: Long = NULL_TIMESTAMP,
    val systemUptimeNanos: Long = NULL_TIMESTAMP,
    val unixNanos: Long = NULL_TIMESTAMP,
) : Comparable<Timestamp> {

    enum class PreferredType {
        ELAPSED,
        SYSTEM_UPTIME,
        UNIX,
        ANY
    }

    // The preferred and most accurate time type to use when running Timestamp operations or
    // comparisons
    private val preferredType: PreferredType
        get() {
            if (elapsedNanos != NULL_TIMESTAMP && systemUptimeNanos != NULL_TIMESTAMP) {
                return PreferredType.ANY
            }

            // We assume that elapsedNanos and systemUptimeNanos are more accurate in traces, so we
            // prefer those
            if (elapsedNanos != NULL_TIMESTAMP) {
                return PreferredType.ELAPSED
            }
            if (systemUptimeNanos != NULL_TIMESTAMP) {
                return PreferredType.SYSTEM_UPTIME
            }
            if (unixNanos != NULL_TIMESTAMP) {
                return PreferredType.UNIX
            }

            throw RuntimeException("No valid timestamp available")
        }

    val hasAllTimestamps: Boolean =
        this.elapsedNanos != NULL_TIMESTAMP &&
            this.systemUptimeNanos != NULL_TIMESTAMP &&
            this.unixNanos != NULL_TIMESTAMP

    val hasUnixTimestamp: Boolean = unixNanos != NULL_TIMESTAMP

    override fun compareTo(other: Timestamp): Int {
        var useType = PreferredType.ANY
        if (other.preferredType == this.preferredType) {
            useType = this.preferredType
        } else if (this.preferredType == PreferredType.ANY) {
            useType = other.preferredType
        } else if (other.preferredType == PreferredType.ANY) {
            useType = this.preferredType
        }

        when (useType) {
            PreferredType.ELAPSED -> {
                return when {
                    this.elapsedNanos > other.elapsedNanos -> 1
                    this.elapsedNanos < other.elapsedNanos -> -1
                    else -> 0
                }
            }
            PreferredType.SYSTEM_UPTIME -> {
                return when {
                    this.systemUptimeNanos > other.systemUptimeNanos -> 1
                    this.systemUptimeNanos < other.systemUptimeNanos -> -1
                    else -> 0
                }
            }
            PreferredType.UNIX,
            PreferredType.ANY -> {
                // Continue to code below to be handled
            }
        }

        // If preferred timestamps don't match then comparing UNIX timestamps is probably most
        // accurate
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

    operator fun minus(nanos: Long): Timestamp {
        val elapsedNanos = max(this.elapsedNanos - nanos, NULL_TIMESTAMP)
        val systemUptimeNanos = max(this.systemUptimeNanos - nanos, NULL_TIMESTAMP)
        val unixNanos = max(this.unixNanos - nanos, NULL_TIMESTAMP)
        return Timestamp(elapsedNanos, systemUptimeNanos, unixNanos)
    }

    operator fun minus(timestamp: Timestamp): Timestamp {
        val elapsedNanos =
            if (this.elapsedNanos != NULL_TIMESTAMP && timestamp.elapsedNanos != NULL_TIMESTAMP) {
                this.elapsedNanos - timestamp.elapsedNanos
            } else {
                NULL_TIMESTAMP
            }
        val systemUptimeNanos =
            if (
                this.systemUptimeNanos != NULL_TIMESTAMP &&
                    timestamp.systemUptimeNanos != NULL_TIMESTAMP
            ) {
                this.systemUptimeNanos - timestamp.systemUptimeNanos
            } else {
                NULL_TIMESTAMP
            }
        val unixNanos =
            if (this.unixNanos != NULL_TIMESTAMP && timestamp.unixNanos != NULL_TIMESTAMP) {
                this.unixNanos - timestamp.unixNanos
            } else {
                NULL_TIMESTAMP
            }
        return Timestamp(elapsedNanos, systemUptimeNanos, unixNanos)
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
        val MIN = Timestamp(1, 1, 1)
        val MAX = Timestamp(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)
    }
}
