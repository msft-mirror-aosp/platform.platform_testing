/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.server.wm.flicker;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.runner.AndroidJUnit4;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains {@link AssertionsChecker} tests. To run this test: {@code atest
 * FlickerLibTest:AssertionsCheckerTest}
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AssertionsCheckerTest {

    /**
     * Returns a list of SimpleEntry objects with {@code data} and incremental timestamps starting
     * at 0.
     */
    private static List<SimpleEntry> getTestEntries(int... data) {
        List<SimpleEntry> entries = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            entries.add(new SimpleEntry(i, data[i]));
        }
        return entries;
    }

    @Test
    public void canCheckAllEntries() {
        AssertionsChecker<SimpleEntry> checker = new AssertionsChecker<>();
        checker.add(it -> it.isData42(), "isData42");

        List<AssertionResult> failures = checker.test(getTestEntries(1, 1, 1, 1, 1));

        assertThat(failures).hasSize(5);
    }

    @Test
    public void canCheckFirstEntry() {
        AssertionsChecker<SimpleEntry> checker = new AssertionsChecker<>();
        checker.checkFirstEntry();
        checker.add(it -> it.isData42(), "isData42");

        List<AssertionResult> failures = checker.test(getTestEntries(1, 1, 1, 1, 1));

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getTimestamp()).isEqualTo(0);
    }

    @Test
    public void canCheckLastEntry() {
        AssertionsChecker<SimpleEntry> checker = new AssertionsChecker<>();
        checker.checkLastEntry();
        checker.add(it -> it.isData42(), "isData42");

        List<AssertionResult> failures = checker.test(getTestEntries(1, 1, 1, 1, 1));

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getTimestamp()).isEqualTo(4);
    }

    @Test
    public void canCheckRangeOfEntries() {
        AssertionsChecker<SimpleEntry> checker = new AssertionsChecker<>();
        checker.filterByRange(1, 2);
        checker.add(it -> it.isData42(), "isData42");

        List<AssertionResult> failures = checker.test(getTestEntries(1, 42, 42, 1, 1));

        assertThat(failures).hasSize(0);
    }

    @Test
    public void emptyRangePasses() {
        AssertionsChecker<SimpleEntry> checker = new AssertionsChecker<>();
        checker.filterByRange(9, 10);
        checker.add(it -> it.isData42(), "isData42");

        List<AssertionResult> failures = checker.test(getTestEntries(1, 1, 1, 1, 1));

        assertThat(failures).isEmpty();
    }

    @Test
    public void canCheckChangingAssertions() {
        AssertionsChecker<SimpleEntry> checker = new AssertionsChecker<>();
        checker.add(it -> it.isData42(), "isData42");
        checker.add(it -> it.isData0(), "isData0");
        checker.checkChangingAssertions();

        List<AssertionResult> failures = checker.test(getTestEntries(42, 0, 0, 0, 0));

        assertThat(failures).isEmpty();
    }

    @Test
    public void canCheckChangingAssertions_withNoAssertions() {
        AssertionsChecker<SimpleEntry> checker = new AssertionsChecker<>();
        checker.checkChangingAssertions();

        List<AssertionResult> failures = checker.test(getTestEntries(42, 0, 0, 0, 0));

        assertThat(failures).isEmpty();
    }

    @Test
    public void canCheckChangingAssertions_withSingleAssertion() {
        AssertionsChecker<SimpleEntry> checker = new AssertionsChecker<>();
        checker.add(it -> it.isData42(), "isData42");
        checker.checkChangingAssertions();

        List<AssertionResult> failures = checker.test(getTestEntries(42, 42, 42, 42, 42));

        assertThat(failures).isEmpty();
    }

    @Test
    public void canFailCheckChangingAssertions_ifStartingAssertionFails() {
        AssertionsChecker<SimpleEntry> checker = new AssertionsChecker<>();
        checker.add(it -> it.isData42(), "isData42");
        checker.add(it -> it.isData0(), "isData0");
        checker.checkChangingAssertions();

        List<AssertionResult> failures = checker.test(getTestEntries(0, 0, 0, 0, 0));

        assertThat(failures).hasSize(1);
    }

    @Test
    public void canFailCheckChangingAssertions_ifStartingAssertionAlwaysPasses() {
        AssertionsChecker<SimpleEntry> checker = new AssertionsChecker<>();
        checker.add(it -> it.isData42(), "isData42");
        checker.add(it -> it.isData0(), "isData0");
        checker.checkChangingAssertions();

        List<AssertionResult> failures = checker.test(getTestEntries(0, 0, 0, 0, 0));

        assertThat(failures).hasSize(1);
    }

    @Test
    public void canFailCheckChangingAssertions_ifUsingCompoundAssertion() {
        AssertionsChecker<SimpleEntry> checker = new AssertionsChecker<>();
        checker.add(it -> it.isData42(), "isData42");
        checker.append(it -> it.isData0(), "isData0");
        checker.checkChangingAssertions();

        List<AssertionResult> failures = checker.test(getTestEntries(0, 0, 0, 0, 0));

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getAssertionName()).contains("isData42");
        assertThat(failures.get(0).getAssertionName()).contains("isData0");
        assertThat(failures.get(0).getReason()).contains("!is42");
        assertThat(failures.get(0).getReason()).doesNotContain("!is0");
    }

    static class SimpleEntry implements ITraceEntry {
        long mTimestamp;
        int mData;

        SimpleEntry(long timestamp, int data) {
            this.mTimestamp = timestamp;
            this.mData = data;
        }

        @Override
        public long getTimestamp() {
            return mTimestamp;
        }

        AssertionResult isData42() {
            return new AssertionResult("!is42", "is42", this.mTimestamp, this.mData == 42);
        }

        AssertionResult isData0() {
            return new AssertionResult("!is0", "is0", this.mTimestamp, this.mData == 0);
        }
    }
}
