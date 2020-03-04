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

import com.android.server.wm.flicker.Assertions.NamedAssertion;
import com.android.server.wm.flicker.Assertions.CompoundAssertion;
import com.android.server.wm.flicker.Assertions.Result;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Captures some of the common logic in {@link LayersTraceSubject} and {@link WmTraceSubject} used
 * to filter trace entries and combine multiple assertions.
 *
 * @param <T> trace entry type
 */
public class AssertionsChecker<T extends ITraceEntry> {
    private boolean mFilterEntriesByRange = false;
    private long mFilterStartTime = 0;
    private long mFilterEndTime = 0;
    private boolean mSkipUntilFirstAssertion = false;
    private AssertionOption mOption = AssertionOption.NONE;
    private List<CompoundAssertion<T>> mAssertions = new LinkedList<>();

    public void add(Assertions.TraceAssertion<T> assertion, String name) {
        mAssertions.add(new CompoundAssertion<>(assertion, name));
    }

    public void append(Assertions.TraceAssertion<T> assertion, String name) {
        CompoundAssertion<T> lastAssertion = mAssertions.get(mAssertions.size() - 1);
        lastAssertion.add(assertion, name);
    }

    /**
     * Ignores the first entries in the trace, until the first assertion passes. If it reaches the
     * end of the trace without passing any assertion, return a failure with the name/reason from
     * the first assertion
     *
     * @return
     */
    public void skipUntilFirstAssertion() {
        mSkipUntilFirstAssertion = true;
    }

    public void filterByRange(long startTime, long endTime) {
        mFilterEntriesByRange = true;
        mFilterStartTime = startTime;
        mFilterEndTime = endTime;
    }

    private void setOption(AssertionOption option) {
        if (mOption != AssertionOption.NONE && option != mOption) {
            throw new IllegalArgumentException(
                    "Cannot use " + mOption + " option with " + option + " option.");
        }
        mOption = option;
    }

    public void checkFirstEntry() {
        setOption(AssertionOption.CHECK_FIRST_ENTRY);
    }

    public void checkLastEntry() {
        setOption(AssertionOption.CHECK_LAST_ENTRY);
    }

    public void checkChangingAssertions() {
        setOption(AssertionOption.CHECK_CHANGING_ASSERTIONS);
    }

    /**
     * Filters trace entries then runs assertions returning a list of failures.
     *
     * @param entries list of entries to perform assertions on
     * @return list of failed assertion results
     */
    public List<Result> test(List<T> entries) {
        List<T> filteredEntries;

        if (mFilterEntriesByRange) {
            filteredEntries =
                    entries.stream()
                            .filter(
                                    e ->
                                            ((e.getTimestamp() >= mFilterStartTime)
                                                    && (e.getTimestamp() <= mFilterEndTime)))
                            .collect(Collectors.toList());
        } else {
            filteredEntries = entries;
        }

        switch (mOption) {
            case CHECK_CHANGING_ASSERTIONS:
                return assertChanges(filteredEntries);
            case CHECK_FIRST_ENTRY:
                return assertEntry(filteredEntries.get(0));
            case CHECK_LAST_ENTRY:
                return assertEntry(filteredEntries.get(filteredEntries.size() - 1));
        }
        return assertAll(filteredEntries);
    }

    /**
     * Steps through each trace entry checking if provided assertions are true in the order they are
     * added. Each assertion must be true for at least a single trace entry.
     *
     * <p>This can be used to check for asserting a change in property over a trace. Such as
     * visibility for a window changes from true to false or top-most window changes from A to B and
     * back to A again.
     *
     * <p>It is also possible to ignore failures on initial elements, until the first assertion
     * passes, this allows the trace to be recorded for longer periods, and the checks to happen
     * only after some time.
     */
    private List<Result> assertChanges(List<T> entries) {
        List<Result> failures = new ArrayList<>();
        int entryIndex = 0;
        int assertionIndex = 0;
        int lastPassedAssertionIndex = -1;

        if (mAssertions.size() == 0) {
            return failures;
        }

        while (assertionIndex < mAssertions.size() && entryIndex < entries.size()) {
            NamedAssertion<T> currentAssertion = mAssertions.get(assertionIndex);
            Result result = currentAssertion.apply(entries.get(entryIndex));
            boolean ignoreFailure = mSkipUntilFirstAssertion && lastPassedAssertionIndex == -1;

            if (result.passed()) {
                lastPassedAssertionIndex = assertionIndex;
                entryIndex++;
                continue;
            }

            if (ignoreFailure) {
                entryIndex++;
                continue;
            }

            if (lastPassedAssertionIndex != assertionIndex) {
                failures.add(result);
                break;
            }
            assertionIndex++;

            if (assertionIndex == mAssertions.size()) {
                failures.add(result);
                break;
            }
        }

        if (lastPassedAssertionIndex == -1 && mAssertions.size() > 0 && failures.isEmpty()) {
            failures.add(new Result(false /* success */, mAssertions.get(0).getName()));
        }

        if (failures.isEmpty()) {
            if (assertionIndex != mAssertions.size() - 1) {
                String reason =
                        "\nAssertion "
                                + mAssertions.get(assertionIndex).getName()
                                + " never became false";
                reason +=
                        "\nPassed assertions: "
                                + mAssertions
                                        .stream()
                                        .limit(assertionIndex)
                                        .map(NamedAssertion::getName)
                                        .collect(Collectors.joining(","));
                reason +=
                        "\nUntested assertions: "
                                + mAssertions
                                        .stream()
                                        .skip(assertionIndex + 1)
                                        .map(NamedAssertion::getName)
                                        .collect(Collectors.joining(","));

                Result result =
                        new Result(
                                false /* success */,
                                0 /* timestamp */,
                                "assertChanges",
                                "Not all assertions passed." + reason);
                failures.add(result);
            }
        }
        return failures;
    }

    private List<Result> assertEntry(T entry) {
        List<Result> failures = new ArrayList<>();
        for (NamedAssertion<T> assertion : mAssertions) {
            Result result = assertion.apply(entry);
            if (result.failed()) {
                failures.add(result);
            }
        }
        return failures;
    }

    private List<Result> assertAll(List<T> entries) {
        return mAssertions
                .stream()
                .flatMap(assertion -> entries.stream().map(assertion).filter(Result::failed))
                .collect(Collectors.toList());
    }

    private enum AssertionOption {
        NONE,
        CHECK_CHANGING_ASSERTIONS,
        CHECK_FIRST_ENTRY,
        CHECK_LAST_ENTRY,
    }
}
