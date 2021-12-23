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

import androidx.annotation.VisibleForTesting;

import com.android.os.nano.StatsLog;

import java.util.HashMap;
import java.util.Map;

/**
 * StatsdStatsHelper consist of helper methods to set the Statsd metadata collection and retrieve
 * the necessary information from statsd.
 */
public class StatsdStatsHelper implements ICollectorHelper<Long> {

    static final String STATSDSTATS_PREFIX = "statsdstats";
    static final String ATOM_STATS_PREFIX = "atom_stats";

    interface IStatsdHelper {
        StatsLog.StatsdStatsReport getStatsdStatsReport();
    }

    private static class DefaultStatsdHelper implements IStatsdHelper {

        private StatsdHelper mStatsdHelper = new StatsdHelper();

        @Override
        public StatsLog.StatsdStatsReport getStatsdStatsReport() {
            return mStatsdHelper.getStatsdStatsReport();
        }
    }

    private IStatsdHelper mStatsdHelper = new DefaultStatsdHelper();

    public StatsdStatsHelper() {}

    /**
     * Constructor to simulate an externally provided statsd helper instance. Should not be used
     * except for testing.
     */
    @VisibleForTesting
    StatsdStatsHelper(IStatsdHelper helper) {
        mStatsdHelper = helper;
    }

    /** Resets statsd metadata */
    @Override
    public boolean startCollecting() {
        // TODO: http://b/204890512 implement metadata reset
        return true;
    }

    /** Collect the statsd metadata accumulated during the test run. */
    @Override
    public Map<String, Long> getMetrics() {
        Map<String, Long> resultMap = new HashMap<>();

        final StatsLog.StatsdStatsReport report = mStatsdHelper.getStatsdStatsReport();
        populateAtomStats(report.atomStats, resultMap);

        return resultMap;
    }

    private void populateAtomStats(
            StatsLog.StatsdStatsReport.AtomStats[] atomStats, Map<String, Long> resultMap) {
        final String metricKeyPrefix =
                MetricUtility.constructKey(STATSDSTATS_PREFIX, ATOM_STATS_PREFIX);

        for (final StatsLog.StatsdStatsReport.AtomStats dataItem : atomStats) {
            final String metricKeyPrefixWithTag =
                    MetricUtility.constructKey(metricKeyPrefix, String.valueOf(dataItem.tag));
            resultMap.put(
                    MetricUtility.constructKey(metricKeyPrefixWithTag, "count"),
                    Long.valueOf(dataItem.count));
            resultMap.put(
                    MetricUtility.constructKey(metricKeyPrefixWithTag, "error_count"),
                    Long.valueOf(dataItem.errorCount));
        }
    }

    /** No op. */
    @Override
    public boolean stopCollecting() {
        return true;
    }
}
