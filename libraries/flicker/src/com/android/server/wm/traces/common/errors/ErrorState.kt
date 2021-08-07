package com.android.server.wm.traces.common.errors

import com.android.server.wm.traces.common.ITraceEntry
import com.android.server.wm.traces.common.prettyTimestamp

/**
 * A state at a particular time within a trace that holds a list of errors there may be.
 * @param errors Errors contained in the state
 * @param timestamp Timestamp of this state
 */
data class ErrorState(
    val errors: Array<Error>,
    override val timestamp: Long
) : ITraceEntry {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ErrorState) return false
        if (timestamp != other.timestamp) return false
        if (errors != other.errors) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + errors.contentDeepHashCode()
        return result
    }

    override fun toString(): String = "FlickerErrorState(" +
            "timestamp=${prettyTimestamp(timestamp)}, numberOfErrors=${errors.size})"
}
