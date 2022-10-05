/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.view.WindowManagerGlobal
import androidx.test.platform.app.InstrumentationRegistry
import com.android.compatibility.common.util.SystemUtil
import com.android.server.wm.flicker.FlickerRunResult.Companion.RunStatus
import com.android.server.wm.flicker.dsl.FlickerBuilder
import com.android.server.wm.flicker.helpers.BrowserAppHelper
import com.android.server.wm.flicker.helpers.MessagingAppHelper
import com.google.common.truth.Truth
import java.lang.RuntimeException
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.mockito.junit.MockitoJUnitRunner

private const val TEST_NAME = "TransitionRunnerTest"

/**
 * Contains [TransitionRunner] tests.
 *
 * To run this test: `atest FlickerLibTest:TransitionRunnerTest`
 */
@RunWith(MockitoJUnitRunner::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TransitionRunnerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @After
    fun assertTracingStopped() {
        val windowManager = WindowManagerGlobal.getWindowManagerService()
        Truth.assertWithMessage("Layers Trace not stopped")
            .that(windowManager.isLayerTracing)
            .isFalse()
        Truth.assertWithMessage("WM Trace not stopped")
            .that(windowManager.isWindowTraceEnabled)
            .isFalse()
    }

    @Before
    fun clearOutputDir() {
        SystemUtil.runShellCommand("rm -rf ${getDefaultFlickerOutputDir()}")
    }

    @Test
    fun canRunTransition() {
        val runner = TransitionRunner()
        var executed = false
        val flicker =
            FlickerBuilder(instrumentation)
                .withTestName { TEST_NAME }
                .apply { transitions { executed = true } }
                .build(runner)
        Truth.assertThat(executed).isFalse()
        val result = runner.execute(flicker)
        runner.cleanUp()
        Truth.assertThat(executed).isTrue()
        Truth.assertThat(result.transitionExecutionError).isNull()
        Truth.assertThat(result.ranSuccessfully).isTrue()
    }

    @Test
    fun storesTransitionExecutionErrors() {
        val runner = TransitionRunner()
        val flicker =
            FlickerBuilder(instrumentation)
                .withTestName { TEST_NAME }
                .apply { transitions { throw RuntimeException("Failed to execute transition") } }
                .build(runner)
        val result = runner.execute(flicker)
        runner.cleanUp()
        Truth.assertThat(result.transitionExecutionError).isNotNull()
    }

    @Test
    fun storesSuccessExecutionStatusInRunResult() {
        val runner = TransitionRunner()
        val flicker =
            FlickerBuilder(instrumentation)
                .withTestName { TEST_NAME }
                .apply { transitions {} }
                .build(runner)
        val result = runner.execute(flicker)
        Truth.assertThat(result.status).isEqualTo(RunStatus.ASSERTION_SUCCESS)
    }

    @Test
    fun storesFailedExecutionStatusInRunResult() {
        val runner = TransitionRunner()
        val flicker =
            FlickerBuilder(instrumentation)
                .withTestName { TEST_NAME }
                .apply { transitions { throw RuntimeException("Failed to execute transition") } }
                .build(runner)
        val result = runner.execute(flicker)
        Truth.assertThat(result.status).isEqualTo(RunStatus.RUN_FAILED)
    }

    @Test
    fun savesTraceOnTransitionExecutionErrors() {
        val runner = TransitionRunner()
        val flicker =
            FlickerBuilder(instrumentation)
                .withTestName { TEST_NAME }
                .apply { transitions { throw Throwable() } }
                .build(runner)
        runner.execute(flicker)

        assertArchiveContainsAllTraces(runStatus = RunStatus.RUN_FAILED, testName = TEST_NAME)
    }

    @Test
    fun savesTraceOnRunCleanupErrors() {
        val runner = TransitionRunner()
        val flicker =
            FlickerBuilder(instrumentation)
                .withTestName { TEST_NAME }
                .apply {
                    transitions {}
                    teardown { throw RuntimeException("Fail on run teardown") }
                }
                .build(runner)
        runner.execute(flicker)

        assertArchiveContainsAllTraces(runStatus = RunStatus.RUN_FAILED, testName = TEST_NAME)
    }

    @Test
    fun savesTraceOnTestCleanupErrors() {
        val runner = TransitionRunner()
        val flicker =
            FlickerBuilder(instrumentation)
                .withTestName { TEST_NAME }
                .apply {
                    transitions {}
                    teardown { throw RuntimeException("Fail on test teardown") }
                }
                .build(runner)
        runner.execute(flicker)

        assertArchiveContainsAllTraces(runStatus = RunStatus.RUN_FAILED, testName = TEST_NAME)
    }

    @Test
    fun savesTraceOnTestSetupErrors() {
        val runner = TransitionRunner()
        val flicker =
            FlickerBuilder(instrumentation)
                .withTestName { TEST_NAME }
                .apply {
                    setup { throw RuntimeException("Fail on test setup") }
                    transitions {}
                    teardown {}
                }
                .build(runner)
        runner.execute(flicker)

        assertArchiveContainsAllTraces(runStatus = RunStatus.RUN_FAILED, testName = TEST_NAME)
    }

    @Test
    fun savesTraceOnTestTransitionErrors() {
        val runner = TransitionRunner()
        val flicker =
            FlickerBuilder(instrumentation)
                .withTestName { TEST_NAME }
                .apply {
                    setup {}
                    transitions { throw RuntimeException("Fail on transition teardown") }
                    teardown {}
                }
                .build(runner)
        runner.execute(flicker)

        assertArchiveContainsAllTraces(runStatus = RunStatus.RUN_FAILED, testName = TEST_NAME)
    }

    @Test
    fun savesTraceOnTestTeardownErrors() {
        val runner = TransitionRunner()
        val flicker =
            FlickerBuilder(instrumentation)
                .withTestName { TEST_NAME }
                .apply {
                    setup {}
                    transitions {}
                    teardown { throw RuntimeException("Fail on test teardown") }
                }
                .build(runner)
        runner.execute(flicker)

        assertArchiveContainsAllTraces(runStatus = RunStatus.RUN_FAILED, testName = TEST_NAME)
    }

    @Test
    fun cropsTraceAtTheRightTimestamp() {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        val setupAndTearDownTestApp = BrowserAppHelper(instrumentation)
        val transitionTestApp = MessagingAppHelper(instrumentation)

        val runner = TransitionRunner()
        val flicker =
            FlickerBuilder(instrumentation)
                .withTestName { TEST_NAME }
                .apply {
                    setup {
                        // Shouldn't be in the trace we run assertions on
                        setupAndTearDownTestApp.launchViaIntent(wmHelper)
                        setupAndTearDownTestApp.exit(wmHelper)
                    }
                    transitions {
                        // Should be in the trace we run assertions on
                        transitionTestApp.launchViaIntent(wmHelper)
                    }
                    teardown {
                        // Shouldn't be in the trace we run assertions on
                        setupAndTearDownTestApp.launchViaIntent(wmHelper)
                        setupAndTearDownTestApp.exit(wmHelper)
                    }
                }
                .build(runner)
        val result = runner.execute(flicker)

        val setupAndTearDownTestAppNeverExists =
            FlickerTestParameter.buildWMAssertion {
                require(
                    this.subjects.none {
                        it.wmState
                            .getActivitiesForWindow(setupAndTearDownTestApp.componentMatcher)
                            .isNotEmpty()
                    }
                ) {
                    "${setupAndTearDownTestApp.appName} window existed at some point " +
                        "but shouldn't have."
                }
            }

        val transitionTestAppExistsAtSomePoint =
            FlickerTestParameter.buildWMAssertion {
                require(
                    this.subjects.any {
                        it.wmState
                            .getActivitiesForWindow(transitionTestApp.componentMatcher)
                            .isNotEmpty()
                    }
                ) {
                    "${transitionTestApp.appName} window didn't exist at any point " +
                        "but should have."
                }
            }

        val setupAndTearDownTestLayerNeverExists =
            FlickerTestParameter.buildLayersAssertion {
                require(
                    this.subjects.none {
                        setupAndTearDownTestApp.componentMatcher.layerMatchesAnyOf(
                            it.entry.flattenedLayers.filter { it.isVisible }
                        )
                    }
                ) {
                    "${setupAndTearDownTestApp.appName} layer was visible at some point " +
                        "but shouldn't have."
                }

                require(
                    this.subjects.none {
                        setupAndTearDownTestApp.componentMatcher.layerMatchesAnyOf(
                            it.entry.flattenedLayers
                        )
                    }
                ) {
                    "${setupAndTearDownTestApp.appName} layer existed at some point " +
                        "but shouldn't have."
                }
            }

        val transitionTestLayerExistsAtSomePoint =
            FlickerTestParameter.buildLayersAssertion {
                require(
                    this.subjects.any {
                        transitionTestApp.componentMatcher.layerMatchesAnyOf(
                            it.entry.flattenedLayers
                        )
                    }
                ) {
                    "${transitionTestApp.appName} layer didn't exist at any point " +
                        "but should have."
                }
            }

        Truth.assertThat(result.checkAssertion(setupAndTearDownTestAppNeverExists)).isNull()
        Truth.assertThat(result.checkAssertion(transitionTestAppExistsAtSomePoint)).isNull()

        Truth.assertThat(result.checkAssertion(setupAndTearDownTestLayerNeverExists)).isNull()
        Truth.assertThat(result.checkAssertion(transitionTestLayerExistsAtSomePoint)).isNull()
    }
}
