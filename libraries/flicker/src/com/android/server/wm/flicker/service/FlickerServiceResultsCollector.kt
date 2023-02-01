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

package com.android.server.wm.flicker.service

import android.app.Instrumentation
import android.device.collectors.BaseMetricListener
import android.device.collectors.DataRecord
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.android.internal.annotations.VisibleForTesting
import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.flicker.runner.ExecutionError
import com.android.server.wm.flicker.service.assertors.AssertionResult
import com.android.server.wm.traces.common.service.AssertionInvocationGroup
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure

/**
 * Collects all the Flicker Service's metrics which are then uploaded for analysis and monitoring to
 * the CrystalBall database.
 */
class FlickerServiceResultsCollector(
    private val tracesCollector: ITracesCollector,
    private val flickerService: IFlickerService = FlickerService(),
    instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
    private val collectMetricsPerTest: Boolean = true,
    private val reportOnlyForPassingTests: Boolean = true
) : BaseMetricListener(), IFlickerServiceResultsCollector {
    private var hasFailedTest = false
    private var testSkipped = false

    private val _executionErrors = mutableListOf<ExecutionError>()
    override val executionErrors: List<ExecutionError>
        get() = _executionErrors

    @VisibleForTesting val assertionResults = mutableListOf<AssertionResult>()
    @VisibleForTesting
    val assertionResultsByTest = mutableMapOf<Description, List<AssertionResult>>()

    init {
        setInstrumentation(instrumentation)
    }

    override fun onTestRunStart(runData: DataRecord, description: Description) {
        errorReportingBlock {
            Log.i(LOG_TAG, "onTestRunStart :: collectMetricsPerTest = $collectMetricsPerTest")
            if (!collectMetricsPerTest) {
                hasFailedTest = false
                tracesCollector.start()
            }
        }
    }

    override fun onTestStart(testData: DataRecord, description: Description) {
        errorReportingBlock {
            Log.i(LOG_TAG, "onTestStart :: collectMetricsPerTest = $collectMetricsPerTest")
            if (collectMetricsPerTest) {
                hasFailedTest = false
                tracesCollector.start()
            }
            testSkipped = false
        }
    }

    override fun onTestFail(testData: DataRecord, description: Description, failure: Failure) {
        errorReportingBlock {
            Log.i(LOG_TAG, "onTestFail")
            hasFailedTest = true
        }
    }

    override fun testSkipped(description: Description) {
        errorReportingBlock {
            Log.i(LOG_TAG, "testSkipped")
            testSkipped = true
        }
    }

    override fun onTestEnd(testData: DataRecord, description: Description) {
        errorReportingBlock {
            Log.i(LOG_TAG, "onTestEnd :: collectMetricsPerTest = $collectMetricsPerTest")
            if (collectMetricsPerTest && !testSkipped) {
                stopTracingAndCollectFlickerMetrics(testData, description)
            }
        }
    }

    override fun onTestRunEnd(runData: DataRecord, result: Result) {
        errorReportingBlock {
            Log.i(LOG_TAG, "onTestRunEnd :: collectMetricsPerTest = $collectMetricsPerTest")
            if (!collectMetricsPerTest) {
                stopTracingAndCollectFlickerMetrics(runData)
            }
        }
    }

    private fun stopTracingAndCollectFlickerMetrics(
        dataRecord: DataRecord,
        description: Description? = null
    ) {
        Log.i(LOG_TAG, "Stopping trace collection")
        tracesCollector.stop()
        Log.i(LOG_TAG, "Stopped trace collection")
        if (reportOnlyForPassingTests && hasFailedTest) {
            return
        }

        val reader = tracesCollector.getResultReader()
        dataRecord.addStringMetric(WINSCOPE_FILE_PATH_KEY, reader.artifactPath.toString())
        Log.i(LOG_TAG, "Processing traces")
        val results = flickerService.process(reader)
        Log.i(LOG_TAG, "Got ${results.size} results")
        assertionResults.addAll(results)
        if (description != null) {
            require(assertionResultsByTest[description] == null) {
                "Test description already contains flicker assertion results."
            }
            assertionResultsByTest[description] = results
        }
        val aggregatedResults = processFlickerResults(results)
        collectMetrics(dataRecord, aggregatedResults)
    }

    private fun processFlickerResults(
        results: List<AssertionResult>
    ): Map<String, AggregatedFlickerResult> {
        val aggregatedResults = mutableMapOf<String, AggregatedFlickerResult>()
        for (result in results) {
            val key = getKeyForAssertionResult(result)
            if (!aggregatedResults.containsKey(key)) {
                aggregatedResults[key] = AggregatedFlickerResult()
            }
            aggregatedResults[key]!!.addResult(result)
        }
        return aggregatedResults
    }

    private fun collectMetrics(
        data: DataRecord,
        aggregatedResults: Map<String, AggregatedFlickerResult>
    ) {
        val it = aggregatedResults.entries.iterator()

        while (it.hasNext()) {
            val (key, result) = it.next()
            Log.v(LOG_TAG, "Adding metric ${key}_FAILURES = ${result.failures}")
            data.addStringMetric("${key}_FAILURES", "${result.failures}")
        }
    }

    private fun getKeyForAssertionResult(result: AssertionResult): String {
        val assertionName = "${result.scenario}#${result.assertionName}"
        return "$FAAS_METRICS_PREFIX::$assertionName"
    }

    private fun errorReportingBlock(function: () -> Unit) {
        try {
            function()
        } catch (e: Throwable) {
            Log.e(FLICKER_TAG, "Error executing in FlickerServiceResultsCollector", e)
            _executionErrors.add(ExecutionError(e))
        }
    }

    override fun testContainsFlicker(description: Description): Boolean {
        val resultsForTest = resultsForTest(description)
        return resultsForTest.any { it.failed }
    }

    override fun resultsForTest(description: Description): List<AssertionResult> {
        val resultsForTest = assertionResultsByTest[description]
        requireNotNull(resultsForTest) { "No results set for test $description" }
        return resultsForTest
    }

    companion object {
        // Unique prefix to add to all FaaS metrics to identify them
        private const val FAAS_METRICS_PREFIX = "FAAS"
        private const val LOG_TAG = "$FLICKER_TAG-Collector"
        private const val WINSCOPE_FILE_PATH_KEY = "winscope_file_path"

        class AggregatedFlickerResult {
            var failures = 0
            var passes = 0
            val errors = mutableListOf<String>()
            var invocationGroup: AssertionInvocationGroup? = null

            fun addResult(result: AssertionResult) {
                if (result.failed) {
                    failures++
                    errors.add(result.assertionError?.message ?: "FAILURE WITHOUT ERROR MESSAGE...")
                } else {
                    passes++
                }

                if (invocationGroup == null) {
                    invocationGroup = result.invocationGroup
                }

                if (invocationGroup != result.invocationGroup) {
                    error("Unexpected assertion group mismatch")
                }
            }
        }
    }
}
