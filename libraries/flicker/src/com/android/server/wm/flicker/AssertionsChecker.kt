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

/**
 * Captures some of the common logic in [LayersTraceSubject] and [WmTraceSubject] used
 * to filter trace entries and combine multiple assertions.
 *
 * @param <T> trace entry type </T>
 */
class AssertionsChecker<T : ITraceEntry> {
    private var filterEntriesByRange = false
    private var filterStartTime: Long = 0
    private var filterEndTime: Long = 0
    private var skipUntilFirstAssertion = false
    private var option = AssertionOption.NONE
    private val assertions = mutableListOf<CompoundAssertion<T>>()

    /**
     * Start a new set of assertions and add [assertion] to it.
     */
    fun add(assertion: TraceAssertion<T>, name: String) {
        assertions.add(CompoundAssertion(assertion, name))
    }

    /**
     * Append [assertion] to the last existing set of assertions.
     */
    fun append(assertion: TraceAssertion<T>, name: String) {
        assertions.last().add(assertion, name)
    }

    /**
     * Ignores the first entries in the trace, until the first assertion passes. If it reaches the
     * end of the trace without passing any assertion, return a failure with the name/reason from
     * the first assertion
     */
    fun skipUntilFirstAssertion() {
        skipUntilFirstAssertion = true
    }

    /**
     * Run the assertions for all trace entries within the specified time range
     */
    fun filterByRange(startTime: Long, endTime: Long) {
        filterEntriesByRange = true
        filterStartTime = startTime
        filterEndTime = endTime
    }

    private fun setOption(option: AssertionOption) {
        require(!(this.option != AssertionOption.NONE && option != this.option)) {
            "Cannot use ${this.option} option with $option option."
        }
        this.option = option
    }

    /**
     * Run the assertions only in the first trace entry
     */
    fun checkFirstEntry() {
        setOption(AssertionOption.CHECK_FIRST_ENTRY)
    }

    /**
     * Run the assertions only in the last  trace entry
     */
    fun checkLastEntry() {
        setOption(AssertionOption.CHECK_LAST_ENTRY)
    }

    fun checkChangingAssertions() {
        setOption(AssertionOption.CHECK_CHANGING_ASSERTIONS)
    }

    /**
     * Filters trace entries then runs assertions returning a list of failures.
     *
     * @param entries list of entries to perform assertions on
     * @return list of failed assertion results
     */
    fun test(entries: List<T>): List<AssertionResult> {
        val filteredEntries = if (filterEntriesByRange) {
            entries.filter { it.timestamp in filterStartTime..filterEndTime }
        } else {
            entries
        }

        return when (option) {
            AssertionOption.CHECK_CHANGING_ASSERTIONS -> assertChanges(filteredEntries)
            AssertionOption.CHECK_FIRST_ENTRY -> assertEntry(filteredEntries.first())
            AssertionOption.CHECK_LAST_ENTRY -> assertEntry(filteredEntries.last())
            else -> assertAll(filteredEntries)
        }
    }

    /**
     * Steps through each trace entry checking if provided assertions are true in the order they are
     * added. Each assertion must be true for at least a single trace entry.
     *
     *
     * This can be used to check for asserting a change in property over a trace. Such as
     * visibility for a window changes from true to false or top-most window changes from A to B and
     * back to A again.
     *
     *
     * It is also possible to ignore failures on initial elements, until the first assertion
     * passes, this allows the trace to be recorded for longer periods, and the checks to happen
     * only after some time.
     */
    private fun assertChanges(entries: List<T>): List<AssertionResult> {
        val failures: MutableList<AssertionResult> = ArrayList()
        var entryIndex = 0
        var assertionIndex = 0
        var lastPassedAssertionIndex = -1
        if (assertions.size == 0) {
            return failures
        }
        while (assertionIndex < assertions.size && entryIndex < entries.size) {
            val currentAssertion: NamedAssertion<T> = assertions[assertionIndex]
            val result: AssertionResult = currentAssertion.invoke(entries[entryIndex])
            val ignoreFailure = skipUntilFirstAssertion && lastPassedAssertionIndex == -1
            if (result.passed()) {
                lastPassedAssertionIndex = assertionIndex
                entryIndex++
                continue
            }
            if (ignoreFailure) {
                entryIndex++
                continue
            }
            if (lastPassedAssertionIndex != assertionIndex) {
                failures.add(result)
                break
            }
            assertionIndex++
            if (assertionIndex == assertions.size) {
                failures.add(result)
                break
            }
        }
        if (lastPassedAssertionIndex == -1 && assertions.size > 0 && failures.isEmpty()) {
            failures.add(AssertionResult(success = false, reason = assertions[0].name))
        }
        if (failures.isEmpty() && assertionIndex != assertions.size - 1) {
            var reason = "Assertion ${assertions[assertionIndex].name} never became false"
            reason += "\n\tPassed assertions: " +
                    assertions.take(assertionIndex)
                            .joinToString(",") { it.name }
            reason += "\n\tUntested assertions: " +
                    assertions.drop(assertionIndex + 1)
                            .joinToString(",") { it.name }
            val result = AssertionResult(
                    "Not all assertions passed.$reason",
                    assertionName = "assertChanges",
                    timestamp = 0,
                    success = false)
            failures.add(result)
        }
        return failures
    }

    private fun assertEntry(entry: T): List<AssertionResult> {
        val failures: MutableList<AssertionResult> = ArrayList()
        for (assertion in assertions) {
            val result: AssertionResult = assertion.invoke(entry)
            if (result.failed()) {
                failures.add(result)
            }
        }
        return failures
    }

    private fun assertAll(entries: List<T>): List<AssertionResult> {
        return assertions.flatMap { assertion ->
            entries.map(assertion).filter { it.failed() }
        }
    }

    private enum class AssertionOption {
        NONE, CHECK_CHANGING_ASSERTIONS, CHECK_FIRST_ENTRY, CHECK_LAST_ENTRY
    }
}
