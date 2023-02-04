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

package com.android.server.wm.flicker.assertions

import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.AssertionTag
import com.android.server.wm.flicker.io.IReader
import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

/**
 * Helper class to read traces from a [resultReader] and parse them into subjects for assertion
 *
 * @param resultReader to read the result artifacts
 */
open class SubjectsParser(private val resultReader: IReader) {
    fun getSubjects(tag: String): List<FlickerSubject> {
        val result = mutableListOf<FlickerSubject>()

        if (tag == AssertionTag.ALL) {
            wmTraceSubject?.let { result.add(it) }
            layersTraceSubject?.let { result.add(it) }
        } else {
            getWmStateSubject(tag)?.let { result.add(it) }
            getLayerTraceEntrySubject(tag)?.let { result.add(it) }
        }
        eventLogSubject?.let { result.add(it) }

        return result
    }

    /** Truth subject that corresponds to a [WindowManagerTrace] */
    private val wmTraceSubject: WindowManagerTraceSubject?
        get() = doGetWmTraceSubject()

    protected open fun doGetWmTraceSubject(): WindowManagerTraceSubject? {
        val trace = resultReader.readWmTrace() ?: return null
        return WindowManagerTraceSubject(trace)
    }

    /** Truth subject that corresponds to a [LayersTrace] */
    private val layersTraceSubject: LayersTraceSubject?
        get() = doGetLayersTraceSubject()

    protected open fun doGetLayersTraceSubject(): LayersTraceSubject? {
        val trace = resultReader.readLayersTrace() ?: return null
        return LayersTraceSubject(trace)
    }

    /** Truth subject that corresponds to a [WindowManagerState] */
    private fun getWmStateSubject(tag: String): WindowManagerStateSubject? =
        doGetWmStateSubject(tag)

    protected open fun doGetWmStateSubject(tag: String): WindowManagerStateSubject? {
        return when (tag) {
            AssertionTag.START -> wmTraceSubject?.subjects?.firstOrNull()
            AssertionTag.END -> wmTraceSubject?.subjects?.lastOrNull()
            else -> {
                val trace = resultReader.readWmState(tag) ?: return null
                WindowManagerStateSubject(trace.first())
            }
        }
    }

    /** Truth subject that corresponds to a [LayerTraceEntry] */
    private fun getLayerTraceEntrySubject(tag: String): LayerTraceEntrySubject? =
        doGetLayerTraceEntrySubject(tag)

    protected open fun doGetLayerTraceEntrySubject(tag: String): LayerTraceEntrySubject? {
        return when (tag) {
            AssertionTag.START -> layersTraceSubject?.subjects?.firstOrNull()
            AssertionTag.END -> layersTraceSubject?.subjects?.lastOrNull()
            else -> {
                val trace = resultReader.readLayersDump(tag) ?: return null
                return LayersTraceSubject(trace).first()
            }
        }
    }

    /** Truth subject that corresponds to a list of [FocusEvent] */
    val eventLogSubject: EventLogSubject?
        get() = doGetEventLogSubject()

    protected open fun doGetEventLogSubject(): EventLogSubject? {
        val trace = resultReader.readEventLogTrace() ?: return null
        return EventLogSubject(trace)
    }

    /**
     * A trace of all transitions that ran during the run that can be used by FaaS to determine
     * which assertion to run and on which parts of the run.
     */
    @VisibleForTesting
    fun readTransitionsTraceForTesting(): TransitionsTrace? = resultReader.readTransitionsTrace()
}
