/*
 * Copyright (C) 2020 The Android Open Source Project
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

/** Contains the result of an assertion including the reason for failed assertions.  */
data class AssertionResult constructor(
    val reason: String,
    val assertionName: String = "",
    val timestamp: Long = 0,
    val success: Boolean = true
) {
    /**
    * Creates a new instance with name and success
    *
    * @param assertionName Name of the assertion
    * @param success If the assertion passes or not
    */
    constructor(assertionName: String, success: Boolean): this(
            reason = "",
            assertionName = assertionName,
            timestamp = 0,
            success = success
    )

    /**
     * Creates a new instance with name and assertion to determine success.
     *
     * @param assertionName Name of the assertion
     * @param predicate Expression to determine if the assertion passes or not
     */
    constructor(assertionName: String, predicate: () -> Boolean): this(
        reason = "",
        assertionName = assertionName,
        timestamp = 0,
        success = predicate()
    )

    /** Returns the negated `Result` and adds a negation prefix to the assertion name.  */
    fun negate(): AssertionResult {
        val negatedAssertionName: String = if (assertionName.startsWith(NEGATION_PREFIX)) {
            assertionName.substring(NEGATION_PREFIX.length + 1)
        } else {
            NEGATION_PREFIX + assertionName
        }
        return AssertionResult(reason, negatedAssertionName, timestamp, !success)
    }

    fun passed(): Boolean = success

    fun failed(): Boolean = !success

    override fun toString(): String = """
        Timestamp: ${prettyTimestamp(timestamp)}
        Assertion: $assertionName
        Reason:   $reason
        """.trimIndent()

    private val MILLISECOND_AS_NANOSECONDS: Long = 1000000
    private val SECOND_AS_NANOSECONDS: Long = 1000000000
    private val MINUTE_AS_NANOSECONDS: Long = 60000000000
    private val HOUR_AS_NANOSECONDS: Long = 3600000000000
    private val DAY_AS_NANOSECONDS: Long = 86400000000000

    private fun prettyTimestamp(timestampNs: Long): String {
        var remainingNs = timestampNs
        val prettyTimestamp = StringBuilder()

        val timeUnitToNanoSeconds = mapOf(
                "d" to DAY_AS_NANOSECONDS,
                "h" to HOUR_AS_NANOSECONDS,
                "m" to MINUTE_AS_NANOSECONDS,
                "s" to SECOND_AS_NANOSECONDS,
                "ms" to MILLISECOND_AS_NANOSECONDS
        )

        for ((timeUnit, ns) in timeUnitToNanoSeconds) {
            val convertedTime = remainingNs / ns
            remainingNs %= ns
            prettyTimestamp.append("$convertedTime$timeUnit")
        }

        return prettyTimestamp.toString()
    }

    companion object {
        const val NEGATION_PREFIX = "!"
    }
}