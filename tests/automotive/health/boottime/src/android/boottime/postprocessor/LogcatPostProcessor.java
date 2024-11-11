/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.boottime.postprocessor;

import com.android.loganalysis.item.GenericTimingItem;
import com.android.loganalysis.item.SystemServicesTimingItem;
import com.android.loganalysis.parser.TimingsLogParser;
import com.android.tradefed.config.Option;
import com.android.tradefed.config.OptionClass;
import com.android.tradefed.log.LogUtil;
import com.android.tradefed.metrics.proto.MetricMeasurement.Metric;
import com.android.tradefed.result.LogFile;
import com.android.tradefed.result.TestDescription;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** A Post Processor that processes text file containing logcat logs into key-value pairs */
@OptionClass(alias = "logcat-post-processor")
public class LogcatPostProcessor extends BaseBootTimeTestLogPostProcessor {
    private static final String BOOT_COMPLETE_KEY = "boot_complete_key";
    /** Matches the line indicating kernel start. It is starting point of the whole boot process */
    private static final Pattern KERNEL_START_PATTERN = Pattern.compile("Linux version");

    // 03-10 21:43:40.328 1005 1005 D SystemServerTiming:StartWifi took to
    // complete: 3474ms
    // 03-10 21:43:40.328 1005 1005 D component:subcomponent took to complete:
    // 3474ms
    @Option(
            name = "components",
            shortName = 'c',
            description =
                    "Comma separated list of component names to parse the granular boot info"
                            + " printed in the logcat.")
    private String mComponentNames = null;

    @Option(
            name = "full-components",
            shortName = 'f',
            description =
                    "Comma separated list of component_subcomponent names to parse the granular"
                            + " boot info printed in the logcat.")
    private String mFullCompNames = null;

    @Option(
            name = "boot-time-pattern",
            description =
                    "Named boot time regex patterns which are used to capture signals in logcat and"
                            + " calculate duration between device boot to the signal being logged."
                            + " Key: name of custom boot metric, Value: regex to match single"
                            + " logcat line. Maybe repeated.")
    private Map<String, String> mBootTimePatterns = new HashMap<>();

    @Option(
            name = "starting-phase-signal",
            description =
                    "Keywords to match the starting phase of the boot.")
    private String startingPhase = "Starting phase 1000";

    private List<Double> mDmesgBootCompleteTimes;
    private Pattern mBootCompletePattern;

    @Override
    public void setUp() {
        mBootCompletePattern = Pattern.compile(startingPhase);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Metric.Builder> processTestMetricsAndLogs(
            TestDescription testDescription,
            HashMap<String, Metric> testMetrics,
            Map<String, LogFile> testLogs) {
        LogUtil.CLog.v("Processing test logs for %s", testDescription.getTestName());
        if (!testMetrics.containsKey(DMESG_BOOT_COMPLETE_TIME)) {
            LogUtil.CLog.w(
                    "Dmesg boot complete metric not found. Custom boot metrics will not be "
                            + "calculated");
        } else {
            String dmesgBootCompleteStringValue =
                    testMetrics.get(DMESG_BOOT_COMPLETE_TIME).getMeasurements().getSingleString();
            mDmesgBootCompleteTimes =
                    Arrays.asList(dmesgBootCompleteStringValue.split(",")).stream()
                            .map(Double::parseDouble)
                            .toList();
        }
        return processLogcatLogs(filterFiles(testLogs));
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Metric.Builder> processRunMetricsAndLogs(
            HashMap<String, Metric> rawMetrics, Map<String, LogFile> runLogs) {
        // noop
        return new HashMap<>();
    }

    /**
     * Process logcat testLog files reported by the test
     *
     * @param files List of logcat files
     * @return Map with metric keys and stringified double values joined by comma
     */
    private Map<String, Metric.Builder> processLogcatLogs(List<File> files) {
        int iteration = 0;
        ArrayListMultimap<String, Double> metrics = ArrayListMultimap.create();
        for (File file : files) {
            LogUtil.CLog.d("Parsing logcat file %s", file.getPath());
            try {
                Map<String, Double> granularBootMetricsMap = analyzeGranularBootInfo(file);
                metrics.putAll(Multimaps.forMap(granularBootMetricsMap));
                if (!mBootTimePatterns.isEmpty()) {
                    Map<String, Double> customBootMetricMap =
                            analyzeCustomBootInfo(file, mDmesgBootCompleteTimes.get(iteration));
                    metrics.putAll(Multimaps.forMap(customBootMetricMap));
                } else {
                    LogUtil.CLog.i(
                            "No boot-time-pattern values provided. Skipping analyzeCustomBootInfo");
                }
            } catch (IndexOutOfBoundsException e) {
                LogUtil.CLog.e("Missing dmesg boot complete signals for iteration %d", iteration);
                LogUtil.CLog.e(e);
            }
            iteration++;
        }

        return buildTfMetrics(metrics.asMap());
    }

    /**
     * Parse the logcat file for granular boot info (eg different system services start time) based
     * on the component name or full component name (i.e component_subcompname)
     */
    private Map<String, Double> analyzeGranularBootInfo(File file) {
        Map<String, Double> metrics = new HashMap<>();
        String[] compStr = new String[0];
        String[] fullCompStr = new String[0];
        boolean isFilterSet = mComponentNames != null || mFullCompNames != null;

        if (mComponentNames != null) {
            compStr = mComponentNames.split(",");
        }
        if (mFullCompNames != null) {
            fullCompStr = mFullCompNames.split(",");
        }

        Set<String> compSet = new HashSet<>(Arrays.asList(compStr));
        Set<String> fullCompSet = new HashSet<>(Arrays.asList(fullCompStr));

        try (InputStreamReader ir = new InputStreamReader(new FileInputStream(file));
                BufferedReader input = new BufferedReader(ir)) {
            TimingsLogParser parser = new TimingsLogParser();
            List<SystemServicesTimingItem> items = parser.parseSystemServicesTimingItems(input);
            for (SystemServicesTimingItem item : items) {
                String componentName = item.getComponent();
                String fullCompName =
                        String.format("%s_%s", item.getComponent(), item.getSubcomponent());
                // If filter not set then capture timing info for all the components otherwise
                // only for the given component names and full component names.
                if (!isFilterSet
                        || compSet.contains(componentName)
                        || fullCompSet.contains(fullCompName)) {
                    Double time =
                            item.getDuration() != null ? item.getDuration() : item.getStartTime();
                    if (time != null) {
                        metrics.put(fullCompName, time);
                    }
                }
            }
        } catch (IOException ioe) {
            LogUtil.CLog.e("Problem parsing the granular boot information");
            LogUtil.CLog.e(ioe);
        }
        return metrics;
    }

    /** Parse the logcat file to get boot time metrics given patterns defined by tester. */
    private Map<String, Double> analyzeCustomBootInfo(File file, double dmesgBootCompleteTime) {
        Map<String, Double> metrics = new HashMap<>();
        try (InputStreamReader ir = new InputStreamReader(new FileInputStream(file));
                BufferedReader input = new BufferedReader(ir)) {
            List<GenericTimingItem> items =
                    createCustomTimingsParser().parseGenericTimingItems(input);
            Map<String, GenericTimingItem> itemsMap =
                    items.stream()
                            .collect(Collectors.toMap(GenericTimingItem::getName, item -> item));
            if (!itemsMap.containsKey(BOOT_COMPLETE_KEY)) {
                LogUtil.CLog.e("Missing boot complete signals from logcat");
                return metrics;
            }
            for (Map.Entry<String, GenericTimingItem> metric : itemsMap.entrySet()) {
                GenericTimingItem itemsForMetric = metric.getValue();
                if (itemsForMetric.getName().isEmpty()) {
                    LogUtil.CLog.e("Missing value for metric %s", metric.getKey());
                    continue;
                }
                double duration = dmesgBootCompleteTime + itemsForMetric.getDuration();
                metrics.put(metric.getKey(), duration);
                LogUtil.CLog.d(
                        "Added boot metric: %s with duration value: %f", metric.getKey(), duration);
            }
        } catch (IOException ioe) {
            LogUtil.CLog.e("Problem parsing the custom boot information");
            LogUtil.CLog.e(ioe);
        }
        return metrics;
    }

    private TimingsLogParser createCustomTimingsParser() {
        TimingsLogParser parser = new TimingsLogParser();
        parser.addDurationPatternPair(BOOT_COMPLETE_KEY, KERNEL_START_PATTERN, mBootCompletePattern);
        for (Map.Entry<String, String> pattern : mBootTimePatterns.entrySet()) {
            LogUtil.CLog.d(
                    "Adding boot metric with name: %s, pattern: %s",
                    pattern.getKey(), pattern.getValue());
            parser.addDurationPatternPair(
                    pattern.getKey(), mBootCompletePattern, Pattern.compile(pattern.getValue()));
        }
        return parser;
    }
}
