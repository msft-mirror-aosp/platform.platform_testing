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

package com.android.server.wm.flicker.assertions

import com.android.server.wm.flicker.common.AssertionResult

/**
 * Checks assertion on a single trace entry.
 *
 * @param <T> trace entry type to perform the assertion on. </T>
 */
typealias TraceAssertion<T> = (T) -> AssertionResult

/**
 * Collection of functional interfaces and classes representing assertions and their associated
 * results. Assertions are functions that are applied over a single trace entry and returns a result
 * which includes a detailed reason if the assertion fails.
 */
object Assertions {
    @JvmStatic
    fun <T> ((T) -> AssertionResult).negate(): TraceAssertion<T> {
        return { it: T -> this.invoke(it).negate() }
    }
}

/**
 * Returns an assertion that represents the logical negation of this assertion.
 *
 * @return a assertion that represents the logical negation of this assertion
 */

/**
 * Utility class to store assertions with an identifier to help generate more useful debug data
 * when dealing with multiple assertions.
 */
open class NamedAssertion<T>(
    private val assertion: TraceAssertion<T>,
    open val name: String
) : TraceAssertion<T> {
    override fun invoke(t: T): AssertionResult = assertion.invoke(t)

    override fun toString(): String = "Assertion($name)"
}

class CompoundAssertion<T>(assertion: TraceAssertion<T>, name: String) :
        NamedAssertion<T>(assertion, name) {
    private val assertions: MutableList<NamedAssertion<T>> = ArrayList()

    init {
        add(assertion, name)
    }

    override val name: String
        get() = assertions.joinToString(" and ") { p: NamedAssertion<T> -> p.name }

    override fun invoke(t: T): AssertionResult {
        val assertionResults = assertions.map { it.invoke(t) }
        val passed = assertionResults.all { it.passed() }
        val reason = assertionResults
                .filterNot { it.passed() }
                .joinToString(" and ") { it.reason }
        return assertionResults
                .map { AssertionResult(reason, name, it.timestamp, passed) }
                .firstOrNull() ?: AssertionResult(reason, success = passed)
    }

    override fun toString(): String = "CompoundAssertion($name)"

    fun add(assertion: TraceAssertion<T>, name: String) {
        assertions.add(NamedAssertion(assertion, name))
    }
}