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

import com.android.server.wm.flicker.assertions.AssertionsChecker
import com.android.server.wm.flicker.common.AssertionResult
import com.android.server.wm.flicker.common.traces.ITraceEntry
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [AssertionsChecker] tests. To run this test: `atest
 * FlickerLibTest:AssertionsCheckerTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AssertionsCheckerTest {
    @Test
    fun canCheckAllEntries() {
        val checker = AssertionsChecker<SimpleEntry>()
        checker.add({ it.isData42 }, "isData42")
        val failures = checker.test(getTestEntries(1, 1, 1, 1, 1))
        Truth.assertThat(failures).hasSize(5)
    }

    @Test
    fun canCheckFirstEntry() {
        val checker = AssertionsChecker<SimpleEntry>()
        checker.checkFirstEntry()
        checker.add({ it.isData42 }, "isData42")
        val failures = checker.test(getTestEntries(1, 1, 1, 1, 1))
        Truth.assertThat(failures).hasSize(1)
        Truth.assertThat(failures.first().timestamp).isEqualTo(0)
    }

    @Test
    fun canCheckLastEntry() {
        val checker = AssertionsChecker<SimpleEntry>()
        checker.checkLastEntry()
        checker.add({ it.isData42 }, "isData42")
        val failures = checker.test(getTestEntries(1, 1, 1, 1, 1))
        Truth.assertThat(failures).hasSize(1)
        Truth.assertThat(failures.first().timestamp).isEqualTo(4)
    }

    @Test
    fun canCheckRangeOfEntries() {
        val checker = AssertionsChecker<SimpleEntry>()
        checker.filterByRange(1, 2)
        checker.add({ it.isData42 }, "isData42")
        val failures = checker.test(getTestEntries(1, 42, 42, 1, 1))
        Truth.assertThat(failures).hasSize(0)
    }

    @Test
    fun emptyRangePasses() {
        val checker = AssertionsChecker<SimpleEntry>()
        checker.filterByRange(9, 10)
        checker.add({ it.isData42 }, "isData42")
        val failures = checker.test(getTestEntries(1, 1, 1, 1, 1))
        Truth.assertThat(failures).isEmpty()
    }

    @Test
    fun canCheckChangingAssertions() {
        val checker = AssertionsChecker<SimpleEntry>()
        checker.add({ it.isData42 }, "isData42")
        checker.add({ it.isData0 }, "isData0")
        checker.checkChangingAssertions()
        val failures = checker.test(getTestEntries(42, 0, 0, 0, 0))
        Truth.assertThat(failures).isEmpty()
    }

    @Test
    fun canCheckChangingAssertions_withNoAssertions() {
        val checker = AssertionsChecker<SimpleEntry>()
        checker.checkChangingAssertions()
        val failures = checker.test(getTestEntries(42, 0, 0, 0, 0))
        Truth.assertThat(failures).isEmpty()
    }

    @Test
    fun canCheckChangingAssertions_withSingleAssertion() {
        val checker = AssertionsChecker<SimpleEntry>()
        checker.add({ it.isData42 }, "isData42")
        checker.checkChangingAssertions()
        val failures = checker.test(getTestEntries(42, 42, 42, 42, 42))
        Truth.assertThat(failures).isEmpty()
    }

    @Test
    fun canFailCheckChangingAssertions_ifStartingAssertionFails() {
        val checker = AssertionsChecker<SimpleEntry>()
        checker.add({ it.isData42 }, "isData42")
        checker.add({ it.isData0 }, "isData0")
        checker.checkChangingAssertions()
        val failures = checker.test(getTestEntries(0, 0, 0, 0, 0))
        Truth.assertThat(failures).hasSize(1)
    }

    @Test
    fun canFailCheckChangingAssertions_ifStartingAssertionAlwaysPasses() {
        val checker = AssertionsChecker<SimpleEntry>()
        checker.add({ it.isData42 }, "isData42")
        checker.add({ it.isData0 }, "isData0")
        checker.checkChangingAssertions()
        val failures = checker.test(getTestEntries(0, 0, 0, 0, 0))
        Truth.assertThat(failures).hasSize(1)
    }

    @Test
    fun canFailCheckChangingAssertions_ifUsingCompoundAssertion() {
        val checker = AssertionsChecker<SimpleEntry>()
        checker.add({ it.isData42 }, "isData42")
        checker.append({ it.isData0 }, "isData0")
        checker.checkChangingAssertions()
        val failures = checker.test(getTestEntries(0, 0, 0, 0, 0))
        Truth.assertThat(failures).hasSize(1)
        Truth.assertThat(failures.first().assertionName).contains("isData42")
        Truth.assertThat(failures.first().assertionName).contains("isData0")
        Truth.assertThat(failures.first().reason).contains("!is42")
        Truth.assertThat(failures.first().reason).doesNotContain("!is0")
    }

    data class SimpleEntry(override var timestamp: Long, private var mData: Int) : ITraceEntry {
        val isData42: AssertionResult
            get() = AssertionResult("!is42", "is42", timestamp, mData == 42)

        val isData0: AssertionResult
            get() = AssertionResult("!is0", "is0", timestamp, mData == 0)
    }

    companion object {
        /**
         * Returns a list of SimpleEntry objects with `data` and incremental timestamps starting
         * at 0.
         */
        private fun getTestEntries(vararg data: Int): List<SimpleEntry> =
                data.indices.map { SimpleEntry(it.toLong(), data[it])
        }
    }
}
