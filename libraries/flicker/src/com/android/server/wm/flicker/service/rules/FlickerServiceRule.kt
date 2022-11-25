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

package com.android.server.wm.flicker.service.rules

import android.platform.test.rule.TestWatcher
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.service.FlickerServiceResultsCollector
import com.android.server.wm.flicker.service.FlickerServiceTracesCollector
import com.android.server.wm.flicker.service.IFlickerServiceResultsCollector
import org.junit.AssumptionViolatedException
import org.junit.runner.Description
import org.junit.runner.notification.Failure

/**
 * A test rule that runs Flicker as a Service on the tests this rule is applied to.
 *
 * Note there are performance implications to using this test rule in tests. Tracing will be enabled
 * during the test which will slow down everything. So if the test is performance critical then an
 * alternative should be used.
 *
 * @see TODO for examples on how to use this test rule in your own tests
 */
open class FlickerServiceRule
@JvmOverloads
constructor(
    private val metricsCollector: IFlickerServiceResultsCollector =
        FlickerServiceResultsCollector(
            tracesCollector = FlickerServiceTracesCollector(getDefaultFlickerOutputDir()),
            instrumentation = InstrumentationRegistry.getInstrumentation()
        ),
    // null parses to false (so defaults to not failing tests)
    private val failTestOnFaasFailure: Boolean =
        InstrumentationRegistry.getArguments().getString("faas:blocking").toBoolean()
) : TestWatcher() {

    /** Invoked when a test is about to start */
    public override fun starting(description: Description) {
        Log.i(LOG_TAG, "Test starting $description")
        metricsCollector.testStarted(description)
    }

    /** Invoked when a test succeeds */
    public override fun succeeded(description: Description) {
        Log.i(LOG_TAG, "Test succeeded $description")
    }

    /** Invoked when a test fails */
    public override fun failed(e: Throwable?, description: Description) {
        Log.e(LOG_TAG, "$description test failed  with $e")
        metricsCollector.testFailure(Failure(description, e))
    }

    /** Invoked when a test is skipped due to a failed assumption. */
    public override fun skipped(e: AssumptionViolatedException, description: Description) {
        Log.i(LOG_TAG, "Test skipped $description with $e")
        metricsCollector.testSkipped(description)
    }

    /** Invoked when a test method finishes (whether passing or failing) */
    public override fun finished(description: Description) {
        Log.i(LOG_TAG, "Test finished $description")
        metricsCollector.testFinished(description)
        if (metricsCollector.executionErrors.isNotEmpty()) {
            for (executionError in metricsCollector.executionErrors) {
                Log.e(LOG_TAG, "FaaS reported execution errors", executionError)
            }
            throw metricsCollector.executionErrors[0]
        }
        if (failTestOnFaasFailure && metricsCollector.testContainsFlicker(description)) {
            throw metricsCollector.resultsForTest(description).first { it.failed }.assertionError
                ?: error("Unexpectedly missing assertion error")
        }
    }

    companion object {
        val LOG_TAG = "FlickerServiceRule"
    }
}
