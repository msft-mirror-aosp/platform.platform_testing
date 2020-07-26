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

package com.android.server.wm.flicker

import androidx.annotation.VisibleForTesting
import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit

/** Contains the result of an assertion including the reason for failed assertions.  */
data class AssertionResult @JvmOverloads constructor(
    val reason: String,
    val assertionName: String = "",
    val timestamp: Long = 0,
    val success: Boolean
) {
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

    @VisibleForTesting
    fun assertPassed() {
        Truth.assertWithMessage(this.reason).that(this.passed()).isTrue()
    }

    @JvmOverloads
    @VisibleForTesting
    fun assertFailed(reason: String? = null) {
        Truth.assertWithMessage(this.reason).that(this.failed()).isTrue()
        if (reason != null) {
            Truth.assertThat(this.reason).contains(reason)
        }
    }

    override fun toString(): String = """
        Timestamp: ${prettyTimestamp(timestamp)}
        Assertion: $assertionName
        Reason:   $reason
        """.trimIndent()

    private fun prettyTimestamp(timestampNs: Long): String {
        var remainingNs = timestampNs
        val prettyTimestamp = StringBuilder()
        val timeUnits = arrayOf(
                TimeUnit.DAYS,
                TimeUnit.HOURS,
                TimeUnit.MINUTES,
                TimeUnit.SECONDS,
                TimeUnit.MILLISECONDS
        )
        val unitSuffixes = arrayOf("d", "h", "m", "s", "ms")
        for (i in timeUnits.indices) {
            val convertedTime = timeUnits[i].convert(remainingNs, TimeUnit.NANOSECONDS)
            remainingNs -= TimeUnit.NANOSECONDS.convert(convertedTime, timeUnits[i])
            prettyTimestamp.append(convertedTime).append(unitSuffixes[i])
        }
        return prettyTimestamp.toString()
    }

    companion object {
        const val NEGATION_PREFIX = "!"
    }
}