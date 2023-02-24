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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.flicker.service.IScenarioInstance
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.service.AssertionInvocationGroup
import com.android.server.wm.traces.common.service.AssertionInvocationGroup.NON_BLOCKING

/** Base class for a FaaS assertion */
abstract class AssertionTemplate : IAssertionTemplate {
    override val assertionName = this@AssertionTemplate::class.java.simpleName
    private var stabilityGroup: AssertionInvocationGroup = NON_BLOCKING

    override fun createAssertion(scenarioInstance: IScenarioInstance): IFaasAssertion {
        return object : IFaasAssertion {
            override val name: String
                get() = this@AssertionTemplate.assertionName

            override val stabilityGroup: AssertionInvocationGroup
                get() = this@AssertionTemplate.stabilityGroup

            override fun evaluate(): AssertionResult {
                val wmTraceSubject =
                    scenarioInstance.reader.readWmTrace()?.let { WindowManagerTraceSubject(it) }
                val layersTraceSubject =
                    scenarioInstance.reader.readLayersTrace()?.let { LayersTraceSubject(it) }

                var assertionError: Throwable? = null
                try {
                    if (wmTraceSubject !== null) {
                        doEvaluate(scenarioInstance, wmTraceSubject)
                    }
                    if (layersTraceSubject !== null) {
                        doEvaluate(scenarioInstance, layersTraceSubject)
                    }
                    if (wmTraceSubject !== null && layersTraceSubject !== null) {
                        doEvaluate(scenarioInstance, wmTraceSubject, layersTraceSubject)
                    }
                } catch (e: Throwable) {
                    assertionError = e
                }

                return AssertionResult(this, assertionError)
            }
        }
    }

    /**
     * Evaluates assertions that require only WM traces. NOTE: Will not run if WM trace is not
     * available.
     */
    protected open fun doEvaluate(
        scenarioInstance: IScenarioInstance,
        wmSubject: WindowManagerTraceSubject
    ) {
        // Does nothing, unless overridden
    }

    /**
     * Evaluates assertions that require only SF traces. NOTE: Will not run if layers trace is not
     * available.
     */
    protected open fun doEvaluate(
        scenarioInstance: IScenarioInstance,
        layerSubject: LayersTraceSubject
    ) {
        // Does nothing, unless overridden
    }

    /**
     * Evaluates assertions that require both SF and WM traces. NOTE: Will not run if any of the
     * traces are not available.
     */
    protected open fun doEvaluate(
        scenarioInstance: IScenarioInstance,
        wmSubject: WindowManagerTraceSubject,
        layerSubject: LayersTraceSubject
    ) {
        // Does nothing, unless overridden
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        // Ensure both assertions are instances of the same class.
        return this::class == other::class
    }

    override fun hashCode(): Int {
        var result = stabilityGroup.hashCode()
        result = 31 * result + assertionName.hashCode()
        return result
    }
}
