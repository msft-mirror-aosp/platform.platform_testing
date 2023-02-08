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

import android.os.Bundle
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.flicker.Scenario
import com.android.server.wm.flicker.annotation.FlickerServiceCompatible
import com.android.server.wm.flicker.datastore.CachedResultReader
import com.android.server.wm.flicker.datastore.DataStore
import com.android.server.wm.flicker.helpers.IS_FAAS_ENABLED
import com.android.server.wm.flicker.helpers.isShellTransitionsEnabled
import com.android.server.wm.flicker.service.FlickerService
import com.android.server.wm.flicker.service.FlickerServiceResultsCollector
import com.android.server.wm.flicker.service.assertors.IAssertionResult
import com.android.server.wm.traces.parser.withPerfettoTrace
import org.junit.runner.Description
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import org.junit.runners.model.TestClass

class FlickerServiceDecorator(
    testClass: TestClass,
    scenario: Scenario?,
    inner: IFlickerJUnitDecorator?
) : AbstractFlickerRunnerDecorator(testClass, scenario, inner) {
    private val arguments: Bundle = InstrumentationRegistry.getArguments()
    private val flickerService = FlickerService()
    private val metricsCollector: FlickerServiceResultsCollector? =
        scenario?.let {
            FlickerServiceResultsCollector(
                LegacyFlickerTraceCollector(scenario),
                collectMetricsPerTest = true
            )
        }

    private val onlyBlocking
        get() =
            scenario?.getConfigValue<Boolean>(Scenario.FAAS_BLOCKING)
                ?: arguments.getString(Scenario.FAAS_BLOCKING).toBoolean()

    private val isClassFlickerServiceCompatible: Boolean
        get() =
            testClass.annotations.filterIsInstance<FlickerServiceCompatible>().firstOrNull() != null

    override fun getChildDescription(method: FrameworkMethod?): Description? {
        requireNotNull(scenario) { "Expected to have a scenario to run" }
        return if (method is FlickerServiceCachedTestCase) {
            Description.createTestDescription(
                testClass.javaClass,
                "${method.name}[${scenario.description}]",
                *method.getAnnotations()
            )
        } else {
            inner?.getChildDescription(method)
        }
    }

    override fun getTestMethods(test: Any): List<FrameworkMethod> {
        val result = inner?.getTestMethods(test)?.toMutableList() ?: mutableListOf()
        if (shouldComputeTestMethods()) {
            withPerfettoTrace(
                "$FAAS_METRICS_PREFIX getTestMethods ${testClass.javaClass.simpleName}"
            ) {
                result.addAll(computeFlickerServiceTests(test))
                Log.d(FLICKER_TAG, "Computed ${result.size} flicker tests")
            }
        }
        return result
    }

    override fun getMethodInvoker(method: FrameworkMethod, test: Any): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                if (method is FlickerServiceCachedTestCase) {
                    val description = getChildDescription(method) ?: error("Missing description")
                    method.execute(description)
                } else {
                    inner?.getMethodInvoker(method, test)?.evaluate()
                }
            }
        }
    }

    private fun shouldComputeTestMethods(): Boolean {
        // Don't compute when called from validateInstanceMethods since this will fail
        // as the parameters will not be set. And AndroidLogOnlyBuilder is a non-executing runner
        // used to run tests in dry-run mode, so we don't want to execute in flicker transition in
        // that case either.
        val stackTrace = Thread.currentThread().stackTrace
        val isDryRun =
            stackTrace.any { it.methodName == "validateInstanceMethods" } ||
                stackTrace.any {
                    it.className == "androidx.test.internal.runner.AndroidLogOnlyBuilder"
                } ||
                stackTrace.any {
                    it.className == "androidx.test.internal.runner.NonExecutingRunner"
                }

        val filters = getFiltersFromArguments()
        // a method is filtered out if there's a filter and the filter doesn't include it's class
        // or if the filter includes its class, but it's not flicker as a service
        val isFilteredOut =
            filters.isNotEmpty() && !(filters[testClass.javaClass.simpleName] ?: false)

        return IS_FAAS_ENABLED &&
            isShellTransitionsEnabled &&
            isClassFlickerServiceCompatible &&
            !isFilteredOut &&
            !isDryRun
    }

    private fun getFiltersFromArguments(): Map<String, Boolean> {
        val testFilters = arguments.getString(OPTION_NAME) ?: return emptyMap()
        val result = mutableMapOf<String, Boolean>()

        // Test the display name against all filter arguments.
        for (testFilter in testFilters.split(",")) {
            val filterComponents = testFilter.split("#")
            if (filterComponents.size != 2) {
                Log.e(
                    LOG_TAG,
                    "Invalid filter-tests instrumentation argument supplied, $testFilter."
                )
                continue
            }
            val methodName = filterComponents[1]
            val className = filterComponents[0]
            result[className] = methodName.startsWith(FAAS_METRICS_PREFIX)
        }

        return result
    }

    /**
     * Runs the flicker transition to collect the traces and run FaaS on them to get the FaaS
     * results and then create functional test results for each of them.
     */
    private fun computeFlickerServiceTests(test: Any): List<FrameworkMethod> {
        requireNotNull(scenario) { "Expected to have a scenario to run" }
        if (!DataStore.containsFlickerServiceResult(scenario)) {
            this.doRunFlickerService(test)
        }
        val aggregateResults =
            DataStore.getFlickerServiceResults(scenario).groupBy { it.assertion.name }

        val cachedResultMethod =
            FlickerServiceCachedTestCase::class.java.getMethod("execute", Description::class.java)
        return aggregateResults.keys.mapIndexed { idx, value ->
            FlickerServiceCachedTestCase(
                cachedResultMethod,
                scenario,
                value,
                onlyBlocking,
                metricsCollector,
                isLast = aggregateResults.keys.size == idx
            )
        }
    }

    private fun doRunFlickerService(test: Any): List<IAssertionResult> {
        requireNotNull(scenario) { "Expected to have a scenario to run" }
        val description =
            Description.createTestDescription(
                this::class.java.simpleName,
                "computeFlickerServiceTests"
            )
        this.doRunTransition(test, description)

        val reader = CachedResultReader(scenario, DEFAULT_TRACE_CONFIG)
        val results = flickerService.process(reader)

        DataStore.addFlickerServiceResults(scenario, results)
        return results
    }

    companion object {
        private const val FAAS_METRICS_PREFIX = "FAAS"
        private const val OPTION_NAME = "filter-tests"
        private val LOG_TAG = FlickerServiceDecorator::class.java.simpleName
    }
}
