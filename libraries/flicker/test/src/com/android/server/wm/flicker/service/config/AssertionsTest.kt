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

package com.android.server.wm.flicker.service.config

import android.annotation.SuppressLint
import android.util.Log
import com.android.server.wm.flicker.TraceFileReader
import com.android.server.wm.flicker.assertiongenerator.AssertionGenConfigTestConst.Companion.emptyDeviceTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.AssertionProducerTestConst.Companion.expectedAssertionNamesFileTrace
import com.android.server.wm.flicker.assertiongenerator.ElementLifecycleExtractorTestConst
import com.android.server.wm.flicker.assertiongenerator.ScenarioConfig
import com.android.server.wm.flicker.assertiongenerator.WindowManagerTestConst.Companion.wmTrace_ROTATION_0
import com.android.server.wm.flicker.assertiongenerator.common.AssertionFactory
import com.android.server.wm.flicker.service.AssertionEngine
import com.android.server.wm.flicker.service.AssertionGeneratorConfigProducer
import com.android.server.wm.flicker.service.assertors.AssertionResult
import com.android.server.wm.traces.common.DeviceTraceDump
import com.android.server.wm.traces.common.service.PlatformConsts
import com.android.server.wm.traces.common.service.Scenario
import com.android.server.wm.traces.common.service.ScenarioInstance
import com.android.server.wm.traces.common.service.ScenarioType
import com.android.server.wm.traces.common.transactions.Transaction
import com.android.server.wm.traces.common.transactions.TransactionsTraceEntry
import com.android.server.wm.traces.common.transition.Transition
import com.android.server.wm.traces.common.transition.TransitionChange
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.google.common.truth.Truth
import kotlin.Long.Companion.MAX_VALUE
import org.junit.Test

/**
 * Contains [Assertions] tests.
 *
 * To run this test: `atest FlickerLibTest:AssertionsTest`
 */
class AssertionsTest {
    class Setup(finishTransactionVSyncId: Long) {
        private lateinit var traceDump: DeviceTraceDump
        lateinit var scenario: Scenario
        lateinit var config: Map<Scenario, ScenarioConfig>
        lateinit var transitionsTrace: TransitionsTrace
        lateinit var scenarioInstance: ScenarioInstance
        lateinit var transition: Transition
        private var startTransaction = Transaction(1, 0, 0, 0, 1)
        // vSyncId should be the last vSyncId in the entries list
        private var finishTransaction = Transaction(2, 0, finishTransactionVSyncId, 0, 2)

        private fun createTransitionsTrace() {
            transition =
                Transition(
                    Transition.Companion.Type.OPEN,
                    -MAX_VALUE,
                    MAX_VALUE,
                    0,
                    startTransaction,
                    finishTransaction,
                    listOf(TransitionChange("", Transition.Companion.Type.OPEN)),
                    true,
                    false
                )
            transitionsTrace = TransitionsTrace(arrayOf(transition))
        }

        private fun createScenarioInstance() {
            scenarioInstance =
                ScenarioInstance(
                    scenario,
                    -MAX_VALUE,
                    MAX_VALUE,
                    startTransaction,
                    finishTransaction,
                    transition
                )
        }

        fun createAppliedInEntryForTransaction(transaction: Transaction): TransactionsTraceEntry {
            return TransactionsTraceEntry(
                transaction.requestedVSyncId,
                transaction.requestedVSyncId,
                arrayOf(transaction)
            )
        }

        fun createTransactionWithAppliedEntry(transaction: Transaction): Transaction {
            val appliedInEntry = createAppliedInEntryForTransaction(transaction)
            val newTransaction =
                Transaction(
                    transaction.pid,
                    transaction.uid,
                    transaction.requestedVSyncId,
                    appliedInEntry,
                    transaction.postTime,
                    transaction.id
                )
            return newTransaction
        }

        fun setup(traceDump: DeviceTraceDump, scenario: Scenario) {
            this.traceDump = traceDump
            this.scenario = scenario
            val config = mutableMapOf<Scenario, ScenarioConfig>()
            config[scenario] =
                ScenarioConfig(arrayOf(traceDump), arrayOf(emptyDeviceTraceConfiguration))
            this.config = config
            startTransaction = createTransactionWithAppliedEntry(startTransaction)
            finishTransaction = createTransactionWithAppliedEntry(finishTransaction)
            createTransitionsTrace()
            createScenarioInstance()
        }
    }

    fun assertResultsPass(assertionResults: List<AssertionResult>) {
        var failed = 0
        val failInfo: MutableList<String> = mutableListOf()
        assertionResults.forEach { assertionResult ->
            run {
                if (assertionResult.failed) {
                    failed++
                    failInfo.add(
                        assertionResult.assertionName +
                            "\n" +
                            assertionResult.assertionError.toString() +
                            "\n\n"
                    )
                }
            }
        }
        try {
            Truth.assertThat(failed).isEqualTo(0)
        } catch (err: AssertionError) {
            throw RuntimeException("$failed assertions failed:\n" + failInfo.toString())
        }
    }

    @Test
    fun AssertionEngine_analyzeGeneratedAssertions_pass() {
        val layersTrace =
            ElementLifecycleExtractorTestConst.createTrace_arg(
                ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAssertionProducer
            )
        val wmTrace = wmTrace_ROTATION_0
        val traceDump = DeviceTraceDump(null, layersTrace)
        val scenarioType = ScenarioType.APP_LAUNCH
        val scenario = Scenario(scenarioType, PlatformConsts.Rotation.ROTATION_0)
        val testSetup = Setup(3)
        testSetup.setup(traceDump, scenario)
        var assertionEngine =
            AssertionEngine(AssertionGeneratorConfigProducer(testSetup.config)) {
                Log.v("FLICKER-ASSERT", it)
            }
        val assertionResults =
            assertionEngine.analyze(
                wmTrace,
                layersTrace,
                testSetup.transitionsTrace,
                AssertionEngine.AssertionsToUse.GENERATED
            )
        assertResultsPass(assertionResults)
    }

    @Test
    fun AssertionEngine_analyzeGeneratedAssertions_fail() {
        val layersTraceForGeneration =
            ElementLifecycleExtractorTestConst.createTrace_arg(
                ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAllVisibilityAssertions
            )
        val wmTrace = wmTrace_ROTATION_0
        val traceDump = DeviceTraceDump(null, layersTraceForGeneration)
        val scenarioType = ScenarioType.APP_LAUNCH
        val scenario = Scenario(scenarioType, PlatformConsts.Rotation.ROTATION_0)
        val testSetup = Setup(3)
        testSetup.setup(traceDump, scenario)
        var assertionEngine =
            AssertionEngine(AssertionGeneratorConfigProducer(testSetup.config)) {
                Log.v("FLICKER-ASSERT", it)
            }
        val layersTraceForTest =
            ElementLifecycleExtractorTestConst.createTrace_arg(
                ElementLifecycleExtractorTestConst.mapOfFlattenedLayersAllVisibilityAssertions_fail1
            )
        val failSetup = Setup(2)
        failSetup.setup(DeviceTraceDump(null, layersTraceForTest), scenario)
        val assertionResults =
            assertionEngine.analyze(
                wmTrace,
                layersTraceForTest,
                failSetup.transitionsTrace,
                AssertionEngine.AssertionsToUse.GENERATED
            )
        val assertionError = assertionResults[0].assertionError.toString()
        Truth.assertThat(
            assertionError.contains(
                "Assertion : notContains(StatusBar)\n" + "\tCould find: Layer:StatusBar"
            )
        )
    }

    @Test
    fun AssertionEngine_analyze_pass_traceFile() {
        val defaultConfigProducer =
            AssertionGeneratorConfigProducer(configDir = "/assertiongenerator_config_test")
        val assertionEngine = AssertionEngine(defaultConfigProducer) { Log.v("FLICKER-ASSERT", it) }
        val config = defaultConfigProducer.produce()
        val scenario = Scenario(ScenarioType.APP_LAUNCH, PlatformConsts.Rotation.ROTATION_0)
        val traceDump = config[scenario]?.deviceTraceDumps?.get(0)
        traceDump ?: run { throw RuntimeException("No deviceTraceDump for scenario APP_LAUNCH") }
        val layersTrace = traceDump.layersTrace
        val transitionsTrace = traceDump.transitionsTrace
        val wmTrace = traceDump.wmTrace

        transitionsTrace ?: run { throw RuntimeException("Null transitionsTrace") }
        wmTrace ?: run { throw RuntimeException("Null wmTrace") }
        layersTrace ?: run { throw RuntimeException("Null layersTrace") }

        val assertionResults =
            assertionEngine.analyze(
                wmTrace,
                layersTrace,
                transitionsTrace,
                AssertionEngine.AssertionsToUse.GENERATED
            )
        assertResultsPass(assertionResults)
    }

    @SuppressLint("VisibleForTests")
    @Test
    fun AssertionEngine_analyze_assertionNames_traceFile() {
        val configDir = "/assertiongenerator_config_test"
        val goldenTracesConfig = TraceFileReader.getGoldenTracesConfig(configDir)
        val scenario = Scenario(ScenarioType.APP_LAUNCH, PlatformConsts.Rotation.ROTATION_0)
        val traceDump = goldenTracesConfig[scenario]!!.deviceTraceDumps[0]
        val traceConfiguration = goldenTracesConfig[scenario]!!.traceConfigurations[0]

        val assertionFactory = AssertionFactory(goldenTracesConfig)
        val layersTrace = traceDump.layersTrace
        val wmTrace = traceDump.wmTrace!!
        val transitionsTrace = traceDump.transitionsTrace!!

        val scenarioInstances = mutableListOf<ScenarioInstance>()
        val scenarioType = ScenarioType.APP_LAUNCH
        scenarioInstances.addAll(
            scenarioType.getInstances(transitionsTrace, wmTrace) { m ->
                Log.d("AssertionEngineTest", m)
            }
        )

        var assertionsStr = ""

        layersTrace?.run {
            for (scenarioInstance in scenarioInstances) {
                val assertions =
                    Assertions.getGeneratedAssertionsForScenario(
                        scenarioInstance,
                        assertionFactory
                    ) { Log.v("FLICKER-ASSERT", it) }
                assertions.forEach { assertion -> assertionsStr += assertion.name + "\n" }
            }
            Truth.assertThat(assertionsStr).isEqualTo(expectedAssertionNamesFileTrace)
        }
            ?: throw RuntimeException("LayersTrace is null")
    }
}
