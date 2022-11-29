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

package com.android.server.wm.traces.common.service

import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

enum class ScenarioType(
    val description: String,
    val executionCondition: AssertionExecutionCondition
) {
    COMMON("Common", AssertionExecutionCondition.ALWAYS),
    APP_LAUNCH("AppLaunch", AssertionExecutionCondition.APP_LAUNCH),
    APP_CLOSE("AppClose", AssertionExecutionCondition.APP_CLOSE),
    // NOT YET IMPLEMENTED SCENARIOS - set to never run
    ROTATION("Rotation", AssertionExecutionCondition.NEVER),
    IME_APPEAR("ImeAppear", AssertionExecutionCondition.NEVER),
    IME_DISAPPEAR("ImeDisappear", AssertionExecutionCondition.NEVER),
    PIP_ENTER("PipEnter", AssertionExecutionCondition.NEVER),
    PIP_EXIT("PipExit", AssertionExecutionCondition.NEVER);

    /**
     * Gets the concrete instances of a Scenario that appear in the provided traces. An instance
     * (which includes start and end marks) is returned for each point in the traces a scenario is
     * detected to have occurred.
     */
    fun getInstances(
        transitionsTrace: TransitionsTrace,
        wmTrace: WindowManagerTrace,
        logger: ((String) -> Unit)? = null
    ): Collection<ScenarioInstance> {
        val rotation = wmTrace.getInitialRotation()
        return getInstancesWithRotation(transitionsTrace, rotation, logger)
    }

    /**
     * Gets the concrete instances of a Scenario that appear in the provided traces. An instance
     * (which includes start and end marks) is returned for each point in the traces a scenario is
     * detected to have occurred.
     *
     * The rotation is the provided one. This function should be used for tests where there is no
     * mock wmTrace, so the rotation can be passed directly
     */
    fun getInstancesWithRotation(
        transitionsTrace: TransitionsTrace,
        rotation: PlatformConsts.Rotation,
        logger: ((String) -> Unit)? = null
    ): Collection<ScenarioInstance> {
        val scenario = Scenario(this, rotation)
        val scenarioInstances = mutableListOf<ScenarioInstance>()
        for (transition in transitionsTrace.entries) {
            if (transition.isIncomplete) {
                // We don't want to run assertions on incomplete transitions
                logger?.invoke("Skipping running assertions on incomplete transition $transition")
                continue
            }
            if (this.executionCondition.shouldExecute(transition)) {
                requireNotNull(transition.startTransaction) {
                    "Completed transition shouldn't have a null startTransaction"
                }
                requireNotNull(transition.finishTransaction) {
                    "Completed transition shouldn't have a null finishTransaction"
                }
                scenarioInstances.add(
                    ScenarioInstance(
                        scenario,
                        transition.collectingStart,
                        transition.end,
                        transition.startTransaction,
                        transition.finishTransaction,
                        transition
                    )
                )
            }
        }

        return scenarioInstances
    }

    companion object {
        val scenariosByDescription: Map<String, ScenarioType> =
            mapOf(
                "Common" to COMMON,
                "AppLaunch" to APP_LAUNCH,
                "AppClose" to APP_CLOSE,
                "Rotation" to ROTATION,
                "ImeAppear" to IME_APPEAR,
                "ImeDisappear" to IME_DISAPPEAR,
                "PipEnter" to PIP_ENTER,
                "PipExit" to PIP_EXIT
            )
    }
}
