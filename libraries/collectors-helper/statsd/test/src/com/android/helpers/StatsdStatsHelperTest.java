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

        public TestNonEmptyStatsdHelper() {
            populateAtomStatsTestData(testReport);
        }

        private void populateAtomStatsTestData(StatsLog.StatsdStatsReport testReport) {
            testReport.atomStats = new StatsLog.StatsdStatsReport.AtomStats[ATOM_STATS_COUNT];

            for (int i = 0; i < ATOM_STATS_COUNT; i++) {
                testReport.atomStats[i] = new StatsLog.StatsdStatsReport.AtomStats();
                int fieldValue = i + 1;
                testReport.atomStats[i].tag = fieldValue++;
                testReport.atomStats[i].count = fieldValue++;
                testReport.atomStats[i].errorCount = fieldValue++;
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

    @Test
    public void testNonEmptyReport() throws Exception {
        StatsdStatsHelper.IStatsdHelper statsdHelper = new TestNonEmptyStatsdHelper();
        StatsdStatsHelper statsdStatsHelper = new StatsdStatsHelper(statsdHelper);

        assertTrue(statsdStatsHelper.startCollecting());
        final Map<String, Long> result = statsdStatsHelper.getMetrics();
        verifyAtomStats(result, TestNonEmptyStatsdHelper.ATOM_STATS_COUNT);
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
