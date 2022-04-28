/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.device.collectors;

import android.device.collectors.annotations.OptionClass;
import android.os.Bundle;
import androidx.annotation.VisibleForTesting;

import com.android.helpers.HeapDumpHelper;

import org.junit.runner.Description;

import java.util.Arrays;
import java.util.function.Function;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link BaseCollectionListener} that captures and logs the heapdump.
 */
@OptionClass(alias = "heapdump-listener")
public class HeapDumpListener extends BaseCollectionListener<String> {

    private static final String SPACES_PATTERN = "\\s+";
    private static final String REPLACEMENT_CHAR = "#";
    private static final String FILE_ID_FORMAT = "%s_%d";
    private static final String FILE_NAME_PREFIX_FORMAT = "%s_%s";
    @VisibleForTesting static final String ITERATION_SEPARATOR = ",";
    @VisibleForTesting static final String ENABLE_ITERATION_IDS = "enable-iteration-ids";
    @VisibleForTesting static final String ITERATION_ALL_ENABLE = "iteration-all-enable";
    Map<String, Integer> mTestIterationCount = new HashMap<String, Integer>();
    Set<Integer> mValidIterationIds;
    boolean mIsDisabled = false;
    boolean mIsEnabledForAll = false;

    public HeapDumpListener() {
        createHelperInstance(new HeapDumpHelper());
    }

    @VisibleForTesting
    public HeapDumpListener(Bundle args, HeapDumpHelper helper) {
        super(args, helper);
    }

    /** Process the test arguments */
    @Override
    public void setupAdditionalArgs() {
        Bundle args = getArgsBundle();

        mIsEnabledForAll =
                Boolean.parseBoolean(args.getString(ITERATION_ALL_ENABLE, String.valueOf(false)));

        if (!mIsEnabledForAll) {
            String iterations = args.getString(ENABLE_ITERATION_IDS);
            if (iterations == null || iterations.isEmpty()) {
                mIsDisabled = true;
                return;
            }
            String[] iterationArray = iterations.split(ITERATION_SEPARATOR);
            Set<String> validIterationIdsStr = new HashSet<String>(Arrays.asList(iterationArray));
            mValidIterationIds = validIterationIdsStr.stream().map(s -> Integer.parseInt(s))
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public void testStart(Function<String, Boolean> filter, Description description) {
        if (mIsDisabled) {
            mHelper.startCollecting(false, null);
            return;
        }

        updateIterationCount(description);

        if (mIsEnabledForAll) {
            mHelper.startCollecting(true, getHeapDumpFileId(description));
        } else {
            if (mValidIterationIds.contains(mTestIterationCount.get(getTestFileName(description)))) {
                mHelper.startCollecting(true, getHeapDumpFileId(description));
            } else {
                mHelper.startCollecting(false, null);
            }
        }
    }

    /**
     * Update the map that tracks the number of times each test method is called.
     */
    private void updateIterationCount(Description description) {
        String testId = getTestFileName(description);
        if (mTestIterationCount.containsKey(testId)) {
            mTestIterationCount.put(testId,
                    mTestIterationCount.get(getTestFileName(description)) + 1);
        } else {
            mTestIterationCount.put(testId, 1);
        }
    }

    /**
     * Returns the unique identifier using the test name and the current iteration number
     * used to create the heap dump file.
     */
    private String getHeapDumpFileId(Description description) {
        return String.format(FILE_ID_FORMAT, getTestFileName(description),
                mTestIterationCount.get(getTestFileName(description)));
    }

    /**
     * Returns the packagename.classname_methodname which has no spaces and used to create file
     * names.
     */
    public static String getTestFileName(Description description) {
        return String.format(FILE_NAME_PREFIX_FORMAT,
                description.getClassName().replaceAll(SPACES_PATTERN, REPLACEMENT_CHAR).trim(),
                description.getMethodName().replaceAll(SPACES_PATTERN, REPLACEMENT_CHAR).trim());
    }
}
