/*
 * Copyright (C) 2020 The Android Open Source Project
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

import com.android.os.AtomsProto;
import com.android.os.AtomsProto.Atom;
import com.android.os.StatsLog.EventMetricData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper consisting of helper methods to set system interactions configs in statsd and retrieve the
 * necessary information from statsd using the config id.
 */
public class UiActionLatencyHelper implements ICollectorHelper<StringBuilder> {

    private static final String LOG_TAG = UiActionLatencyHelper.class.getSimpleName();

    private final StatsdHelper mStatsdHelper = new StatsdHelper();

    /** Set up the system actions latency statsd config. */
    @Override
    public boolean startCollecting() {
        Log.i(LOG_TAG, "Adding system actions latency config to statsd.");
        List<Integer> atomIdList = new ArrayList<>();
        atomIdList.add(Atom.UI_ACTION_LATENCY_REPORTED_FIELD_NUMBER);
        return mStatsdHelper.addEventConfig(atomIdList);
    }

    /** Collect the system actions latency metrics from the statsd. */
    @Override
    public Map<String, StringBuilder> getMetrics() {
        Log.i(LOG_TAG, "get metrics.");
        Map<String, StringBuilder> latenciesMap = new HashMap<>();
        for (EventMetricData dataItem : mStatsdHelper.getEventMetrics()) {
            final Atom atom = dataItem.getAtom();
            if (atom.hasUiActionLatencyReported()) {
                final AtomsProto.UIActionLatencyReported uiActionLatencyReported =
                        atom.getUiActionLatencyReported();

                final String action = String.valueOf(uiActionLatencyReported.getAction());

                MetricUtility.addMetric(
                        MetricUtility.constructKey("latency", action),
                        uiActionLatencyReported.getLatencyMillis(),
                        latenciesMap);
            }
        }

        return latenciesMap;
    }

    /** Remove the statsd config. */
    @Override
    public boolean stopCollecting() {
        return mStatsdHelper.removeStatsConfig();
    }
}
