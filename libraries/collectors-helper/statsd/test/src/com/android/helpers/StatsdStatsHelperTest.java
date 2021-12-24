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
package com.android.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.runner.AndroidJUnit4;

import com.android.os.nano.StatsLog;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

/**
 * Android Unit tests for {@link StatsdStatsHelper}.
 *
 * <p>To run: Disable SELinux: adb shell setenforce 0; if this fails with "permission denied", try
 * Build and install Development apk. "adb shell su 0 setenforce 0" atest
 * CollectorsHelperTest:com.android.helpers.StatsdStatsTest
 */
@RunWith(AndroidJUnit4.class)
public class StatsdStatsHelperTest {

    private static class TestNonEmptyStatsdHelper implements StatsdStatsHelper.IStatsdHelper {

        StatsLog.StatsdStatsReport testReport = new StatsLog.StatsdStatsReport();

        static final int ATOM_STATS_COUNT = 2;
        static final int CONFIG_STATS_COUNT = 1;
        static final int CONFIG_STATS_METRIC_COUNT = 2;
        static final int CONFIG_STATS_CONDITION_COUNT = 2;
        static final int CONFIG_STATS_ALERT_COUNT = 2;
        static final int CONFIG_STATS_MATCHER_COUNT = 2;
        static final int PULLED_ATOM_STATS_COUNT = 2;

        public TestNonEmptyStatsdHelper() {
            populateAtomStatsTestData(testReport);
            populateConfigStatsTestData(testReport);
            populateAnomalyAlarmStatsTestData(testReport);
            populatePulledAtomStatsTestData(testReport);
        }

        private static void populateAtomStatsTestData(StatsLog.StatsdStatsReport testReport) {
            testReport.atomStats = new StatsLog.StatsdStatsReport.AtomStats[ATOM_STATS_COUNT];

            for (int i = 0; i < ATOM_STATS_COUNT; i++) {
                testReport.atomStats[i] = new StatsLog.StatsdStatsReport.AtomStats();
                int fieldValue = i + 1;
                testReport.atomStats[i].tag = fieldValue++;
                testReport.atomStats[i].count = fieldValue++;
                testReport.atomStats[i].errorCount = fieldValue++;
            }
        }

        private static void populateConfigStatsTestData(StatsLog.StatsdStatsReport testReport) {
            testReport.configStats = new StatsLog.StatsdStatsReport.ConfigStats[CONFIG_STATS_COUNT];
            for (int i = 0; i < CONFIG_STATS_COUNT; i++) {
                testReport.configStats[i] = new StatsLog.StatsdStatsReport.ConfigStats();
                testReport.configStats[i].id = i + 1;
                testReport.configStats[i].metricCount = CONFIG_STATS_METRIC_COUNT;
                testReport.configStats[i].conditionCount = CONFIG_STATS_CONDITION_COUNT;
                testReport.configStats[i].alertCount = CONFIG_STATS_ALERT_COUNT;
                testReport.configStats[i].matcherCount = CONFIG_STATS_MATCHER_COUNT;

                testReport.configStats[i].metricStats =
                        populateConfigStatsMetricTestData(CONFIG_STATS_METRIC_COUNT);
                testReport.configStats[i].conditionStats =
                        populateConfigStatsConditionTestData(CONFIG_STATS_CONDITION_COUNT);
                testReport.configStats[i].matcherStats =
                        populateConfigStatsMatcherTestData(CONFIG_STATS_ALERT_COUNT);
                testReport.configStats[i].alertStats =
                        populateConfigStatsAlertTestData(CONFIG_STATS_MATCHER_COUNT);
            }
        }

        private static StatsLog.StatsdStatsReport.AlertStats[] populateConfigStatsAlertTestData(
                int configStatsAlertCount) {
            StatsLog.StatsdStatsReport.AlertStats[] alertStats =
                    new StatsLog.StatsdStatsReport.AlertStats[configStatsAlertCount];

            for (int i = 0; i < configStatsAlertCount; i++) {
                alertStats[i] = new StatsLog.StatsdStatsReport.AlertStats();
                int fieldValue = i + 1;
                alertStats[i].id = fieldValue++;
                alertStats[i].alertedTimes = fieldValue++;
            }

            return alertStats;
        }

        private static StatsLog.StatsdStatsReport.MetricStats[] populateConfigStatsMetricTestData(
                int configStatsMetricCount) {
            StatsLog.StatsdStatsReport.MetricStats[] metricStats =
                    new StatsLog.StatsdStatsReport.MetricStats[configStatsMetricCount];

            for (int i = 0; i < configStatsMetricCount; i++) {
                metricStats[i] = new StatsLog.StatsdStatsReport.MetricStats();
                int fieldValue = i + 1;
                metricStats[i].id = fieldValue++;
                metricStats[i].maxTupleCounts = fieldValue++;
            }

            return metricStats;
        }

        private static StatsLog.StatsdStatsReport.ConditionStats[]
                populateConfigStatsConditionTestData(int configStatsConditionCount) {
            StatsLog.StatsdStatsReport.ConditionStats[] conditionStats =
                    new StatsLog.StatsdStatsReport.ConditionStats[configStatsConditionCount];

            for (int i = 0; i < configStatsConditionCount; i++) {
                conditionStats[i] = new StatsLog.StatsdStatsReport.ConditionStats();
                int fieldValue = i + 1;
                conditionStats[i].id = fieldValue++;
                conditionStats[i].maxTupleCounts = fieldValue++;
            }

            return conditionStats;
        }

        private static StatsLog.StatsdStatsReport.MatcherStats[] populateConfigStatsMatcherTestData(
                int configStatsMatcherCount) {
            StatsLog.StatsdStatsReport.MatcherStats[] matcherStats =
                    new StatsLog.StatsdStatsReport.MatcherStats[configStatsMatcherCount];

            for (int i = 0; i < configStatsMatcherCount; i++) {
                matcherStats[i] = new StatsLog.StatsdStatsReport.MatcherStats();
                int fieldValue = i + 1;
                matcherStats[i].id = fieldValue++;
                matcherStats[i].matchedTimes = fieldValue++;
            }

            return matcherStats;
        }

        private static void populateAnomalyAlarmStatsTestData(
                StatsLog.StatsdStatsReport testReport) {
            testReport.anomalyAlarmStats = new StatsLog.StatsdStatsReport.AnomalyAlarmStats();
            testReport.anomalyAlarmStats.alarmsRegistered = 1;
        }

        private void populatePulledAtomStatsTestData(StatsLog.StatsdStatsReport testReport) {
            testReport.pulledAtomStats =
                    new StatsLog.StatsdStatsReport.PulledAtomStats[PULLED_ATOM_STATS_COUNT];

            for (int i = 0; i < PULLED_ATOM_STATS_COUNT; i++) {
                testReport.pulledAtomStats[i] = new StatsLog.StatsdStatsReport.PulledAtomStats();
                int fieldValue = i + 1;
                testReport.pulledAtomStats[i].atomId = fieldValue++;
                testReport.pulledAtomStats[i].totalPull = fieldValue++;
                testReport.pulledAtomStats[i].totalPullFromCache = fieldValue++;
                testReport.pulledAtomStats[i].minPullIntervalSec = fieldValue++;
                testReport.pulledAtomStats[i].averagePullTimeNanos = fieldValue++;
                testReport.pulledAtomStats[i].maxPullTimeNanos = fieldValue++;
                testReport.pulledAtomStats[i].averagePullDelayNanos = fieldValue++;
                testReport.pulledAtomStats[i].dataError = fieldValue++;
                testReport.pulledAtomStats[i].pullTimeout = fieldValue++;
                testReport.pulledAtomStats[i].pullExceedMaxDelay = fieldValue++;
                testReport.pulledAtomStats[i].pullFailed = fieldValue++;
                testReport.pulledAtomStats[i].emptyData = fieldValue++;
                testReport.pulledAtomStats[i].registeredCount = fieldValue++;
                testReport.pulledAtomStats[i].unregisteredCount = fieldValue++;
                testReport.pulledAtomStats[i].atomErrorCount = fieldValue++;
                testReport.pulledAtomStats[i].binderCallFailed = fieldValue++;
                testReport.pulledAtomStats[i].failedUidProviderNotFound = fieldValue++;
                testReport.pulledAtomStats[i].pullerNotFound = fieldValue++;
            }
        }

        @Override
        public StatsLog.StatsdStatsReport getStatsdStatsReport() {
            return testReport;
        }
    }

    private static class TestEmptyStatsdHelper implements StatsdStatsHelper.IStatsdHelper {

        StatsLog.StatsdStatsReport testReport = new StatsLog.StatsdStatsReport();

        public TestEmptyStatsdHelper() {}

        @Override
        public StatsLog.StatsdStatsReport getStatsdStatsReport() {
            return testReport;
        }
    }

    private static void verifyAtomStats(Map<String, Long> result, int atomsCount) {
        for (int i = 0; i < atomsCount; i++) {
            final String metricKeyPrefix =
                    MetricUtility.constructKey(
                            StatsdStatsHelper.STATSDSTATS_PREFIX,
                            StatsdStatsHelper.ATOM_STATS_PREFIX,
                            String.valueOf(i + 1));
            int fieldValue = i + 2;
            assertEquals(
                    result.get(MetricUtility.constructKey(metricKeyPrefix, "count")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(MetricUtility.constructKey(metricKeyPrefix, "error_count")),
                    Long.valueOf(fieldValue++));
        }
    }

    private static void verifyConfigAlertStats(
            Map<String, Long> result, String metricKeyPrefix, long count) {
        for (long i = 0; i < count; i++) {
            final String metricKey =
                    MetricUtility.constructKey(
                            metricKeyPrefix,
                            StatsdStatsHelper.ALERT_STATS_PREFIX,
                            String.valueOf(i + 1),
                            "alerted_times");
            assertEquals(result.get(metricKey), Long.valueOf(i + 2));
        }
    }

    private static void verifyConfigMatcherStats(
            Map<String, Long> result, String metricKeyPrefix, long count) {
        for (long i = 0; i < count; i++) {
            final String metricKey =
                    MetricUtility.constructKey(
                            metricKeyPrefix,
                            StatsdStatsHelper.MATCHER_STATS_PREFIX,
                            String.valueOf(i + 1),
                            "matched_times");
            assertEquals(result.get(metricKey), Long.valueOf(i + 2));
        }
    }

    private static void verifyConfigConditionStats(
            Map<String, Long> result, String metricKeyPrefix, long count) {
        for (long i = 0; i < count; i++) {
            final String metricKey =
                    MetricUtility.constructKey(
                            metricKeyPrefix,
                            StatsdStatsHelper.CONDITION_STATS_PREFIX,
                            String.valueOf(i + 1),
                            "max_tuple_counts");
            assertEquals(result.get(metricKey), Long.valueOf(i + 2));
        }
    }

    private static void verifyConfigMetricStats(
            Map<String, Long> result, String metricKeyPrefix, long count) {
        for (long i = 0; i < count; i++) {
            final String metricKey =
                    MetricUtility.constructKey(
                            metricKeyPrefix,
                            StatsdStatsHelper.METRIC_STATS_PREFIX,
                            String.valueOf(i + 1),
                            "max_tuple_counts");
            assertEquals(result.get(metricKey), Long.valueOf(i + 2));
        }
    }

    private static void verifyConfigStats(
            Map<String, Long> result,
            int configStatsCount,
            int configStatsMetricCount,
            int configStatsConditionCount,
            int configStatsMatcherCount,
            int configStatsAlertCount) {

        for (int i = 0; i < configStatsCount; i++) {
            final String metricKeyPrefix =
                    MetricUtility.constructKey(
                            StatsdStatsHelper.STATSDSTATS_PREFIX,
                            StatsdStatsHelper.CONFIG_STATS_PREFIX,
                            String.valueOf(i + 1));

            final String metricCountKey =
                    MetricUtility.constructKey(metricKeyPrefix, "metric_count");
            assertEquals(result.get(metricCountKey), Long.valueOf(configStatsMetricCount));
            verifyConfigMetricStats(result, metricKeyPrefix, result.get(metricCountKey));
            final String conditionCountKey =
                    MetricUtility.constructKey(metricKeyPrefix, "condition_count");
            assertEquals(result.get(conditionCountKey), Long.valueOf(configStatsConditionCount));
            verifyConfigConditionStats(result, metricKeyPrefix, result.get(conditionCountKey));
            final String matcherCountKey =
                    MetricUtility.constructKey(metricKeyPrefix, "matcher_count");
            assertEquals(result.get(matcherCountKey), Long.valueOf(configStatsMatcherCount));
            verifyConfigMatcherStats(result, metricKeyPrefix, result.get(matcherCountKey));
            final String alertCountKey = MetricUtility.constructKey(metricKeyPrefix, "alert_count");
            assertEquals(result.get(alertCountKey), Long.valueOf(configStatsAlertCount));
            verifyConfigAlertStats(result, metricKeyPrefix, result.get(alertCountKey));
        }
    }

    private static void verifyAnomalyAlarmStats(Map<String, Long> result) {
        final String metricKeyPrefix =
                MetricUtility.constructKey(
                        StatsdStatsHelper.STATSDSTATS_PREFIX,
                        StatsdStatsHelper.ANOMALY_ALARM_STATS_PREFIX);
        final String metricKey = MetricUtility.constructKey(metricKeyPrefix, "alarms_registered");
        assertEquals(result.get(metricKey), Long.valueOf(1));
    }

    private static void verifyPulledAtomStats(Map<String, Long> result, int pulledAtomStatsCount) {
        for (int i = 0; i < pulledAtomStatsCount; i++) {
            int fieldValue = i + 1;
            final String metricKeyPrefix =
                    MetricUtility.constructKey(
                            StatsdStatsHelper.STATSDSTATS_PREFIX,
                            StatsdStatsHelper.PULLED_ATOM_STATS_PREFIX,
                            String.valueOf(fieldValue++));
            assertEquals(
                    result.get(MetricUtility.constructKey(metricKeyPrefix, "total_pull")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(
                            MetricUtility.constructKey(metricKeyPrefix, "total_pull_from_cache")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(
                            MetricUtility.constructKey(metricKeyPrefix, "min_pull_interval_sec")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(
                            MetricUtility.constructKey(metricKeyPrefix, "average_pull_time_nanos")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(MetricUtility.constructKey(metricKeyPrefix, "max_pull_time_nanos")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(
                            MetricUtility.constructKey(
                                    metricKeyPrefix, "average_pull_delay_nanos")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(MetricUtility.constructKey(metricKeyPrefix, "data_error")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(MetricUtility.constructKey(metricKeyPrefix, "pull_timeout")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(
                            MetricUtility.constructKey(metricKeyPrefix, "pull_exceed_max_delay")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(MetricUtility.constructKey(metricKeyPrefix, "pull_failed")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(MetricUtility.constructKey(metricKeyPrefix, "empty_data")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(
                            MetricUtility.constructKey(metricKeyPrefix, "pull_registered_count")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(
                            MetricUtility.constructKey(metricKeyPrefix, "pull_unregistered_count")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(MetricUtility.constructKey(metricKeyPrefix, "atom_error_count")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(MetricUtility.constructKey(metricKeyPrefix, "binder_call_failed")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(
                            MetricUtility.constructKey(
                                    metricKeyPrefix, "failed_uid_provider_not_found")),
                    Long.valueOf(fieldValue++));
            assertEquals(
                    result.get(MetricUtility.constructKey(metricKeyPrefix, "puller_not_found")),
                    Long.valueOf(fieldValue++));
        }
    }

    @Test
    public void testNonEmptyReport() throws Exception {
        StatsdStatsHelper.IStatsdHelper statsdHelper = new TestNonEmptyStatsdHelper();
        StatsdStatsHelper statsdStatsHelper = new StatsdStatsHelper(statsdHelper);

        assertTrue(statsdStatsHelper.startCollecting());
        final Map<String, Long> result = statsdStatsHelper.getMetrics();
        verifyAtomStats(result, TestNonEmptyStatsdHelper.ATOM_STATS_COUNT);
        verifyConfigStats(
                result,
                TestNonEmptyStatsdHelper.CONFIG_STATS_COUNT,
                TestNonEmptyStatsdHelper.CONFIG_STATS_METRIC_COUNT,
                TestNonEmptyStatsdHelper.CONFIG_STATS_CONDITION_COUNT,
                TestNonEmptyStatsdHelper.CONFIG_STATS_MATCHER_COUNT,
                TestNonEmptyStatsdHelper.CONFIG_STATS_ALERT_COUNT);
        verifyAnomalyAlarmStats(result);
        verifyPulledAtomStats(result, TestNonEmptyStatsdHelper.PULLED_ATOM_STATS_COUNT);
        assertTrue(statsdStatsHelper.stopCollecting());
    }

    @Test
    public void testEmptyReport() throws Exception {
        StatsdStatsHelper.IStatsdHelper statsdHelper = new TestEmptyStatsdHelper();
        StatsdStatsHelper statsdStatsHelper = new StatsdStatsHelper(statsdHelper);

        assertTrue(statsdStatsHelper.startCollecting());
        final Map<String, Long> result = statsdStatsHelper.getMetrics();
        assertEquals(result.size(), 0);
        assertTrue(statsdStatsHelper.stopCollecting());
    }
}
