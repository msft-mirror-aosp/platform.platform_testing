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

import com.android.internal.jank.InteractionJankMonitor;
import com.android.os.nano.AtomsProto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper consisting of helper methods to set system interactions configs in statsd and retrieve the
 * necessary information from statsd using the config id.
 */
public class UiInteractionFrameInfoHelper implements ICollectorHelper<StringBuilder> {

    private static final String LOG_TAG = UiInteractionFrameInfoHelper.class.getSimpleName();

    private final StatsdHelper mStatsdHelper = new StatsdHelper();

    /** Set up the system interactions jank statsd config. */
    @Override
    public boolean startCollecting() {
        Log.i(LOG_TAG, "Adding system interactions config to statsd.");
        List<Integer> atomIdList = new ArrayList<>();
        atomIdList.add(AtomsProto.Atom.UI_INTERACTION_FRAME_INFO_REPORTED_FIELD_NUMBER);
        return mStatsdHelper.addEventConfig(atomIdList);
    }

    /** Collect the system interactions jank metrics from the statsd. */
    @Override
    public Map<String, StringBuilder> getMetrics() {
        Log.i(LOG_TAG, "get metrics.");
        Map<String, StringBuilder> frameInfoMap = new HashMap<>();
        for (com.android.os.nano.StatsLog.EventMetricData dataItem :
                mStatsdHelper.getEventMetrics()) {
            final AtomsProto.Atom atom = dataItem.atom;
            if (atom.hasUiInteractionFrameInfoReported()) {
                final AtomsProto.UIInteractionFrameInfoReported uiInteractionFrameInfoReported =
                        atom.getUiInteractionFrameInfoReported();

                final String interactionType =
                        InteractionJankMonitor.getNameOfInteraction(
                                uiInteractionFrameInfoReported.interactionType);

                MetricUtility.addMetric(
                        MetricUtility.constructKey("cuj", interactionType, "total_frames"),
                        uiInteractionFrameInfoReported.totalFrames,
                        frameInfoMap);

                MetricUtility.addMetric(
                        MetricUtility.constructKey("cuj", interactionType, "missed_frames"),
                        uiInteractionFrameInfoReported.missedFrames,
                        frameInfoMap);

                MetricUtility.addMetric(
                        MetricUtility.constructKey("cuj", interactionType, "max_frame_time_ms"),
                        uiInteractionFrameInfoReported.maxFrameTimeNanos / 1000000.0,
                        frameInfoMap);
            }
        }

        return frameInfoMap;
    }

    /** Remove the statsd config. */
    @Override
    public boolean stopCollecting() {
        return mStatsdHelper.removeStatsConfig();
    }
}
