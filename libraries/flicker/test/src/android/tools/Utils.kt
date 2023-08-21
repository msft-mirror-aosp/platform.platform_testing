/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools

import android.app.Instrumentation
import android.tools.common.Scenario
import android.tools.common.ScenarioBuilder
import android.tools.common.io.Reader
import android.tools.common.io.WINSCOPE_EXT
import android.tools.common.parsers.events.EventLogParser
import android.tools.device.flicker.datastore.CachedResultWriter
import android.tools.device.flicker.legacy.AbstractFlickerTestData
import android.tools.device.flicker.legacy.FlickerBuilder
import android.tools.device.flicker.legacy.FlickerTestData
import android.tools.device.traces.TRACE_CONFIG_REQUIRE_CHANGES
import android.tools.device.traces.io.ResultReader
import android.tools.device.traces.io.ResultWriter
import android.tools.device.traces.monitors.ITransitionMonitor
import android.tools.device.traces.monitors.ScreenRecorder
import android.tools.device.traces.monitors.events.EventLogMonitor
import android.tools.device.traces.monitors.surfaceflinger.LayersTraceMonitor
import android.tools.device.traces.monitors.surfaceflinger.TransactionsTraceMonitor
import android.tools.device.traces.monitors.wm.ShellTransitionTraceMonitor
import android.tools.device.traces.monitors.wm.WindowManagerTraceMonitor
import android.tools.device.traces.monitors.wm.WmTransitionTraceMonitor
import android.tools.device.traces.parsers.WindowManagerStateHelper
import android.tools.device.traces.parsers.surfaceflinger.LayersTraceParser
import android.tools.device.traces.parsers.surfaceflinger.TransactionsTraceParser
import android.tools.device.traces.parsers.wm.TransitionTraceParser
import android.tools.device.traces.parsers.wm.WindowManagerTraceParser
import android.tools.rules.DataStoreCleanupRule
import android.tools.utils.CleanFlickerEnvironmentRule
import android.tools.utils.InMemoryArtifact
import android.tools.utils.ParsedTracesReader
import android.tools.utils.TEST_SCENARIO
import android.tools.utils.readAsset
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.rules.RuleChain
import org.mockito.Mockito

fun CleanFlickerEnvironmentRuleWithDataStore(): RuleChain =
    CleanFlickerEnvironmentRule().around(DataStoreCleanupRule())

internal fun getTraceReaderFromScenario(scenario: String): Reader {
    val scenarioTraces = getScenarioTraces("AppLaunch")

    return ParsedTracesReader(
        artifact = InMemoryArtifact(scenario),
        wmTrace = WindowManagerTraceParser().parse(scenarioTraces.wmTrace.readBytes()),
        layersTrace = LayersTraceParser().parse(scenarioTraces.layersTrace.readBytes()),
        transitionsTrace =
            TransitionTraceParser()
                .parse(
                    scenarioTraces.wmTransitions.readBytes(),
                    scenarioTraces.shellTransitions.readBytes()
                ),
        transactionsTrace =
            TransactionsTraceParser().parse(scenarioTraces.transactions.readBytes()),
        eventLog = EventLogParser().parse(scenarioTraces.eventLog.readBytes()),
    )
}

fun getScenarioTraces(scenario: String): FlickerBuilder.TraceFiles {
    lateinit var wmTrace: File
    lateinit var layersTrace: File
    lateinit var transactionsTrace: File
    lateinit var wmTransitionTrace: File
    lateinit var shellTransitionTrace: File
    lateinit var eventLog: File
    val traces =
        mapOf<String, (File) -> Unit>(
            "wm_trace" to { wmTrace = it },
            "layers_trace" to { layersTrace = it },
            "transactions_trace" to { transactionsTrace = it },
            "wm_transition_trace" to { wmTransitionTrace = it },
            "shell_transition_trace" to { shellTransitionTrace = it },
            "eventlog" to { eventLog = it }
        )
    for ((traceName, resultSetter) in traces.entries) {
        val traceBytes = readAsset("scenarios/$scenario/$traceName$WINSCOPE_EXT")
        val traceFile = File.createTempFile(traceName, WINSCOPE_EXT)
        traceFile.writeBytes(traceBytes)
        resultSetter.invoke(traceFile)
    }

    return FlickerBuilder.TraceFiles(
        wmTrace,
        layersTrace,
        transactionsTrace,
        wmTransitionTrace,
        shellTransitionTrace,
        eventLog,
    )
}

fun createMockedFlicker(
    setup: List<FlickerTestData.() -> Unit> = emptyList(),
    teardown: List<FlickerTestData.() -> Unit> = emptyList(),
    transitions: List<FlickerTestData.() -> Unit> = emptyList(),
    extraMonitor: ITransitionMonitor? = null
): FlickerTestData {
    val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    val uiDevice: UiDevice = UiDevice.getInstance(instrumentation)
    val mockedFlicker = Mockito.mock(AbstractFlickerTestData::class.java)
    val monitors: MutableList<ITransitionMonitor> =
        mutableListOf(WindowManagerTraceMonitor(), LayersTraceMonitor())
    extraMonitor?.let { monitors.add(it) }
    Mockito.`when`(mockedFlicker.wmHelper).thenReturn(WindowManagerStateHelper())
    Mockito.`when`(mockedFlicker.device).thenReturn(uiDevice)
    Mockito.`when`(mockedFlicker.outputDir).thenReturn(createTempDirectory().toFile())
    Mockito.`when`(mockedFlicker.traceMonitors).thenReturn(monitors)
    Mockito.`when`(mockedFlicker.transitionSetup).thenReturn(setup)
    Mockito.`when`(mockedFlicker.transitionTeardown).thenReturn(teardown)
    Mockito.`when`(mockedFlicker.transitions).thenReturn(transitions)
    return mockedFlicker
}

fun captureTrace(scenario: Scenario, actions: () -> Unit): ResultReader {
    if (scenario.isEmpty) {
        ScenarioBuilder().forClass("UNNAMED_CAPTURE").build()
    }
    val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    val writer =
        ResultWriter()
            .forScenario(scenario)
            .withOutputDir(createTempDirectory().toFile())
            .setRunComplete()
    val monitors =
        listOf(
            ScreenRecorder(instrumentation.targetContext),
            EventLogMonitor(),
            TransactionsTraceMonitor(),
            WmTransitionTraceMonitor(),
            ShellTransitionTraceMonitor(),
            WindowManagerTraceMonitor(),
            LayersTraceMonitor()
        )
    try {
        monitors.forEach { it.start() }
        actions.invoke()
    } finally {
        monitors.forEach { it.stop(writer) }
    }
    val result = writer.write()

    return ResultReader(result, TRACE_CONFIG_REQUIRE_CHANGES)
}

internal fun newTestCachedResultWriter(scenario: Scenario = TEST_SCENARIO) =
    CachedResultWriter()
        .forScenario(scenario)
        .withOutputDir(createTempDirectory().toFile())
        .setRunComplete()
