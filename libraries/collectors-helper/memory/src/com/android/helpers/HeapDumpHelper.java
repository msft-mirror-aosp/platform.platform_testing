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

import android.os.Debug;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;

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
    private static final String HEAPDUMP_OUTPUT_FILE_METRIC_NAME = "heapdump_file";

    boolean mIsEnabled = false;
    String mId = null;
    File mResultsFile = null;

    @Override
    public boolean startCollecting() {
        return true;
    }

    @Override
    public boolean startCollecting(boolean isEnabled, String id) {
        mIsEnabled = isEnabled;
        mId = id;
        if (!collectHeapDump()) {
            return false;
        }
        return createHeapDumpEmptyFile(id);
    }

    @Override
    public Map<String, String> getMetrics() {

        HashMap<String, String> heapDumpFinalMap = new HashMap<>();
        if (mIsEnabled) {
            Log.i(TAG, "Metric collector enabled. Dumping the hprof.");
            if (mId != null && !mId.isEmpty()) {
                try {
                    Debug.dumpHprofData(mResultsFile.getAbsolutePath());
                    heapDumpFinalMap.put(HEAPDUMP_OUTPUT_FILE_METRIC_NAME,
                            mResultsFile.getAbsolutePath());
                } catch (Throwable e) {
                    Log.e(TAG, "dumpHprofData failed", e);
                }
            } else {
                Log.e(TAG, "Metric collector is enabled but the heap dump file id is not valid.");
            }
        } else {
            Log.i(TAG, "Metric collector is disabled.");
        }
        return heapDumpFinalMap;
    }

    /**
     * Create an empty file that will be used for dumping the heap profile.
     *
     * @param fileName name of the empty file.
     * @return true if the file creation is successful.
     */
    private boolean createHeapDumpEmptyFile(String fileName) {
        try {
            mResultsFile =
                    File.createTempFile(
                            fileName,
                            ".hprof",
                            InstrumentationRegistry.getInstrumentation()
                                    .getContext()
                                    .getExternalFilesDir(null));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Returns true if heap dump collection is enabled and heap dump file name is valid.
     */
    private boolean collectHeapDump() {
        if (!mIsEnabled || mId == null || mId.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean stopCollecting() {
        mIsEnabled = false;
        mId = null;
        return true;
    }

    public File getResultsFile() {
        return mResultsFile;
    }
}
