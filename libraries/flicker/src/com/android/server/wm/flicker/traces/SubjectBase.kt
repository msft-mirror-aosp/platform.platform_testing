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

package com.android.server.wm.flicker.traces

import com.android.server.wm.flicker.assertions.AssertionsChecker
import com.android.server.wm.flicker.assertions.TraceAssertion
import com.android.server.wm.flicker.common.AssertionResult
import com.android.server.wm.flicker.common.traces.IRangedSubject
import com.android.server.wm.flicker.common.traces.ITrace
import com.android.server.wm.flicker.common.traces.ITraceEntry
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import java.nio.file.Paths

/**
 * Base truth subject.
 */
abstract class SubjectBase<Trace : ITrace<Entry>, Entry : ITraceEntry> protected constructor(
    fm: FailureMetadata,
    private val subject: Trace
) : Subject(fm, subject), IRangedSubject<Entry> {
    protected val assertionsChecker = AssertionsChecker<Entry>()
    protected var newAssertion = true

    protected fun addAssertion(name: String, assertion: TraceAssertion<Entry>) {
        if (newAssertion) {
            assertionsChecker.add(assertion, name)
        } else {
            assertionsChecker.append(assertion, name)
        }
    }

    /**
     * Run the assertions for all trace entries
     */
    override fun forAllEntries() {
        assertionsChecker.checkChangingAssertions()
        test()
    }

    /**
     * Run the assertions for all trace entries within the specified time range
     */
    override fun forRange(startTime: Long, endTime: Long) {
        assertionsChecker.filterByRange(startTime, endTime)
        test()
    }

    /**
     * Run the assertions only in the first trace entry
     */
    override fun inTheBeginning() {
        check("No entries found.").that(subject.entries).isNotEmpty()
        assertionsChecker.checkFirstEntry()
        test()
    }

    /**
     * Run the assertions only in the last  trace entry
     */
    override fun atTheEnd() {
        check("No entries found.").that(subject.entries).isNotEmpty()
        assertionsChecker.checkLastEntry()
        test()
    }

    /**
     * Run the assertions
     */
    private fun test() {
        val failures = assertionsChecker.test(subject.entries)
        if (failures.isNotEmpty()) {
            val failureLogs = failures.joinToString("\n") { it.toString() }
            var tracePath = ""
            if (subject.hasSource()) {
                val failureTracePath = Paths.get(subject.source)
                tracePath = """

                    $traceName Trace can be found in: ${failureTracePath.toAbsolutePath()}
                    Checksum: ${subject.sourceChecksum}

                    """.trimIndent()
            }
            failWithActual(tracePath + "\n" + failureLogs, subject)
        }
    }

    abstract val traceName: String

    /**
     * Checks whether all the trace entries on the list are visible for more than one consecutive
     * entry
     *
     * @param [visibleEntries] a list of all the entries with their name, their index inside the
     * entries list and their corresponding [ITraceEntry]
     * @param [assertionName] the name of the assertion
     * @return an [AssertionResult] indicating if all the entries are shown more than one
     * consecutive time
     */
    protected fun visibleEntriesShownMoreThanOneConsecutiveTime(
        visibleEntries: List<Triple<String, Int, ITraceEntry>>,
        assertionName: String
    ):
            AssertionResult {
        visibleEntries.distinct() //
                .groupBy({ it.first }, { Pair(it.second, it.third) }) //
                .forEach { (entryName, indexesList) ->
            val listOfConsecutiveNumbers = getListsOfConsecutiveNumbers(indexesList)
            listOfConsecutiveNumbers.forEach {
                if (it.size <= 1) {
                    val timestamp = if (it.size == 1) it[0].second.timestamp else 0L
                    return AssertionResult(
                            "No two consecutive visible entries shown for $entryName",
                            assertionName, timestamp, false)
                }
            }
        }
        return AssertionResult("All visible entries are shown for more than one consecutive time",
                assertionName,
                0L, true)
    }

    private fun getListsOfConsecutiveNumbers(indexList: List<Pair<Int, ITraceEntry>>):
            MutableList<MutableList<Pair<Int, ITraceEntry>>> {
        return indexList.fold(mutableListOf()) { accumulator, index ->
            if (accumulator.isEmpty() || accumulator.last().last().first != index.first - 1) {
                accumulator.add(mutableListOf(index))
            } else {
                accumulator.last().add(index)
            }
            accumulator
        }
    }
}
