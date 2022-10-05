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
import com.android.server.wm.flicker.service.ITracesCollector
import java.nio.file.Path
import org.junit.AssumptionViolatedException
import org.junit.runner.Description

/**
 * A test rule that runs Flicker as a Service on the tests this rule is applied to.
 *
 * Note there are performance implications to using this test rule in tests. Tracing will be enabled
 * during the test which will slow down everything. So if the test is performance critical then an
 * alternative should be used.
 *
 * @see TODO for examples on how to use this test rule in your own tests
 */
class FlickerServiceRule
@JvmOverloads
constructor(
    outputDir: Path = getDefaultFlickerOutputDir(),
    tracesCollector: ITracesCollector = FlickerServiceTracesCollector(outputDir)
) : TestWatcher() {
    private val metricsCollector =
        FlickerServiceResultsCollector(
            outputDir,
            tracesCollector = tracesCollector,
            instrumentation = InstrumentationRegistry.getInstrumentation()
        )

    /** Invoked when a test is about to start */
    override fun starting(description: Description) {
        Log.i(LOG_TAG, "Test starting $description")
        metricsCollector.testStarted(description)
    }

    /** Invoked when a test method finishes (whether passing or failing) */
    override fun finished(description: Description) {
        Log.i(LOG_TAG, "Test finished $description")
        metricsCollector.testFinished(description)
    }

    /** Invoked when a test succeeds */
    override fun succeeded(description: Description) {
        Log.i(LOG_TAG, "Test succeeded $description")
    }

    /** Invoked when a test fails */
    override fun failed(e: Throwable?, description: Description) {
        Log.e(LOG_TAG, "$description test failed  with $e")
    }

    /** Invoked when a test is skipped due to a failed assumption. */
    override fun skipped(e: AssumptionViolatedException, description: Description) {
        Log.i(LOG_TAG, "Test skipped $description with $e")
    }

    companion object {
        val LOG_TAG = "FlickerServiceRule"
    }
}
