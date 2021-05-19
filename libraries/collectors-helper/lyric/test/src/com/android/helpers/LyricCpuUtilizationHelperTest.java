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

import static org.junit.Assert.assertEquals;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

/**
 * Android unit test for {@link LyricCpuUtilizationHelper}
 *
 * <p>To run: atest CollectorsHelperTest:com.android.helpers.tests.LyricCpuUtilizationHelper
 */
@RunWith(AndroidJUnit4.class)
public class LyricCpuUtilizationHelperTest {

    private static final String METRIC_KEY = "cpu_util_%s_%s";

    @Test
    public void testProcessLine() {
        String testString =
                "CPU Usage during ProcessInput for [>] p2 cam2_retiming:empty_group after 593"
                        + " invocations - User: 1.709118ms (Max: 3.67ms Min:0) System: 425.17025us"
                        + " (Max: 3.372ms Min:0) Wall: 50.14003675ms (Max: 55.676595ms"
                        + " Min:-1.5s)";

        Map<String, Double> metrics = LyricCpuUtilizationHelper.processLine(testString);

        String node = "cam2_retiming-empty_group";
        assertEquals(
                Double.valueOf(593),
                metrics.get(String.format(METRIC_KEY, node, "number_of_invocations")));
        assertEquals(
                Double.valueOf(1.709118),
                metrics.get(String.format(METRIC_KEY, node, "user_time")));
        assertEquals(
                Double.valueOf(3.67),
                metrics.get(String.format(METRIC_KEY, node, "user_time_max")));
        assertEquals(
                Double.valueOf(0), metrics.get(String.format(METRIC_KEY, node, "user_time_min")));
        assertEquals(
                Double.valueOf(0.42517025),
                metrics.get(String.format(METRIC_KEY, node, "system_time")));
        assertEquals(
                Double.valueOf(3.372),
                metrics.get(String.format(METRIC_KEY, node, "system_time_max")));
        assertEquals(
                Double.valueOf(0), metrics.get(String.format(METRIC_KEY, node, "system_time_min")));
        assertEquals(
                Double.valueOf(50.14003675),
                metrics.get(String.format(METRIC_KEY, node, "wall_time")));
        assertEquals(
                Double.valueOf(55.676595),
                metrics.get(String.format(METRIC_KEY, node, "wall_time_max")));
        assertEquals(
                Double.valueOf(-1500),
                metrics.get(String.format(METRIC_KEY, node, "wall_time_min")));
    }
}
