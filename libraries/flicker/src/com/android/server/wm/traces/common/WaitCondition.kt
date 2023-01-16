/*
 * Copyright (C) 2021 The Android Open Source Project
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

import kotlin.js.JsName

/**
 * The utility class to wait a condition with customized options. The default retry policy is 5
 * times with interval 1 second.
 *
 * @param <T> The type of the object to validate.
 *
 * <p>Sample:</p> <pre> // Simple case. if (Condition.waitFor("true value", () -> true)) {
 * ```
 *     println("Success");
 * ```
 * } // Wait for customized result with customized validation. String result =
 * WaitForCondition.Builder(supplier = () -> "Result string")
 * ```
 *         .withCondition(str -> str.equals("Expected string"))
 *         .withRetryIntervalMs(500)
 *         .withRetryLimit(3)
 *         .onFailure(str -> println("Failed on " + str)))
 *         .build()
 *         .waitFor()
 * ```
 * </pre>
 *
 * @param condition If it returns true, that means the condition is satisfied.
 */
class WaitCondition<T>
private constructor(
    @JsName("supplier") private val supplier: () -> T,
    @JsName("condition") private val condition: Condition<T>,
    @JsName("retryLimit") private val retryLimit: Int,
    @JsName("onLog") private val onLog: ((String, Boolean) -> Unit)?,
    @JsName("onFailure") private val onFailure: ((T) -> Any)?,
    @JsName("onRetry") private val onRetry: ((T) -> Any)?,
    @JsName("onSuccess") private val onSuccess: ((T) -> Any)?,
    @JsName("onStart") private val onStart: ((String) -> Any)?,
    @JsName("onEnd") private val onEnd: (() -> Any)?
) {
    /** @return `false` if the condition does not satisfy within the time limit. */
    @JsName("waitFor")
    fun waitFor(): Boolean {
        onStart?.invoke("waitFor")
        try {
            return doWaitFor()
        } finally {
            onEnd?.invoke()
        }
    }

    private fun doWaitFor(): Boolean {
        onLog?.invoke("***Waiting for $condition", /* isError */ false)
        var currState: T? = null
        var success = false
        for (i in 0..retryLimit) {
            val result = doWaitForRetry(i)
            success = result.first
            currState = result.second
            if (success) {
                break
            } else if (i < retryLimit) {
                onRetry?.invoke(currState)
            }
        }

        return if (success) {
            true
        } else {
            doNotifyFailure(currState)
            false
        }
    }

    private fun doWaitForRetry(retryNr: Int): Pair<Boolean, T> {
        onStart?.invoke("doWaitForRetry")
        try {
            val currState = supplier.invoke()
            return if (condition.isSatisfied(currState)) {
                onLog?.invoke("***Waiting for $condition ... Success!", /* isError */ false)
                onSuccess?.invoke(currState)
                Pair(true, currState)
            } else {
                val detailedMessage = condition.getMessage(currState)
                onLog?.invoke(
                    "***Waiting for $detailedMessage... retry=${retryNr + 1}",
                    /* isError */ true
                )
                Pair(false, currState)
            }
        } finally {
            onEnd?.invoke()
        }
    }

    private fun doNotifyFailure(currState: T?) {
        val detailedMessage =
            if (currState != null) {
                condition.getMessage(currState)
            } else {
                condition.toString()
            }
        onLog?.invoke("***Waiting for $detailedMessage ... Failed!", /* isError */ true)
        if (onFailure != null) {
            require(currState != null) { "Missing last result for failure notification" }
            onFailure.invoke(currState)
        }
    }

    class Builder<T>(
        @JsName("supplier") private val supplier: () -> T,
        @JsName("retryLimit") private var retryLimit: Int
    ) {
        @JsName("conditions") private val conditions = mutableListOf<Condition<T>>()
        private var onStart: ((String) -> Any)? = null
        private var onEnd: (() -> Any)? = null
        private var onFailure: ((T) -> Any)? = null
        private var onRetry: ((T) -> Any)? = null
        private var onSuccess: ((T) -> Any)? = null
        private var onLog: ((String, Boolean) -> Unit)? = null

        @JsName("withCondition")
        fun withCondition(condition: Condition<T>) = apply { conditions.add(condition) }

        @JsName("withConditionAndMessage")
        fun withCondition(message: String, condition: (T) -> Boolean) = apply {
            withCondition(Condition(message, condition))
        }

        @JsName("spreadConditionList")
        private fun spreadConditionList(): List<Condition<T>> =
            conditions.flatMap {
                if (it is ConditionList<T>) {
                    it.conditions
                } else {
                    listOf(it)
                }
            }

        /**
         * Executes the action when the condition does not satisfy within the time limit. The passed
         * object to the consumer will be the last result from the supplier.
         */
        @JsName("onFailure")
        fun onFailure(onFailure: (T) -> Any): Builder<T> = apply { this.onFailure = onFailure }

        @JsName("onLog")
        fun onLog(onLog: (String, Boolean) -> Unit): Builder<T> = apply { this.onLog = onLog }

        @JsName("onRetry")
        fun onRetry(onRetry: ((T) -> Any)? = null): Builder<T> = apply { this.onRetry = onRetry }

        @JsName("onStart")
        fun onStart(onStart: ((String) -> Any)? = null): Builder<T> = apply {
            this.onStart = onStart
        }

        @JsName("onEnd")
        fun onEnd(onEnd: (() -> Any)? = null): Builder<T> = apply { this.onEnd = onEnd }

        @JsName("onSuccess")
        fun onSuccess(onRetry: ((T) -> Any)? = null): Builder<T> = apply {
            this.onSuccess = onRetry
        }

        @JsName("build")
        fun build(): WaitCondition<T> =
            WaitCondition(
                supplier,
                ConditionList(spreadConditionList()),
                retryLimit,
                onLog,
                onFailure,
                onRetry,
                onSuccess,
                onStart,
                onEnd
            )
    }

    companion object {
        // TODO(b/112837428): Implement a incremental retry policy to reduce the unnecessary
        // constant time, currently keep the default as 5*1s because most of the original code
        // uses it, and some tests might be sensitive to the waiting interval.
        @JsName("DEFAULT_RETRY_LIMIT") const val DEFAULT_RETRY_LIMIT = 50
        @JsName("DEFAULT_RETRY_INTERVAL_MS") const val DEFAULT_RETRY_INTERVAL_MS = 100L
    }
}
