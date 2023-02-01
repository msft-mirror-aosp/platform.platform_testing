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

package com.android.helpers;

import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HeapDumpHelper is a helper used to collect the heapdump and store the output in a
 * file created using the given id.
 */
public class HeapDumpHelper implements ICollectorHelper<String> {
    private static final String TAG = HeapDumpHelper.class.getSimpleName();
    private static final String HEAPDUMP_MANAGED_OUTPUT_FILE_METRIC_NAME = "managed_heapdump_file_";
    private static final String HEAPDUMP_NATIVE_OUTPUT_FILE_METRIC_NAME = "native_heapdump_file_";
    private static final String HEAPDUMP_CMD = "am dumpheap %s %s";
    private static final String NATIVE_HEAPDUMP_CMD = "am dumpheap -n %s %s";

    String mId = null;
    File mResultsFile = null;
    private String[] mProcessNames = null;
    private String mTestOutputDir = null;
    private boolean mNativeHeapDumpEnabled = false;
    private UiDevice mUiDevice;
    HashMap<String, String> mHeapDumpFinalMap;

    @Override
    public boolean startCollecting() {
        return true;
    }

    public void setUp(String testOutputDir, String... processNames) {
        mProcessNames = processNames;
        mTestOutputDir = testOutputDir;
        mUiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
      }

    @Override
    public boolean startCollecting(String id) {
        mId = id;
        mHeapDumpFinalMap = new HashMap<>();
        if (!collectHeapDump()) {
            return false;
        }
        return true;
    }

    @Override
    public Map<String, String> getMetrics() {

        Log.i(TAG, "Metric collector enabled. Dumping the hprof.");
        int processCount = 0;
        for (String processName : mProcessNames) {
            if (mId != null && !mId.isEmpty()) {
                try {
                    processCount++;
                    String finalHeapDumpPath =
                            String.format(
                                    "%s%s%s_%s.hprof",
                                    mTestOutputDir,
                                    HEAPDUMP_MANAGED_OUTPUT_FILE_METRIC_NAME,
                                    processName.replace("/", "#"),
                                    mId);
                    execHeapDump(processName, finalHeapDumpPath, false);
                    mHeapDumpFinalMap.put(
                            HEAPDUMP_MANAGED_OUTPUT_FILE_METRIC_NAME + processCount,
                            finalHeapDumpPath);
                    if (mNativeHeapDumpEnabled) {
                        String finalNativeHeapDumpPath =
                                String.format(
                                        "%s%s%s_%s.txt",
                                        mTestOutputDir,
                                        HEAPDUMP_NATIVE_OUTPUT_FILE_METRIC_NAME,
                                        processName.replace("/", "#"),
                                        mId);
                        execHeapDump(processName, finalNativeHeapDumpPath, true);
                        mHeapDumpFinalMap.put(
                                HEAPDUMP_NATIVE_OUTPUT_FILE_METRIC_NAME + processCount,
                                finalNativeHeapDumpPath);
                    }

                } catch (Throwable e) {
                    Log.e(TAG, "dumpheap command failed", e);
                }
            } else {
                Log.e(TAG, "Metric collector is enabled but the heap dump file id is not valid.");
            }
        }
        return mHeapDumpFinalMap;
    }

    private String execHeapDump(String processName, String filePath, boolean isNativeHeapDump)
            throws IOException {
        try {
            String heapdumpCommand = isNativeHeapDump ? NATIVE_HEAPDUMP_CMD : HEAPDUMP_CMD;
            Log.i(
                    TAG,
                    "Running heapdump command :"
                            + String.format(heapdumpCommand, processName, filePath));
            return mUiDevice.executeShellCommand(
                    String.format(heapdumpCommand, processName, filePath));
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Unable to execute heapdump command for %s ", processName), e);
        }
    }

    /**
     * Returns true if heap dump collection is enabled and heap dump file name is valid.
     */
    private boolean collectHeapDump() {
        if (mId == null || mId.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean stopCollecting() {
        mId = null;
        return true;
    }

    public void enableNativeHeapDump() {
        mNativeHeapDumpEnabled = true;
    }

    public Map<String,String> getFinalResultsMap() {
        return mHeapDumpFinalMap;
    }
}
