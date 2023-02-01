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

import android.app.UiAutomation;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.annotation.VisibleForTesting;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BugReportDurationHelper is used to collect the durations of a bug report's component sections
 * during a test.
 */
public class BugReportDurationHelper implements ICollectorHelper<Double> {

    private static final String TAG = BugReportDurationHelper.class.getSimpleName();

    private static final String LS_CMD = "ls %s";
    private static final String UNZIP_CMD = "unzip -p %s %s";
    private static final String DURATION_FILTER = "was the duration of \'";
    private static final String SHOWMAP_FILTER = "SHOW MAP";

    // This pattern will match a group of characters representing a number with a decimal point.
    private Pattern durationPattern = Pattern.compile("[0-9]+\\.[0-9]+");
    // This pattern will match a group of characters enclosed by \'.
    private Pattern keyPattern = Pattern.compile("'.+'");

    private String bugReportDir;

    private UiDevice device;

    public BugReportDurationHelper(String dir) {
        super();
        // The helper methods assume that the directory path is terminated by '/'.
        if (dir.charAt(dir.length() - 1) != '/') {
            dir += '/';
        }
        bugReportDir = dir;
    }

    @Override
    public boolean startCollecting() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Log.d(TAG, "Started collecting for BugReportDuration.");
        return true;
    }

    @Override
    public Map<String, Double> getMetrics() {
        Map<String, Double> metrics = new HashMap<>();
        String archive = getLatestBugReport();
        // No bug report was found, so there are no metrics to return.
        if (archive == null) {
            return metrics;
        }
        ArrayList<String> durationLines = extractAndFilterBugReport(archive);
        // No lines relevant to bug report durations were found, so there are no metrics to return.
        if (durationLines == null) {
            Log.w(TAG, "No lines relevant to bug report durations were found.");
            return metrics;
        }
        /*
         * Some examples of duration-relevant lines are:
         *     ------ 44.619s was the duration of 'dumpstate_board()' ------
         *     ------ 21.397s was the duration of 'DUMPSYS' ------
         */
        for (String line : durationLines) {
            String section = parseSection(line);
            double duration = parseDuration(line);
            // The line doesn't contain the expected \' characters or duration value.
            if (section == null || duration == -1) {
                Log.e(TAG, "Section name or duration could not be parsed from: " + line);
                continue;
            }
            String key = convertSectionToKey(section);
            // Some sections are collected multiple times (e.g. trusty version, system log).
            metrics.put(key, duration + metrics.getOrDefault(key, 0.0));
        }
        return metrics;
    }

    @Override
    public boolean stopCollecting() {
        return true;
    }

    // Returns the name of the most recent bug report .zip in bugReportDir.
    @VisibleForTesting
    public String getLatestBugReport() {
        try {
            // executeShellCommand will return files (separated by '\n') in a single String.
            String[] files =
                    device.executeShellCommand(String.format(LS_CMD, bugReportDir)).split("\n");
            HashSet<String> bugreports = new HashSet<>();
            for (String file : files) {
                if (file.contains("bugreport") && file.contains("zip")) {
                    bugreports.add(file);
                }
            }
            if (bugreports.size() == 0) {
                Log.e(TAG, "Failed to find a bug report in " + bugReportDir);
                return null;
            }
            // Returns the newest bug report. Bug report names contain a timestamp, so the
            // lexicographically-greatest name will correspond to the most recent bug report.
            return Collections.max(bugreports);
        } catch (IOException e) {
            Log.e(TAG, "Failed to find a bug report in  " + bugReportDir + ": " + e.getMessage());
            return null;
        }
    }

    // Extracts a bug report .txt to stdout and returns an ArrayList of lines containing section
    // names and durations.
    @VisibleForTesting
    public ArrayList<String> extractAndFilterBugReport(String archive) {
        String entry = archive.replace("zip", "txt");
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        String cmd = String.format(UNZIP_CMD, bugReportDir + archive, entry);
        try (InputStream is =
                        new ParcelFileDescriptor.AutoCloseInputStream(
                                automation.executeShellCommand(cmd));
                BufferedReader br = new BufferedReader(new InputStreamReader(is)); ) {
            ArrayList<String> durationLines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(DURATION_FILTER) && !line.contains(SHOWMAP_FILTER)) {
                    durationLines.add(line);
                }
            }
            return durationLines;
        } catch (IOException e) {
            Log.e(TAG, "Failed to extract and parse the raw bug report: " + e.getMessage());
            return null;
        }
    }

    // Parses a section duration from the input duration-relevant log line.
    @VisibleForTesting
    public double parseDuration(String line) {
        Matcher m = durationPattern.matcher(line);
        if (m.find()) {
            return Double.parseDouble(m.group());
        } else {
            return -1;
        }
    }

    // Parses a section name from the input duration-relevant log line.
    @VisibleForTesting
    public String parseSection(String line) {
        Matcher m = keyPattern.matcher(line);
        if (m.find()) {
            return m.group().replace("\'", "");
        } else {
            return null;
        }
    }

    // Converts a bug report section name to a key by replacing spaces with '-', lowercasing, and
    // prepending "bugreport-duration-".
    @VisibleForTesting
    public String convertSectionToKey(String section) {
        return "bugreport-duration-" + section.replace(" ", "-").toLowerCase();
    }
}
