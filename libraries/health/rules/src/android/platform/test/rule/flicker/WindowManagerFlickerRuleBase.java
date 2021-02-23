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

package android.platform.test.rule.flicker;

import android.os.Environment;
import android.platform.test.rule.TestWatcher;
import android.util.Log;

import com.android.server.wm.flicker.monitor.WindowManagerTraceMonitor;
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace;
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser;

import com.google.common.io.Files;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Paths;
import java.io.File;
import org.junit.runner.Description;

/**
 * Base class that encapstulates the logic for collecting and parsing the window manager trace.
 */
public abstract class WindowManagerFlickerRuleBase extends TestWatcher {

    private static final String TAG = WindowManagerFlickerRuleBase.class.getSimpleName();

    private static final String WM_FLICKER_RULE_DISABLED = "wm-flicker-rule-disabled";
    private static final String WM_TRACE_DIRECTORY_ROOT = "wm-trace-directory-root";
    // Suffix is added by the trace monitor at the time of saving the file.
    private static final String WM_TRACE_FILE_SUFFIX = "wm_trace.pb";
    // To keep track of the method name and the current iteration count.
    private static Map<String, Integer> mMethodNameCount = new HashMap<>();

    private WindowManagerTraceMonitor mWmTraceMonitor;
    private String mTraceDirectoryRoot = null;
    private boolean mIsRuleDisabled= false;

    public WindowManagerFlickerRuleBase() {
    }

    @Override
    protected void starting(Description description) {
        if(isWmFlickerDisabled()) {
            Log.v(TAG, "WM Flicker rule is disabled.");
            return;
        }

        setupRootDirectory();

        // Verify if WM tracing is already started by another window manager based
        // rule. Otherwise proceed with starting the trace.
        if (!mWmTraceMonitor.isEnabled()) {
            mWmTraceMonitor.start();
            Log.v(TAG, "WM trace started successfully.");
        } else {
            Log.v(TAG, "WM trace already enabled.");
            return;
        }

        // Update the method name with current iteration count.
        if (mMethodNameCount.containsKey(description.toString())) {
            mMethodNameCount.put(description.toString(),
                    mMethodNameCount.get(description.toString()) + 1);
        } else {
            mMethodNameCount.put(description.toString(), 1);
        }

        // Cleanup the trace file from previous test runs.
        if (new File(getFinalTraceFilePath(description)).exists()) {
            new File(getFinalTraceFilePath(description)).delete();
            Log.v(TAG, "Removed the already existing wm trace file.");
        }
    }

    @Override
    protected void finished(Description description) {
        if (isWmFlickerDisabled()) {
            return;
        }
        // Verify if WM tracing is already stopped by another window manager based
        // rule. Otherwise proceed with stopping the trace.
        if (mWmTraceMonitor.isEnabled()) {
            mWmTraceMonitor.stop();
            Log.v(TAG, "WM trace stopped successfully.");
        } else {
            Log.v(TAG, "WM trace already stopped.");
        }

        // Verify if WM trace file already exist for the current test. It could have been created
        // by another Window manager based rule.
        if (!new File(getFinalTraceFilePath(description)).exists()) {
            // Appends the trace file suffix "_wm_trace.pb" and store it under the root directory.
            mWmTraceMonitor.save(getFileNamePrefix(description));
            Log.v(TAG, "WM trace successfully saved in the destination folder.");
        } else {
            Log.v(TAG, "WM trace already saved in the destination folder.");
        }
        processWindowManagerTrace(getFinalTraceFilePath(description));
    }

    private String getFinalTraceFilePath(Description description) {
        return String.format(
                "%s%s_%s", mTraceDirectoryRoot, getFileNamePrefix(description),
                WM_TRACE_FILE_SUFFIX);
    }

    /**
     * Returns false if wm-flicker-rule-disabled is unset or returns the true/false value
     * of wm-flicker-rule-disabled.
     */
    private Boolean isWmFlickerDisabled() {
        String value = getArguments().getString(WM_FLICKER_RULE_DISABLED);
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    /**
    * Setup the root directory to save the WM traces collected during the test.
    */
    private void setupRootDirectory() {
        mTraceDirectoryRoot = getArguments().getString(WM_TRACE_DIRECTORY_ROOT,
                Environment.getExternalStorageDirectory().getPath() + "/wmtrace/");
        if (!mTraceDirectoryRoot.endsWith("/")) {
            mTraceDirectoryRoot = new StringBuilder(mTraceDirectoryRoot).append("/").toString();
        }

        // Create root directory if it does not already exist.
        File rootDirectory = new File(mTraceDirectoryRoot);
        if (!rootDirectory.exists()) {
            if (rootDirectory.mkdirs()) {
                Log.v(TAG, "WM trace root directory created successfully.");
            } else {
                throw new RuntimeException(
                        "Unable to create the WM trace root directory." + mTraceDirectoryRoot);
            }
        } else {
            Log.v(TAG, "WM trace root directory already exists.");
        }
        mWmTraceMonitor = new WindowManagerTraceMonitor(Paths.get(mTraceDirectoryRoot));
    }

    /**
     * Construct file name using the class name, method name and the current iteration count
     * of the method.
     *
     * @param description
     * @return the file name used to save the WM trace proto file.
     */
    private String getFileNamePrefix(Description description) {
        return description.getClassName() + "_" + description.getMethodName() + "_"
                + mMethodNameCount.get(description.toString());
    }

    /**
     * Parse the window manager trace file and assert for flicker conditions.
     *
     * @param finalTraceFilePath
     */
    protected void processWindowManagerTrace(String finalTraceFilePath) {
        Log.v(TAG, "Processing window manager trace file.");
        try {
            byte[] wmTraceByteArray = Files.toByteArray(new File(finalTraceFilePath));
            if (wmTraceByteArray != null) {
                WindowManagerTrace wmTrace = WindowManagerTraceParser
                        .parseFromTrace(wmTraceByteArray);
                validateFlickerConditions(wmTrace);
            } else {
                throw new RuntimeException("Window manager trace contents are empty.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to read the proto file." + finalTraceFilePath);
        }
    }

    /**
     * Child class should implement this method to test for flicker conditions using the
     * WM trace file.
     *
     * @param wmTrace
     */
    protected abstract void validateFlickerConditions(WindowManagerTrace wmTrace);
}
