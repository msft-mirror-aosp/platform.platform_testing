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

import android.util.Log
import com.android.server.wm.flicker.FlickerRunResult.Companion.RunStatus
import com.android.server.wm.flicker.monitor.IFileGeneratingMonitor
import com.android.server.wm.flicker.monitor.ITransitionMonitor
import com.android.server.wm.flicker.monitor.NoTraceMonitor
import com.android.server.wm.traces.common.ConditionList
import com.android.server.wm.traces.common.WindowManagerConditionsFactory
import com.android.server.wm.traces.parser.getCurrentState
import java.io.IOException
import java.nio.file.Files
import org.junit.runner.Description

/**
 * Runner to execute the transitions of a flicker test
 *
 * The commands are executed in the following order:
 * 1) [Flicker.transitionSetup]
 * 2) Start monitors
 * 3) [Flicker.transitions]
 * 4) Stop monitors
 * 5) [Flicker.transitionTeardown]
 *
 * If the tests were already executed, reuse the previous results
 *
 */
open class TransitionRunner {
    private val tags = mutableSetOf<String>()
    private var _result: FlickerRunResult? = null
    private val result: FlickerRunResult get() {
        requireNotNull(_result) { "Result not initialized" }
        return _result!!
    }

    /**
     * Executes the setup, transitions and teardown defined in [flicker]
     *
     * @param flicker test specification
     * @throws IllegalArgumentException If the transitions are empty
     */
    open fun execute(flicker: Flicker, useCacheIfAvailable: Boolean = true): FlickerResult {
        check(flicker)
        return run(flicker)
    }

    /**
     * Validate the [flicker] test specification before executing the transitions
     *
     * @param flicker test specification
     * @throws IllegalArgumentException If the transitions are empty
     */
    protected fun check(flicker: Flicker) {
        require(flicker.transitions.isNotEmpty() || onlyHasNoTraceMonitors(flicker)) {
            "A flicker test must include transitions to run"
        }
    }

    private fun onlyHasNoTraceMonitors(flicker: Flicker) =
            flicker.traceMonitors.all { it is NoTraceMonitor }

    open fun cleanUp() {
        tags.clear()
        _result = null
    }

    /**
     * Runs the actual setup, transitions and teardown defined in [flicker]
     *
     * @param flicker test specification
     */
    internal open fun run(flicker: Flicker): FlickerResult {
        val runResult = FlickerRunResult(flicker.testName)
        _result = runResult
        safeExecution(flicker) {
            val description = Description.createSuiteDescription(flicker.testName)
            if (flicker.faasEnabled) {
                Log.d(FLICKER_TAG, "${flicker.testName} - Setting up FaaS")
                flicker.faas.testStarted(description)
            }
            Log.d(FLICKER_TAG, "${flicker.testName} - Running transition setup")
            runTransitionSetup(flicker)
            Log.d(FLICKER_TAG, "${flicker.testName} - Running transition")
            runTransition(flicker)
            Log.d(FLICKER_TAG, "${flicker.testName} - Running transition teardown")
            runTransitionTeardown(flicker)
            Log.d(FLICKER_TAG, "${flicker.testName} - Processing transition traces")
            processRunTraces(flicker, RunStatus.ASSERTION_SUCCESS)
            if (flicker.faasEnabled) {
                Log.d(FLICKER_TAG, "${flicker.testName} - " +
                        "Notifying FaaS of finished transition")
                flicker.faas.testFinished(description)
                if (flicker.faas.executionErrors.isNotEmpty()) {
                    runResult.setExecutionError(flicker.faas.executionErrors[0])
                    for (executionError in flicker.faas.executionErrors) {
                        Log.e(FLICKER_TAG, "FaaS reported execution errors", executionError)
                    }
                }
            }
        }

        val result = FlickerResult(runResult)
        cleanUp()
        return result
    }

    private fun safeExecution(
        flicker: Flicker,
        alreadyFailed: Boolean = false,
        execution: () -> Unit
    ) {
        try {
            execution()
        } catch (e: ExecutionError) {
            Log.e(FLICKER_TAG, "A Flicker Execution Error occurred!", e)
            if (alreadyFailed) {
                // If we already failed don't try and handle failures since we are most likely
                // failing because of the previous execution error.
                return
            }

            result.setExecutionError(e)
            result.setStatus(RunStatus.RUN_FAILED)

            when (e) {
                is TransitionSetupFailure -> {
                    // If we fail on the setup we simply want to run the teardown.
                    safeExecution(flicker, true) {
                        runTransitionTeardown(flicker)
                    }
                }
                is TransitionExecutionFailure -> {
                    // If a transition fails to run we want to store the traces for the transition
                    // up to the point the failure occurred and then try and run the teardown.
                    flicker.traceMonitors.forEach { it.tryStop() }
                    safeExecution(flicker, true) {
                        runTransitionTeardown(flicker)
                        processRunTraces(flicker, RunStatus.RUN_FAILED)
                    }
                }
                is TransitionTeardownFailure -> {
                    // If the teardown failure we still want to try and process the traces
                    safeExecution(flicker, true) {
                        processRunTraces(flicker, RunStatus.RUN_FAILED)
                    }
                }
                is TraceProcessingFailure -> {
                    // Nothing to do & considered handled
                }
                else -> {
                    throw e
                }
            }
        }
    }

    /**
     * Parses the traces collected by the monitors to generate FlickerRunResults containing the
     * parsed trace and information about the status of the run.
     * The run results are added to the resultBuilders list which is then used to run Flicker
     * assertions on.
     */
    @Throws(TraceProcessingFailure::class)
    private fun processRunTraces(
        flicker: Flicker,
        status: RunStatus
    ) {
        try {
            result.setStatus(status)
            setMonitorResults(flicker, result)
            result.lock()

            if (flicker.faasEnabled && !status.isFailure) {
                // Don't run FaaS on failed transitions
                val wmTrace = result.buildWmTrace()
                val layersTrace = result.buildLayersTrace()
                val transitionsTrace = result.buildTransitionsTrace()

                flicker.faasTracesCollector.wmTrace = wmTrace
                flicker.faasTracesCollector.layersTrace = layersTrace
                flicker.faasTracesCollector.transitionsTrace = transitionsTrace
            }
        } catch (e: Throwable) {
            // We have failed to add the results to the runs, so we can effectively consider these
            // results as "lost" as they won't be used from now forth. So we can safely rename
            // to file to indicate the failure and make it easier to find in the archives.
            flicker.traceMonitors.forEach {
                // All monitors that generate files we want to keep in the archives should implement
                // IFileGeneratingMonitor
                if (it is IFileGeneratingMonitor) {
                    Utils.addStatusToFileName(it.outputFile, RunStatus.PARSING_FAILURE)
                }
            }
            throw TraceProcessingFailure(e)
        }
    }

    @Throws(TransitionSetupFailure::class)
    private fun runTransitionSetup(flicker: Flicker) {
        try {
            flicker.transitionSetup.forEach { it.invoke(flicker) }
            flicker.wmHelper.StateSyncBuilder()
                .add(UI_STABLE_CONDITIONS)
                .waitFor()
        } catch (e: Throwable) {
            throw TransitionSetupFailure(e)
        }
    }

    @Throws(TransitionExecutionFailure::class)
    private fun runTransition(flicker: Flicker) {
        try {
            flicker.traceMonitors.forEach { it.start() }
            flicker.transitions.forEach { it.invoke(flicker) }
            flicker.wmHelper.StateSyncBuilder()
                    .add(UI_STABLE_CONDITIONS)
                    .waitFor()
            flicker.traceMonitors.forEach { it.tryStop() }
        } catch (e: Throwable) {
            throw TransitionExecutionFailure(e)
        }
    }

    @Throws(TransitionTeardownFailure::class)
    private fun runTransitionTeardown(flicker: Flicker) {
        try {
            flicker.transitionTeardown.forEach { it.invoke(flicker) }
        } catch (e: Throwable) {
            throw TransitionTeardownFailure(e)
        }
    }

    private fun setMonitorResults(
        flicker: Flicker,
        result: FlickerRunResult,
    ): FlickerRunResult {

        flicker.traceMonitors.forEach {
            result.setResultsFromMonitor(it)
        }

        return result
    }

    private fun ITransitionMonitor.tryStop() {
        this.run {
            try {
                stop()
            } catch (e: Exception) {
                Log.e(FLICKER_TAG, "Unable to stop $this", e)
            }
        }
    }

    private fun getTaggedFilePath(flicker: Flicker, tag: String, file: String) =
        "${flicker.testName}_${tag}_$file"

    /**
     * Captures a snapshot of the device state and associates it with a new tag.
     *
     * This tag can be used to make assertions about the state of the device when the
     * snapshot is collected.
     *
     * [tag] is used as part of the trace file name, thus, only valid letters and digits
     * can be used
     *
     * @param flicker test specification
     * @throws IllegalArgumentException If [tag] contains invalid characters
     */
    open fun createTag(flicker: Flicker, tag: String) {
        require(!tag.contains(" ")) {
            "The test tag $tag can not contain spaces since it is a part of the file name"
        }
        tags.add(tag)

        val deviceStateBytes = getCurrentState(flicker.instrumentation.uiAutomation)
        try {
            val wmDumpFile = flicker.outputDir.resolve(
                getTaggedFilePath(flicker, tag, "wm_dump")
            )
            Files.write(wmDumpFile, deviceStateBytes.first)

            val layersDumpFile = flicker.outputDir.resolve(
                getTaggedFilePath(flicker, tag, "layers_dump")
            )
            Files.write(layersDumpFile, deviceStateBytes.second)

            result.addTaggedState(
                tag,
                wmDumpFile.toFile(),
                layersDumpFile.toFile(),
            )
        } catch (e: IOException) {
            throw RuntimeException("Unable to create trace file: ${e.message}", e)
        }
    }

    companion object {
        /**
         * Conditions that determine when the UI is in a stable stable and no windows or layers are
         * animating or changing state.
         */
        private val UI_STABLE_CONDITIONS = ConditionList(
            listOf(
                WindowManagerConditionsFactory.isWMStateComplete(),
                WindowManagerConditionsFactory.hasLayersAnimating().negate()
            )
        )

        open class ExecutionError(private val inner: Throwable) : Throwable(inner) {
            init {
                super.setStackTrace(inner.stackTrace)
            }

            override val message: String?
                get() = inner.toString()
        }

        class TransitionSetupFailure(val e: Throwable) : ExecutionError(e)
        class TransitionExecutionFailure(val e: Throwable) : ExecutionError(e)
        class TraceProcessingFailure(val e: Throwable) : ExecutionError(e)
        class TransitionTeardownFailure(val e: Throwable) : ExecutionError(e)
    }
}
