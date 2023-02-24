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
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.AbstractFlickerTestData
import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.IFlickerTestData
import com.android.server.wm.flicker.TEST_SCENARIO
import com.android.server.wm.flicker.assertExceptionMessage
import com.android.server.wm.flicker.assertExceptionMessageCause
import com.android.server.wm.flicker.assertThrows
import com.android.server.wm.flicker.io.ResultReader
import com.android.server.wm.flicker.io.ResultWriter
import com.android.server.wm.flicker.newTestResultWriter
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.mockito.Mockito

/** Tests for [TransitionExecutionRule] */
@SuppressLint("VisibleForTests")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TransitionExecutionRuleTest {
    private var executed = false

    private val runTransition: IFlickerTestData.() -> Unit = {
        executed = true
        SystemClock.sleep(100)
    }
    private val runCreateValidTags: IFlickerTestData.() -> Unit = {
        createTag(VALID_TAG_1)
        createTag(VALID_TAG_2)
    }
    private val runInvalidTagSpace: IFlickerTestData.() -> Unit = { createTag(INVALID_TAG_SPACE) }
    private val runInvalidTagUnderscore: IFlickerTestData.() -> Unit = {
        createTag(INVALID_TAG_UNDERSCORE)
    }
    private val throwTransitionError: IFlickerTestData.() -> Unit = { error(Consts.FAILURE) }
    private val throwAssertionError: IFlickerTestData.() -> Unit = {
        throw AssertionError(Consts.FAILURE)
    }

    @Before
    fun setup() {
        executed = false
    }

    @Test
    fun runSuccessfully() {
        val rule = createRule(listOf(runTransition))
        rule.apply(base = null, description = Consts.description(this)).evaluate()
        Truth.assertWithMessage("Transition executed").that(executed).isTrue()
    }

    @Test
    fun setTransitionStartAndEndTime() {
        val writer = newTestResultWriter()
        val rule = createRule(listOf(runTransition), writer)
        rule.apply(base = null, description = Consts.description(this)).evaluate()
        val result = writer.write()
        TestUtils.validateTransitionTime(result)
    }

    @Test
    fun throwsTransitionFailure() {
        val failure =
            assertThrows<TransitionExecutionFailure> {
                val rule = createRule(listOf(throwTransitionError))
                rule.apply(base = null, description = Consts.description(this)).evaluate()
            }
        assertExceptionMessageCause(failure, Consts.FAILURE)
        Truth.assertWithMessage("Transition executed").that(executed).isFalse()
    }

    @Test
    fun throwsTransitionFailureEmptyTransitions() {
        val failure =
            assertThrows<TransitionExecutionFailure> {
                val rule = createRule(listOf())
                rule.apply(base = null, description = Consts.description(this)).evaluate()
            }
        assertExceptionMessageCause(failure, EMPTY_TRANSITIONS_ERROR)
        Truth.assertWithMessage("Transition executed").that(executed).isFalse()
    }

    @Test
    fun throwsAssertionFailure() {
        val failure =
            assertThrows<AssertionError> {
                val rule = createRule(listOf(throwAssertionError))
                rule.apply(base = null, description = Consts.description(this)).evaluate()
            }
        Truth.assertWithMessage("Failure").that(failure).hasMessageThat().contains(Consts.FAILURE)
        Truth.assertWithMessage("Transition executed").that(executed).isFalse()
    }

    @Test
    fun createsValidTags() {
        val writer = newTestResultWriter()
        val rule = createRule(listOf(runCreateValidTags), writer)
        rule.apply(base = null, description = Consts.description(this)).evaluate()
        val result = writer.write()
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        val wmStateValidTag1 =
            reader.readWmState(VALID_TAG_1) ?: error("Couldn't parse WM state for $VALID_TAG_1")
        val wmStateValidTag2 =
            reader.readWmState(VALID_TAG_2) ?: error("Couldn't parse WM state for $VALID_TAG_2")
        val layerStateValidTag1 =
            reader.readLayersDump(VALID_TAG_1) ?: error("Couldn't parse SF state for $VALID_TAG_1")
        val layerStateValidTag2 =
            reader.readLayersDump(VALID_TAG_2) ?: error("Couldn't parse SF state for $VALID_TAG_2")

        Truth.assertWithMessage("File count").that(reader.countFiles()).isEqualTo(4)
        Truth.assertWithMessage("WM State - $VALID_TAG_1")
            .that(wmStateValidTag1.entries)
            .isNotEmpty()
        Truth.assertWithMessage("WM State - $VALID_TAG_2")
            .that(wmStateValidTag2.entries)
            .isNotEmpty()
        Truth.assertWithMessage("SF State - $VALID_TAG_1")
            .that(layerStateValidTag1.entries)
            .isNotEmpty()
        Truth.assertWithMessage("SF State - $VALID_TAG_2")
            .that(layerStateValidTag2.entries)
            .isNotEmpty()
    }

    @Test
    fun throwErrorCreateInvalidTagWithSpace() {
        val writer = newTestResultWriter()
        val failure =
            assertThrows<TransitionExecutionFailure> {
                val rule = createRule(listOf(runInvalidTagSpace), writer)
                rule.apply(base = null, description = Consts.description(this)).evaluate()
            }
        assertExceptionMessage(failure, INVALID_TAG_SPACE)
    }

    @Test
    fun throwErrorCreateInvalidTagDuplicate() {
        val writer = newTestResultWriter()
        val failure =
            assertThrows<TransitionExecutionFailure> {
                val rule = createRule(listOf(runCreateValidTags, runCreateValidTags), writer)
                rule.apply(base = null, description = Consts.description(this)).evaluate()
            }
        assertExceptionMessage(failure, VALID_TAG_1)
    }

    @Test
    fun throwErrorCreateInvalidTagWithUnderscore() {
        val writer = newTestResultWriter()
        val failure =
            assertThrows<TransitionExecutionFailure> {
                val rule = createRule(listOf(runInvalidTagUnderscore), writer)
                rule.apply(base = null, description = Consts.description(this)).evaluate()
            }
        assertExceptionMessage(failure, INVALID_TAG_UNDERSCORE)
    }

    companion object {
        private const val VALID_TAG_1 = "ValidTag1"
        private const val VALID_TAG_2 = "Valid_Tag2"
        private const val INVALID_TAG_SPACE = "Invalid Tag"
        private const val INVALID_TAG_UNDERSCORE = "Invalid__Tag"

        private fun createRule(
            commands: List<IFlickerTestData.() -> Unit>,
            writer: ResultWriter = newTestResultWriter()
        ): TransitionExecutionRule {
            val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
            val mockedFlicker = Mockito.mock(AbstractFlickerTestData::class.java)
            return TransitionExecutionRule(
                mockedFlicker,
                writer,
                TEST_SCENARIO,
                instrumentation,
                commands,
                WindowManagerStateHelper()
            )
        }
    }
}
