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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

import com.google.common.annotations.VisibleForTesting;

/**
 * TopicsLatencyHelper consist of helper methods to collect Topics API call latencies
 *
 * <p>TODO(b/234452723): Change metric collector to use either statsd or perfetto instead of logcat
 */
public class TopicsLatencyHelper implements ICollectorHelper<Long> {

    private static final String TAG = "TopicsLatencyHelper";

    private static final String TOPICS_HOT_START_LATENCY_METRIC = "TOPICS_HOT_START_LATENCY_METRIC";
    private static final String TOPICS_COLD_START_LATENCY_METRIC =
            "TOPICS_COLD_START_LATENCY_METRIC";

    private static final DateTimeFormatter LOG_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private static final Pattern sLatencyMetricPattern =
            Pattern.compile("TopicsCrystalBallTest: \\((.*): (\\d+)\\)");

    private Instant mInstant;
    private final Clock mClock;
    private final Supplier<MetricsEventStreamReader> mMetricsEventStreamReaderSupplier;

    public TopicsLatencyHelper() {
        mClock = Clock.systemUTC();
        mMetricsEventStreamReaderSupplier = () -> new MetricsEventStreamReader();
    }

    @VisibleForTesting
    public TopicsLatencyHelper(
            Supplier<MetricsEventStreamReader> metricsEventStreamSupplier, Clock clock) {
        this.mMetricsEventStreamReaderSupplier = metricsEventStreamSupplier;
        mClock = clock;
    }

    @Override
    public boolean startCollecting() {
        mInstant = mClock.instant();
        return true;
    }

    @Override
    public Map<String, Long> getMetrics() {
        try {
            return processOutput(
                    mMetricsEventStreamReaderSupplier.get().getMetricsEvents(mInstant));
        } catch (Exception e) {
            Log.e(TAG, "Failed to collect TopicsManager metrics.", e);
        }

        return Collections.emptyMap();
    }

    private Map<String, Long> processOutput(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String line = "";
        Map<String, Long> output = new HashMap<String, Long>();
        while ((line = bufferedReader.readLine()) != null) {
            Matcher matcher = sLatencyMetricPattern.matcher(line);
            while (matcher.find()) {
                /**
                 * The lines from Logcat will look like: 06-13 18:09:24.058 20765 20781 D
                 * TopicsCrystalBallTest: (TOPICS_HOT_START_LATENCY_METRIC: 14)
                 */
                String metric = matcher.group(1);
                long latency = Long.parseLong(matcher.group(2));
                if (TOPICS_HOT_START_LATENCY_METRIC.equals(metric)) {
                    output.put(TOPICS_HOT_START_LATENCY_METRIC, latency);
                } else if (TOPICS_COLD_START_LATENCY_METRIC.equals(metric)) {
                    output.put(TOPICS_COLD_START_LATENCY_METRIC, latency);
                }
            }
        }

        return output;
    }

    @Override
    public boolean stopCollecting() {
        return true;
    }

    @VisibleForTesting
    public static class MetricsEventStreamReader {
        /** Return TopicsCrystalBallTest logs that will be used to build the test metrics. */
        public InputStream getMetricsEvents(Instant startTime) throws IOException {
            ProcessBuilder pb =
                    new ProcessBuilder(
                            Arrays.asList(
                                    "logcat",
                                    "-s",
                                    "TopicsCrystalBallTest:D",
                                    "-t",
                                    LOG_TIME_FORMATTER.format(startTime)));
            return pb.start().getInputStream();
        }
    }
}
