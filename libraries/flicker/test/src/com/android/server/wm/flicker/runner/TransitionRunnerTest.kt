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

package com.android.server.wm.flicker.runner

import android.annotation.SuppressLint
import android.app.Instrumentation
import android.os.SystemClock
import android.view.WindowManagerGlobal
import androidx.test.platform.app.InstrumentationRegistry
import com.android.compatibility.common.util.SystemUtil
import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.IFlickerTestData
import com.android.server.wm.flicker.ITransitionMonitor
import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.flicker.TEST_SCENARIO
import com.android.server.wm.flicker.assertExceptionMessageCause
import com.android.server.wm.flicker.createMockedFlicker
import com.android.server.wm.flicker.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.io.ResultReader
import com.android.server.wm.flicker.io.ResultWriter
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/** Tests for [TransitionRunner] */
@SuppressLint("VisibleForTests")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TransitionRunnerTest {
    private val executionOrder = mutableListOf<String>()

    private val runSetup: IFlickerTestData.() -> Unit = {
        executionOrder.add(Consts.SETUP)
        SystemClock.sleep(100)
    }
    private val runTeardown: IFlickerTestData.() -> Unit = {
        executionOrder.add(Consts.TEARDOWN)
        SystemClock.sleep(100)
    }
    private val runTransition: IFlickerTestData.() -> Unit = {
        executionOrder.add(Consts.TRANSITION)
        SystemClock.sleep(100)
    }
    private val throwError: IFlickerTestData.() -> Unit = { error(Consts.FAILURE) }

    @Before
    fun setup() {
        executionOrder.clear()
        SystemUtil.runShellCommand("rm -rf ${getDefaultFlickerOutputDir()}")
    }

    @After
    fun assertTracingStopped() {
        val windowManager = WindowManagerGlobal.getWindowManagerService()
        Truth.assertWithMessage("Layers Trace running").that(windowManager.isLayerTracing).isFalse()
        Truth.assertWithMessage("WM Trace running")
            .that(windowManager.isWindowTraceEnabled)
            .isFalse()
    }

    @Test
    fun runsTransition() {
        val runner = TransitionRunner(TEST_SCENARIO, instrumentation, ResultWriter())
        val dummyMonitor = dummyMonitor()
        val mockedFlicker =
            createMockedFlicker(
                setup = listOf(runSetup),
                teardown = listOf(runTeardown),
                transitions = listOf(runTransition),
                extraMonitor = dummyMonitor
            )
        val result = runner.execute(mockedFlicker, Consts.description(this))
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)

        validateExecutionOrder(hasTransition = true)
        dummyMonitor.validate()
        TestUtils.validateTransitionTime(result)
        Truth.assertWithMessage("Run status")
            .that(reader.runStatus)
            .isEqualTo(RunStatus.RUN_EXECUTED)
    }

    @Test
    fun failsWithNoTransitions() {
        val runner = TransitionRunner(TEST_SCENARIO, instrumentation, ResultWriter())
        val dummyMonitor = dummyMonitor()
        val mockedFlicker =
            createMockedFlicker(
                setup = listOf(runSetup),
                teardown = listOf(runTeardown),
                extraMonitor = dummyMonitor
            )
        val result = runner.execute(mockedFlicker, Consts.description(this))
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)

        validateExecutionOrder(hasTransition = false)
        dummyMonitor.validate()
        TestUtils.validateTransitionTime(result)
        Truth.assertWithMessage("Run status").that(reader.runStatus).isEqualTo(RunStatus.RUN_FAILED)
        assertExceptionMessageCause(result.executionError, EMPTY_TRANSITIONS_ERROR)
    }

    @Test
    fun failsWithTransitionError() {
        val runner = TransitionRunner(TEST_SCENARIO, instrumentation, ResultWriter())
        val dummyMonitor = dummyMonitor()
        val mockedFlicker =
            createMockedFlicker(
                setup = listOf(runSetup),
                teardown = listOf(runTeardown),
                transitions = listOf(throwError),
                extraMonitor = dummyMonitor
            )
        val result = runner.execute(mockedFlicker, Consts.description(this))
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)

        validateExecutionOrder(hasTransition = false)
        dummyMonitor.validate()
        TestUtils.validateTransitionTime(result)
        Truth.assertWithMessage("Run status").that(reader.runStatus).isEqualTo(RunStatus.RUN_FAILED)
        assertExceptionMessageCause(result.executionError, Consts.FAILURE)
    }

    @Test
    fun failsWithSetupErrorAndHasTraces() {
        val runner = TransitionRunner(TEST_SCENARIO, instrumentation, ResultWriter())
        val dummyMonitor = dummyMonitor()
        val mockedFlicker =
            createMockedFlicker(
                setup = listOf(runSetup, throwError),
                teardown = listOf(runTeardown),
                transitions = listOf(runTransition),
                extraMonitor = dummyMonitor
            )
        val result = runner.execute(mockedFlicker, Consts.description(this))
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)

        validateExecutionOrder(hasTransition = false)
        dummyMonitor.validate()
        TestUtils.validateTransitionTimeIsEmpty(result)
        Truth.assertWithMessage("Run status").that(reader.runStatus).isEqualTo(RunStatus.RUN_FAILED)
        assertExceptionMessageCause(result.executionError, Consts.FAILURE)
    }

    @Test
    fun failsWithTeardownErrorAndHasTraces() {
        val runner = TransitionRunner(TEST_SCENARIO, instrumentation, ResultWriter())
        val dummyMonitor = dummyMonitor()
        val mockedFlicker =
            createMockedFlicker(
                setup = listOf(runSetup),
                teardown = listOf(runTeardown, throwError),
                transitions = listOf(runTransition),
                extraMonitor = dummyMonitor
            )
        val result = runner.execute(mockedFlicker, Consts.description(this))
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)

        validateExecutionOrder(hasTransition = true)
        dummyMonitor.validate()
        TestUtils.validateTransitionTime(result)
        Truth.assertWithMessage("Run status").that(reader.runStatus).isEqualTo(RunStatus.RUN_FAILED)
        assertExceptionMessageCause(result.executionError, Consts.FAILURE)
    }

    private fun assertContainsOrNot(value: String, hasValue: Boolean): String? {
        return if (hasValue) {
            Truth.assertWithMessage("$value executed").that(executionOrder).contains(value)
            value
        } else {
            Truth.assertWithMessage("$value skipped").that(executionOrder).doesNotContain(value)
            null
        }
    }

    private fun validateExecutionOrder(hasTransition: Boolean) {
        val expected = mutableListOf<String>()
        assertContainsOrNot(Consts.SETUP, hasValue = true)?.also { expected.add(it) }
        assertContainsOrNot(Consts.TRANSITION, hasTransition)?.also { expected.add(it) }
        assertContainsOrNot(Consts.TEARDOWN, hasValue = true)?.also { expected.add(it) }

        Truth.assertWithMessage("Execution order")
            .that(executionOrder)
            .containsExactlyElementsIn(expected)
            .inOrder()
    }

    private fun dummyMonitor() =
        object : ITransitionMonitor {
            private var startExecuted = false
            private var stopExecuted = false
            private var setResultExecuted = false

            override fun start() {
                startExecuted = true
            }

            override fun stop() {
                stopExecuted = true
            }

            override fun setResult(result: ResultWriter) {
                setResultExecuted = true
            }

            fun validate() {
                Truth.assertWithMessage("Start executed").that(startExecuted).isTrue()
                Truth.assertWithMessage("Stop executed").that(stopExecuted).isTrue()
                Truth.assertWithMessage("Set result executed").that(setResultExecuted).isTrue()
            }
        }

    companion object {
        private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
    }
}
