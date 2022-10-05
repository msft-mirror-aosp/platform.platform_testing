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

package com.android.server.wm.flicker.assertions

/**
 * Checks assertion on a single trace entry.
 *
 * @param <T> trace entry type to perform the assertion on. </T>
 */
typealias Assertion<T> = (T) -> Unit

/**
 * Utility class to store assertions with an identifier to help generate more useful debug data when
 * dealing with multiple assertions.
 *
 * @param assertion Assertion to execute
 * @param name Assertion name
 * @param isOptional If the assertion is optional (can fail) or not (must pass)
 */
open class NamedAssertion<T>(
    private val assertion: Assertion<T>,
    open val name: String,
    open val isOptional: Boolean = false
) : Assertion<T> {
    override fun invoke(target: T): Unit = assertion.invoke(target)

    override fun toString(): String = "Assertion($name)${if (isOptional) "[optional]" else ""}"

    /**
     * We can't check the actual assertion is the same. We are checking for the name, which should
     * have a 1:1 correspondence with the assertion, but there is no actual guarantee of the same
     * execution of the assertion even if isEqual() is true.
     */
    open fun isEqual(other: Any?): Boolean {
        if (other !is NamedAssertion<*>) {
            return false
        }
        if (name != other.name) {
            return false
        }
        if (isOptional != other.isOptional) {
            return false
        }
        return true
    }
}

/** Utility class to store assertions composed of multiple individual assertions */
class CompoundAssertion<T>(assertion: Assertion<T>, name: String, optional: Boolean) :
    NamedAssertion<T>(assertion, name) {
    private val assertions = mutableListOf<NamedAssertion<T>>()

    init {
        add(assertion, name, optional)
    }

    override val isOptional: Boolean
        get() = assertions.all { it.isOptional }

    override val name: String
        get() = assertions.joinToString(" and ") { it.name }

    /**
     * Executes all [assertions] on [target]
     *
     * In case of failure, returns the first non-optional failure (if available) or the first failed
     * assertion
     */
    override fun invoke(target: T) {
        val failures =
            assertions.mapNotNull { assertion ->
                val error = kotlin.runCatching { assertion.invoke(target) }.exceptionOrNull()
                if (error != null) {
                    Pair(assertion, error)
                } else {
                    null
                }
            }
        val nonOptionalFailure = failures.firstOrNull { !it.first.isOptional }
        if (nonOptionalFailure != null) {
            throw nonOptionalFailure.second
        }
        val firstFailure = failures.firstOrNull()
        if (firstFailure != null) {
            throw firstFailure.second
        }
    }

    override fun toString(): String = name

    override fun isEqual(other: Any?): Boolean {
        if (other !is CompoundAssertion<*>) {
            return false
        }
        if (!super.isEqual(other)) {
            return false
        }
        assertions.forEachIndexed { index, assertion ->
            if (!assertion.isEqual(other.assertions[index])) {
                return false
            }
        }
        return true
    }

    /** Adds a new assertion to the list */
    fun add(assertion: Assertion<T>, name: String, optional: Boolean) {
        assertions.add(NamedAssertion(assertion, name, optional))
    }
}
