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

package com.android.server.wm.flicker

import android.app.Instrumentation
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.android.server.wm.flicker.datastore.CachedResultWriter
import com.android.server.wm.flicker.io.ResultWriter
import com.android.server.wm.flicker.io.WINSCOPE_EXT
import com.android.server.wm.flicker.monitor.LayersTraceMonitor
import com.android.server.wm.flicker.monitor.WindowManagerTraceMonitor
import com.android.server.wm.flicker.traces.FlickerSubjectException
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.layers.LayersTraceParser
import com.android.server.wm.traces.parser.transaction.TransactionsTraceParser
import com.android.server.wm.traces.parser.transition.TransitionsTraceParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerDumpParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser
import com.google.common.io.ByteStreams
import com.google.common.truth.StringSubject
import com.google.common.truth.Truth
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeBytes
import org.mockito.Mockito

internal val TEST_SCENARIO = ScenarioBuilder().forClass("test").build()

internal fun outputFileName(status: RunStatus) =
    Paths.get("/sdcard/flicker/${status.prefix}_test_ROTATION_0_GESTURAL_NAV.zip")

internal fun newTestResultWriter() =
    ResultWriter()
        .forScenario(TEST_SCENARIO)
        .withOutputDir(getDefaultFlickerOutputDir())
        .setRunComplete()

internal fun newTestCachedResultWriter() =
    CachedResultWriter()
        .forScenario(TEST_SCENARIO)
        .withOutputDir(getDefaultFlickerOutputDir())
        .setRunComplete()

internal fun readWmTraceFromFile(
    relativePath: String,
    from: Long = Long.MIN_VALUE,
    to: Long = Long.MAX_VALUE,
    addInitialEntry: Boolean = true
): WindowManagerTrace {
    return try {
        WindowManagerTraceParser()
            .parse(readAsset(relativePath), from, to, addInitialEntry, clearCache = false)
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}

internal fun readWmTraceFromDumpFile(relativePath: String): WindowManagerTrace {
    return try {
        WindowManagerDumpParser().parse(readAsset(relativePath), clearCache = false)
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}

internal fun readLayerTraceFromFile(
    relativePath: String,
    ignoreOrphanLayers: Boolean = true
): LayersTrace {
    return try {
        LayersTraceParser(
                ignoreLayersStackMatchNoDisplay = false,
                ignoreLayersInVirtualDisplay = false
            ) { ignoreOrphanLayers }
            .parse(readAsset(relativePath))
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}

internal fun readTransactionsTraceFromFile(relativePath: String): TransactionsTrace {
    return try {
        TransactionsTraceParser().parse(readAsset(relativePath))
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}

internal fun readTransitionsTraceFromFile(
    relativePath: String,
    transactionsTrace: TransactionsTrace
): TransitionsTrace {
    return try {
        TransitionsTraceParser(transactionsTrace).parse(readAsset(relativePath))
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}

@Throws(Exception::class)
internal fun readAsset(relativePath: String): ByteArray {
    val context: Context = InstrumentationRegistry.getInstrumentation().context
    val inputStream = context.resources.assets.open("testdata/$relativePath")
    return ByteStreams.toByteArray(inputStream)
}

@Throws(IOException::class)
fun readAssetAsFile(relativePath: String): File {
    val context: Context = InstrumentationRegistry.getInstrumentation().context
    return File(context.cacheDir, relativePath).also {
        if (!it.exists()) {
            it.outputStream().use { cache ->
                context.assets.open("testdata/$relativePath").use { inputStream ->
                    inputStream.copyTo(cache)
                }
            }
        }
    }
}

/**
 * Runs `r` and asserts that an exception of type `expectedThrowable` is thrown.
 * @param expectedThrowable the type of throwable that is expected to be thrown
 * @param r the [Runnable] which is run and expected to throw.
 * @throws AssertionError if `r` does not throw, or throws a runnable that is not an instance of
 * `expectedThrowable`.
 */
inline fun <reified ExceptionType> assertThrows(r: () -> Unit): ExceptionType {
    try {
        r()
    } catch (t: Throwable) {
        when {
            ExceptionType::class.java.isInstance(t) -> return t as ExceptionType
            t is Exception ->
                throw AssertionError(
                    "Expected ${ExceptionType::class.java}, but got '${t.javaClass}'",
                    t
                )
            // Re-throw Errors and other non-Exception throwables.
            else -> throw t
        }
    }
    error("Expected exception ${ExceptionType::class.java}, but nothing was thrown")
}

fun assertFailureFact(
    failure: FlickerSubjectException,
    factKey: String,
    factIndex: Int = 0
): StringSubject {
    val matchingFacts = failure.facts.filter { it.key == factKey }

    if (factIndex >= matchingFacts.size) {
        val message = buildString {
            appendLine("Cannot find failure fact with key '$factKey' and index $factIndex")
            appendLine()
            appendLine("Available facts:")
            failure.facts.forEach { appendLine(it.toString()) }
        }
        throw AssertionError(message)
    }

    return Truth.assertThat(matchingFacts[factIndex].value)
}

fun assertThatErrorContainsDebugInfo(error: Throwable, withBlameEntry: Boolean = true) {
    Truth.assertThat(error).hasMessageThat().contains("What?")
    Truth.assertThat(error).hasMessageThat().contains("Where?")
    Truth.assertThat(error).hasMessageThat().contains("Facts")
    Truth.assertThat(error).hasMessageThat().contains("Trace start")
    Truth.assertThat(error).hasMessageThat().contains("Trace end")

    if (withBlameEntry) {
        Truth.assertThat(error).hasMessageThat().contains("State")
    }
}

fun assertArchiveContainsFiles(archivePath: Path, expectedFiles: List<String>) {
    Truth.assertWithMessage("Expected trace archive `$archivePath` to exist")
        .that(Files.exists(archivePath))
        .isTrue()

    val archiveStream = ZipInputStream(FileInputStream(archivePath.toFile()))

    val actualFiles = generateSequence { archiveStream.nextEntry }.map { it.name }.toList()

    Truth.assertWithMessage("Trace archive doesn't contain all expected traces")
        .that(actualFiles)
        .containsExactlyElementsIn(expectedFiles)
}

fun getScenarioTraces(scenario: String): FlickerBuilder.TraceFiles {
    val randomString = (1..10).map { (('A'..'Z') + ('a'..'z')).random() }.joinToString("")

    var wmTrace: File? = null
    var layersTrace: File? = null
    var transactionsTrace: File? = null
    var transitionsTrace: File? = null
    val traces =
        mapOf<String, (File) -> Unit>(
            "wm_trace" to { wmTrace = it },
            "layers_trace" to { layersTrace = it },
            "transactions_trace" to { transactionsTrace = it },
            "transition_trace" to { transitionsTrace = it }
        )
    for ((traceName, resultSetter) in traces.entries) {
        val traceBytes = readAsset("scenarios/$scenario/$traceName$WINSCOPE_EXT")
        val traceFile =
            getDefaultFlickerOutputDir().resolve("${traceName}_$randomString$WINSCOPE_EXT")
        traceFile.parent.createDirectories()
        traceFile.createFile()
        traceFile.writeBytes(traceBytes)
        resultSetter.invoke(traceFile.toFile())
    }

    return FlickerBuilder.TraceFiles(
        wmTrace!!,
        layersTrace!!,
        transactionsTrace!!,
        transitionsTrace!!
    )
}

fun assertExceptionMessage(error: Throwable?, expectedValue: String) {
    Truth.assertWithMessage("Expected exception")
        .that(error)
        .hasMessageThat()
        .contains(expectedValue)
}

fun assertExceptionMessageCause(error: Throwable?, expectedValue: String) {
    Truth.assertWithMessage("Expected cause")
        .that(error)
        .hasCauseThat()
        .hasMessageThat()
        .contains(expectedValue)
}

fun createMockedFlicker(
    setup: List<IFlickerTestData.() -> Unit> = emptyList(),
    teardown: List<IFlickerTestData.() -> Unit> = emptyList(),
    transitions: List<IFlickerTestData.() -> Unit> = emptyList(),
    extraMonitor: ITransitionMonitor? = null
): IFlickerTestData {
    val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    val uiDevice: UiDevice = UiDevice.getInstance(instrumentation)
    val mockedFlicker = Mockito.mock(AbstractFlickerTestData::class.java)
    val monitors: MutableList<ITransitionMonitor> =
        mutableListOf(WindowManagerTraceMonitor(), LayersTraceMonitor())
    extraMonitor?.let { monitors.add(it) }
    Mockito.`when`(mockedFlicker.wmHelper).thenReturn(WindowManagerStateHelper())
    Mockito.`when`(mockedFlicker.device).thenReturn(uiDevice)
    Mockito.`when`(mockedFlicker.outputDir).thenReturn(getDefaultFlickerOutputDir())
    Mockito.`when`(mockedFlicker.traceMonitors).thenReturn(monitors)
    Mockito.`when`(mockedFlicker.transitionSetup).thenReturn(setup)
    Mockito.`when`(mockedFlicker.transitionTeardown).thenReturn(teardown)
    Mockito.`when`(mockedFlicker.transitions).thenReturn(transitions)
    return mockedFlicker
}
