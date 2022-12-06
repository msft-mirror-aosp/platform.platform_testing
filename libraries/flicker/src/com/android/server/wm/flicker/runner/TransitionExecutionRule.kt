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
import android.util.Log
import com.android.server.wm.flicker.IFlickerTestData
import com.android.server.wm.flicker.io.ResultWriter
import com.android.server.wm.flicker.io.TraceType
import com.android.server.wm.traces.common.IScenario
import com.android.server.wm.traces.parser.getCurrentState
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import java.nio.file.Files
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Test rule to execute the transition and update [resultWriter]
 *
 * @param flicker test definition
 * @param resultWriter to write
 * @param scenario to run the transition
 * @param instrumentation to interact with the device
 * @param commands to run during the transition
 * @param wmHelper to stabilize the UI before/after transitions
 */
class TransitionExecutionRule(
    private val flicker: IFlickerTestData,
    private val resultWriter: ResultWriter,
    private val scenario: IScenario,
    private val instrumentation: Instrumentation = flicker.instrumentation,
    private val commands: List<IFlickerTestData.() -> Any> = flicker.transitions,
    private val wmHelper: WindowManagerStateHelper = flicker.wmHelper
) : TestRule {
    private var tags = mutableSetOf<String>()

    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    Utils.executeAndNotifyRunner(scenario, "Running transition $description") {
                        doRunBeforeTransition()
                        commands.forEach { it.invoke(flicker) }
                        base?.evaluate()
                    }
                } catch (e: Throwable) {
                    throw if (e is AssertionError) {
                        e
                    } else {
                        TransitionExecutionFailure(e)
                    }
                } finally {
                    doRunAfterTransition()
                }
            }
        }
    }

    private fun doRunBeforeTransition() {
        Utils.executeAndNotifyRunner(scenario, "Running doRunBeforeTransition") {
            Log.d(FLICKER_RUNNER_TAG, "doRunBeforeTransition")
            resultWriter.setTransitionStartTime()
            flicker.setCreateTagListener { doCreateTag(it) }
            doValidate()
        }
    }

    private fun doRunAfterTransition() {
        Utils.executeAndNotifyRunner(scenario, "Running doRunAfterTransition") {
            Log.d(FLICKER_RUNNER_TAG, "doRunAfterTransition")
            Utils.doWaitForUiStabilize(wmHelper)
            resultWriter.setTransitionEndTime()
            flicker.clearTagListener()
        }
    }

    private fun doValidate() {
        require(commands.isNotEmpty()) { EMPTY_TRANSITIONS_ERROR }
    }

    private fun doValidateTag(tag: String) {
        require(!tags.contains(tag)) { "Tag `$tag` already used" }
        require(!tag.contains(" ")) { "Tag can't contain spaces, instead it was `$tag`" }
        require(!tag.contains("__")) { "Tag can't `__``, instead it was `$tag`" }
    }

    private fun doCreateTag(tag: String) {
        Utils.executeAndNotifyRunner(scenario, "Creating tag $tag") {
            doValidateTag(tag)
            tags.add(tag)

            val deviceStateBytes = getCurrentState(instrumentation.uiAutomation)
            val wmDumpFile = Files.createTempFile(TraceType.WM_DUMP.fileName, tag)
            val layersDumpFile = Files.createTempFile(TraceType.SF_DUMP.fileName, tag)

            Files.write(wmDumpFile, deviceStateBytes.first)
            Files.write(layersDumpFile, deviceStateBytes.second)

            resultWriter.addTraceResult(TraceType.WM_DUMP, wmDumpFile.toFile(), tag)
            resultWriter.addTraceResult(TraceType.SF_DUMP, layersDumpFile.toFile(), tag)
        }
    }
}
