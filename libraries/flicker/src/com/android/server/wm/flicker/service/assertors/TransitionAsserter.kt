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

package com.android.server.wm.flicker.service.assertors

import android.util.Log
import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.service.ScenarioInstance
import com.android.server.wm.traces.common.transition.Transition
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

/**
 * Class that runs FASS assertions.
 */
class TransitionAsserter(
    internal val assertions: List<AssertionData>,
    private val logger: (String) -> Unit
) {
    /** {@inheritDoc} */
    fun analyze(
        scenarioInstance: ScenarioInstance,
        wmTrace: WindowManagerTrace,
        layersTrace: LayersTrace
    ): List<AssertionResult> {
        val (wmTraceForTransition, layersTraceForTransition) =
            splitTraces(scenarioInstance, wmTrace, layersTrace)

        if (wmTraceForTransition == null) {
            Log.w(FLICKER_TAG, "wmTraceForTransition was null for $scenarioInstance")
        }

        if (layersTraceForTransition == null) {
            Log.w(FLICKER_TAG, "layersTraceForTransition was null for $scenarioInstance")
        }

        require(wmTraceForTransition != null || layersTraceForTransition != null) {
            "At least one of wm or layers trace must not be null for $scenarioInstance"
        }

        logger.invoke("Running assertions...")
        return runAssertionsOnSubjects(
            scenarioInstance, wmTraceForTransition, layersTraceForTransition,
            wmTrace, layersTrace, assertions
        )
    }

    private fun runAssertionsOnSubjects(
        scenarioInstance: ScenarioInstance,
        wmTraceToAssert: WindowManagerTrace?,
        layersTraceToAssert: LayersTrace?,
        entireWmTrace: WindowManagerTrace,
        entireLayersTrace: LayersTrace,
        assertions: List<AssertionData>
    ): List<AssertionResult> {
        val results: MutableList<AssertionResult> = mutableListOf()

        assertions.forEach {
            val assertion = it.assertionBuilder
            logger.invoke("Running assertion $assertion for ${it.scenario}")
            val faasFacts = FaasData(scenarioInstance, entireWmTrace, entireLayersTrace).toFacts()
            val wmSubject = if (wmTraceToAssert != null) {
                WindowManagerTraceSubject.assertThat(wmTraceToAssert, facts = faasFacts)
            } else {
                null
            }
            val layersSubject = if (layersTraceToAssert != null) {
                LayersTraceSubject.assertThat(layersTraceToAssert, facts = faasFacts)
            } else {
                null
            }
            val result = assertion.evaluate(scenarioInstance, wmSubject, layersSubject, it.scenario)
            results.add(result)
        }

        return results
    }

    /**
     * Splits a [WindowManagerTrace] and a [LayersTrace] by a [Transition].
     *
     * @param tag a list with all [TransitionTag]s
     * @param wmTrace Window Manager trace
     * @param layersTrace Surface Flinger trace
     * @return a list with [WindowManagerTrace] blocks
     */
    private fun splitTraces(
        scenarioInstance: ScenarioInstance,
        wmTrace: WindowManagerTrace,
        layersTrace: LayersTrace
    ): FilteredTraces {
        var filteredWmTrace: WindowManagerTrace? = wmTrace.scenarioInstanceSlice(scenarioInstance)
        var filteredLayersTrace: LayersTrace? = layersTrace.scenarioInstanceSlice(scenarioInstance)

        if (filteredWmTrace?.entries?.isEmpty() == true) {
            // Empty trace, nothing to assert on the wmTrace
            logger("Got an empty wmTrace for $scenarioInstance")
            filteredWmTrace = null
        }

        if (filteredLayersTrace?.entries?.isEmpty() == true) {
            // Empty trace, nothing to assert on the layers trace
            logger("Got an empty surface trace for $scenarioInstance")
            filteredLayersTrace = null
        }

        return FilteredTraces(filteredWmTrace, filteredLayersTrace)
    }

    data class FilteredTraces(
        val wmTrace: WindowManagerTrace?,
        val layersTrace: LayersTrace?
    )
}

data class TraceIndices(
    val startIndex: Int,
    val endIndex: Int
)

/**
 * Return a new trace contains only the entries that were applied during the transition's execution.
 */
public fun WindowManagerTrace.scenarioInstanceSlice(
    scenarioInstance: ScenarioInstance
): WindowManagerTrace {
    return slice(scenarioInstance.startTimestamp, scenarioInstance.endTimestamp)
}

/**
 * Return the start and end indices where the layersTrace needs to be trimmed
 * to match only the entries that were applied during the transition's execution.
 */
fun LayersTrace.scenarioInstanceSliceIndices(
    scenarioInstance: ScenarioInstance
): TraceIndices? {
    var startIndex = -1
    var prevVsyncId = -1L
    for (i in entries.indices) {
        val currentVsyncId = entries[i].vSyncId

        if (scenarioInstance.startTransaction.appliedVSyncId in (prevVsyncId + 1)..currentVsyncId) {
            startIndex = i
        }

        prevVsyncId = currentVsyncId
    }

    if (startIndex == -1) {
        Log.w(FLICKER_TAG, "Skipping... Didn't find start layers entry for $scenarioInstance.")
        return null
    }

    var endIndex = -1
    prevVsyncId = if (startIndex < 1) {
        -1L
    } else {
        entries[startIndex - 1].vSyncId
    }
    for (i in startIndex until entries.size) {
        val currentVsyncId = entries[i].vSyncId

        if (scenarioInstance.finishTransaction.appliedVSyncId
            in (prevVsyncId + 1)..currentVsyncId) {
            endIndex = i
        }

        prevVsyncId = currentVsyncId
    }

    if (endIndex == -1) {
        Log.w(FLICKER_TAG, "Skipping... Didn't find start layers entry for $scenarioInstance.")
        return null
    }

    if (startIndex == endIndex) {
        Log.w(FLICKER_TAG, "Start and end of $scenarioInstance happen in same entry.")
    }

    return TraceIndices(startIndex, endIndex)
}

/**
 * Return a new trace contains only the entries that were applied during the transition's execution.
 */
fun LayersTrace.scenarioInstanceSlice(
    scenarioInstance: ScenarioInstance
): LayersTrace? {
    return scenarioInstanceSliceIndices(scenarioInstance)?.let {
        LayersTrace(entries.slice(it.startIndex..it.endIndex).toTypedArray())
    }
}
