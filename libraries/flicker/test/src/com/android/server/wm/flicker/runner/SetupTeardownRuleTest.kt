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

import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.AbstractFlickerTestData
import com.android.server.wm.flicker.IFlickerTestData
import com.android.server.wm.flicker.TEST_SCENARIO
import com.android.server.wm.flicker.assertThrows
import com.android.server.wm.flicker.io.ResultWriter
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.mockito.Mockito

/** Tests for [SetupTeardownRule] */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SetupTeardownRuleTest {
    private var setupExecuted = false
    private var teardownExecuted = false

    private val runSetup: IFlickerTestData.() -> Unit = { setupExecuted = true }
    private val runTeardown: IFlickerTestData.() -> Unit = { teardownExecuted = true }
    private val throwError: IFlickerTestData.() -> Unit = { error(Consts.FAILURE) }

    @Before
    fun setup() {
        setupExecuted = false
        teardownExecuted = false
    }

    @Test
    fun executesSetupTeardown() {
        val rule = createRule(listOf(runSetup), listOf(runTeardown))
        rule.apply(base = null, description = Consts.description(this)).evaluate()
        Truth.assertWithMessage("Setup executed").that(setupExecuted).isTrue()
        Truth.assertWithMessage("Teardown executed").that(teardownExecuted).isTrue()
    }

    @Test
    fun throwsSetupFailureAndExecutesTeardown() {
        val failure =
            assertThrows(TransitionSetupFailure::class.java) {
                val rule = createRule(listOf(throwError, runSetup), listOf(runTeardown))
                rule.apply(base = null, description = Consts.description(this)).evaluate()
            }
        Truth.assertWithMessage("Failure").that(failure).hasMessageThat().contains(Consts.FAILURE)
        Truth.assertWithMessage("Setup executed").that(setupExecuted).isFalse()
        Truth.assertWithMessage("Teardown executed").that(teardownExecuted).isTrue()
    }

    @Test
    fun throwsTeardownFailure() {
        val failure =
            assertThrows(TransitionTeardownFailure::class.java) {
                val rule = createRule(listOf(runSetup), listOf(throwError, runTeardown))
                rule.apply(base = null, description = Consts.description(this)).evaluate()
            }
        Truth.assertWithMessage("Failure").that(failure).hasMessageThat().contains(Consts.FAILURE)
        Truth.assertWithMessage("Setup executed").that(setupExecuted).isTrue()
        Truth.assertWithMessage("Teardown executed").that(teardownExecuted).isFalse()
    }

    companion object {
        private fun createRule(
            setupCommands: List<IFlickerTestData.() -> Unit>,
            teardownCommands: List<IFlickerTestData.() -> Unit>
        ): SetupTeardownRule {
            val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
            val mockedFlicker = Mockito.mock(AbstractFlickerTestData::class.java)
            return SetupTeardownRule(
                mockedFlicker,
                ResultWriter(),
                TEST_SCENARIO,
                instrumentation,
                setupCommands,
                teardownCommands,
                WindowManagerStateHelper()
            )
        }
    }
}
