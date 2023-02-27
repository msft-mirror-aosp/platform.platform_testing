/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.common.flicker.subject

import android.tools.common.Timestamp
import android.tools.common.flicker.assertions.Fact

/** Base subject for flicker assertions */
abstract class FlickerSubject {
    abstract val timestamp: Timestamp
    protected abstract val parent: FlickerSubject?

    protected abstract val selfFacts: List<Fact>
    val completeFacts: List<Fact>
        get() {
            val facts = selfFacts.toMutableList()
            parent?.run {
                val ancestorFacts = this.completeFacts
                facts.addAll(ancestorFacts)
            }
            return facts
        }

    /**
     * Fails an assertion on a subject
     *
     * @param reason for the failure
     */
    open fun fail(reason: List<Fact>): FlickerSubject = apply {
        require(reason.isNotEmpty()) { "Failure should contain at least 1 fact" }
        throw FlickerSubjectException(timestamp, reason + completeFacts)
    }

    fun fail(reason: Fact, vararg rest: Fact): FlickerSubject = apply {
        val what = mutableListOf(reason).also { it.addAll(rest) }
        fail(what)
    }

    /**
     * Fails an assertion on a subject
     *
     * @param reason for the failure
     */
    fun fail(reason: Fact): FlickerSubject = apply { fail(listOf(reason)) }

    /**
     * Fails an assertion on a subject
     *
     * @param reason for the failure
     */
    fun fail(reason: String): FlickerSubject = apply { fail(Fact("Reason", reason)) }

    /**
     * Fails an assertion on a subject
     *
     * @param reason for the failure
     * @param value for the failure
     */
    fun fail(reason: String, value: Any): FlickerSubject = apply { fail(Fact(reason, value)) }

    /**
     * Fails an assertion on a subject
     *
     * @param reason for the failure
     */
    fun fail(reason: Throwable) {
        if (reason is FlickerSubjectException) {
            throw reason
        } else {
            throw FlickerSubjectException(timestamp, completeFacts, reason)
        }
    }

    fun check(lazyMessage: () -> String): CheckSubjectBuilder {
        return CheckSubjectBuilder(this, lazyMessage)
    }

    companion object {
        const val ASSERTION_TAG = "Assertion"
    }
}
