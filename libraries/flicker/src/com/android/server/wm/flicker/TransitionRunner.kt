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
import com.android.server.wm.flicker.monitor.ITransitionMonitor
import com.android.server.wm.traces.common.ConditionList
import com.android.server.wm.traces.common.WindowManagerConditionsFactory
import com.android.server.wm.traces.parser.DeviceDumpParser
import com.android.server.wm.traces.parser.getCurrentState
import java.io.IOException
import java.nio.file.Files

/**
 * Runner to execute the transitions of a flicker test
 *
 * The commands are executed in the following order:
 * 1) [Flicker.testSetup]
 * 2) [Flicker.runSetup
 * 3) Start monitors
 * 4) [Flicker.transitions]
 * 5) Stop monitors
 * 6) [Flicker.runTeardown]
 * 7) [Flicker.testTeardown]
 *
 * If the tests were already executed, reuse the previous results
 *
 */
open class TransitionRunner {
    /**
     * Iteration identifier during test run
     */
    private var iteration = 0
    private val tags = mutableSetOf<String>()
    private var tagsResults = mutableListOf<FlickerRunResult>()

    /**
     * Executes the setup, transitions and teardown defined in [flicker]
     *
     * @param flicker test specification
     * @throws IllegalArgumentException If the transitions are empty or repetitions is set to 0
     */
    open fun execute(flicker: Flicker): FlickerResult {
        check(flicker)
        return run(flicker)
    }

    /**
     * Validate the [flicker] test specification before executing the transitions
     *
     * @param flicker test specification
     * @throws IllegalArgumentException If the transitions are empty or repetitions is set to 0
     */
    protected fun check(flicker: Flicker) {
        require(flicker.transitions.isNotEmpty()) {
            "A flicker test must include transitions to run" }
        require(flicker.repetitions > 0) {
            "Number of repetitions must be greater than 0" }
    }

    open fun cleanUp() {
        tags.clear()
        tagsResults.clear()
    }

    /**
     * Runs the actual setup, transitions and teardown defined in [flicker]
     *
     * @param flicker test specification
     */
    internal open fun run(flicker: Flicker): FlickerResult {
        val runs = mutableListOf<FlickerRunResult>()
        val executionErrors = mutableListOf<Throwable>()
        safeExecution(flicker, runs, executionErrors) {
            runTestSetup(flicker)

            for (x in 0 until flicker.repetitions) {
                iteration = x
                runTransitionSetup(flicker)
                runTransition(flicker)
                runTransitionTeardown(flicker)
                saveRunResults(flicker, runs)
            }

            runTestTeardown(flicker)
        }

        runs.addAll(tagsResults)
        val result = FlickerResult(runs.toList(), tags.toSet(), executionErrors)
        cleanUp()
        return result
    }

    private fun safeExecution(
        flicker: Flicker,
        runs: MutableList<FlickerRunResult>,
        executionErrors: MutableList<Throwable>,
        execution: () -> Unit
    ) {
        try {
            execution()
        } catch (e: TestSetupFailure) {
            // If we failure on the test setup we can't run any of the transitions
            executionErrors.add(e)
        } catch (e: TransitionSetupFailure) {
            // If we fail on the transition run setup then we don't want to run any further
            // transitions nor save any results for this run. We simply want to run the test
            // teardown.
            executionErrors.add(e)
            flicker.traceMonitors.forEach { it.tryStop() }
            safeExecution(flicker, runs, executionErrors) {
                runTestTeardown(flicker)
            }
        } catch (e: TransitionExecutionFailure) {
            // If a transition fails to run we don't want to run the following iterations as the
            // device is likely in an unexpected state which would lead to further errors. We simply
            // want to run the test teardown
            executionErrors.add(e)
            flicker.traceMonitors.forEach { it.tryStop() }
            safeExecution(flicker, runs, executionErrors) {
                runTestTeardown(flicker)
            }
        } catch (e: SaveRunResultsFailure) {
            // If we fail to save the run results we still want to run the teardowns and report the
            // execution error.
            executionErrors.add(e)
            safeExecution(flicker, runs, executionErrors) {
                runTransitionTeardown(flicker)
                runTestTeardown(flicker)
            }
        } catch (e: TransitionTeardownFailure) {
            // If a transition teardown fails to run we don't want to run the following iterations
            // as the device is likely in an unexpected state which would lead to further errors.
            // But, we do want to run the test teardown.
            executionErrors.add(e)
            safeExecution(flicker, runs, executionErrors) {
                runTestTeardown(flicker)
            }
        } catch (e: TestTeardownFailure) {
            // If we fail in the execution of the test teardown there is nothing else to do apart
            // from reporting the execution error.
            executionErrors.add(e)
        }
    }

    @Throws(SaveRunResultsFailure::class)
    private fun saveRunResults(flicker: Flicker, runs: MutableList<FlickerRunResult>) {
        try {
            val runResults = saveResult(flicker, iteration)
            runs.addAll(runResults)
        } catch (e: Throwable) {
            throw SaveRunResultsFailure(e)
        }
    }

    @Throws(TestSetupFailure::class)
    private fun runTestSetup(flicker: Flicker) {
        try {
            flicker.testSetup.forEach { it.invoke(flicker) }
        } catch (e: Throwable) {
            throw TestSetupFailure(e)
        }
    }

    @Throws(TestTeardownFailure::class)
    private fun runTestTeardown(flicker: Flicker) {
        try {
            flicker.testTeardown.forEach { it.invoke(flicker) }
        } catch (e: Throwable) {
            throw TestTeardownFailure(e)
        }
    }

    @Throws(TransitionSetupFailure::class)
    private fun runTransitionSetup(flicker: Flicker) {
        try {
            flicker.runSetup.forEach { it.invoke(flicker) }
            flicker.wmHelper.waitFor(UI_STABLE_CONDITIONS)
        } catch (e: Throwable) {
            throw TransitionSetupFailure(e)
        }
    }

    @Throws(TransitionExecutionFailure::class)
    private fun runTransition(flicker: Flicker) {
        try {
            flicker.traceMonitors.forEach { it.start() }
            flicker.transitions.forEach { it.invoke(flicker) }
        } catch (e: Throwable) {
            throw TransitionExecutionFailure(e)
        }
    }

    @Throws(TransitionTeardownFailure::class)
    private fun runTransitionTeardown(flicker: Flicker) {
        try {
            flicker.wmHelper.waitFor(UI_STABLE_CONDITIONS)
            flicker.traceMonitors.forEach { it.tryStop() }
            flicker.runTeardown.forEach { it.invoke(flicker) }
        } catch (e: Throwable) {
            throw TransitionTeardownFailure(e)
        }
    }

    private fun saveResult(flicker: Flicker, iteration: Int): List<FlickerRunResult> {
        val resultBuilder = FlickerRunResult.Builder()
        flicker.traceMonitors.forEach {
            it.save(resultBuilder)
        }

        return resultBuilder.buildAll(flicker.testName, iteration)
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
        "${flicker.testName}_${iteration}_${tag}_$file"

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
        val deviceState = DeviceDumpParser.fromDump(deviceStateBytes.first, deviceStateBytes.second)
        try {
            val wmTraceFile = flicker.outputDir.resolve(
                getTaggedFilePath(flicker, tag, "wm_trace"))
            Files.write(wmTraceFile, deviceStateBytes.first)

            val layersTraceFile = flicker.outputDir.resolve(
                getTaggedFilePath(flicker, tag, "layers_trace"))
            Files.write(layersTraceFile, deviceStateBytes.second)

            val builder = FlickerRunResult.Builder()
            val result = builder.buildStateResult(
                tag,
                deviceState.wmState?.asTrace(),
                deviceState.layerState?.asTrace()
            )
            tagsResults.add(result)
        } catch (e: IOException) {
            throw RuntimeException("Unable to create trace file: ${e.message}", e)
        }
    }

    companion object {
        /**
         * Conditions that determine when the UI is in a stable stable and no windows or layers are
         * animating or changing state.
         */
        private val UI_STABLE_CONDITIONS = ConditionList(listOf(
                WindowManagerConditionsFactory.isWMStateComplete(),
                WindowManagerConditionsFactory.hasLayersAnimating().negate()
        ))

        class TestSetupFailure(val e: Throwable) : Throwable(e)
        class TransitionSetupFailure(val e: Throwable) : Throwable(e)
        class TransitionExecutionFailure(val e: Throwable) : Throwable(e)
        class SaveRunResultsFailure(val e: Throwable) : Throwable(e)
        class TransitionTeardownFailure(val e: Throwable) : Throwable(e)
        class TestTeardownFailure(val e: Throwable) : Throwable(e)
    }
}
