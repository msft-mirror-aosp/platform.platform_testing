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

package com.android.server.wm.flicker

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.TransitionRunner.Companion.ExecutionError
import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.assertions.FlickerAssertionError
import com.android.server.wm.flicker.assertions.FlickerAssertionErrorBuilder
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.dsl.AssertionTag
import com.android.server.wm.flicker.helpers.clearableLazy
import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.DeviceDumpParser
import com.android.server.wm.traces.parser.layers.LayersTraceParser
import com.android.server.wm.traces.parser.transaction.TransactionsTraceParser
import com.android.server.wm.traces.parser.transition.TransitionsTraceParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser
import java.io.File
import java.util.concurrent.TimeUnit

val CHAR_POOL: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

/** Defines the result of a flicker run */
class FlickerRunResult(
    testName: String,
    private val traceConfig: TraceConfigs = DEFAULT_TRACE_CONFIG
) {

    data class TraceTime(
        val elapsedRealtimeNanos: Long,
        val systemTime: Long,
        val unixTimeNanos: Long
    ) {
        companion object {
            val MIN = TraceTime(0, 0, 0)
            val MAX = TraceTime(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)
        }
    }

    /**
     * Logs the start and end times of the transition, so we can crop the traces to exclude the
     * setups and teardowns to only run the assertion on the transition.
     */
    lateinit var transitionStartTime: TraceTime
    lateinit var transitionEndTime: TraceTime

    /**
     * The object responsible for managing the trace file associated with this result.
     *
     * By default the file manager is the RunResult itself but in the case the RunResult is derived
     * or extracted from another RunResult then that other RunResult should be the trace file
     * manager.
     */
    private val artifacts: RunResultArtifacts =
        RunResultArtifacts(getDefaultFlickerOutputDir().resolve("$testName.zip"))

    /** Truth subject that corresponds to a [WindowManagerTrace] */
    internal var wmTraceSubject: WindowManagerTraceSubject? by clearableLazy {
        buildWmTraceSubject()
    }
        private set

    /** Truth subject that corresponds to a [LayersTrace] */
    internal var layersTraceSubject: LayersTraceSubject? by clearableLazy {
        buildLayersTraceSubject()
    }
        private set

    /** Truth subject that corresponds to a list of [FocusEvent] */
    @VisibleForTesting
    var eventLogSubject: EventLogSubject? by clearableLazy { buildEventLog() }
        private set

    /**
     * A trace of all transitions that ran during the run that can be used by FaaS to determine
     * which assertion to run and on which parts of the run.
     */
    var transitionsTrace: TransitionsTrace? by clearableLazy { buildTransitionsTrace() }
        private set

    /**
     * A collection of tagged states collected during the run. Stored as a mapping from tag to state
     * entry subjects representing the dump.
     */
    var taggedStates: Map<String, List<StateDump>>? by clearableLazy { buildTaggedStates() }
        private set

    internal val traceName = this.artifacts.path.fileName ?: "UNNAMED_TRACE"

    val status: RunStatus
        get() {
            return this.artifacts.status
        }

    val isSuccessfulRun: Boolean
        get() = !isFailedRun
    val isFailedRun: Boolean
        get() {
            require(status != RunStatus.UNDEFINED) {
                "RunStatus cannot be UNDEFINED for $traceName"
            }
            // Other types of failures can only happen if the run has succeeded
            return status == RunStatus.RUN_FAILED
        }

    var transitionExecutionError: ExecutionError? = null
        private set

    var faasExecutionError: ExecutionError? = null
        private set

    fun setTransitionExecutionError(executionError: ExecutionError) {
        require(this.transitionExecutionError == null) { "Execution error already set" }
        this.transitionExecutionError = executionError
    }

    fun setFaasExecutionError(executionError: ExecutionError) {
        require(this.faasExecutionError == null) { "Execution error already set" }
        this.faasExecutionError = executionError
    }

    fun getSubjects(tag: String): List<FlickerSubject> {
        val result = mutableListOf<FlickerSubject>()

        if (tag == AssertionTag.ALL) {
            wmTraceSubject?.run { result.add(this) }
            layersTraceSubject?.run { result.add(this) }
            eventLogSubject?.run { result.add(this) }
        } else {
            taggedStates!![tag]?.forEach { it.wmState?.run { result.add(this) } }
            taggedStates!![tag]?.forEach { it.layersState?.run { result.add(this) } }
        }

        return result
    }

    fun checkAssertion(assertion: AssertionData): FlickerAssertionError? {
        require(status != RunStatus.UNDEFINED) { "A valid RunStatus has not been provided" }
        return try {
            assertion.checkAssertion(this)
            null
        } catch (error: Throwable) {
            this.artifacts.status = RunStatus.ASSERTION_FAILED
            FlickerAssertionErrorBuilder()
                .fromError(error)
                .atTag(assertion.tag)
                .withTrace(this.artifacts)
                .build()
        }
    }

    private val taggedStateBuilders: MutableMap<String, MutableList<StateDumpFileNames>> =
        mutableMapOf()
    private var wmTraceFileName: String? = null
    private var layersTraceFileName: String? = null
    private var transactionsTraceFileName: String? = null
    private var transitionsTraceFileName: String? = null

    data class StateDumpFileNames(
        val wmDumpFileName: String,
        val layersDumpFileName: String,
    )

    @VisibleForTesting var eventLog: List<FocusEvent>? = null

    fun setStatus(status: RunStatus) {
        this.artifacts.status = status
    }

    fun setWmTrace(traceFile: File) {
        wmTraceFileName = traceFile.name
        this.artifacts.addFile(traceFile)
    }

    fun setLayersTrace(traceFile: File) {
        layersTraceFileName = traceFile.name
        this.artifacts.addFile(traceFile)
    }

    fun setScreenRecording(screenRecording: File) {
        this.artifacts.addFile(screenRecording)
    }

    fun setTransactionsTrace(traceFile: File) {
        transactionsTraceFileName = traceFile.name
        this.artifacts.addFile(traceFile)
    }

    fun setTransitionsTrace(traceFile: File) {
        transitionsTraceFileName = traceFile.name
        this.artifacts.addFile(traceFile)
    }

    fun addTaggedState(tag: String, wmDumpFile: File, layersDumpFile: File) {
        if (taggedStateBuilders[tag] == null) {
            taggedStateBuilders[tag] = mutableListOf()
        }
        // Append random string to support multiple dumps with the same tag
        val randomString =
            (1..10)
                .map { i -> kotlin.random.Random.nextInt(0, CHAR_POOL.size) }
                .map(CHAR_POOL::get)
                .joinToString("")
        val wmDumpArchiveName = wmDumpFile.name + randomString
        val layersDumpArchiveName = layersDumpFile.name + randomString
        taggedStateBuilders[tag]!!.add(StateDumpFileNames(wmDumpArchiveName, layersDumpArchiveName))
        this.artifacts.addFile(wmDumpFile, wmDumpArchiveName)
        this.artifacts.addFile(layersDumpFile, layersDumpArchiveName)
    }

    fun setResultsFromMonitor(resultSetter: IResultSetter) {
        resultSetter.setResult(this)
    }

    /**
     * @return a Window Manager trace for the part of the trace we want to run the assertions on.
     */
    internal fun buildWmTrace(): WindowManagerTrace? {
        val wmTraceFileName = this.wmTraceFileName ?: return null
        val traceData = this.artifacts.getFileBytes(wmTraceFileName)
        val fullTrace =
            WindowManagerTraceParser.parseFromTrace(traceData, clearCacheAfterParsing = false)
        require(!traceConfig.wmTrace.required || fullTrace.entries.isNotEmpty()) {
            "Full WM trace is empty..."
        }
        val trace =
            fullTrace.slice(
                transitionStartTime.elapsedRealtimeNanos,
                transitionEndTime.elapsedRealtimeNanos,
                addInitialEntry = true
            )
        val minimumEntries = minimumTraceEntriesForConfig(traceConfig.wmTrace)
        require(trace.entries.size >= minimumEntries) {
            "WM trace contained ${trace.entries.size} entries, " +
                "expected at least $minimumEntries... :: " +
                "transition starts at ${transitionStartTime.elapsedRealtimeNanos} and " +
                "ends at ${transitionEndTime.elapsedRealtimeNanos}."
        }
        return trace
    }

    /** @return a layers trace for the part of the trace we want to run the assertions on. */
    internal fun buildLayersTrace(): LayersTrace? {
        val layersTraceFileName = this.layersTraceFileName ?: return null
        val traceData = this.artifacts.getFileBytes(layersTraceFileName)
        val fullTrace = LayersTraceParser.parseFromTrace(traceData, clearCacheAfterParsing = false)
        require(!traceConfig.layersTrace.required || fullTrace.entries.isNotEmpty()) {
            "Full layers trace is empty..."
        }
        val trace =
            fullTrace.slice(
                transitionStartTime.systemTime,
                transitionEndTime.systemTime,
                addInitialEntry = true
            )
        val minimumEntries = minimumTraceEntriesForConfig(traceConfig.layersTrace)
        require(trace.entries.size >= minimumEntries) {
            "Layers trace contained ${trace.entries.size} entries, " +
                "expected at least $minimumEntries... :: " +
                "transition starts at ${transitionStartTime.systemTime} and " +
                "ends at ${transitionEndTime.systemTime}."
        }
        return trace
    }

    private fun buildFullTransactionsTrace(): TransactionsTrace? {
        val transactionsTrace = this.transactionsTraceFileName ?: return null
        val traceData = this.artifacts.getFileBytes(transactionsTrace)
        val fullTrace = TransactionsTraceParser.parseFromTrace(traceData)
        require(fullTrace.entries.isNotEmpty()) { "Transactions trace was empty..." }
        return fullTrace
    }

    /** @return a transactions trace for the part of the trace we want to run the assertions on. */
    private fun buildTransactionsTrace(): TransactionsTrace? {
        val fullTrace = buildFullTransactionsTrace() ?: return null
        val trace = fullTrace.slice(transitionStartTime.systemTime, transitionEndTime.systemTime)
        require(trace.entries.isNotEmpty()) { "Trimmed transactions trace was empty..." }
        return trace
    }

    /** @return a transitions trace for the part of the trace we want to run the assertions on. */
    internal fun buildTransitionsTrace(): TransitionsTrace? {
        val transactionsTrace = buildFullTransactionsTrace()
        val transitionsTrace = this.transitionsTraceFileName ?: return null
        val traceData = this.artifacts.getFileBytes(transitionsTrace)
        val fullTrace = TransitionsTraceParser.parseFromTrace(traceData, transactionsTrace!!)
        val trace =
            fullTrace.slice(
                transitionStartTime.elapsedRealtimeNanos,
                transitionEndTime.elapsedRealtimeNanos
            )
        if (!traceConfig.transitionsTrace.allowNoChange) {
            require(trace.entries.isNotEmpty()) { "Transitions trace was empty..." }
        }
        return trace
    }

    private fun buildTaggedStates(): Map<String, List<StateDump>> {
        val taggedStates = mutableMapOf<String, List<StateDump>>()
        for ((tag, states) in taggedStateBuilders.entries) {
            val taggedStatesList = mutableListOf<StateDump>()
            taggedStates[tag] = taggedStatesList
            for (state in states) {
                val wmDumpData = this.artifacts.getFileBytes(state.wmDumpFileName)
                val layersDumpData = this.artifacts.getFileBytes(state.layersDumpFileName)
                val deviceState =
                    DeviceDumpParser.fromDump(
                        wmDumpData,
                        layersDumpData,
                        clearCacheAfterParsing = false
                    )

                val wmStateSubject =
                    WindowManagerTraceSubject.assertThat(deviceState.wmState.asTrace()).first()
                val layersStateSubject =
                    LayersTraceSubject.assertThat(deviceState.layerState.asTrace()).first()
                taggedStatesList.add(StateDump(wmStateSubject, layersStateSubject))
            }
        }

        require(taggedStates[AssertionTag.START] == null) { "START tag is reserved" }
        taggedStates[AssertionTag.START] = listOf(buildStartState())
        require(taggedStates[AssertionTag.END] == null) { "END tag is reserved" }
        taggedStates[AssertionTag.END] = listOf(buildEndState())

        return taggedStates
    }

    private fun buildStartState(): StateDump {
        return StateDump(buildWmTraceSubject()?.first(), buildLayersTraceSubject()?.first())
    }

    private fun buildEndState(): StateDump {
        return StateDump(buildWmTraceSubject()?.last(), buildLayersTraceSubject()?.last())
    }

    /** @return a transitions trace for the part of the trace we want to run the assertions on. */
    private fun buildEventLog(): EventLogSubject? {
        val eventLog = eventLog ?: return null
        return EventLogSubject.assertThat(eventLog)
            .split(transitionStartTime.unixTimeNanos, transitionEndTime.unixTimeNanos)
    }

    private fun buildWmTraceSubject(): WindowManagerTraceSubject? {
        val wmTrace = buildWmTrace() ?: return null
        return WindowManagerTraceSubject.assertThat(wmTrace)
    }

    private fun buildLayersTraceSubject(): LayersTraceSubject? {
        val layersTrace = buildLayersTrace() ?: return null
        return LayersTraceSubject.assertThat(layersTrace)
    }

    fun lock() {
        this.artifacts.lock()
    }

    fun clearFromMemory() {
        wmTraceSubject = null
        layersTraceSubject = null
        eventLogSubject = null
        transitionsTrace = null
        taggedStates = null
    }

    private fun minimumTraceEntriesForConfig(config: TraceConfig): Int {
        return if (config.allowNoChange) 1 else 2
    }

    fun notifyTransitionStarting() {
        this.transitionStartTime =
            FlickerRunResult.TraceTime(
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos(),
                systemTime = SystemClock.uptimeNanos(),
                unixTimeNanos =
                    TimeUnit.NANOSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            )
    }

    fun notifyTransitionEnded() {
        this.transitionEndTime =
            FlickerRunResult.TraceTime(
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos(),
                systemTime = SystemClock.uptimeNanos(),
                unixTimeNanos =
                    TimeUnit.NANOSECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            )
    }

    interface IResultSetter {
        fun setResult(result: FlickerRunResult)
    }

    companion object {
        enum class RunStatus(val prefix: String = "", val isFailure: Boolean) {
            UNDEFINED("???", false),
            ASSERTION_SUCCESS("PASS", false),
            RUN_FAILED("FAILED_RUN", true),
            PARSING_FAILURE("FAILED_PARSING", true),
            ASSERTION_FAILED("FAIL", true);

            companion object {
                fun merge(runStatuses: List<RunStatus>): RunStatus {
                    val precedence = listOf(ASSERTION_FAILED, RUN_FAILED, ASSERTION_SUCCESS)
                    for (status in precedence) {
                        if (runStatuses.any { it == status }) {
                            return status
                        }
                    }

                    return UNDEFINED
                }
            }
        }
    }

    data class StateDump(
        val wmState: WindowManagerStateSubject?,
        val layersState: LayerTraceEntrySubject?
    )
}
