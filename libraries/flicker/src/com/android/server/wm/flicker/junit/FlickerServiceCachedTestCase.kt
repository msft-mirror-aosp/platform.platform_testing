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

package com.android.server.wm.flicker.junit

import com.android.server.wm.flicker.datastore.DataStore
import com.android.server.wm.flicker.service.FlickerServiceResultsCollector
import com.android.server.wm.flicker.service.assertors.IAssertionResult
import com.android.server.wm.traces.common.Cache
import com.android.server.wm.traces.common.IScenario
import com.android.server.wm.traces.common.service.AssertionInvocationGroup
import java.lang.reflect.Method
import org.junit.Assume
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runners.model.FrameworkMethod

class FlickerServiceCachedTestCase(
    method: Method,
    scenario: IScenario,
    internal val assertionName: String,
    private val onlyBlocking: Boolean,
    private val metricsCollector: FlickerServiceResultsCollector?,
    private val isLast: Boolean
) : FrameworkMethod(method) {
    private val fullResults =
        DataStore.getFlickerServiceResultsForAssertion(scenario, assertionName)
    private val results: List<IAssertionResult>
        get() =
            fullResults.filter {
                !onlyBlocking || it.assertion.stabilityGroup == AssertionInvocationGroup.BLOCKING
            }

    override fun invokeExplosively(target: Any?, vararg params: Any?): Any {
        error("Shouldn't have reached here")
    }

    override fun getName(): String = "FaaS_$assertionName"

    fun execute(description: Description) {
        val results = results

        if (isLast) {
            metricsCollector?.testStarted(description)
        }
        try {
            Assume.assumeFalse(results.isEmpty())
            results.firstOrNull { it.assertionError != null }?.assertionError?.let { throw it }
        } catch (e: Throwable) {
            metricsCollector?.testFailure(Failure(description, e))
            throw e
        } finally {
            if (isLast) {
                Cache.clear()
                metricsCollector?.testFinished(description)
            }
        }
    }
}
