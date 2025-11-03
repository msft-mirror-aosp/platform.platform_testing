/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.platform.test.rule

import android.util.Log
import androidx.benchmark.junit4.PerfettoTraceRule
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.traceprocessor.ExperimentalTraceProcessorApi
import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A JUnit rule for capturing instantaneous Perfetto traces with Java Heap dumps before and/or after
 * a test method run.
 *
 * This rule uses the [PerfettoTraceRule] to capture the required perfetto traces. Each of these
 * traces will show up as a new artifact in the test run.
 *
 * The rule can be enabled via test arguments using the key "java-heap-collector-enabled". By
 * default when this rule is enabled, java heap dumps are collected before and after a test run. Use
 * test arguments to enable or disable before and/or after test trace collection using the keys
 * "collect-before-test" and "collect-after-test" respectively.
 */
@ExperimentalPerfettoCaptureApi
@ExperimentalTraceProcessorApi
class TestJavaHeapTraceCollectorRule : TestWatcher() {

    // Two trace objects for each trace to be captured.
    private lateinit var capturedTraceBeforeTest: PerfettoTrace
    private lateinit var capturedTraceAfterTest: PerfettoTrace

    private val perfettoTraceConfig = generateConfig()

    override fun apply(base: Statement, description: Description): Statement {
        if (readTestArgument(JAVA_HEAP_COLLECTOR_OVERRIDE_KEY)) {
            return captureJavaHeapTraces(base, description)
        }
        return base
    }

    private fun captureJavaHeapTraces(base: Statement, description: Description): Statement {
        val testName = "${description.className}_${description.methodName}"

        val emptyStatement: Statement =
            object : Statement() {
                @kotlin.Throws(Throwable::class)
                public override fun evaluate() {
                    // The evaluate() call on this object will complete without any action.
                }
            }

        // Two trace rules to capture perfetto traces before and after the test run.
        val perfettoTraceBeforeTestRule =
            PerfettoTraceRule(
                /* config= */ PerfettoConfig.Text(perfettoTraceConfig),
                /* enableAppTagTracing= */ false,
                labelProvider = { description -> "${JAVA_HEAP_BEFORE_TEST_LABEL}_$testName" },
            ) { trace ->
                capturedTraceBeforeTest = trace
                Log.i(TAG, "Java Heap trace captured before test: ${trace.path}")
            }

        val perfettoTraceAfterTestRule =
            PerfettoTraceRule(
                /* config= */ PerfettoConfig.Text(perfettoTraceConfig),
                /* enableAppTagTracing= */ false,
                labelProvider = { description -> "${JAVA_HEAP_AFTER_TEST_LABEL}_$testName" },
            ) { trace ->
                capturedTraceAfterTest = trace
                Log.i(TAG, "Java Heap trace captured after test: ${trace.path}")
            }

        // Statement starts a perfetto trace, executes the empty statement, and creates a trace
        // file.
        val perfettoStatementBeforeTest =
            perfettoTraceBeforeTestRule.apply(emptyStatement, description)

        // Statement starts a perfetto trace, executes the empty statement, and creates a trace
        // file.
        val perfettoStatementAfterTest =
            perfettoTraceAfterTestRule.apply(emptyStatement, description)

        val finalStatement: Statement =
            object : Statement() {
                @kotlin.Throws(Throwable::class)
                public override fun evaluate() {
                    // Capture an instantaneous trace with java before test method.
                    if (readTestArgument(JAVA_HEAP_BEFORE_TEST)) {
                        perfettoStatementBeforeTest.evaluate()
                    }
                    // Execute test method.
                    base.evaluate()
                    // Capture an instantaneous trace with java after test method.
                    if (readTestArgument(JAVA_HEAP_AFTER_TEST)) {
                        perfettoStatementAfterTest.evaluate()
                    }
                }
            }
        return super.apply(finalStatement, description)
    }

    /**
     * Reads a boolean test argument.
     *
     * @param key The key of the test argument.
     * @return The boolean value of the test argument. Defaults to `false` if the argument is not
     *   present.
     */
    private fun readTestArgument(key: String): Boolean {
        val args = InstrumentationRegistry.getArguments()
        val flag = args.getString(key, "false").toBoolean()
        if (flag) {
            Log.i(TAG, "$key is enabled via the test arguments.")
        }
        return flag
    }

    /**
     * Reads a string test argument with value as comma separated list of processes to be traced.
     *
     * @return The comma separated string containing names of all processes to be traced. Defaults
     *   to `com.android.systemui` if the argument is not present.
     */
    private fun getProcessNames(): String {
        val args = InstrumentationRegistry.getArguments()
        val processNames =
            args.getString(JAVA_HEAP_PROCESSES_TO_TRACK, "com.android.systemui").toString()
        return processNames
    }

    /**
     * Generates a multi-line string containing 'process_cmdline' entries from a comma-separated
     * string.
     *
     * @return A string containing the formatted process_cmdline lines.
     */
    fun generateProcessCmdlines(): String {
        val processList = getProcessNames().split(',').filter { it.isNotBlank() }.map { it.trim() }

        return buildString {
                processList.forEach { processName ->
                    append("""process_cmdline: "$processName"""")
                    appendLine()
                }
            }
            .toString()
    }

    /** Generates a perfetto config to capture java heap trace for given processes. */
    private fun generateConfig(): String {
        val processCmdlinesBlock = generateProcessCmdlines()
        return """
        buffers {
          size_kb: 256000
          fill_policy: DISCARD
        }

        data_sources {
          config {
            name: "android.java_hprof"
            java_hprof_config {
$processCmdlinesBlock
              dump_smaps: true
            }
          }
        }

        data_sources: {
          config {
            name: "linux.process_stats"
            target_buffer: 0
            process_stats_config {
              scan_all_processes_on_start: true
            }
          }
        }
    """
    }

    companion object {
        private const val TAG = "TestJavaHeapTraceCollectorRule"
        private const val JAVA_HEAP_COLLECTOR_OVERRIDE_KEY = "java-heap-collector-enabled"
        private const val JAVA_HEAP_PROCESSES_TO_TRACK = "process-names-to-trace"
        private const val JAVA_HEAP_BEFORE_TEST = "collect-before-test"
        private const val JAVA_HEAP_AFTER_TEST = "collect-after-test"
        private const val JAVA_HEAP_BEFORE_TEST_LABEL = "before_test_java_heap"
        private const val JAVA_HEAP_AFTER_TEST_LABEL = "after_test_java_heap"
    }
}
