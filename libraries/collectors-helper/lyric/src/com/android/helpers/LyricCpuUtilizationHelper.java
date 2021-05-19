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

package com.android.helpers;

import android.util.Log;
import androidx.annotation.VisibleForTesting;
import androidx.test.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a collector helper that collects the dumpsys output for specified services and puts them
 * into files.
 */
public class LyricCpuUtilizationHelper implements ICollectorHelper<Double> {

    private static final String TAG = LyricCpuUtilizationHelper.class.getSimpleName();

    private static final String DUMPSYS_CMD = "dumpsys media.camera";

    private static final Pattern TIME_REGEX_PATTERN =
            Pattern.compile("((?:-|)\\d+\\.?\\d*)([a-zA-Z]*)");

    private static final String TIME_REGEX = "((?:-|)\\d+\\.?\\d*[a-zA-Z]*)";

    private static final Pattern CPU_USAGE_PATTERN =
            Pattern.compile(
                    String.format(
                            "CPU Usage during ProcessInput for \\[(?:>|\\s)] p\\d+ (.*) after"
                                + " (\\d+) invocations - User: %s \\(Max: %s Min:%s\\) System: %s"
                                + " \\(Max: %s Min:%s\\) Wall: %s \\(Max: %s Min:%s\\)",
                            TIME_REGEX,
                            TIME_REGEX,
                            TIME_REGEX,
                            TIME_REGEX,
                            TIME_REGEX,
                            TIME_REGEX,
                            TIME_REGEX,
                            TIME_REGEX,
                            TIME_REGEX));

    private static final String METRIC_KEY = "cpu_util_%s_%s";

    private UiDevice mUiDevice;

    @Override
    public boolean startCollecting() {
        mUiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        return true;
    }

    @Override
    public Map<String, Double> getMetrics() {
        Map<String, Double> metrics = new HashMap<>();
        try {
            String res = mUiDevice.executeShellCommand(DUMPSYS_CMD);
            BufferedReader bufReader = new BufferedReader(new StringReader(res));
            String line = bufReader.readLine();
            while (line != null) {
                metrics.putAll(processLine(line));
                line = bufReader.readLine();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to collect Lyric CPU metrics.");
        }
        return metrics;
    }

    @Override
    public boolean stopCollecting() {
        return true;
    }

    @VisibleForTesting
    static Map<String, Double> processLine(String line) {
        Matcher matcher = CPU_USAGE_PATTERN.matcher(line);
        Map<String, Double> metrics = new HashMap<>();
        if (!matcher.find()) {
            return metrics;
        }
        String node = matcher.group(1).replace(":", "-");
        metrics.put(
                String.format(METRIC_KEY, node, "number_of_invocations"),
                Double.parseDouble(matcher.group(2)));
        metrics.put(String.format(METRIC_KEY, node, "user_time"), parseTime(matcher.group(3)));
        metrics.put(String.format(METRIC_KEY, node, "user_time_max"), parseTime(matcher.group(4)));
        metrics.put(String.format(METRIC_KEY, node, "user_time_min"), parseTime(matcher.group(5)));
        metrics.put(String.format(METRIC_KEY, node, "system_time"), parseTime(matcher.group(6)));
        metrics.put(
                String.format(METRIC_KEY, node, "system_time_max"), parseTime(matcher.group(7)));
        metrics.put(
                String.format(METRIC_KEY, node, "system_time_min"), parseTime(matcher.group(8)));
        metrics.put(String.format(METRIC_KEY, node, "wall_time"), parseTime(matcher.group(9)));
        metrics.put(String.format(METRIC_KEY, node, "wall_time_max"), parseTime(matcher.group(10)));
        metrics.put(String.format(METRIC_KEY, node, "wall_time_min"), parseTime(matcher.group(11)));

        return metrics;
    }

    /** Takes a time string and returns the value in milliseconds. */
    private static Double parseTime(String timeString) {
        Matcher matcher = TIME_REGEX_PATTERN.matcher(timeString);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Time string does not match the expected format.");
        }
        double value = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2);
        switch (unit) {
            case "us":
                value /= 1000;
                break;
            case "s":
                value *= 1000;
                break;
            default:
                break;
        }
        return value;
    }
}
