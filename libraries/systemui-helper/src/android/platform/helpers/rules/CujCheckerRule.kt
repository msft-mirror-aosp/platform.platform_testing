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

package android.platform.helpers.rules

import android.os.Build
import android.platform.test.rule.ArtifactSaver.artifactFile
import android.platform.test.rule.getLowestAncestorClassAnnotation
import android.tools.traces.parsers.perfetto.ShellServerLifecycleManager
import android.util.Log
import androidx.benchmark.junit4.PerfettoTraceRule
import androidx.benchmark.macro.runServer
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.traceprocessor.ExperimentalTraceProcessorApi
import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.platform.app.InstrumentationRegistry
import com.android.app.tracing.traceSection
import com.android.internal.jank.Cuj
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Annotation used to pass the expected CUJ IDs to CujCheckerRule.
 *
 * Example usage:
 * ```
 * @ExpectedCUJs({Cuj.CUJ_NOTIFICATION_ADD, Cuj.CUJ_NOTIFICATION_REMOVE})
 * @Test
 * fun bigPictureStyleNotificationTest() {
 *     // ... Test code ...
 * }
 * ```
 *
 * @param value An array of [com.android.internal.jank.Cuj.CujType] (CUJs) expected to be present in
 *   the trace.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class ExpectedCUJs(val value: IntArray)

/**
 * Annotation used to pass the expected CUJ names(optionally with tags) to CujCheckerRule.
 *
 * Example usage:
 * ```
 * @ExpectedCUJsByName({"LOCKSCREEN_TRANSITION_FROM_AOD::DEFAULT", "CUJ_NOTIFICATION_REMOVE"})
 * @Test
 * fun bigPictureStyleNotificationTest() {
 *     // ... Test code ...
 * }
 * ```
 *
 * @param value An array of [String] (CUJs) expected to be present in the trace.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class ExpectedCUJsByName(val value: Array<String>)

/**
 * A JUnit rule for verifying the presence of expected CUJs in a Perfetto trace.
 *
 * This rule uses the [PerfettoTraceRule] to capture a Perfetto trace during test execution and then
 * checks if the expected CUJs, as specified by the [@ExpectedCUJs] annotation, are present in the
 * trace.
 *
 * The rule can be enabled or disabled via the `enabledFromRuleConstructor` constructor parameter or
 * via test arguments using the key "cuj-checker-enabled". If the rule is disabled either way, trace
 * capturing and CUJ checking will be skipped.
 *
 * @param enabledFromRuleConstructor Whether the rule is enabled. Defaults to `false`.
 */
@ExperimentalPerfettoCaptureApi
@ExperimentalTraceProcessorApi
class CujCheckerRule(private val enabledFromRuleConstructor: Boolean = false) : TestWatcher() {

    private lateinit var testName: String
    private lateinit var capturedTrace: PerfettoTrace
    private val expectedCujsFromIds = mutableSetOf<String>()
    private val expectedCujsFromNames = mutableSetOf<String>()
    private val isCuttlefishDevice: Boolean = Build.MODEL.contains("Cuttlefish")

    private val perfettoTraceRule =
        PerfettoTraceRule(
            /* config= */ PerfettoConfig.Text(CUJ_CHECKER_PERFETTO_CONFIG),
            /* enableAppTagTracing= */ false,
        ) { trace ->
            capturedTrace = trace
            Log.d(TAG, "Trace captured: ${trace.path}")
        }

    override fun apply(base: Statement, description: Description): Statement {
        if (!isEnabled()) return base

        expectedCujsFromIds.addAll(
            collectAnnotationValues(description, ExpectedCUJs::class.java) { annotation ->
                annotation.value.asIterable().map { Cuj.getNameOfCuj(it) }
            }
        )

        expectedCujsFromNames.addAll(
            collectAnnotationValues(description, ExpectedCUJsByName::class.java) { annotation ->
                annotation.value.toList()
            }
        )

        testName = description.className
        val perfettoStatement = perfettoTraceRule.apply(base, description)
        return super.apply(perfettoStatement, description)
    }

    override fun succeeded(description: Description?) {
        if (!::capturedTrace.isInitialized) {
            Log.w(TAG, "Trace not present. Skipping CUJ check.")
        } else {
            traceSection(TAG) {
                saveTraceArtifactIfRequestedForDebugging()
                processTraceAndCheckCujs()
            }
        }
        super.succeeded(description)
    }

    /**
     * Processes the trace and checks if the expected CUJs are present.
     *
     * @throws AssertionError if the expected CUJs are not present in the trace.
     */
    private fun processTraceAndCheckCujs() {
        Log.d(TAG, "Processing trace")
        TraceProcessor.runServer(
            // This implementation allows usage of the platform trace processor shell.
            serverLifecycleManager = ShellServerLifecycleManager(),
            eventCallback =
                object : TraceProcessor.EventCallback {
                    override fun onLoadTraceFailure(trace: PerfettoTrace, throwable: Throwable) {
                        Log.e(TAG, "Unable to load trace", throwable)
                        throw throwable
                    }
                },
            tracer = TraceProcessor.Tracer(),
        ) {
            loadTrace(capturedTrace) {
                if (!hasPacketLossErrors(this)) {
                    val cujsInTrace = extractCujsFromTrace(this)

                    // In case of comparing with expected CUJs from Ids,
                    // ignore tag(if any) after the '::' separator.
                    assertWithMessage("Missing CUJs from IDs in test: $testName")
                        .that(cujsInTrace.map { cuj -> cuj.split("::")[0] })
                        .containsAtLeastElementsIn(expectedCujsFromIds)
                    // In case of comparing with expected CUJs from names,
                    // consider the full CUJ name along with the tag(if any).
                    assertWithMessage("Missing named CUJs in test: $testName")
                        .that(cujsInTrace)
                        .containsAtLeastElementsIn(expectedCujsFromNames)
                } else {
                    Log.e(TAG, "Skipping CUJ check due to incomplete trace.")
                }
            }
        }
    }

    /**
     * Checks if there are packet loss errors in the trace, due to which the trace may be
     * incomplete.
     *
     * @param session The trace processor session.
     * @return `true` if there are packet loss errors, `false` otherwise.
     */
    private fun hasPacketLossErrors(session: TraceProcessor.Session): Boolean {
        // TODO: b/402679603 - Update to track skip cuj check due to bad trace.
        val errorsList =
            session
                .query(
                    """
                    select name from stats where
                    (severity = 'error' or severity = 'data_loss')
                    and value != 0;
                    """
                        .trimIndent()
                )
                .toList()
        if (errorsList.isNotEmpty()) {
            Log.w(TAG, "Incomplete trace errors: $errorsList")
            return true
        }
        return false
    }

    /**
     * Extracts the CUJs from the trace.
     *
     * @param session The trace processor session.
     * @return A set of CUJ names.
     */
    private fun extractCujsFromTrace(session: TraceProcessor.Session): Set<String> {
        return session
            .query(
                """
                    INCLUDE PERFETTO MODULE android.cujs.sysui_cujs;
                    SELECT cuj_name FROM android_jank_latency_cujs;
                """
                    .trimIndent()
            )
            .map { row -> row.string("cuj_name") }
            .toSet()
    }

    private fun saveTraceArtifactIfRequestedForDebugging() {
        if (readTestArgument(CUJ_CHECKER_SAVE_TRACE_ARTIFACT_KEY)) {
            saveTraceArtifact(capturedTrace, testName)
        }
    }

    private fun saveTraceArtifact(trace: PerfettoTrace, testName: String) {
        try {
            val traceFile = File(trace.path)
            val artifact =
                artifactFile(
                    "${testName}-cuj-checker-trace-${System.currentTimeMillis()}.perfetto-trace"
                )
            Files.copy(traceFile.toPath(), artifact.toPath(), StandardCopyOption.REPLACE_EXISTING)
            Log.d(TAG, "Trace saved to: ${artifact.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error while saving or validating trace file", e)
        }
    }

    private fun isEnabled(): Boolean =
        (enabledFromRuleConstructor || readTestArgument(CUJ_CHECKER_OVERRIDE_KEY)) &&
            !isCuttlefishDevice

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
            Log.d(TAG, "$key is enabled via the test arguments.")
        }
        return flag
    }

    /**
     * Finds all annotations of type T on the method and class hierarchy, extracts values using the
     * [transform] function, and returns a merged list.
     *
     * @param description The input description to the rule.
     * @param annotationClass The annotation for which the contents should be fetched.
     * @param transform lambda to extract values from annotation based on type(eg. Int, String).
     * @return The merged list of annotation values found for the method or class.
     */
    private fun <T : Annotation, V> collectAnnotationValues(
        description: Description,
        annotationClass: Class<T>,
        transform: (T) -> Iterable<V>,
    ): MutableSet<V> {
        val collectedValues = mutableSetOf<V>()

        // Helper to safely extract and add values to list.
        fun extractAndAdd(annotation: T?) {
            if (annotation != null) {
                collectedValues.addAll(transform(annotation))
            }
        }

        // Check for annotated method.
        extractAndAdd(description.getAnnotation(annotationClass))

        // Check Class Hierarchy for annotation.
        extractAndAdd(getLowestAncestorClassAnnotation(description.testClass, annotationClass))

        // Return list of distinct values within annotation.
        return collectedValues.distinct().toMutableSet()
    }

    companion object {
        private const val TAG = "CujCheckerRule"
        private const val CUJ_CHECKER_OVERRIDE_KEY = "cuj-checker-enabled"
        private const val CUJ_CHECKER_SAVE_TRACE_ARTIFACT_KEY = "cuj-checker-save-trace-artifact"
        private const val CUJ_CHECKER_PERFETTO_CONFIG =
            """
            buffers: {
                size_kb: 32768
                fill_policy: RING_BUFFER
            }
            # Buffer 1: For process_stats
            buffers: {
                size_kb: 2048
                fill_policy: RING_BUFFER
            }
            data_sources: {
                config {
                    name: "linux.process_stats"
                    target_buffer: 1
                    process_stats_config {
                        scan_all_processes_on_start: true
                    }
                }
            }
            data_sources: {
                config {
                    name: "android.surfaceflinger.frametimeline"
                    target_buffer: 0
                }
            }
            data_sources: {
                config {
                    name: "linux.ftrace"
                    target_buffer: 0
                    ftrace_config {
                        buffer_size_kb: 16384
                        drain_period_ms: 250
                        atrace_categories: "input"
                        atrace_categories: "view"
                        atrace_categories: "wm"
                        atrace_apps: "com.android.systemui,com.google.android.apps.nexuslauncher"
                    }
                }
            }
            write_into_file: true
            file_write_period_ms: 1000
            flush_period_ms: 10000
        """
    }
}
